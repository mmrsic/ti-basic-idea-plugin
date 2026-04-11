package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase

class TiBasicDisplayColumnGuidesTest : TiBasicTestBase() {

    fun `test no hints for line shorter than 28 chars`() {
        assertEmpty(displayColumnBreakOffsets(0, 10, TI99_4A_DISPLAY_COLUMNS))
    }

    fun `test no hints for line of exactly 28 chars`() {
        assertEmpty(displayColumnBreakOffsets(0, 28, TI99_4A_DISPLAY_COLUMNS))
    }

    fun `test hint at offset 28 for 29-char line starting at 0`() {
        assertEquals(listOf(28), displayColumnBreakOffsets(0, 29, TI99_4A_DISPLAY_COLUMNS))
    }

    fun `test hint at offset 28 only for line of exactly 56 chars`() {
        assertEquals(listOf(28), displayColumnBreakOffsets(0, 56, TI99_4A_DISPLAY_COLUMNS))
    }

    fun `test hints at offsets 28 and 56 for 57-char line starting at 0`() {
        assertEquals(listOf(28, 56), displayColumnBreakOffsets(0, 57, TI99_4A_DISPLAY_COLUMNS))
    }

    fun `test hint offset accounts for non-zero line start`() {
        assertEquals(listOf(100 + 28), displayColumnBreakOffsets(100, 30, TI99_4A_DISPLAY_COLUMNS))
    }

    fun `test three hints for 85-char line`() {
        assertEquals(listOf(28, 56, 84), displayColumnBreakOffsets(0, 85, TI99_4A_DISPLAY_COLUMNS))
    }

    fun `test no global guides for longest line shorter than 28 chars`() {
        assertEmpty(displayColumnGuideColumns(10, TI99_4A_DISPLAY_COLUMNS))
    }

    fun `test no global guides for longest line of exactly 28 chars`() {
        assertEmpty(displayColumnGuideColumns(28, TI99_4A_DISPLAY_COLUMNS))
    }

    fun `test one global guide for longest line of 29 chars`() {
        assertEquals(listOf(28), displayColumnGuideColumns(29, TI99_4A_DISPLAY_COLUMNS))
    }

    fun `test one global guide for longest line of exactly 56 chars`() {
        assertEquals(listOf(28), displayColumnGuideColumns(56, TI99_4A_DISPLAY_COLUMNS))
    }

    fun `test global guides follow longest line across whole file`() {
        val file = configureFile(
            "100 PRINT \"SHORT\"\n" +
                "110 PRINT \"123456789012345678901234567890123456789012345678901234567\"\n" +
                "120 END",
        )

        assertEquals(
            listOf(28, 56),
            displayColumnGuideColumns(longestLineLength(file.viewProvider.document!!), TI99_4A_DISPLAY_COLUMNS),
        )
    }
}

