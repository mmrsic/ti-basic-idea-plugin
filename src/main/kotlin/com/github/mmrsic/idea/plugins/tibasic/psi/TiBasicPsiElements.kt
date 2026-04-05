package com.github.mmrsic.idea.plugins.tibasic.psi

import com.github.mmrsic.idea.plugins.tibasic.ext.childrenAfter
import com.github.mmrsic.idea.plugins.tibasic.ext.childrenOfType
import com.github.mmrsic.idea.plugins.tibasic.ext.firstChildOfType
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
import com.intellij.psi.tree.IElementType

val VALID_LINE_NUMBER_RANGE = 1..32767

private val DEF_VARIABLE_TYPES: Set<IElementType> = setOf(
    TiBasicTokenTypes.NUMERIC_VARIABLE,
    TiBasicTokenTypes.STRING_VARIABLE,
    TiBasicTokenTypes.INVALID_VARIABLE_NAME,
)

// ...existing code...

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

abstract class TiBasicScreenPrintStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicPrintStatement(node: ASTNode) : TiBasicScreenPrintStatement(node) {
    fun isFileOutput(): Boolean =
        node.firstChildOfType(TiBasicTokenTypes.HASH) != null

    fun fileNumberExpr(): TiBasicExpression? {
        if (!isFileOutput()) return null
        return node.childrenOfType(TiBasicNodeTypes.EXPRESSION).getOrNull(0)?.psi as? TiBasicExpression
    }

    fun recordNumberExpr(): TiBasicExpression? {
        if (node.firstChildOfType(TiBasicTokenTypes.REC_KEYWORD) == null) return null
        return node.childrenOfType(TiBasicNodeTypes.EXPRESSION).getOrNull(1)?.psi as? TiBasicExpression
    }
}

class TiBasicDisplayStatement(node: ASTNode) : TiBasicScreenPrintStatement(node)

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

class TiBasicInputStatement(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun isFileInput(): Boolean =
        node.firstChildOfType(TiBasicTokenTypes.HASH) != null

    fun fileNumberExpr(): TiBasicExpression? {
        if (!isFileInput()) return null
        return node.childrenOfType(TiBasicNodeTypes.EXPRESSION).getOrNull(0)?.psi as? TiBasicExpression
    }

    fun recordNumberExpr(): TiBasicExpression? {
        if (node.firstChildOfType(TiBasicTokenTypes.REC_KEYWORD) == null) return null
        return node.childrenOfType(TiBasicNodeTypes.EXPRESSION).getOrNull(1)?.psi as? TiBasicExpression
    }

    fun inputVariableAccesses(): List<TiBasicVariableAccess> =
        node.childrenOfType(TiBasicNodeTypes.VARIABLE_ACCESS).map { it.psi as TiBasicVariableAccess }
}

class TiBasicReadStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicDataStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicRestoreStatement(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun isFileRestore(): Boolean =
        node.firstChildOfType(TiBasicTokenTypes.HASH) != null

    fun fileNumberExpr(): TiBasicExpression? {
        if (!isFileRestore()) return null
        return node.childrenOfType(TiBasicNodeTypes.EXPRESSION).getOrNull(0)?.psi as? TiBasicExpression
    }

    fun recordNumberExpr(): TiBasicExpression? {
        if (node.firstChildOfType(TiBasicTokenTypes.REC_KEYWORD) == null) return null
        return node.childrenOfType(TiBasicNodeTypes.EXPRESSION).getOrNull(1)?.psi as? TiBasicExpression
    }
}

class TiBasicUnknownStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicInvalidLine(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicTabFunction(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicExpression(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicCallStatement(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun subprogramName(): String? =
        node.firstChildOfType(TiBasicTokenTypes.CALL_SUBPROGRAM_NAME)?.text?.uppercase()

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

    fun subscriptDimCount(): Int =
        node.childrenOfType(TiBasicNodeTypes.EXPRESSION).size
}

class TiBasicFunctionCall(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun functionName(): String? =
        (node.firstChildOfType(TiBasicTokenTypes.NUMERIC_FUNCTION_KEYWORD)
            ?: node.firstChildOfType(TiBasicTokenTypes.STRING_FUNCTION_KEYWORD))?.text?.uppercase()

    fun arguments(): List<TiBasicExpression> =
        node.childrenOfType(TiBasicNodeTypes.EXPRESSION).map { it.psi as TiBasicExpression }
}

class TiBasicDimStatement(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun dimVariableAccesses(): List<TiBasicVariableAccess> =
        node.childrenOfType(TiBasicNodeTypes.VARIABLE_ACCESS).map { it.psi as TiBasicVariableAccess }
}

class TiBasicOptionBaseStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicOpenStatement(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun fileNumberExpr(): TiBasicExpression? =
        node.childrenOfType(TiBasicNodeTypes.EXPRESSION).getOrNull(0)?.psi as? TiBasicExpression

    fun fileNameExpr(): TiBasicExpression? =
        node.childrenOfType(TiBasicNodeTypes.EXPRESSION).getOrNull(1)?.psi as? TiBasicExpression

    fun options(): List<TiBasicOpenOption> =
        node.childrenOfType(TiBasicNodeTypes.OPEN_OPTION).map { it.psi as TiBasicOpenOption }
}

class TiBasicOpenOption(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun optionKeywordType() = node.nonWhitespaceChildren
        .firstOrNull { it.elementType != TiBasicTokenTypes.COMMA }
        ?.elementType

    fun optionExpression(): TiBasicExpression? =
        node.firstChildOfType(TiBasicNodeTypes.EXPRESSION)?.psi as? TiBasicExpression
}

class TiBasicCloseStatement(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun hasDeleteModifier(): Boolean = node.firstChildOfType(TiBasicTokenTypes.DELETE_KEYWORD) != null
}

