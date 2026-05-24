package com.github.mmrsic.idea.plugins.tibasic.ide.toolwindow.debug

import com.intellij.ui.JBColor
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer

internal data class TiBasicDebugListingRow(
    val sourceLineIndex: Int,
    val lineNumber: String?,
    val codeChunk: String,
    val continuation: Boolean,
)

internal fun buildDebugListingRows(sourceLines: List<String>): List<TiBasicDebugListingRow> =
    buildList {
        sourceLines.forEachIndexed { sourceLineIndex, sourceLine ->
            val lineNumberMatch = DEBUG_LINE_NUMBER_PATTERN.matchEntire(sourceLine)
            if (lineNumberMatch == null) {
                sourceLine.chunked(DEBUG_PROGRAM_WRAP_LENGTH)
                    .ifEmpty { listOf("") }
                    .forEachIndexed { chunkIndex, chunk ->
                        add(
                            TiBasicDebugListingRow(
                                sourceLineIndex = sourceLineIndex,
                                lineNumber = null,
                                codeChunk = chunk,
                                continuation = chunkIndex > 0,
                            ),
                        )
                    }
                return@forEachIndexed
            }
            val lineNumber = lineNumberMatch.groupValues[1]
            val code = lineNumberMatch.groupValues[2]
            code.chunked(DEBUG_PROGRAM_WRAP_LENGTH)
                .ifEmpty { listOf("") }
                .forEachIndexed { chunkIndex, chunk ->
                    add(
                        TiBasicDebugListingRow(
                            sourceLineIndex = sourceLineIndex,
                            lineNumber = lineNumber.takeIf { chunkIndex == 0 },
                            codeChunk = chunk,
                            continuation = chunkIndex > 0,
                        ),
                    )
                }
        }
    }

internal class TiBasicDebugListingRenderer(
    private val currentSourceLineIndexProvider: () -> Int?,
) : JPanel(BorderLayout()), ListCellRenderer<TiBasicDebugListingRow> {

    private val markerLabel = createLabel()
    private val lineNumberLabel = createLabel(Font.BOLD)
    private val codeLabel = createLabel()
    private val lineNumberPanel = JPanel(BorderLayout())

    init {
        isOpaque = true
        border = BorderFactory.createEmptyBorder(CELL_VERTICAL_PADDING, CELL_HORIZONTAL_PADDING, CELL_VERTICAL_PADDING, CELL_HORIZONTAL_PADDING)
        add(
            JPanel(BorderLayout()).also { prefixPanel ->
                prefixPanel.isOpaque = false
                prefixPanel.border = BorderFactory.createEmptyBorder(0, 0, 0, PREFIX_GAP)
                markerLabel.border = BorderFactory.createEmptyBorder(0, 0, 0, MARKER_GAP)
                prefixPanel.add(markerLabel, BorderLayout.WEST)
                lineNumberPanel.isOpaque = true
                lineNumberPanel.border = BorderFactory.createEmptyBorder(0, LINE_NUMBER_HORIZONTAL_PADDING, 0, LINE_NUMBER_HORIZONTAL_PADDING)
                lineNumberPanel.add(lineNumberLabel, BorderLayout.CENTER)
                prefixPanel.add(lineNumberPanel, BorderLayout.CENTER)
                add(prefixPanel, BorderLayout.WEST)
            },
            BorderLayout.WEST,
        )
        add(codeLabel, BorderLayout.CENTER)
    }

    override fun getListCellRendererComponent(
        list: JList<out TiBasicDebugListingRow>,
        value: TiBasicDebugListingRow,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean,
    ): Component {
        val isProgramCounterLine = value.sourceLineIndex == currentSourceLineIndexProvider()
        val rowBackground = when {
            isProgramCounterLine -> list.selectionBackground
            isSelected -> list.selectionBackground
            else -> list.background
        }
        val rowForeground = when {
            isProgramCounterLine -> list.selectionForeground
            isSelected -> list.selectionForeground
            else -> list.foreground
        }
        background = rowBackground
        foreground = rowForeground
        markerLabel.text = if (isProgramCounterLine && !value.continuation) PROGRAM_COUNTER_PREFIX else NO_PROGRAM_COUNTER_PREFIX
        markerLabel.foreground = rowForeground
        lineNumberLabel.text = value.lineNumber?.padStart(DEBUG_LINE_NUMBER_FIELD_WIDTH) ?: CONTINUATION_LINE_NUMBER_PLACEHOLDER
        lineNumberLabel.foreground = if (isProgramCounterLine) rowForeground else DEBUG_LINE_NUMBER_FOREGROUND
        lineNumberPanel.background = if (isProgramCounterLine) rowBackground else DEBUG_LINE_NUMBER_BACKGROUND
        codeLabel.text = value.codeChunk
        codeLabel.foreground = rowForeground
        return this
    }

    private fun createLabel(style: Int = Font.PLAIN): JLabel =
        JLabel().also { label ->
            label.isOpaque = false
            label.font = Font(Font.MONOSPACED, style, label.font.size)
        }
}

private const val DEBUG_PROGRAM_WRAP_LENGTH = 28
private const val DEBUG_LINE_NUMBER_FIELD_WIDTH = 5
private const val CELL_VERTICAL_PADDING = 1
private const val CELL_HORIZONTAL_PADDING = 4
private const val PREFIX_GAP = 4
private const val MARKER_GAP = 2
private const val LINE_NUMBER_HORIZONTAL_PADDING = 4
private const val PROGRAM_COUNTER_PREFIX = "\u25b6"
private const val NO_PROGRAM_COUNTER_PREFIX = " "
private const val CONTINUATION_LINE_NUMBER_PLACEHOLDER = "     "
private val DEBUG_LINE_NUMBER_PATTERN = Regex("""^(\d+)(.*)$""")
private val DEBUG_LINE_NUMBER_BACKGROUND = JBColor(0xE9F2FF, 0x334861)
private val DEBUG_LINE_NUMBER_FOREGROUND = JBColor(0x204A87, 0xB9D7FF)
