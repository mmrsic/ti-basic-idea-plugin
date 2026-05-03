package com.github.mmrsic.idea.plugins.tibasic.toolwindow

import java.awt.Component
import java.awt.FlowLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.TableCellRenderer

internal class TiBasicCharacterDefinitionIconRenderer : TableCellRenderer {

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

        val icons = (value as? TiBasicCharacterIcons)?.icons.orEmpty()
        icons.forEach { icon ->
            panel.add(JLabel(icon))
        }
        return panel
    }
}
