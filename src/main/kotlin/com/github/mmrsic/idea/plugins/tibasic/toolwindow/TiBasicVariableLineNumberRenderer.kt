package com.github.mmrsic.idea.plugins.tibasic.toolwindow

import com.intellij.ui.JBColor
import java.awt.Component
import java.awt.FlowLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.TableCellRenderer

class TiBasicVariableLineNumberRenderer : TableCellRenderer {

    val panel: JPanel = JPanel(FlowLayout(FlowLayout.LEFT, 2, 0)).also { it.isOpaque = true }

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

        lineNumbers.forEachIndexed { index, lineNum ->
            if (index > 0) {
                panel.add(JLabel(",").also { it.foreground = table.foreground })
            }
            panel.add(
                JLabel(lineNum.toString()).also {
                    it.foreground = JBColor.BLUE
                    it.toolTipText = lineNum.toString()
                },
            )
        }

        return panel
    }
}
