package com.github.mmrsic.idea.plugins.tibasic.ide.debug

internal class TiBasicDebugSessionForNextTest : TiBasicDebugSessionBaseTest() {

    fun `test FOR current arguments display shows evaluated expressions with explicit increment`() {
        var session = startSession(
            """
            100 LET A=2
            110 LET B=4
            120 FOR I=A+1 TO B*2 STEP A-1
            130 NEXT I
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()

        assertEquals(
            listOf(
                "initial-value = 03",
                "limit = 08",
                "increment = 01",
                "(iterations = 6)",
            ),
            session.currentArgumentDisplays,
        )
    }

    fun `test FOR current arguments display uses implicit increment one`() {
        val session = startSession(
            """
            100 FOR I=2 TO 5
            110 NEXT I
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                "initial-value = 02",
                "limit = 05",
                "increment = 01",
                "(iterations = 4)",
            ),
            session.currentArgumentDisplays,
        )
    }

    fun `test FOR step assigns initial value to control variable`() {
        var session = startSession(
            """
            100 LET A=2
            110 FOR I=A+1 TO 5
            120 PRINT I
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()

        assertEquals("3", session.numericVariables["I"]?.usualDisplay)
        assertEquals(120, session.currentProgramLine?.lineNumber)
    }

    fun `test NEXT current arguments display shows increment adjusted control variable and jump decision`() {
        var session = startSession(
            """
            100 LET A=2
            110 LET B=1
            120 FOR I=A+1 TO 5 STEP B
            130 NEXT I
            140 PRINT I
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()
        session = session.step()

        assertEquals(
            listOf(
                "increment = 01",
                "control-variable I = 4",
                "limit = 05 (jump to 130)",
            ),
            session.currentArgumentDisplays,
        )
    }

    fun `test NEXT step adjusts control variable and jumps back into loop`() {
        var session = startSession(
            """
            100 LET A=2
            110 LET B=1
            120 FOR I=A+1 TO 5 STEP B
            130 NEXT I
            140 PRINT I
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()
        session = session.step()
        session = session.step()

        assertEquals("4", session.numericVariables["I"]?.usualDisplay)
        assertEquals(130, session.currentProgramLine?.lineNumber)
    }

    fun `test NEXT current arguments display shows loop end decision when limit is exceeded`() {
        var session = startSession(
            """
            100 FOR I=4 TO 5
            110 NEXT I
            120 PRINT I
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()

        assertEquals(
            listOf(
                "increment = 01",
                "control-variable I = 6",
                "limit = 05 (loop end)",
            ),
            session.currentArgumentDisplays,
        )
    }

    fun `test NEXT step ends loop when incremented value exceeds limit`() {
        var session = startSession(
            """
            100 FOR I=4 TO 5
            110 NEXT I
            120 PRINT I
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()
        session = session.step()

        assertEquals("6", session.numericVariables["I"]?.usualDisplay)
        assertEquals(120, session.currentProgramLine?.lineNumber)
    }

}