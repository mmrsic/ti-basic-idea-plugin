package com.github.mmrsic.idea.plugins.tibasic.ide.debug

import com.github.mmrsic.idea.plugins.tibasic.language.model.TiColor

internal class TiBasicDebugSessionCallClearTest : TiBasicDebugSessionBaseTest() {

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

    fun `test CALL CLEAR keeps current debug screen background`() {
        var session = startSession(
            """
            100 CALL SCREEN(2)
            110 CALL CLEAR
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()

        assertEquals(TiColor.Black, session.screenContents.screenBackground)
    }
}