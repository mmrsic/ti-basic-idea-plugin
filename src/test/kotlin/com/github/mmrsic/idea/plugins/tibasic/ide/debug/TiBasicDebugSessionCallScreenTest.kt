package com.github.mmrsic.idea.plugins.tibasic.ide.debug

import com.github.mmrsic.idea.plugins.tibasic.language.model.TiColor

internal class TiBasicDebugSessionCallScreenTest : TiBasicDebugSessionBaseTest() {


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

    fun `test CALL SCREEN current arguments display shows resolved color code and name`() {
        var session = startSession(
            """
            100 LET C=2
            110 CALL SCREEN(C+1)
            """.trimIndent(),
        )

        session = session.step()

        assertEquals("color-code = 03 (Medium Green)", session.currentArgumentsDisplay)
    }

    fun `test CALL SCREEN current arguments display shows incorrect expression`() {
        val session = startSession("100 CALL SCREEN(1/0)")

        assertEquals("<incorrect expression>", session.currentArgumentsDisplay)
    }

    fun `test CALL SCREEN current arguments display shows string number mismatch`() {
        val session = startSession("100 CALL SCREEN(\"A\")")

        assertEquals("<incorrect expression> (string-number-mismatch)", session.currentArgumentsDisplay)
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

}