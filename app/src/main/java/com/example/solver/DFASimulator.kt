package com.example.solver

import com.example.model.DFA
import com.example.model.SimulationStep

object DFASimulator {

    fun trace(dfa: DFA, input: String): List<SimulationStep> {
        val steps = mutableListOf<SimulationStep>()
        val startId = dfa.startState?.id ?: return emptyList()

        var currentState = startId
        var remaining = input

        // Initial step
        steps.add(
            SimulationStep(
                stepIndex = 0,
                currentState = currentState,
                symbol = null,
                nextState = null,
                remainingString = remaining,
                isFinalStep = input.isEmpty(),
                isAccepted = dfa.finalStates.any { it.id == currentState }
            )
        )

        for (i in input.indices) {
            val symbol = input[i].toString()
            val nextState = dfa.transitions.firstOrNull { it.from == currentState && it.symbol == symbol }?.to
            remaining = input.substring(i + 1)

            if (nextState == null) {
                // Trap / rejected transition
                steps.add(
                    SimulationStep(
                        stepIndex = i + 1,
                        currentState = currentState,
                        symbol = symbol,
                        nextState = "TRAP (No Transition)",
                        remainingString = remaining,
                        isFinalStep = true,
                        isAccepted = false
                    )
                )
                return steps
            }

            currentState = nextState
            val isFinal = i == input.length - 1
            val isAccepted = isFinal && dfa.finalStates.any { it.id == currentState }

            steps.add(
                SimulationStep(
                    stepIndex = i + 1,
                    currentState = currentState,
                    symbol = symbol,
                    nextState = nextState,
                    remainingString = remaining,
                    isFinalStep = isFinal,
                    isAccepted = isAccepted
                )
            )
        }

        return steps
    }
}
