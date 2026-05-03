package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase

class TiBasicSoundToneTest : TiBasicTestBase() {

    fun `test resolve sound playback with literal arguments`() {
        val file = configureFile("100 CALL SOUND(100,440,2)")
        assertEquals(
            TiBasicSoundPlayback(100, listOf(TiBasicSoundTone(440, 2))),
            resolveSoundPlayback(file.callStatements().single(), file),
        )
    }

    fun `test resolve sound playback with constant variables`() {
        val file = configureFile("100 LET D=100\n110 LET P=440\n120 LET V=2\n130 CALL SOUND(D,P,V)")
        assertEquals(
            TiBasicSoundPlayback(100, listOf(TiBasicSoundTone(440, 2))),
            resolveSoundPlayback(file.callStatements().single(), file),
        )
    }

    fun `test resolve sound playback with two tones`() {
        val file = configureFile("100 CALL SOUND(20,220,0,294,0)")
        assertEquals(
            TiBasicSoundPlayback(20, listOf(TiBasicSoundTone(220, 0), TiBasicSoundTone(294, 0))),
            resolveSoundPlayback(file.callStatements().single(), file),
        )
    }

    fun `test resolve sound playback returns null for invalid volume`() {
        val file = configureFile("100 CALL SOUND(100,440,31)")
        assertNull(resolveSoundPlayback(file.callStatements().single(), file))
    }

    fun `test resolve sound playback returns null for non constant argument`() {
        val file = configureFile("100 INPUT P\n110 CALL SOUND(100,P,2)")
        assertNull(resolveSoundPlayback(file.callStatements().single(), file))
    }
}
