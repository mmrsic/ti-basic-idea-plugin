package com.github.mmrsic.idea.plugins.tibasic.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

class TiBasicLine(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun lineNumber(): Int = node.firstChildNode.text.toLongOrNull()
        ?.takeIf { it <= Int.MAX_VALUE }?.toInt() ?: Int.MAX_VALUE
}

class TiBasicPrintStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicExpression(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicCommentLine(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun commentText(): String = node.firstChildNode.text
}


