package com.github.mmrsic.idea.plugins.tibasic.ide.debug

import com.github.mmrsic.idea.plugins.tibasic.language.model.TiColor

internal class TiBasicDebugSessionTest : TiBasicDebugSessionBaseTest() {

    fun `test initial session starts at lowest program line number`() {
        val session = startSession(
            """
            200 PRINT "SECOND"
            100 PRINT "FIRST"
            """.trimIndent(),
        )

        assertEquals(TiBasicDebugSessionStatus.Paused, session.status)
        assertEquals(100, session.currentProgramLine?.lineNumber)
        assertEquals(1, session.currentSourceLineIndex)
    }

    fun `test sequential stepping visits next higher line number`() {
        var session = startSession(
            """
            100 PRINT "A"
            140 PRINT "C"
            120 PRINT "B"
            """.trimIndent(),
        )

        session = session.step()
        assertEquals(120, session.currentProgramLine?.lineNumber)

        session = session.step()
        assertEquals(140, session.currentProgramLine?.lineNumber)
    }

    fun `test sequential stepping skips REM lines`() {
        var session = startSession(
            """
            100 PRINT "A"
            110 REM comment
            120 REM more
            130 PRINT "B"
            """.trimIndent(),
        )

        session = session.step()

        assertEquals(130, session.currentProgramLine?.lineNumber)
    }

    fun `test stepping on REM line continues to next non REM line`() {
        var session = startSession(
            """
            100 REM comment
            110 REM more
            120 PRINT "B"
            """.trimIndent(),
        )

        session = session.step()

        assertEquals(120, session.currentProgramLine?.lineNumber)
    }

    fun `test debug session initializes sixteen CALL COLOR character sets`() {
        val session = startSession("100 PRINT \"A\"")

        assertEquals((1..16).toSet(), session.screenContents.characterSetColors.keys)
        assertTrue(session.screenContents.characterSetColors.values.all { colors ->
            colors.fg == TiColor.Black && colors.bg == TiColor.Transparent
        })
    }

    fun `test malformed supported statement shows incorrect statement`() {
        val malformedLines = listOf(
            "100 GOTO",
            "100 GOSUB X",
            "100 IF X>0",
            "100 IF X>0 THEN",
            "100 IF X>0 THEN 200 ELSE",
            "100 RETURN 1",
            "100 END 1",
            "100 STOP 1",
        )

        malformedLines.forEach { line ->
            val session = startSession(line).step()
            assertEquals(line, TiBasicDebugSessionStatus.PendingStop, session.status)
            assertEquals(line, TiBasicDebugMetadata.message(TiBasicDebugMetadata.incorrectStatementKey), session.statusMessage)
        }
    }

    fun `test inspect evaluates current string expressions against debugger state`() {
        var session = startSession(
            """
            100 LET A$="HI"
            110 PRINT A$
            """.trimIndent(),
        )

        session = session.step()

        val inspectResult = inspectExpression(project, session, "A$&STR$(4711)")

        assertEquals("\"HI4711\" = 06 H I 4 7 1 1", inspectResult?.displayText)
    }

    fun `test inspect evaluates numeric expressions derived from debugger string state`() {
        var session = startSession(
            """
            100 LET A$="HELLO"
            110 PRINT A$
            """.trimIndent(),
        )

        session = session.step()

        val inspectResult = inspectExpression(project, session, "LEN(A$)+2")

        assertEquals("7", inspectResult?.displayText)
    }

    fun `test inspect evaluates numeric variables from debugger state`() {
        var session = startSession(
            """
            100 LET A=5
            110 PRINT A
            """.trimIndent(),
        )

        session = session.step()

        val inspectResult = inspectExpression(project, session, "A+1")

        assertEquals("6", inspectResult?.displayText)
    }

    fun `test valid but unsupported statements are ignored and step sequentially`() {
        var session = startSession(
            """
            100 PRINT "A"
            110 DATA 1
            120 PRINT "B"
            """.trimIndent(),
        )

        session = session.step()
        assertEquals(110, session.currentProgramLine?.lineNumber)

        session = session.step()
        assertEquals(120, session.currentProgramLine?.lineNumber)
    }

    fun `test unknown statement shows incorrect statement and stops on next step`() {
        var session = startSession("100 BLAH")

        session = session.step()
        assertEquals(TiBasicDebugSessionStatus.PendingStop, session.status)
        assertEquals(TiBasicDebugMetadata.message(TiBasicDebugMetadata.incorrectStatementKey), session.statusMessage)
        assertEquals(100, session.currentProgramLine?.lineNumber)

        session = session.step()
        assertEquals(TiBasicDebugSessionStatus.Stopped, session.status)
    }

    fun `test END enters pending stop and finishes on next step`() {
        var session = startSession("100 END")

        session = session.step()
        assertEquals(TiBasicDebugSessionStatus.PendingStop, session.status)
        assertNull(session.statusMessage)

        session = session.step()
        assertEquals(TiBasicDebugSessionStatus.Stopped, session.status)
    }

    fun `test unknown numeric references in program code are initialized to zero`() {
        var session = startSession(
            """
            100 PRINT X
            110 PRINT X
            """.trimIndent(),
        )

        session = session.step()

        assertEquals("0", session.numericVariables["X"]?.usualDisplay)
        assertEquals(listOf(0, 0, 0, 0, 0, 0, 0, 0), session.numericVariables["X"]?.internalBytes)
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test SEG dollar initializes unknown string references as empty string`() {
        var session = startSession(
            """
            100 LET B$=SEG$(A$,1,3)
            110 PRINT B$
            """.trimIndent(),
        )

        session = session.step()

        assertEquals("", session.stringVariables["A$"]?.text)
        assertEquals("00", session.stringVariables["A$"]?.internalDisplay)
        assertEquals("", session.stringVariables["B$"]?.text)
        assertEquals("00", session.stringVariables["B$"]?.internalDisplay)
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

}