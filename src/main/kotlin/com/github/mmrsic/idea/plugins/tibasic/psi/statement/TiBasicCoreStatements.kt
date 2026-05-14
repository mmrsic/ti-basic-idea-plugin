package com.github.mmrsic.idea.plugins.tibasic.psi.statement

import com.github.mmrsic.idea.plugins.tibasic.ext.childrenAfter
import com.github.mmrsic.idea.plugins.tibasic.ext.childrenOfType
import com.github.mmrsic.idea.plugins.tibasic.ext.firstChildOfType
import com.github.mmrsic.idea.plugins.tibasic.ext.nonWhitespaceChildren
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes
import com.github.mmrsic.idea.plugins.tibasic.psi.expression.TiBasicExpression
import com.github.mmrsic.idea.plugins.tibasic.psi.expression.TiBasicVariableAccess
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.tree.IElementType

private val DEF_VARIABLE_TYPES: Set<IElementType> = setOf(
    TiBasicTokenTypes.NUMERIC_VARIABLE,
    TiBasicTokenTypes.STRING_VARIABLE,
    TiBasicTokenTypes.INVALID_VARIABLE_NAME,
)
private val DATA_ITEM_TYPES: Set<IElementType> = setOf(
    TiBasicTokenTypes.NUMERIC_LITERAL,
    TiBasicTokenTypes.STRING_LITERAL,
    TiBasicTokenTypes.PRINT_ARGUMENT,
)
private val VALID_OPTION_BASE_VALUES: Set<Int> = setOf(0, 1)

class TiBasicDefStatement(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun functionNameNode(): ASTNode? =
        node.childrenAfter(TiBasicTokenTypes.DEF_KEYWORD)
            .firstOrNull { it.elementType in DEF_VARIABLE_TYPES }

    fun functionName(): String? = functionNameNode()?.text?.uppercase()

    fun parameterNode(): ASTNode? {
        if (node.firstChildOfType(TiBasicTokenTypes.LPAREN) == null) return null
        return node.childrenAfter(TiBasicTokenTypes.LPAREN)
            .firstOrNull { it.elementType in DEF_VARIABLE_TYPES }
    }

    fun bodyExpression(): TiBasicExpression? =
        node.firstChildOfType(TiBasicNodeTypes.EXPRESSION)?.psi as? TiBasicExpression
}

class TiBasicLine(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun lineNumber(): Int =
        node
            .firstChildNode.text.toLongOrNull()
            ?.takeIf { it <= Int.MAX_VALUE }?.toInt()
            ?: Int.MAX_VALUE
}

class TiBasicLineNumberListStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicDeleteStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicLetStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicRemStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicEndStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicStopStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicRandomizeStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicGotoStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicGosubStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicReturnStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicOnGotoStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicOnGosubStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicIfStatement(node: ASTNode) : ASTWrapperPsiElement(node)
{
    fun conditionExpression(): TiBasicExpression? =
        node.firstChildOfType(TiBasicNodeTypes.EXPRESSION)?.psi as? TiBasicExpression

    fun thenLineNumber(): Int? =
        node.childrenAfter(TiBasicTokenTypes.THEN_KEYWORD)
            .firstOrNull { it.elementType == TiBasicTokenTypes.NUMERIC_LITERAL }
            ?.text?.toIntOrNull()

    fun elseLineNumber(): Int? =
        node.childrenAfter(TiBasicTokenTypes.ELSE_KEYWORD)
            .firstOrNull { it.elementType == TiBasicTokenTypes.NUMERIC_LITERAL }
            ?.text?.toIntOrNull()
}

private fun ASTNode.forNextControlVariableName(): String? =
    firstChildOfType(TiBasicNodeTypes.VARIABLE_ACCESS)?.firstChildNode?.text?.uppercase()

class TiBasicForStatement(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun controlVariableName(): String? = node.forNextControlVariableName()

    fun expressions(): List<TiBasicExpression> =
        node.childrenOfType(TiBasicNodeTypes.EXPRESSION).map { it.psi as TiBasicExpression }

    fun startExpression(): TiBasicExpression? = expressions().getOrNull(0)

    fun endExpression(): TiBasicExpression? = expressions().getOrNull(1)

    fun stepExpression(): TiBasicExpression? = expressions().getOrNull(2)
}

class TiBasicNextStatement(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun controlVariableName(): String? = node.forNextControlVariableName()
}

class TiBasicReadStatement(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun variableAccesses(): List<TiBasicVariableAccess> =
        node.childrenOfType(TiBasicNodeTypes.VARIABLE_ACCESS).map { it.psi as TiBasicVariableAccess }
}

class TiBasicDataStatement(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun dataItems(): List<String> {
        val items = mutableListOf<String>()
        val children = node.nonWhitespaceChildren.drop(1)
        if (children.isEmpty()) {
            return listOf("")
        }
        var expectingItem = true
        children.forEach { child ->
            when (child.elementType) {
                TiBasicTokenTypes.COMMA -> {
                    if (expectingItem) {
                        items += ""
                    }
                    expectingItem = true
                }

                in DATA_ITEM_TYPES -> {
                    items += child.text.removePrefix("\"").removeSuffix("\"")
                    expectingItem = false
                }
            }
        }
        if (children.lastOrNull()?.elementType == TiBasicTokenTypes.COMMA) {
            items += ""
        }
        return items
    }
}

class TiBasicUnknownStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicInvalidLine(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicDimStatement(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun dimVariableAccesses(): List<TiBasicVariableAccess> =
        node.childrenOfType(TiBasicNodeTypes.VARIABLE_ACCESS).map { it.psi as TiBasicVariableAccess }
}

class TiBasicOptionBaseStatement(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun optionBaseValue(): Int? =
        node.nonWhitespaceChildren
            .firstOrNull { it.elementType != TiBasicTokenTypes.OPTION_BASE_KEYWORD }
            ?.text
            ?.toIntOrNull()
            ?.takeIf { it in VALID_OPTION_BASE_VALUES }
}
