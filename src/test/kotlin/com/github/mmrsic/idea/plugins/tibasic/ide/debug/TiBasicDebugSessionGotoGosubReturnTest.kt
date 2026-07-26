package com.github.mmrsic.idea.plugins.tibasic.ide.debug

internal class TiBasicDebugSessionGotoGosubReturnTest : TiBasicDebugSessionBaseTest() {

    fun `test GOTO jumps to target line`() {
        var session = startSession(
            """
            100 GOTO 300
            200 PRINT "SKIP"
            300 PRINT "DONE"
            """.trimIndent(),
        )

        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.Paused, session.status)
        assertEquals(300, session.currentProgramLine?.lineNumber)
    }

    fun `test GOSUB and RETURN continue after calling line`() {
        var session = startSession(
            """
            100 GOSUB 300
            110 PRINT "AFTER"
            300 PRINT "IN SUB"
            310 RETURN
            """.trimIndent(),
        )

        session = session.step()
        assertEquals(300, session.currentProgramLine?.lineNumber)
        assertEquals(listOf(100), session.gosubOriginLineNumbers)

        session = session.step()
        assertEquals(310, session.currentProgramLine?.lineNumber)

        session = session.step()
        assertEquals(110, session.currentProgramLine?.lineNumber)
        assertTrue(session.gosubOriginLineNumbers.isEmpty())
    }

    fun `test RETURN without GOSUB shows runtime error then stops on next step`() {
        var session = startSession("100 RETURN")

        session = session.step()
        assertEquals(TiBasicDebugSessionStatus.PendingStop, session.status)
        assertEquals(TiBasicDebugMetadata.message(TiBasicDebugMetadata.cantDoThatKey), session.statusMessage)
        assertEquals(100, session.currentProgramLine?.lineNumber)

        session = session.step()
        assertEquals(TiBasicDebugSessionStatus.Stopped, session.status)
        assertNull(session.currentProgramLine)
    }

    fun `test missing GOTO target line shows bad line number then stops on next step`() {
        var session = startSession("100 GOTO 999")

        session = session.step()
        assertEquals(TiBasicDebugSessionStatus.PendingStop, session.status)
        assertEquals(TiBasicDebugMetadata.message(TiBasicDebugMetadata.badLineNumberKey), session.statusMessage)

        session = session.step()
        assertEquals(TiBasicDebugSessionStatus.Stopped, session.status)
    }

}