package com.github.mmrsic.idea.plugins.tibasic.psi

import com.github.mmrsic.idea.plugins.tibasic.TiBasicNodeTypes
import com.github.mmrsic.idea.plugins.tibasic.TiBasicTokenTypes
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

class TiBasicLine(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun lineNumber(): Int = node.firstChildNode.text.toLongOrNull()
        ?.takeIf { it <= Int.MAX_VALUE }?.toInt() ?: Int.MAX_VALUE
}

class TiBasicPrintStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicExpression(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicVariableAccess(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun hasSubscriptParens(): Boolean =
        node.getChildren(null).any { it.elementType == TiBasicTokenTypes.LPAREN }

    fun subscriptDimCount(): Int =
        node.getChildren(null).count { it.elementType == TiBasicNodeTypes.EXPRESSION }
}

class TiBasicCommentLine(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun commentText(): String = node.firstChildNode.text
}


