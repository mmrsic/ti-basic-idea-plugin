package com.github.mmrsic.idea.plugins.tibasic.language.analysis

import com.github.mmrsic.idea.plugins.tibasic.common.ext.nonWhitespaceChildren
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.parser.TiBasicNodeTypes
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicExpression
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicVariableAccess
import com.github.mmrsic.idea.plugins.tibasic.language.values.parseTiBasicDecimalLiteral
import com.intellij.lang.ASTNode
import java.math.BigDecimal
import java.math.MathContext

private val DECIMAL_EXPRESSION_MATH_CONTEXT = MathContext.DECIMAL64
private val DECIMAL_ADDITIVE_OPERATOR_TYPES = setOf(
    TiBasicTokenTypes.PLUS_OP,
    TiBasicTokenTypes.MINUS_OP,
)
private val DECIMAL_MULTIPLICATIVE_OPERATOR_TYPES = setOf(
    TiBasicTokenTypes.MUL_OP,
    TiBasicTokenTypes.DIV_OP,
)
private val DECIMAL_UNSUPPORTED_OPERATOR_TYPES = setOf(
    TiBasicTokenTypes.POW_OP,
)
private val DECIMAL_UNARY_OPERATOR_TYPES = setOf(
    TiBasicTokenTypes.PLUS_OP,
    TiBasicTokenTypes.MINUS_OP,
)

fun resolveDecimalExpressionValue(
    expression: TiBasicExpression?,
    resolveVariableValue: (TiBasicVariableAccess) -> BigDecimal?,
): BigDecimal? =
    resolveDecimalExpressionNodes(expression?.node?.nonWhitespaceChildren.orEmpty(), resolveVariableValue)

internal fun resolveDecimalExpressionNodes(
    nodes: List<ASTNode>,
    resolveVariableValue: (TiBasicVariableAccess) -> BigDecimal?,
): BigDecimal? {
    if (nodes.isEmpty()) return null
    if (isFullyParenthesized(nodes)) {
        return resolveDecimalExpressionNodes(nodes.subList(1, nodes.lastIndex), resolveVariableValue)
    }
    lastTopLevelBinaryOperatorIndex(nodes, DECIMAL_ADDITIVE_OPERATOR_TYPES)?.let { operatorIndex ->
        val left = resolveDecimalExpressionNodes(nodes.subList(0, operatorIndex), resolveVariableValue) ?: return null
        val right = resolveDecimalExpressionNodes(nodes.subList(operatorIndex + 1, nodes.size), resolveVariableValue) ?: return null
        return when (nodes[operatorIndex].elementType) {
            TiBasicTokenTypes.PLUS_OP -> left + right
            TiBasicTokenTypes.MINUS_OP -> left - right
            else -> null
        }
    }
    lastTopLevelBinaryOperatorIndex(nodes, DECIMAL_MULTIPLICATIVE_OPERATOR_TYPES)?.let { operatorIndex ->
        val left = resolveDecimalExpressionNodes(nodes.subList(0, operatorIndex), resolveVariableValue) ?: return null
        val right = resolveDecimalExpressionNodes(nodes.subList(operatorIndex + 1, nodes.size), resolveVariableValue) ?: return null
        return when (nodes[operatorIndex].elementType) {
            TiBasicTokenTypes.MUL_OP -> left.multiply(right, DECIMAL_EXPRESSION_MATH_CONTEXT)
            TiBasicTokenTypes.DIV_OP ->
                if (right.compareTo(BigDecimal.ZERO) == 0) {
                    null
                } else {
                    left.divide(right, DECIMAL_EXPRESSION_MATH_CONTEXT)
                }

            else -> null
        }
    }
    if (lastTopLevelBinaryOperatorIndex(nodes, DECIMAL_UNSUPPORTED_OPERATOR_TYPES) != null) return null
    return resolveSignedDecimalValue(nodes, resolveVariableValue)
}

private fun resolveSignedDecimalValue(
    nodes: List<ASTNode>,
    resolveVariableValue: (TiBasicVariableAccess) -> BigDecimal?,
): BigDecimal? {
    if (nodes.isEmpty()) return null
    var sign = BigDecimal.ONE
    var index = 0
    while (index < nodes.size && nodes[index].elementType in DECIMAL_UNARY_OPERATOR_TYPES) {
        if (nodes[index].elementType == TiBasicTokenTypes.MINUS_OP) {
            sign = sign.negate()
        }
        index++
    }
    if (index > 0) {
        val resolvedValue = resolveDecimalExpressionNodes(nodes.subList(index, nodes.size), resolveVariableValue) ?: return null
        return sign.multiply(resolvedValue)
    }
    if (nodes.size != 1) return null
    return when (nodes.single().elementType) {
        TiBasicTokenTypes.NUMERIC_LITERAL -> parseTiBasicDecimalLiteral(nodes.single().text)
        TiBasicNodeTypes.VARIABLE_ACCESS -> {
            val variableAccess = nodes.single().psi as? TiBasicVariableAccess
            if (variableAccess == null || variableAccess.hasSubscriptParens()) {
                null
            } else {
                resolveVariableValue(variableAccess)
            }
        }

        else -> null
    }
}
