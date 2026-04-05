package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.VALID_LINE_NUMBER_RANGE
import com.github.mmrsic.idea.plugins.tibasic.util.countUnclosedParens
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler
import com.intellij.psi.PsiDocumentManager

class TiBasicShiftEnterHandler(private val originalHandler: EditorActionHandler) : EditorWriteActionHandler() {

    override fun executeWriteAction(editor: Editor, caret: Caret?, dataContext: DataContext) {
        val project = editor.project
        val psiFile = project?.let { PsiDocumentManager.getInstance(it).getPsiFile(editor.document) }
        if (psiFile !is TiBasicFile) {
            originalHandler.execute(editor, caret, dataContext)
            return
        }
        if (shouldAutoInsertLineNumber(editor, psiFile)) {
            insertNewLineWithAutoLineNumber(editor, psiFile)
        } else {
            closeUnclosedParensAtLineEnd(editor)
            originalHandler.execute(editor, caret, dataContext)
        }
    }

    private fun closeUnclosedParensAtLineEnd(editor: Editor) {
        if (!TiBasicParenAutoCloseSettings.getInstance().autoCloseOnShiftEnter) return
        val document = editor.document
        val lineNumber = document.getLineNumber(editor.caretModel.offset)
        val lineEnd = document.getLineEndOffset(lineNumber)
        val lineStart = document.getLineStartOffset(lineNumber)
        val lineText = document.text.substring(lineStart, lineEnd)
        val unclosed = countUnclosedParens(lineText)
        if (unclosed > 0) {
            document.insertString(lineEnd, ")".repeat(unclosed))
            editor.caretModel.moveToOffset(lineEnd + unclosed)
        }
    }

    private fun shouldAutoInsertLineNumber(editor: Editor, file: TiBasicFile): Boolean {
        val cursorOffset = editor.caretModel.offset
        return file.lines()
            .none { it.textRange.startOffset > cursorOffset }
    }

    private fun insertNewLineWithAutoLineNumber(editor: Editor, file: TiBasicFile) {
        closeUnclosedParensAtLineEnd(editor)
        val maxLineNumber = file.lines()
            .mapNotNull { it.lineNumber().takeIf { n -> n in VALID_LINE_NUMBER_RANGE } }
            .maxOrNull() ?: 0
        val nextLineNumber = ((maxLineNumber / 10) + 1) * 10
        val document = editor.document
        val lineEnd = document.getLineEndOffset(document.getLineNumber(editor.caretModel.offset))
        val insertText = "\n$nextLineNumber "
        document.insertString(lineEnd, insertText)
        editor.caretModel.moveToOffset(lineEnd + insertText.length)
    }
}
