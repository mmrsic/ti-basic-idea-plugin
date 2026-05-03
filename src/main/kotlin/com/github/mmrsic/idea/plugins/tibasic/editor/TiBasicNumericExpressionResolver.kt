package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.ext.nonWhitespaceChildren
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes
import com.github.mmrsic.idea.plugins.tibasic.psi.expression.TiBasicExpression
import com.github.mmrsic.idea.plugins.tibasic.psi.expression.TiBasicVariableAccess
import com.intellij.lang.ASTNode
import com.intellij.psi.tree.IElementType

internal fun resolveNumericExpressionValue(
    expression: TiBasicExpression?,
    resolveVariableValue: (TiBasicVariableAccess) -> Int?,
): Int? =
    resolveNumericExpressionNodes(expression?.node?.nonWhitespaceChildren.orEmpty(), resolveVariableValue)

internal fun resolveNumericExpressionNodes(
    nodes: List<ASTNode>,
    resolveVariableValue: (TiBasicVariableAccess) -> Int?,
): Int? {
    if (nodes.isEmpty()) return null
    if (isFullyParenthesized(nodes)) {
        return resolveNumericExpressionNodes(nodes.subList(1, nodes.lastIndex), resolveVariableValue)
    }
    lastTopLevelBinaryOperatorIndex(nodes, ADDITIVE_OPERATOR_TYPES)?.let { operatorIndex ->
        val left = resolveNumericExpressionNodes(nodes.subList(0, operatorIndex), resolveVariableValue) ?: return null
        val right = resolveNumericExpressionNodes(nodes.subList(operatorIndex + 1, nodes.size), resolveVariableValue) ?: return null
        return when (nodes[operatorIndex].elementType) {
            TiBasicTokenTypes.PLUS_OP -> safeIntResult(left.toLong() + right.toLong())
            TiBasicTokenTypes.MINUS_OP -> safeIntResult(left.toLong() - right.toLong())
            else -> null
        }
    }
    lastTopLevelBinaryOperatorIndex(nodes, MULTIPLICATIVE_OPERATOR_TYPES)?.let { operatorIndex ->
        val left = resolveNumericExpressionNodes(nodes.subList(0, operatorIndex), resolveVariableValue) ?: return null
        val right = resolveNumericExpressionNodes(nodes.subList(operatorIndex + 1, nodes.size), resolveVariableValue) ?: return null
        return when (nodes[operatorIndex].elementType) {
            TiBasicTokenTypes.MUL_OP -> safeIntResult(left.toLong() * right.toLong())
            TiBasicTokenTypes.DIV_OP ->
                if (right != 0 && left % right == 0) {
                    left / right
                } else {
                    null
                }

            else -> null
        }
    }
    if (lastTopLevelBinaryOperatorIndex(nodes, UNSUPPORTED_NUMERIC_OPERATOR_TYPES) != null) return null
    return resolveSignedNumericValue(nodes, resolveVariableValue)
}

internal fun firstTopLevelBinaryOperatorIndex(children: List<ASTNode>, operatorTypes: Set<IElementType>): Int? {
    var nestingDepth = 0
    var expectsOperand = true
    children.forEachIndexed { index, child ->
        when (child.elementType) {
            TiBasicTokenTypes.LPAREN -> nestingDepth++
            TiBasicTokenTypes.RPAREN -> {
                nestingDepth--
                if (nestingDepth == 0) {
                    expectsOperand = false
                }
            }

            else -> if (nestingDepth == 0) {
                when {
                    child.isExpressionOperand() -> expectsOperand = false
                    child.elementType in UNARY_EXPRESSION_OPERATOR_TYPES && expectsOperand -> Unit
                    child.elementType in operatorTypes && !expectsOperand -> return index
                }
            }
        }
    }
    return null
}

internal fun lastTopLevelBinaryOperatorIndex(children: List<ASTNode>, operatorTypes: Set<IElementType>): Int? {
    var nestingDepth = 0
    var expectsOperand = true
    var lastMatch: Int? = null
    children.forEachIndexed { index, child ->
        when (child.elementType) {
            TiBasicTokenTypes.LPAREN -> nestingDepth++
            TiBasicTokenTypes.RPAREN -> {
                nestingDepth--
                if (nestingDepth == 0) {
                    expectsOperand = false
                }
            }

            else -> if (nestingDepth == 0) {
                when {
                    child.isExpressionOperand() -> expectsOperand = false
                    child.elementType in UNARY_EXPRESSION_OPERATOR_TYPES && expectsOperand -> Unit
                    child.elementType in operatorTypes && !expectsOperand -> {
                        lastMatch = index
                        expectsOperand = true
                    }
                }
            }
        }
    }
    return lastMatch
}

internal fun isFullyParenthesized(children: List<ASTNode>): Boolean {
    if (children.size < 2) return false
    if (children.first().elementType != TiBasicTokenTypes.LPAREN || children.last().elementType != TiBasicTokenTypes.RPAREN) {
        return false
    }
    var nestingDepth = 0
    children.forEachIndexed { index, child ->
        when (child.elementType) {
            TiBasicTokenTypes.LPAREN -> nestingDepth++
            TiBasicTokenTypes.RPAREN -> nestingDepth--
        }
        if (nestingDepth == 0 && index < children.lastIndex) {
            return false
        }
    }
    return nestingDepth == 0
}

private fun resolveSignedNumericValue(
    nodes: List<ASTNode>,
    resolveVariableValue: (TiBasicVariableAccess) -> Int?,
): Int? {
    if (nodes.isEmpty()) return null
    var sign = 1
    var index = 0
    while (index < nodes.size && nodes[index].elementType in UNARY_EXPRESSION_OPERATOR_TYPES) {
        if (nodes[index].elementType == TiBasicTokenTypes.MINUS_OP) {
            sign *= -1
        }
        index++
    }
    if (index > 0) {
        val resolvedValue = resolveNumericExpressionNodes(nodes.subList(index, nodes.size), resolveVariableValue) ?: return null
        return safeIntResult(sign.toLong() * resolvedValue.toLong())
    }
    if (nodes.size != 1) return null
    return when (nodes.single().elementType) {
        TiBasicTokenTypes.NUMERIC_LITERAL -> nodes.single().text.toIntOrNull()
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

private fun safeIntResult(value: Long): Int? =
    value.toInt()
        .takeIf { it.toLong() == value }

private val ADDITIVE_OPERATOR_TYPES = setOf(
    TiBasicTokenTypes.PLUS_OP,
    TiBasicTokenTypes.MINUS_OP,
)

private val MULTIPLICATIVE_OPERATOR_TYPES = setOf(
    TiBasicTokenTypes.MUL_OP,
    TiBasicTokenTypes.DIV_OP,
)

private val UNSUPPORTED_NUMERIC_OPERATOR_TYPES = setOf(
    TiBasicTokenTypes.POW_OP,
)

internal val ARITHMETIC_OPERATOR_TYPES = setOf(
    TiBasicTokenTypes.PLUS_OP,
    TiBasicTokenTypes.MINUS_OP,
    TiBasicTokenTypes.MUL_OP,
    TiBasicTokenTypes.DIV_OP,
    TiBasicTokenTypes.POW_OP,
)

internal val UNARY_EXPRESSION_OPERATOR_TYPES = setOf(
    TiBasicTokenTypes.PLUS_OP,
    TiBasicTokenTypes.MINUS_OP,
)

private val EXPRESSION_VALUE_NODE_TYPES = setOf(
    TiBasicTokenTypes.NUMERIC_LITERAL,
    TiBasicTokenTypes.STRING_LITERAL,
    TiBasicNodeTypes.VARIABLE_ACCESS,
    TiBasicNodeTypes.FUNCTION_CALL,
)

private fun ASTNode.isExpressionOperand(): Boolean = elementType in EXPRESSION_VALUE_NODE_TYPES
