package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase

@Suppress("DialogTitleCapitalization")
class TiBasicCallSoundLineMarkerTest : TiBasicTestBase() {

    fun `test gutter icon appears for CALL SOUND with literal arguments`() {
        configureFile("100 CALL SOUND(100,440,2)")
        val gutters = myFixture.findAllGutters()
        assertEquals("Exactly one gutter icon must appear for playable CALL SOUND", 1, gutters.size)
    }

    fun `test gutter icon appears for CALL SOUND with negative duration`() {
        configureFile("100 CALL SOUND(-99,440,2)")
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

    fun `test gutter icon tooltip contains noise channel details`() {
        configureFile("100 CALL SOUND(20,220,0,294,1,330,2,-8,3)")
        val gutters = myFixture.findAllGutters()
        assertEquals("Exactly one gutter icon must appear for playable CALL SOUND with noise", 1, gutters.size)
        val tooltip = gutters[0].tooltipText ?: ""
        assertTrue("Tooltip must contain third tone", tooltip.contains("tone3=pitch=330/vol=2"))
        assertTrue("Tooltip must contain noise selector", tooltip.contains("noise=selector=-8"))
        assertTrue("Tooltip must contain noise type", tooltip.contains("type=white"))
        assertTrue("Tooltip must contain noise rate", tooltip.contains("rate=tone3"))
    }

    fun `test gutter icon appears for CALL SOUND with single noise selector`() {
        configureFile("2271 CALL SOUND(3000,-1,0)")
        val gutters = myFixture.findAllGutters()
        assertEquals("Exactly one gutter icon must appear for CALL SOUND noise-only playback", 1, gutters.size)
        val tooltip = gutters[0].tooltipText ?: ""
        assertTrue("Tooltip must contain the noise selector", tooltip.contains("noise=selector=-1"))
        assertTrue("Tooltip must contain the periodic noise type", tooltip.contains("type=periodic"))
    }

    fun `test gutter icon appears for tone3 linked noise using previous tone3 pitch`() {
        configureFile("100 CALL SOUND(50,220,0,330,0,110,0)\n110 CALL SOUND(3000,-4,0)")
        val gutters = myFixture.findAllGutters()

        assertEquals("Exactly two gutter icons must appear for both playable CALL SOUND lines", 2, gutters.size)
    }

    fun `test gutter icon does not appear for CALL SOUND with invalid volume`() {
        configureFile("100 CALL SOUND(100,440,31)")
        val gutters = myFixture.findAllGutters()
        assertTrue("No gutter icon must appear when volume is outside 0..30", gutters.isEmpty())
    }

    fun `test gutter icon does not appear for CALL SOUND with invalid noise code`() {
        configureFile("100 CALL SOUND(20,220,0,294,1,330,2,-9,3)")
        val gutters = myFixture.findAllGutters()
        assertTrue("No gutter icon must appear when the fourth channel is not a valid noise code", gutters.isEmpty())
    }

    fun `test gutter icon does not appear for CALL SOUND with non constant variable`() {
        configureFile("100 INPUT P\n110 CALL SOUND(100,P,2)")
        val gutters = myFixture.findAllGutters()
        assertTrue("No gutter icon must appear when pitch is not statically determinable", gutters.isEmpty())
    }
}
