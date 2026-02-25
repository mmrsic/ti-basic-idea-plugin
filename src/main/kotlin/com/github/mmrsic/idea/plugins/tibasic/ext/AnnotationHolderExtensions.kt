package com.github.mmrsic.idea.plugins.tibasic.ext

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

fun AnnotationHolder.error(message: String, element: PsiElement, quickFix: IntentionAction? = null) =
    newAnnotation(HighlightSeverity.ERROR, message).range(element)
        .let { if (quickFix != null) it.withFix(quickFix) else it }
        .create()

fun AnnotationHolder.error(message: String, range: TextRange) =
    newAnnotation(HighlightSeverity.ERROR, message).range(range).create()

fun AnnotationHolder.warning(message: String, element: PsiElement, quickFix: IntentionAction? = null) =
    newAnnotation(HighlightSeverity.WARNING, message).range(element)
        .let { if (quickFix != null) it.withFix(quickFix) else it }
        .create()

fun AnnotationHolder.warning(message: String, range: TextRange) =
    newAnnotation(HighlightSeverity.WARNING, message).range(range).create()
