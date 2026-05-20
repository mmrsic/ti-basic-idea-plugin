package com.github.mmrsic.idea.plugins.tibasic.toolwindow

import com.intellij.util.ui.JBUI
import java.awt.Component
import javax.swing.JTable
import javax.swing.JTextArea
import javax.swing.table.TableCellRenderer

internal class TiBasicWrappedTextCellRenderer : JTextArea(), TableCellRenderer {

    init {
        lineWrap = true
        wrapStyleWord = true
        isOpaque = true
        border = JBUI.Borders.empty(2, 4)
    }

    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int,
    ): Component {
        text = value?.toString().orEmpty()
        font = table.font
        foreground = if (isSelected) table.selectionForeground else table.foreground
        background = if (isSelected) table.selectionBackground else table.background
        setSize(table.columnModel.getColumn(column).width, Int.MAX_VALUE)
        return this
    }
}
