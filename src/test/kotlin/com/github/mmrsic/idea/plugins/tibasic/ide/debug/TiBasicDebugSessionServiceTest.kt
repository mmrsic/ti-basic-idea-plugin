package com.github.mmrsic.idea.plugins.tibasic.ide.debug

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase
import com.github.mmrsic.idea.plugins.tibasic.editor.TiBasicSoundPlayback
import com.github.mmrsic.idea.plugins.tibasic.editor.TiBasicSoundTone

class TiBasicDebugSessionServiceTest : TiBasicTestBase() {

    fun `test stepping CALL SOUND triggers playback handler`() {
        val file = configureFile("100 CALL SOUND(120,440,2)")
        val sessionService = project.getService(TiBasicDebugSessionService::class.java)
        val snapshot = TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document)
        var played: TiBasicSoundPlayback? = null
        val originalHandler = sessionService.soundPlaybackHandler
        sessionService.soundPlaybackHandler = { _, playback -> played = playback }

        try {
            sessionService.startSession(snapshot)

            sessionService.step()

            assertEquals(
                TiBasicSoundPlayback(
                    duration = 120,
                    tones = listOf(TiBasicSoundTone(pitch = 440, volume = 2)),
                ),
                played,
            )
        } finally {
            sessionService.soundPlaybackHandler = originalHandler
        }
    }
}
