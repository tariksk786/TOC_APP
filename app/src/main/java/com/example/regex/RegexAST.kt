package com.example.regex

/**
 * Sealed interface representing an Abstract Syntax Tree (AST) node for Regular Expressions.
 * Used for purely symbolic manipulation in Arden's Theorem solver.
 */
sealed interface RegexNode {
    val precedence: Int

    fun containsVariable(varName: String): Boolean

    fun substitute(varName: String, replacement: RegexNode): RegexNode

    fun simplify(): RegexNode

    fun toDisplayString(): String

    fun matchesString(input: String): Boolean
}

/**
 * Represents the empty set (∅) - language accepting no strings.
 */
data object EmptySet : RegexNode {
    override val precedence: Int = 4

    override fun containsVariable(varName: String): Boolean = false

    override fun substitute(varName: String, replacement: RegexNode): RegexNode = this

    override fun simplify(): RegexNode = this

    override fun toDisplayString(): String = "∅"

    override fun matchesString(input: String): Boolean = false

    override fun toString(): String = "∅"
}

/**
 * Represents epsilon (ε) - language accepting only the empty string.
 */
data object Epsilon : RegexNode {
    override val precedence: Int = 4

    override fun containsVariable(varName: String): Boolean = false

    override fun substitute(varName: String, replacement: RegexNode): RegexNode = this

    override fun simplify(): RegexNode = this

    override fun toDisplayString(): String = "ε"

    override fun matchesString(input: String): Boolean = input.isEmpty()

    override fun toString(): String = "ε"
}

/**
 * Represents an alphabet terminal symbol (e.g. 'a', 'b', '0', '1').
 */
data class Literal(val symbol: String) : RegexNode {
    override val precedence: Int = 4

    override fun containsVariable(varName: String): Boolean = false

    override fun substitute(varName: String, replacement: RegexNode): RegexNode = this

    override fun simplify(): RegexNode = this

    override fun toDisplayString(): String = symbol

    override fun matchesString(input: String): Boolean = input == symbol

    override fun toString(): String = symbol
}

/**
 * Represents a state equation variable (e.g. "R0", "R1", "Rq0").
 */
data class Variable(val name: String) : RegexNode {
    override val precedence: Int = 4

    override fun containsVariable(varName: String): Boolean = this.name == varName

    override fun substitute(varName: String, replacement: RegexNode): RegexNode {
        return if (this.name == varName) replacement else this
    }

    override fun simplify(): RegexNode = this

    override fun toDisplayString(): String = name

    override fun matchesString(input: String): Boolean = false

    override fun toString(): String = name
}

/**
 * Represents Kleene Star: (child)*
 */
data class Star(val child: RegexNode) : RegexNode {
    override val precedence: Int = 3

    override fun containsVariable(varName: String): Boolean = child.containsVariable(varName)

    override fun substitute(varName: String, replacement: RegexNode): RegexNode {
        return Star(child.substitute(varName, replacement)).simplify()
    }

    override fun simplify(): RegexNode {
        val sChild = child.simplify()
        return when (sChild) {
            is EmptySet -> Epsilon
            is Epsilon -> Epsilon
            is Star -> sChild // (A*)* = A*
            else -> Star(sChild)
        }
    }

    override fun toDisplayString(): String {
        val childStr = if (child.precedence < precedence || child is Union || child is Concat) {
            "(${child.toDisplayString()})"
        } else {
            child.toDisplayString()
        }
        return "$childStr*"
    }

    override fun matchesString(input: String): Boolean {
        if (input.isEmpty()) return true
        val n = input.length
        // Dynamic programming for Kleene star matching
        val dp = BooleanArray(n + 1)
        dp[0] = true
        for (i in 1..n) {
            for (j in 0 until i) {
                if (dp[j] && child.matchesString(input.substring(j, i))) {
                    dp[i] = true
                    break
                }
            }
        }
        return dp[n]
    }

    override fun toString(): String = toDisplayString()
}

/**
 * Represents Concatenation of factors: A · B
 */
data class Concat(val factors: List<RegexNode>) : RegexNode {
    override val precedence: Int = 2

    override fun containsVariable(varName: String): Boolean =
        factors.any { it.containsVariable(varName) }

    override fun substitute(varName: String, replacement: RegexNode): RegexNode {
        return Concat(factors.map { it.substitute(varName, replacement) }).simplify()
    }

    override fun simplify(): RegexNode {
        val flat = mutableListOf<RegexNode>()
        for (f in factors) {
            val sf = f.simplify()
            if (sf is Concat) {
                flat.addAll(sf.factors)
            } else {
                flat.add(sf)
            }
        }

        // Rule: If any factor is EmptySet (∅), whole concatenation is EmptySet
        if (flat.any { it is EmptySet }) return EmptySet

        // Rule: Remove Epsilon (ε) factors unless list becomes empty
        val filtered = flat.filter { it !is Epsilon }
        return when {
            filtered.isEmpty() -> Epsilon
            filtered.size == 1 -> filtered[0]
            else -> Concat(filtered)
        }
    }

    override fun toDisplayString(): String {
        return factors.joinToString("") { factor ->
            if (factor.precedence < precedence || factor is Union) {
                "(${factor.toDisplayString()})"
            } else {
                factor.toDisplayString()
            }
        }
    }

    override fun matchesString(input: String): Boolean {
        return matchHelper(factors, 0, input)
    }

    private fun matchHelper(list: List<RegexNode>, index: Int, remaining: String): Boolean {
        if (index == list.size) return remaining.isEmpty()
        val current = list[index]
        for (split in 0..remaining.length) {
            val prefix = remaining.substring(0, split)
            if (current.matchesString(prefix)) {
                val suffix = remaining.substring(split)
                if (matchHelper(list, index + 1, suffix)) {
                    return true
                }
            }
        }
        return false
    }

    override fun toString(): String = toDisplayString()
}

/**
 * Represents Union / Alternation of terms: A + B
 */
data class Union(val terms: List<RegexNode>) : RegexNode {
    override val precedence: Int = 1

    override fun containsVariable(varName: String): Boolean =
        terms.any { it.containsVariable(varName) }

    override fun substitute(varName: String, replacement: RegexNode): RegexNode {
        return Union(terms.map { it.substitute(varName, replacement) }).simplify()
    }

    override fun simplify(): RegexNode {
        val flat = mutableListOf<RegexNode>()
        for (t in terms) {
            val st = t.simplify()
            if (st is Union) {
                flat.addAll(st.terms)
            } else {
                flat.add(st)
            }
        }

        // Rule: Remove EmptySet (∅ + R = R)
        val nonNull = flat.filter { it !is EmptySet }
        if (nonNull.isEmpty()) return EmptySet

        // Rule: Deduplicate identical terms (R + R = R) preserving canonical order
        val distinct = mutableListOf<RegexNode>()
        for (item in nonNull) {
            if (distinct.none { it.toDisplayString() == item.toDisplayString() }) {
                distinct.add(item)
            }
        }

        return when {
            distinct.isEmpty() -> EmptySet
            distinct.size == 1 -> distinct[0]
            else -> Union(distinct)
        }
    }

    override fun toDisplayString(): String {
        return terms.joinToString(" + ") { term ->
            term.toDisplayString()
        }
    }

    override fun matchesString(input: String): Boolean {
        return terms.any { it.matchesString(input) }
    }

    override fun toString(): String = toDisplayString()
}
