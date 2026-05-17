package com.github.mmrsic.idea.plugins.tibasic.action.preview

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase
import com.github.mmrsic.idea.plugins.tibasic.lang.TiColor

class TiBasicScreenPreviewEvaluatorTest : TiBasicTestBase() {

    fun `test CALL HCHAR writes characters horizontally`() {
        val file = configureFile("100 CALL HCHAR(2,3,65,3)")

        val preview = evaluateSelectedScreenPreview(file.lines())

        assertEquals("A", preview.cellAt(2, 3).displayText)
        assertEquals("A", preview.cellAt(2, 4).displayText)
        assertEquals("A", preview.cellAt(2, 5).displayText)
    }

    fun `test CALL VCHAR writes characters vertically`() {
        val file = configureFile("100 CALL VCHAR(2,3,66,3)")

        val preview = evaluateSelectedScreenPreview(file.lines())

        assertEquals("B", preview.cellAt(2, 3).displayText)
        assertEquals("B", preview.cellAt(3, 3).displayText)
        assertEquals("B", preview.cellAt(4, 3).displayText)
    }

    fun `test selection local LET values are used for preview`() {
        val file = configureFile(
            """
            100 LET R=2
            110 LET C=4
            120 LET N=65
            130 CALL HCHAR(R,C,N)
            """.trimIndent(),
        )

        val preview = evaluateSelectedScreenPreview(file.lines())

        assertEquals("A", preview.cellAt(2, 4).displayText)
        assertFalse(preview.isPartial)
    }

    fun `test CALL CHAR and CALL COLOR affect final cell rendering`() {
        val file = configureFile(
            """
            100 CALL HCHAR(1,1,65)
            110 CALL CHAR(65,"FF")
            120 CALL COLOR(5,2,3)
            130 CALL SCREEN(4)
            """.trimIndent(),
        )

        val preview = evaluateSelectedScreenPreview(file.lines())
        val cell = preview.cellAt(1, 1)

        assertEquals("FF00000000000000", cell.pattern)
        assertEquals(TiColor.Black, cell.fg)
        assertEquals(TiColor.MediumGreen, cell.bg)
    }

    fun `test CALL SCREEN affects default background without CALL COLOR`() {
        val file = configureFile(
            """
            100 CALL HCHAR(1,1,65)
            110 CALL SCREEN(5)
            """.trimIndent(),
        )

        val preview = evaluateSelectedScreenPreview(file.lines())

        assertEquals(TiColor.DarkBlue, preview.cellAt(1, 1).bg)
    }

    fun `test CALL CLEAR resets screen contents and preview state`() {
        val file = configureFile(
            """
            100 CALL CHAR(65,"FF")
            110 CALL COLOR(5,2,3)
            120 CALL HCHAR(1,1,65)
            130 CALL CLEAR
            140 CALL HCHAR(1,1,65)
            """.trimIndent(),
        )

        val preview = evaluateSelectedScreenPreview(file.lines())
        val cell = preview.cellAt(1, 1)

        assertNull(cell.pattern)
        assertEquals("A", cell.displayText)
        assertEquals(TiColor.Black, cell.fg)
        assertEquals(TiColor.White, cell.bg)
    }

    fun `test unresolved preview statement yields partial preview warning`() {
        val file = configureFile(
            """
            100 INPUT R
            110 CALL HCHAR(R,1,65)
            """.trimIndent(),
        )

        val preview = evaluateSelectedScreenPreview(file.lines())

        assertTrue(preview.isPartial)
        assertEquals(1, preview.warnings.size)
        assertTrue(preview.warnings.single().contains("Line 110"))
        assertNull(preview.cellAt(1, 1).displayText)
    }

    fun `test writes are clipped to 32x24 screen`() {
        val file = configureFile("100 CALL HCHAR(24,31,65,4)")

        val preview = evaluateSelectedScreenPreview(file.lines())

        assertEquals("A", preview.cellAt(24, 31).displayText)
        assertEquals("A", preview.cellAt(24, 32).displayText)
    }
}
