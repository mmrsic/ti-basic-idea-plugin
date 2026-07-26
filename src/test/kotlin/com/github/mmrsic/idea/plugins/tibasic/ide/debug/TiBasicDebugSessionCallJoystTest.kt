package com.github.mmrsic.idea.plugins.tibasic.ide.debug

internal class TiBasicDebugSessionCallJoystTest : TiBasicDebugSessionBaseTest() {

    fun `test CALL JOYST defaults to centered position`() {
        var session = startSession(
            """
            100 CALL JOYST(1,X,Y)
            110 PRINT X
            """.trimIndent(),
        )

        assertEquals(1, session.joystickRequest?.keyUnit)
        assertEquals(0, session.joystickRequest?.position?.x)
        assertEquals(0, session.joystickRequest?.position?.y)
        assertEquals("center", session.joystickRequest?.position?.compactDisplay)

        session = session.step()

        assertEquals("0", session.numericVariables["X"]?.usualDisplay)
        assertEquals("0", session.numericVariables["Y"]?.usualDisplay)
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test CALL JOYST applies selected joystick position`() {
        var session = startSession(
            """
            100 CALL JOYST(2,X,Y)
            110 PRINT X
            """.trimIndent(),
        ).copy(keyboardScanInput = "4,-4")

        session = session.step()

        assertEquals("4", session.numericVariables["X"]?.usualDisplay)
        assertEquals("-4", session.numericVariables["Y"]?.usualDisplay)
        assertEquals(110, session.currentProgramLine?.lineNumber)
        assertNull(session.joystickRequest)
    }

}