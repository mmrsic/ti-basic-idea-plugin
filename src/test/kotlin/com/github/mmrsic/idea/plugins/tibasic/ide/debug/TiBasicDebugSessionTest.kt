package com.github.mmrsic.idea.plugins.tibasic.ide.debug

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase
import com.github.mmrsic.idea.plugins.tibasic.language.model.TiColor

class TiBasicDebugSessionTest : TiBasicTestBase() {

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

    fun `test CALL CLEAR sets all screen positions to code 32`() {
        var session = startSession(
            """
            100 CALL CLEAR
            110 PRINT "A"
            """.trimIndent(),
        )

        session = session.step()

        assertTrue(session.screenContents.characterCodes.flatten().all { code -> code == 32 })
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test CALL SCREEN updates debug screen background`() {
        var session = startSession(
            """
            100 CALL SCREEN(2)
            110 PRINT "A"
            """.trimIndent(),
        )

        session = session.step()

        assertEquals(TiColor.Black, session.screenContents.screenBackground)
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test CALL SCREEN treats transparent color as black in debugger`() {
        var session = startSession(
            """
            100 CALL SCREEN(1)
            110 PRINT "A"
            """.trimIndent(),
        )

        session = session.step()

        assertEquals(TiColor.Black, session.screenContents.screenBackground)
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test PRINT writes evaluated string output into row 24 from column 3`() {
        var session = startSession("100 PRINT \"HI\"")

        session = session.step()

        assertEquals(72, session.screenContents.characterCodes[23][2])
        assertEquals(73, session.screenContents.characterCodes[23][3])
        assertEquals(32, session.screenContents.characterCodes[23][0])
        assertEquals(32, session.screenContents.characterCodes[23][1])
        assertEquals(32, session.screenContents.characterCodes[23][30])
        assertEquals(32, session.screenContents.characterCodes[23][31])
    }

    fun `test PRINT wraps after twenty eight characters and leaves outer columns unchanged`() {
        var session = startSession("100 PRINT \"ABCDEFGHIJKLMNOPQRSTUVWXYZ12!\"")

        session = session.step()

        assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXYZ12", screenText(session, 23, 3, 28))
        assertEquals("!", session.screenContents.characterCodes[23][2].toChar().toString())
        assertEquals(32, session.screenContents.characterCodes[23][0])
        assertEquals(32, session.screenContents.characterCodes[23][1])
        assertEquals(32, session.screenContents.characterCodes[23][30])
        assertEquals(32, session.screenContents.characterCodes[23][31])
    }

    fun `test PRINT colon separator performs a line feed`() {
        var session = startSession("100 PRINT \"A\":\"B\"")

        session = session.step()

        assertEquals('A'.code, session.screenContents.characterCodes[22][2])
        assertEquals('B'.code, session.screenContents.characterCodes[23][2])
    }

    fun `test PRINT without trailing separator performs implicit line feed before next print`() {
        var session = startSession(
            """
            100 PRINT "A"
            110 PRINT "B"
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()

        assertEquals('A'.code, session.screenContents.characterCodes[22][2])
        assertEquals('B'.code, session.screenContents.characterCodes[23][2])
    }

    fun `test PRINT with two trailing colons leaves one blank bottom line`() {
        var session = startSession("100 PRINT \"HELLO\"::")

        session = session.step()

        assertEquals("HELLO", screenText(session, 23, 3, 5))
        assertEquals("     ", screenText(session, 24, 3, 5))
    }

    fun `test PRINT with three trailing colons leaves two blank bottom lines`() {
        var session = startSession("100 PRINT \"HELLO\":::")

        session = session.step()

        assertEquals("HELLO", screenText(session, 22, 3, 5))
        assertEquals("     ", screenText(session, 23, 3, 5))
        assertEquals("     ", screenText(session, 24, 3, 5))
    }

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

    fun `test malformed supported statement shows incorrect statement`() {
        val malformedLines = listOf(
            "100 GOTO",
            "100 GOSUB X",
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

    fun `test numeric LET evaluates expression and stores numeric variable`() {
        var session = startSession(
            """
            100 LET A=2
            110 LET B=A+3
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()

        assertEquals("2", session.numericVariables["A"]?.usualDisplay)
        assertEquals("5", session.numericVariables["B"]?.usualDisplay)
    }

    fun `test numeric LET initializes unknown numeric references before evaluation`() {
        var session = startSession(
            """
            100 LET B=A+2
            110 PRINT B
            """.trimIndent(),
        )

        session = session.step()

        assertEquals("0", session.numericVariables["A"]?.usualDisplay)
        assertEquals("2", session.numericVariables["B"]?.usualDisplay)
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test string LET stores TI-Basic internal string representation`() {
        var session = startSession(
            """
            100 LET A$="HELLO"
            110 PRINT A$
            """.trimIndent(),
        )

        session = session.step()

        assertEquals("HELLO", session.stringVariables["A$"]?.text)
        assertEquals("05 H E L L O", session.stringVariables["A$"]?.internalDisplay)
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test string LET can copy from known string variable`() {
        var session = startSession(
            """
            100 LET A$="HI"
            110 LET B$=A$
            120 PRINT B$
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()

        assertEquals("HI", session.stringVariables["B$"]?.text)
        assertEquals("02 H I", session.stringVariables["B$"]?.internalDisplay)
        assertEquals(120, session.currentProgramLine?.lineNumber)
    }

    fun `test unknown string reference on right side is initialized as empty string`() {
        var session = startSession(
            """
            100 LET B$=A$
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

    fun `test unknown string reference in concatenation is initialized before assignment`() {
        var session = startSession(
            """
            100 LET B$=A$&"X"
            110 PRINT B$
            """.trimIndent(),
        )

        session = session.step()

        assertEquals("", session.stringVariables["A$"]?.text)
        assertEquals("00", session.stringVariables["A$"]?.internalDisplay)
        assertEquals("X", session.stringVariables["B$"]?.text)
        assertEquals("01 X", session.stringVariables["B$"]?.internalDisplay)
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test malformed LET statement shows incorrect statement and stops on next step`() {
        var session = startSession("100 LET A$=")

        session = session.step()
        assertEquals(TiBasicDebugSessionStatus.PendingStop, session.status)
        assertEquals(TiBasicDebugMetadata.message(TiBasicDebugMetadata.incorrectStatementKey), session.statusMessage)
        assertEquals(100, session.currentProgramLine?.lineNumber)

        session = session.step()
        assertEquals(TiBasicDebugSessionStatus.Stopped, session.status)
    }

    fun `test long string LET warns about truncation to 255 characters`() {
        val overlongString = "A".repeat(256)
        var session = startSession(
            """
            100 LET A$="$overlongString"
            110 PRINT A$
            """.trimIndent(),
        )

        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.Paused, session.status)
        assertEquals(TiBasicDebugMetadata.message(TiBasicDebugMetadata.stringCutTo255CharactersKey), session.statusMessage)
        assertEquals(255, session.stringVariables["A$"]?.text?.length)
        assertTrue(session.stringVariables["A$"]?.internalDisplay?.startsWith("FF A A A") == true)
    }

    fun `test intermediate string results are truncated before reuse`() {
        val overlongPrefix = "A".repeat(255)
        var session = startSession(
            """
            100 LET B$=SEG$("$overlongPrefix"&"BC",256,1)
            110 PRINT B$
            """.trimIndent(),
        )

        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.Paused, session.status)
        assertEquals(TiBasicDebugMetadata.message(TiBasicDebugMetadata.stringCutTo255CharactersKey), session.statusMessage)
        assertEquals("", session.stringVariables["B$"]?.text)
        assertEquals("00", session.stringVariables["B$"]?.internalDisplay)
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test string LET supports concatenation and CHR dollar`() {
        var session = startSession(
            """
            10 a$=""
            20 b$="1"
            30 c$=a$&b$
            40 d$=chr$(27)&c$&c$&chr$(48)
            """.trimIndent(),
        )

        repeat(4) { session = session.step() }

        assertEquals("1", session.stringVariables["C$"]?.text)
        assertEquals("01 1", session.stringVariables["C$"]?.internalDisplay)
        assertEquals("04 1B 1 1 0", session.stringVariables["D$"]?.internalDisplay)
        assertEquals("${27.toChar()}110", session.stringVariables["D$"]?.text)
    }

    fun `test string LET supports STR dollar`() {
        var session = startSession(
            """
            100 LET A$=STR$(23)
            110 LET B$=STR$(-5)
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()

        assertEquals("23", session.stringVariables["A$"]?.text)
        assertEquals(listOf(2, 50, 51), session.stringVariables["A$"]?.internalBytes)
        assertEquals("-5", session.stringVariables["B$"]?.text)
        assertEquals(listOf(2, 45, 53), session.stringVariables["B$"]?.internalBytes)
    }

    fun `test string LET supports SEG dollar`() {
        var session = startSession(
            """
            100 LET A$="HELLO"
            110 LET B$=SEG$(A$,2,3)
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()

        assertEquals("ELL", session.stringVariables["B$"]?.text)
        assertEquals("03 E L L", session.stringVariables["B$"]?.internalDisplay)
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

    fun `test CALL KEY shows bad value for rounded mode outside valid range`() {
        var session = startSession("100 CALL KEY(5.6,K,S)")

        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.PendingStop, session.status)
        assertEquals(TiBasicDebugMetadata.message(TiBasicDebugMetadata.badValueKey, "5.6"), session.statusMessage)
    }

    fun `test CALL KEY mode 4 accepts rounded scan codes up to 143`() {
        var session = startSession(
            """
            100 CALL KEY(4,K,S)
            110 PRINT K
            """.trimIndent(),
        ).copy(keyboardScanInput = "142.6")

        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.Paused, session.status)
        assertEquals("143", session.numericVariables["K"]?.usualDisplay)
        assertEquals("1", session.numericVariables["S"]?.usualDisplay)
        assertEquals(4, session.lastKeyboardMode)
    }

    fun `test CALL KEY mode zero defaults to mode five when no previous keyboard mode exists`() {
        var session = startSession(
            """
            100 CALL KEY(0,K,S)
            110 PRINT K
            """.trimIndent(),
        )

        assertEquals(5, session.keyboardRequest?.mode)
        assertEquals("-1", session.keyboardRequest?.scanInput)

        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.Paused, session.status)
        assertEquals("-1", session.numericVariables["K"]?.usualDisplay)
        assertEquals("0", session.numericVariables["S"]?.usualDisplay)
        assertEquals(5, session.lastKeyboardMode)
        assertEquals(110, session.currentProgramLine?.lineNumber)
        assertNull(session.keyboardRequest)
    }

    fun `test CALL KEY mode zero reuses last keyboard mode`() {
        var session = startSession(
            """
            100 CALL KEY(2,K,S)
            110 CALL KEY(0,K,S)
            120 PRINT K
            """.trimIndent(),
        ).copy(keyboardScanInput = "5")

        session = session.step()

        assertEquals(2, session.lastKeyboardMode)
        assertEquals(2, session.keyboardRequest?.mode)

        session = session.copy(keyboardScanInput = "19")
        session = session.step()

        assertEquals("19", session.numericVariables["K"]?.usualDisplay)
        assertEquals("1", session.numericVariables["S"]?.usualDisplay)
        assertEquals(2, session.lastKeyboardMode)
        assertEquals(120, session.currentProgramLine?.lineNumber)
    }

    fun `test CALL KEY rounds valid mode 2 scan input and sets key status to one`() {
        var session = startSession(
            """
            100 CALL KEY(2,K,S)
            110 PRINT K
            """.trimIndent(),
        ).copy(keyboardScanInput = "18.6")

        session = session.step()

        assertEquals("19", session.numericVariables["K"]?.usualDisplay)
        assertEquals("1", session.numericVariables["S"]?.usualDisplay)
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test CALL KEY mode 3 accepts rounded scan codes in its allowed range`() {
        var session = startSession(
            """
            100 CALL KEY(3,K,S)
            110 PRINT K
            """.trimIndent(),
        ).copy(keyboardScanInput = "94.6")

        session = session.step()

        assertEquals("95", session.numericVariables["K"]?.usualDisplay)
        assertEquals("1", session.numericVariables["S"]?.usualDisplay)
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test CALL KEY mode 5 accepts rounded scan code 187`() {
        var session = startSession(
            """
            100 CALL KEY(5,K,S)
            110 PRINT K
            """.trimIndent(),
        ).copy(keyboardScanInput = "186.6")

        session = session.step()

        assertEquals("187", session.numericVariables["K"]?.usualDisplay)
        assertEquals("1", session.numericVariables["S"]?.usualDisplay)
        assertEquals(5, session.lastKeyboardMode)
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test CALL KEY with invalid scan input stays on current line and shows bad value`() {
        var session = startSession("100 CALL KEY(1,K,S)").copy(keyboardScanInput = "20")

        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.Paused, session.status)
        assertEquals(TiBasicDebugMetadata.message(TiBasicDebugMetadata.badValueKey, 20), session.statusMessage)
        assertEquals(100, session.currentProgramLine?.lineNumber)
        assertNull(session.numericVariables["K"])
        assertNull(session.numericVariables["S"])
        assertEquals(1, session.keyboardRequest?.mode)
        assertEquals("20", session.keyboardRequest?.scanInput)
    }

    fun `test CALL KEY mode 3 rejects disallowed scan input 96`() {
        var session = startSession("100 CALL KEY(3,K,S)").copy(keyboardScanInput = "96")

        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.Paused, session.status)
        assertEquals(TiBasicDebugMetadata.message(TiBasicDebugMetadata.badValueKey, 96), session.statusMessage)
        assertEquals(100, session.currentProgramLine?.lineNumber)
        assertEquals(3, session.keyboardRequest?.mode)
    }

    private fun startSession(programText: String): TiBasicDebugSession {
        val file = configureFile(programText)
        val snapshot = TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document)
        return snapshot.initialSession()
    }

    private fun screenText(session: TiBasicDebugSession, row: Int, column: Int, length: Int): String =
        (column - 1 until column - 1 + length)
            .map { index -> session.screenContents.characterCodes[row - 1][index].toChar() }
            .joinToString(separator = "")
}
