package com.github.mmrsic.idea.plugins.tibasic.toolwindow

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.Component
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.JTextArea
import javax.swing.table.TableCellRenderer

abstract class TiBasicLineNumberRenderer<T : Any> : TableCellRenderer {

    val panel: JPanel = JPanel(GridBagLayout()).also { it.isOpaque = true }

    protected abstract fun occurrences(value: Any?): List<T>

    protected abstract fun lineNumberOf(occurrence: T): Int

    protected open fun trailingText(value: Any?): String? = null

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

        val lineNumbers = occurrences(value).map(::lineNumberOf).distinct().sorted()
        val columnWidth = table.columnModel.getColumn(column).width
        val metrics = table.getFontMetrics(table.font)
        val maxContentWidth = (columnWidth - CELL_PADDING.left - CELL_PADDING.right).coerceAtLeast(metrics.charWidth('0'))
        val nextGridXByRow = mutableMapOf(0 to 0)
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
                    nextGridXByRow.putIfAbsent(gridY, 0)
                }
                panel.add(
                    JLabel(separator).also {
                        it.foreground = table.foreground
                    },
                    constraints(gridX, gridY),
                )
                gridX += 1
                nextGridXByRow[gridY] = gridX
                currentLineWidth += if (currentLineWidth == 0) separatorWidth else scaledSeparatorGap() + separatorWidth
            }
            val labelText = lineNum.toString()
            val labelWidth = metrics.stringWidth(labelText)
            if (currentLineWidth > 0 && currentLineWidth + scaledSeparatorGap() + labelWidth > maxContentWidth) {
                gridY += 1
                gridX = 0
                currentLineWidth = 0
                nextGridXByRow.putIfAbsent(gridY, 0)
            }
            panel.add(
                JLabel(labelText).also {
                    it.foreground = JBColor.BLUE
                    it.toolTipText = labelText
                },
                constraints(gridX, gridY),
            )
            gridX += 1
            nextGridXByRow[gridY] = gridX
            currentLineWidth += if (currentLineWidth == 0) labelWidth else scaledSeparatorGap() + labelWidth
        }

        trailingText(value)?.takeIf(String::isNotBlank)?.let { text ->
            panel.add(
                JTextArea(text).also {
                    it.isOpaque = false
                    it.isEditable = false
                    it.isFocusable = false
                    it.lineWrap = true
                    it.wrapStyleWord = true
                    it.font = table.font
                    it.foreground = table.foreground
                    it.background = panel.background
                    it.border = null
                },
                trailingTextConstraints(gridX, gridY),
            )
        } ?: run {
            nextGridXByRow.forEach { (rowIndex, nextGridX) ->
                panel.add(
                    JPanel().also { it.isOpaque = false },
                    trailingSpacerConstraints(nextGridX, rowIndex),
                )
            }
        }

        return panel
    }
}

class TiBasicVariableLineNumberRenderer : TiBasicLineNumberRenderer<TiBasicVariableOccurrence>() {

    override fun occurrences(value: Any?): List<TiBasicVariableOccurrence> =
        (value as? List<*>)?.filterIsInstance<TiBasicVariableOccurrence>().orEmpty()

    override fun lineNumberOf(occurrence: TiBasicVariableOccurrence): Int = occurrence.lineNumber
}

class TiBasicVariableDimensionsRenderer : TiBasicLineNumberRenderer<TiBasicVariableOccurrence>() {

    override fun occurrences(value: Any?): List<TiBasicVariableOccurrence> =
        (value as? TiBasicVariableDimensionsDisplay)?.occurrences.orEmpty()

    override fun lineNumberOf(occurrence: TiBasicVariableOccurrence): Int = occurrence.lineNumber

    override fun trailingText(value: Any?): String? = (value as? TiBasicVariableDimensionsDisplay)?.text
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

private fun trailingSpacerConstraints(
    gridX: Int,
    gridY: Int,
): GridBagConstraints =
    GridBagConstraints().also {
        it.gridx = gridX
        it.gridy = gridY
        it.weightx = 1.0
        it.fill = GridBagConstraints.HORIZONTAL
    }

private fun trailingTextConstraints(
    gridX: Int,
    gridY: Int,
): GridBagConstraints =
    GridBagConstraints().also {
        it.gridx = gridX
        it.gridy = gridY
        it.weightx = 1.0
        it.fill = GridBagConstraints.HORIZONTAL
        it.anchor = GridBagConstraints.WEST
        it.insets = CELL_PADDING
    }

private val CELL_PADDING = JBUI.insets(2)
private fun scaledSeparatorGap(): Int = JBUI.scale(4)
