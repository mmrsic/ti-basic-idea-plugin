package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase

class TiBasicDisplayColumnHintProviderTest : TiBasicTestBase() {

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
}
