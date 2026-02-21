package com.github.mmrsic.idea.plugins.tibasic

import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicLine
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement

class TiBasicAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is TiBasicFile) return
        val lines = element.children.filterIsInstance<TiBasicLine>()
        lines.zipWithNext().forEach { (previous, current) ->
            if (current.lineNumber() <= previous.lineNumber()) {
                holder.newAnnotation(
                    HighlightSeverity.WARNING,
                    "Line number ${current.lineNumber()} does not follow ascending order (previous: ${previous.lineNumber()})"
                ).range(current).create()
            }
        }
    }
}

