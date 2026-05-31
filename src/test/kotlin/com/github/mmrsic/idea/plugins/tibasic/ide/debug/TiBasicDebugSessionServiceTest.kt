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

    fun `test skip stops on CALL KEY`() {
        val code = """
            100 FOR I=1 TO 10
            110 CALL KEY(0,K,S)
            120 NEXT I
        """.trimIndent()
        val file = configureFile(code)
        val sessionService = project.getService(TiBasicDebugSessionService::class.java)
        val snapshot = TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document)

        sessionService.startSession(snapshot) // Paused on 100
        sessionService.step() // Paused on 110
        sessionService.step() // Paused on 120 (NEXT I)

        assertEquals(120, sessionService.currentSession()?.currentProgramLine?.lineNumber)
        assertTrue(sessionService.currentSession()?.currentProgramLine?.semantics is TiBasicDebugLineSemantics.Next)

        sessionService.skip()

        // Should have stepped 120 -> 110 and stopped there because CALL KEY requires input
        assertEquals(110, sessionService.currentSession()?.currentProgramLine?.lineNumber)
        assertNotNull(sessionService.currentSession()?.keyboardRequest)
    }

    fun `test skip stops before loop exit`() {
        val code = """
            100 FOR I=1 TO 2
            110 NEXT I
            120 PRINT "DONE"
        """.trimIndent()
        val file = configureFile(code)
        val sessionService = project.getService(TiBasicDebugSessionService::class.java)
        val snapshot = TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document)

        sessionService.startSession(snapshot) // Paused on 100
        sessionService.step() // Paused on 110 (I=1)

        // At 110, I=1. Skip should step 110 -> 110 (next iteration) and stop on 110 because next step would exit
        sessionService.skip()

        assertEquals(110, sessionService.currentSession()?.currentProgramLine?.lineNumber)
        // Verify I=2.0 (it should have advanced to the next iteration)
        assertEquals(2.0, sessionService.currentSession()?.numericVariables?.get("I")?.value?.toDouble())

        // Now call skip again, it should stop immediately because loop exit is imminent
        sessionService.skip()
        assertEquals(110, sessionService.currentSession()?.currentProgramLine?.lineNumber)
        assertEquals(2.0, sessionService.currentSession()?.numericVariables?.get("I")?.value?.toDouble())
    }
}
