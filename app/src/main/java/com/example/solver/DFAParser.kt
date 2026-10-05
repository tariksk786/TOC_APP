package com.example.solver

import com.example.model.DFA
import com.example.model.DFAState
import com.example.model.Transition

object DFAParser {

    fun parse(text: String): DFA {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

        var stateIds = mutableListOf<String>()
        var alphabet = mutableListOf<String>()
        var startStateId = ""
        val finalStateIds = mutableSetOf<String>()
        val transitions = mutableListOf<Transition>()

        val transitionLineRegex1 = Regex("""^([A-Za-z0-9_]+)\s*,\s*([A-Za-z0-9_ε])\s*=\s*([A-Za-z0-9_]+)$""")
        val transitionLineRegex2 = Regex("""^([A-Za-z0-9_]+)\s*--?\s*([A-Za-z0-9_ε])\s*-->?\s*([A-Za-z0-9_]+)$""")
        val transitionLineRegex3 = Regex("""^δ?\s*\(\s*([A-Za-z0-9_]+)\s*,\s*([A-Za-z0-9_ε])\s*\)\s*=\s*([A-Za-z0-9_]+)$""")
        val transitionLineRegex4 = Regex("""^([A-Za-z0-9_]+)\s*,\s*([A-Za-z0-9_ε])\s*->\s*([A-Za-z0-9_]+)$""")

        for (line in lines) {
            val lower = line.lowercase()
            when {
                lower.startsWith("states:") || lower.startsWith("state:") -> {
                    val content = line.substringAfter(":").trim()
                    stateIds = content.split(",", " ", ";").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
                }
                lower.startsWith("alphabet:") || lower.startsWith("symbols:") || lower.startsWith("sigma:") -> {
                    val content = line.substringAfter(":").trim()
                    alphabet = content.split(",", " ", ";").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
                }
                lower.startsWith("start:") || lower.startsWith("initial:") || lower.startsWith("start state:") -> {
                    startStateId = line.substringAfter(":").trim().split(",", " ", ";").firstOrNull { it.isNotEmpty() } ?: ""
                }
                lower.startsWith("final:") || lower.startsWith("accepting:") || lower.startsWith("final states:") || lower.startsWith("accepting states:") -> {
                    val content = line.substringAfter(":").trim()
                    val finals = content.split(",", " ", ";").map { it.trim() }.filter { it.isNotEmpty() }
                    finalStateIds.addAll(finals)
                }
                else -> {
                    // Try parsing transition line
                    val match1 = transitionLineRegex1.matchEntire(line)
                    val match2 = transitionLineRegex2.matchEntire(line)
                    val match3 = transitionLineRegex3.matchEntire(line)
                    val match4 = transitionLineRegex4.matchEntire(line)
                    val match = match1 ?: match2 ?: match3 ?: match4

                    if (match != null) {
                        val from = match.groupValues[1]
                        val symbol = match.groupValues[2]
                        val to = match.groupValues[3]
                        transitions.add(Transition(from = from, symbol = symbol, to = to))
                        if (!stateIds.contains(from)) stateIds.add(from)
                        if (!stateIds.contains(to)) stateIds.add(to)
                        if (!alphabet.contains(symbol) && symbol != "ε") alphabet.add(symbol)
                    } else if (line.contains("|")) {
                        // Table row: q0 | a | q1
                        val parts = line.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                        if (parts.size == 3 && !parts[0].equals("from", ignoreCase = true)) {
                            val from = parts[0]
                            val symbol = parts[1]
                            val to = parts[2]
                            transitions.add(Transition(from = from, symbol = symbol, to = to))
                            if (!stateIds.contains(from)) stateIds.add(from)
                            if (!stateIds.contains(to)) stateIds.add(to)
                            if (!alphabet.contains(symbol) && symbol != "ε") alphabet.add(symbol)
                        }
                    }
                }
            }
        }

        if (startStateId.isEmpty() && stateIds.isNotEmpty()) {
            startStateId = stateIds.first()
        }

        // Deduplicate states while preserving order
        val distinctStateIds = stateIds.distinct()
        val dfaStates = distinctStateIds.map { id ->
            DFAState(
                id = id,
                isStart = id == startStateId,
                isFinal = finalStateIds.contains(id)
            )
        }

        return DFA(
            states = dfaStates,
            alphabet = alphabet.distinct(),
            transitions = transitions
        )
    }

    fun toCompactString(dfa: DFA): String {
        val sb = StringBuilder()
        sb.appendLine("States: ${dfa.states.joinToString(", ") { it.id }}")
        sb.appendLine("Alphabet: ${dfa.alphabet.joinToString(", ")}")
        sb.appendLine("Start: ${dfa.startState?.id ?: ""}")
        sb.appendLine("Final: ${dfa.finalStates.joinToString(", ") { it.id }}")
        sb.appendLine()
        for (t in dfa.transitions) {
            sb.appendLine("${t.from},${t.symbol}=${t.to}")
        }
        return sb.toString().trim()
    }
}
