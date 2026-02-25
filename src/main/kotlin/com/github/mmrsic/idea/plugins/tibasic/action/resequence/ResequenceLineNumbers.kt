package com.github.mmrsic.idea.plugins.tibasic.action.resequence

import com.github.mmrsic.idea.plugins.tibasic.ext.childrenAfter
import com.github.mmrsic.idea.plugins.tibasic.ext.childrenOfType
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.psi.*
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

const val RESEQUENCE_DEFAULT_START = 100
const val RESEQUENCE_DEFAULT_STEP = 10

fun resequencedText(file: TiBasicFile, start: Int = RESEQUENCE_DEFAULT_START, step: Int = RESEQUENCE_DEFAULT_STEP): String {
    val lines = file.lines()
    val originalText = file.text
    if (lines.isEmpty()) return originalText
    val oldToNew = lines.mapIndexed { index, line -> line.lineNumber() to (start + index * step) }.toMap()
    val replacements = collectReplacements(lines, oldToNew, start, step)
    return applyReplacements(originalText, replacements)
}

private data class Replacement(val startOffset: Int, val endOffset: Int, val newText: String)

private fun collectReplacements(lines: List<TiBasicLine>, oldToNew: Map<Int, Int>, start: Int, step: Int): List<Replacement> {
    val replacements = mutableListOf<Replacement>()
    lines.forEachIndexed { index, line ->
        val newNumber = start + index * step
        val oldNumberText = line.lineNumber().toString()
        val lineStart = line.textRange.startOffset
        replacements.add(Replacement(lineStart, lineStart + oldNumberText.length, newNumber.toString()))
        line.children.forEach { collectLineNumberTargetReplacements(it, oldToNew, replacements) }
    }
    return replacements
}

private fun collectLineNumberTargetReplacements(
    element: PsiElement,
    oldToNew: Map<Int, Int>,
    replacements: MutableList<Replacement>,
) {
    when (element) {
        is TiBasicGotoStatement ->
            element.node.childrenOfType(TiBasicTokenTypes.NUMERIC_LITERAL)
                .forEach { replaceIfMapped(it, oldToNew, replacements) }

        is TiBasicOnGotoStatement ->
            element.node.childrenAfter(TiBasicTokenTypes.GOTO_KEYWORD)
                .filter { it.elementType == TiBasicTokenTypes.NUMERIC_LITERAL }
                .forEach { replaceIfMapped(it, oldToNew, replacements) }

        is TiBasicLineNumberListStatement ->
            element.node.childrenOfType(TiBasicTokenTypes.NUMERIC_LITERAL)
                .forEach { replaceIfMapped(it, oldToNew, replacements) }
    }
}

private fun replaceIfMapped(node: ASTNode, oldToNew: Map<Int, Int>, replacements: MutableList<Replacement>) {
    val target = node.text.toIntOrNull() ?: return
    val newTarget = oldToNew[target] ?: return
    replacements.add(Replacement(node.startOffset, node.startOffset + node.textLength, newTarget.toString()))
}

private fun applyReplacements(originalText: String, replacements: List<Replacement>): String {
    val result = StringBuilder(originalText)
    replacements.sortedByDescending { it.startOffset }.forEach { r ->
        result.replace(r.startOffset, r.endOffset, r.newText)
    }
    return result.toString()
}
