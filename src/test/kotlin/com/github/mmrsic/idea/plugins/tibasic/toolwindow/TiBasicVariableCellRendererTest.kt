package com.github.mmrsic.idea.plugins.tibasic.toolwindow

import javax.swing.JTable

class TiBasicVariableCellRendererTest : junit.framework.TestCase() {

    fun `test wrapped text renderer grows preferred height for narrow column`() {
        val renderer = TiBasicWrappedTextCellRenderer()
        val table = JTable(1, 1)
        table.columnModel.getColumn(0).width = 40

        renderer.getTableCellRendererComponent(
            table = table,
            value = "\"307C6EF8FE7C7C30\", \"0C3E761F7F3E3E0C\"",
            isSelected = false,
            hasFocus = false,
            row = 0,
            column = 0,
        )

        assertTrue(renderer.preferredSize.height > table.getFontMetrics(table.font).height)
    }

    fun `test line number renderer wraps labels for narrow column`() {
        val renderer = TiBasicVariableLineNumberRenderer()
        val table = JTable(1, 1)
        table.columnModel.getColumn(0).width = 30

        renderer.getTableCellRendererComponent(
            table = table,
            value = listOf(
                TiBasicVariableOccurrence(100, 0, AccessType.WRITE),
                TiBasicVariableOccurrence(200, 0, AccessType.WRITE),
                TiBasicVariableOccurrence(300, 0, AccessType.WRITE),
            ),
            isSelected = false,
            hasFocus = false,
            row = 0,
            column = 0,
        )
        renderer.panel.setSize(30, Int.MAX_VALUE)
        renderer.panel.doLayout()

        val lineLabels = renderer.panel.components.filterIsInstance<javax.swing.JLabel>().filter { it.text != "," }
        assertTrue(lineLabels.any { it.y > 0 })
    }
}
