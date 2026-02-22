package com.github.mmrsic.idea.plugins.tibasic

import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicLine
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicPrintStatement
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement

class TiBasicAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is TiBasicFile -> {
                val lines = element.children.filterIsInstance<TiBasicLine>()
                val duplicates = duplicateLineNumbers(lines)
                annotateDuplicateLineNumbers(duplicates, holder)
                annotateNonAscendingLineNumbers(lines, duplicates, holder)
            }
            is TiBasicPrintStatement -> annotateInvalidPrintArgument(element, holder)
        }
    }

    private fun annotateInvalidPrintArgument(statement: TiBasicPrintStatement, holder: AnnotationHolder) {
        val invalidArg = statement.node.findChildByType(TiBasicTokenTypes.PRINT_ARGUMENT) ?: return
        holder
            .newAnnotation(HighlightSeverity.ERROR, "PRINT argument must be a string literal expression")
            .range(invalidArg.textRange)
            .create()
    }

    private fun duplicateLineNumbers(lines: List<TiBasicLine>): Set<TiBasicLine> {
        val seen = mutableSetOf<Int>()
        return lines.filter { !seen.add(it.lineNumber()) }.toSet()
    }

    private fun annotateDuplicateLineNumbers(duplicates: Set<TiBasicLine>, holder: AnnotationHolder) {
        duplicates
            .forEach { line ->
                holder
                    .newAnnotation(HighlightSeverity.ERROR, "Duplicate line number ${line.lineNumber()}")
                    .range(line)
                    .withFix(ResequenceQuickFix(line))
                    .create()
            }
    }

    private fun annotateNonAscendingLineNumbers(lines: List<TiBasicLine>, duplicates: Set<TiBasicLine>, holder: AnnotationHolder) {
        lines.zipWithNext().forEach { (previous, current) ->
            if (current in duplicates) return@forEach
            if (current.lineNumber() <= previous.lineNumber()) {
                holder
                    .newAnnotation(
                        HighlightSeverity.WARNING,
                        "Line number ${current.lineNumber()} does not follow ascending order (previous: ${previous.lineNumber()})",
                    )
                    .range(current)
                    .withFix(ResequenceQuickFix(current))
                    .create()
            }
        }
    }
}

