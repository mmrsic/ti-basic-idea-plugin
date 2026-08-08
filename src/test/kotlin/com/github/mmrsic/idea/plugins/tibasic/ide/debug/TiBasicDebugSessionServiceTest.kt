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

    fun `test skip on GOSUB runs subroutine and stops after RETURN`() {
        val code = """
            100 GOSUB 300
            110 PRINT "DONE"
            300 LET X=1
            310 RETURN
        """.trimIndent()
        val file = configureFile(code)
        val sessionService = project.getService(TiBasicDebugSessionService::class.java)
        val snapshot = TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document)

        sessionService.startSession(snapshot)

        assertEquals(100, sessionService.currentSession()?.currentProgramLine?.lineNumber)
        assertTrue(sessionService.currentSession()?.currentProgramLine?.semantics is TiBasicDebugLineSemantics.Gosub)

        sessionService.skip()

        assertEquals(110, sessionService.currentSession()?.currentProgramLine?.lineNumber)
        assertTrue(sessionService.currentSession()?.gosubOriginLineNumbers?.isEmpty() == true)
        assertEquals(1.0, sessionService.currentSession()?.numericVariables?.get("X")?.value?.toDouble())
    }

    fun `test skip on ON GOSUB runs selected subroutine and stops after RETURN`() {
        val code = """
            100 ON 2 GOSUB 300,400
            110 PRINT "DONE"
            300 LET X=1
            400 LET X=2
            410 RETURN
        """.trimIndent()
        val file = configureFile(code)
        val sessionService = project.getService(TiBasicDebugSessionService::class.java)
        val snapshot = TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document)

        sessionService.startSession(snapshot)

        assertEquals(100, sessionService.currentSession()?.currentProgramLine?.lineNumber)
        assertTrue(sessionService.currentSession()?.currentProgramLine?.semantics is TiBasicDebugLineSemantics.OnGosub)

        sessionService.skip()

        assertEquals(110, sessionService.currentSession()?.currentProgramLine?.lineNumber)
        assertTrue(sessionService.currentSession()?.gosubOriginLineNumbers?.isEmpty() == true)
        assertEquals(2.0, sessionService.currentSession()?.numericVariables?.get("X")?.value?.toDouble())
    }

    fun `test updatePendingInputValues updates session and notifies listeners once`() {
        val file = configureFile("100 INPUT A,B")
        val sessionService = project.getService(TiBasicDebugSessionService::class.java)
        val snapshot = TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document)

        var notificationCount = 0
        sessionService.addListener({ _, _ -> notificationCount++ }, testRootDisposable)

        sessionService.startSession(snapshot)

        sessionService.updatePendingInputValues(mapOf("A" to "10", "B" to "20"))

        assertEquals(2, notificationCount)
        assertEquals("10", sessionService.currentSession()?.pendingInputValues?.get("A"))
        assertEquals("20", sessionService.currentSession()?.pendingInputValues?.get("B"))
    }
}
