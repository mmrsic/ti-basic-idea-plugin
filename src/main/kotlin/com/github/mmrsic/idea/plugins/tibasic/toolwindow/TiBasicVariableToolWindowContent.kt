package com.github.mmrsic.idea.plugins.tibasic.toolwindow

import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicBundle
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.intellij.codeInsight.highlighting.HighlightManager
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.ui.table.JBTable
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.RowSorter
import javax.swing.SortOrder
import javax.swing.table.TableCellRenderer
import javax.swing.event.TableColumnModelEvent
import javax.swing.event.TableColumnModelListener
import javax.swing.table.TableRowSorter

class TiBasicVariableToolWindowContent(project: Project) : TiBasicFileToolWindowContent(project) {

    private val settings = TiBasicVariableToolWindowSettings.getInstance()
    private val tableModel = TiBasicVariableTableModel(showArrayElementConstants = settings.showArrayElementConstants)
    private val dimensionsRenderer = TiBasicVariableDimensionsRenderer()
    private val lineNumberRenderer = TiBasicVariableLineNumberRenderer()
    private val wrappedTextRenderer = TiBasicWrappedTextCellRenderer()
    private val table = JBTable(tableModel)
    private val constantsToggle = JCheckBox(TiBasicBundle.message("tool.window.variables.show.constants"))
    private val activeHighlighters = ArrayList<RangeHighlighter>()
    private val sorter = TableRowSorter(tableModel)
    private val lineNumberComparator = compareBy<Any?> {
        (it as? List<*>)
            ?.filterIsInstance<TiBasicVariableOccurrence>()
            ?.minOfOrNull { occ -> occ.lineNumber }
            ?: Int.MAX_VALUE
    }
    private val defaultSort = listOf(VariableTableSort(NAME_COLUMN, SortOrder.ASCENDING))

    init {
        constantsToggle.isSelected = settings.showArrayElementConstants
        constantsToggle.addActionListener {
            settings.showArrayElementConstants = constantsToggle.isSelected
            tableModel.setShowArrayElementConstants(constantsToggle.isSelected)
            refreshForFile(currentFile)
        }
        setupTable()
        initializeToolWindow(table)
    }

    override fun toolbarComponents(): List<JComponent> = listOf(constantsToggle)

    private fun setupTable() {
        table.setDefaultRenderer(TiBasicVariableDimensionsDisplay::class.java, dimensionsRenderer)
        table.setDefaultRenderer(List::class.java, lineNumberRenderer)
        table.setDefaultRenderer(String::class.java, wrappedTextRenderer)
        table.tableHeader.defaultRenderer = leftAlignedHeaderRenderer(table.tableHeader.defaultRenderer)
        table.rowSorter = sorter
        configureVisibleColumns(sortState = defaultSort)
        table.addMouseListener(LineNumberClickHandler())
        table.selectionModel.addListSelectionListener { event ->
            if (!event.valueIsAdjusting) updateHighlightsForSelection()
        }
        table.columnModel.addColumnModelListener(RowHeightUpdateListener())
        table.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(event: ComponentEvent) = updateRowHeights()
        })
    }

    override fun refreshForFile(file: TiBasicFile?) {
        clearHighlights()
        val sortState = currentSortState()
        val entries = if (file != null) TiBasicVariableCollector.collect(file) else emptyList()
        tableModel.updateEntries(entries)
        configureVisibleColumns(sortState = sortState)
        updateRowHeights()
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
            if (row < 0) return
            val modelColumn = table.convertColumnIndexToModel(col)
            if (modelColumn == DIMENSIONS_COLUMN) {
                navigateFromDimensionsCell(row, col, e)
                return
            }
            if (!tableModel.isLineNumberColumn(modelColumn)) return

            val cellRect = table.getCellRect(row, col, false)
            val relX = e.x - cellRect.x

            val modelRow = table.convertRowIndexToModel(row)
            val occurrences = (tableModel.getValueAt(modelRow, modelColumn) as? List<*>)
                ?.filterIsInstance<TiBasicVariableOccurrence>() ?: return

            table.prepareRenderer(lineNumberRenderer, row, col)
            lineNumberRenderer.panel.setSize(cellRect.width, cellRect.height)
            lineNumberRenderer.panel.doLayout()

            val clickedLabel = lineNumberRenderer.panel.components
                .firstOrNull { c -> c is JLabel && c.bounds.contains(relX, e.y - cellRect.y) } as? JLabel
                ?: return
            val lineNum = clickedLabel.text.toIntOrNull() ?: return

            val occurrence = occurrences.firstOrNull { it.lineNumber == lineNum } ?: return
            navigateToOffset(occurrence.offset)
        }

        private fun navigateFromDimensionsCell(
            row: Int,
            col: Int,
            event: MouseEvent,
        ) {
            val cellRect = table.getCellRect(row, col, false)
            val relX = event.x - cellRect.x
            val relY = event.y - cellRect.y
            val modelRow = table.convertRowIndexToModel(row)
            val dimensionsDisplay = tableModel.getValueAt(modelRow, DIMENSIONS_COLUMN) as? TiBasicVariableDimensionsDisplay ?: return

            table.prepareRenderer(dimensionsRenderer, row, col)
            dimensionsRenderer.panel.setSize(cellRect.width, cellRect.height)
            dimensionsRenderer.panel.doLayout()

            val clickedLabel = dimensionsRenderer.panel.components
                .firstOrNull { component -> component is JLabel && component.bounds.contains(relX, relY) } as? JLabel
                ?: return
            val lineNum = clickedLabel.text.toIntOrNull() ?: return
            val occurrence = dimensionsDisplay.occurrences.firstOrNull { it.lineNumber == lineNum } ?: return
            navigateToOffset(occurrence.offset)
        }
    }

    private fun updateRowHeights() {
        for (row in 0 until table.rowCount) {
            val preferredHeight = (0 until table.columnCount)
                .maxOfOrNull { column -> preferredCellHeight(row, column) }
                ?: table.rowHeight
            if (table.getRowHeight(row) != preferredHeight) {
                table.setRowHeight(row, preferredHeight)
            }
        }
    }

    private fun preferredCellHeight(
        row: Int,
        column: Int,
    ): Int {
        val renderer = table.getCellRenderer(row, column)
        val component = renderer.getTableCellRendererComponent(
            table,
            table.getValueAt(row, column),
            false,
            false,
            row,
            column,
        )
        component.setSize(table.columnModel.getColumn(column).width, Int.MAX_VALUE)
        return component.preferredSize.height
    }

    private inner class RowHeightUpdateListener : TableColumnModelListener {
        override fun columnAdded(event: TableColumnModelEvent) = Unit
        override fun columnRemoved(event: TableColumnModelEvent) = Unit
        override fun columnMoved(event: TableColumnModelEvent) = Unit
        override fun columnSelectionChanged(event: javax.swing.event.ListSelectionEvent) = Unit
        override fun columnMarginChanged(event: javax.swing.event.ChangeEvent) = updateRowHeights()
    }

    private fun configureVisibleColumns(sortState: List<VariableTableSort>) {
        tableModel.modelColumnIndex(DIMENSIONS_COLUMN)?.let {
            sorter.setComparator(
                it,
                compareBy<Any?>(
                    { (it as? TiBasicVariableDimensionsDisplay)?.occurrences?.minOfOrNull(TiBasicVariableOccurrence::lineNumber) ?: Int.MAX_VALUE },
                    { (it as? TiBasicVariableDimensionsDisplay)?.text ?: "" },
                ),
            )
        }
        tableModel.modelColumnIndex(WRITES_COLUMN)?.let { sorter.setComparator(it, lineNumberComparator) }
        tableModel.modelColumnIndex(READS_COLUMN)?.let { sorter.setComparator(it, lineNumberComparator) }
        restoreSortState(sortState)
        tableModel.modelColumnIndex(DIMENSIONS_COLUMN)?.let { table.columnModel.getColumn(it).preferredWidth = 180 }
        tableModel.modelColumnIndex(RANGE_COLUMN)?.let { table.columnModel.getColumn(it).preferredWidth = 140 }
    }

    private fun currentSortState(): List<VariableTableSort> =
        sorter.sortKeys
            .mapNotNull { sortKey ->
                tableModel.columnIdAt(sortKey.column)?.let { columnId ->
                    VariableTableSort(columnId, sortKey.sortOrder)
                }
            }
            .ifEmpty { defaultSort }

    private fun restoreSortState(sortState: List<VariableTableSort>) {
        val restoredSortKeys = sortState
            .mapNotNull { sort ->
                tableModel.modelColumnIndex(sort.columnId)?.let { modelColumn ->
                    RowSorter.SortKey(modelColumn, sort.order)
                }
            }
            .ifEmpty {
                defaultSort.mapNotNull { sort ->
                    tableModel.modelColumnIndex(sort.columnId)?.let { modelColumn ->
                        RowSorter.SortKey(modelColumn, sort.order)
                    }
                }
            }
        sorter.sortKeys = restoredSortKeys
    }

    private fun leftAlignedHeaderRenderer(delegate: TableCellRenderer): TableCellRenderer =
        TableCellRenderer { table, value, isSelected, hasFocus, row, column ->
            delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                .also { component ->
                    (component as? JLabel)?.horizontalAlignment = JLabel.LEFT
                }
        }
}

private data class VariableTableSort(
    val columnId: Int,
    val order: SortOrder,
)
