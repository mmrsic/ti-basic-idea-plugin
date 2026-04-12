package com.github.mmrsic.idea.plugins.tibasic.psi.statement

import com.github.mmrsic.idea.plugins.tibasic.ext.childrenAfter
import com.github.mmrsic.idea.plugins.tibasic.ext.childrenOfType
import com.github.mmrsic.idea.plugins.tibasic.ext.firstChildOfType
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

private fun ASTNode.forNextControlVariableName(): String? =
    firstChildOfType(TiBasicNodeTypes.VARIABLE_ACCESS)?.firstChildNode?.text?.uppercase()

class TiBasicForStatement(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun controlVariableName(): String? = node.forNextControlVariableName()
}

class TiBasicNextStatement(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun controlVariableName(): String? = node.forNextControlVariableName()
}

class TiBasicReadStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicDataStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicUnknownStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicInvalidLine(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicDimStatement(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun dimVariableAccesses(): List<TiBasicVariableAccess> =
        node.childrenOfType(TiBasicNodeTypes.VARIABLE_ACCESS).map { it.psi as TiBasicVariableAccess }
}

class TiBasicOptionBaseStatement(node: ASTNode) : ASTWrapperPsiElement(node)

