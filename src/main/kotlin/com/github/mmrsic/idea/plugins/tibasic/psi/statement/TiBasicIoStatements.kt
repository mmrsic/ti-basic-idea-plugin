package com.github.mmrsic.idea.plugins.tibasic.psi.statement

import com.github.mmrsic.idea.plugins.tibasic.ext.childrenOfType
import com.github.mmrsic.idea.plugins.tibasic.ext.firstChildOfType
import com.github.mmrsic.idea.plugins.tibasic.ext.nonWhitespaceChildren
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes
import com.github.mmrsic.idea.plugins.tibasic.psi.contracts.TiBasicFileNumberStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.contracts.TiBasicRecordNumberStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.expression.TiBasicExpression
import com.github.mmrsic.idea.plugins.tibasic.psi.expression.TiBasicVariableAccess
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

abstract class TiBasicScreenPrintStatement(node: ASTNode) : ASTWrapperPsiElement(node)

class TiBasicPrintStatement(node: ASTNode) : TiBasicFileNumberStatement, TiBasicScreenPrintStatement(node), TiBasicRecordNumberStatement {

    override fun recKeywordNode(): ASTNode? = node.firstChildOfType(TiBasicTokenTypes.REC_KEYWORD)

    override fun fileNumberExpr(): TiBasicExpression? {
        if (!isFileOutput()) return null
        return node.childrenOfType(TiBasicNodeTypes.EXPRESSION).getOrNull(0)?.psi as? TiBasicExpression
    }

    fun isFileOutput(): Boolean =
        node.firstChildOfType(TiBasicTokenTypes.HASH) != null

    override fun recordNumberExpr(): TiBasicExpression? {
        if (recKeywordNode() == null) return null
        return node.childrenOfType(TiBasicNodeTypes.EXPRESSION).getOrNull(1)?.psi as? TiBasicExpression
    }
}

class TiBasicDisplayStatement(node: ASTNode) : TiBasicScreenPrintStatement(node)

class TiBasicInputStatement(node: ASTNode) : TiBasicFileNumberStatement, TiBasicRecordNumberStatement, ASTWrapperPsiElement(node) {

    override fun recKeywordNode(): ASTNode? = node.firstChildOfType(TiBasicTokenTypes.REC_KEYWORD)

    override fun recordNumberExpr(): TiBasicExpression? {
        if (recKeywordNode() == null) return null
        return node.childrenOfType(TiBasicNodeTypes.EXPRESSION).getOrNull(1)?.psi as? TiBasicExpression
    }

    fun isFileInput(): Boolean = node.firstChildOfType(TiBasicTokenTypes.HASH) != null

    override fun fileNumberExpr(): TiBasicExpression? {
        if (!isFileInput()) return null
        return node.childrenOfType(TiBasicNodeTypes.EXPRESSION).getOrNull(0)?.psi as? TiBasicExpression
    }

    fun inputVariableAccesses(): List<TiBasicVariableAccess> =
        node.childrenOfType(TiBasicNodeTypes.VARIABLE_ACCESS).map { it.psi as TiBasicVariableAccess }
}

class TiBasicRestoreStatement(node: ASTNode) : TiBasicRecordNumberStatement, ASTWrapperPsiElement(node) {

    override fun recKeywordNode(): ASTNode? = node.firstChildOfType(TiBasicTokenTypes.REC_KEYWORD)

    override fun recordNumberExpr(): TiBasicExpression? =
        when {
            recKeywordNode() != null -> node.childrenOfType(TiBasicNodeTypes.EXPRESSION).getOrNull(1)?.psi as? TiBasicExpression
            isFileRestore() -> null
            else -> node.childrenOfType(TiBasicNodeTypes.EXPRESSION).getOrNull(0)?.psi as? TiBasicExpression
        }

    fun isFileRestore(): Boolean =
        node.firstChildOfType(TiBasicTokenTypes.HASH) != null

    fun fileNumberExpr(): TiBasicExpression? {
        if (!isFileRestore()) return null
        return node.childrenOfType(TiBasicNodeTypes.EXPRESSION).getOrNull(0)?.psi as? TiBasicExpression
    }
}

class TiBasicOpenStatement(node: ASTNode) : TiBasicFileNumberStatement, ASTWrapperPsiElement(node) {
    override fun fileNumberExpr(): TiBasicExpression? =
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

class TiBasicCloseStatement(node: ASTNode) : TiBasicFileNumberStatement, ASTWrapperPsiElement(node) {
    override fun fileNumberExpr(): TiBasicExpression? =
        node.childrenOfType(TiBasicNodeTypes.EXPRESSION).getOrNull(0)?.psi as? TiBasicExpression

    fun hasDeleteModifier(): Boolean = node.firstChildOfType(TiBasicTokenTypes.DELETE_KEYWORD) != null
}
