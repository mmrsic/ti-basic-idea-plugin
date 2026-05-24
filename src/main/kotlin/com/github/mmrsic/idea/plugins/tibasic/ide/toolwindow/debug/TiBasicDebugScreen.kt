package com.github.mmrsic.idea.plugins.tibasic.ide.toolwindow.debug

import com.github.mmrsic.idea.plugins.tibasic.editor.asciiCharacterName
import com.github.mmrsic.idea.plugins.tibasic.editor.paintTiBasicCharPattern
import com.github.mmrsic.idea.plugins.tibasic.language.model.TiColor
import com.github.mmrsic.idea.plugins.tibasic.language.runtime.screen.TI_BASIC_SCREEN_COLUMNS
import com.github.mmrsic.idea.plugins.tibasic.language.runtime.screen.TI_BASIC_SCREEN_ROWS
import com.github.mmrsic.idea.plugins.tibasic.language.runtime.screen.TI_BASIC_SPACE_CHARACTER_CODE
import com.github.mmrsic.idea.plugins.tibasic.language.runtime.screen.tiBasicCharacterPattern
import com.intellij.ui.JBColor
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GradientPaint
import java.awt.Rectangle
import java.awt.RenderingHints
import kotlin.math.roundToInt
import javax.swing.JComponent

internal data class TiBasicDebugScreenState(
    val screenBackground: TiColor,
    val characterForeground: TiColor,
    val characterBackground: TiColor,
    val characterCodes: List<List<Int>>,
    val characterPatterns: Map<Int, String> = emptyMap(),
) {
    companion object {
        fun initial(): TiBasicDebugScreenState =
            TiBasicDebugScreenState(
                screenBackground = INITIAL_SCREEN_BACKGROUND,
                characterForeground = INITIAL_CHARACTER_FOREGROUND,
                characterBackground = INITIAL_CHARACTER_BACKGROUND,
                characterCodes = initialCharacterCodes(),
            )
    }
}

private fun initialCharacterCodes(): List<List<Int>> =
    List(TI_BASIC_SCREEN_ROWS) { rowIndex ->
        MutableList(TI_BASIC_SCREEN_COLUMNS) { TI_BASIC_SPACE_CHARACTER_CODE }
            .also { row ->
                if (rowIndex == INITIAL_RUN_PROMPT_ROW_INDEX) {
                    INITIAL_RUN_PROMPT.forEachIndexed { offset, character ->
                        row[INITIAL_RUN_PROMPT_COLUMN_INDEX + offset] = character.code
                    }
                }
            }
    }

internal class TiBasicDebugScreenComponent(
    internal var state: TiBasicDebugScreenState = TiBasicDebugScreenState.initial(),
) : JComponent() {
    internal var keepAspectRatio: Boolean = true
        set(value) {
            field = value
            repaint()
        }

    init {
        preferredSize = Dimension(
            DEBUG_SCREEN_WIDTH,
            DEBUG_SCREEN_HEIGHT,
        )
    }

    override fun paintComponent(graphics: Graphics) {
        super.paintComponent(graphics)
        val g = graphics as Graphics2D
        g.paint = GradientPaint(
            0f,
            0f,
            JBColor(DEBUG_SCREEN_SHADE_LIGHT, DEBUG_SCREEN_SHADE_DARK),
            width.toFloat(),
            height.toFloat(),
            JBColor(DEBUG_SCREEN_SHADE_DARK, DEBUG_SCREEN_SHADE_LIGHT),
        )
        g.fillRect(0, 0, width, height)
        val screenBounds = scaledScreenBounds()
        val screenBackgroundColor = JBColor(state.screenBackground.rgbValue, state.screenBackground.rgbValue)
        g.color = screenBackgroundColor
        g.fillRect(screenBounds.x, screenBounds.y, screenBounds.width, screenBounds.height)
        val screenGraphics = g.create(screenBounds.x, screenBounds.y, screenBounds.width, screenBounds.height) as Graphics2D
        try {
            screenGraphics.scale(
                screenBounds.width.toDouble() / DEBUG_SCREEN_WIDTH,
                screenBounds.height.toDouble() / DEBUG_SCREEN_HEIGHT,
            )
            screenGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF)
            screenGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
            screenGraphics.font = Font(Font.MONOSPACED, Font.PLAIN, DEBUG_SCREEN_FONT_SIZE)
            state.characterCodes.forEachIndexed { rowIndex, row ->
                row.forEachIndexed { columnIndex, code ->
                    paintCodeCell(screenGraphics, rowIndex, columnIndex, code)
                }
            }
        } finally {
            screenGraphics.dispose()
        }
    }

    internal fun scaledScreenBounds(): Rectangle {
        if (!keepAspectRatio) {
            return Rectangle(0, 0, width, height)
        }
        val scale = minOf(
            width.toDouble() / DEBUG_SCREEN_WIDTH,
            height.toDouble() / DEBUG_SCREEN_HEIGHT,
        )
        val scaledWidth = (DEBUG_SCREEN_WIDTH * scale).roundToInt()
        val scaledHeight = (DEBUG_SCREEN_HEIGHT * scale).roundToInt()
        return Rectangle(
            (width - scaledWidth) / 2,
            (height - scaledHeight) / 2,
            scaledWidth,
            scaledHeight,
        )
    }

    private fun paintCodeCell(
        graphics: Graphics2D,
        rowIndex: Int,
        columnIndex: Int,
        code: Int,
    ) {
        val x = DEBUG_SCREEN_SIDE_MARGIN + columnIndex * DEBUG_SCREEN_CELL_SIZE
        val y = rowIndex * DEBUG_SCREEN_CELL_SIZE
        val cellBackground = state.characterBackground.takeUnless { color -> color == TiColor.Transparent } ?: state.screenBackground
        graphics.color = JBColor(cellBackground.rgbValue, cellBackground.rgbValue)
        graphics.fillRect(x, y, DEBUG_SCREEN_CELL_SIZE, DEBUG_SCREEN_CELL_SIZE)
        tiBasicCharacterPattern(code, state.characterPatterns)?.let { pattern ->
            paintTiBasicCharPattern(
                g = graphics,
                x = x,
                y = y,
                pixelSize = 1,
                hexPattern = pattern,
                fg = state.characterForeground,
                bg = cellBackground,
            )
            return
        }
        val displayText = debugScreenDisplayText(code) ?: return
        graphics.color = JBColor(state.characterForeground.rgbValue, state.characterForeground.rgbValue)
        val metrics = graphics.getFontMetrics(graphics.font)
        val textX = x + ((DEBUG_SCREEN_CELL_SIZE - metrics.stringWidth(displayText)) / 2)
        val textY = y + ((DEBUG_SCREEN_CELL_SIZE - metrics.height) / 2) + metrics.ascent - DEBUG_SCREEN_TEXT_BASELINE_ADJUSTMENT
        graphics.drawString(displayText, textX, textY)
    }
}

internal fun debugScreenDisplayText(code: Int): String? =
    asciiCharacterName(code)
        ?.takeIf { name -> name.length == 1 }
        ?: code
            .takeIf { value -> value != TI_BASIC_SPACE_CHARACTER_CODE }
            ?.toString(16)
            ?.uppercase()
            ?.padStart(DEBUG_SCREEN_HEX_WIDTH, '0')

private const val DEBUG_SCREEN_CELL_SIZE = 8
private const val DEBUG_SCREEN_SIDE_MARGIN = 4
private const val DEBUG_SCREEN_WIDTH = TI_BASIC_SCREEN_COLUMNS * DEBUG_SCREEN_CELL_SIZE + 2 * DEBUG_SCREEN_SIDE_MARGIN
private const val DEBUG_SCREEN_HEIGHT = TI_BASIC_SCREEN_ROWS * DEBUG_SCREEN_CELL_SIZE
private const val DEBUG_SCREEN_FONT_SIZE = 8
private const val DEBUG_SCREEN_HEX_WIDTH = 2
private const val DEBUG_SCREEN_TEXT_BASELINE_ADJUSTMENT = 1
private const val INITIAL_RUN_PROMPT_ROW_INDEX = 22
private const val INITIAL_RUN_PROMPT_COLUMN_INDEX = 2
private const val INITIAL_RUN_PROMPT = "> run"
private const val DEBUG_SCREEN_SHADE_LIGHT = 0xD3D3D3
private const val DEBUG_SCREEN_SHADE_DARK = 0x8C8C8C
private val INITIAL_SCREEN_BACKGROUND = TiColor.LightGreen
private val INITIAL_CHARACTER_FOREGROUND = TiColor.Black
private val INITIAL_CHARACTER_BACKGROUND = TiColor.Transparent
