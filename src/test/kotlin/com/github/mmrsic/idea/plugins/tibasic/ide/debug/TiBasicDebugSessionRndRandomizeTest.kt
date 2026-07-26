package com.github.mmrsic.idea.plugins.tibasic.ide.debug

import java.math.BigDecimal

internal class TiBasicDebugSessionRndRandomizeTest : TiBasicDebugSessionBaseTest() {

    fun `test RND produces the same initial sequence in separate debugger sessions`() {
        var firstSession = startSession(
            """
            100 LET A=RND
            110 LET B=RND
            """.trimIndent(),
        )
        var secondSession = startSession(
            """
            100 LET A=RND
            110 LET B=RND
            """.trimIndent(),
        )

        firstSession = firstSession.step()
        firstSession = firstSession.step()
        secondSession = secondSession.step()
        secondSession = secondSession.step()

        val firstA = firstSession.numericVariables["A"]?.value
        val firstB = firstSession.numericVariables["B"]?.value
        val secondA = secondSession.numericVariables["A"]?.value
        val secondB = secondSession.numericVariables["B"]?.value
        assertEquals("0.52918778230732", firstSession.numericVariables["A"]?.usualDisplay)
        assertEquals("0.3913360723005", firstSession.numericVariables["B"]?.usualDisplay)
        assertEquals(firstA, secondA)
        assertEquals(firstB, secondB)
        assertTrue(firstA != null && firstA > BigDecimal.ZERO && firstA < BigDecimal.ONE)
        assertTrue(firstB != null && firstB > BigDecimal.ZERO && firstB < BigDecimal.ONE)
    }

    fun `test RANDOMIZE with equal seed reproduces the same RND sequence`() {
        var session = startSession(
            """
            100 RANDOMIZE 42.9
            110 LET A=RND
            120 LET B=RND
            130 RANDOMIZE 42
            140 LET C=RND
            150 LET D=RND
            """.trimIndent(),
        )

        repeat(6) {
            session = session.step()
        }

        assertEquals(session.numericVariables["A"]?.value, session.numericVariables["C"]?.value)
        assertEquals(session.numericVariables["B"]?.value, session.numericVariables["D"]?.value)
    }

}