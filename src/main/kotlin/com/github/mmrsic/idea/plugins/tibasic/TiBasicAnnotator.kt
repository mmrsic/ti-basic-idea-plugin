package com.github.mmrsic.idea.plugins.tibasic

import com.github.mmrsic.idea.plugins.tibasic.psi.*
import com.intellij.lang.ASTNode
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
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

            is TiBasicLine -> annotateLineNumber(element, holder)
            is TiBasicPrintStatement -> annotateInvalidPrintArgument(element, holder)
            is TiBasicLineNumberListStatement -> annotateLineNumberListStatement(element, holder)
            is TiBasicDeleteStatement -> annotateDeleteStatement(element, holder)
            is TiBasicUnknownStatement -> annotateUnknownStatement(element, holder)
            is TiBasicInvalidLine -> holder.newAnnotation(HighlightSeverity.ERROR, "Line number expected").range(element).create()
            is TiBasicVariableAccess -> annotateVariableAccess(element, holder)
            is TiBasicExpression -> annotateExpression(element, holder)
        }
    }

    private fun annotateUnknownStatement(statement: TiBasicUnknownStatement, holder: AnnotationHolder) {
        val statementNode = statement.node.firstChildNode ?: return
        val firstWord = statementNode.text.trimEnd().split(Regex("""\s+""")).first().uppercase()
        val message = if (firstWord in COMMANDS_UPPERCASE) "Command must not be used as statement"
        else "Incorrect statement"
        holder.newAnnotation(HighlightSeverity.ERROR, message).range(statementNode.textRange).create()
    }

    private fun annotateInvalidPrintArgument(statement: TiBasicPrintStatement, holder: AnnotationHolder) {
        val validChildren = setOf(TiBasicTokenTypes.PRINT_KEYWORD, TokenType.WHITE_SPACE, TiBasicNodeTypes.EXPRESSION)
        val expression = statement.children.filterIsInstance<TiBasicExpression>().firstOrNull()
        val isNumericExpr = expression != null && !isStringExpression(expression)
        val isStringExpr = expression != null && isStringExpression(expression)
        statement.node.getChildren(null)
            .filter { it.elementType !in validChildren }
            .forEach { child ->
                val message = when {
                    child.elementType == TiBasicTokenTypes.INVALID_VARIABLE_NAME -> "Bad variable name"
                    isNumericExpr && child.elementType in STRING_MISMATCH_TYPES -> "String-Number-Mismatch"
                    isNumericExpr && child.elementType == TiBasicNodeTypes.VARIABLE_ACCESS &&
                            child.firstChildNode?.elementType == TiBasicTokenTypes.STRING_VARIABLE -> "String-Number-Mismatch"

                    isStringExpr && child.elementType in NUMERIC_MISMATCH_TYPES -> "String-Number-Mismatch"
                    else -> "PRINT argument must be an expression"
                }
                holder.newAnnotation(HighlightSeverity.ERROR, message).range(child.textRange).create()
            }
    }

    private fun annotateLineNumberListStatement(statement: TiBasicLineNumberListStatement, holder: AnnotationHolder) {
        val children = statement.node.getChildren(null)
            .filter { it.elementType != TokenType.WHITE_SPACE && it.elementType != TiBasicTokenTypes.LINE_NUMBER_LIST_KEYWORD }
        var expectNumber = true
        var trailingComma: ASTNode? = null
        for (child in children) {
            if (expectNumber) {
                when {
                    child.elementType == TiBasicTokenTypes.NUMERIC_LITERAL -> {
                        val value = child.text.toLongOrNull()
                        if (value == null || value !in 1L..32767L) {
                            holder.newAnnotation(HighlightSeverity.ERROR, "Line number must be between 1 and 32767")
                                .range(child.textRange).create()
                        }
                        expectNumber = false
                        trailingComma = null
                    }

                    else -> {
                        holder.newAnnotation(HighlightSeverity.ERROR, "Line number expected")
                            .range(child.textRange).create()
                        trailingComma = null
                    }
                }
            } else {
                when {
                    child.elementType == TiBasicTokenTypes.COMMA -> {
                        expectNumber = true
                        trailingComma = child
                    }

                    else -> {
                        holder.newAnnotation(HighlightSeverity.ERROR, "Comma expected")
                            .range(child.textRange).create()
                        trailingComma = null
                    }
                }
            }
        }
        if (trailingComma != null) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Line number expected")
                .range(trailingComma.textRange).create()
        }
        annotateUndefinedLineNumbers(statement, children, holder)
    }

    private fun annotateUndefinedLineNumbers(
        statement: TiBasicLineNumberListStatement,
        children: List<ASTNode>,
        holder: AnnotationHolder,
    ) {
        val file = statement.containingFile as? TiBasicFile ?: return
        val definedLineNumbers =
            file.children
                .filterIsInstance<TiBasicLine>()
                .map { it.lineNumber() }
                .filter { it in 1..32767 }
                .toSet()
        children
            .filter { it.elementType == TiBasicTokenTypes.NUMERIC_LITERAL }
            .forEach { child ->
                val value = child.text.toLongOrNull() ?: return@forEach
                if (value in 1L..32767L && value.toInt() !in definedLineNumbers) {
                    holder.newAnnotation(HighlightSeverity.WARNING, "Bad line number")
                        .range(child.textRange).create()
                }
            }
    }

    private fun annotateDeleteStatement(statement: TiBasicDeleteStatement, holder: AnnotationHolder) {
        val validChildren = setOf(TiBasicTokenTypes.DELETE_KEYWORD, TokenType.WHITE_SPACE, TiBasicNodeTypes.EXPRESSION)
        val expression = statement.children.filterIsInstance<TiBasicExpression>().firstOrNull()
        val keywordNode = statement.node.getChildren(null)
            .first { it.elementType == TiBasicTokenTypes.DELETE_KEYWORD }
        if (expression == null) {
            holder.newAnnotation(HighlightSeverity.ERROR, "String expression expected")
                .range(keywordNode.textRange).create()
            return
        }
        if (!isStringExpression(expression)) {
            holder.newAnnotation(HighlightSeverity.ERROR, "String expression expected")
                .range(expression.textRange).create()
        }
        statement.node.getChildren(null)
            .filter { it.elementType !in validChildren }
            .forEach { child ->
                holder.newAnnotation(HighlightSeverity.ERROR, "String expression expected")
                    .range(child.textRange).create()
            }
    }

    private fun annotateVariableAccess(varAccess: TiBasicVariableAccess, holder: AnnotationHolder) {
        if (!varAccess.hasSubscriptParens()) return
        val dimCount = varAccess.subscriptDimCount()
        if (dimCount == 0 || dimCount > 3) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Bad subscript definition")
                .range(varAccess)
                .create()
        }
    }

    private fun annotateExpression(expr: TiBasicExpression, holder: AnnotationHolder) {
        val children = expr.node.getChildren(null).filter { it.elementType != TokenType.WHITE_SPACE }
        if (children.any { it.elementType in COMPARISON_OP_TYPES }) return
        val isString = isStringExpression(expr)
        children.forEach { child ->
            val isMismatch = if (isString) {
                child.elementType in NUMERIC_MISMATCH_TYPES ||
                        (child.elementType == TiBasicNodeTypes.VARIABLE_ACCESS &&
                                child.firstChildNode?.elementType == TiBasicTokenTypes.NUMERIC_VARIABLE)
            } else {
                child.elementType in STRING_MISMATCH_TYPES ||
                        (child.elementType == TiBasicNodeTypes.VARIABLE_ACCESS &&
                                child.firstChildNode?.elementType == TiBasicTokenTypes.STRING_VARIABLE)
            }
            if (isMismatch) {
                holder.newAnnotation(HighlightSeverity.ERROR, "String-Number-Mismatch")
                    .range(child.textRange)
                    .create()
            }
        }
    }

    private fun isStringExpression(expr: TiBasicExpression): Boolean {
        val children = expr.node.getChildren(null).filter { it.elementType != TokenType.WHITE_SPACE }
        if (children.any { it.elementType in COMPARISON_OP_TYPES }) return false
        if (children.any { it.elementType == TiBasicTokenTypes.CONCAT_OP }) return true
        val first = children.firstOrNull() ?: return false
        if (first.elementType == TiBasicTokenTypes.STRING_LITERAL) return true
        if (first.elementType == TiBasicNodeTypes.VARIABLE_ACCESS &&
            first.firstChildNode?.elementType == TiBasicTokenTypes.STRING_VARIABLE
        ) return true
        if (first.elementType == TiBasicTokenTypes.LPAREN) {
            val inner = children.drop(1).dropLast(1)
            if (inner.isEmpty()) return false
            if (inner.any { it.elementType == TiBasicTokenTypes.CONCAT_OP }) return true
            val firstInner = inner.firstOrNull() ?: return false
            return firstInner.elementType == TiBasicTokenTypes.STRING_LITERAL ||
                    (firstInner.elementType == TiBasicNodeTypes.VARIABLE_ACCESS &&
                            firstInner.firstChildNode?.elementType == TiBasicTokenTypes.STRING_VARIABLE)
        }
        return false
    }

    private fun annotateLineNumber(line: TiBasicLine, holder: AnnotationHolder) {
        val lineNumberNode = line.node.firstChildNode
        val lineNumber = lineNumberNode.text.toLongOrNull()
        if (lineNumber == null || lineNumber !in 1L..32767L) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Bad line number").range(lineNumberNode.textRange).create()
        }
    }

    private fun annotateVariableNameConflicts(file: TiBasicFile, holder: AnnotationHolder) {
        val allAccesses = collectVariableAccesses(file)
        val stringAccesses = allAccesses.filter { it.firstChildNode?.elementType == TiBasicTokenTypes.STRING_VARIABLE }
        val numericAccesses = allAccesses.filter { it.firstChildNode?.elementType == TiBasicTokenTypes.NUMERIC_VARIABLE }
        annotateNameConflicts(stringAccesses, holder, ::variableAccessName, ::variableAccessDimCount)
        annotateNameConflicts(numericAccesses, holder, ::variableAccessName, ::variableAccessDimCount)
        val commands = COMMANDS_UPPERCASE
        val keywords = TiBasicKeywords.getKeywords().map { it.uppercase() }.toSet()
        allAccesses.forEach { node ->
            val name = variableAccessName(node)
            val message = when (name) {
                in commands -> "Command must not be used as variable name"
                in keywords -> "Keyword cannot be used as variable name"
                else -> null
            }
            if (message != null) {
                holder.newAnnotation(HighlightSeverity.ERROR, message)
                    .range(node.textRange)
                    .create()
            }
        }
    }

    private fun collectVariableAccesses(file: TiBasicFile): List<ASTNode> {
        val result = mutableListOf<ASTNode>()
        fun traverse(node: ASTNode) {
            if (node.elementType == TiBasicNodeTypes.VARIABLE_ACCESS) result.add(node)
            for (child in node.getChildren(null)) traverse(child)
        }
        file.children.filterIsInstance<TiBasicLine>()
            .flatMap { it.children.filterIsInstance<TiBasicPrintStatement>() }
            .flatMap { it.children.filterIsInstance<TiBasicExpression>() }
            .forEach { traverse(it.node) }
        return result
    }

    private fun annotateNameConflicts(
        nodes: List<ASTNode>,
        holder: AnnotationHolder,
        nameOf: (ASTNode) -> String,
        dimCountOf: (ASTNode) -> Int,
    ) {
        val byName = mutableMapOf<String, MutableList<ASTNode>>()
        for (node in nodes) byName.getOrPut(nameOf(node)) { mutableListOf() }.add(node)
        for ((_, conflictNodes) in byName) {
            if (conflictNodes.map { dimCountOf(it) }.toSet().size > 1) {
                conflictNodes.forEach { node ->
                    holder.newAnnotation(HighlightSeverity.ERROR, "Name conflict").range(node.textRange).create()
                }
            }
        }
    }

    private fun variableAccessName(node: ASTNode): String = node.firstChildNode.text.uppercase()

    private fun variableAccessDimCount(node: ASTNode): Int =
        node.getChildren(null).count { it.elementType == TiBasicNodeTypes.EXPRESSION }

    private fun duplicateLineNumbers(lines: List<TiBasicLine>): Set<TiBasicLine> {
        val seen = mutableSetOf<Int>()
        return lines.filter { it.lineNumber() in 1..32767 && !seen.add(it.lineNumber()) }.toSet()
    }

    private fun annotateDuplicateLineNumbers(duplicates: Set<TiBasicLine>, holder: AnnotationHolder) {
        duplicates.forEach { line ->
            holder.newAnnotation(HighlightSeverity.ERROR, "Duplicate line number ${line.lineNumber()}")
                .range(line)
                .withFix(ResequenceQuickFix(line))
                .create()
        }
    }

    private fun annotateNonAscendingLineNumbers(lines: List<TiBasicLine>, duplicates: Set<TiBasicLine>, holder: AnnotationHolder) {
        lines.filter { it.lineNumber() in 1..32767 }.zipWithNext().forEach { (previous, current) ->
            if (current in duplicates) return@forEach
            if (current.lineNumber() <= previous.lineNumber()) {
                holder.newAnnotation(
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

private val COMMANDS_UPPERCASE = TiBasicKeywords.getCommands().map { it.uppercase() }.toSet()

private val COMPARISON_OP_TYPES = setOf(
    TiBasicTokenTypes.EQ_OP,
    TiBasicTokenTypes.LT_OP,
    TiBasicTokenTypes.GT_OP,
    TiBasicTokenTypes.NEQ_OP,
    TiBasicTokenTypes.LE_OP,
    TiBasicTokenTypes.GE_OP,
)

private val STRING_MISMATCH_TYPES = setOf(
    TiBasicTokenTypes.STRING_LITERAL,
    TiBasicTokenTypes.STRING_VARIABLE,
    TiBasicTokenTypes.CONCAT_OP,
)

private val NUMERIC_MISMATCH_TYPES = setOf(
    TiBasicTokenTypes.NUMERIC_LITERAL,
    TiBasicTokenTypes.PLUS_OP,
    TiBasicTokenTypes.MINUS_OP,
    TiBasicTokenTypes.MUL_OP,
    TiBasicTokenTypes.DIV_OP,
    TiBasicTokenTypes.POW_OP,
    TiBasicTokenTypes.EQ_OP,
    TiBasicTokenTypes.LT_OP,
    TiBasicTokenTypes.GT_OP,
    TiBasicTokenTypes.NEQ_OP,
    TiBasicTokenTypes.LE_OP,
    TiBasicTokenTypes.GE_OP,
)
