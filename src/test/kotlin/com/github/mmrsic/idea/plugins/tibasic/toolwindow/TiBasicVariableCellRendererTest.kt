package com.github.mmrsic.idea.plugins.tibasic.toolwindow

import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.JTextArea

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
                TiBasicVariableOccurrence(100, 0, AccessType.WRITE, null, emptyList()),
                TiBasicVariableOccurrence(200, 0, AccessType.WRITE, null, emptyList()),
                TiBasicVariableOccurrence(300, 0, AccessType.WRITE, null, emptyList()),
            ),
            isSelected = false,
            hasFocus = false,
            row = 0,
            column = 0,
        )
        renderer.panel.setSize(30, Int.MAX_VALUE)
        renderer.panel.doLayout()

        val lineLabels = renderer.panel.components.filterIsInstance<JLabel>().filter { it.text != "," }
        assertTrue(lineLabels.any { it.y > 0 })
    }

    fun `test line number renderer keeps labels left aligned in wide column`() {
        val renderer = TiBasicVariableLineNumberRenderer()
        val table = JTable(1, 1)
        table.columnModel.getColumn(0).width = 120

        renderer.getTableCellRendererComponent(
            table = table,
            value = listOf(
                TiBasicVariableOccurrence(100, 0, AccessType.WRITE, null, emptyList()),
                TiBasicVariableOccurrence(200, 0, AccessType.WRITE, null, emptyList()),
            ),
            isSelected = false,
            hasFocus = false,
            row = 0,
            column = 0,
        )
        renderer.panel.setSize(120, Int.MAX_VALUE)
        renderer.panel.doLayout()

        val firstLineLabel = renderer.panel.components
            .filterIsInstance<JLabel>()
            .first { it.text == "100" }
        assertTrue(firstLineLabel.x <= 4)
    }

    fun `test dimensions renderer shows clickable line prefix and DIM text`() {
        val renderer = TiBasicVariableDimensionsRenderer()
        val table = JTable(1, 1)
        table.columnModel.getColumn(0).width = 180

        renderer.getTableCellRendererComponent(
            table = table,
            value = TiBasicVariableDimensionsDisplay(
                occurrences = listOf(TiBasicVariableOccurrence(200, 0, AccessType.NONE, null, emptyList())),
                text = "DIM A(1-10,1-10,1-10)",
            ),
            isSelected = false,
            hasFocus = false,
            row = 0,
            column = 0,
        )
        renderer.panel.setSize(180, Int.MAX_VALUE)
        renderer.panel.doLayout()

        val labels = renderer.panel.components.filterIsInstance<JLabel>()
        val textArea = renderer.panel.components.filterIsInstance<JTextArea>().single()
        assertTrue(labels.any { it.text == "200" })
        assertEquals("DIM A(1-10,1-10,1-10)", textArea.text)
    }
}
