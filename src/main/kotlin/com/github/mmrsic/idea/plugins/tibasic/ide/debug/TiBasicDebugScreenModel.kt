package com.github.mmrsic.idea.plugins.tibasic.ide.debug

import com.github.mmrsic.idea.plugins.tibasic.language.model.TiColor
import com.github.mmrsic.idea.plugins.tibasic.language.runtime.screen.TI_BASIC_SCREEN_COLUMNS
import com.github.mmrsic.idea.plugins.tibasic.language.runtime.screen.TI_BASIC_SCREEN_ROWS
import com.github.mmrsic.idea.plugins.tibasic.language.runtime.screen.TI_BASIC_SPACE_CHARACTER_CODE

internal data class TiBasicDebugScreenContents(
    val screenBackground: TiColor = INITIAL_SCREEN_BACKGROUND,
    val characterCodes: List<List<Int>> = initialDebugScreenCharacterCodes(),
    val characterPatterns: Map<Int, String> = emptyMap(),
    val characterSetColors: Map<Int, TiBasicDebugCharacterSetColors> = initialDebugCharacterSetColors(),
    val printCursorRow: Int = INITIAL_PRINT_CURSOR_ROW,
    val printCursorColumn: Int = INITIAL_PRINT_CURSOR_COLUMN,
)

internal data class TiBasicDebugCharacterSetColors(
    val fg: TiColor,
    val bg: TiColor,
)

internal fun initialDebugScreenCharacterCodes(): List<List<Int>> =
    blankDebugScreenCharacterCodes().mapIndexed { rowIndex, row ->
        row.toMutableList().also { mutableRow ->
            if (rowIndex == INITIAL_RUN_PROMPT_ROW_INDEX) {
                INITIAL_RUN_PROMPT.forEachIndexed { offset, character ->
                    mutableRow[INITIAL_RUN_PROMPT_COLUMN_INDEX + offset] = character.code
                }
            }
        }
    }

internal fun blankDebugScreenCharacterCodes(): List<List<Int>> =
    List(TI_BASIC_SCREEN_ROWS) {
        List(TI_BASIC_SCREEN_COLUMNS) { TI_BASIC_SPACE_CHARACTER_CODE }
    }

internal fun initialDebugCharacterSetColors(): Map<Int, TiBasicDebugCharacterSetColors> =
    (FIRST_CHARACTER_SET..LAST_CHARACTER_SET).associateWith {
        TiBasicDebugCharacterSetColors(
            fg = INITIAL_CHARACTER_SET_FOREGROUND,
            bg = INITIAL_CHARACTER_SET_BACKGROUND,
        )
    }

private const val INITIAL_RUN_PROMPT_ROW_INDEX = 22
private const val INITIAL_RUN_PROMPT_COLUMN_INDEX = 2
private const val INITIAL_RUN_PROMPT = "> run"
internal const val INITIAL_PRINT_CURSOR_ROW = TI_BASIC_SCREEN_ROWS
internal const val INITIAL_PRINT_CURSOR_COLUMN = 3
internal val INITIAL_SCREEN_BACKGROUND = TiColor.LightGreen
internal val INITIAL_CHARACTER_SET_FOREGROUND = TiColor.Black
internal val INITIAL_CHARACTER_SET_BACKGROUND = TiColor.Transparent
private const val FIRST_CHARACTER_SET = 1
private const val LAST_CHARACTER_SET = 16
