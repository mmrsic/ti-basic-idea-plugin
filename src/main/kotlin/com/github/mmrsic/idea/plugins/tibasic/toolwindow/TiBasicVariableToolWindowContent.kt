package com.github.mmrsic.idea.plugins.tibasic.toolwindow

import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.intellij.codeInsight.highlighting.HighlightManager
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.ui.table.JBTable
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JLabel
import javax.swing.RowSorter
import javax.swing.SortOrder
import javax.swing.table.TableRowSorter

class TiBasicVariableToolWindowContent(project: Project) : TiBasicFileToolWindowContent(project) {

    private val tableModel = TiBasicVariableTableModel()
    private val lineNumberRenderer = TiBasicVariableLineNumberRenderer()
    private val table = JBTable(tableModel)
    private val activeHighlighters = ArrayList<RangeHighlighter>()

    init {
        setupTable()
        initializeToolWindow(table)
    }

    private fun setupTable() {
        table.setDefaultRenderer(List::class.java, lineNumberRenderer)
        val sorter = TableRowSorter(tableModel)
        val lineNumberComparator = compareBy<Any?> {
            (it as? List<*>)
                ?.filterIsInstance<TiBasicVariableOccurrence>()
                ?.minOfOrNull { occ -> occ.lineNumber }
                ?: Int.MAX_VALUE
        }
        sorter.setComparator(DIM_LINE_COLUMN, lineNumberComparator)
        sorter.setComparator(WRITES_COLUMN, lineNumberComparator)
        sorter.setComparator(READS_COLUMN, lineNumberComparator)
        table.rowSorter = sorter
        sorter.sortKeys = listOf(RowSorter.SortKey(0, SortOrder.ASCENDING))
        table.columnModel.getColumn(DIMENSIONS_COLUMN).preferredWidth = 90
        table.columnModel.getColumn(BASE_COLUMN).preferredWidth = 45
        table.columnModel.getColumn(DIM_LINE_COLUMN).preferredWidth = 45
        table.columnModel.getColumn(CONST_COLUMN).preferredWidth = 70
        table.addMouseListener(LineNumberClickHandler())
        table.selectionModel.addListSelectionListener { event ->
            if (!event.valueIsAdjusting) updateHighlightsForSelection()
        }
    }

    override fun refreshForFile(file: TiBasicFile?) {
        clearHighlights()
        val entries = if (file != null) TiBasicVariableCollector.collect(file) else emptyList()
        tableModel.updateEntries(entries)
    }

    private fun updateHighlightsForSelection() {
        val selectedRow = table.selectedRow
        if (selectedRow < 0) {
            clearHighlights()
            return
        }
        val modelRow = table.convertRowIndexToModel(selectedRow)
        highlightVariable(tableModel.entryAt(modelRow))
    }

    private fun highlightVariable(entry: TiBasicVariableEntry) {
        clearHighlights()
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        val highlightManager = HighlightManager.getInstance(project)
        val nameLength = entry.name.length
        for (occ in entry.dimOccurrences + entry.occurrences) {
            val attributesKey = if (occ.accessType == AccessType.WRITE)
                EditorColors.WRITE_SEARCH_RESULT_ATTRIBUTES
            else
                EditorColors.SEARCH_RESULT_ATTRIBUTES
            highlightManager.addRangeHighlight(
                editor, occ.offset, occ.offset + nameLength,
                attributesKey, false, activeHighlighters,
            )
        }
    }

    private fun clearHighlights() {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        if (editor != null) {
            val highlightManager = HighlightManager.getInstance(project)
            activeHighlighters.forEach { highlightManager.removeSegmentHighlighter(editor, it) }
        }
        activeHighlighters.clear()
    }

    private inner class LineNumberClickHandler : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
            val row = table.rowAtPoint(e.point)
            val col = table.columnAtPoint(e.point)
            if (row < 0 || col !in DIM_LINE_COLUMN..READS_COLUMN) return

            val cellRect = table.getCellRect(row, col, false)
            val relX = e.x - cellRect.x

            val modelRow = table.convertRowIndexToModel(row)
            val occurrences = (tableModel.getValueAt(modelRow, col) as? List<*>)
                ?.filterIsInstance<TiBasicVariableOccurrence>() ?: return

            table.prepareRenderer(lineNumberRenderer, row, col)
            lineNumberRenderer.panel.setSize(cellRect.width, cellRect.height)
            lineNumberRenderer.panel.doLayout()

            val clickedLabel = lineNumberRenderer.panel.components
                .firstOrNull { c -> c is JLabel && c.x <= relX && relX < c.x + c.width } as? JLabel
                ?: return
            val lineNum = clickedLabel.text.toIntOrNull() ?: return

            val occurrence = occurrences.firstOrNull { it.lineNumber == lineNum } ?: return
            navigateToOffset(occurrence.offset)
        }
    }
}
