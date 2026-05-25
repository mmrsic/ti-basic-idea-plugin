package com.github.mmrsic.idea.plugins.tibasic.ide.toolwindow.debug

import com.github.mmrsic.idea.plugins.tibasic.editor.TiBasicCharPatternBitmap
import com.github.mmrsic.idea.plugins.tibasic.editor.TI_BASIC_CHAR_PATTERN_GRID_SIZE
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import javax.swing.JComponent

internal class TiBasicDebugArgumentPatternPreviewComponent : JComponent() {

    internal var hexPattern: String? = null
        set(value) {
            field = value
            isVisible = value != null
            repaint()
        }

    init {
        isOpaque = false
        isVisible = false
        preferredSize = Dimension(
            ARGUMENT_PATTERN_PREVIEW_PIXEL_SIZE * TI_BASIC_CHAR_PATTERN_GRID_SIZE,
            ARGUMENT_PATTERN_PREVIEW_PIXEL_SIZE * TI_BASIC_CHAR_PATTERN_GRID_SIZE,
        )
    }

    override fun paintComponent(graphics: Graphics) {
        super.paintComponent(graphics)
        val pattern = hexPattern ?: return
        val bitmap = TiBasicCharPatternBitmap.fromHexPattern(pattern)
        for (row in 0 until TI_BASIC_CHAR_PATTERN_GRID_SIZE) {
            for (column in 0 until TI_BASIC_CHAR_PATTERN_GRID_SIZE) {
                graphics.color = if (bitmap.bitAt(row, column)) {
                    ARGUMENT_PATTERN_FOREGROUND
                } else {
                    ARGUMENT_PATTERN_BACKGROUND
                }
                graphics.fillRect(
                    column * ARGUMENT_PATTERN_PREVIEW_PIXEL_SIZE,
                    row * ARGUMENT_PATTERN_PREVIEW_PIXEL_SIZE,
                    ARGUMENT_PATTERN_PREVIEW_PIXEL_SIZE,
                    ARGUMENT_PATTERN_PREVIEW_PIXEL_SIZE,
                )
            }
        }
    }
}

private const val ARGUMENT_PATTERN_PREVIEW_PIXEL_SIZE = 3
private val ARGUMENT_PATTERN_FOREGROUND = JBColor(Color(0x66, 0x66, 0x66), Color(0x66, 0x66, 0x66))
private val ARGUMENT_PATTERN_BACKGROUND = JBColor(Color(0xDD, 0xDD, 0xDD), Color(0xDD, 0xDD, 0xDD))
