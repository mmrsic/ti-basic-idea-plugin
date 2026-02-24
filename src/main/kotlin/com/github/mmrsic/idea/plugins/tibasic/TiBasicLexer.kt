package com.github.mmrsic.idea.plugins.tibasic

import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

/**
 * Lexer for TI-Basic source files.
 *
 * A valid line has the form: ```[ws] <lineNumber> [ws] PRINT [ws] [argument] [ws]```
 * where `lineNumber` is an integer in 1..32767. All other lines are treated as COMMENT tokens.
 * Leading and trailing whitespace on valid lines is emitted as WHITE_SPACE tokens.
 *
 * Within a valid line the lexer produces:
 *  - WHITE_SPACE    – optional leading spaces/tabs
 *  - LINE_NUMBER    – the leading integer
 *  - WHITE_SPACE    – spaces/tabs between tokens
 *  - PRINT_KEYWORD  – the PRINT keyword
 *  - WHITE_SPACE    – optional spaces/tabs after PRINT
 *  - (per argument token): STRING_LITERAL | CONCAT_OP | PRINT_ARGUMENT | WHITE_SPACE
 *  - WHITE_SPACE    – optional trailing spaces/tabs
 *
 * For invalid lines the entire line content (without the newline) is a single COMMENT token.
 * Newlines themselves are emitted as WHITE_SPACE so the parser can use them as line separators.
 */
class TiBasicLexer : LexerBase() {

    private companion object {
        val VALID_LINE = Regex("""^([ \t]*)(\d+)([ \t]+)(PRINT|BREAK|UNBREAK|TRACE|UNTRACE)([ \t]*)(.*)$""", RegexOption.IGNORE_CASE)
        val PARTIAL_KEYWORD_LINE = Regex("""^([ \t]*)(\d+)([ \t]+)([A-Za-z]+)([ \t]*)$""")
        val TRAILING_WS = Regex("""([ \t]*)$""")
        val VALID_VARIABLE_NAME = Regex("""^[A-Z@\[\]\\_][A-Z0-9@_]{0,13}\$$""", RegexOption.IGNORE_CASE)
        val VALID_NUMERIC_VARIABLE_NAME = Regex("""^[A-Za-z@\[\]\\_][A-Za-z0-9@_]{0,14}$""")
        val NUMERIC_LITERAL = Regex("""^\d+(\.\d+)?([Ee][+-]?\d+)?$""")
        val LINE_NUMBER_LIST_KEYWORDS = setOf("BREAK", "UNBREAK", "TRACE", "UNTRACE")
    }

    private enum class LineKind { VALID, PARTIAL_KEYWORD, COMMENT }

    private data class LineToken(val start: Int, val end: Int, val type: IElementType)

    private var buffer: CharSequence = ""
    private var endOffset = 0
    private var tokens: List<LineToken> = emptyList()
    private var tokenIndex = 0

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.endOffset = endOffset
        val lineStart = findLineStart(buffer, startOffset)
        tokens = tokenize(buffer, lineStart, endOffset)
        tokenIndex = tokens.indexOfLast { it.start <= startOffset }.coerceAtLeast(0)
    }

    override fun getState(): Int = 0

    override fun getTokenType(): IElementType? = tokens.getOrNull(tokenIndex)?.type

    override fun getTokenStart(): Int = tokens.getOrNull(tokenIndex)?.start ?: endOffset

    override fun getTokenEnd(): Int = tokens.getOrNull(tokenIndex)?.end ?: endOffset

    override fun advance() {
        tokenIndex++
    }

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = endOffset

    private fun tokenize(buffer: CharSequence, startOffset: Int, endOffset: Int): List<LineToken> {
        val result = mutableListOf<LineToken>()
        var pos = startOffset
        while (pos < endOffset) {
            val lineEnd = findLineEnd(buffer, pos, endOffset)
            val lineText = buffer.subSequence(pos, lineEnd).toString()
            val kind = classifyLine(lineText)
            when (kind) {
                LineKind.VALID -> result.addAll(tokenizeValidLine(pos, VALID_LINE.find(lineText)!!))
                LineKind.PARTIAL_KEYWORD -> result.addAll(tokenizePartialKeywordLine(pos, PARTIAL_KEYWORD_LINE.find(lineText)!!))
                LineKind.COMMENT -> if (lineEnd > pos) result.add(LineToken(pos, lineEnd, TiBasicTokenTypes.COMMENT))
            }
            pos = lineEnd
            if (pos < endOffset && buffer[pos] == '\r') pos++
            if (pos < endOffset && buffer[pos] == '\n') {
                result.add(LineToken(pos, pos + 1, TokenType.WHITE_SPACE))
                pos++
            }
        }
        return result
    }

    private fun findLineStart(buffer: CharSequence, offset: Int): Int {
        var i = offset
        while (i > 0 && buffer[i - 1] != '\n') i--
        return i
    }

    private fun findLineEnd(buffer: CharSequence, from: Int, limit: Int): Int {
        var i = from
        while (i < limit && buffer[i] != '\n' && buffer[i] != '\r') i++
        return i
    }

    private fun classifyLine(lineText: String): LineKind {
        if (VALID_LINE.containsMatchIn(lineText)) return LineKind.VALID
        if (PARTIAL_KEYWORD_LINE.containsMatchIn(lineText)) return LineKind.PARTIAL_KEYWORD
        return LineKind.COMMENT
    }

    private fun tokenizeValidLine(lineStart: Int, match: MatchResult): List<LineToken> {
        val result = mutableListOf<LineToken>()
        var offset = lineStart
        val (leadingWs, numStr, ws1, printStr, ws2, afterPrint) = match.destructured
        val trailingWsLength = TRAILING_WS.find(afterPrint)!!.value.length
        val argStr = afterPrint.dropLast(trailingWsLength)
        if (leadingWs.isNotEmpty()) {
            result.add(LineToken(offset, offset + leadingWs.length, TokenType.WHITE_SPACE))
            offset += leadingWs.length
        }
        result.add(LineToken(offset, offset + numStr.length, TiBasicTokenTypes.LINE_NUMBER))
        offset += numStr.length
        if (ws1.isNotEmpty()) {
            result.add(LineToken(offset, offset + ws1.length, TokenType.WHITE_SPACE))
            offset += ws1.length
        }
        val keywordType = if (printStr.uppercase() in LINE_NUMBER_LIST_KEYWORDS)
            TiBasicTokenTypes.LINE_NUMBER_LIST_KEYWORD
        else
            TiBasicTokenTypes.PRINT_KEYWORD
        result.add(LineToken(offset, offset + printStr.length, keywordType))
        offset += printStr.length
        if (ws2.isNotEmpty()) {
            result.add(LineToken(offset, offset + ws2.length, TokenType.WHITE_SPACE))
            offset += ws2.length
        }
        if (argStr.isNotEmpty()) {
            result.addAll(tokenizeArgument(offset, argStr))
            offset += argStr.length
        }
        if (trailingWsLength > 0) {
            result.add(LineToken(offset, offset + trailingWsLength, TokenType.WHITE_SPACE))
        }
        return result
    }

    private fun tokenizePartialKeywordLine(lineStart: Int, match: MatchResult): List<LineToken> {
        val result = mutableListOf<LineToken>()
        var offset = lineStart
        val (leadingWs, numStr, ws1, keywordStr, trailingWs) = match.destructured
        if (leadingWs.isNotEmpty()) {
            result.add(LineToken(offset, offset + leadingWs.length, TokenType.WHITE_SPACE))
            offset += leadingWs.length
        }
        result.add(LineToken(offset, offset + numStr.length, TiBasicTokenTypes.LINE_NUMBER))
        offset += numStr.length
        result.add(LineToken(offset, offset + ws1.length, TokenType.WHITE_SPACE))
        offset += ws1.length
        val keywordType = if (keywordStr.uppercase() in LINE_NUMBER_LIST_KEYWORDS)
            TiBasicTokenTypes.LINE_NUMBER_LIST_KEYWORD
        else
            TiBasicTokenTypes.PRINT_KEYWORD
        result.add(LineToken(offset, offset + keywordStr.length, keywordType))
        offset += keywordStr.length
        if (trailingWs.isNotEmpty()) {
            result.add(LineToken(offset, offset + trailingWs.length, TokenType.WHITE_SPACE))
        }
        return result
    }


    private fun tokenizeArgument(offset: Int, argStr: String): List<LineToken> {
        val result = mutableListOf<LineToken>()
        var i = 0
        while (i < argStr.length) {
            val ch = argStr[i]
            when {
                ch.isWhitespace() -> {
                    val start = i
                    while (i < argStr.length && argStr[i].isWhitespace()) i++
                    result.add(LineToken(offset + start, offset + i, TokenType.WHITE_SPACE))
                }
                ch == '"' -> {
                    val start = i++
                    var closed = false
                    while (i < argStr.length) {
                        when {
                            argStr[i] == '"' && i + 1 < argStr.length && argStr[i + 1] == '"' -> i += 2
                            argStr[i] == '"' -> { i++; closed = true; break }
                            else -> i++
                        }
                    }
                    result.add(
                        LineToken(
                            offset + start,
                            offset + i,
                            if (closed) TiBasicTokenTypes.STRING_LITERAL else TiBasicTokenTypes.PRINT_ARGUMENT
                        )
                    )
                }

                ch == '&' -> {
                    result.add(LineToken(offset + i, offset + i + 1, TiBasicTokenTypes.CONCAT_OP)); i++
                }

                ch == '+' -> {
                    result.add(LineToken(offset + i, offset + i + 1, TiBasicTokenTypes.PLUS_OP)); i++
                }

                ch == '-' -> {
                    result.add(LineToken(offset + i, offset + i + 1, TiBasicTokenTypes.MINUS_OP)); i++
                }

                ch == '*' -> {
                    result.add(LineToken(offset + i, offset + i + 1, TiBasicTokenTypes.MUL_OP)); i++
                }

                ch == '/' -> {
                    result.add(LineToken(offset + i, offset + i + 1, TiBasicTokenTypes.DIV_OP)); i++
                }

                ch == '^' -> {
                    result.add(LineToken(offset + i, offset + i + 1, TiBasicTokenTypes.POW_OP)); i++
                }

                ch == '=' -> {
                    result.add(LineToken(offset + i, offset + i + 1, TiBasicTokenTypes.EQ_OP)); i++
                }

                ch == '<' -> {
                    when {
                        i + 1 < argStr.length && argStr[i + 1] == '>' -> {
                            result.add(LineToken(offset + i, offset + i + 2, TiBasicTokenTypes.NEQ_OP)); i += 2
                        }

                        i + 1 < argStr.length && argStr[i + 1] == '=' -> {
                            result.add(LineToken(offset + i, offset + i + 2, TiBasicTokenTypes.LE_OP)); i += 2
                        }

                        else -> {
                            result.add(LineToken(offset + i, offset + i + 1, TiBasicTokenTypes.LT_OP)); i++
                        }
                    }
                }

                ch == '>' -> {
                    if (i + 1 < argStr.length && argStr[i + 1] == '=') {
                        result.add(LineToken(offset + i, offset + i + 2, TiBasicTokenTypes.GE_OP)); i += 2
                    } else {
                        result.add(LineToken(offset + i, offset + i + 1, TiBasicTokenTypes.GT_OP)); i++
                    }
                }

                ch == '(' -> {
                    result.add(LineToken(offset + i, offset + i + 1, TiBasicTokenTypes.LPAREN)); i++
                }

                ch == ')' -> {
                    result.add(LineToken(offset + i, offset + i + 1, TiBasicTokenTypes.RPAREN)); i++
                }

                ch == ',' -> {
                    result.add(LineToken(offset + i, offset + i + 1, TiBasicTokenTypes.COMMA)); i++
                }

                ch.isDigit() -> {
                    val start = i
                    while (i < argStr.length && argStr[i].isDigit()) i++
                    if (i < argStr.length && argStr[i] == '.') {
                        i++
                        while (i < argStr.length && argStr[i].isDigit()) i++
                    }
                    if (i < argStr.length && (argStr[i] == 'E' || argStr[i] == 'e')) {
                        i++
                        if (i < argStr.length && (argStr[i] == '+' || argStr[i] == '-')) i++
                        while (i < argStr.length && argStr[i].isDigit()) i++
                    }
                    val text = argStr.substring(start, i)
                    result.add(
                        LineToken(
                            offset + start,
                            offset + i,
                            if (NUMERIC_LITERAL.matches(text)) TiBasicTokenTypes.NUMERIC_LITERAL else TiBasicTokenTypes.PRINT_ARGUMENT
                        )
                    )
                }

                isVariableFirstChar(ch) -> {
                    val start = i++
                    while (i < argStr.length && (argStr[i].isLetterOrDigit() || argStr[i] == '@' || argStr[i] == '_')) i++
                    if (i < argStr.length && argStr[i] == '$') i++
                    val text = argStr.substring(start, i)
                    result.add(LineToken(offset + start, offset + i, classifyIdentifierToken(text)))
                }

                else -> {
                    result.add(LineToken(offset + i, offset + i + 1, TiBasicTokenTypes.PRINT_ARGUMENT)); i++
                }
            }
        }
        return result
    }

    private fun classifyIdentifierToken(text: String): IElementType {
        if (text.endsWith('$', ignoreCase = true))
            return if (VALID_VARIABLE_NAME.matches(text)) TiBasicTokenTypes.STRING_VARIABLE else TiBasicTokenTypes.INVALID_VARIABLE_NAME
        return if (VALID_NUMERIC_VARIABLE_NAME.matches(text)) TiBasicTokenTypes.NUMERIC_VARIABLE else TiBasicTokenTypes.INVALID_VARIABLE_NAME
    }

    private fun isVariableFirstChar(c: Char): Boolean =
        c.isLetter() || c == '@' || c == '[' || c == ']' || c == '\\' || c == '_'
}
