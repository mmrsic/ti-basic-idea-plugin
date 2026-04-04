package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.intellij.codeInsight.hints.declarative.HintColorKind
import com.intellij.codeInsight.hints.declarative.HintFormat
import com.intellij.codeInsight.hints.declarative.InlayHintsCollector
import com.intellij.codeInsight.hints.declarative.InlayHintsProvider
import com.intellij.codeInsight.hints.declarative.InlayTreeSink
import com.intellij.codeInsight.hints.declarative.InlineInlayPosition
import com.intellij.codeInsight.hints.declarative.OwnBypassCollector
import com.intellij.codeInsight.hints.declarative.impl.DeclarativeInlayHintsPassFactory
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile

/** Number of character columns visible per text row on the TI-99/4A display. */
internal const val TI99_4A_DISPLAY_COLUMNS = 28

private val CARET_LISTENER_KEY = Key.create<Boolean>("tibasic.displayColumn.caretListener")

/**
 * Returns the absolute document offsets at which column-break indicators should be placed.
 *
 * Scans the line whose content starts at [lineStart] and has [lineLength] characters,
 * and returns one offset for every multiple of [columnWidth] that falls strictly inside
 * the line (i.e., at column *columnWidth*, 2×*columnWidth*, …, but only while
 * `col < lineLength`).
 */
internal fun displayColumnBreakOffsets(lineStart: Int, lineLength: Int, columnWidth: Int): List<Int> {
    val result = mutableListOf<Int>()
    var col = columnWidth
    while (col < lineLength) {
        result.add(lineStart + col)
        col += columnWidth
    }
    return result
}

/**
 * Inlay hints provider that marks TI-99/4A display column boundaries within each source line.
 *
 * The TI-99/4A text mode shows 28 visible characters per screen row. This provider inserts
 * a thin "┊" separator at every [TI99_4A_DISPLAY_COLUMNS]-th character of the relevant editor
 * lines so programmers can see at a glance where the real hardware would wrap to the next row.
 *
 * The display mode is configurable via [TiBasicColumnHintSettings]:
 * - [ColumnHintDisplayMode.ALL_LINES]: hints on every line (default)
 * - [ColumnHintDisplayMode.CARET_LINE_ONLY]: hint only on the line containing the cursor
 * - [ColumnHintDisplayMode.DISABLED]: no hints
 */
class TiBasicDisplayColumnHintProvider : InlayHintsProvider {

    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector? {
        if (file !is TiBasicFile) return null
        val project = editor.project ?: return null
        if (editor.getUserData(CARET_LISTENER_KEY) != true) {
            editor.putUserData(CARET_LISTENER_KEY, true)
            editor.caretModel.addCaretListener(object : CaretListener {
                private var lastLine = editor.caretModel.currentCaret.logicalPosition.line
                override fun caretPositionChanged(event: CaretEvent) {
                    val newLine = event.newPosition.line
                    if (newLine == lastLine) return
                    lastLine = newLine
                    DeclarativeInlayHintsPassFactory.scheduleRecompute(editor, project)
                }
            })
        }
        return Collector(editor)
    }

    private class Collector(private val editor: Editor) : OwnBypassCollector {

        override fun collectHintsForFile(file: PsiFile, sink: InlayTreeSink) {
            val mode = TiBasicColumnHintSettings.getInstance().displayMode
            if (mode == ColumnHintDisplayMode.DISABLED) return
            val document = editor.document
            val noBackground = HintFormat.default.withColorKind(HintColorKind.TextWithoutBackground)
            val lineRange = when (mode) {
                ColumnHintDisplayMode.CARET_LINE_ONLY -> {
                    val caretLine = editor.caretModel.currentCaret.logicalPosition.line
                    caretLine..caretLine
                }

                else -> 0 until document.lineCount
            }
            for (lineIndex in lineRange) {
                val lineStart = document.getLineStartOffset(lineIndex)
                val lineLength = document.getLineEndOffset(lineIndex) - lineStart
                for (offset in displayColumnBreakOffsets(lineStart, lineLength, TI99_4A_DISPLAY_COLUMNS)) {
                    sink.addPresentation(
                        InlineInlayPosition(offset, relatedToPrevious = false),
                        hintFormat = noBackground,
                    ) { text("┊") }
                }
            }
        }
    }
}
