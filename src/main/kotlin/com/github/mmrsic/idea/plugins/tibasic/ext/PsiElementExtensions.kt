package com.github.mmrsic.idea.plugins.tibasic.ext

import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicGosubStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicGotoStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicIfStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicLine
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicLineNumberListStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicOnGosubStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicOnGotoStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicRestoreStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.VALID_LINE_NUMBER_RANGE
import com.github.mmrsic.idea.plugins.tibasic.psi.containingTiBasicFile
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
    is TiBasicGosubStatement,
    is TiBasicRestoreStatement -> node.childrenOfType(TiBasicTokenTypes.NUMERIC_LITERAL)

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
