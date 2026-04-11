package com.github.mmrsic.idea.plugins.tibasic.editor

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Graphics
import java.awt.Point
import java.awt.Rectangle

private const val GUIDE_VERTICAL_MARGIN_PX = 2
private const val GUIDE_X_SHIFT_PX = -1

internal class TiBasicDisplayColumnGuideRenderer : CustomHighlighterRenderer {

    var guideColumns: List<Int> = emptyList()

    override fun paint(editor: Editor, highlighter: RangeHighlighter, graphics: Graphics) {
        if (guideColumns.isEmpty()) {
            return
        }
        val document = editor.document
        val visibleLines = visibleLineRange(editor, graphics.clipBounds, document.lineCount) ?: return
        graphics.color = guideColor(editor)
        for (lineIndex in visibleLines) {
            val y = editor.logicalPositionToXY(LogicalPosition(lineIndex, 0)).y
            val yStart = y + GUIDE_VERTICAL_MARGIN_PX
            val yEnd = y + editor.lineHeight - GUIDE_VERTICAL_MARGIN_PX
            if (yStart > yEnd) {
                continue
            }
            for (guideColumn in guideColumns) {
                val x = editor.logicalPositionToXY(LogicalPosition(lineIndex, guideColumn)).x + GUIDE_X_SHIFT_PX
                graphics.drawLine(x, yStart, x, yEnd)
            }
        }
    }

    private fun guideColor(editor: Editor): Color =
        editor.colorsScheme.getColor(EditorColors.INDENT_GUIDE_COLOR)
            ?: editor.colorsScheme.getColor(EditorColors.RIGHT_MARGIN_COLOR)
            ?: JBColor.GRAY

    private fun visibleLineRange(editor: Editor, clipBounds: Rectangle, lineCount: Int): IntRange? {
        if (lineCount == 0) {
            return null
        }
        val lastDocumentLine = lineCount - 1
        val firstLine = editor.xyToLogicalPosition(Point(0, clipBounds.y.coerceAtLeast(0))).line.coerceIn(0, lastDocumentLine)
        val lastLineY = (clipBounds.y + clipBounds.height).coerceAtLeast(0)
        val lastLine = editor.xyToLogicalPosition(Point(0, lastLineY)).line.coerceIn(firstLine, lastDocumentLine)
        return firstLine..lastLine
    }
}

