package com.example.regex

/**
 * Handles professional academic formatting of Regular Expressions and Language Equations.
 * Preserves correct parentheses, operator precedence, and mathematical symbols (ε, ∅, *).
 */
object RegexFormatter {

    fun format(node: RegexNode): String {
        return node.toDisplayString()
    }

    fun formatEquation(variable: String, rhs: RegexNode): String {
        return "$variable = ${rhs.toDisplayString()}"
    }

    fun formatSystem(equations: Map<String, RegexNode>): String {
        return equations.entries.joinToString("\n") { (v, expr) ->
            "$v = ${expr.toDisplayString()}"
        }
    }

    /**
     * Maps DFA state name (e.g. "q0", "q1", "A") to standard textbook language variable ("R0", "R1", "RA").
     */
    fun stateToVariable(stateId: String): String {
        val clean = stateId.trim()
        return if (clean.startsWith("q", ignoreCase = true) && clean.length > 1 && clean.substring(1).all { it.isDigit() }) {
            "R" + clean.substring(1) // q0 -> R0, q1 -> R1
        } else if (clean.all { it.isLetterOrDigit() }) {
            "R" + clean // A -> RA, B -> RB
        } else {
            "R_" + clean
        }
    }
}
