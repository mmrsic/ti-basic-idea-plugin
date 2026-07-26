package com.github.mmrsic.idea.plugins.tibasic.ide.debug

import com.github.mmrsic.idea.plugins.tibasic.editor.TiBasicSoundNoise
import com.github.mmrsic.idea.plugins.tibasic.editor.TiBasicSoundPlayback
import com.github.mmrsic.idea.plugins.tibasic.editor.TiBasicSoundTone

internal class TiBasicDebugSessionCallSoundTest : TiBasicDebugSessionBaseTest() {


    fun `test CALL SOUND step resolves playback from current debugger variables`() {
        var session = startSession(
            """
            100 LET P=440
            110 CALL SOUND(120,P,2)
            """.trimIndent(),
        )

        session = session.step()
        val stepResult = session.stepWithEffects()

        assertEquals(
            TiBasicSoundPlayback(
                duration = 120,
                tones = listOf(TiBasicSoundTone(pitch = 440, volume = 2)),
            ),
            stepResult.soundPlayback,
        )
        assertEquals(TiBasicDebugSessionStatus.PendingStop, stepResult.session.status)
    }

    fun `test CALL SOUND noise with tone3 reuses previous debugger tone3 pitch`() {
        var session = startSession(
            """
            100 CALL SOUND(10,110,1,220,2,330,3)
            110 CALL SOUND(10,-4,4)
            """.trimIndent(),
        )

        session = session.stepWithEffects().session
        val stepResult = session.stepWithEffects()

        assertEquals(
            TiBasicSoundPlayback(
                duration = 10,
                tones = emptyList(),
                noise = TiBasicSoundNoise(selector = -4, volume = 4, tone3Pitch = 330),
            ),
            stepResult.soundPlayback,
        )
        assertEquals(TiBasicDebugSessionStatus.PendingStop, stepResult.session.status)
    }
}