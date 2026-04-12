package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.ext.lineNumberReferenceNodes
import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicLanguage
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.common.VALID_LINE_NUMBER_RANGE
import com.intellij.codeInsight.editorActions.CopyPastePreProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RawText
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory

class TiBasicPastePreProcessor : CopyPastePreProcessor {

    override fun preprocessOnCopy(file: PsiFile, startOffsets: IntArray, endOffsets: IntArray, text: String): String? = null

    override fun preprocessOnPaste(project: Project, file: PsiFile, editor: Editor, text: String, rawText: RawText?): String {
        if (file !is TiBasicFile || !isAtEndOfFile(editor, file)) return text
        val maxLineNumber = file.lines().maxValidLineNumber()
        return renumberLines(text, maxLineNumber, project)
    }

    private fun renumberLines(text: String, maxLineNumber: Int, project: Project): String {
        val tempFile = PsiFileFactory.getInstance(project)
            .createFileFromText("_paste.tibasic", TiBasicLanguage, text) as? TiBasicFile
            ?: return renumberLinesTextOnly(text, maxLineNumber)
        val replacements = mutableListOf<TextReplacement>()
        var increment = 0
        for (line in tempFile.lines()) {
            val oldNumber = line.lineNumber()
            if (oldNumber !in VALID_LINE_NUMBER_RANGE) continue
            val newNumber = nextLineNumber(maxLineNumber, increment++)
            val delta = newNumber - oldNumber
            val lineStart = line.textRange.startOffset
            replacements.add(TextReplacement(lineStart, lineStart + oldNumber.toString().length, newNumber.toString()))
            line.children.flatMap { it.lineNumberReferenceNodes() }.forEach { node ->
                val refValue = node.text.toIntOrNull() ?: return@forEach
                replacements.add(TextReplacement(node.startOffset, node.startOffset + node.textLength, (refValue + delta).toString()))
            }
        }
        return applyTextReplacements(text, replacements)
    }

    private fun renumberLinesTextOnly(text: String, maxLineNumber: Int): String {
        var increment = 0
        return text.split('\n').joinToString("\n") { line ->
            val lineNumber = leadingLineNumber(line) ?: return@joinToString line
            nextLineNumber(maxLineNumber, increment++).toString() + line.substring(lineNumber.toString().length)
        }
    }

    private fun leadingLineNumber(line: String): Int? {
        val digits = line.takeWhile { it.isDigit() }
        if (digits.isEmpty()) return null
        val rest = line.substring(digits.length)
        if (rest.isNotEmpty() && !rest.startsWith(' ')) return null
        val number = digits.toIntOrNull() ?: return null
        return if (number in VALID_LINE_NUMBER_RANGE) number else null
    }
}
