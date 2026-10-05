package com.example.solver

import com.example.regex.RegexNode
import com.example.regex.RegexSimplifier

data class SubstitutionResult(
    val targetVariable: String,
    val substitutedVariable: String,
    val equationBefore: String,
    val replacementExpression: String,
    val equationAfter: String,
    val resultAST: RegexNode
)

object SubstitutionEngine {

    /**
     * Performs real symbolic substitution of [varName] = [replacement] into [targetExpr].
     * Returns the updated AST and formatted before/after representations.
     */
    fun substitute(
        targetVar: String,
        targetExpr: RegexNode,
        varName: String,
        replacement: RegexNode
    ): SubstitutionResult {
        val beforeStr = "$targetVar = ${targetExpr.toDisplayString()}"
        val replacedNode = targetExpr.substitute(varName, replacement)
        val simplifiedNode = RegexSimplifier.simplify(replacedNode)
        val afterStr = "$targetVar = ${simplifiedNode.toDisplayString()}"

        return SubstitutionResult(
            targetVariable = targetVar,
            substitutedVariable = varName,
            equationBefore = beforeStr,
            replacementExpression = "$varName = ${replacement.toDisplayString()}",
            equationAfter = afterStr,
            resultAST = simplifiedNode
        )
    }
}
