package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.countUnclosedParens
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler
import com.intellij.psi.PsiDocumentManager

class TiBasicEnterHandler(private val originalHandler: EditorActionHandler) : EditorWriteActionHandler() {

    override fun executeWriteAction(editor: Editor, caret: Caret?, dataContext: DataContext) {
        if (!TiBasicParenAutoCloseSettings.getInstance().autoCloseOnEnter) {
            originalHandler.execute(editor, caret, dataContext)
            return
        }
        val project = editor.project
        val psiFile = project?.let { PsiDocumentManager.getInstance(it).getPsiFile(editor.document) }
        if (psiFile !is TiBasicFile || !isCursorAtLineEnd(editor)) {
            originalHandler.execute(editor, caret, dataContext)
            return
        }
        val document = editor.document
        val lineNumber = document.getLineNumber(editor.caretModel.offset)
        val lineStart = document.getLineStartOffset(lineNumber)
        val lineEnd = document.getLineEndOffset(lineNumber)
        val lineText = document.text.substring(lineStart, lineEnd)
        val unclosed = countUnclosedParens(lineText)
        if (unclosed > 0) {
            document.insertString(lineEnd, ")".repeat(unclosed))
            editor.caretModel.moveToOffset(lineEnd + unclosed)
        }
        originalHandler.execute(editor, caret, dataContext)
    }

    private fun isCursorAtLineEnd(editor: Editor): Boolean {
        val document = editor.document
        val offset = editor.caretModel.offset
        val lineEnd = document.getLineEndOffset(document.getLineNumber(offset))
        val trailingText = document.text.substring(offset, lineEnd)
        return trailingText.isBlank()
    }
}
