package com.github.mmrsic.idea.plugins.tibasic.language.runtime.screen

internal data class TiBasicScreenPosition(
    val row: Int,
    val column: Int,
)

internal fun tiBasicScreenWritePositions(
    row: Int,
    column: Int,
    repeatCount: Int,
    horizontal: Boolean,
): List<TiBasicScreenPosition> {
    if (row !in 1..TI_BASIC_SCREEN_ROWS || column !in 1..TI_BASIC_SCREEN_COLUMNS || repeatCount < 0) {
        return emptyList()
    }
    return (0 until repeatCount.coerceAtMost(TI_BASIC_SCREEN_CELLS))
        .mapNotNull { offset -> tiBasicScreenPosition(row, column, offset, horizontal) }
}

internal fun tiBasicScreenPosition(
    row: Int,
    column: Int,
    offset: Int,
    horizontal: Boolean,
): TiBasicScreenPosition? {
    if (row !in 1..TI_BASIC_SCREEN_ROWS || column !in 1..TI_BASIC_SCREEN_COLUMNS) {
        return null
    }
    val wrappedIndex = if (horizontal) {
        ((row - 1) * TI_BASIC_SCREEN_COLUMNS + (column - 1) + offset) % TI_BASIC_SCREEN_CELLS
    } else {
        ((column - 1) * TI_BASIC_SCREEN_ROWS + (row - 1) + offset) % TI_BASIC_SCREEN_CELLS
    }
    return if (horizontal) {
        TiBasicScreenPosition(
            row = wrappedIndex / TI_BASIC_SCREEN_COLUMNS + 1,
            column = wrappedIndex % TI_BASIC_SCREEN_COLUMNS + 1,
        )
    } else {
        TiBasicScreenPosition(
            row = wrappedIndex % TI_BASIC_SCREEN_ROWS + 1,
            column = wrappedIndex / TI_BASIC_SCREEN_ROWS + 1,
        )
    }
}

internal const val TI_BASIC_SCREEN_CELLS = TI_BASIC_SCREEN_COLUMNS * TI_BASIC_SCREEN_ROWS
