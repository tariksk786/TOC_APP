package com.example.solver

import com.example.model.DFA
import com.example.model.ValidationResult

object DFAValidator {

    fun validate(dfa: DFA): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (dfa.states.isEmpty()) {
            errors.add("No states defined. At least one state is required.")
            return ValidationResult(isValid = false, errors = errors)
        }

        val stateIds = dfa.states.map { it.id }.toSet()

        // Check unique state names
        if (stateIds.size != dfa.states.size) {
            errors.add("Duplicate state IDs found. Each state must have a unique identifier.")
        }

        // Start state check
        val startStates = dfa.states.filter { it.isStart }
        if (startStates.isEmpty()) {
            errors.add("No initial state selected. Exactly one start state is required.")
        } else if (startStates.size > 1) {
            errors.add("Multiple initial states (${startStates.joinToString { it.id }}) defined. A DFA must have exactly one start state.")
        }

        // Final states check
        val finalStates = dfa.finalStates
        if (finalStates.isEmpty()) {
            errors.add("No accepting / final state selected. At least one final state is required.")
        }

        // Transitions validation
        val seenTransitions = mutableSetOf<Pair<String, String>>()
        for (t in dfa.transitions) {
            if (!stateIds.contains(t.from)) {
                errors.add("Transition ${t.from} --${t.symbol}--> ${t.to} is invalid because source state '${t.from}' does not exist.")
            }
            if (!stateIds.contains(t.to)) {
                errors.add("Transition ${t.from} --${t.symbol}--> ${t.to} is invalid because target state '${t.to}' does not exist.")
            }
            if (t.symbol.isBlank()) {
                errors.add("Transition from '${t.from}' to '${t.to}' has an empty or blank symbol.")
            }

            // Check determinism: no multiple transitions on same (from, symbol)
            val key = Pair(t.from, t.symbol)
            if (seenTransitions.contains(key)) {
                errors.add("Non-deterministic transition detected: State '${t.from}' has multiple transitions for symbol '${t.symbol}'.")
            } else {
                seenTransitions.add(key)
            }
        }

        // Check if accepting state is reachable from start state
        val startId = dfa.startState?.id
        if (startId != null && errors.isEmpty()) {
            val visited = mutableSetOf<String>()
            val queue = ArrayDeque<String>()
            queue.add(startId)
            visited.add(startId)

            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                val nexts = dfa.transitions.filter { it.from == current }.map { it.to }
                for (next in nexts) {
                    if (visited.add(next)) {
                        queue.add(next)
                    }
                }
            }

            val unreachableFinals = finalStates.filter { !visited.contains(it.id) }
            if (unreachableFinals.size == finalStates.size) {
                warnings.add("Warning: No accepting state is reachable from the start state '${startId}'. Language accepted will be empty (∅).")
            }
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
            message = if (errors.isEmpty()) "✓ DFA Validated Successfully" else "DFA Validation Failed"
        )
    }
}
