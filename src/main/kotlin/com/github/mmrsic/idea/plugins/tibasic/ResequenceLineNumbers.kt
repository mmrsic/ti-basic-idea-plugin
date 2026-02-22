package com.github.mmrsic.idea.plugins.tibasic

import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicLine

const val RESEQUENCE_DEFAULT_START = 100
const val RESEQUENCE_DEFAULT_STEP = 10

fun resequencedText(file: TiBasicFile, start: Int = RESEQUENCE_DEFAULT_START, step: Int = RESEQUENCE_DEFAULT_STEP): String {
    val lines = file.children.filterIsInstance<TiBasicLine>()
    val originalText = file.text
    if (lines.isEmpty()) return originalText
    val result = StringBuilder(originalText)
    var delta = 0
    lines.forEachIndexed { index, line ->
        val newNumber = start + index * step
        val oldNumberText = line.lineNumber().toString()
        val newNumberText = newNumber.toString()
        val lineStart = line.textRange.startOffset + delta
        val numberEnd = lineStart + oldNumberText.length
        result.replace(lineStart, numberEnd, newNumberText)
        delta += newNumberText.length - oldNumberText.length
    }
    return result.toString()
}

