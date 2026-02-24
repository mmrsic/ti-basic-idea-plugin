package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.VALID_LINE_NUMBER_RANGE
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
        if (psiFile !is TiBasicFile || !shouldAutoInsertLineNumber(editor, psiFile)) {
            originalHandler.execute(editor, caret, dataContext)
            return
        }
        insertNewLineWithAutoLineNumber(editor, psiFile)
    }

    private fun shouldAutoInsertLineNumber(editor: Editor, file: TiBasicFile): Boolean {
        val cursorOffset = editor.caretModel.offset
        return file.lines()
            .none { it.textRange.startOffset > cursorOffset }
    }

    private fun insertNewLineWithAutoLineNumber(editor: Editor, file: TiBasicFile) {
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
