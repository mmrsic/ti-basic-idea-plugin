package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicLexer
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.common.VALID_LINE_NUMBER_RANGE
import com.github.mmrsic.idea.plugins.tibasic.psi.statement.TiBasicLine
import com.intellij.openapi.editor.Editor

internal data class TextReplacement(val start: Int, val end: Int, val newText: String)
internal data class LineContext(val text: String, val caretInLine: Int)

private const val LINE_NUMBER_ROUNDING_STEP = 10

internal fun applyTextReplacements(text: String, replacements: List<TextReplacement>): String {
    val result = StringBuilder(text)
    replacements.sortedByDescending { it.start }.forEach { r ->
        result.replace(r.start, r.end, r.newText)
    }
    return result.toString()
}

internal fun isAtEndOfFile(editor: Editor, file: TiBasicFile): Boolean {
    val checkOffset = if (editor.selectionModel.hasSelection())
        editor.selectionModel.selectionEnd
    else
        editor.caretModel.offset
    return file.lines().none { it.textRange.startOffset > checkOffset }
}

internal fun shouldAutoInsertLineNumber(editor: Editor, file: TiBasicFile): Boolean =
    isAtEndOfFile(editor, file)

internal fun generatedAutoLineNumber(file: TiBasicFile): Int =
    nextLineNumber(file.lines().maxValidLineNumber(), 0)

internal fun shouldOfferAutoLineNumberCompletion(editor: Editor, file: TiBasicFile): Boolean {
    val lineContext = currentLineContext(editor)
    val generatedLineNumber = generatedAutoLineNumber(file).toString()
    return shouldAutoInsertLineNumber(editor, file) &&
        if (lineContext.caretInLine == 0) {
            !startsWithLineNumber(lineContext.text)
        } else {
            typedLineNumberPrefix(lineContext)?.let(generatedLineNumber::startsWith) == true
        }
}

internal fun List<TiBasicLine>.maxValidLineNumber(): Int =
    mapNotNull { it.lineNumber().takeIf { n -> n in VALID_LINE_NUMBER_RANGE } }
        .maxOrNull() ?: 0

internal fun nextLineNumber(maxLineNumber: Int, increment: Int): Int =
    nextLineNumber(
        maxLineNumber = maxLineNumber,
        increment = increment,
        delta = TiBasicAutoLineNumberSettings.getInstance().autoLineNumberDelta,
        roundToTens = TiBasicAutoLineNumberSettings.getInstance().roundToTens,
    )

internal fun nextLineNumber(maxLineNumber: Int, increment: Int, delta: Int, roundToTens: Boolean): Int {
    val rawNextLineNumber = maxLineNumber + delta * (increment + 1)
    if (!roundToTens) {
        return rawNextLineNumber
    }

    var previousGeneratedLineNumber = maxLineNumber
    repeat(increment + 1) { index ->
        val rawLineNumber = maxLineNumber + delta * (index + 1)
        previousGeneratedLineNumber = roundUpToNextStrictlyGreaterMultipleOfTen(
            maxOf(rawLineNumber, previousGeneratedLineNumber + 1),
        )
    }
    return previousGeneratedLineNumber
}

internal fun currentLineContext(editor: Editor): LineContext {
    val document = editor.document
    val offset = editor.caretModel.offset
    val lineNumber = document.getLineNumber(offset)
    val lineStart = document.getLineStartOffset(lineNumber)
    val lineEnd = document.getLineEndOffset(lineNumber)
    return LineContext(
        text = document.text.substring(lineStart, lineEnd),
        caretInLine = offset - lineStart,
    )
}

internal fun shouldInsertSpaceAfterLineNumber(lineContext: LineContext, typedChar: Char): Boolean {
    if (typedChar.isWhitespace() || typedChar.isDigit() || lineContext.caretInLine != lineContext.text.length) {
        return false
    }
    val lineNumber = lineContext.text.toIntOrNull() ?: return false
    return lineNumber in VALID_LINE_NUMBER_RANGE &&
        createsUnknownStatementWithInsertedSeparator(lineContext.text, typedChar)
}

private fun createsUnknownStatementWithInsertedSeparator(lineText: String, typedChar: Char): Boolean {
    val simulatedLine = "$lineText $typedChar"
    val lexer = TiBasicLexer()
    lexer.start(simulatedLine, 0, simulatedLine.length, 0)
    while (lexer.tokenType != null) {
        if (lexer.tokenType == TiBasicTokenTypes.UNKNOWN_STATEMENT_TEXT) {
            return true
        }
        lexer.advance()
    }
    return false
}

private fun roundUpToNextStrictlyGreaterMultipleOfTen(number: Int): Int {
    val remainder = number % LINE_NUMBER_ROUNDING_STEP
    return if (remainder == 0) {
        number
    } else {
        number + (LINE_NUMBER_ROUNDING_STEP - remainder)
    }
}

private fun startsWithLineNumber(lineText: String): Boolean =
    lineText.dropWhile { it.isWhitespace() }
        .takeWhile { it.isDigit() }
        .isNotEmpty()

internal fun typedLineNumberPrefix(lineContext: LineContext): String? =
    lineContext.text.take(lineContext.caretInLine)
        .takeIf { prefix -> prefix.isNotEmpty() && prefix.all(Char::isDigit) }
