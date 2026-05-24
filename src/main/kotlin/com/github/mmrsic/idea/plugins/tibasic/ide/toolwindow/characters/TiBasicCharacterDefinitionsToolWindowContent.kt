package com.github.mmrsic.idea.plugins.tibasic.ide.toolwindow.characters

import com.github.mmrsic.idea.plugins.tibasic.ide.toolwindow.TiBasicFileToolWindowContent
import com.github.mmrsic.idea.plugins.tibasic.ide.toolwindow.variables.TiBasicLineNumberRenderer
import com.github.mmrsic.idea.plugins.tibasic.language.analysis.calls.collectCallCharDefinitions
import com.github.mmrsic.idea.plugins.tibasic.language.analysis.calls.collectCallColorAssignments
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.TiBasicFile
import com.intellij.openapi.project.Project
import com.intellij.ui.table.JBTable
import javax.swing.RowSorter
import javax.swing.SortOrder
import javax.swing.table.TableRowSorter

class TiBasicCharacterDefinitionsToolWindowContent(project: Project) : TiBasicFileToolWindowContent(project) {

    private val tableModel = TiBasicCharacterDefinitionsTableModel()
    private val iconRenderer = TiBasicCharacterDefinitionIconRenderer()
    private val lineNumberRenderer = TiBasicCharacterDefinitionLineNumberRenderer()
    private val table = JBTable(tableModel)

    init {
        setupTable()
        initializeToolWindow(table)
    }

    override fun refreshForFile(file: TiBasicFile?) {
        val entries = computeReadAction {
            if (file != null) {
                buildCharacterDefinitionEntries(
                    definitions = collectCallCharDefinitions(file),
                    colorAssignments = collectCallColorAssignments(file),
                )
            } else {
                emptyList()
            }
        }
        tableModel.updateEntries(entries)
    }

    private fun setupTable() {
        table.setDefaultRenderer(TiBasicCharacterIcons::class.java, iconRenderer)
        table.setDefaultRenderer(List::class.java, lineNumberRenderer)
        val sorter = TableRowSorter(tableModel)
        table.rowSorter = sorter
        sorter.sortKeys = listOf(
            RowSorter.SortKey(CHARACTER_CODE_COLUMN, SortOrder.ASCENDING),
            RowSorter.SortKey(CHARACTER_LINE_COLUMN, SortOrder.ASCENDING),
        )
        table.rowHeight = maxOf(table.rowHeight, 18)
        table.columnModel.getColumn(CHARACTER_CODE_COLUMN).preferredWidth = 60
        table.columnModel.getColumn(CHARACTER_ASCII_COLUMN).preferredWidth = 70
        table.columnModel.getColumn(CHARACTER_PATTERN_COLUMN).preferredWidth = 190
        table.columnModel.getColumn(CHARACTER_ICON_COLUMN).preferredWidth = 180
        table.columnModel.getColumn(CHARACTER_LINE_COLUMN).preferredWidth = 80
        table.addMouseListener(TiBasicCharacterDefinitionLineClickHandler())
    }

    private inner class TiBasicCharacterDefinitionLineClickHandler : java.awt.event.MouseAdapter() {
        override fun mouseClicked(event: java.awt.event.MouseEvent) {
            val row = table.rowAtPoint(event.point)
            val col = table.columnAtPoint(event.point)
            if (row < 0 || table.convertColumnIndexToModel(col) != CHARACTER_LINE_COLUMN) return

            val cellRect = table.getCellRect(row, col, false)
            val relativeX = event.x - cellRect.x
            val relativeY = event.y - cellRect.y
            val modelRow = table.convertRowIndexToModel(row)
            val occurrences = (tableModel.getValueAt(modelRow, CHARACTER_LINE_COLUMN) as? List<*>)
                ?.filterIsInstance<TiBasicCharacterDefinitionOccurrence>()
                ?: return

            table.prepareRenderer(lineNumberRenderer, row, col)
            lineNumberRenderer.panel.setSize(cellRect.width, cellRect.height)
            lineNumberRenderer.panel.doLayout()

            val clickedLabel = lineNumberRenderer.panel.components
                .firstOrNull { component -> component is javax.swing.JLabel && component.bounds.contains(relativeX, relativeY) }
                as? javax.swing.JLabel
                ?: return
            val lineNumber = clickedLabel.text.toIntOrNull() ?: return
            val occurrence = occurrences.firstOrNull { it.lineNumber == lineNumber } ?: return
            navigateToOffset(occurrence.offset)
        }
    }
}

private class TiBasicCharacterDefinitionLineNumberRenderer :
    TiBasicLineNumberRenderer<TiBasicCharacterDefinitionOccurrence>() {

    override fun occurrences(value: Any?): List<TiBasicCharacterDefinitionOccurrence> =
        (value as? List<*>)?.filterIsInstance<TiBasicCharacterDefinitionOccurrence>().orEmpty()

    override fun lineNumberOf(occurrence: TiBasicCharacterDefinitionOccurrence): Int = occurrence.lineNumber
}
