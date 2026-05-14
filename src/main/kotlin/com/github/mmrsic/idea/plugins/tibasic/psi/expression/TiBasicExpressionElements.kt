package com.github.mmrsic.idea.plugins.tibasic.psi.expression

import com.github.mmrsic.idea.plugins.tibasic.ext.childrenOfType
import com.github.mmrsic.idea.plugins.tibasic.ext.firstChildOfType
import com.github.mmrsic.idea.plugins.tibasic.ext.firstChildType
import com.github.mmrsic.idea.plugins.tibasic.ext.nonWhitespaceChildren
import com.github.mmrsic.idea.plugins.tibasic.findusages.TiBasicVariableReference
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.LocalSearchScope

class TiBasicExpression(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun variableAccess(): TiBasicVariableAccess? =
        (node.nonWhitespaceChildren.singleOrNull()?.psi as? TiBasicVariableAccess)

    fun numericVariableTarget(): TiBasicVariableAccess? =
        variableAccess()?.takeIf { it.node.firstChildType == TiBasicTokenTypes.NUMERIC_VARIABLE }
}

class TiBasicCallStatement(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun subprogramName(): String? =
        node.firstChildOfType(TiBasicTokenTypes.CALL_SUBPROGRAM_NAME)?.text?.uppercase()

    fun hasArgumentParens(): Boolean =
        node.firstChildOfType(TiBasicTokenTypes.LPAREN) != null

    fun hasClosingArgumentParen(): Boolean =
        node.firstChildOfType(TiBasicTokenTypes.RPAREN) != null

    fun arguments(): List<TiBasicExpression> =
        node.childrenOfType(TiBasicNodeTypes.EXPRESSION).map { it.psi as TiBasicExpression }
}

class TiBasicVariableAccess(node: ASTNode) : ASTWrapperPsiElement(node), PsiNamedElement {
    override fun getName(): String? = node.firstChildNode?.text?.uppercase()
    override fun setName(name: String): PsiElement = this
    override fun getReference(): PsiReference = TiBasicVariableReference(this)
    override fun getUseScope() = LocalSearchScope(containingFile)

    fun hasSubscriptParens(): Boolean =
        node.firstChildOfType(TiBasicTokenTypes.LPAREN) != null

    fun hasClosingSubscriptParen(): Boolean =
        node.firstChildOfType(TiBasicTokenTypes.RPAREN) != null

    fun subscriptExpressions(): List<TiBasicExpression> =
        node.childrenOfType(TiBasicNodeTypes.EXPRESSION).map { it.psi as TiBasicExpression }

    fun subscriptDimCount(): Int =
        subscriptExpressions().size
}

class TiBasicFunctionCall(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun functionName(): String? =
        (node.firstChildOfType(TiBasicTokenTypes.NUMERIC_FUNCTION_KEYWORD)
            ?: node.firstChildOfType(TiBasicTokenTypes.STRING_FUNCTION_KEYWORD))?.text?.uppercase()

    fun arguments(): List<TiBasicExpression> =
        node.childrenOfType(TiBasicNodeTypes.EXPRESSION).map { it.psi as TiBasicExpression }
}

class TiBasicTabFunction(node: ASTNode) : ASTWrapperPsiElement(node)
