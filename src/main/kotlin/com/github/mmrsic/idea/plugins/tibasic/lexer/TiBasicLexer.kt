package com.github.mmrsic.idea.plugins.tibasic.lexer

import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicBuiltInFunctions
import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

/**
 * Lexer for TI-Basic source files.
 *
 * Every program line must begin with a line number (integer in 1..32767).
 * Lines are classified into five kinds:
 *  - VALID_STATEMENT        – line number + known keyword (PRINT, INPUT, READ, DATA, RESTORE, REM, BREAK, etc.)
 *  - LINE_NUMBER_ONLY       – just a line number with optional surrounding whitespace
 *  - LET_IMPLICIT_STATEMENT – line number + variable assignment without leading LET keyword
 *  - UNKNOWN_STATEMENT      – line number + unrecognised content
 *  - NO_LINE_NUMBER         – no leading line number (non-blank content)
 *
 * Newlines are emitted as WHITE_SPACE tokens so the parser can use them as line separators.
 */
class TiBasicLexer : LexerBase() {

    private companion object {
        val VALID_LINE =
            Regex(
                """^([ \t]*)(\d+)([ \t]+)(GOTO|GO[ \t]+TO|GOSUB|GO[ \t]+SUB|ON|IF|FOR|NEXT|PRINT|DISPLAY|INPUT|READ|DATA|RESTORE|RETURN|BREAK|UNBREAK|TRACE|UNTRACE|DELETE|REM|LET|END|STOP|CALL|RANDOMIZE|DEF|DIM|OPTION[ \t]+BASE|OPEN|CLOSE)(?![A-Za-z0-9])([ \t]*)(.*)$""",
                RegexOption.IGNORE_CASE
            )
        val LINE_NUMBER_ONLY = Regex("""^([ \t]*)(\d+)([ \t]*)$""")
        val IMPLICIT_LET_LINE = Regex("""^[ \t]*\d+[ \t]+[A-Za-z@\[\]\\_][A-Za-z0-9@_]*\$?(?:[ \t]*\([^)]*\))?[ \t]*=.*$""")
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
                LineKind.NO_LINE_NUMBER -> if (lineEnd > pos) result.add(
                    LineToken(
                        pos,
                        lineEnd,
                        if (lineText.isNotBlank()) TiBasicTokenTypes.NO_LINE_NUMBER_TEXT else TokenType.WHITE_SPACE,
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
        val (leadingWs, numStr, ws1, printStr, ws2, afterPrint) = match.destructured
        val trailingWsLength = TRAILING_WS.find(afterPrint)!!.value.length
        val argStr = afterPrint.dropLast(trailingWsLength)
        var offset = appendOptionalWhitespace(result, lineStart, leadingWs)
        offset = appendToken(result, offset, numStr, TiBasicTokenTypes.LINE_NUMBER)
        offset = appendOptionalWhitespace(result, offset, ws1)
        val normalizedKeyword = printStr.trim().replace(Regex("""[ \t]+"""), " ").uppercase()
        val keywordType = when (normalizedKeyword) {
            in LINE_NUMBER_LIST_KEYWORDS -> TiBasicTokenTypes.LINE_NUMBER_LIST_KEYWORD
            "DELETE" -> TiBasicTokenTypes.DELETE_KEYWORD
            "REM" -> TiBasicTokenTypes.REM_KEYWORD
            "LET" -> TiBasicTokenTypes.LET_KEYWORD
            "END" -> TiBasicTokenTypes.END_KEYWORD
            "STOP" -> TiBasicTokenTypes.STOP_KEYWORD
            "GOTO", "GO TO" -> TiBasicTokenTypes.GOTO_KEYWORD
            "GOSUB", "GO SUB" -> TiBasicTokenTypes.GOSUB_KEYWORD
            "RETURN" -> TiBasicTokenTypes.RETURN_KEYWORD
            "ON" -> TiBasicTokenTypes.ON_KEYWORD
            "IF" -> TiBasicTokenTypes.IF_KEYWORD
            "FOR" -> TiBasicTokenTypes.FOR_KEYWORD
            "NEXT" -> TiBasicTokenTypes.NEXT_KEYWORD
            "INPUT" -> TiBasicTokenTypes.INPUT_KEYWORD
            "READ" -> TiBasicTokenTypes.READ_KEYWORD
            "DATA" -> TiBasicTokenTypes.DATA_KEYWORD
            "RESTORE" -> TiBasicTokenTypes.RESTORE_KEYWORD
            "DISPLAY" -> TiBasicTokenTypes.DISPLAY_KEYWORD
            "CALL" -> TiBasicTokenTypes.CALL_KEYWORD
            "RANDOMIZE" -> TiBasicTokenTypes.RANDOMIZE_KEYWORD
            "DEF" -> TiBasicTokenTypes.DEF_KEYWORD
            "DIM" -> TiBasicTokenTypes.DIM_KEYWORD
            "OPTION BASE" -> TiBasicTokenTypes.OPTION_BASE_KEYWORD
            "OPEN" -> TiBasicTokenTypes.OPEN_KEYWORD
            "CLOSE" -> TiBasicTokenTypes.CLOSE_KEYWORD
            else -> TiBasicTokenTypes.PRINT_KEYWORD
        }
        offset = appendToken(result, offset, printStr, keywordType)
        offset = appendOptionalWhitespace(result, offset, ws2)
        if (argStr.isNotEmpty()) {
            when (normalizedKeyword) {
                "REM" -> offset = appendToken(result, offset, argStr, TiBasicTokenTypes.REM_TEXT)
                "DATA" -> {
                    result.addAll(tokenizeDataContent(offset, argStr))
                    offset += argStr.length
                }

                "CALL" -> {
                    result.addAll(tokenizeCallArguments(offset, argStr))
                    offset += argStr.length
                }

                else -> {
                    result.addAll(tokenizeArgument(offset, argStr))
                    offset += argStr.length
                }
            }
        }
        appendOptionalWhitespace(result, offset, afterPrint.takeLast(trailingWsLength))
        return result
    }

    private fun tokenizeLineNumberOnlyLine(lineStart: Int, match: MatchResult): List<LineToken> {
        val result = mutableListOf<LineToken>()
        val (leadingWs, numStr, trailingWs) = match.destructured
        var offset = appendOptionalWhitespace(result, lineStart, leadingWs)
        offset = appendToken(result, offset, numStr, TiBasicTokenTypes.LINE_NUMBER)
        appendOptionalWhitespace(result, offset, trailingWs)
        return result
    }

    private fun tokenizeUnknownStatementLine(lineStart: Int, match: MatchResult): List<LineToken> =
        tokenizeStatementContent(lineStart, match) { offset, text ->
            listOf(LineToken(offset, offset + text.length, TiBasicTokenTypes.UNKNOWN_STATEMENT_TEXT))
        }

    private fun tokenizeImplicitLetLine(lineStart: Int, match: MatchResult): List<LineToken> =
        tokenizeStatementContent(lineStart, match, ::tokenizeArgument)

    private fun tokenizeStatementContent(
        lineStart: Int,
        match: MatchResult,
        tokenizeContent: (Int, String) -> List<LineToken>,
    ): List<LineToken> {
        val result = mutableListOf<LineToken>()
        val (leadingWs, numStr, ws1, statementText) = match.destructured
        var offset = appendOptionalWhitespace(result, lineStart, leadingWs)
        offset = appendToken(result, offset, numStr, TiBasicTokenTypes.LINE_NUMBER)
        offset = appendToken(result, offset, ws1, TokenType.WHITE_SPACE)
        val trailingWsLength = TRAILING_WS.find(statementText)!!.value.length
        val stmtText = statementText.dropLast(trailingWsLength)
        if (stmtText.isNotEmpty()) {
            result.addAll(tokenizeContent(offset, stmtText))
            offset += stmtText.length
        }
        appendOptionalWhitespace(result, offset, statementText.takeLast(trailingWsLength))
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
                    val token = tokenizeStringLiteral(argStr, offset, i)
                    result.add(token); i = token.end - offset
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

                ch == '<' -> when {
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

                ch == '>' -> when {
                    i + 1 < argStr.length && argStr[i + 1] == '=' -> {
                        result.add(LineToken(offset + i, offset + i + 2, TiBasicTokenTypes.GE_OP)); i += 2
                    }

                    else -> {
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

                ch == ':' -> {
                    result.add(LineToken(offset + i, offset + i + 1, TiBasicTokenTypes.COLON)); i++
                }

                ch == ';' -> {
                    result.add(LineToken(offset + i, offset + i + 1, TiBasicTokenTypes.SEMICOLON)); i++
                }

                ch == '#' -> {
                    result.add(LineToken(offset + i, offset + i + 1, TiBasicTokenTypes.HASH)); i++
                }

                ch == '.' && (i + 1 >= argStr.length || !argStr[i + 1].isDigit()) -> {
                    result.add(LineToken(offset + i, offset + i + 1, TiBasicTokenTypes.DOT)); i++
                }

                ch.isDigit() || (ch == '.' && i + 1 < argStr.length && argStr[i + 1].isDigit()) -> {
                    val token = tokenizeNumber(argStr, offset, i)
                    result.add(token); i = token.end - offset
                }

                isVariableFirstChar(ch) -> {
                    val token = tokenizeIdentifier(argStr, offset, i)
                    result.add(token); i = token.end - offset
                }

                else -> {
                    result.add(LineToken(offset + i, offset + i + 1, TiBasicTokenTypes.PRINT_ARGUMENT)); i++
                }
            }
        }
        return result
    }

    private fun tokenizeCallArguments(offset: Int, argStr: String): List<LineToken> {
        val result = mutableListOf<LineToken>()
        var i = 0
        while (i < argStr.length && argStr[i].isWhitespace()) i++
        if (i > 0) result.add(LineToken(offset, offset + i, TokenType.WHITE_SPACE))
        if (i < argStr.length && isVariableFirstChar(argStr[i])) {
            val nameStart = i
            i++
            while (i < argStr.length && (argStr[i].isLetterOrDigit() || argStr[i] in "@_")) i++
            result.add(LineToken(offset + nameStart, offset + i, TiBasicTokenTypes.CALL_SUBPROGRAM_NAME))
        }
        if (i < argStr.length) result.addAll(tokenizeArgument(offset + i, argStr.substring(i)))
        return result
    }

    private fun tokenizeStringLiteral(s: String, offset: Int, start: Int): LineToken {
        var i = start + 1
        var closed = false
        while (i < s.length) {
            when {
                s[i] == '"' && i + 1 < s.length && s[i + 1] == '"' -> i += 2
                s[i] == '"' -> {
                    i++; closed = true; break
                }

                else -> i++
            }
        }
        return LineToken(
            offset + start,
            offset + i,
            if (closed) TiBasicTokenTypes.STRING_LITERAL else TiBasicTokenTypes.PRINT_ARGUMENT,
        )
    }

    private fun tokenizeNumber(s: String, offset: Int, start: Int): LineToken {
        var i = start
        val startsWithDot = s[i] == '.'
        if (startsWithDot) {
            i++
            while (i < s.length && s[i].isDigit()) i++
        } else {
            while (i < s.length && s[i].isDigit()) i++
            if (i < s.length && s[i] == '.' && i + 1 < s.length && s[i + 1].isDigit()) {
                i++
                while (i < s.length && s[i].isDigit()) i++
            }
        }
        if (i < s.length && s[i] in "Ee") {
            i++
            if (i < s.length && s[i] in "+-") i++
            while (i < s.length && s[i].isDigit()) i++
        }
        val text = s.substring(start, i)
        return LineToken(
            offset + start,
            offset + i,
            if (startsWithDot || NUMERIC_LITERAL.matches(text)) TiBasicTokenTypes.NUMERIC_LITERAL else TiBasicTokenTypes.PRINT_ARGUMENT,
        )
    }

    private fun tokenizeIdentifier(s: String, offset: Int, start: Int): LineToken {
        var i = start + 1
        while (i < s.length && (s[i].isLetterOrDigit() || s[i] in "@_")) i++
        if (i < s.length && s[i] == '$') i++
        val text = s.substring(start, i)
        val end: Int
        val type: IElementType
        when (text.uppercase()) {
            "GOTO" -> {
                end = i; type = TiBasicTokenTypes.GOTO_KEYWORD
            }

            "GOSUB" -> {
                end = i; type = TiBasicTokenTypes.GOSUB_KEYWORD
            }

            "RETURN" -> {
                end = i; type = TiBasicTokenTypes.RETURN_KEYWORD
            }

            "THEN" -> {
                end = i; type = TiBasicTokenTypes.THEN_KEYWORD
            }

            "ELSE" -> {
                end = i; type = TiBasicTokenTypes.ELSE_KEYWORD
            }

            "TAB" -> {
                end = i; type = TiBasicTokenTypes.TAB_KEYWORD
            }

            "TO" -> {
                end = i; type = TiBasicTokenTypes.TO_KEYWORD
            }

            "STEP" -> {
                end = i; type = TiBasicTokenTypes.STEP_KEYWORD
            }

            "DISPLAY" -> {
                end = i; type = TiBasicTokenTypes.DISPLAY_KEYWORD
            }

            "INPUT" -> {
                end = i; type = TiBasicTokenTypes.INPUT_KEYWORD
            }

            "REC" -> {
                end = i; type = TiBasicTokenTypes.REC_KEYWORD
            }

            "SEQUENTIAL" -> {
                end = i; type = TiBasicTokenTypes.SEQUENTIAL_KEYWORD
            }

            "RELATIVE" -> {
                end = i; type = TiBasicTokenTypes.RELATIVE_KEYWORD
            }

            "INTERNAL" -> {
                end = i; type = TiBasicTokenTypes.INTERNAL_KEYWORD
            }

            "OUTPUT" -> {
                end = i; type = TiBasicTokenTypes.OUTPUT_KEYWORD
            }

            "APPEND" -> {
                end = i; type = TiBasicTokenTypes.APPEND_KEYWORD
            }

            "UPDATE" -> {
                end = i; type = TiBasicTokenTypes.UPDATE_KEYWORD
            }

            "FIXED" -> {
                end = i; type = TiBasicTokenTypes.FIXED_KEYWORD
            }

            "VARIABLE" -> {
                end = i; type = TiBasicTokenTypes.VARIABLE_KEYWORD
            }

            "PERMANENT" -> {
                end = i; type = TiBasicTokenTypes.PERMANENT_KEYWORD
            }

            "DELETE" -> {
                end = i; type = TiBasicTokenTypes.DELETE_KEYWORD
            }

            "GO" -> {
                val endOfGoTo = goToEnd(s, i)
                if (endOfGoTo >= 0) {
                    end = endOfGoTo; type = TiBasicTokenTypes.GOTO_KEYWORD
                } else {
                    val endOfGoSub = goSubEnd(s, i)
                    if (endOfGoSub >= 0) {
                        end = endOfGoSub; type = TiBasicTokenTypes.GOSUB_KEYWORD
                    } else {
                        end = i; type = classifyIdentifierToken(text)
                    }
                }
            }

            else -> {
                end = i
                type = when (text.uppercase()) {
                    in TiBasicBuiltInFunctions.numericFunctionNames() -> TiBasicTokenTypes.NUMERIC_FUNCTION_KEYWORD
                    in TiBasicBuiltInFunctions.stringFunctionNames() -> TiBasicTokenTypes.STRING_FUNCTION_KEYWORD
                    else -> classifyIdentifierToken(text)
                }
            }
        }
        return LineToken(offset + start, offset + end, type)
    }

    private fun tokenizeDataContent(offset: Int, content: String): List<LineToken> {
        val result = mutableListOf<LineToken>()
        var i = 0
        while (i <= content.length) {
            val wsStart = i
            while (i < content.length && content[i].isWhitespace()) i++
            if (i > wsStart) result.add(LineToken(offset + wsStart, offset + i, TokenType.WHITE_SPACE))
            if (i >= content.length) break
            when {
                content[i] == ',' -> {
                    result.add(LineToken(offset + i, offset + i + 1, TiBasicTokenTypes.COMMA))
                    i++
                }

                content[i] == '"' -> {
                    val token = tokenizeStringLiteral(content, offset, i)
                    result.add(token)
                    i = token.end - offset
                }

                else -> {
                    val start = i
                    while (i < content.length && content[i] != ',') i++
                    val text = content.substring(start, i)
                    val trimmed = text.trimEnd()
                    val tokenEnd = start + trimmed.length
                    if (trimmed.isNotEmpty()) {
                        val type = if (NUMERIC_LITERAL.matches(trimmed)) TiBasicTokenTypes.NUMERIC_LITERAL
                        else TiBasicTokenTypes.PRINT_ARGUMENT
                        result.add(LineToken(offset + start, offset + tokenEnd, type))
                    }
                    if (tokenEnd < i) result.add(LineToken(offset + tokenEnd, offset + i, TokenType.WHITE_SPACE))
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
        if (afterTo < s.length && (s[afterTo].isLetterOrDigit() || s[afterTo] in "_@")) return -1
        return afterTo
    }

    /** Returns end position of "GO SUB" in [s] starting at [i] (pointing just after "GO"), or -1 if not found. */
    private fun goSubEnd(s: String, i: Int): Int {
        var j = i
        if (j >= s.length || !s[j].isWhitespace()) return -1
        while (j < s.length && s[j].isWhitespace()) j++
        if (j + 3 > s.length || !s.substring(j, j + 3).equals("SUB", ignoreCase = true)) return -1
        val afterSub = j + 3
        if (afterSub < s.length && (s[afterSub].isLetterOrDigit() || s[afterSub] in "_@")) return -1
        return afterSub
    }

    private fun isVariableFirstChar(c: Char): Boolean =
        c.isLetter() || c in "@[]\\_"

    private fun appendToken(
        result: MutableList<LineToken>,
        offset: Int,
        str: String,
        type: IElementType,
    ): Int {
        result.add(LineToken(offset, offset + str.length, type))
        return offset + str.length
    }

    private fun appendOptionalWhitespace(result: MutableList<LineToken>, offset: Int, ws: String): Int =
        if (ws.isEmpty()) offset else appendToken(result, offset, ws, TokenType.WHITE_SPACE)
}
