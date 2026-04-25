package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicLexer
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.common.VALID_LINE_NUMBER_RANGE
import com.github.mmrsic.idea.plugins.tibasic.psi.statement.TiBasicLine
import com.intellij.openapi.editor.Editor

internal data class TextReplacement(val start: Int, val end: Int, val newText: String)
internal data class LineContext(val text: String, val caretInLine: Int)

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

internal fun List<TiBasicLine>.maxValidLineNumber(): Int =
    mapNotNull { it.lineNumber().takeIf { n -> n in VALID_LINE_NUMBER_RANGE } }
        .maxOrNull() ?: 0

internal fun nextLineNumber(maxLineNumber: Int, increment: Int): Int =
    ((maxLineNumber / 10) + 1 + increment) * 10

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
