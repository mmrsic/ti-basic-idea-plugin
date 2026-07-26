package com.github.mmrsic.idea.plugins.tibasic.ide.debug

internal class TiBasicDebugSessionCallKeyTest : TiBasicDebugSessionBaseTest() {

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
}