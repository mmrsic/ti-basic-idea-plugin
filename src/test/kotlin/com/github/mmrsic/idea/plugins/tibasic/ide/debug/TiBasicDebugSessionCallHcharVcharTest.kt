package com.github.mmrsic.idea.plugins.tibasic.ide.debug

internal class TiBasicDebugSessionCallHcharVcharTest : TiBasicDebugSessionBaseTest() {

    fun `test CALL HCHAR current arguments display shows resolved values`() {
        val session = startSession("100 CALL HCHAR(2,3,65,4)")

        assertEquals(
            """
            row = 02
            column = 03
            character-code = 65
            repeat = 04
            """.trimIndent(),
            session.currentArgumentsDisplay,
        )
    }

    fun `test CALL HCHAR writes repeated character codes across row wrap`() {
        var session = startSession(
            """
            100 CALL HCHAR(24,32,65,3)
            110 PRINT "A"
            """.trimIndent(),
        )

        session = session.step()

        assertEquals(65, session.screenContents.characterCodes[23][31])
        assertEquals(65, session.screenContents.characterCodes[0][0])
        assertEquals(65, session.screenContents.characterCodes[0][1])
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test CALL VCHAR writes repeated character codes across column wrap`() {
        var session = startSession(
            """
            100 CALL VCHAR(24,32,66,3)
            110 PRINT "A"
            """.trimIndent(),
        )

        session = session.step()

        assertEquals(66, session.screenContents.characterCodes[23][31])
        assertEquals(66, session.screenContents.characterCodes[0][0])
        assertEquals(66, session.screenContents.characterCodes[1][0])
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test CALL HCHAR caps screen writes after seven hundred sixty eight placements`() {
        var session = startSession(
            """
            100 CALL HCHAR(1,1,67,1000)
            110 PRINT "A"
            """.trimIndent(),
        )

        session = session.step()

        assertTrue(session.screenContents.characterCodes.flatten().all { code -> code == 67 })
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }
}