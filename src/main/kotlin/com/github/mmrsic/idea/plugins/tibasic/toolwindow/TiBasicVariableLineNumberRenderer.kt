package com.github.mmrsic.idea.plugins.tibasic.toolwindow

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.Component
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.TableCellRenderer

class TiBasicVariableLineNumberRenderer : TableCellRenderer {

    val panel: JPanel = JPanel(GridBagLayout()).also { it.isOpaque = true }

    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int,
    ): Component {
        panel.removeAll()
        panel.background = if (isSelected) table.selectionBackground else table.background

        val occurrences = (value as? List<*>)?.filterIsInstance<TiBasicVariableOccurrence>() ?: return panel
        val lineNumbers = occurrences.map { it.lineNumber }.distinct().sorted()
        val columnWidth = table.columnModel.getColumn(column).width
        val metrics = table.getFontMetrics(table.font)
        val maxContentWidth = (columnWidth - CELL_PADDING.left - CELL_PADDING.right).coerceAtLeast(metrics.charWidth('0'))
        var currentLineWidth = 0
        var gridX = 0
        var gridY = 0

        lineNumbers.forEachIndexed { index, lineNum ->
            if (index > 0) {
                val separator = ","
                val separatorWidth = metrics.stringWidth(separator)
                if (currentLineWidth > 0 && currentLineWidth + scaledSeparatorGap() + separatorWidth > maxContentWidth) {
                    gridY += 1
                    gridX = 0
                    currentLineWidth = 0
                }
                panel.add(
                    JLabel(separator).also {
                        it.foreground = table.foreground
                    },
                    constraints(gridX, gridY),
                )
                gridX += 1
                currentLineWidth += if (currentLineWidth == 0) separatorWidth else scaledSeparatorGap() + separatorWidth
            }
            val labelText = lineNum.toString()
            val labelWidth = metrics.stringWidth(labelText)
            if (currentLineWidth > 0 && currentLineWidth + scaledSeparatorGap() + labelWidth > maxContentWidth) {
                gridY += 1
                gridX = 0
                currentLineWidth = 0
            }
            panel.add(
                JLabel(labelText).also {
                    it.foreground = JBColor.BLUE
                    it.toolTipText = labelText
                },
                constraints(gridX, gridY),
            )
            gridX += 1
            currentLineWidth += if (currentLineWidth == 0) labelWidth else scaledSeparatorGap() + labelWidth
        }

        return panel
    }

    private fun constraints(
        gridX: Int,
        gridY: Int,
    ): GridBagConstraints =
        GridBagConstraints().also {
            it.gridx = gridX
            it.gridy = gridY
            it.anchor = GridBagConstraints.WEST
            it.insets = CELL_PADDING
        }
}

private val CELL_PADDING = JBUI.insets(2)
private fun scaledSeparatorGap(): Int = JBUI.scale(4)
