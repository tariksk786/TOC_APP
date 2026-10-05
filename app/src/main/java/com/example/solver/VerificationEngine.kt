package com.example.solver

import com.example.model.DFA
import com.example.model.VerificationItem
import com.example.regex.RegexNode

object VerificationEngine {

    /**
     * Generates a systematic suite of test strings over the DFA's alphabet,
     * checks both the DFA deterministic transition simulation and the
     * derived Regular Expression symbolic matcher, and compares results.
     */
    fun verify(dfa: DFA, regex: RegexNode): List<VerificationItem> {
        val alphabet = dfa.alphabet.ifEmpty { listOf("a", "b") }
        val testStrings = generateTestStrings(alphabet, maxLen = 3)

        return testStrings.map { str ->
            val dfaResult = dfa.simulate(str)
            val regexResult = regex.matchesString(str)
            VerificationItem(
                testString = if (str.isEmpty()) "ε (empty string)" else str,
                dfaAccepted = dfaResult,
                regexAccepted = regexResult,
                passed = dfaResult == regexResult
            )
        }
    }

    private fun generateTestStrings(alphabet: List<String>, maxLen: Int): List<String> {
        val results = mutableListOf("") // empty string epsilon

        var currentLevel = listOf("")
        for (len in 1..maxLen) {
            val nextLevel = mutableListOf<String>()
            for (prefix in currentLevel) {
                for (sym in alphabet) {
                    val candidate = prefix + sym
                    nextLevel.add(candidate)
                    results.add(candidate)
                    if (results.size >= 16) break // keep test suite concise and representative
                }
                if (results.size >= 16) break
            }
            currentLevel = nextLevel
            if (results.size >= 16) break
        }

        return results
    }
}
