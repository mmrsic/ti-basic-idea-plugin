package com.github.mmrsic.idea.plugins.tibasic.language.analysis.calls

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase
import com.github.mmrsic.idea.plugins.tibasic.language.model.TiColor

class TiBasicCallColorAssignmentCollectorTest : TiBasicTestBase() {

    fun `test collector keeps repeated CALL COLOR assignments as separate entries`() {
        val file = configureFile(
            """
            100 CALL COLOR(5,7,16)
            110 CALL COLOR(5,7,16)
            120 CALL COLOR(5,2,3)
            """.trimIndent(),
        )

        val assignments = collectCallColorAssignments(file)

        assertEquals(listOf(5, 5, 5), assignments.map { it.set })
        assertEquals(List(3) { 64..71 }, assignments.map { it.codeRange })
        assertEquals(listOf(TiColor.DarkRed, TiColor.DarkRed, TiColor.Black), assignments.map { it.fg })
        assertEquals(listOf(TiColor.White, TiColor.White, TiColor.MediumGreen), assignments.map { it.bg })
        assertEquals(listOf(100, 110, 120), assignments.map { it.lineNumber })
    }

    fun `test collector resolves CALL COLOR arguments from READ DATA`() {
        val file = configureFile(
            """
            100 READ S,FG,BG
            110 DATA 5,7,16
            120 CALL COLOR(S,FG,BG)
            """.trimIndent(),
        )

        val assignments = collectCallColorAssignments(file)

        assertEquals(1, assignments.size)
        assertEquals(5, assignments.single().set)
        assertEquals(64..71, assignments.single().codeRange)
        assertEquals(TiColor.DarkRed, assignments.single().fg)
        assertEquals(TiColor.White, assignments.single().bg)
        assertEquals(120, assignments.single().lineNumber)
    }

    fun `test collector tracks the active CALL SCREEN background for transparent CALL COLOR values`() {
        val file = configureFile(
            """
            100 CALL SCREEN(5)
            110 CALL COLOR(5,1,16)
            120 CALL SCREEN(1)
            130 CALL COLOR(5,1,1)
            """.trimIndent(),
        )

        val assignments = collectCallColorAssignments(file)

        assertEquals(listOf(TiColor.DarkBlue, TiColor.Black), assignments.map { it.screenBackground })
    }

    fun `test collector omits invalid or unresolved CALL COLOR assignments`() {
        val file = configureFile(
            """
            100 CALL COLOR(0,7,16)
            110 CALL COLOR(5,0,16)
            120 INPUT S
            130 CALL COLOR(S,7,16)
            """.trimIndent(),
        )

        val assignments = collectCallColorAssignments(file)

        assertTrue(assignments.isEmpty())
    }
}
