package com.example.regex

object RegexFactory {

    fun union(terms: List<RegexNode>): RegexNode {
        return Union(terms).simplify()
    }

    fun union(vararg terms: RegexNode): RegexNode {
        return Union(terms.toList()).simplify()
    }

    fun concat(factors: List<RegexNode>): RegexNode {
        return Concat(factors).simplify()
    }

    fun concat(vararg factors: RegexNode): RegexNode {
        return Concat(factors.toList()).simplify()
    }

    fun star(child: RegexNode): RegexNode {
        return Star(child).simplify()
    }

    /**
     * Expands products over unions: A(B + C) = AB + AC, (A + B)C = AC + BC
     */
    fun expand(node: RegexNode): RegexNode {
        return when (val s = node.simplify()) {
            is Concat -> {
                val unionIndex = s.factors.indexOfFirst { it is Union }
                if (unionIndex != -1) {
                    val unionNode = s.factors[unionIndex] as Union
                    val before = s.factors.subList(0, unionIndex)
                    val after = s.factors.subList(unionIndex + 1, s.factors.size)
                    val distributedTerms = unionNode.terms.map { term ->
                        expand(Concat(before + term + after))
                    }
                    Union(distributedTerms).simplify()
                } else {
                    Concat(s.factors.map { expand(it) }).simplify()
                }
            }
            is Union -> Union(s.terms.map { expand(it) }).simplify()
            else -> s
        }
    }

    /**
     * Splits [rhs] into (A, B) such that rhs = A * Var(varName) + B,
     * where Var(varName) does not appear in A or B.
     * If Var(varName) does not appear in rhs, returns null.
     */
    fun splitLinearForm(rhs: RegexNode, varName: String): Pair<RegexNode, RegexNode>? {
        if (!rhs.containsVariable(varName)) {
            return null
        }

        // Expand any concatenation over union to isolate linear variable terms
        val expanded = expand(rhs)

        // Collect all union terms
        val terms = when (expanded) {
            is Union -> expanded.terms
            else -> listOf(expanded)
        }

        val aTerms = mutableListOf<RegexNode>()
        val bTerms = mutableListOf<RegexNode>()

        for (term in terms) {
            if (!term.containsVariable(varName)) {
                bTerms.add(term)
            } else {
                // Extract coefficient of Var(varName)
                // In right-linear grammars, term is either Var(varName) (prefix is Epsilon)
                // or Concat(prefixFactors..., Var(varName))
                when (term) {
                    is Variable -> {
                        if (term.name == varName) {
                            aTerms.add(Epsilon)
                        } else {
                            bTerms.add(term)
                        }
                    }
                    is Concat -> {
                        val factors = term.factors
                        if (factors.lastOrNull() is Variable && (factors.last() as Variable).name == varName) {
                            val prefixFactors = factors.dropLast(1)
                            val prefix = if (prefixFactors.isEmpty()) Epsilon else concat(prefixFactors)
                            aTerms.add(prefix)
                        } else {
                            // If varName appears anywhere else in factors, extract prefix up to varName
                            // or fallback to substituting
                            var found = false
                            val prefixList = mutableListOf<RegexNode>()
                            for (f in factors) {
                                if (f is Variable && f.name == varName) {
                                    found = true
                                    break
                                } else {
                                    prefixList.add(f)
                                }
                            }
                            if (found) {
                                aTerms.add(if (prefixList.isEmpty()) Epsilon else concat(prefixList))
                            } else {
                                bTerms.add(term)
                            }
                        }
                    }
                    else -> {
                        // Fallback: If it's a complex term containing the variable, consider it with Epsilon prefix
                        aTerms.add(Epsilon)
                    }
                }
            }
        }

        val a = if (aTerms.isEmpty()) EmptySet else union(aTerms)
        val b = if (bTerms.isEmpty()) EmptySet else union(bTerms)
        return Pair(a, b)
    }
}
