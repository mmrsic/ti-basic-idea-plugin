package com.github.mmrsic.idea.plugins.tibasic

import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TiBasicSyntaxHighlightingTest : BasePlatformTestCase() {

    // ── full-buffer lexing ──────────────────────────────────────────────────

    fun testPrintKeywordTokenInValidLine() {
        val lexer = TiBasicLexer()
        lexer.start("100 PRINT")
        assertEquals(TiBasicTokenTypes.LINE_NUMBER, lexer.tokenType)
        lexer.advance()
        assertEquals(TokenType.WHITE_SPACE, lexer.tokenType)
        lexer.advance()
        assertEquals(TiBasicTokenTypes.PRINT_KEYWORD, lexer.tokenType)
        assertEquals("PRINT", lexer.tokenSequence.toString())
    }

    fun testInvalidLineProducesCommentToken() {
        val lexer = TiBasicLexer()
        lexer.start("X")
        assertEquals(TiBasicTokenTypes.COMMENT, lexer.tokenType)
        assertEquals("X", lexer.tokenSequence.toString())
    }

    fun testLeadingWhitespaceOnValidLineEmittedAsWhiteSpaceToken() {
        val lexer = TiBasicLexer()
        lexer.start("  100 PRINT")
        assertEquals(TokenType.WHITE_SPACE, lexer.tokenType)
        assertEquals("  ", lexer.tokenSequence.toString())
        lexer.advance()
        assertEquals(TiBasicTokenTypes.LINE_NUMBER, lexer.tokenType)
    }

    fun testTrailingWhitespaceOnValidLineEmittedAsWhiteSpaceToken() {
        val lexer = TiBasicLexer()
        lexer.start("100 PRINT   ")
        // advance past LINE_NUMBER, WHITE_SPACE, PRINT_KEYWORD
        lexer.advance(); lexer.advance(); lexer.advance()
        assertEquals(TokenType.WHITE_SPACE, lexer.tokenType)
        assertEquals("   ", lexer.tokenSequence.toString())
        lexer.advance()
        assertNull(lexer.tokenType)
    }

    fun testLeadingAndTrailingWhitespaceOnValidLine() {
        val lexer = TiBasicLexer()
        lexer.start("\t100 PRINT \"X\" \t")
        assertEquals(TokenType.WHITE_SPACE, lexer.tokenType)
        lexer.advance()
        assertEquals(TiBasicTokenTypes.LINE_NUMBER, lexer.tokenType)
        // consume WHITE_SPACE, PRINT_KEYWORD, WHITE_SPACE, PRINT_ARGUMENT, then reach trailing WHITE_SPACE
        lexer.advance(); lexer.advance(); lexer.advance(); lexer.advance(); lexer.advance()
        assertEquals(TokenType.WHITE_SPACE, lexer.tokenType)
        assertEquals(" \t", lexer.tokenSequence.toString())
    }

    // Simulates IntelliJ restarting the lexer at a token boundary mid-line.
    // Buffer: "100 PRINT \"hello\""
    //          0123456789012345678
    //          LINE_NR WS PRINT WS STRING_LITERAL
    //          [0,3)  [3,4) [4,9) [9,10) [10,17)

    fun testRestartAtPrintKeywordPositionYieldsPrintKeywordFirst() {
        val buffer = "100 PRINT \"hello\""
        val lexer = TiBasicLexer()
        lexer.start(buffer, 4, buffer.length, 0)
        assertEquals(TiBasicTokenTypes.PRINT_KEYWORD, lexer.tokenType)
        assertEquals("PRINT", lexer.tokenSequence.toString())
    }

    fun testRestartAtWhiteSpaceAfterPrintKeywordYieldsWhiteSpaceFirst() {
        val buffer = "100 PRINT \"hello\""
        val lexer = TiBasicLexer()
        lexer.start(buffer, 9, buffer.length, 0)
        assertEquals(TokenType.WHITE_SPACE, lexer.tokenType)
    }

    fun testRestartAtStringLiteralPositionYieldsStringLiteralFirst() {
        val buffer = "100 PRINT \"hello\""
        val lexer = TiBasicLexer()
        lexer.start(buffer, 10, buffer.length, 0)
        assertEquals(TiBasicTokenTypes.STRING_LITERAL, lexer.tokenType)
        assertEquals("\"hello\"", lexer.tokenSequence.toString())
    }

    fun testRestartAtStringLiteralPositionDoesNotProduceCommentToken() {
        val buffer = "100 PRINT \"hello\""
        val lexer = TiBasicLexer()
        lexer.start(buffer, 10, buffer.length, 0)
        assertNotEquals(TiBasicTokenTypes.COMMENT, lexer.tokenType)
    }

    fun testRestartMidLineTokensFollowCorrectly() {
        // After restarting at PRINT_KEYWORD, the subsequent tokens must be WS then STRING_LITERAL
        val buffer = "100 PRINT \"hello\""
        val lexer = TiBasicLexer()
        lexer.start(buffer, 4, buffer.length, 0)
        assertEquals(TiBasicTokenTypes.PRINT_KEYWORD, lexer.tokenType)
        lexer.advance()
        assertEquals(TokenType.WHITE_SPACE, lexer.tokenType)
        lexer.advance()
        assertEquals(TiBasicTokenTypes.STRING_LITERAL, lexer.tokenType)
        lexer.advance()
        assertNull(lexer.tokenType)
    }

    fun testRestartMidSecondLineYieldsCorrectTokens() {
        // Buffer with two lines; restart at PRINT_KEYWORD of line 2.
        // "100 PRINT \"a\"\n200 PRINT \"b\""
        //  [0,13)=line1  13=\n  [14,27)=line2
        // Line 2: "200 PRINT \"b\""
        //          14  18    24
        val buffer = "100 PRINT \"a\"\n200 PRINT \"b\""
        val printKeywordLine2 = 18
        val lexer = TiBasicLexer()
        lexer.start(buffer, printKeywordLine2, buffer.length, 0)
        assertEquals(TiBasicTokenTypes.PRINT_KEYWORD, lexer.tokenType)
        lexer.advance()
        assertEquals(TokenType.WHITE_SPACE, lexer.tokenType)
        lexer.advance()
        assertEquals(TiBasicTokenTypes.STRING_LITERAL, lexer.tokenType)
        assertEquals("\"b\"", lexer.tokenSequence.toString())
    }

    fun testRestartAtArgumentOfSecondLineDoesNotProduceComment() {
        // Restart at the STRING_LITERAL token of line 2 – must not become COMMENT.
        val buffer = "100 PRINT \"a\"\n200 PRINT \"b\""
        val stringLiteralLine2 = 24
        val lexer = TiBasicLexer()
        lexer.start(buffer, stringLiteralLine2, buffer.length, 0)
        assertEquals(TiBasicTokenTypes.STRING_LITERAL, lexer.tokenType)
        assertEquals("\"b\"", lexer.tokenSequence.toString())
    }

    // ── syntax highlighter attribute mapping ───────────────────────────────

    fun testSyntaxHighlighterReturnKeywordAttributeForPrintKeywordToken() {
        val highlighter = TiBasicSyntaxHighlighter()
        val attributes = highlighter.getTokenHighlights(TiBasicTokenTypes.PRINT_KEYWORD)
        assertTrue("PRINT_KEYWORD token type must map to at least one TextAttributesKey", attributes.isNotEmpty())
        assertEquals(TiBasicSyntaxHighlighter.KEYWORD, attributes[0])
    }

    fun testSyntaxHighlighterReturnCommentAttributeForCommentToken() {
        val highlighter = TiBasicSyntaxHighlighter()
        val attributes = highlighter.getTokenHighlights(TiBasicTokenTypes.COMMENT)
        assertTrue("COMMENT token type must map to at least one TextAttributesKey", attributes.isNotEmpty())
        assertEquals(TiBasicSyntaxHighlighter.COMMENT, attributes[0])
    }

    fun testSyntaxHighlighterReturnStringLiteralAttributeForStringLiteralToken() {
        val highlighter = TiBasicSyntaxHighlighter()
        val attributes = highlighter.getTokenHighlights(TiBasicTokenTypes.STRING_LITERAL)
        assertTrue("STRING_LITERAL token type must map to at least one TextAttributesKey", attributes.isNotEmpty())
        assertEquals(TiBasicSyntaxHighlighter.STRING_LITERAL, attributes[0])
    }

    private fun assertNotEquals(unexpected: IElementType, actual: IElementType?) {
        assertFalse("Expected token type to differ from $unexpected", actual == unexpected)
    }
}
