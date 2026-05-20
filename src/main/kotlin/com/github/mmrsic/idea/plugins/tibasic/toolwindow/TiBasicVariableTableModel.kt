package com.github.mmrsic.idea.plugins.tibasic.toolwindow

import javax.swing.table.AbstractTableModel

private val COLUMN_NAMES = mapOf(
    NAME_COLUMN to "Name",
    TYPE_COLUMN to "Type",
    DIMENSIONS_COLUMN to "Dimensions",
    BASE_COLUMN to "Base",
    DIM_LINE_COLUMN to "DIM",
    WRITES_COLUMN to "Writes",
    READS_COLUMN to "Reads",
    RANGE_COLUMN to "Range",
)
private val ARRAY_COLUMNS = listOf(DIMENSIONS_COLUMN, BASE_COLUMN, DIM_LINE_COLUMN)
private val ALWAYS_VISIBLE_COLUMNS = listOf(NAME_COLUMN, TYPE_COLUMN, WRITES_COLUMN, READS_COLUMN, RANGE_COLUMN)
const val NAME_COLUMN = 0
const val TYPE_COLUMN = 1
const val DIMENSIONS_COLUMN = 2
const val BASE_COLUMN = 3
const val DIM_LINE_COLUMN = 4
const val WRITES_COLUMN = 5
const val READS_COLUMN = 6
const val RANGE_COLUMN = 7

class TiBasicVariableTableModel(private var entries: List<TiBasicVariableEntry> = emptyList()) : AbstractTableModel() {

    private var visibleColumns = ALWAYS_VISIBLE_COLUMNS

    fun updateEntries(newEntries: List<TiBasicVariableEntry>) {
        val newVisibleColumns = visibleColumns(newEntries)
        val structureChanged = visibleColumns != newVisibleColumns
        entries = newEntries
        visibleColumns = newVisibleColumns
        if (structureChanged) {
            fireTableStructureChanged()
        } else {
            fireTableDataChanged()
        }
    }

    fun entryAt(modelRow: Int): TiBasicVariableEntry = entries[modelRow]
    fun hasColumn(columnId: Int): Boolean = columnId in visibleColumns
    fun modelColumnIndex(columnId: Int): Int? = visibleColumns.indexOf(columnId).takeIf { it >= 0 }
    fun isLineNumberColumn(modelColumn: Int): Boolean = visibleColumns[modelColumn] in lineNumberColumns

    override fun getRowCount(): Int = entries.size
    override fun getColumnCount(): Int = visibleColumns.size
    override fun getColumnName(column: Int): String = COLUMN_NAMES.getValue(visibleColumns[column])

    override fun getColumnClass(column: Int): Class<*> =
        if (isLineNumberColumn(column)) List::class.java else String::class.java

    override fun getValueAt(row: Int, column: Int): Any? {
        val entry = entries[row]
        return when (visibleColumns[column]) {
            NAME_COLUMN -> entry.name
            TYPE_COLUMN -> entry.type.displayName
            DIMENSIONS_COLUMN -> entry.dimensions
            BASE_COLUMN -> entry.optionBase
            DIM_LINE_COLUMN -> entry.dimOccurrences
            WRITES_COLUMN -> entry.occurrences.filter { it.accessType == AccessType.WRITE }
            READS_COLUMN -> entry.occurrences.filter { it.accessType == AccessType.READ }
            RANGE_COLUMN -> entry.rangeDisplay
            else -> null
        }
    }

    private fun visibleColumns(entries: List<TiBasicVariableEntry>): List<Int> =
        buildList {
            addAll(listOf(NAME_COLUMN, TYPE_COLUMN))
            if (entries.any { it.arrayDetails != null }) {
                addAll(ARRAY_COLUMNS)
            }
            addAll(listOf(WRITES_COLUMN, READS_COLUMN, RANGE_COLUMN))
        }
}

private val lineNumberColumns = setOf(DIM_LINE_COLUMN, WRITES_COLUMN, READS_COLUMN)
