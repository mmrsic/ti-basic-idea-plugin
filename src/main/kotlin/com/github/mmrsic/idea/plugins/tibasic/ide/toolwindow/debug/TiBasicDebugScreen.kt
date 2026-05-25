package com.github.mmrsic.idea.plugins.tibasic.ide.toolwindow.debug

import com.github.mmrsic.idea.plugins.tibasic.editor.asciiCharacterName
import com.github.mmrsic.idea.plugins.tibasic.editor.callColorCharacterSetRange
import com.github.mmrsic.idea.plugins.tibasic.editor.paintTiBasicCharPattern
import com.github.mmrsic.idea.plugins.tibasic.ide.debug.TiBasicDebugCharacterSetColors
import com.github.mmrsic.idea.plugins.tibasic.ide.debug.INITIAL_SCREEN_BACKGROUND
import com.github.mmrsic.idea.plugins.tibasic.ide.debug.initialDebugCharacterSetColors
import com.github.mmrsic.idea.plugins.tibasic.ide.debug.initialDebugScreenCharacterCodes
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
import java.awt.Rectangle
import java.awt.RenderingHints
import javax.swing.JComponent

internal data class TiBasicDebugScreenState(
    val screenBackground: TiColor,
    val characterSetColors: Map<Int, TiBasicDebugCharacterSetColors>,
    val characterCodes: List<List<Int>>,
    val characterPatterns: Map<Int, String> = emptyMap(),
) {
    companion object {
        fun initial(): TiBasicDebugScreenState =
            TiBasicDebugScreenState(
                screenBackground = INITIAL_SCREEN_BACKGROUND,
                characterSetColors = initialDebugCharacterSetColors(),
                characterCodes = initialDebugScreenCharacterCodes(),
            )
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
        paintDebugPreviewBackdrop(g, width, height)
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
        return scaledDebugPreviewBounds(
            availableWidth = width,
            availableHeight = height,
            previewWidth = DEBUG_SCREEN_WIDTH,
            previewHeight = DEBUG_SCREEN_HEIGHT,
            keepAspectRatio = keepAspectRatio,
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
        val characterColors = colorsForCode(code, state)
        val cellBackground = characterColors.bg.takeUnless { color -> color == TiColor.Transparent } ?: state.screenBackground
        val characterForeground = characterColors.fg.takeUnless { color -> color == TiColor.Transparent } ?: state.screenBackground
        graphics.color = JBColor(cellBackground.rgbValue, cellBackground.rgbValue)
        graphics.fillRect(x, y, DEBUG_SCREEN_CELL_SIZE, DEBUG_SCREEN_CELL_SIZE)
        tiBasicCharacterPattern(code, state.characterPatterns)?.let { pattern ->
            paintTiBasicCharPattern(
                g = graphics,
                x = x,
                y = y,
                pixelSize = 1,
                hexPattern = pattern,
                fg = characterForeground,
                bg = cellBackground,
            )
            return
        }
        val displayText = debugScreenDisplayText(code) ?: return
        graphics.color = JBColor(characterForeground.rgbValue, characterForeground.rgbValue)
        val metrics = graphics.getFontMetrics(graphics.font)
        val textX = x + ((DEBUG_SCREEN_CELL_SIZE - metrics.stringWidth(displayText)) / 2)
        val textY = y + ((DEBUG_SCREEN_CELL_SIZE - metrics.height) / 2) + metrics.ascent - DEBUG_SCREEN_TEXT_BASELINE_ADJUSTMENT
        graphics.drawString(displayText, textX, textY)
    }
}

private fun colorsForCode(code: Int, state: TiBasicDebugScreenState): TiBasicDebugCharacterSetColors =
    state.characterSetColors.entries
        .firstOrNull { (set, _) -> code in (callColorCharacterSetRange(set) ?: IntRange.EMPTY) }
        ?.value
        ?: DEFAULT_CHARACTER_SET_COLORS

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
internal const val DEBUG_SCREEN_WIDTH = TI_BASIC_SCREEN_COLUMNS * DEBUG_SCREEN_CELL_SIZE + 2 * DEBUG_SCREEN_SIDE_MARGIN
internal const val DEBUG_SCREEN_HEIGHT = TI_BASIC_SCREEN_ROWS * DEBUG_SCREEN_CELL_SIZE
private const val DEBUG_SCREEN_FONT_SIZE = 8
private const val DEBUG_SCREEN_HEX_WIDTH = 2
private const val DEBUG_SCREEN_TEXT_BASELINE_ADJUSTMENT = 1
private val DEFAULT_CHARACTER_SET_COLORS = TiBasicDebugCharacterSetColors(
    fg = TiColor.Black,
    bg = TiColor.Transparent,
)
