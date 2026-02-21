package com.github.mmrsic.idea.plugins.tibasic

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TiBasicSyntaxHighlightingTest : BasePlatformTestCase() {
    fun testPrintTokenIsClassifiedAsKeyword() {
        val lexer = TiBasicLexer()
        lexer.start("PRINT 123")
        assertEquals(TiBasicTokenTypes.KEYWORD, lexer.tokenType)
        assertEquals("PRINT", lexer.tokenSequence.toString())
    }

    fun testIdentifierTokenIsClassifiedAsIdentifier() {
        val lexer = TiBasicLexer()
        lexer.start("X")
        assertEquals(TiBasicTokenTypes.IDENTIFIER, lexer.tokenType)
    }

    fun testSyntaxHighlighterReturnKeywordAttributeForPrintToken() {
        val highlighter = TiBasicSyntaxHighlighter()
        val attributes = highlighter.getTokenHighlights(TiBasicTokenTypes.KEYWORD)
        assertTrue("KEYWORD token type must map to at least one TextAttributesKey", attributes.isNotEmpty())
        assertEquals(TiBasicSyntaxHighlighter.KEYWORD, attributes[0])
    }
}
