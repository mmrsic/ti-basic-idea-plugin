package com.github.mmrsic.idea.plugins.tibasic.psi

import com.github.mmrsic.idea.plugins.tibasic.ext.childrenOfType
import com.github.mmrsic.idea.plugins.tibasic.ext.firstChildOfType
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

class TiBasicEndStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicStopStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicGotoStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicOnGotoStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicIfStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicForStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicNextStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicInputStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicReadStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicDataStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicRestoreStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicUnknownStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicInvalidLine(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicTabFunction(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicExpression(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicVariableAccess(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun hasSubscriptParens(): Boolean =
        node.firstChildOfType(TiBasicTokenTypes.LPAREN) != null

    fun subscriptDimCount(): Int =
        node.childrenOfType(TiBasicNodeTypes.EXPRESSION).size
}
