package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase

private const val REM_LINE_PREFIX = "100 REM "

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

    fun `test no global guides when longest line stays outside default preview distance`() {
        assertEmpty(
            displayColumnGuideColumns(
                longestLineLength = 25,
                columnWidth = TI99_4A_DISPLAY_COLUMNS,
                previewDistance = DEFAULT_DISPLAY_COLUMN_GUIDE_PREVIEW_DISTANCE,
            ),
        )
    }

    fun `test one global guide when longest line enters default preview distance`() {
        assertEquals(
            listOf(28),
            displayColumnGuideColumns(
                longestLineLength = 26,
                columnWidth = TI99_4A_DISPLAY_COLUMNS,
                previewDistance = DEFAULT_DISPLAY_COLUMN_GUIDE_PREVIEW_DISTANCE,
            ),
        )
    }

    fun `test exact display width still shows the matching global guide`() {
        assertEquals(
            listOf(28),
            displayColumnGuideColumns(
                longestLineLength = 28,
                columnWidth = TI99_4A_DISPLAY_COLUMNS,
                previewDistance = DEFAULT_DISPLAY_COLUMN_GUIDE_PREVIEW_DISTANCE,
            ),
        )
    }

    fun `test zero preview distance delays second guide until second boundary is reached`() {
        assertEquals(
            listOf(28),
            displayColumnGuideColumns(
                longestLineLength = 55,
                columnWidth = TI99_4A_DISPLAY_COLUMNS,
                previewDistance = 0,
            ),
        )
    }

    fun `test zero preview distance shows second guide at exact second boundary`() {
        assertEquals(
            listOf(28, 56),
            displayColumnGuideColumns(
                longestLineLength = 56,
                columnWidth = TI99_4A_DISPLAY_COLUMNS,
                previewDistance = 0,
            ),
        )
    }

    fun `test default preview distance shows second guide before second boundary`() {
        assertEquals(
            listOf(28, 56),
            displayColumnGuideColumns(
                longestLineLength = 54,
                columnWidth = TI99_4A_DISPLAY_COLUMNS,
                previewDistance = DEFAULT_DISPLAY_COLUMN_GUIDE_PREVIEW_DISTANCE,
            ),
        )
    }

    fun `test global guides follow longest line across whole file`() {
        val file = configureFile(
            remLine(20) + "\n" +
                remLine(54) + "\n" +
                "120 END",
        )

        assertEquals(
            listOf(28, 56),
            displayColumnGuideColumns(
                longestLineLength = longestLineLength(file.viewProvider.document!!),
                columnWidth = TI99_4A_DISPLAY_COLUMNS,
                previewDistance = DEFAULT_DISPLAY_COLUMN_GUIDE_PREVIEW_DISTANCE,
            ),
        )
    }

    private fun remLine(totalLineLength: Int): String =
        REM_LINE_PREFIX + "A".repeat(totalLineLength - REM_LINE_PREFIX.length)
}
