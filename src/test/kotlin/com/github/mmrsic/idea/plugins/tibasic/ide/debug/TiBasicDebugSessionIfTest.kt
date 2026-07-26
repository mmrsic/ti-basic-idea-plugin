package com.github.mmrsic.idea.plugins.tibasic.ide.debug

internal class TiBasicDebugSessionIfTest : TiBasicDebugSessionBaseTest() {

    fun `test IF with non-zero numeric expression jumps to THEN line`() {
        var session = startSession(
            """
            100 LET X=2
            110 IF X-1 THEN 300
            200 PRINT "NO"
            300 PRINT "YES"
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.Paused, session.status)
        assertEquals(300, session.currentProgramLine?.lineNumber)
    }

    fun `test IF with zero numeric expression uses implicit else continuation`() {
        var session = startSession(
            """
            100 LET X=1
            110 IF X-1 THEN 300
            200 PRINT "NO"
            300 PRINT "YES"
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.Paused, session.status)
        assertEquals(200, session.currentProgramLine?.lineNumber)
    }

    fun `test IF with false comparison jumps to explicit ELSE line`() {
        var session = startSession(
            """
            100 LET X=1
            110 IF X>5 THEN 300 ELSE 200
            200 PRINT "NO"
            300 PRINT "YES"
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.Paused, session.status)
        assertEquals(200, session.currentProgramLine?.lineNumber)
    }

    fun `test IF with true string comparison jumps to THEN line`() {
        var session = startSession(
            """
            100 LET A$="YES"
            110 IF A$="YES" THEN 300 ELSE 200
            200 PRINT "NO"
            300 PRINT "OK"
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.Paused, session.status)
        assertEquals(300, session.currentProgramLine?.lineNumber)
    }

    fun `test IF with sum of parenthesized comparisons jumps to THEN line`() {
        var session = startSession(
            """
            100 LET K=74
            110 IF (K=74)+(K=106) THEN 300 ELSE 200
            200 PRINT "NO"
            300 PRINT "YES"
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.Paused, session.status)
        assertEquals(300, session.currentProgramLine?.lineNumber)
    }

    fun `test IF current arguments display shows evaluated numeric subexpressions in evaluation order`() {
        var session = startSession(
            """
            100 LET X=2
            110 IF X-1 THEN 300
            300 PRINT "YES"
            """.trimIndent(),
        )

        session = session.step()

        assertEquals(
            listOf(
                "2 - 1 -> 1",
                "1 -> true",
            ),
            session.currentArgumentDisplays,
        )
    }

    fun `test IF current arguments display shows parenthesized comparison sum evaluation`() {
        var session = startSession(
            """
            100 LET K=74
            110 IF (K=74)+(K=106) THEN 300
            300 PRINT "YES"
            """.trimIndent(),
        )

        session = session.step()

        assertEquals(
            listOf(
                "74 = 74 -> true",
                "true -> -1",
                "74 = 106 -> false",
                "false -> 0",
                "-1 + 0 -> -1",
                "-1 -> true",
            ),
            session.currentArgumentDisplays,
        )
    }

    fun `test IF current arguments display resolves string subexpressions with variable values`() {
        var session = startSession(
            """
            100 LET A$="Y"
            110 IF A$&"ES"="YES" THEN 300
            300 PRINT "YES"
            """.trimIndent(),
        )

        session = session.step()

        assertEquals(
            listOf(
                "\"Y\" & \"ES\" -> \"YES\"",
                "\"YES\" = \"YES\" -> true",
            ),
            session.currentArgumentDisplays,
        )
    }

    fun `test IF with missing THEN target line shows bad line number then stops on next step`() {
        var session = startSession("100 IF 1 THEN 999")

        session = session.step()
        assertEquals(TiBasicDebugSessionStatus.PendingStop, session.status)
        assertEquals(TiBasicDebugMetadata.message(TiBasicDebugMetadata.badLineNumberKey), session.statusMessage)

        session = session.step()
        assertEquals(TiBasicDebugSessionStatus.Stopped, session.status)
    }

}