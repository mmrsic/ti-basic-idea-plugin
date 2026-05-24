package com.github.mmrsic.idea.plugins.tibasic.editor

import com.intellij.lang.BracePair
import junit.framework.TestCase
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.lexer.TiBasicTokenTypes

class TiBasicBraceMatcherTest : TestCase() {

    private val braceMatcher = TiBasicBraceMatcher()

    fun `test getPairs returns single pair`() {
        assertEquals(1, braceMatcher.getPairs().size)
    }

    fun `test bracket pair uses LPAREN and RPAREN`() {
        val pair: BracePair = braceMatcher.getPairs()[0]
        assertEquals(TiBasicTokenTypes.LPAREN, pair.leftBraceType)
        assertEquals(TiBasicTokenTypes.RPAREN, pair.rightBraceType)
    }

    fun `test bracket pair is not structural`() {
        val pair: BracePair = braceMatcher.getPairs()[0]
        assertFalse(pair.isStructural)
    }

    fun `test isPairedBracesAllowedBeforeType returns true for LPAREN`() {
        assertTrue(braceMatcher.isPairedBracesAllowedBeforeType(TiBasicTokenTypes.LPAREN, null))
    }

    fun `test isPairedBracesAllowedBeforeType returns true for RPAREN`() {
        assertTrue(braceMatcher.isPairedBracesAllowedBeforeType(TiBasicTokenTypes.RPAREN, null))
    }
}
