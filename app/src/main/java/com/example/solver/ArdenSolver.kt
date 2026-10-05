package com.example.solver

import com.example.model.ArdenSolution
import com.example.model.DFA
import com.example.model.SolverStep
import com.example.regex.*

object ArdenSolver {

    fun solve(dfa: DFA): ArdenSolution {
        val states = dfa.states
        val startState = dfa.startState ?: states.first()
        val startVar = RegexFormatter.stateToVariable(startState.id)
        val steps = mutableListOf<SolverStep>()
        var stepCounter = 1
        var ardenCount = 0
        var subsCount = 0

        // Step 1: Build initial state language equations using EquationBuilder
        val eqResult = EquationBuilder.buildEquations(dfa)
        val eqMap = eqResult.equations.toMutableMap()
        val currentDisplayMap = eqResult.equationStrings.toMutableMap()

        steps.add(
            SolverStep(
                stepNumber = stepCounter++,
                title = "CREATE STATE EQUATIONS",
                equationBefore = "DFA Analysis (${states.size} states, Alphabet: {${dfa.alphabet.joinToString()}})",
                operation = "Setup State Language Equations",
                theoremUsed = null,
                substitution = null,
                equationAfter = eqResult.fullSystemFormatted,
                explanation = "Set up linear language equations for every state qi where Ri represents strings reaching an accepting state.\n" +
                        "For every transition qi --a--> qj, term 'aRj' is added to Ri.\n" +
                        "For every final/accepting state, 'ε' is added.",
                currentEquations = currentDisplayMap.toMap(),
                whyExplanation = "In formal language theory, each variable Ri represents the language of words accepted from state qi. " +
                        "A transition reading symbol 'a' moves to qj, prepending 'a' to language Rj. Accepting states accept the empty word ε directly."
            )
        )

        // Elimination order: Process all states other than the start state first, from last to first
        val eliminationOrder = states
            .map { RegexFormatter.stateToVariable(it.id) }
            .filter { it != startVar }
            .reversed()

        for (vName in eliminationOrder) {
            var currentRhs = eqMap[vName] ?: EmptySet

            // 1. Check if equation has a self-loop: vName = A * vName + B
            val split = RegexFactory.splitLinearForm(currentRhs, vName)
            if (split != null && split.first !is EmptySet) {
                val (a, b) = split
                val eqBeforeStr = "$vName = ${currentRhs.toDisplayString()}"
                val aStar = RegexSimplifier.simplify(RegexFactory.star(a))
                val resolvedRhs = RegexSimplifier.simplify(RegexFactory.concat(aStar, b))
                eqMap[vName] = resolvedRhs
                currentDisplayMap[vName] = "$vName = ${resolvedRhs.toDisplayString()}"
                ardenCount++

                val eqAfterStr = "$vName = ${resolvedRhs.toDisplayString()}"
                steps.add(
                    SolverStep(
                        stepNumber = stepCounter++,
                        title = "SOLVE $vName",
                        equationBefore = eqBeforeStr,
                        operation = "Using Arden's Theorem: $vName = (${a.toDisplayString()})$vName + (${b.toDisplayString()}) ⟹ $vName = (${a.toDisplayString()})* (${b.toDisplayString()})",
                        theoremUsed = "Arden's Theorem",
                        substitution = null,
                        equationAfter = eqAfterStr,
                        explanation = "$vName has self-recursion with coefficient (${a.toDisplayString()}).\n" +
                                "Applying Arden's Theorem gives $vName = (${a.toDisplayString()})* [${b.toDisplayString()}].",
                        currentEquations = currentDisplayMap.toMap(),
                        whyExplanation = "$vName has the form R = AR + B where A = ${a.toDisplayString()} and B = ${b.toDisplayString()}.\n" +
                                "Since ε ∉ L(A), Arden's Theorem guarantees the unique solution R = A*B."
                    )
                )
                currentRhs = resolvedRhs
            }

            // 2. Substitute resolved vName into all other equations that contain it
            val otherVars = eqMap.keys.filter { it != vName }
            for (otherVar in otherVars) {
                val otherRhs = eqMap[otherVar] ?: continue
                if (otherRhs.containsVariable(vName)) {
                    val subResult = SubstitutionEngine.substitute(
                        targetVar = otherVar,
                        targetExpr = otherRhs,
                        varName = vName,
                        replacement = currentRhs
                    )
                    eqMap[otherVar] = subResult.resultAST
                    currentDisplayMap[otherVar] = subResult.equationAfter
                    subsCount++

                    steps.add(
                        SolverStep(
                            stepNumber = stepCounter++,
                            title = "SUBSTITUTE $vName INTO $otherVar",
                            equationBefore = subResult.equationBefore,
                            operation = "Substitute ${subResult.replacementExpression}",
                            theoremUsed = "Symbolic Substitution",
                            substitution = "$vName ⟼ ${currentRhs.toDisplayString()}",
                            equationAfter = subResult.equationAfter,
                            explanation = "Replaced variable $vName in $otherVar with its resolved expression (${currentRhs.toDisplayString()}) and simplified algebraically.",
                            currentEquations = currentDisplayMap.toMap(),
                            whyExplanation = "By algebraic substitution, eliminating already-resolved variable $vName from $otherVar preserves language equivalence while reducing the number of unknowns."
                        )
                    )
                }
            }
        }

        // Finally, resolve start state variable: startVar
        var startRhs = eqMap[startVar] ?: EmptySet
        val startSplit = RegexFactory.splitLinearForm(startRhs, startVar)
        if (startSplit != null && startSplit.first !is EmptySet) {
            val (a, b) = startSplit
            val eqBeforeStr = "$startVar = ${startRhs.toDisplayString()}"
            val aStar = RegexSimplifier.simplify(RegexFactory.star(a))
            val resolvedStart = RegexSimplifier.simplify(RegexFactory.concat(aStar, b))
            eqMap[startVar] = resolvedStart
            currentDisplayMap[startVar] = "$startVar = ${resolvedStart.toDisplayString()}"
            ardenCount++

            val eqAfterStr = "$startVar = ${resolvedStart.toDisplayString()}"
            steps.add(
                SolverStep(
                    stepNumber = stepCounter++,
                    title = "SOLVE START STATE $startVar",
                    equationBefore = eqBeforeStr,
                    operation = "Using Arden's Theorem on $startVar: (${a.toDisplayString()})* (${b.toDisplayString()})",
                    theoremUsed = "Arden's Theorem",
                    substitution = null,
                    equationAfter = eqAfterStr,
                    explanation = "The start state variable $startVar has a recursive self-loop (${a.toDisplayString()}).\n" +
                            "Applied Arden's Theorem to obtain the closed-form expression $startVar = (${a.toDisplayString()})* [${b.toDisplayString()}].",
                    currentEquations = currentDisplayMap.toMap(),
                    whyExplanation = "All other state variables have been substituted out. Solving $startVar via Arden's Theorem yields the complete regular expression for the DFA."
                )
            )
            startRhs = resolvedStart
        }

        // Final simplification step
        val finalSimplified = RegexSimplifier.simplify(startRhs)
        val finalRegexStr = finalSimplified.toDisplayString()

        steps.add(
            SolverStep(
                stepNumber = stepCounter,
                title = "FINAL REGULAR EXPRESSION",
                equationBefore = "$startVar = ${startRhs.toDisplayString()}",
                operation = "Algebraic Regular Expression Simplification",
                theoremUsed = "Identity Laws (R+∅=R, εR=R, (∅)*=ε, R+R=R)",
                substitution = null,
                equationAfter = "$startVar = $finalRegexStr",
                explanation = "Applied algebraic simplification laws to obtain the minimal canonical Regular Expression for start state '$startVar'.",
                currentEquations = mapOf(startVar to finalRegexStr),
                whyExplanation = "Algebraic reduction removes redundant identity terms (ε, ∅, duplicate terms) while strictly preserving language equivalence L(R)."
            )
        )

        return ArdenSolution(
            dfa = dfa,
            initialEquations = eqResult.equationStrings,
            steps = steps,
            finalRegex = finalSimplified,
            finalRegexString = finalRegexStr,
            ardenApplicationsCount = ardenCount,
            substitutionsCount = subsCount
        )
    }
}
