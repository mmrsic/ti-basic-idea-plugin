package com.github.mmrsic.idea.plugins.tibasic.toolwindow

import javax.swing.table.AbstractTableModel

private val COLUMN_NAMES = arrayOf("Name", "Type", "Dimensions", "Base", "DIM", "Writes", "Reads", "Range")
const val DIMENSIONS_COLUMN = 2
const val BASE_COLUMN = 3
const val DIM_LINE_COLUMN = 4
const val WRITES_COLUMN = 5
const val READS_COLUMN = 6
const val RANGE_COLUMN = 7

class TiBasicVariableTableModel(private var entries: List<TiBasicVariableEntry> = emptyList()) : AbstractTableModel() {

    fun updateEntries(newEntries: List<TiBasicVariableEntry>) {
        entries = newEntries
        fireTableDataChanged()
    }

    fun entryAt(modelRow: Int): TiBasicVariableEntry = entries[modelRow]

    override fun getRowCount(): Int = entries.size
    override fun getColumnCount(): Int = COLUMN_NAMES.size
    override fun getColumnName(column: Int): String = COLUMN_NAMES[column]

    override fun getColumnClass(column: Int): Class<*> = when (column) {
        DIM_LINE_COLUMN, WRITES_COLUMN, READS_COLUMN -> List::class.java
        else -> String::class.java
    }

    override fun getValueAt(row: Int, column: Int): Any? {
        val entry = entries[row]
        return when (column) {
            0 -> entry.name
            1 -> entry.type.displayName
            DIMENSIONS_COLUMN -> entry.dimensions
            BASE_COLUMN -> entry.optionBase
            DIM_LINE_COLUMN -> entry.dimOccurrences
            WRITES_COLUMN -> entry.occurrences.filter { it.accessType == AccessType.WRITE }
            READS_COLUMN -> entry.occurrences.filter { it.accessType == AccessType.READ }
            RANGE_COLUMN -> entry.rangeDisplay
            else -> null
        }
    }
}
