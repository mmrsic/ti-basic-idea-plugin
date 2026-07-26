package com.github.mmrsic.idea.plugins.tibasic.ide.debug

import java.math.BigDecimal

internal class TiBasicDebugSessionInputTest : TiBasicDebugSessionBaseTest() {

    fun `test INPUT sets numeric and string variables and continues`() {
        var session = startSession(
            """
            100 INPUT A,B$
            110 END
            """.trimIndent(),
        )
        assertEquals(100, session.currentProgramLine?.lineNumber)

        session = session.copy(pendingInputValues = mapOf("A" to "42", "B$" to "HELLO"))

        session = session.step()

        assertEquals(110, session.currentProgramLine?.lineNumber)
        assertEquals(BigDecimal("42"), session.numericVariables["A"]?.value)
        assertEquals("HELLO", session.stringVariables["B$"]?.text)
    }

    fun `test INPUT with invalid numeric value stays paused and shows error`() {
        var session = startSession(
            """
            100 INPUT A
            110 END
            """.trimIndent(),
        )

        session = session.copy(pendingInputValues = mapOf("A" to "ABC"))

        session = session.step()

        assertEquals(100, session.currentProgramLine?.lineNumber)
        assertEquals(TiBasicDebugSessionStatus.Paused, session.status)
        assertNotNull(session.statusMessage)
    }

    fun `test INPUT expands comma separated value list from first field`() {
        var session = startSession(
            """
            100 INPUT A,B,C
            110 END
            """.trimIndent(),
        )

        session = session.copy(pendingInputValues = mapOf("A" to "1,2,3", "B" to "", "C" to ""))

        session = session.step()

        assertEquals(110, session.currentProgramLine?.lineNumber)
        assertEquals(BigDecimal("1"), session.numericVariables["A"]?.value)
        assertEquals(BigDecimal("2"), session.numericVariables["B"]?.value)
        assertEquals(BigDecimal("3"), session.numericVariables["C"]?.value)
    }

    fun `test INPUT with comma separated value count mismatch stays paused`() {
        var session = startSession(
            """
            100 INPUT A,B,C
            110 END
            """.trimIndent(),
        )

        session = session.copy(pendingInputValues = mapOf("A" to "1,2", "B" to "", "C" to ""))

        session = session.step()

        assertEquals(100, session.currentProgramLine?.lineNumber)
        assertEquals(TiBasicDebugSessionStatus.Paused, session.status)
        assertEquals("Combined INPUT values must match target count", session.statusMessage)
    }
}