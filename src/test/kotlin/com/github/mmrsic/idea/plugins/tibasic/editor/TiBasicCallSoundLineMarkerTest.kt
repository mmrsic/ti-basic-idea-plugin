package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase

class TiBasicCallSoundLineMarkerTest : TiBasicTestBase() {

    fun `test gutter icon appears for CALL SOUND with literal arguments`() {
        configureFile("100 CALL SOUND(100,440,2)")
        val gutters = myFixture.findAllGutters()
        assertEquals("Exactly one gutter icon must appear for playable CALL SOUND", 1, gutters.size)
    }

    fun `test gutter icon tooltip contains resolved tone values`() {
        configureFile("100 CALL SOUND(100,440,2)")
        val gutters = myFixture.findAllGutters()
        assertEquals(1, gutters.size)
        val tooltip = gutters[0].tooltipText ?: ""
        assertTrue("Tooltip must contain duration", tooltip.contains("dur=100"))
        assertTrue("Tooltip must contain pitch", tooltip.contains("pitch=440"))
        assertTrue("Tooltip must contain volume", tooltip.contains("vol=2"))
    }

    fun `test gutter icon appears for CALL SOUND with constant numeric variables`() {
        configureFile("100 LET D=100\n110 LET P=440\n120 LET V=2\n130 CALL SOUND(D,P,V)")
        val gutters = myFixture.findAllGutters()
        assertEquals("Gutter icon must appear when all three arguments resolve from constants", 1, gutters.size)
    }

    fun `test gutter icon appears for CALL SOUND with two tones`() {
        configureFile("100 CALL SOUND(20,220,0,294,0)")
        val gutters = myFixture.findAllGutters()
        assertEquals("Gutter icon must appear for playable multi-tone CALL SOUND", 1, gutters.size)
        val tooltip = gutters[0].tooltipText ?: ""
        assertTrue("Tooltip must contain first tone", tooltip.contains("tone1=pitch=220/vol=0"))
        assertTrue("Tooltip must contain second tone", tooltip.contains("tone2=pitch=294/vol=0"))
    }

    fun `test gutter icon does not appear for CALL SOUND with invalid volume`() {
        configureFile("100 CALL SOUND(100,440,31)")
        val gutters = myFixture.findAllGutters()
        assertTrue("No gutter icon must appear when volume is outside 0..30", gutters.isEmpty())
    }

    fun `test gutter icon does not appear for CALL SOUND with non constant variable`() {
        configureFile("100 INPUT P\n110 CALL SOUND(100,P,2)")
        val gutters = myFixture.findAllGutters()
        assertTrue("No gutter icon must appear when pitch is not statically determinable", gutters.isEmpty())
    }
}
