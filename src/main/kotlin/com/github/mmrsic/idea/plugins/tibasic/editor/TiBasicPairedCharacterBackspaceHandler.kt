package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.TiBasicFile
import com.intellij.codeInsight.editorActions.BackspaceHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile

class TiBasicPairedCharacterBackspaceHandler : BackspaceHandlerDelegate() {

    private var shouldDeleteClosingQuote = false

    override fun beforeCharDeleted(c: Char, file: PsiFile, editor: Editor) {
        shouldDeleteClosingQuote = file is TiBasicFile &&
            !editor.selectionModel.hasSelection() &&
            c == DOUBLE_QUOTE &&
            editor.caretModel.offset < editor.document.textLength &&
            editor.document.charsSequence[editor.caretModel.offset] == DOUBLE_QUOTE
    }

    override fun charDeleted(c: Char, file: PsiFile, editor: Editor): Boolean {
        val deleteClosingQuote = shouldDeleteClosingQuote
        shouldDeleteClosingQuote = false
        if (!deleteClosingQuote || file !is TiBasicFile) {
            return false
        }

        val offset = editor.caretModel.offset
        if (offset >= editor.document.textLength || editor.document.charsSequence[offset] != DOUBLE_QUOTE) {
            return false
        }

        editor.document.deleteString(offset, offset + 1)
        return true
    }
}
