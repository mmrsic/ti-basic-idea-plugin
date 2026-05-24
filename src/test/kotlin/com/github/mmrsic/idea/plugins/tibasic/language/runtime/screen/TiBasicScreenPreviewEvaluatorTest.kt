package com.github.mmrsic.idea.plugins.tibasic.language.runtime.screen

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase
import com.github.mmrsic.idea.plugins.tibasic.language.model.TiColor

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

    fun `test CALL HCHAR wraps to the next row at the right screen edge`() {
        val file = configureFile("100 CALL HCHAR(2,31,65,4)")

        val preview = evaluateSelectedScreenPreview(file.lines())

        assertEquals("A", preview.cellAt(2, 31).displayText)
        assertEquals("A", preview.cellAt(2, 32).displayText)
        assertEquals("A", preview.cellAt(3, 1).displayText)
        assertEquals("A", preview.cellAt(3, 2).displayText)
    }

    fun `test CALL HCHAR wraps from the last screen cell to the top left`() {
        val file = configureFile("100 CALL HCHAR(24,32,65,2)")

        val preview = evaluateSelectedScreenPreview(file.lines())

        assertEquals("A", preview.cellAt(24, 32).displayText)
        assertEquals("A", preview.cellAt(1, 1).displayText)
    }

    fun `test CALL VCHAR wraps to the next column at the bottom screen edge`() {
        val file = configureFile("100 CALL VCHAR(23,2,66,4)")

        val preview = evaluateSelectedScreenPreview(file.lines())

        assertEquals("B", preview.cellAt(23, 2).displayText)
        assertEquals("B", preview.cellAt(24, 2).displayText)
        assertEquals("B", preview.cellAt(1, 3).displayText)
        assertEquals("B", preview.cellAt(2, 3).displayText)
    }

    fun `test CALL VCHAR wraps from the last screen cell to the top left`() {
        val file = configureFile("100 CALL VCHAR(24,32,66,2)")

        val preview = evaluateSelectedScreenPreview(file.lines())

        assertEquals("B", preview.cellAt(24, 32).displayText)
        assertEquals("B", preview.cellAt(1, 1).displayText)
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

    fun `test CALL SCREEN rounds decimal colors before applying the screen background`() {
        val file = configureFile(
            """
            100 CALL HCHAR(1,1,65)
            110 CALL SCREEN(4.6)
            """.trimIndent(),
        )

        val preview = evaluateSelectedScreenPreview(file.lines())

        assertEquals(TiColor.DarkBlue, preview.cellAt(1, 1).bg)
    }

    fun `test transparent CALL COLOR entries use the current CALL SCREEN background`() {
        val file = configureFile(
            """
            100 CALL SCREEN(5)
            110 CALL COLOR(5,1,1)
            120 CALL HCHAR(1,1,65)
            """.trimIndent(),
        )

        val preview = evaluateSelectedScreenPreview(file.lines())
        val cell = preview.cellAt(1, 1)

        assertEquals(TiColor.DarkBlue, cell.fg)
        assertEquals(TiColor.DarkBlue, cell.bg)
    }

    fun `test transparent CALL SCREEN is treated like black for transparent CALL COLOR entries`() {
        val file = configureFile(
            """
            100 CALL SCREEN(1)
            110 CALL COLOR(5,1,3)
            120 CALL HCHAR(1,1,65)
            """.trimIndent(),
        )

        val preview = evaluateSelectedScreenPreview(file.lines())
        val cell = preview.cellAt(1, 1)

        assertEquals(TiColor.Black, cell.fg)
        assertEquals(TiColor.MediumGreen, cell.bg)
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

        assertEquals(tiBasicCharacterPattern(65), cell.pattern)
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

    fun `test repeat count zero leaves the screen unchanged`() {
        val file = configureFile("100 CALL HCHAR(24,31,65,0)")

        val preview = evaluateSelectedScreenPreview(file.lines())

        assertNull(preview.cellAt(24, 31).displayText)
        assertNull(preview.cellAt(24, 32).displayText)
    }
}
