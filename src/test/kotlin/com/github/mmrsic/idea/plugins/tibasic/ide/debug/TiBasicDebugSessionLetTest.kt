package com.github.mmrsic.idea.plugins.tibasic.ide.debug

import java.math.BigDecimal

internal class TiBasicDebugSessionLetTest : TiBasicDebugSessionBaseTest() {

    fun `test numeric LET with string expression shows string number mismatch`() {
        var session = startSession("100 LET A=\"HELLO\"")

        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.PendingStop, session.status)
        assertEquals(TiBasicDebugMetadata.message(TiBasicDebugMetadata.stringNumberMismatchKey), session.statusMessage)
    }

    fun `test string LET with numeric expression shows string number mismatch`() {
        var session = startSession("100 LET A$=5")

        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.PendingStop, session.status)
        assertEquals(TiBasicDebugMetadata.message(TiBasicDebugMetadata.stringNumberMismatchKey), session.statusMessage)
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

    fun `test string LET with CHR dollar concat and numeric variable reports string number mismatch`() {
        var session = startSession("1640 s$=chr$(130)&\" X=\"&X1")

        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.PendingStop, session.status)
        assertEquals(TiBasicDebugMetadata.message(TiBasicDebugMetadata.stringNumberMismatchKey), session.statusMessage)
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

    fun `test numeric LET supports INT function with positive decimal`() {
        var session = startSession(
            """
            100 LET A=INT(123.45)
            110 PRINT A
            """.trimIndent(),
        )

        session = session.step()

        assertEquals("123", session.numericVariables["A"]?.usualDisplay)
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test numeric LET supports INT function with negative decimal`() {
        var session = startSession(
            """
            100 LET A=INT(-123.45)
            110 PRINT A
            """.trimIndent(),
        )

        session = session.step()

        assertEquals("-124", session.numericVariables["A"]?.usualDisplay)
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test implicit numeric LET supports INT around RND without parentheses`() {
        var session = startSession(
            """
            920 K=INT(22*RND)+1
            930 PRINT K
            """.trimIndent(),
        )

        session = session.step()

        val value = session.numericVariables["K"]?.value
        assertEquals(930, session.currentProgramLine?.lineNumber)
        assertTrue(value != null && value >= BigDecimal.ONE && value <= BigDecimal("22"))
    }


    fun `test numeric array LET stores element at subscript`() {
        var session = startSession(
            """
            100 LET A(1)=42
            110 PRINT A(1)
            """.trimIndent(),
        )

        session = session.step()

        assertEquals("42", session.numericArrayVariables["A"]?.get(listOf(1))?.usualDisplay)
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test numeric array LET with variable subscript`() {
        var session = startSession(
            """
            100 LET I=2
            110 LET A(I)=5
            120 PRINT A(2)
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()

        assertEquals("5", session.numericArrayVariables["A"]?.get(listOf(2))?.usualDisplay)
        assertEquals(120, session.currentProgramLine?.lineNumber)
    }

    fun `test string array LET stores element at subscript`() {
        var session = startSession(
            """
            100 LET B$(3)="HI"
            110 PRINT B$(3)
            """.trimIndent(),
        )

        session = session.step()

        assertEquals("HI", session.stringArrayVariables["B$"]?.get(listOf(3))?.text)
        assertEquals("02 H I", session.stringArrayVariables["B$"]?.get(listOf(3))?.internalDisplay)
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test numeric array LET updates same element on reassignment`() {
        var session = startSession(
            """
            100 LET A(1)=10
            110 LET A(1)=20
            120 PRINT A(1)
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()

        assertEquals("20", session.numericArrayVariables["A"]?.get(listOf(1))?.usualDisplay)
        assertEquals(1, session.numericArrayVariables["A"]?.size)
        assertEquals(120, session.currentProgramLine?.lineNumber)
    }

    fun `test numeric array LET with two-dimensional subscript`() {
        var session = startSession(
            """
            100 LET A(2,3)=7
            110 PRINT A(2,3)
            """.trimIndent(),
        )

        session = session.step()

        assertEquals("7", session.numericArrayVariables["A"]?.get(listOf(2, 3))?.usualDisplay)
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test numeric expression evaluates array element reference`() {
        var session = startSession(
            """
            100 LET I=2
            110 LET A(I)=4
            120 LET B=A(I)+3
            130 END
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()
        session = session.step()

        assertEquals("7", session.numericVariables["B"]?.usualDisplay)
        assertEquals(130, session.currentProgramLine?.lineNumber)
    }

    fun `test implicit numeric LET supports division by negative literal`() {
        var session = startSession(
            """
            1050 Y=8
            1060 Y=Y/-4
            1070 PRINT Y
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()

        assertEquals("-2", session.numericVariables["Y"]?.usualDisplay)
        assertEquals(1070, session.currentProgramLine?.lineNumber)
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

}