package com.github.mmrsic.idea.plugins.tibasic.highlight

import com.github.mmrsic.idea.plugins.tibasic.action.resequence.ResequenceQuickFix
import com.github.mmrsic.idea.plugins.tibasic.ext.*
import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicKeywords
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes
import com.github.mmrsic.idea.plugins.tibasic.psi.*
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
                annotateForNextBalance(element, holder)
            }

            is TiBasicLine -> annotateLineNumber(element, holder)
            is TiBasicLetStatement -> annotateLetStatement(element, holder)
            is TiBasicScreenPrintStatement -> annotateInvalidPrintArgument(element, holder)
            is TiBasicTabFunction -> annotateTabFunction(element, holder)
            is TiBasicLineNumberListStatement -> annotateLineNumberListStatement(element, holder)
            is TiBasicDeleteStatement -> annotateDeleteStatement(element, holder)
            is TiBasicGotoStatement -> annotateGotoStatement(element, holder)
            is TiBasicOnGotoStatement -> annotateOnGotoStatement(element, holder)
            is TiBasicIfStatement -> annotateIfStatement(element, holder)
            is TiBasicForStatement -> annotateForStatement(element, holder)
            is TiBasicNextStatement -> annotateNextStatement(element, holder)
            is TiBasicEndStatement -> annotateTrailingContent(element.node, "END", holder)
            is TiBasicStopStatement -> annotateTrailingContent(element.node, "STOP", holder)
            is TiBasicInputStatement -> annotateInputStatement(element, holder)
            is TiBasicReadStatement -> annotateReadStatement(element, holder)
            is TiBasicDataStatement -> annotateDataStatement(element, holder)
            is TiBasicRestoreStatement -> annotateRestoreStatement(element, holder)
            is TiBasicUnknownStatement -> annotateUnknownStatement(element, holder)
            is TiBasicInvalidLine -> holder.error("Line number expected", element)
            is TiBasicVariableAccess -> annotateVariableAccess(element, holder)
            is TiBasicExpression -> annotateExpression(element, holder)
            else -> if (element.node.elementType == TiBasicTokenTypes.TAB_KEYWORD && element.parent !is TiBasicTabFunction) {
                holder.error("TAB is only valid in a PRINT or DISPLAY statement", element)
            }
        }
    }

    private fun annotateTrailingContent(stmtNode: ASTNode, stmtName: String, holder: AnnotationHolder) {
        val trailing =
            stmtNode.allChildren
                .dropWhile { it.elementType == TiBasicTokenTypes.END_KEYWORD || it.elementType == TiBasicTokenTypes.STOP_KEYWORD }
                .filter { it.elementType != TokenType.WHITE_SPACE }
        if (trailing.isEmpty()) return
        val range = TextRange(trailing.first().startOffset, trailing.last().startOffset + trailing.last().textLength)
        holder.warning("Everything after $stmtName statement is ignored", range)
    }

    private fun annotateInputStatement(statement: TiBasicInputStatement, holder: AnnotationHolder) {
        val children = statement.node.nonWhitespaceChildren
        val colonNode = children.firstOrNull { it.elementType == TiBasicTokenTypes.COLON }
        val expressionNode = children.firstOrNull { it.elementType == TiBasicNodeTypes.EXPRESSION }
        if (colonNode != null && expressionNode != null) {
            val promptExpr = expressionNode.psi as? TiBasicExpression
            if (promptExpr != null && !isStringExpression(promptExpr)) {
                holder.error("String expression expected as INPUT prompt", expressionNode.textRange)
            }
        }
        annotateVariableList(
            children.filter { it.elementType == TiBasicNodeTypes.VARIABLE_ACCESS },
            statement,
            holder,
        )
    }

    private fun annotateReadStatement(statement: TiBasicReadStatement, holder: AnnotationHolder) {
        annotateVariableList(
            statement.node.nonWhitespaceChildren.filter { it.elementType == TiBasicNodeTypes.VARIABLE_ACCESS },
            statement,
            holder,
        )
    }

    private fun annotateVariableList(varAccessNodes: List<ASTNode>, statement: PsiElement, holder: AnnotationHolder) {
        if (varAccessNodes.isEmpty()) {
            holder.error("Incorrect statement", statement)
            return
        }
        varAccessNodes.forEach { varNode ->
            if (varNode.firstChildType == TiBasicTokenTypes.INVALID_VARIABLE_NAME) {
                holder.error("Bad variable name", varNode.textRange)
            }
        }
    }

    private fun annotateDataStatement(statement: TiBasicDataStatement, holder: AnnotationHolder) {
        val dataItemTypes = setOf(
            TiBasicTokenTypes.STRING_LITERAL,
            TiBasicTokenTypes.NUMERIC_LITERAL,
            TiBasicTokenTypes.PRINT_ARGUMENT,
            TiBasicTokenTypes.COMMA,
        )
        val hasDataContent = statement.node.nonWhitespaceChildren
            .any { it.elementType in dataItemTypes }
        if (!hasDataContent) {
            holder.error("Incorrect statement", statement)
        }
    }

    private fun annotateRestoreStatement(statement: TiBasicRestoreStatement, holder: AnnotationHolder) {
        val contentNodes = statement.node.nonWhitespaceChildren
            .filter { it.elementType != TiBasicTokenTypes.RESTORE_KEYWORD }
        if (contentNodes.isEmpty()) return
        if (contentNodes.size != 1 || contentNodes[0].elementType != TiBasicTokenTypes.NUMERIC_LITERAL) {
            holder.error("Incorrect statement", statement)
            return
        }
        val lineNumberNode = contentNodes[0]
        val lineNumber = lineNumberNode.text.toLongOrNull()?.toInt()
        val definedLineNumbers = statement.containingTiBasicFile
            ?.lines()?.map { it.lineNumber() }?.filter { it in VALID_LINE_NUMBER_RANGE }?.toSet()
        validateLineNumberExists(lineNumberNode, lineNumber, definedLineNumbers, holder)
    }

    private fun annotateForStatement(statement: TiBasicForStatement, holder: AnnotationHolder) {
        val children = statement.node.nonWhitespaceChildren
        val varAccessNode = children.firstOrNull { it.elementType == TiBasicNodeTypes.VARIABLE_ACCESS }
        val eqNode = children.firstOrNull { it.elementType == TiBasicTokenTypes.EQ_OP }
        val toNode = children.firstOrNull { it.elementType == TiBasicTokenTypes.TO_KEYWORD }
        val expressions = children.filter { it.elementType == TiBasicNodeTypes.EXPRESSION }
        val stepNode = children.firstOrNull { it.elementType == TiBasicTokenTypes.STEP_KEYWORD }
        val expectedExprCount = if (stepNode != null) 3 else 2

        if (varAccessNode == null || eqNode == null || toNode == null || expressions.size < expectedExprCount) {
            holder.error("Incorrect statement", statement)
            return
        }

        when (varAccessNode.firstChildType) {
            TiBasicTokenTypes.INVALID_VARIABLE_NAME ->
                holder.error("Bad variable name", varAccessNode.textRange)

            TiBasicTokenTypes.STRING_VARIABLE ->
                holder.error("Numeric variable expected", varAccessNode.textRange)
        }

        expressions.forEach { exprNode ->
            val expr = exprNode.psi as? TiBasicExpression ?: return@forEach
            if (isStringExpression(expr)) {
                holder.error("String-number mismatch", exprNode.textRange)
            }
        }
    }

    private fun annotateNextStatement(statement: TiBasicNextStatement, holder: AnnotationHolder) {
        val varAccessNode = statement.node.nonWhitespaceChildren
            .firstOrNull { it.elementType == TiBasicNodeTypes.VARIABLE_ACCESS }

        if (varAccessNode == null) {
            holder.error("Incorrect statement", statement)
            return
        }

        when (varAccessNode.firstChildType) {
            TiBasicTokenTypes.INVALID_VARIABLE_NAME -> holder.error("Bad variable name", varAccessNode.textRange)
            TiBasicTokenTypes.STRING_VARIABLE -> holder.error("Numeric variable expected", varAccessNode.textRange)
        }
    }

    private fun annotateForNextBalance(file: TiBasicFile, holder: AnnotationHolder) {
        val forStatements = file.forStatements()
        val nextStatements = file.nextStatements()
        val nFor = forStatements.size
        val nNext = nextStatements.size
        if (nFor == nNext) return
        val message = "FOR-NEXT-ERROR: $nFor FOR statements and $nNext NEXT statements"
        if (nFor > nNext) {
            forStatements.takeLast(nFor - nNext).forEach { holder.warning(message, it) }
        } else {
            nextStatements.takeLast(nNext - nFor).forEach { holder.warning(message, it) }
        }
    }

    private fun annotateGotoStatement(statement: TiBasicGotoStatement, holder: AnnotationHolder) {
        val contentNodes = statement.node.nonWhitespaceChildren.filter { it.elementType != TiBasicTokenTypes.GOTO_KEYWORD }
        if (contentNodes.size != 1 || contentNodes[0].elementType != TiBasicTokenTypes.NUMERIC_LITERAL) {
            holder.error("Incorrect statement", statement)
            return
        }
        val lineNumberNode = contentNodes[0]
        val lineNumber = lineNumberNode.text.toLongOrNull()?.toInt()
        val definedLineNumbers =
            statement.containingTiBasicFile
                ?.lines()
                ?.map { it.lineNumber() }
                ?.filter { it in VALID_LINE_NUMBER_RANGE }
                ?.toSet()
        validateLineNumberExists(lineNumberNode, lineNumber, definedLineNumbers, holder)
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

    private fun annotateIfStatement(statement: TiBasicIfStatement, holder: AnnotationHolder) {
        val children = statement.node.nonWhitespaceChildren
        val expression = statement.firstChildOfType<TiBasicExpression>()

        if (expression == null || statement.node.firstChildOfType(TiBasicTokenTypes.THEN_KEYWORD) == null) {
            holder.error("Incorrect statement", statement)
            return
        }

        if (isStringExpression(expression)) {
            holder.error("String-number mismatch", expression)
        }

        val afterThen = children.dropWhile { it.elementType != TiBasicTokenTypes.THEN_KEYWORD }.drop(1)
        if (afterThen.isEmpty() || afterThen[0].elementType != TiBasicTokenTypes.NUMERIC_LITERAL) {
            holder.error("Incorrect statement", statement)
            return
        }

        val thenLineNumberNode = afterThen[0]
        val thenLineNumber = thenLineNumberNode.text.toLongOrNull()?.toInt()
        val elseIdx = afterThen.indexOfFirst { it.elementType == TiBasicTokenTypes.ELSE_KEYWORD }
        val definedLineNumbers = statement.containingTiBasicFile
            ?.lines()?.map { it.lineNumber() }?.filter { it in VALID_LINE_NUMBER_RANGE }?.toSet()

        if (elseIdx < 0) {
            if (afterThen.size > 1) {
                holder.error("Incorrect statement", statement)
            } else {
                validateLineNumberExists(thenLineNumberNode, thenLineNumber, definedLineNumbers, holder)
            }
            return
        }

        if (elseIdx != 1) {
            holder.error("Incorrect statement", statement)
            return
        }

        val afterElse = afterThen.drop(elseIdx + 1)
        if (afterElse.isEmpty() || afterElse[0].elementType != TiBasicTokenTypes.NUMERIC_LITERAL) {
            holder.error("Incorrect statement", statement)
            return
        }
        if (afterElse.size > 1) {
            holder.error("Incorrect statement", statement)
            return
        }

        val elseLineNumberNode = afterElse[0]
        val elseLineNumber = elseLineNumberNode.text.toLongOrNull()?.toInt()
        validateLineNumberExists(thenLineNumberNode, thenLineNumber, definedLineNumbers, holder)
        validateLineNumberExists(elseLineNumberNode, elseLineNumber, definedLineNumbers, holder)
    }

    private fun validateLineNumberExists(lineNumberNode: ASTNode, lineNumber: Int?, definedLineNumbers: Set<Int>?, holder: AnnotationHolder) {
        if (lineNumber == null || lineNumber !in VALID_LINE_NUMBER_RANGE) {
            holder.error("Bad line number", lineNumberNode.textRange)
            return
        }
        if (definedLineNumbers != null && lineNumber !in definedLineNumbers) {
            holder.warning("Bad line number", lineNumberNode.textRange)
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
        val allowedLetStatementTypes = setOf(
            TiBasicTokenTypes.LET_KEYWORD,
            TiBasicNodeTypes.VARIABLE_ACCESS,
            TiBasicTokenTypes.EQ_OP,
            TiBasicNodeTypes.EXPRESSION,
            TokenType.WHITE_SPACE,
        )
        if (statement.node.allChildren.any { it.elementType !in allowedLetStatementTypes }) {
            holder.error("Incorrect statement", statement)
            return
        }
        val isStringVar = varAccessNode.firstChildType == TiBasicTokenTypes.STRING_VARIABLE
        val isStringExpr = isStringExpression(expression)
        if (isStringVar != isStringExpr) {
            val mismatchRange = TextRange(varAccessNode.textRange.startOffset, expression.textRange.endOffset)
            holder.error("String-number mismatch", mismatchRange)
        }
    }

    private fun annotateInvalidPrintArgument(statement: TiBasicScreenPrintStatement, holder: AnnotationHolder) {
        var previousWasExpression = false
        for (child in statement.node.allChildren) {
            when (child.elementType) {
                TiBasicNodeTypes.EXPRESSION -> {
                    if (previousWasExpression) {
                        holder.error("Separator expected between expressions", child.textRange)
                    }
                    previousWasExpression = true
                }

                TiBasicNodeTypes.TAB_FUNCTION -> {
                    if (previousWasExpression) {
                        holder.error("Separator expected between expressions", child.textRange)
                    }
                    previousWasExpression = true
                }

                in TiBasicTokenTypes.PRINT_SEPARATORS -> previousWasExpression = false

                TokenType.WHITE_SPACE -> { /* whitespace does not break the chain */
                }

                TiBasicTokenTypes.PRINT_KEYWORD,
                TiBasicTokenTypes.DISPLAY_KEYWORD -> { /* skip */
                }

                else -> {
                    previousWasExpression = false // invalid token breaks the consecutive-expression chain
                    val expression = statement.firstChildOfType<TiBasicExpression>()
                    val isNumericExpr = expression != null && !isStringExpression(expression)
                    val isStringExpr = expression != null && isStringExpression(expression)
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
        }
    }

    private fun annotateTabFunction(tabFunction: TiBasicTabFunction, holder: AnnotationHolder) {
        val hasLParen = tabFunction.node.firstChildOfType(TiBasicTokenTypes.LPAREN) != null
        if (!hasLParen) {
            holder.error("TAB requires a numeric argument in parentheses", tabFunction)
            return
        }
        val hasExpression = tabFunction.node.firstChildOfType(TiBasicNodeTypes.EXPRESSION) != null
        if (!hasExpression) {
            holder.error("TAB requires a numeric argument", tabFunction)
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
            for (child in node.childSequence) traverse(child)
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
