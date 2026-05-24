package com.github.mmrsic.idea.plugins.tibasic.common.ext

import com.github.mmrsic.idea.plugins.tibasic.language.syntax.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.common.VALID_LINE_NUMBER_RANGE
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.containingTiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicLine
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicGosubStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicGotoStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicIfStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicLineNumberListStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicOnGosubStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicOnGotoStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicRestoreStatement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

inline fun <reified T : PsiElement> PsiElement.firstChildOfType(): T? =
    children.filterIsInstance<T>().firstOrNull()

fun PsiElement.lineNumberReferenceNodes(): List<ASTNode> = when (this) {
    is TiBasicOnGotoStatement ->
        node.childrenAfter(TiBasicTokenTypes.GOTO_KEYWORD)
            .filter { it.elementType == TiBasicTokenTypes.NUMERIC_LITERAL }

    is TiBasicOnGosubStatement ->
        node.childrenAfter(TiBasicTokenTypes.GOSUB_KEYWORD)
            .filter { it.elementType == TiBasicTokenTypes.NUMERIC_LITERAL }

    is TiBasicIfStatement ->
        node.childrenAfter(TiBasicTokenTypes.THEN_KEYWORD)
            .filter { it.elementType == TiBasicTokenTypes.NUMERIC_LITERAL }

    is TiBasicLineNumberListStatement,
    is TiBasicGotoStatement,
    is TiBasicGosubStatement -> node.childrenOfType(TiBasicTokenTypes.NUMERIC_LITERAL)

    is TiBasicRestoreStatement -> recordNumberExpr()
        ?.node
        ?.childrenOfType(TiBasicTokenTypes.NUMERIC_LITERAL)
        .orEmpty()

    else -> emptyList()
}

fun PsiElement.isLineNumberReference(): Boolean =
    node?.elementType == TiBasicTokenTypes.NUMERIC_LITERAL &&
            parent?.lineNumberReferenceNodes()?.any { it.psi == this } == true

fun PsiElement.referencedLineNumber(): Int? =
    if (isLineNumberReference()) {
        text.toIntOrNull()?.takeIf { it in VALID_LINE_NUMBER_RANGE }
    } else {
        null
    }

fun PsiElement.resolveReferencedLine(): TiBasicLine? =
    referencedLineNumber()?.let { targetLineNumber ->
        containingTiBasicFile?.lineByNumber(targetLineNumber)
    }
