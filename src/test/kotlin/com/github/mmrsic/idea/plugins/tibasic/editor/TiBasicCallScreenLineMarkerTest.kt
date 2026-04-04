package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase

class TiBasicCallScreenLineMarkerTest : TiBasicTestBase() {

    fun `test gutter icon appears for CALL SCREEN with literal color argument`() {
        configureFile("100 CALL SCREEN(2)")
        val gutters = myFixture.findAllGutters()
        assertEquals("Exactly one gutter icon must appear for CALL SCREEN", 1, gutters.size)
    }

    fun `test gutter icon tooltip shows correct color name for CALL SCREEN`() {
        configureFile("100 CALL SCREEN(5)")
        val gutters = myFixture.findAllGutters()
        assertEquals(1, gutters.size)
        val tooltip = gutters[0].tooltipText ?: ""
        assertTrue("Tooltip must contain 'CALL SCREEN'", tooltip.contains("CALL SCREEN"))
        assertTrue("Tooltip must contain the resolved color name", tooltip.contains("DarkBlue"))
    }

    fun `test gutter icon appears for CALL SCREEN with constant numeric variable`() {
        configureFile("100 LET C=7\n110 CALL SCREEN(C)")
        val gutters = myFixture.findAllGutters()
        assertEquals("Gutter icon must appear for CALL SCREEN with constant numeric variable", 1, gutters.size)
        val tooltip = gutters[0].tooltipText ?: ""
        assertTrue("Tooltip must show color resolved from constant variable", tooltip.contains("DarkRed"))
    }

    fun `test gutter icon shows Transparent for CALL SCREEN with non-constant variable`() {
        configureFile("100 INPUT C\n110 CALL SCREEN(C)")
        val gutters = myFixture.findAllGutters()
        assertEquals("Gutter icon must appear even when variable is non-constant", 1, gutters.size)
        val tooltip = gutters[0].tooltipText ?: ""
        assertTrue("Non-constant variable must resolve to Transparent", tooltip.contains("Transparent"))
    }

    fun `test gutter icon shows Transparent for CALL SCREEN with out-of-range color`() {
        configureFile("100 CALL SCREEN(99)")
        val gutters = myFixture.findAllGutters()
        assertEquals(1, gutters.size)
        val tooltip = gutters[0].tooltipText ?: ""
        assertTrue("Out-of-range color must show Transparent", tooltip.contains("Transparent"))
    }

    fun `test gutter icon does not appear for CALL CHAR`() {
        configureFile("100 CALL CHAR(96,\"0000000000000000\")")
        val gutters = myFixture.findAllGutters()
        assertEquals("CALL CHAR must not produce a SCREEN gutter icon", 1, gutters.size)
        val tooltip = gutters[0].tooltipText ?: ""
        assertFalse("SCREEN provider must not fire for CALL CHAR", tooltip.contains("CALL SCREEN"))
    }

    fun `test gutter icon does not appear for CALL COLOR`() {
        configureFile("100 CALL COLOR(2,7,1)")
        val gutters = myFixture.findAllGutters()
        assertEquals("CALL COLOR must not produce a SCREEN gutter icon", 1, gutters.size)
        val tooltip = gutters[0].tooltipText ?: ""
        assertFalse("SCREEN provider must not fire for CALL COLOR", tooltip.contains("CALL SCREEN"))
    }

    fun `test gutter icon does not appear for CALL CLEAR`() {
        configureFile("100 CALL CLEAR")
        val gutters = myFixture.findAllGutters()
        assertTrue("No gutter icon must appear for CALL CLEAR", gutters.isEmpty())
    }
}
