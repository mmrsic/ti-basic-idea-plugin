package com.github.mmrsic.idea.plugins.tibasic.psi

import com.github.mmrsic.idea.plugins.tibasic.ext.allChildren
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

val VALID_LINE_NUMBER_RANGE = 1..32767

class TiBasicLine(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun lineNumber(): Int =
        node
            .firstChildNode.text.toLongOrNull()
            ?.takeIf { it <= Int.MAX_VALUE }?.toInt()
            ?: Int.MAX_VALUE
}

class TiBasicPrintStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicLineNumberListStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicDeleteStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicLetStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicRemStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicUnknownStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicInvalidLine(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicExpression(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicVariableAccess(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun hasSubscriptParens(): Boolean =
        node.allChildren.any { it.elementType == TiBasicTokenTypes.LPAREN }

    fun subscriptDimCount(): Int =
        node.allChildren.count { it.elementType == TiBasicNodeTypes.EXPRESSION }
}

class TiBasicCommentLine(node: ASTNode) : ASTWrapperPsiElement(node)


