package com.example.solver

import com.example.model.DFA
import com.example.model.DFAState
import com.example.model.Transition
import com.example.regex.*

data class StateEquationInfo(
    val stateId: String,
    val variableName: String,
    val isFinal: Boolean,
    val outgoingTransitions: List<Transition>,
    val transitionTerms: List<String>,
    val hasEpsilon: Boolean,
    val equationAST: RegexNode,
    val equationString: String
)

data class StateEquationsResult(
    val equations: Map<String, RegexNode>,       // VariableName -> AST
    val equationStrings: Map<String, String>,   // VariableName -> Formatted String
    val stateInfoList: List<StateEquationInfo>,
    val fullSystemFormatted: String
)

object EquationBuilder {

    /**
     * Constructs the linear language equations from the actual DFA states and transitions.
     * For each state qi:
     *   Ri represents the set of strings taking state qi to an accepting state.
     *   For every transition qi --a--> qj, adds term 'a Rj'.
     *   If qi is an accepting/final state, adds term 'ε'.
     */
    fun buildEquations(dfa: DFA): StateEquationsResult {
        val states = dfa.states
        val eqMap = mutableMapOf<String, RegexNode>()
        val eqStrings = mutableMapOf<String, String>()
        val infoList = mutableListOf<StateEquationInfo>()

        for (state in states) {
            val varName = RegexFormatter.stateToVariable(state.id)
            val transitionsFromState = dfa.getTransitionsFrom(state.id)
            val terms = mutableListOf<RegexNode>()
            val termDescriptions = mutableListOf<String>()

            // Group transitions by target state to factor symbols: e.g. aR2 + bR2 -> (a+b)R2
            val groupedByDest = transitionsFromState.groupBy { it.to }
            for ((destId, transList) in groupedByDest) {
                val targetVarName = RegexFormatter.stateToVariable(destId)
                val targetVar = Variable(targetVarName)
                val symbols = transList.map { it.symbol }.distinct()
                val symbolNodes = symbols.map { Literal(it) }
                val symbolExpr = if (symbolNodes.size == 1) symbolNodes[0] else RegexFactory.union(symbolNodes)
                
                val termNode = RegexFactory.concat(symbolExpr, targetVar)
                terms.add(termNode)

                val symDisplay = if (symbols.size == 1) symbols[0] else "(${symbols.joinToString("+")})"
                val symListStr = symbols.joinToString(",")
                termDescriptions.add("$symDisplay$targetVarName (from ${state.id} --$symListStr--> $destId)")
            }

            val hasEpsilon = state.isFinal
            if (hasEpsilon) {
                terms.add(Epsilon)
                termDescriptions.add("ε (because ${state.id} is an accepting/final state)")
            }

            val rhsNode = if (terms.isEmpty()) EmptySet else RegexFactory.union(terms)
            eqMap[varName] = rhsNode

            val eqStr = "$varName = ${rhsNode.toDisplayString()}"
            eqStrings[varName] = eqStr

            infoList.add(
                StateEquationInfo(
                    stateId = state.id,
                    variableName = varName,
                    isFinal = state.isFinal,
                    outgoingTransitions = transitionsFromState,
                    transitionTerms = termDescriptions,
                    hasEpsilon = hasEpsilon,
                    equationAST = rhsNode,
                    equationString = eqStr
                )
            )
        }

        val fullSystem = infoList.joinToString("\n") { it.equationString }

        return StateEquationsResult(
            equations = eqMap,
            equationStrings = eqStrings,
            stateInfoList = infoList,
            fullSystemFormatted = fullSystem
        )
    }
}
