package com.github.mmrsic.idea.plugins.tibasic.ide.toolwindow.debug

import com.github.mmrsic.idea.plugins.tibasic.editor.callColorCharacterSetRange
import com.github.mmrsic.idea.plugins.tibasic.editor.paintTiBasicCharPattern
import com.github.mmrsic.idea.plugins.tibasic.ide.debug.TiBasicDebugCharacterSetColors
import com.github.mmrsic.idea.plugins.tibasic.ide.debug.TiBasicDebugScreenContents
import com.github.mmrsic.idea.plugins.tibasic.language.model.TiColor
import com.github.mmrsic.idea.plugins.tibasic.language.runtime.screen.tiBasicCharacterPattern
import com.intellij.ui.JBColor
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Rectangle
import java.awt.RenderingHints
import javax.swing.JComponent

internal data class TiBasicDebugCharacterSetPreviewState(
    val screenBackground: TiColor,
    val characterSetColors: Map<Int, TiBasicDebugCharacterSetColors>,
    val characterPatterns: Map<Int, String> = emptyMap(),
) {
    companion object {
        fun fromScreenContents(screenContents: TiBasicDebugScreenContents): TiBasicDebugCharacterSetPreviewState =
            TiBasicDebugCharacterSetPreviewState(
                screenBackground = screenContents.screenBackground,
                characterSetColors = screenContents.characterSetColors,
                characterPatterns = screenContents.characterPatterns,
            )
    }
}

internal class TiBasicDebugCharacterSetPreviewComponent(
    internal var state: TiBasicDebugCharacterSetPreviewState,
) : JComponent() {
    internal var keepAspectRatio: Boolean = true
        set(value) {
            field = value
            repaint()
        }

    init {
        preferredSize = Dimension(DEBUG_SCREEN_WIDTH, DEBUG_SCREEN_HEIGHT)
    }

    override fun paintComponent(graphics: Graphics) {
        super.paintComponent(graphics)
        val g = graphics as Graphics2D
        paintDebugPreviewBackdrop(g, width, height)
        val previewBounds = scaledPreviewBounds()
        g.color = JBColor(state.screenBackground.rgbValue, state.screenBackground.rgbValue)
        g.fillRect(previewBounds.x, previewBounds.y, previewBounds.width, previewBounds.height)
        val previewGraphics = g.create(previewBounds.x, previewBounds.y, previewBounds.width, previewBounds.height) as Graphics2D
        try {
            previewGraphics.scale(
                previewBounds.width.toDouble() / DEBUG_SCREEN_WIDTH,
                previewBounds.height.toDouble() / DEBUG_SCREEN_HEIGHT,
            )
            previewGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
            previewGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
            val characterSetOrigin = characterSetOrigin()
            repeat(CHARACTER_SET_COUNT) { setOffset ->
                repeat(CHARACTERS_PER_SET) { characterOffset ->
                    paintCharacter(
                        previewGraphics,
                        code = FIRST_PREVIEW_CODE + setOffset * CHARACTERS_PER_SET + characterOffset,
                        rowIndex = setOffset,
                        columnIndex = characterOffset,
                        origin = characterSetOrigin,
                    )
                }
            }
        } finally {
            previewGraphics.dispose()
        }
    }

    internal fun scaledPreviewBounds(): Rectangle =
        scaledDebugPreviewBounds(
            availableWidth = width,
            availableHeight = height,
            previewWidth = DEBUG_SCREEN_WIDTH,
            previewHeight = DEBUG_SCREEN_HEIGHT,
            keepAspectRatio = keepAspectRatio,
        )

    internal fun characterSetOrigin(): Point =
        Point(
            (DEBUG_SCREEN_WIDTH - CHARACTER_SET_PREVIEW_WIDTH) / 2,
            (DEBUG_SCREEN_HEIGHT - CHARACTER_SET_PREVIEW_HEIGHT) / 2,
        )

    private fun paintCharacter(
        graphics: Graphics2D,
        code: Int,
        rowIndex: Int,
        columnIndex: Int,
        origin: Point,
    ) {
        val x = origin.x + CHARACTER_SET_PREVIEW_SIDE_MARGIN + columnIndex * CHARACTER_SET_CELL_SIZE
        val y = origin.y + CHARACTER_SET_PREVIEW_TOP_MARGIN + rowIndex * CHARACTER_SET_CELL_SIZE
        val colors = colorsForCode(code)
        val background = colors.bg.takeUnless { it == TiColor.Transparent } ?: state.screenBackground
        val foreground = colors.fg.takeUnless { it == TiColor.Transparent } ?: state.screenBackground
        graphics.color = JBColor(background.rgbValue, background.rgbValue)
        graphics.fillRect(x, y, CHARACTER_SET_CELL_SIZE, CHARACTER_SET_CELL_SIZE)
        tiBasicCharacterPattern(code, state.characterPatterns)?.let { pattern ->
            paintTiBasicCharPattern(
                g = graphics,
                x = x,
                y = y,
                pixelSize = 1,
                hexPattern = pattern,
                fg = foreground,
                bg = background,
            )
        }
    }

    private fun colorsForCode(code: Int): TiBasicDebugCharacterSetColors =
        state.characterSetColors.entries
            .firstOrNull { (set, _) -> code in (callColorCharacterSetRange(set) ?: IntRange.EMPTY) }
            ?.value
            ?: TiBasicDebugCharacterSetColors(
                fg = TiColor.Black,
                bg = TiColor.Transparent,
            )
}

private const val CHARACTER_SET_COUNT = 16
private const val CHARACTERS_PER_SET = 8
private const val CHARACTER_SET_CELL_SIZE = 8
private const val CHARACTER_SET_PREVIEW_SIDE_MARGIN = 4
private const val CHARACTER_SET_PREVIEW_TOP_MARGIN = 4
private const val FIRST_PREVIEW_CODE = 32
internal const val CHARACTER_SET_PREVIEW_WIDTH = CHARACTERS_PER_SET * CHARACTER_SET_CELL_SIZE + 2 * CHARACTER_SET_PREVIEW_SIDE_MARGIN
internal const val CHARACTER_SET_PREVIEW_HEIGHT = CHARACTER_SET_COUNT * CHARACTER_SET_CELL_SIZE + 2 * CHARACTER_SET_PREVIEW_TOP_MARGIN
