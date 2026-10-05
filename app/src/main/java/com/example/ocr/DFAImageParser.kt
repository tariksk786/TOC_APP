package com.example.ocr

import com.example.model.DFA
import com.example.model.DFAState
import com.example.model.Transition

enum class ParseStatus {
    DETECTED,
    PARTIAL,
    REJECTED
}

data class DFAParseResult(
    val status: ParseStatus,
    val dfa: DFA?,
    val confidence: Float, // 0.0 to 1.0
    val rawText: String,
    val detectedStates: List<String>,
    val detectedAlphabet: List<String>,
    val detectedStart: String?,
    val detectedFinals: List<String>,
    val detectedTransitions: List<Transition>,
    val message: String
)

object DFAImageParser {

    private val STATE_TOKEN_REGEX = Regex("\\b[qQsSpP]\\d+\\b|\\b[A-E]\\b")

    fun parse(rawText: String): DFAParseResult {
        val trimmed = rawText.trim()
        if (trimmed.length < 3) {
            return reject(rawText, "Image contains insufficient text.")
        }

        val lines = trimmed.lines().map { it.trim() }.filter { it.isNotBlank() }

        val foundStates = mutableSetOf<String>()
        val foundAlphabet = mutableSetOf<String>()
        var foundStart: String? = null
        val foundFinals = mutableSetOf<String>()
        val foundTransitions = mutableListOf<Transition>()

        var tableHeaderSymbols: List<String>? = null

        for (line in lines) {
            val lower = line.lowercase()

            // Skip comment/header lines like "Transition Table:"
            if (lower == "transition table:" || lower == "transition table" || lower == "dfa:" || lower == "automata:") {
                continue
            }

            // 1. Explicit States: States: q0, q1, q2 or Q = {q0, q1}
            if ((lower.startsWith("state:") || lower.startsWith("states:") || lower.startsWith("states =") || lower.startsWith("states=") || lower.startsWith("q =") || lower.startsWith("q:") || lower.startsWith("q="))) {
                val valuePart = line.substringAfter(":").substringAfter("=").replace("{", "").replace("}", "")
                val statesInLine = valuePart.split("[,;\\s]+".toRegex()).filter { it.isNotBlank() }
                for (s in statesInLine) {
                    val clean = s.trim().removePrefix("*").removePrefix("->").removePrefix(">")
                    if (clean.isNotBlank()) foundStates.add(clean)
                }
                continue
            }

            // 2. Explicit Alphabet: Alphabet: a, b or Sigma = {0, 1}
            if ((lower.startsWith("alphabet") || lower.startsWith("sigma") || lower.startsWith("symbols") || lower.startsWith("inputs") || lower.startsWith("σ") || lower.startsWith("∑")) && (lower.contains(":") || lower.contains("="))) {
                val valuePart = line.substringAfter(":").substringAfter("=").replace("{", "").replace("}", "")
                val symsInLine = valuePart.split("[,;\\s]+".toRegex()).filter { it.isNotBlank() }
                for (sym in symsInLine) {
                    val clean = sym.trim()
                    if (clean.length == 1 && clean.all { it.isLetterOrDigit() }) {
                        foundAlphabet.add(clean)
                    }
                }
                continue
            }

            // 3. Explicit Start State: Start: q0 or Initial: q0
            if ((lower.startsWith("start") || lower.startsWith("initial")) && (lower.contains(":") || lower.contains("="))) {
                val cleanVal = line.substringAfter(":").substringAfter("=").trim().split("[,;\\s]+".toRegex()).firstOrNull() ?: ""
                val clean = cleanVal.removePrefix("->").removePrefix(">").trim()
                if (clean.isNotBlank()) {
                    foundStart = clean
                    foundStates.add(clean)
                }
                continue
            }

            // 4. Explicit Final States: Final: q2 or Accepting: {q1, q2} or F = {q2}
            if ((lower.startsWith("final") || lower.startsWith("accepting") || lower.startsWith("f =") || lower.startsWith("f:") || lower.startsWith("f=")) && (lower.contains(":") || lower.contains("="))) {
                val valuePart = line.substringAfter(":").substringAfter("=").replace("{", "").replace("}", "")
                val finalsInLine = valuePart.split("[,;\\s]+".toRegex()).filter { it.isNotBlank() }
                for (f in finalsInLine) {
                    val clean = f.trim().removePrefix("*")
                    if (clean.isNotBlank()) {
                        foundFinals.add(clean)
                        foundStates.add(clean)
                    }
                }
                continue
            }

            // 5. Detect Transition Table header (e.g. State | a | b or State 0 1)
            // Header must not contain '=' or '->' or ':'
            if (tableHeaderSymbols == null && !line.contains("=") && !line.contains("->") && !line.contains(":") && (line.contains("|") || lower.startsWith("state "))) {
                val tokens = line.split("[|\\s,]+".toRegex()).filter { it.isNotBlank() }
                val symbols = tokens.filter { it.length == 1 && (it[0].isLetter() || it[0].isDigit()) && it.lowercase() != "q" }
                if (symbols.size >= 1 && tokens.any { it.equals("state", ignoreCase = true) || it.equals("q", ignoreCase = true) || line.contains("|") }) {
                    tableHeaderSymbols = symbols
                    foundAlphabet.addAll(symbols)
                    continue
                }
            }

            // 6. Transition row parsing (e.g. Table row: ->q0 | q1 | q0 or *q1 | q1 | q0)
            if (tableHeaderSymbols != null && !line.contains("=") && !line.contains(":") && (line.contains("|") || line.contains("->") || line.contains("*") || tableHeaderSymbols.isNotEmpty())) {
                val tokens = line.split("[|\\s,]+".toRegex()).filter { it.isNotBlank() }
                if (tokens.size >= 2) {
                    var rawState = tokens[0]
                    var isStart = false
                    var isFinal = false
                    if (rawState.contains("->") || rawState.contains(">")) {
                        isStart = true
                        rawState = rawState.replace("->", "").replace(">", "").trim()
                    }
                    if (rawState.contains("*")) {
                        isFinal = true
                        rawState = rawState.replace("*", "").trim()
                    }

                    if (rawState.isNotBlank() && (STATE_TOKEN_REGEX.matches(rawState) || rawState.length <= 4)) {
                        foundStates.add(rawState)
                        if (isStart && foundStart == null) foundStart = rawState
                        if (isFinal) foundFinals.add(rawState)

                        val targetTokens = tokens.drop(1)
                        for (idx in targetTokens.indices) {
                            if (idx < tableHeaderSymbols.size) {
                                val sym = tableHeaderSymbols[idx]
                                val target = targetTokens[idx].trim().removePrefix("*")
                                if (target.isNotBlank() && target != "-" && target != "none" && target != "∅") {
                                    foundStates.add(target)
                                    foundTransitions.add(Transition(rawState, sym, target))
                                }
                            }
                        }
                        continue
                    }
                }
            }

            // 7. Parse δ(q0, a) = q1 or delta(q0, a) = q1 or d(q0, a) = q1
            val deltaMatch = Regex("""(?:[δΔ]|delta|\bd\b)\s*\(\s*([a-zA-Z0-9_]+)\s*,\s*([a-zA-Z0-9])\s*\)\s*=\s*([a-zA-Z0-9_]+)""", RegexOption.IGNORE_CASE).find(line)
            if (deltaMatch != null) {
                val from = deltaMatch.groupValues[1].trim()
                val sym = deltaMatch.groupValues[2].trim()
                val to = deltaMatch.groupValues[3].trim()
                foundStates.add(from)
                foundStates.add(to)
                foundAlphabet.add(sym)
                foundTransitions.add(Transition(from, sym, to))
                continue
            }

            // 8. Parse standard transition: q0, a = q1 or q0,a=q1
            val commaEqualMatch = Regex("""\b([a-zA-Z0-9_]+)\s*,\s*([a-zA-Z0-9])\s*=\s*([a-zA-Z0-9_]+)\b""").find(line)
            if (commaEqualMatch != null) {
                val from = commaEqualMatch.groupValues[1].trim()
                val sym = commaEqualMatch.groupValues[2].trim()
                val to = commaEqualMatch.groupValues[3].trim()
                foundStates.add(from)
                foundStates.add(to)
                foundAlphabet.add(sym)
                foundTransitions.add(Transition(from, sym, to))
                continue
            }

            // 9. Parse arrow notation: q0 --a--> q1 or q0 -a-> q1 or q0 -> a -> q1
            val arrowMatch1 = Regex("""\b([a-zA-Z0-9_]+)\s*--?\s*([a-zA-Z0-9])\s*--?>\s*([a-zA-Z0-9_]+)\b""").find(line)
            if (arrowMatch1 != null) {
                val from = arrowMatch1.groupValues[1].trim()
                val sym = arrowMatch1.groupValues[2].trim()
                val to = arrowMatch1.groupValues[3].trim()
                foundStates.add(from)
                foundStates.add(to)
                foundAlphabet.add(sym)
                foundTransitions.add(Transition(from, sym, to))
                continue
            }

            val arrowMatch2 = Regex("""\b([a-zA-Z0-9_]+)\s*->\s*([a-zA-Z0-9_]+)\s+(?:on|with|label=)\s*([a-zA-Z0-9])\b""").find(line)
            if (arrowMatch2 != null) {
                val from = arrowMatch2.groupValues[1].trim()
                val to = arrowMatch2.groupValues[2].trim()
                val sym = arrowMatch2.groupValues[3].trim()
                foundStates.add(from)
                foundStates.add(to)
                foundAlphabet.add(sym)
                foundTransitions.add(Transition(from, sym, to))
                continue
            }

            // 10. Check for lone state markers like *q1 or ->q0
            if (line.startsWith("->") || line.startsWith(">")) {
                val s = line.removePrefix("->").removePrefix(">").trim()
                if (s.isNotBlank() && STATE_TOKEN_REGEX.matches(s)) {
                    foundStart = s
                    foundStates.add(s)
                }
            } else if (line.startsWith("*")) {
                val s = line.removePrefix("*").trim()
                if (s.isNotBlank() && STATE_TOKEN_REGEX.matches(s)) {
                    foundFinals.add(s)
                    foundStates.add(s)
                }
            }
        }

        // Deduplicate transitions
        val distinctTransitions = foundTransitions.distinctBy { "${it.from},${it.symbol}" }

        val hasStates = foundStates.isNotEmpty()
        val hasAlphabet = foundAlphabet.isNotEmpty()
        val hasTransitions = distinctTransitions.isNotEmpty()
        val hasStart = foundStart != null
        val hasFinals = foundFinals.isNotEmpty()

        // Structural confidence calculation
        var confidenceScore = 0.0f
        if (hasStates) confidenceScore += 0.25f
        if (hasAlphabet) confidenceScore += 0.25f
        if (hasTransitions) confidenceScore += 0.20f
        if (hasStart) confidenceScore += 0.15f
        if (hasFinals) confidenceScore += 0.15f

        // Boost confidence if transitions match states and alphabet
        if (hasTransitions && hasStates && hasAlphabet) {
            val validTransCount = distinctTransitions.count {
                it.from in foundStates && it.to in foundStates && it.symbol in foundAlphabet
            }
            val matchRatio = validTransCount.toFloat() / distinctTransitions.size.toFloat()
            if (matchRatio >= 0.8f) {
                confidenceScore = (confidenceScore * 1.1f).coerceAtMost(0.98f)
            }
        }

        // Case A: REJECTED if no states or transitions found (selfie, nature, blank, unrelated text)
        if (!hasStates && !hasTransitions) {
            return reject(rawText, "We could not detect a valid DFA in this image.\nPlease upload a DFA diagram, transition table, or automata question.")
        }

        // Auto-assign start state if only 1 state exists or default to first state if partial
        val statesList = foundStates.toList().sorted()
        val alphabetList = foundAlphabet.toList().sorted()
        val finalStart = foundStart ?: statesList.firstOrNull()
        val finalFinals = foundFinals.toList()

        val dfaStates = statesList.map { id ->
            DFAState(
                id = id,
                isStart = (id == finalStart),
                isFinal = (id in finalFinals)
            )
        }

        val candidateDfa = DFA(
            states = dfaStates,
            alphabet = alphabetList,
            transitions = distinctTransitions
        )

        // Case B: DETECTED (High confidence: states, alphabet, start, final, and transitions all present)
        if (hasStates && hasAlphabet && hasTransitions && hasStart && hasFinals && confidenceScore >= 0.70f) {
            return DFAParseResult(
                status = ParseStatus.DETECTED,
                dfa = candidateDfa,
                confidence = confidenceScore,
                rawText = rawText,
                detectedStates = statesList,
                detectedAlphabet = alphabetList,
                detectedStart = finalStart,
                detectedFinals = finalFinals,
                detectedTransitions = distinctTransitions,
                message = "DFA successfully detected from image."
            )
        }

        // Case C: PARTIAL (States or transitions detected, but missing some properties)
        if (hasStates || hasTransitions) {
            return DFAParseResult(
                status = ParseStatus.PARTIAL,
                dfa = candidateDfa,
                confidence = confidenceScore.coerceIn(0.35f, 0.69f),
                rawText = rawText,
                detectedStates = statesList,
                detectedAlphabet = alphabetList,
                detectedStart = finalStart,
                detectedFinals = finalFinals,
                detectedTransitions = distinctTransitions,
                message = "Partial DFA detected. Please verify manually."
            )
        }

        // Otherwise: REJECTED
        return reject(rawText, "We could not detect a valid DFA in this image.\nPlease upload a DFA diagram, transition table, or automata question.")
    }

    private fun reject(rawText: String, msg: String): DFAParseResult {
        return DFAParseResult(
            status = ParseStatus.REJECTED,
            dfa = null,
            confidence = 0.0f,
            rawText = rawText,
            detectedStates = emptyList(),
            detectedAlphabet = emptyList(),
            detectedStart = null,
            detectedFinals = emptyList(),
            detectedTransitions = emptyList(),
            message = msg
        )
    }
}
