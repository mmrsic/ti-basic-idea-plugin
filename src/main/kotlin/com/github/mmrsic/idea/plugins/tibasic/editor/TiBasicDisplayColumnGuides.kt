package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicFileType
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager

internal const val TI99_4A_DISPLAY_COLUMNS = 28

internal fun displayColumnBreakOffsets(lineStart: Int, lineLength: Int, columnWidth: Int): List<Int> {
    val result = mutableListOf<Int>()
    var col = columnWidth
    while (col < lineLength) {
        result.add(lineStart + col)
        col += columnWidth
    }
    return result
}

internal fun longestLineLength(document: Document): Int =
    (0 until document.lineCount)
        .maxOfOrNull { lineIndex ->
            document.getLineEndOffset(lineIndex) - document.getLineStartOffset(lineIndex)
        }
        ?: 0

internal fun displayColumnGuideColumns(longestLineLength: Int, columnWidth: Int): List<Int> {
    val result = mutableListOf<Int>()
    var column = columnWidth
    while (column < longestLineLength) {
        result.add(column)
        column += columnWidth
    }
    return result
}

internal fun isTiBasicEditor(editor: Editor): Boolean =
    FileDocumentManager.getInstance().getFile(editor.document)?.fileType == TiBasicFileType

