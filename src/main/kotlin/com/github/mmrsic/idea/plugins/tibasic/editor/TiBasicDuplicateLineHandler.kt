package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.common.ext.lineNumberReferenceNodes
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.common.VALID_LINE_NUMBER_RANGE
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler
import com.intellij.psi.PsiDocumentManager

class TiBasicDuplicateLineHandler(private val originalHandler: EditorActionHandler) : EditorWriteActionHandler() {

    override fun executeWriteAction(editor: Editor, caret: Caret?, dataContext: DataContext) {
        val project = editor.project
        val documentManager = project?.let { PsiDocumentManager.getInstance(it) }
        val psiFile = documentManager?.getPsiFile(editor.document)
        if (psiFile !is TiBasicFile || !isAtEndOfFile(editor, psiFile)) {
            originalHandler.execute(editor, caret, dataContext)
            return
        }
        val lines = psiFile.lines()
        val originalLineCount = lines.size
        val maxLineNumber = lines.maxValidLineNumber()

        originalHandler.execute(editor, caret, dataContext)
        documentManager.commitDocument(editor.document)

        val newLines = psiFile.lines().drop(originalLineCount)
        val replacements = mutableListOf<TextReplacement>()
        newLines.forEachIndexed { i, line ->
            val oldNumber = line.lineNumber()
            if (oldNumber !in VALID_LINE_NUMBER_RANGE) return@forEachIndexed
            val newNumber = nextLineNumber(maxLineNumber, i)
            val lineStart = line.textRange.startOffset
            replacements.add(TextReplacement(lineStart, lineStart + oldNumber.toString().length, newNumber.toString()))
            val delta = newNumber - oldNumber
            line.children.flatMap { it.lineNumberReferenceNodes() }.forEach { node ->
                val refValue = node.text.toIntOrNull() ?: return@forEach
                replacements.add(TextReplacement(node.startOffset, node.startOffset + node.textLength, (refValue + delta).toString()))
            }
        }
        replacements.sortedByDescending { it.start }
            .forEach { r -> editor.document.replaceString(r.start, r.end, r.newText) }
    }
}
