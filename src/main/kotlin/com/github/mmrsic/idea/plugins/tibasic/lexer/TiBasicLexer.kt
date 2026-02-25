package com.github.mmrsic.idea.plugins.tibasic.lexer

import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

/**
 * Lexer for TI-Basic source files.
 *
 * Every program line must begin with a line number (integer in 1..32767).
 * Lines are classified into four kinds:
 *  - VALID_STATEMENT   – line number + known keyword (PRINT, REM, BREAK, etc.)
 *  - LINE_NUMBER_ONLY  – just a line number with optional surrounding whitespace
 *  - UNKNOWN_STATEMENT – line number + unrecognised content
 *  - NO_LINE_NUMBER    – no leading line number (non-blank content)
 *
 * Newlines are emitted as WHITE_SPACE tokens so the parser can use them as line separators.
 */
class TiBasicLexer : LexerBase() {

    private companion object {
        val VALID_LINE =
            Regex(
                """^([ \t]*)(\d+)([ \t]+)(GOTO|GO[ \t]+TO|ON|IF|PRINT|BREAK|UNBREAK|TRACE|UNTRACE|DELETE|REM|LET|END|STOP)([ \t]*)(.*)$""",
                RegexOption.IGNORE_CASE
            )
        val LINE_NUMBER_ONLY = Regex("""^([ \t]*)(\d+)([ \t]*)$""")
        val IMPLICIT_LET_LINE = Regex("""^[ \t]*\d+[ \t]+[A-Za-z@\[\]\\_][A-Za-z0-9@_]*\$?(?:\([^)]*\))?[ \t]*=.*$""")
        val UNKNOWN_STATEMENT_LINE = Regex("""^([ \t]*)(\d+)([ \t]+)(\S.*)$""")
        val TRAILING_WS = Regex("""([ \t]*)$""")
        val VALID_VARIABLE_NAME = Regex("""^[A-Z@\[\]\\_][A-Z0-9@_]{0,13}\$$""", RegexOption.IGNORE_CASE)
        val VALID_NUMERIC_VARIABLE_NAME = Regex("""^[A-Za-z@\[\]\\_][A-Za-z0-9@_]{0,14}$""")
        val NUMERIC_LITERAL = Regex("""^\d+(\.\d+)?([Ee][+-]?\d+)?$""")
        val LINE_NUMBER_LIST_KEYWORDS = setOf("BREAK", "UNBREAK", "TRACE", "UNTRACE")
    }

    private enum class LineKind { VALID_STATEMENT, LINE_NUMBER_ONLY, LET_IMPLICIT_STATEMENT, UNKNOWN_STATEMENT, NO_LINE_NUMBER }

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
                LineKind.VALID_STATEMENT -> result.addAll(tokenizeValidLine(pos, VALID_LINE.find(lineText)!!))
                LineKind.LINE_NUMBER_ONLY -> result.addAll(tokenizeLineNumberOnlyLine(pos, LINE_NUMBER_ONLY.find(lineText)!!))
                LineKind.LET_IMPLICIT_STATEMENT -> result.addAll(tokenizeImplicitLetLine(pos, UNKNOWN_STATEMENT_LINE.find(lineText)!!))
                LineKind.UNKNOWN_STATEMENT -> result.addAll(tokenizeUnknownStatementLine(pos, UNKNOWN_STATEMENT_LINE.find(lineText)!!))
                LineKind.NO_LINE_NUMBER -> if (lineEnd > pos && lineText.isNotBlank()) result.add(
                    LineToken(
                        pos,
                        lineEnd,
                        TiBasicTokenTypes.NO_LINE_NUMBER_TEXT
                    )
                )
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
        if (VALID_LINE.containsMatchIn(lineText)) return LineKind.VALID_STATEMENT
        if (LINE_NUMBER_ONLY.containsMatchIn(lineText)) return LineKind.LINE_NUMBER_ONLY
        if (IMPLICIT_LET_LINE.containsMatchIn(lineText)) return LineKind.LET_IMPLICIT_STATEMENT
        if (UNKNOWN_STATEMENT_LINE.containsMatchIn(lineText)) return LineKind.UNKNOWN_STATEMENT
        return LineKind.NO_LINE_NUMBER
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
        val normalizedKeyword = printStr.trim().replace(Regex("""[ \t]+"""), " ").uppercase()
        val keywordType = when (normalizedKeyword) {
            in LINE_NUMBER_LIST_KEYWORDS -> TiBasicTokenTypes.LINE_NUMBER_LIST_KEYWORD
            "DELETE" -> TiBasicTokenTypes.DELETE_KEYWORD
            "REM" -> TiBasicTokenTypes.REM_KEYWORD
            "LET" -> TiBasicTokenTypes.LET_KEYWORD
            "END" -> TiBasicTokenTypes.END_KEYWORD
            "STOP" -> TiBasicTokenTypes.STOP_KEYWORD
            "GOTO", "GO TO" -> TiBasicTokenTypes.GOTO_KEYWORD
            "ON" -> TiBasicTokenTypes.ON_KEYWORD
            "IF" -> TiBasicTokenTypes.IF_KEYWORD
            else -> TiBasicTokenTypes.PRINT_KEYWORD
        }
        result.add(LineToken(offset, offset + printStr.length, keywordType))
        offset += printStr.length
        if (normalizedKeyword == "REM") {
            if (ws2.isNotEmpty()) {
                result.add(LineToken(offset, offset + ws2.length, TokenType.WHITE_SPACE))
                offset += ws2.length
            }
            if (argStr.isNotEmpty()) {
                result.add(LineToken(offset, offset + argStr.length, TiBasicTokenTypes.REM_TEXT))
                offset += argStr.length
            }
            if (trailingWsLength > 0) {
                result.add(LineToken(offset, offset + trailingWsLength, TokenType.WHITE_SPACE))
            }
        } else {
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
        }
        return result
    }

    private fun tokenizeLineNumberOnlyLine(lineStart: Int, match: MatchResult): List<LineToken> {
        val result = mutableListOf<LineToken>()
        var offset = lineStart
        val (leadingWs, numStr, trailingWs) = match.destructured
        if (leadingWs.isNotEmpty()) {
            result.add(LineToken(offset, offset + leadingWs.length, TokenType.WHITE_SPACE))
            offset += leadingWs.length
        }
        result.add(LineToken(offset, offset + numStr.length, TiBasicTokenTypes.LINE_NUMBER))
        offset += numStr.length
        if (trailingWs.isNotEmpty()) {
            result.add(LineToken(offset, offset + trailingWs.length, TokenType.WHITE_SPACE))
        }
        return result
    }

    private fun tokenizeUnknownStatementLine(lineStart: Int, match: MatchResult): List<LineToken> {
        val result = mutableListOf<LineToken>()
        var offset = lineStart
        val (leadingWs, numStr, ws1, statementText) = match.destructured
        if (leadingWs.isNotEmpty()) {
            result.add(LineToken(offset, offset + leadingWs.length, TokenType.WHITE_SPACE))
            offset += leadingWs.length
        }
        result.add(LineToken(offset, offset + numStr.length, TiBasicTokenTypes.LINE_NUMBER))
        offset += numStr.length
        result.add(LineToken(offset, offset + ws1.length, TokenType.WHITE_SPACE))
        offset += ws1.length
        val trailingWsLength = TRAILING_WS.find(statementText)!!.value.length
        val stmtText = statementText.dropLast(trailingWsLength)
        if (stmtText.isNotEmpty()) {
            result.add(LineToken(offset, offset + stmtText.length, TiBasicTokenTypes.UNKNOWN_STATEMENT_TEXT))
            offset += stmtText.length
        }
        if (trailingWsLength > 0) {
            result.add(LineToken(offset, offset + trailingWsLength, TokenType.WHITE_SPACE))
        }
        return result
    }


    private fun tokenizeImplicitLetLine(lineStart: Int, match: MatchResult): List<LineToken> {
        val result = mutableListOf<LineToken>()
        var offset = lineStart
        val (leadingWs, numStr, ws1, statementText) = match.destructured
        if (leadingWs.isNotEmpty()) {
            result.add(LineToken(offset, offset + leadingWs.length, TokenType.WHITE_SPACE))
            offset += leadingWs.length
        }
        result.add(LineToken(offset, offset + numStr.length, TiBasicTokenTypes.LINE_NUMBER))
        offset += numStr.length
        result.add(LineToken(offset, offset + ws1.length, TokenType.WHITE_SPACE))
        offset += ws1.length
        val trailingWsLength = TRAILING_WS.find(statementText)!!.value.length
        val stmtText = statementText.dropLast(trailingWsLength)
        if (stmtText.isNotEmpty()) {
            result.addAll(tokenizeArgument(offset, stmtText))
            offset += stmtText.length
        }
        if (trailingWsLength > 0) {
            result.add(LineToken(offset, offset + trailingWsLength, TokenType.WHITE_SPACE))
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
                            argStr[i] == '"' -> {
                                i++; closed = true; break
                            }

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
                    val upperText = text.uppercase()
                    when {
                        upperText == "GOTO" ->
                            result.add(LineToken(offset + start, offset + i, TiBasicTokenTypes.GOTO_KEYWORD))

                        upperText == "THEN" ->
                            result.add(LineToken(offset + start, offset + i, TiBasicTokenTypes.THEN_KEYWORD))

                        upperText == "ELSE" ->
                            result.add(LineToken(offset + start, offset + i, TiBasicTokenTypes.ELSE_KEYWORD))

                        upperText == "GO" -> {
                            val endOfGoTo = goToEnd(argStr, i)
                            if (endOfGoTo >= 0) {
                                result.add(LineToken(offset + start, offset + endOfGoTo, TiBasicTokenTypes.GOTO_KEYWORD))
                                i = endOfGoTo
                            } else {
                                result.add(LineToken(offset + start, offset + i, classifyIdentifierToken(text)))
                            }
                        }

                        else -> result.add(LineToken(offset + start, offset + i, classifyIdentifierToken(text)))
                    }
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

    /** Returns end position of "GO TO" in [s] starting at [i] (pointing just after "GO"), or -1 if not found. */
    private fun goToEnd(s: String, i: Int): Int {
        var j = i
        if (j >= s.length || !s[j].isWhitespace()) return -1
        while (j < s.length && s[j].isWhitespace()) j++
        if (j + 2 > s.length || !s.substring(j, j + 2).equals("TO", ignoreCase = true)) return -1
        val afterTo = j + 2
        if (afterTo < s.length && (s[afterTo].isLetterOrDigit() || s[afterTo] == '_' || s[afterTo] == '@')) return -1
        return afterTo
    }

    private fun isVariableFirstChar(c: Char): Boolean =
        c.isLetter() || c == '@' || c == '[' || c == ']' || c == '\\' || c == '_'
}
