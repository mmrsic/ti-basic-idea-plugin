package com.github.mmrsic.idea.plugins.tibasic.action.preview

import com.github.mmrsic.idea.plugins.tibasic.editor.TI_BASIC_CHAR_PATTERN_GRID_SIZE
import com.github.mmrsic.idea.plugins.tibasic.editor.paintTiBasicCharPattern
import com.github.mmrsic.idea.plugins.tibasic.editor.paintTiColorRect
import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JComponent
import javax.swing.JPanel

private const val SCREEN_CELL_PIXEL_SIZE = 16
private const val CHAR_PATTERN_PIXEL_SIZE = SCREEN_CELL_PIXEL_SIZE / TI_BASIC_CHAR_PATTERN_GRID_SIZE

internal class TiBasicScreenPreviewDialog(
    project: Project,
    private val preview: TiBasicScreenPreview,
) : DialogWrapper(project) {

    init {
        title = TiBasicBundle.message(TiBasicScreenPreviewActionMetadata.dialogTitleKey)
        isResizable = true
        init()
    }

    override fun createCenterPanel(): JComponent =
        JPanel(BorderLayout()).apply {
            if (preview.isPartial) {
                add(
                    JBLabel(
                        buildString {
                            append("<html><b>")
                            append(TiBasicBundle.message(TiBasicScreenPreviewActionMetadata.partialSummaryKey))
                            append("</b><br/>")
                            append(preview.warnings.joinToString("<br/>"))
                            append("</html>")
                        },
                    ),
                    BorderLayout.NORTH,
                )
            }
            add(TiBasicScreenPreviewComponent(preview), BorderLayout.CENTER)
        }
}

private class TiBasicScreenPreviewComponent(
    private val preview: TiBasicScreenPreview,
) : JPanel() {

    init {
        preferredSize = Dimension(
            SCREEN_CELL_PIXEL_SIZE * preview.cells.first().size,
            SCREEN_CELL_PIXEL_SIZE * preview.cells.size,
        )
        font = Font(Font.MONOSPACED, Font.BOLD, 12)
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val graphics = g as Graphics2D
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        preview.cells.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { columnIndex, cell ->
                val x = columnIndex * SCREEN_CELL_PIXEL_SIZE
                val y = rowIndex * SCREEN_CELL_PIXEL_SIZE
                paintTiColorRect(graphics, x, y, SCREEN_CELL_PIXEL_SIZE, SCREEN_CELL_PIXEL_SIZE, cell.bg)
                if (cell.pattern != null) {
                    paintTiBasicCharPattern(
                        g = graphics,
                        x = x,
                        y = y,
                        pixelSize = CHAR_PATTERN_PIXEL_SIZE,
                        hexPattern = cell.pattern,
                        fg = cell.fg,
                        bg = cell.bg,
                    )
                } else {
                    paintAsciiFallback(graphics, x, y, cell)
                }
                graphics.color = JBColor.DARK_GRAY
                graphics.drawRect(x, y, SCREEN_CELL_PIXEL_SIZE, SCREEN_CELL_PIXEL_SIZE)
            }
        }
    }

    private fun paintAsciiFallback(
        graphics: Graphics2D,
        x: Int,
        y: Int,
        cell: TiBasicScreenPreviewCell,
    ) {
        val displayText = cell.displayText ?: return
        graphics.color = JBColor(cell.fg.rgbValue, cell.fg.rgbValue)
        val metrics = graphics.getFontMetrics(font)
        val textX = x + ((SCREEN_CELL_PIXEL_SIZE - metrics.stringWidth(displayText)) / 2)
        val textY = y + ((SCREEN_CELL_PIXEL_SIZE - metrics.height) / 2) + metrics.ascent
        graphics.drawString(displayText, textX, textY)
    }
}
