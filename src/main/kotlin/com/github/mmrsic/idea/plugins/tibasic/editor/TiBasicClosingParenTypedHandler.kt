package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.util.countUnclosedParens
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

private const val CLOSING_PAREN = ')'

class TiBasicClosingParenTypedHandler : TypedHandlerDelegate() {

    override fun beforeCharTyped(
        c: Char,
        project: Project,
        editor: Editor,
        file: PsiFile,
        fileType: FileType,
    ): Result {
        if (!shouldSkipExistingClosingParen(c, editor, file)) {
            return Result.CONTINUE
        }
        editor.caretModel.moveToOffset(editor.caretModel.offset + 1)
        return Result.STOP
    }

    private fun shouldSkipExistingClosingParen(c: Char, editor: Editor, file: PsiFile): Boolean {
        if (c != CLOSING_PAREN || file !is TiBasicFile || editor.selectionModel.hasSelection()) {
            return false
        }
        val document = editor.document
        val offset = editor.caretModel.offset
        if (offset >= document.textLength || document.charsSequence[offset] != CLOSING_PAREN) {
            return false
        }
        val lineNumber = document.getLineNumber(offset)
        val lineStart = document.getLineStartOffset(lineNumber)
        val lineEnd = document.getLineEndOffset(lineNumber)
        val lineText = document.text.substring(lineStart, lineEnd)
        val caretInLine = offset - lineStart
        val unclosedBeforeCaret = countUnclosedParens(lineText.substring(0, caretInLine))
        val unclosedAfterCurrentParen = countUnclosedParens(lineText.substring(0, caretInLine + 1))
        val unclosedAtLineEnd = countUnclosedParens(lineText)
        return unclosedAfterCurrentParen < unclosedBeforeCaret &&
            (unclosedAfterCurrentParen == 0 || unclosedAtLineEnd < unclosedAfterCurrentParen)
    }
}
