package com.github.mmrsic.idea.plugins.tibasic.toolwindow

import com.github.mmrsic.idea.plugins.tibasic.editor.TiBasicCallCharDefinition
import com.github.mmrsic.idea.plugins.tibasic.editor.TiBasicCallColorAssignment
import com.github.mmrsic.idea.plugins.tibasic.lang.TiColor
import javax.swing.Icon
import javax.swing.table.AbstractTableModel

private val COLUMN_NAMES = arrayOf("Code", "ASCII", "Pattern", "Icon", "Lines")
const val CHARACTER_CODE_COLUMN = 0
const val CHARACTER_ASCII_COLUMN = 1
const val CHARACTER_PATTERN_COLUMN = 2
const val CHARACTER_ICON_COLUMN = 3
const val CHARACTER_LINE_COLUMN = 4

internal class TiBasicCharacterDefinitionsTableModel(
    private var entries: List<TiBasicCharacterDefinitionEntry> = emptyList(),
) : AbstractTableModel() {

    fun updateEntries(newEntries: List<TiBasicCharacterDefinitionEntry>) {
        entries = newEntries
        fireTableDataChanged()
    }

    override fun getRowCount(): Int = entries.size
    override fun getColumnCount(): Int = COLUMN_NAMES.size
    override fun getColumnName(column: Int): String = COLUMN_NAMES[column]

    override fun getColumnClass(column: Int): Class<*> = when (column) {
        CHARACTER_CODE_COLUMN -> Int::class.javaObjectType
        CHARACTER_ICON_COLUMN -> TiBasicCharacterIcons::class.java
        CHARACTER_LINE_COLUMN -> List::class.java
        else -> String::class.java
    }

    override fun getValueAt(row: Int, column: Int): Any? {
        val entry = entries[row]
        return when (column) {
            CHARACTER_CODE_COLUMN -> entry.code
            CHARACTER_ASCII_COLUMN -> entry.ascii.orEmpty()
            CHARACTER_PATTERN_COLUMN -> entry.pattern
            CHARACTER_ICON_COLUMN -> entry.icons
            CHARACTER_LINE_COLUMN -> entry.occurrences
            else -> null
        }
    }
}

internal data class TiBasicCharacterColorVariant(
    val fg: TiColor,
    val bg: TiColor,
)

internal data class TiBasicCharacterIcons(
    val pattern: String,
    val colorVariants: List<TiBasicCharacterColorVariant>,
) {
    val icons: List<Icon>
        get() = listOf(com.github.mmrsic.idea.plugins.tibasic.editor.TiBasicCharPatternIcon(pattern)) +
                colorVariants.map { variant ->
                    com.github.mmrsic.idea.plugins.tibasic.editor.TiBasicColoredCharPatternIcon(
                        hexPattern = pattern,
                        fg = variant.fg,
                        bg = variant.bg,
                    )
                }
}

internal data class TiBasicCharacterDefinitionEntry(
    val code: Int,
    val ascii: String?,
    val pattern: String,
    val occurrences: List<TiBasicCharacterDefinitionOccurrence>,
    val icons: TiBasicCharacterIcons,
)

internal data class TiBasicCharacterDefinitionOccurrence(
    val lineNumber: Int,
    val offset: Int,
)

internal fun buildCharacterDefinitionEntries(
    definitions: List<TiBasicCallCharDefinition>,
    colorAssignments: List<TiBasicCallColorAssignment>,
): List<TiBasicCharacterDefinitionEntry> =
    definitions
        .groupBy { definition ->
            Triple(definition.code, definition.ascii, definition.pattern)
        }
        .map { (key, groupedDefinitions) ->
            val representativeDefinition = groupedDefinitions.first()
            TiBasicCharacterDefinitionEntry(
                code = key.first,
                ascii = key.second,
                pattern = key.third,
                occurrences = groupedDefinitions
                    .map { definition ->
                        TiBasicCharacterDefinitionOccurrence(
                            lineNumber = definition.lineNumber,
                            offset = definition.offset,
                        )
                    }
                    .distinctBy { occurrence -> occurrence.lineNumber }
                    .sortedBy(TiBasicCharacterDefinitionOccurrence::lineNumber),
                icons = TiBasicCharacterIcons(
                    pattern = representativeDefinition.pattern,
                    colorVariants = colorAssignments
                        .asSequence()
                        .filter { assignment -> representativeDefinition.code in assignment.codeRange }
                        .map { assignment -> TiBasicCharacterColorVariant(assignment.fg, assignment.bg) }
                        .filterNot { variant -> variant.fg == TiColor.Black && variant.bg == TiColor.White }
                        .distinctBy { variant -> variant.fg to variant.bg }
                        .toList(),
                ),
            )
        }
        .sortedWith(
            compareBy<TiBasicCharacterDefinitionEntry>(
                TiBasicCharacterDefinitionEntry::code,
                { entry -> entry.occurrences.firstOrNull()?.lineNumber ?: Int.MAX_VALUE },
            ),
        )
