package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase
import com.github.mmrsic.idea.plugins.tibasic.language.analysis.calls.collectStaticallyTraceableCallStatements

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

    fun `test resolve sound playback with noise channel`() {
        val file = configureFile("100 CALL SOUND(20,220,0,294,1,330,2,-8,3)")
        assertEquals(
            TiBasicSoundPlayback(
                20,
                listOf(TiBasicSoundTone(220, 0), TiBasicSoundTone(294, 1), TiBasicSoundTone(330, 2)),
                TiBasicSoundNoise(-8, 3, 330),
            ),
            resolveSoundPlayback(file.callStatements().single(), file),
        )
    }

    fun `test resolve sound playback with single noise selector`() {
        val file = configureFile("2271 CALL SOUND(3000,-1,0)")
        assertEquals(
            TiBasicSoundPlayback(3000, emptyList(), TiBasicSoundNoise(-1, 0)),
            resolveSoundPlayback(file.callStatements().single(), file),
        )
    }

    fun `test noise selectors map to TMS9919 shift rates`() {
        assertEquals(TiBasicNoiseShiftRate.HIGH, TiBasicSoundNoise(-1, 0).shiftRate)
        assertEquals(TiBasicNoiseShiftRate.MEDIUM, TiBasicSoundNoise(-2, 0).shiftRate)
        assertEquals(TiBasicNoiseShiftRate.LOW, TiBasicSoundNoise(-3, 0).shiftRate)
        assertEquals(TiBasicNoiseShiftRate.TONE3, TiBasicSoundNoise(-4, 0).shiftRate)
        assertEquals(TiBasicNoiseShiftRate.HIGH, TiBasicSoundNoise(-5, 0).shiftRate)
        assertEquals(TiBasicNoiseShiftRate.MEDIUM, TiBasicSoundNoise(-6, 0).shiftRate)
        assertEquals(TiBasicNoiseShiftRate.LOW, TiBasicSoundNoise(-7, 0).shiftRate)
        assertEquals(TiBasicNoiseShiftRate.TONE3, TiBasicSoundNoise(-8, 0).shiftRate)
    }

    fun `test tone3 linked noise reuses previous tone3 pitch`() {
        val file = configureFile(
            "100 CALL SOUND(50,220,0,330,0,110,0)\n110 CALL SOUND(3000,-4,0)",
        )

        assertEquals(
            110,
            resolveSoundPlayback(file.callStatements()[1], file)?.noise?.tone3Pitch,
        )
    }

    fun `test tone3 linked noise becomes unknown after unresolved previous tone3 assignment`() {
        val file = configureFile(
            "100 INPUT T\n110 CALL SOUND(50,220,0,330,0,T,0)\n120 CALL SOUND(3000,-8,0)",
        )

        assertNull(resolveSoundPlayback(file.callStatements()[1], file)?.noise?.tone3Pitch)
    }

    fun `test resolve sound playback returns null for invalid volume`() {
        val file = configureFile("100 CALL SOUND(100,440,31)")
        assertNull(resolveSoundPlayback(file.callStatements().single(), file))
    }

    fun `test resolve sound playback returns null for invalid noise code`() {
        val file = configureFile("100 CALL SOUND(20,220,0,294,1,330,2,-9,3)")
        assertNull(resolveSoundPlayback(file.callStatements().single(), file))
    }

    fun `test resolve sound playback returns null for multiple noise selectors`() {
        val file = configureFile("100 CALL SOUND(20,-1,0,-5,1)")
        assertNull(resolveSoundPlayback(file.callStatements().single(), file))
    }

    fun `test resolve sound playback returns null for non constant argument`() {
        val file = configureFile("100 INPUT P\n110 CALL SOUND(100,P,2)")
        assertNull(resolveSoundPlayback(file.callStatements().single(), file))
    }

    fun `test static call traversal stops at repeated execution state`() {
        val file = configureFile(
            """
            100 DATA 262,2,0,1
            110 RESTORE 100
            120 READ I,J
            130 CALL SOUND(J*200,I,6)
            140 IF J=2 THEN 110
            """.trimIndent(),
        )

        val traceableCalls = collectStaticallyTraceableCallStatements(file)

        assertEquals(listOf("CALL SOUND(J*200,I,6)"), traceableCalls.map { it.callStatement.text })
        assertEquals(mapOf("I" to "262", "J" to "2"), traceableCalls.single().readDataVariableValues)
    }
}
