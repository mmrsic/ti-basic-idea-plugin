package com.github.mmrsic.idea.plugins.tibasic.ide.debug

internal class TiBasicDebugSessionOnGotoGosubTest : TiBasicDebugSessionBaseTest() {

    fun `test ON GOTO jumps to first target for selector 1`() {
        var session = startSession(
            """
            100 ON 1 GOTO 200,300
            200 PRINT "A"
            300 PRINT "B"
            """.trimIndent(),
        )

        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.Paused, session.status)
        assertEquals(200, session.currentProgramLine?.lineNumber)
    }

    fun `test ON GOTO rounds selector before choosing target`() {
        var session = startSession(
            """
            100 ON 1.6 GOTO 200,300
            200 PRINT "A"
            300 PRINT "B"
            """.trimIndent(),
        )

        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.Paused, session.status)
        assertEquals(300, session.currentProgramLine?.lineNumber)
    }

    fun `test ON GOTO selector below list range shows bad value in source line`() {
        var session = startSession("100 ON 0 GOTO 200\n200 PRINT \"A\"")

        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.PendingStop, session.status)
        assertEquals(TiBasicDebugMetadata.message(TiBasicDebugMetadata.badValueInKey, 100), session.statusMessage)
        assertEquals(100, session.currentProgramLine?.lineNumber)
    }

    fun `test ON GOTO selected missing target line shows bad line number in source line`() {
        var session = startSession("100 ON 1 GOTO 200\n300 PRINT \"A\"")

        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.PendingStop, session.status)
        assertEquals(TiBasicDebugMetadata.message(TiBasicDebugMetadata.badLineNumberInKey, 100), session.statusMessage)
        assertEquals(100, session.currentProgramLine?.lineNumber)
    }

    fun `test ON GOSUB and RETURN continue after calling line`() {
        var session = startSession(
            """
            100 ON 2 GOSUB 300,400
            110 PRINT "AFTER"
            300 PRINT "SKIP"
            400 LET X=1
            410 RETURN
            """.trimIndent(),
        )

        session = session.step()
        assertEquals(400, session.currentProgramLine?.lineNumber)
        assertEquals(listOf(100), session.gosubOriginLineNumbers)

        session = session.step()
        assertEquals(410, session.currentProgramLine?.lineNumber)
        assertEquals(1.0, session.numericVariables["X"]?.value?.toDouble())

        session = session.step()
        assertEquals(110, session.currentProgramLine?.lineNumber)
        assertTrue(session.gosubOriginLineNumbers.isEmpty())
    }
}
