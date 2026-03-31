package com.github.mmrsic.idea.plugins.tibasic.toolwindow

import javax.swing.table.AbstractTableModel

private val COLUMN_NAMES = arrayOf("Name", "Type", "Writes", "Reads", "Const")
const val WRITES_COLUMN = 2
const val READS_COLUMN = 3
const val CONST_COLUMN = 4

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
        2, 3 -> List::class.java
        else -> String::class.java
    }

    override fun getValueAt(row: Int, column: Int): Any? {
        val entry = entries[row]
        return when (column) {
            0 -> entry.name
            1 -> entry.type.displayName
            2 -> entry.occurrences.filter { it.accessType == AccessType.WRITE }
            3 -> entry.occurrences.filter { it.accessType == AccessType.READ }
            4 -> entry.constValue
            else -> null
        }
    }
}
