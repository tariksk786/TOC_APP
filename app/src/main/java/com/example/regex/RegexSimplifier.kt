package com.example.regex

/**
 * Algebraic simplification engine for Regular Expressions AST.
 * Implements standard algebraic identities:
 *  - R + ∅ = R, ∅ + R = R
 *  - ε R = R, R ε = R
 *  - ∅ R = ∅, R ∅ = ∅
 *  - R + R = R (idempotence of union)
 *  - (∅)* = ε
 *  - (ε)* = ε
 *  - (R*)* = R*
 *  - (ε + R)* = R*
 *  - Strictly preserves language semantics L(R).
 */
object RegexSimplifier {

    fun simplify(node: RegexNode): RegexNode {
        return simplifyRec(node)
    }

    private fun simplifyRec(node: RegexNode): RegexNode {
        return when (node) {
            is EmptySet, is Epsilon, is Literal, is Variable -> node
            is Star -> simplifyStar(node)
            is Concat -> simplifyConcat(node)
            is Union -> simplifyUnion(node)
        }
    }

    private fun simplifyStar(star: Star): RegexNode {
        val simplifiedChild = simplifyRec(star.child)
        return when (simplifiedChild) {
            is EmptySet -> Epsilon       // (∅)* = ε
            is Epsilon -> Epsilon        // (ε)* = ε
            is Star -> simplifiedChild   // (R*)* = R*
            is Union -> {
                // If union contains Epsilon: (ε + R)* = R*
                val nonEpsilon = simplifiedChild.terms.filter { it !is Epsilon }
                if (nonEpsilon.isEmpty()) {
                    Epsilon
                } else if (nonEpsilon.size < simplifiedChild.terms.size) {
                    val newUnion = if (nonEpsilon.size == 1) nonEpsilon[0] else Union(nonEpsilon)
                    Star(simplifyRec(newUnion))
                } else {
                    Star(simplifiedChild)
                }
            }
            else -> Star(simplifiedChild)
        }
    }

    private fun simplifyConcat(concat: Concat): RegexNode {
        val flat = mutableListOf<RegexNode>()
        for (f in concat.factors) {
            val sf = simplifyRec(f)
            if (sf is Concat) {
                flat.addAll(sf.factors)
            } else {
                flat.add(sf)
            }
        }

        // Rule: ∅ R = ∅ and R ∅ = ∅ (Any EmptySet factor annihilates concatenation to ∅)
        if (flat.any { it is EmptySet }) {
            return EmptySet
        }

        // Rule: ε R = R and R ε = R (Identity element for concatenation)
        val nonEpsilon = flat.filter { it !is Epsilon }
        return when {
            nonEpsilon.isEmpty() -> Epsilon
            nonEpsilon.size == 1 -> nonEpsilon[0]
            else -> Concat(nonEpsilon)
        }
    }

    private fun simplifyUnion(union: Union): RegexNode {
        val flat = mutableListOf<RegexNode>()
        for (t in union.terms) {
            val st = simplifyRec(t)
            if (st is Union) {
                flat.addAll(st.terms)
            } else {
                flat.add(st)
            }
        }

        // Rule: R + ∅ = R and ∅ + R = R (Identity element for union)
        val nonNull = flat.filter { it !is EmptySet }
        if (nonNull.isEmpty()) return EmptySet

        // Rule: R + R = R (Idempotence of union)
        val distinct = mutableListOf<RegexNode>()
        for (item in nonNull) {
            val itemStr = item.toDisplayString()
            if (distinct.none { it.toDisplayString() == itemStr }) {
                distinct.add(item)
            }
        }

        return when {
            distinct.isEmpty() -> EmptySet
            distinct.size == 1 -> distinct[0]
            else -> Union(distinct)
        }
    }
}
