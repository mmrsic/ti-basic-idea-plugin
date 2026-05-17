package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicLexer
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.common.VALID_LINE_NUMBER_RANGE
import com.github.mmrsic.idea.plugins.tibasic.psi.statement.TiBasicLine
import com.intellij.openapi.editor.Editor
import com.intellij.psi.tree.IElementType

internal data class TextReplacement(val start: Int, val end: Int, val newText: String)
internal data class LineContext(val text: String, val caretInLine: Int)
internal data class LexedLineToken(val start: Int, val end: Int, val type: IElementType)

private const val LINE_NUMBER_ROUNDING_STEP = 10
private const val DECIMAL_SEPARATOR = '.'
private val EXPONENT_MARKER_CHARS = setOf('E', 'e')
private val EXPONENT_SIGN_CHARS = setOf('+', '-')

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
    if (typedChar.isWhitespace() || lineContext.caretInLine != lineContext.text.length) {
        return false
    }
    val lineNumber = lineContext.text.toIntOrNull() ?: return false
    return lineNumber in VALID_LINE_NUMBER_RANGE &&
            startsTokenRequiringLeadingSpace(typedChar)
}

internal fun shouldInsertSpaceBeforeTypedCharacterAfterNumber(
    lineContext: LineContext,
    typedChar: Char,
    treatNumberAsLineNumber: Boolean,
): Boolean {
    if (typedChar.isWhitespace() || lineContext.caretInLine == 0) {
        return false
    }
    val tokenBeforeCaret = lexedLineTokens(lineContext.text)
        .lastOrNull { token -> token.end == lineContext.caretInLine }
        ?: return false
    if (tokenBeforeCaret.type != TiBasicTokenTypes.NUMERIC_LITERAL) {
        return false
    }
    val numericLiteralText = lineContext.text.substring(tokenBeforeCaret.start, tokenBeforeCaret.end)
    return if (treatNumberAsLineNumber) {
        startsTokenRequiringLeadingSpace(typedChar)
    } else {
        !continuesNumericLiteral(numericLiteralText, typedChar) &&
                startsTokenRequiringLeadingSpace(typedChar)
    }
}

private fun lexedLineTokens(lineText: String): List<LexedLineToken> {
    val lexer = TiBasicLexer()
    lexer.start(lineText, 0, lineText.length, 0)
    val tokens = mutableListOf<LexedLineToken>()
    while (lexer.tokenType != null) {
        val tokenType = lexer.tokenType ?: break
        tokens += LexedLineToken(
            start = lexer.tokenStart,
            end = lexer.tokenEnd,
            type = tokenType,
        )
        lexer.advance()
    }
    return tokens
}

private fun startsTokenRequiringLeadingSpace(typedChar: Char): Boolean = typedChar.isLetter()

private fun continuesNumericLiteral(numericLiteralText: String, typedChar: Char): Boolean =
    when {
        typedChar.isDigit() -> true
        typedChar == DECIMAL_SEPARATOR ->
            DECIMAL_SEPARATOR !in numericLiteralText &&
                    numericLiteralText.none(EXPONENT_MARKER_CHARS::contains)

        typedChar in EXPONENT_MARKER_CHARS ->
            numericLiteralText.none(EXPONENT_MARKER_CHARS::contains)

        typedChar in EXPONENT_SIGN_CHARS ->
            numericLiteralText.lastOrNull() in EXPONENT_MARKER_CHARS

        else -> false
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
