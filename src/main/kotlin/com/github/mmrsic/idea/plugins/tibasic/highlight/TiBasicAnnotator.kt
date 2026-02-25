package com.github.mmrsic.idea.plugins.tibasic.highlight

import com.github.mmrsic.idea.plugins.tibasic.action.resequence.ResequenceQuickFix
import com.github.mmrsic.idea.plugins.tibasic.ext.allChildren
import com.github.mmrsic.idea.plugins.tibasic.ext.childrenOfType
import com.github.mmrsic.idea.plugins.tibasic.ext.error
import com.github.mmrsic.idea.plugins.tibasic.ext.firstChildOfType
import com.github.mmrsic.idea.plugins.tibasic.ext.firstChildType
import com.github.mmrsic.idea.plugins.tibasic.ext.nonWhitespaceChildren
import com.github.mmrsic.idea.plugins.tibasic.ext.warning
import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicKeywords
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicGotoStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicOnGotoStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicDeleteStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicEndStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicExpression
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicInvalidLine
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicLetStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicLine
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicLineNumberListStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicPrintStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicStopStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicUnknownStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicVariableAccess
import com.github.mmrsic.idea.plugins.tibasic.psi.VALID_LINE_NUMBER_RANGE
import com.github.mmrsic.idea.plugins.tibasic.psi.containingTiBasicFile
import com.intellij.lang.ASTNode
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType

class TiBasicAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is TiBasicFile -> {
                val lines = element.lines()
                val duplicates = duplicateLineNumbers(lines)
                annotateDuplicateLineNumbers(duplicates, holder)
                annotateNonAscendingLineNumbers(lines, duplicates, holder)
                annotateVariableNameConflicts(element, holder)
            }

            is TiBasicLine -> annotateLineNumber(element, holder)
            is TiBasicLetStatement -> annotateLetStatement(element, holder)
            is TiBasicPrintStatement -> annotateInvalidPrintArgument(element, holder)
            is TiBasicLineNumberListStatement -> annotateLineNumberListStatement(element, holder)
            is TiBasicDeleteStatement -> annotateDeleteStatement(element, holder)
            is TiBasicGotoStatement -> annotateGotoStatement(element, holder)
            is TiBasicOnGotoStatement -> annotateOnGotoStatement(element, holder)
            is TiBasicEndStatement -> annotateTrailingContent(element.node, "END", holder)
            is TiBasicStopStatement -> annotateTrailingContent(element.node, "STOP", holder)
            is TiBasicUnknownStatement -> annotateUnknownStatement(element, holder)
            is TiBasicInvalidLine -> holder.error("Line number expected", element)
            is TiBasicVariableAccess -> annotateVariableAccess(element, holder)
            is TiBasicExpression -> annotateExpression(element, holder)
        }
    }

    private fun annotateTrailingContent(stmtNode: ASTNode, stmtName: String, holder: AnnotationHolder) {
        val trailing = stmtNode.allChildren
            .dropWhile { it.elementType == TiBasicTokenTypes.END_KEYWORD || it.elementType == TiBasicTokenTypes.STOP_KEYWORD }
            .filter { it.elementType != TokenType.WHITE_SPACE }
        if (trailing.isEmpty()) return
        val range = TextRange(trailing.first().startOffset, trailing.last().startOffset + trailing.last().textLength)
        holder.warning("Everything after $stmtName statement is ignored", range)
    }

    private fun annotateGotoStatement(statement: TiBasicGotoStatement, holder: AnnotationHolder) {
        val contentNodes = statement.node.nonWhitespaceChildren
            .filter { it.elementType != TiBasicTokenTypes.GOTO_KEYWORD }
        if (contentNodes.size != 1 || contentNodes[0].elementType != TiBasicTokenTypes.NUMERIC_LITERAL) {
            holder.error("Incorrect statement", statement)
            return
        }
        val lineNumberNode = contentNodes[0]
        val lineNumber = lineNumberNode.text.toLongOrNull()?.toInt()
        if (lineNumber == null || lineNumber !in VALID_LINE_NUMBER_RANGE) {
            holder.error("Bad line number", lineNumberNode.textRange)
            return
        }
        val file = statement.containingTiBasicFile ?: return
        val definedLineNumbers = file.lines()
            .map { it.lineNumber() }
            .filter { it in VALID_LINE_NUMBER_RANGE }
            .toSet()
        if (lineNumber !in definedLineNumbers) {
            holder.warning("Bad line number", lineNumberNode.textRange)
        }
    }

    private fun annotateOnGotoStatement(statement: TiBasicOnGotoStatement, holder: AnnotationHolder) {
        val children = statement.node.nonWhitespaceChildren
        val expression = statement.firstChildOfType<TiBasicExpression>()
        val gotoKeywordNode = children.firstOrNull { it.elementType == TiBasicTokenTypes.GOTO_KEYWORD }

        if (expression == null || gotoKeywordNode == null) {
            holder.error("Incorrect statement", statement)
            return
        }

        if (isStringExpression(expression)) {
            holder.error("String-number mismatch", expression)
        }

        val afterGoto = children
            .dropWhile { it.elementType != TiBasicTokenTypes.GOTO_KEYWORD }
            .drop(1)

        if (afterGoto.isEmpty() || afterGoto[0].elementType != TiBasicTokenTypes.NUMERIC_LITERAL) {
            holder.error("Incorrect statement", statement)
            return
        }

        val file = statement.containingTiBasicFile ?: return
        val definedLineNumbers = file.lines()
            .map { it.lineNumber() }
            .filter { it in VALID_LINE_NUMBER_RANGE }
            .toSet()

        var expectNumber = true
        var trailingComma: ASTNode? = null
        for (child in afterGoto) {
            if (expectNumber) {
                when {
                    child.elementType == TiBasicTokenTypes.NUMERIC_LITERAL -> {
                        val value = child.text.toLongOrNull()?.toInt()
                        if (value == null || value !in VALID_LINE_NUMBER_RANGE) {
                            holder.error("Bad line number", child.textRange)
                        } else if (value !in definedLineNumbers) {
                            holder.warning("Bad line number", child.textRange)
                        }
                        expectNumber = false
                        trailingComma = null
                    }

                    else -> {
                        holder.error("Bad line number", child.textRange)
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
                        holder.error("Bad line number", child.textRange)
                        trailingComma = null
                    }
                }
            }
        }
        if (trailingComma != null) {
            holder.error("Bad line number", trailingComma.textRange)
        }
    }

    private fun annotateUnknownStatement(statement: TiBasicUnknownStatement, holder: AnnotationHolder) {
        val statementNode = statement.node.firstChildNode ?: return
        val firstWord = statementNode.text.trimEnd().split(Regex("""\s+""")).first().uppercase()
        val message = if (firstWord in COMMANDS_UPPERCASE) "Command must not be used as statement"
        else "Incorrect statement"
        holder.error(message, statementNode.textRange)
    }

    private fun annotateLetStatement(statement: TiBasicLetStatement, holder: AnnotationHolder) {
        val varAccessNode = statement.node.firstChildOfType(TiBasicNodeTypes.VARIABLE_ACCESS)
        if (varAccessNode != null && varAccessNode.firstChildType == TiBasicTokenTypes.INVALID_VARIABLE_NAME) {
            holder.error("Bad variable name", varAccessNode.textRange)
            return
        }
        val expression = statement.firstChildOfType<TiBasicExpression>() ?: return
        if (varAccessNode == null) return
        val isStringVar = varAccessNode.firstChildType == TiBasicTokenTypes.STRING_VARIABLE
        val isStringExpr = isStringExpression(expression)
        if (isStringVar != isStringExpr) {
            val mismatchRange = TextRange(varAccessNode.textRange.startOffset, expression.textRange.endOffset)
            holder.error("String-number mismatch", mismatchRange)
        }
    }

    private fun annotateInvalidPrintArgument(statement: TiBasicPrintStatement, holder: AnnotationHolder) {
        val validChildren = setOf(TiBasicTokenTypes.PRINT_KEYWORD, TokenType.WHITE_SPACE, TiBasicNodeTypes.EXPRESSION)
        val expression = statement.firstChildOfType<TiBasicExpression>()
        val isNumericExpr = expression != null && !isStringExpression(expression)
        val isStringExpr = expression != null && isStringExpression(expression)
        statement.node.allChildren
            .filter { it.elementType !in validChildren }
            .forEach { child ->
                val message = when {
                    child.elementType == TiBasicTokenTypes.INVALID_VARIABLE_NAME -> "Bad variable name"
                    isNumericExpr && child.elementType in STRING_MISMATCH_TYPES -> "String-number mismatch"
                    isNumericExpr && child.elementType == TiBasicNodeTypes.VARIABLE_ACCESS &&
                            child.firstChildType == TiBasicTokenTypes.STRING_VARIABLE -> "String-number mismatch"

                    isStringExpr && child.elementType in NUMERIC_MISMATCH_TYPES -> "String-number mismatch"
                    else -> "PRINT argument must be an expression"
                }
                holder.error(message, child.textRange)
            }
    }

    private fun annotateLineNumberListStatement(statement: TiBasicLineNumberListStatement, holder: AnnotationHolder) {
        val children = statement.node.nonWhitespaceChildren
            .filter { it.elementType != TiBasicTokenTypes.LINE_NUMBER_LIST_KEYWORD }
        var expectNumber = true
        var trailingComma: ASTNode? = null
        for (child in children) {
            if (expectNumber) {
                when {
                    child.elementType == TiBasicTokenTypes.NUMERIC_LITERAL -> {
                        val value = child.text.toLongOrNull()
                        if (value == null || value.toInt() !in VALID_LINE_NUMBER_RANGE) {
                            holder.error("Line number must be between 1 and 32767", child.textRange)
                        }
                        expectNumber = false
                        trailingComma = null
                    }

                    else -> {
                        holder.error("Line number expected", child.textRange)
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
                        holder.error("Comma expected", child.textRange)
                        trailingComma = null
                    }
                }
            }
        }
        if (trailingComma != null) {
            holder.error("Line number expected", trailingComma.textRange)
        }
        annotateUndefinedLineNumbers(statement, children, holder)
    }

    private fun annotateUndefinedLineNumbers(
        statement: TiBasicLineNumberListStatement,
        children: List<ASTNode>,
        holder: AnnotationHolder,
    ) {
        val file = statement.containingTiBasicFile ?: return
        val definedLineNumbers =
            file.lines()
                .map { it.lineNumber() }
                .filter { it in VALID_LINE_NUMBER_RANGE }
                .toSet()
        children
            .filter { it.elementType == TiBasicTokenTypes.NUMERIC_LITERAL }
            .forEach { child ->
                val value = child.text.toLongOrNull() ?: return@forEach
                if (value.toInt() in VALID_LINE_NUMBER_RANGE && value.toInt() !in definedLineNumbers) {
                    holder.warning("Bad line number", child.textRange)
                }
            }
    }

    private fun annotateDeleteStatement(statement: TiBasicDeleteStatement, holder: AnnotationHolder) {
        val validChildren = setOf(TiBasicTokenTypes.DELETE_KEYWORD, TokenType.WHITE_SPACE, TiBasicNodeTypes.EXPRESSION)
        val expression = statement.firstChildOfType<TiBasicExpression>()
        val keywordNode = statement.node.firstChildOfType(TiBasicTokenTypes.DELETE_KEYWORD)!!
        if (expression == null) {
            holder.error("String expression expected", keywordNode.textRange)
            return
        }
        if (!isStringExpression(expression)) {
            holder.error("String expression expected", expression.textRange)
        }
        statement.node.allChildren
            .filter { it.elementType !in validChildren }
            .forEach { child ->
                holder.error("String expression expected", child.textRange)
            }
    }

    private fun annotateVariableAccess(varAccess: TiBasicVariableAccess, holder: AnnotationHolder) {
        if (!varAccess.hasSubscriptParens()) return
        val dimCount = varAccess.subscriptDimCount()
        if (dimCount == 0 || dimCount > 3) {
            holder.error("Bad subscript definition", varAccess)
        }
    }

    private fun annotateExpression(expr: TiBasicExpression, holder: AnnotationHolder) {
        val children = expr.node.nonWhitespaceChildren
        if (children.any { it.elementType in COMPARISON_OP_TYPES }) return
        val isString = isStringExpression(expr)
        children.forEach { child ->
            val isMismatch = if (isString) {
                child.elementType in NUMERIC_MISMATCH_TYPES ||
                        (child.elementType == TiBasicNodeTypes.VARIABLE_ACCESS &&
                                child.firstChildType == TiBasicTokenTypes.NUMERIC_VARIABLE)
            } else {
                child.elementType in STRING_MISMATCH_TYPES ||
                        (child.elementType == TiBasicNodeTypes.VARIABLE_ACCESS &&
                                child.firstChildType == TiBasicTokenTypes.STRING_VARIABLE)
            }
            if (isMismatch) {
                holder.error("String-number mismatch", child.textRange)
            }
        }
    }

    private fun isStringExpression(expr: TiBasicExpression): Boolean {
        val children = expr.node.nonWhitespaceChildren
        if (children.any { it.elementType in COMPARISON_OP_TYPES }) return false
        if (children.any { it.elementType == TiBasicTokenTypes.CONCAT_OP }) return true
        val first = children.firstOrNull() ?: return false
        if (first.elementType == TiBasicTokenTypes.STRING_LITERAL) return true
        if (first.elementType == TiBasicNodeTypes.VARIABLE_ACCESS &&
            first.firstChildType == TiBasicTokenTypes.STRING_VARIABLE
        ) return true
        if (first.elementType == TiBasicTokenTypes.LPAREN) {
            val inner = children.drop(1).dropLast(1)
            if (inner.isEmpty()) return false
            if (inner.any { it.elementType == TiBasicTokenTypes.CONCAT_OP }) return true
            val firstInner = inner.firstOrNull() ?: return false
            return firstInner.elementType == TiBasicTokenTypes.STRING_LITERAL ||
                    (firstInner.elementType == TiBasicNodeTypes.VARIABLE_ACCESS &&
                            firstInner.firstChildType == TiBasicTokenTypes.STRING_VARIABLE)
        }
        return false
    }

    private fun annotateLineNumber(line: TiBasicLine, holder: AnnotationHolder) {
        val lineNumberNode = line.node.firstChildNode
        val lineNumber = lineNumberNode.text.toLongOrNull()
        if (lineNumber == null || lineNumber.toInt() !in VALID_LINE_NUMBER_RANGE) {
            holder.error("Bad line number", lineNumberNode.textRange)
        }
    }

    private fun annotateVariableNameConflicts(file: TiBasicFile, holder: AnnotationHolder) {
        val allAccesses = collectVariableAccesses(file)
        val stringAccesses = allAccesses.filter { it.firstChildType == TiBasicTokenTypes.STRING_VARIABLE }
        val numericAccesses = allAccesses.filter { it.firstChildType == TiBasicTokenTypes.NUMERIC_VARIABLE }
        annotateNameConflicts(stringAccesses, holder, ::variableAccessName, ::variableAccessDimCount)
        annotateNameConflicts(numericAccesses, holder, ::variableAccessName, ::variableAccessDimCount)
        val commands = COMMANDS_UPPERCASE
        val keywords = KEYWORDS_UPPERCASE
        allAccesses.forEach { node ->
            val name = variableAccessName(node)
            val message = when (name) {
                in commands -> "Command must not be used as variable name"
                in keywords -> "Keyword cannot be used as variable name"
                else -> null
            }
            if (message != null) {
                holder.error(message, node.textRange)
            }
        }
    }

    private fun collectVariableAccesses(file: TiBasicFile): List<ASTNode> {
        val result = mutableListOf<ASTNode>()
        fun traverse(node: ASTNode) {
            if (node.elementType == TiBasicNodeTypes.VARIABLE_ACCESS) result.add(node)
            for (child in node.allChildren) traverse(child)
        }
        file.lines()
            .flatMap { it.children.toList() }
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
                    holder.error("Name conflict", node.textRange)
                }
            }
        }
    }

    private fun variableAccessName(node: ASTNode): String = node.firstChildNode.text.uppercase()

    private fun variableAccessDimCount(node: ASTNode): Int =
        node.childrenOfType(TiBasicNodeTypes.EXPRESSION).size

    private fun duplicateLineNumbers(lines: List<TiBasicLine>): Set<TiBasicLine> {
        val seen = mutableSetOf<Int>()
        return lines.filter { it.lineNumber() in VALID_LINE_NUMBER_RANGE && !seen.add(it.lineNumber()) }.toSet()
    }

    private fun annotateDuplicateLineNumbers(duplicates: Set<TiBasicLine>, holder: AnnotationHolder) {
        duplicates.forEach { line ->
            holder.error("Duplicate line number ${line.lineNumber()}", line, ResequenceQuickFix(line))
        }
    }

    private fun annotateNonAscendingLineNumbers(lines: List<TiBasicLine>, duplicates: Set<TiBasicLine>, holder: AnnotationHolder) {
        lines.filter { it.lineNumber() in VALID_LINE_NUMBER_RANGE }.zipWithNext().forEach { (previous, current) ->
            if (current in duplicates) return@forEach
            if (current.lineNumber() <= previous.lineNumber()) {
                holder.warning(
                    "Line number ${current.lineNumber()} does not follow ascending order (previous: ${previous.lineNumber()})",
                    current,
                    ResequenceQuickFix(current),
                )
            }
        }
    }

}

private val COMMANDS_UPPERCASE = TiBasicKeywords.getCommands().map { it.uppercase() }.toSet()

private val KEYWORDS_UPPERCASE = TiBasicKeywords.getKeywords().map { it.uppercase() }.toSet()

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
)
