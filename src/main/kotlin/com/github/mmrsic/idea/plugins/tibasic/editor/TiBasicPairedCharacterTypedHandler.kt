package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.util.countUnclosedParens
import com.github.mmrsic.idea.plugins.tibasic.util.isRemLine
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class TiBasicPairedCharacterTypedHandler : TypedHandlerDelegate() {

    override fun beforeCharTyped(
        c: Char,
        project: Project,
        editor: Editor,
        file: PsiFile,
        fileType: FileType,
    ): Result {
        if (file !is TiBasicFile || editor.selectionModel.hasSelection()) {
            return Result.CONTINUE
        }

        return when (c) {
            OPENING_PAREN -> insertPair(editor, "$OPENING_PAREN$CLOSING_PAREN", placeCaretInsidePair = true)
            CLOSING_PAREN -> skipExistingClosingParen(editor)
            DOUBLE_QUOTE -> handleDoubleQuote(editor)
            else -> handleStringCharacterOrLineNumberSpacing(editor, c)
        }
    }

    private fun handleStringCharacterOrLineNumberSpacing(editor: Editor, typedChar: Char): Result =
        handleStringCharacterTrigger(editor, typedChar).let { result ->
            if (result != Result.CONTINUE) {
                result
            } else {
                handleLineNumberSpacing(editor, typedChar)
            }
        }

    private fun handleLineNumberSpacing(editor: Editor, typedChar: Char): Result =
        if (shouldInsertSpaceAfterLineNumber(currentLineContext(editor), typedChar)) {
            insertPair(editor, " $typedChar", placeCaretInsidePair = false)
        } else {
            Result.CONTINUE
        }

    private fun handleStringCharacterTrigger(editor: Editor, typedChar: Char): Result {
        val lineContext = currentLineContext(editor)
        if (!isInsideStringLiteral(lineContext.text, lineContext.caretInLine)) {
            return Result.CONTINUE
        }
        val match = matchStringCharacterTrigger(lineContext, typedChar) ?: return Result.CONTINUE
        val offset = editor.caretModel.offset
        val triggerStartOffset = offset - (lineContext.caretInLine - match.startInLine)
        editor.document.replaceString(triggerStartOffset, offset, match.replacementText)
        editor.caretModel.moveToOffset(triggerStartOffset + match.replacementText.length)
        return Result.STOP
    }

    private fun handleDoubleQuote(editor: Editor): Result {
        if (shouldSkipExistingClosingQuote(editor)) {
            editor.caretModel.moveToOffset(editor.caretModel.offset + 1)
            return Result.STOP
        }
        val lineContext = currentLineContext(editor)
        val insideStringLiteral = isInsideStringLiteral(lineContext.text, lineContext.caretInLine)
        if (insideStringLiteral &&
            !hasClosingQuoteAfterCaret(lineContext.text, lineContext.caretInLine)
        ) {
            return insertPair(
                editor = editor,
                pair = DOUBLE_QUOTE.toString(),
                placeCaretInsidePair = false,
            )
        }
        return insertPair(
            editor = editor,
            pair = DOUBLE_QUOTE_PAIR,
            placeCaretInsidePair = !insideStringLiteral,
        )
    }

    private fun skipExistingClosingParen(editor: Editor): Result {
        if (!shouldSkipExistingClosingParen(editor)) return Result.CONTINUE
        editor.caretModel.moveToOffset(editor.caretModel.offset + 1)
        return Result.STOP
    }

    private fun insertPair(editor: Editor, pair: String, placeCaretInsidePair: Boolean): Result {
        val offset = editor.caretModel.offset
        editor.document.insertString(offset, pair)
        val caretOffset = offset + if (placeCaretInsidePair) 1 else pair.length
        editor.caretModel.moveToOffset(caretOffset)
        return Result.STOP
    }

    private fun shouldSkipExistingClosingParen(editor: Editor): Boolean {
        val document = editor.document
        val offset = editor.caretModel.offset
        if (offset >= document.textLength || document.charsSequence[offset] != CLOSING_PAREN) {
            return false
        }
        val lineContext = currentLineContext(editor)
        val lineText = lineContext.text
        if (isInsideStringLiteral(lineText, lineContext.caretInLine)) {
            return true
        }
        if (isRemLine(lineText)) {
            return true
        }
        val caretInLine = lineContext.caretInLine
        val unclosedBeforeCaret = countUnclosedParens(lineText.substring(0, caretInLine))
        val unclosedAfterCurrentParen = countUnclosedParens(lineText.substring(0, caretInLine + 1))
        val unclosedAtLineEnd = countUnclosedParens(lineText)
        return unclosedAfterCurrentParen < unclosedBeforeCaret &&
            (unclosedAfterCurrentParen == 0 || unclosedAtLineEnd < unclosedAfterCurrentParen)
    }

    private fun shouldSkipExistingClosingQuote(editor: Editor): Boolean {
        val document = editor.document
        val offset = editor.caretModel.offset
        if (offset >= document.textLength || document.charsSequence[offset] != DOUBLE_QUOTE) {
            return false
        }
        val lineContext = currentLineContext(editor)
        return isInsideStringLiteral(lineContext.text, lineContext.caretInLine) &&
            !isEscapedQuote(lineContext.text, lineContext.caretInLine)
    }

    private fun isInsideStringLiteral(lineText: String, caretInLine: Int): Boolean {
        var insideStringLiteral = false
        var index = 0
        while (index < caretInLine) {
            val updatedState = nextStringLiteralState(lineText, index, insideStringLiteral)
            insideStringLiteral = updatedState.insideStringLiteral
            index = updatedState.nextIndex
        }
        return insideStringLiteral
    }

    private fun hasClosingQuoteAfterCaret(lineText: String, caretInLine: Int): Boolean {
        var index = caretInLine
        while (index < lineText.length) {
            when {
                lineText[index] != DOUBLE_QUOTE -> index++
                isEscapedQuote(lineText, index) -> index += DOUBLE_QUOTE_PAIR.length
                else -> return true
            }
        }
        return false
    }

    private fun nextStringLiteralState(
        lineText: String,
        index: Int,
        insideStringLiteral: Boolean,
    ): StringLiteralState =
        when {
            lineText[index] != DOUBLE_QUOTE -> StringLiteralState(insideStringLiteral, index + 1)
            insideStringLiteral && isEscapedQuote(lineText, index) ->
                StringLiteralState(insideStringLiteral, index + DOUBLE_QUOTE_PAIR.length)
            else -> StringLiteralState(!insideStringLiteral, index + 1)
        }

    private fun isEscapedQuote(lineText: String, index: Int): Boolean =
        index + 1 < lineText.length &&
            lineText[index] == DOUBLE_QUOTE &&
            lineText[index + 1] == DOUBLE_QUOTE

    private data class StringLiteralState(
        val insideStringLiteral: Boolean,
        val nextIndex: Int,
    )
}
