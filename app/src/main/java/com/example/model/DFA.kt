package com.example.model

import com.example.regex.RegexNode

data class DFAState(
    val id: String,
    val isStart: Boolean = false,
    val isFinal: Boolean = false
)

data class Transition(
    val from: String,
    val symbol: String,
    val to: String
)

data class DFA(
    val states: List<DFAState>,
    val alphabet: List<String>,
    val transitions: List<Transition>
) {
    val startState: DFAState?
        get() = states.firstOrNull { it.isStart }

    val finalStates: List<DFAState>
        get() = states.filter { it.isFinal }

    fun getTransitionsFrom(stateId: String): List<Transition> {
        return transitions.filter { it.from == stateId }
    }

    /**
     * Simulates the DFA on the given input string.
     * Returns true if accepted, false if rejected.
     */
    fun simulate(input: String): Boolean {
        var current = startState?.id ?: return false
        for (char in input) {
            val sym = char.toString()
            val next = transitions.firstOrNull { it.from == current && it.symbol == sym }?.to
            if (next == null) return false
            current = next
        }
        return finalStates.any { it.id == current }
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val message: String = if (isValid) "DFA Validated Successfully" else "DFA Validation Failed"
)

data class SolverStep(
    val stepNumber: Int,
    val title: String,
    val equationBefore: String,
    val operation: String,
    val theoremUsed: String? = null,
    val substitution: String? = null,
    val equationAfter: String,
    val explanation: String,
    val currentEquations: Map<String, String> = emptyMap(),
    val whyExplanation: String
)

data class ArdenSolution(
    val dfa: DFA,
    val initialEquations: Map<String, String>,
    val steps: List<SolverStep>,
    val finalRegex: RegexNode,
    val finalRegexString: String,
    val ardenApplicationsCount: Int,
    val substitutionsCount: Int
)

data class SimulationStep(
    val stepIndex: Int,
    val currentState: String,
    val symbol: String?,
    val nextState: String?,
    val remainingString: String,
    val isFinalStep: Boolean,
    val isAccepted: Boolean
)

data class VerificationItem(
    val testString: String,
    val dfaAccepted: Boolean,
    val regexAccepted: Boolean,
    val passed: Boolean
)
