package com.github.mmrsic.idea.plugins.tibasic.toolwindow

import javax.swing.table.AbstractTableModel

private val COLUMN_NAMES = mapOf(
    NAME_COLUMN to "Name",
    TYPE_COLUMN to "Type",
    DIMENSIONS_COLUMN to "Dimensions",
    WRITES_COLUMN to "Writes",
    READS_COLUMN to "Reads",
    RANGE_COLUMN to "Range",
)
private val ARRAY_COLUMNS = listOf(DIMENSIONS_COLUMN)
private val ALWAYS_VISIBLE_COLUMNS = listOf(NAME_COLUMN, TYPE_COLUMN, WRITES_COLUMN, READS_COLUMN, RANGE_COLUMN)
const val NAME_COLUMN = 0
const val TYPE_COLUMN = 1
const val DIMENSIONS_COLUMN = 2
const val WRITES_COLUMN = 3
const val READS_COLUMN = 4
const val RANGE_COLUMN = 5

class TiBasicVariableTableModel(
    private var entries: List<TiBasicVariableEntry> = emptyList(),
    private var showArrayElementConstants: Boolean = false,
) : AbstractTableModel() {

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
    fun columnIdAt(modelColumn: Int): Int? = visibleColumns.getOrNull(modelColumn)
    fun isLineNumberColumn(modelColumn: Int): Boolean = visibleColumns[modelColumn] in lineNumberColumns

    fun setShowArrayElementConstants(show: Boolean) {
        if (showArrayElementConstants == show) return
        showArrayElementConstants = show
        fireTableDataChanged()
    }

    override fun getRowCount(): Int = entries.size
    override fun getColumnCount(): Int = visibleColumns.size
    override fun getColumnName(column: Int): String = COLUMN_NAMES.getValue(visibleColumns[column])

    override fun getColumnClass(column: Int): Class<*> =
        when (visibleColumns[column]) {
            DIMENSIONS_COLUMN -> TiBasicVariableDimensionsDisplay::class.java
            in lineNumberColumns -> List::class.java
            else -> String::class.java
        }

    override fun getValueAt(row: Int, column: Int): Any? {
        val entry = entries[row]
        return when (visibleColumns[column]) {
            NAME_COLUMN -> entry.name
            TYPE_COLUMN -> entry.type.displayName
            DIMENSIONS_COLUMN -> entry.dimensionsDisplay
            WRITES_COLUMN -> entry.occurrences.filter { it.accessType == AccessType.WRITE }
            READS_COLUMN -> entry.occurrences.filter { it.accessType == AccessType.READ }
            RANGE_COLUMN -> entry.rangeDisplay(showArrayElementConstants)
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

private val lineNumberColumns = setOf(WRITES_COLUMN, READS_COLUMN)
