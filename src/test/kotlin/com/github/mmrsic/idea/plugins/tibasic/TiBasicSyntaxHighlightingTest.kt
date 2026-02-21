package com.github.mmrsic.idea.plugins.tibasic

import com.intellij.psi.TokenType
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TiBasicSyntaxHighlightingTest : BasePlatformTestCase() {

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
}
