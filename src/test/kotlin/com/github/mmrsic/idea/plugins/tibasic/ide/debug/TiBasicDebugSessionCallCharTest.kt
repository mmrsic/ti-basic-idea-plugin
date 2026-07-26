package com.github.mmrsic.idea.plugins.tibasic.ide.debug

internal class TiBasicDebugSessionCallCharTest : TiBasicDebugSessionBaseTest() {

    fun `test CALL CHAR updates debug character pattern override`() {
        var session = startSession(
            """
            100 CALL CHAR(65,"F0")
            110 PRINT "A"
            """.trimIndent(),
        )

        session = session.step()

        assertEquals("F000000000000000", session.screenContents.characterPatterns[65])
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test CALL CHAR current arguments display shows code pattern and pixel representation label`() {
        val session = startSession("100 CALL CHAR(65,\"F0\")")

        assertEquals(
            """
            ascii-code = 65
            pattern-string = F000000000000000
            (pixel-representation)
            """.trimIndent(),
            session.currentArgumentsDisplay,
        )
        assertEquals("F000000000000000", session.currentArgumentPatternPreview)
    }

    fun `test CALL CHAR ignores digits after the sixteenth`() {
        var session = startSession(
            """
            100 CALL CHAR(65,"1234567890ABCDEF99")
            110 PRINT "A"
            """.trimIndent(),
        )

        session = session.step()

        assertEquals("1234567890ABCDEF", session.screenContents.characterPatterns[65])
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test CALL CHAR current arguments display warns about ignored tail`() {
        val session = startSession("100 CALL CHAR(65,\"1234567890ABCDEF99\")")

        assertEquals(
            """
            ascii-code = 65
            pattern-string = 1234567890ABCDEF (ignored tail: 99)
            (pixel-representation)
            """.trimIndent(),
            session.currentArgumentsDisplay,
        )
        assertEquals("1234567890ABCDEF", session.currentArgumentPatternPreview)
    }

    fun `test CALL CHAR with lowercase pattern shows bad value`() {
        val previewSession = startSession("100 CALL CHAR(65,\"ff\")")

        assertEquals(
            """
            ascii-code = 65
            pattern-string = ff
            Bad Value: pattern-string=ff
            """.trimIndent(),
            previewSession.currentArgumentsDisplay,
        )
        assertNull(previewSession.currentArgumentPatternPreview)

        var session = previewSession

        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.PendingStop, session.status)
        assertEquals(TiBasicDebugMetadata.message(TiBasicDebugMetadata.badValueKey, "pattern-string=ff"), session.statusMessage)
    }

}