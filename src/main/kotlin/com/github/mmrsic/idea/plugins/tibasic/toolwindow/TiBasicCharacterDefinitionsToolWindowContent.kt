package com.github.mmrsic.idea.plugins.tibasic.toolwindow

import com.github.mmrsic.idea.plugins.tibasic.editor.collectCallCharDefinitions
import com.github.mmrsic.idea.plugins.tibasic.editor.collectCallColorAssignments
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.intellij.openapi.project.Project
import com.intellij.ui.table.JBTable
import javax.swing.RowSorter
import javax.swing.SortOrder
import javax.swing.table.TableRowSorter

class TiBasicCharacterDefinitionsToolWindowContent(project: Project) : TiBasicFileToolWindowContent(project) {

    private val tableModel = TiBasicCharacterDefinitionsTableModel()
    private val iconRenderer = TiBasicCharacterDefinitionIconRenderer()
    private val table = JBTable(tableModel)

    init {
        setupTable()
        initializeToolWindow(table)
    }

    override fun refreshForFile(file: TiBasicFile?) {
        val entries = if (file != null) {
            buildCharacterDefinitionEntries(
                definitions = collectCallCharDefinitions(file),
                colorAssignments = collectCallColorAssignments(file),
            )
        } else {
            emptyList()
        }
        tableModel.updateEntries(entries)
    }

    private fun setupTable() {
        table.setDefaultRenderer(TiBasicCharacterIcons::class.java, iconRenderer)
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
        table.columnModel.getColumn(CHARACTER_LINE_COLUMN).preferredWidth = 60
        installLineNavigation(table, CHARACTER_LINE_COLUMN) { modelRow ->
            tableModel.entryAt(modelRow).offset
        }
    }
}
