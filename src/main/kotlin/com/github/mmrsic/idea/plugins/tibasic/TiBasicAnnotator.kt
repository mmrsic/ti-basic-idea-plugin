package com.github.mmrsic.idea.plugins.tibasic

import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicExpression
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicLine
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicPrintStatement
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType

class TiBasicAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is TiBasicFile -> {
                val lines = element.children.filterIsInstance<TiBasicLine>()
                val duplicates = duplicateLineNumbers(lines)
                annotateDuplicateLineNumbers(duplicates, holder)
                annotateNonAscendingLineNumbers(lines, duplicates, holder)
                annotateVariableNameConflicts(element, holder)
            }
            is TiBasicPrintStatement -> annotateInvalidPrintArgument(element, holder)
        }
    }

    private fun annotateInvalidPrintArgument(statement: TiBasicPrintStatement, holder: AnnotationHolder) {
        val validChildren = setOf(
            TiBasicTokenTypes.PRINT_KEYWORD,
            TokenType.WHITE_SPACE,
            TiBasicNodeTypes.EXPRESSION,
        )
        statement.node.getChildren(null)
            .filter { it.elementType !in validChildren }
            .forEach { invalidChild ->
                val message = when (invalidChild.elementType) {
                    TiBasicTokenTypes.INVALID_VARIABLE_NAME -> "Bad variable name"
                    TiBasicTokenTypes.INVALID_SUBSCRIPT -> "Bad subscript definition"
                    else -> "PRINT argument must be an expression"
                }
                holder
                    .newAnnotation(HighlightSeverity.ERROR, message)
                    .range(invalidChild.textRange)
                    .create()
            }
    }

    private fun annotateVariableNameConflicts(file: TiBasicFile, holder: AnnotationHolder) {
        val varNodes = file.children
            .filterIsInstance<TiBasicLine>()
            .flatMap { it.children.filterIsInstance<TiBasicPrintStatement>() }
            .flatMap { it.children.filterIsInstance<TiBasicExpression>() }
            .flatMap { expr ->
                expr.node.getChildren(null)
                    .filter { it.elementType == TiBasicTokenTypes.STRING_VARIABLE }
                    .toList()
            }
        val byName = mutableMapOf<String, MutableList<ASTNode>>()
        for (node in varNodes) {
            byName.getOrPut(variableName(node.text)) { mutableListOf() }.add(node)
        }
        for ((_, nodes) in byName) {
            val distinctDims = nodes.map { subscriptDimCount(it.text) }.toSet()
            if (distinctDims.size > 1) {
                nodes.forEach { node ->
                    holder
                        .newAnnotation(HighlightSeverity.ERROR, "Name conflict")
                        .range(node.textRange)
                        .create()
                }
            }
        }
    }

    private fun variableName(tokenText: String): String =
        tokenText.substring(0, tokenText.indexOf('$') + 1).uppercase()

    private fun subscriptDimCount(tokenText: String): Int {
        val subscript = tokenText.substring(tokenText.indexOf('$') + 1).trim()
        return if (subscript.isEmpty()) 0 else subscript.count { it == ',' } + 1
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

