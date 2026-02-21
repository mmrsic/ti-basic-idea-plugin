package com.github.mmrsic.idea.plugins.tibasic

import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType
import com.intellij.psi.TokenType

class TiBasicLexer : LexerBase() {
    private var buffer: CharSequence = ""
    private var startOffset = 0
    private var endOffset = 0
    private var tokenStart = 0
    private var tokenEnd = 0
    private var tokenType: IElementType? = null

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.startOffset = startOffset
        this.endOffset = endOffset
        this.tokenStart = startOffset
        this.tokenEnd = startOffset
        this.tokenType = null
        advance()
    }

    override fun getState(): Int = 0

    override fun getTokenType(): IElementType? = tokenType

    override fun getTokenStart(): Int = tokenStart

    override fun getTokenEnd(): Int = tokenEnd

    override fun advance() {
        tokenStart = tokenEnd
        if (tokenStart >= endOffset) {
            tokenType = null
            return
        }
        val text = buffer.subSequence(tokenStart, endOffset).toString()
        val wsMatch = Regex("\\s+").find(text)
        if (wsMatch != null && wsMatch.range.first == 0) {
            tokenEnd = tokenStart + wsMatch.value.length
            tokenType = TokenType.WHITE_SPACE
            return
        }
        val wordMatch = Regex("[A-Za-z]+|\\d+").find(text)
        if (wordMatch != null && wordMatch.range.first == 0) {
            tokenEnd = tokenStart + wordMatch.value.length
            tokenType = if (wordMatch.value.equals("PRINT", ignoreCase = true)) TiBasicTokenTypes.KEYWORD else TiBasicTokenTypes.IDENTIFIER
            return
        }
        tokenEnd = tokenStart + 1
        tokenType = TokenType.BAD_CHARACTER
    }

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = endOffset
}
