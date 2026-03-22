package com.github.mmrsic.idea.plugins.tibasic.highlight

import com.github.mmrsic.idea.plugins.tibasic.action.resequence.ResequenceQuickFix
import com.github.mmrsic.idea.plugins.tibasic.ext.*
import com.github.mmrsic.idea.plugins.tibasic.lang.*
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes
import com.github.mmrsic.idea.plugins.tibasic.psi.*
import com.intellij.lang.ASTNode
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil

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
                annotateDefDuplicatesAndSelfReference(element, holder)
                annotateDimDuplicatesAndBeforeUse(element, holder)
                annotateOptionBasePlacement(element, holder)
            }

            is TiBasicLine -> annotateLineNumber(element, holder)
            is TiBasicLetStatement -> annotateLetStatement(element, holder)
            is TiBasicDefStatement -> annotateDefStatement(element, holder)
            is TiBasicDimStatement -> annotateDimStatement(element, holder)
            is TiBasicOptionBaseStatement -> annotateOptionBaseStatement(element, holder)
            is TiBasicScreenPrintStatement -> annotateInvalidPrintArgument(element, holder)
            is TiBasicTabFunction -> annotateTabFunction(element, holder)
            is TiBasicLineNumberListStatement -> annotateLineNumberListStatement(element, holder)
            is TiBasicDeleteStatement -> annotateDeleteStatement(element, holder)
            is TiBasicGotoStatement -> annotateGotoStatement(element, holder)
            is TiBasicGosubStatement -> annotateGosubStatement(element, holder)
            is TiBasicReturnStatement -> annotateTrailingContent(element.node, "RETURN", holder)
            is TiBasicOnGotoStatement -> annotateOnGotoStatement(element, holder)
            is TiBasicOnGosubStatement -> annotateOnGosubStatement(element, holder)
            is TiBasicIfStatement -> annotateIfStatement(element, holder)
            is TiBasicForStatement -> annotateForStatement(element, holder)
            is TiBasicNextStatement -> annotateNextStatement(element, holder)
            is TiBasicEndStatement -> annotateTrailingContent(element.node, "END", holder)
            is TiBasicStopStatement -> annotateTrailingContent(element.node, "STOP", holder)
            is TiBasicRandomizeStatement -> annotateRandomizeStatement(element, holder)
            is TiBasicInputStatement -> annotateInputStatement(element, holder)
            is TiBasicReadStatement -> annotateReadStatement(element, holder)
            is TiBasicDataStatement -> annotateDataStatement(element, holder)
            is TiBasicRestoreStatement -> annotateRestoreStatement(element, holder)
            is TiBasicCallStatement -> annotateCallStatement(element, holder)
            is TiBasicFunctionCall -> annotateFunctionCall(element, holder)
            is TiBasicOpenStatement -> annotateOpenStatement(element, holder)
            is TiBasicCloseStatement -> annotateCloseStatement(element, holder)
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
        val statementKeywords = setOf(
            TiBasicTokenTypes.END_KEYWORD,
            TiBasicTokenTypes.STOP_KEYWORD,
            TiBasicTokenTypes.RETURN_KEYWORD,
        )
        val trailing =
            stmtNode.allChildren
                .dropWhile { it.elementType in statementKeywords }
                .filter { it.elementType != TokenType.WHITE_SPACE }
        if (trailing.isEmpty()) return
        val range = TextRange(trailing.first().startOffset, trailing.last().startOffset + trailing.last().textLength)
        holder.warning("Everything after $stmtName statement is ignored", range)
    }

    private fun annotateRandomizeStatement(statement: TiBasicRandomizeStatement, holder: AnnotationHolder) {
        val trailing = statement.node
            .childrenAfter(TiBasicTokenTypes.RANDOMIZE_KEYWORD)
            .filter { it.elementType != TokenType.WHITE_SPACE && it.elementType != TiBasicNodeTypes.EXPRESSION }
        if (trailing.isNotEmpty()) {
            holder.error(INCORRECT_STATEMENT_RUNTIME_ERROR, statement)
            return
        }
        val exprNode = statement.node.childrenOfType(TiBasicNodeTypes.EXPRESSION).firstOrNull() ?: return
        val expr = exprNode.psi as? TiBasicExpression ?: return
        if (isStringExpression(expr)) {
            holder.error(INCORRECT_STATEMENT_RUNTIME_ERROR, statement)
        }
    }

    private fun annotateDefStatement(statement: TiBasicDefStatement, holder: AnnotationHolder) {
        // Check 1: missing function name
        val funcNameNode = statement.functionNameNode()
        if (funcNameNode == null) {
            holder.error(INCORRECT_STATEMENT_RUNTIME_ERROR, statement)
            return
        }
        // Check 2: invalid function name
        if (funcNameNode.elementType == TiBasicTokenTypes.INVALID_VARIABLE_NAME) {
            holder.error("Bad variable name", funcNameNode.textRange)
            return
        }
        // Check 6: invalid parameter name
        val paramNode = statement.parameterNode()
        if (paramNode != null && paramNode.elementType == TiBasicTokenTypes.INVALID_VARIABLE_NAME) {
            holder.error("Bad variable name", paramNode.textRange)
        }
        // Check 3: missing =
        val hasEq = statement.node.nonWhitespaceChildren.any { it.elementType == TiBasicTokenTypes.EQ_OP }
        if (!hasEq) {
            holder.error(INCORRECT_STATEMENT_RUNTIME_ERROR, statement)
            return
        }
        // Check 4: missing body expression
        val bodyExpr = statement.bodyExpression()
        if (bodyExpr == null) {
            holder.error(INCORRECT_STATEMENT_RUNTIME_ERROR, statement)
            return
        }
        // Check 5: type mismatch between function name and body expression
        val isFuncNameString = funcNameNode.elementType == TiBasicTokenTypes.STRING_VARIABLE
        val isBodyString = isStringExpression(bodyExpr)
        if (isFuncNameString != isBodyString) {
            holder.error("String-number mismatch", statement)
            return
        }
        // Check 7: parameter used as array (with subscripts) in body
        if (paramNode != null) {
            val paramName = paramNode.text.uppercase()
            PsiTreeUtil.findChildrenOfType(bodyExpr, TiBasicVariableAccess::class.java)
                .filter { it.node.firstChildNode?.text?.uppercase() == paramName && it.subscriptDimCount() > 0 }
                .forEach { holder.error(INCORRECT_STATEMENT_RUNTIME_ERROR, it) }
        }
    }

    private fun annotateDefDuplicatesAndSelfReference(file: TiBasicFile, holder: AnnotationHolder) {
        val defs = file.defStatements()
        // Check 8: duplicate function names
        defs.groupBy { it.functionName() ?: "" }
            .values
            .filter { it.size > 1 }
            .forEach { duplicates ->
                duplicates.forEach { stmt ->
                    holder.warning("Duplicate DEF for function name ${stmt.functionName()}", stmt)
                }
            }
        // Check 9: direct self-reference in body expression
        defs.forEach { stmt ->
            val funcName = stmt.functionName()?.uppercase() ?: return@forEach
            val body = stmt.bodyExpression() ?: return@forEach
            PsiTreeUtil.findChildrenOfType(body, TiBasicVariableAccess::class.java)
                .filter { it.node.firstChildNode?.text?.uppercase() == funcName }
                .forEach { holder.warning("DEF function may not reference itself", it) }
        }
    }

    private fun annotateDimStatement(statement: TiBasicDimStatement, holder: AnnotationHolder) {
        val varAccesses = statement.dimVariableAccesses()
        if (varAccesses.isEmpty()) {
            holder.error(INCORRECT_STATEMENT_RUNTIME_ERROR, statement)
            return
        }
        val lastContentNode = statement.node.nonWhitespaceChildren.lastOrNull { it.elementType != TiBasicTokenTypes.DIM_KEYWORD }
        if (lastContentNode?.elementType == TiBasicTokenTypes.COMMA) {
            holder.error(INCORRECT_STATEMENT_RUNTIME_ERROR, lastContentNode.textRange)
            return
        }
        for (varAccess in varAccesses) {
            when {
                varAccess.node.firstChildType == TiBasicTokenTypes.INVALID_VARIABLE_NAME ->
                    holder.error("Bad variable name", varAccess)

                !varAccess.hasSubscriptParens() ->
                    holder.error(INCORRECT_STATEMENT_RUNTIME_ERROR, varAccess)

                !varAccess.hasClosingSubscriptParen() ->
                    holder.error(INCORRECT_STATEMENT_RUNTIME_ERROR, varAccess)

                else -> annotateDimSubscripts(varAccess, holder)
            }
        }
    }

    private fun annotateDimSubscripts(varAccess: TiBasicVariableAccess, holder: AnnotationHolder) {
        val dimCount = varAccess.subscriptDimCount()
        if (dimCount == 0 || dimCount > 3) return
        varAccess.node.childrenOfType(TiBasicNodeTypes.EXPRESSION).forEach { subscriptNode ->
            val expr = subscriptNode.psi as? TiBasicExpression ?: return@forEach
            val error = dimSubscriptError(expr) ?: return@forEach
            holder.error(error, subscriptNode.textRange)
        }
    }

    private fun dimSubscriptError(expr: TiBasicExpression): String? {
        val children = expr.node.nonWhitespaceChildren
        if (children.isEmpty()) return INCORRECT_STATEMENT_RUNTIME_ERROR
        if (children.any { it.elementType == TiBasicNodeTypes.VARIABLE_ACCESS }) {
            return "Variable not allowed as DIM dimension"
        }
        if (children.size == 1 && children[0].elementType == TiBasicTokenTypes.NUMERIC_LITERAL) {
            val text = children[0].text
            return if (text.contains('.') || text.contains('E', ignoreCase = true)) {
                "Float not allowed as DIM dimension"
            } else {
                null
            }
        }
        return "Integer expected as DIM dimension"
    }

    private fun annotateOptionBaseStatement(statement: TiBasicOptionBaseStatement, holder: AnnotationHolder) {
        val contentNodes = statement.node.nonWhitespaceChildren
            .filter { it.elementType != TiBasicTokenTypes.OPTION_BASE_KEYWORD }
        if (contentNodes.isEmpty() || contentNodes.size > 1) {
            holder.error(INCORRECT_STATEMENT_RUNTIME_ERROR, statement)
            return
        }
        val valueNode = contentNodes[0]
        when (valueNode.elementType) {
            TiBasicTokenTypes.NUMERIC_VARIABLE,
            TiBasicTokenTypes.STRING_VARIABLE,
            TiBasicTokenTypes.INVALID_VARIABLE_NAME ->
                holder.error("Variable not allowed as OPTION BASE value", valueNode.textRange)

            TiBasicTokenTypes.NUMERIC_LITERAL -> {
                val text = valueNode.text
                when {
                    text.contains('.') || text.contains('E', ignoreCase = true) ->
                        holder.error("Float not allowed as OPTION BASE value", valueNode.textRange)

                    text.toIntOrNull().let { it != 0 && it != 1 } ->
                        holder.error("OPTION BASE value must be 0 or 1", valueNode.textRange)
                }
            }

            else -> holder.error(INCORRECT_STATEMENT_RUNTIME_ERROR, statement)
        }
    }

    private fun annotateDimDuplicatesAndBeforeUse(file: TiBasicFile, holder: AnnotationHolder) {
        val dimsByArrayName = mutableMapOf<String, MutableList<TiBasicDimStatement>>()
        for (stmt in file.dimStatements()) {
            for (varAccess in stmt.dimVariableAccesses()) {
                val name = varAccess.node.firstChildNode?.text?.uppercase() ?: continue
                dimsByArrayName.getOrPut(name) { mutableListOf() }.add(stmt)
            }
        }
        for ((name, stmts) in dimsByArrayName) {
            if (stmts.size > 1) {
                stmts.distinct().forEach { stmt -> holder.error("Duplicate DIM for array name $name", stmt) }
            }
        }
        for ((name, stmts) in dimsByArrayName) {
            val dimLineNumber = PsiTreeUtil.getParentOfType(stmts.first(), TiBasicLine::class.java)
                ?.lineNumber() ?: continue
            val firstUseLineNumber = file.variableAccesses()
                .filter { varAccess ->
                    varAccess.hasSubscriptParens() &&
                            PsiTreeUtil.getParentOfType(varAccess, TiBasicDimStatement::class.java) == null &&
                            varAccess.node.firstChildNode?.text?.uppercase() == name
                }
                .mapNotNull { PsiTreeUtil.getParentOfType(it, TiBasicLine::class.java)?.lineNumber() }
                .minOrNull() ?: continue
            if (dimLineNumber > firstUseLineNumber) {
                stmts.distinct().forEach { stmt ->
                    holder.warning("DIM for $name must appear before first use at line $firstUseLineNumber", stmt)
                }
            }
        }
    }

    private fun annotateOptionBasePlacement(file: TiBasicFile, holder: AnnotationHolder) {
        val optionBaseStmts = file.optionBaseStatements()
        if (optionBaseStmts.size > 1) {
            optionBaseStmts.forEach { stmt -> holder.error("Duplicate OPTION BASE", stmt) }
            return
        }
        val optionBaseStmt = optionBaseStmts.singleOrNull() ?: return
        val optionBaseLine = PsiTreeUtil.getParentOfType(optionBaseStmt, TiBasicLine::class.java)
            ?.lineNumber() ?: return
        val earliestDimLine = file.dimStatements()
            .mapNotNull { PsiTreeUtil.getParentOfType(it, TiBasicLine::class.java)?.lineNumber() }
            .minOrNull()
        if (earliestDimLine != null && earliestDimLine < optionBaseLine) {
            holder.warning("OPTION BASE must appear before DIM at line $earliestDimLine", optionBaseStmt)
        }
        val earliestArrayUseLine = file.variableAccesses()
            .filter { varAccess ->
                varAccess.hasSubscriptParens() &&
                        PsiTreeUtil.getParentOfType(varAccess, TiBasicDimStatement::class.java) == null
            }
            .mapNotNull { PsiTreeUtil.getParentOfType(it, TiBasicLine::class.java)?.lineNumber() }
            .minOrNull()
        if (earliestArrayUseLine != null && earliestArrayUseLine < optionBaseLine) {
            holder.warning(
                "OPTION BASE must appear before first array use at line $earliestArrayUseLine",
                optionBaseStmt,
            )
        }
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
        annotateJumpStatement(statement, TiBasicTokenTypes.GOTO_KEYWORD, holder)
    }

    private fun annotateGosubStatement(statement: TiBasicGosubStatement, holder: AnnotationHolder) {
        annotateJumpStatement(statement, TiBasicTokenTypes.GOSUB_KEYWORD, holder)
    }

    private fun annotateJumpStatement(statement: PsiElement, jumpKeyword: IElementType, holder: AnnotationHolder) {
        val contentNodes = statement.node.nonWhitespaceChildren.filter { it.elementType != jumpKeyword }
        if (contentNodes.size != 1 || contentNodes[0].elementType != TiBasicTokenTypes.NUMERIC_LITERAL) {
            holder.error("Incorrect statement", statement)
            return
        }
        val lineNumberNode = contentNodes[0]
        val lineNumber = lineNumberNode.text.toLongOrNull()?.toInt()
        val definedLineNumbers = statement.containingTiBasicFile
            ?.lines()
            ?.map { it.lineNumber() }
            ?.filter { it in VALID_LINE_NUMBER_RANGE }
            ?.toSet()
        validateLineNumberExists(lineNumberNode, lineNumber, definedLineNumbers, holder)
    }

    private fun annotateOnGotoStatement(statement: TiBasicOnGotoStatement, holder: AnnotationHolder) {
        annotateOnBranchStatement(statement, TiBasicTokenTypes.GOTO_KEYWORD, holder)
    }

    private fun annotateOnGosubStatement(statement: TiBasicOnGosubStatement, holder: AnnotationHolder) {
        annotateOnBranchStatement(statement, TiBasicTokenTypes.GOSUB_KEYWORD, holder)
    }

    private fun annotateOnBranchStatement(statement: PsiElement, branchKeyword: IElementType, holder: AnnotationHolder) {
        val children = statement.node.nonWhitespaceChildren
        val expression = statement.firstChildOfType<TiBasicExpression>()
        val branchKeywordNode = children.firstOrNull { it.elementType == branchKeyword }

        if (expression == null || branchKeywordNode == null) {
            holder.error("Incorrect statement", statement)
            return
        }

        if (isStringExpression(expression)) {
            holder.error("String-number mismatch", expression)
        }

        val afterBranch = children
            .dropWhile { it.elementType != branchKeyword }
            .drop(1)

        if (afterBranch.isEmpty() || afterBranch[0].elementType != TiBasicTokenTypes.NUMERIC_LITERAL) {
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
        for (child in afterBranch) {
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
        val funcKeywordNode = statement.node.nonWhitespaceChildren.firstOrNull {
            it.elementType == TiBasicTokenTypes.NUMERIC_FUNCTION_KEYWORD ||
                    it.elementType == TiBasicTokenTypes.STRING_FUNCTION_KEYWORD
        }
        if (funcKeywordNode != null) {
            holder.error("Function name cannot be used as variable", funcKeywordNode.textRange)
            return
        }
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
        val openParens = children.count { it.elementType == TiBasicTokenTypes.LPAREN }
        val closeParens = children.count { it.elementType == TiBasicTokenTypes.RPAREN }
        if (openParens > closeParens) {
            holder.error(INCORRECT_STATEMENT_RUNTIME_ERROR, expr)
            return
        }
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

    private fun annotateCallStatement(statement: TiBasicCallStatement, holder: AnnotationHolder) {
        val name = statement.subprogramName()
        val subprogram = TiBasicCallSubprograms.byName(name)
        if (subprogram == null) {
            val nameNode = statement.node.firstChildOfType(TiBasicTokenTypes.CALL_SUBPROGRAM_NAME)
            if (nameNode != null) {
                holder.error("Unknown subprogram: $name", nameNode.textRange)
            } else {
                holder.error("Incorrect statement", statement)
            }
            return
        }
        val argNodes = statement.node.childrenOfType(TiBasicNodeTypes.EXPRESSION)
        val argCount = argNodes.size
        if (argNodes.any { it.firstChildNode == null }) {
            holder.error(subprogram.syntaxViolationError ?: INCORRECT_STATEMENT_RUNTIME_ERROR, statement)
            return
        }
        if (argCount !in subprogram.validArgCounts) {
            holder.error(
                subprogram.syntaxViolationError
                    ?: "Wrong number of arguments for $name: expected ${subprogram.validArgCounts.joinToString(" or ")}, got $argCount",
                statement,
            )
            return
        }
        if (name == "CLEAR") {
            val trailingNodes = statement.node
                .childrenAfter(TiBasicTokenTypes.CALL_SUBPROGRAM_NAME)
                .filter { it.elementType != TokenType.WHITE_SPACE }
            if (trailingNodes.isNotEmpty()) {
                val trailingRange = TextRange(
                    trailingNodes.first().startOffset,
                    trailingNodes.last().textRange.endOffset,
                )
                holder.error(BAD_NAME_RUNTIME_ERROR, trailingRange)
                return
            }
        }
        argNodes.forEachIndexed { index, argNode ->
            val expr = argNode.psi as? TiBasicExpression ?: return@forEachIndexed
            val expectedType = subprogram.argTypes[index % subprogram.argTypes.size]
            val isString = isStringExpression(expr)
            val mismatch = when (expectedType) {
                CallArgType.NUMERIC -> isString
                CallArgType.STRING -> !isString
            }
            if (mismatch) {
                if (subprogram.syntaxViolationError != null) {
                    holder.error(subprogram.syntaxViolationError, statement)
                    return
                }
                holder.warning("Type mismatch at argument ${index + 1} of $name", argNode.textRange)
            }
        }
    }

    private fun annotateFunctionCall(call: TiBasicFunctionCall, holder: AnnotationHolder) {
        val name = call.functionName()
        val signature = TiBasicBuiltInFunctions.byName(name)
        if (signature == null) {
            holder.error("Unknown built-in function: $name", call)
            return
        }
        val hasOpenParen = call.node.firstChildOfType(TiBasicTokenTypes.LPAREN) != null
        val hasCloseParen = call.node.firstChildOfType(TiBasicTokenTypes.RPAREN) != null
        if (hasOpenParen && !hasCloseParen) {
            holder.error(INCORRECT_STATEMENT_RUNTIME_ERROR, call)
            return
        }
        if (hasOpenParen && signature.argCount == 0) {
            holder.error(INCORRECT_STATEMENT_RUNTIME_ERROR, call)
            return
        }
        val args = call.arguments()
        if (args.size != signature.argCount) {
            holder.error(INCORRECT_STATEMENT_RUNTIME_ERROR, call)
            return
        }
        args.forEachIndexed { index, arg ->
            val expectedType = signature.argTypes[index]
            val isString = isStringExpression(arg)
            val mismatch = when (expectedType) {
                CallArgType.NUMERIC -> isString
                CallArgType.STRING -> !isString
            }
            if (mismatch) {
                holder.error(INCORRECT_STATEMENT_RUNTIME_ERROR, call)
                return
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
        if (first.elementType == TiBasicNodeTypes.FUNCTION_CALL &&
            first.firstChildType == TiBasicTokenTypes.STRING_FUNCTION_KEYWORD
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
        annotateDefNameUsageConflicts(file, allAccesses, holder)
    }

    private fun annotateDefNameUsageConflicts(
        file: TiBasicFile,
        allVarAccesses: List<ASTNode>,
        holder: AnnotationHolder,
    ) {
        val defSignaturesByName = buildDefSignaturesByName(file)
        allVarAccesses.forEach { varAccess ->
            val signature = defSignaturesByName[variableAccessName(varAccess)] ?: return@forEach
            val accessHasParens = varAccess.firstChildOfType(TiBasicTokenTypes.LPAREN) != null
            if (signature.hasParameter != accessHasParens) {
                holder.error("Name conflict with line ${signature.lineNumber}", varAccess.textRange)
            }
        }
    }

    private fun buildDefSignaturesByName(file: TiBasicFile): Map<String, DefSignature> =
        file.defStatements()
            .mapNotNull { def ->
                val name = def.functionName() ?: return@mapNotNull null
                val lineNumber = (def.parent as? TiBasicLine)?.lineNumber() ?: return@mapNotNull null
                name to DefSignature(lineNumber, def.parameterNode() != null)
            }
            .toMap()

    private fun collectVariableAccesses(file: TiBasicFile): List<ASTNode> =
        file.variableAccesses().map { it.node }

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

    private fun annotateOpenStatement(statement: TiBasicOpenStatement, holder: AnnotationHolder) {
        val hashNode = statement.node.firstChildOfType(TiBasicTokenTypes.HASH)
        if (hashNode == null) {
            holder.error(INCORRECT_STATEMENT_RUNTIME_ERROR, statement)
            return
        }
        val fileNumberExpr = statement.fileNumberExpr()
        if (fileNumberExpr == null) {
            holder.error(INCORRECT_STATEMENT_RUNTIME_ERROR, statement)
            return
        }
        if (isStringExpression(fileNumberExpr)) {
            holder.error("Numeric expression expected", fileNumberExpr)
        } else {
            annotateFileNumberRange(fileNumberExpr, holder)
        }
        val colonNode = statement.node.firstChildOfType(TiBasicTokenTypes.COLON)
        if (colonNode == null) {
            holder.error(INCORRECT_STATEMENT_RUNTIME_ERROR, statement)
            return
        }
        val fileNameExpr = statement.fileNameExpr()
        if (fileNameExpr == null) {
            holder.error(INCORRECT_STATEMENT_RUNTIME_ERROR, statement)
            return
        }
        if (!isStringExpression(fileNameExpr)) {
            holder.error("String expression expected", fileNameExpr)
        }
        annotateOpenOptions(statement, holder)
    }

    private fun annotateOpenOptions(statement: TiBasicOpenStatement, holder: AnnotationHolder) {
        val fileNameExpr = statement.fileNameExpr()
        if (fileNameExpr != null) {
            val trailingGarbage = statement.node.allChildren
                .dropWhile { it != fileNameExpr.node }
                .drop(1)
                .filter { it.elementType != TokenType.WHITE_SPACE && it.elementType != TiBasicNodeTypes.OPEN_OPTION }
            if (trailingGarbage.isNotEmpty()) {
                holder.error(INCORRECT_STATEMENT_RUNTIME_ERROR, statement)
                return
            }
        }
        val options = statement.options()
        val organizationKeywords = mutableListOf<IElementType>()
        val fileTypeKeywords = mutableListOf<IElementType>()
        val modeKeywords = mutableListOf<IElementType>()
        val recordFormatKeywords = mutableListOf<IElementType>()
        val lifetimeKeywords = mutableListOf<IElementType>()
        for (option in options) {
            val keywordType = option.optionKeywordType()
            when (keywordType) {
                null -> holder.error(INCORRECT_STATEMENT_RUNTIME_ERROR, option)
                in TiBasicTokenTypes.OPEN_ORGANIZATION_KEYWORDS -> {
                    organizationKeywords.add(keywordType)
                    val orgExpr = option.optionExpression()
                    if (orgExpr != null && isStringExpression(orgExpr)) {
                        holder.error("Numeric expression expected", orgExpr)
                    }
                }
                in TiBasicTokenTypes.OPEN_FILE_TYPE_KEYWORDS -> fileTypeKeywords.add(keywordType)
                in TiBasicTokenTypes.OPEN_MODE_KEYWORDS -> modeKeywords.add(keywordType)
                in TiBasicTokenTypes.OPEN_RECORD_FORMAT_KEYWORDS -> {
                    recordFormatKeywords.add(keywordType)
                    val lenExpr = option.optionExpression()
                    if (lenExpr != null && isStringExpression(lenExpr)) {
                        holder.error("Numeric expression expected", lenExpr)
                    }
                }
                in TiBasicTokenTypes.OPEN_LIFETIME_KEYWORDS -> lifetimeKeywords.add(keywordType)
                else -> holder.error(INCORRECT_STATEMENT_RUNTIME_ERROR, option)
            }
        }
        if (organizationKeywords.size > 1) {
            holder.error("Duplicate file organization option", statement)
        }
        if (fileTypeKeywords.size > 1) {
            holder.error("Duplicate file type option", statement)
        }
        if (modeKeywords.size > 1) {
            holder.error("Duplicate open mode option", statement)
        }
        if (recordFormatKeywords.size > 1) {
            holder.error("Duplicate record format option", statement)
        }
        if (lifetimeKeywords.size > 1) {
            holder.error("Duplicate lifetime option", statement)
        }
        val hasRelative = organizationKeywords.contains(TiBasicTokenTypes.RELATIVE_KEYWORD)
        val hasVariable = recordFormatKeywords.contains(TiBasicTokenTypes.VARIABLE_KEYWORD)
        if (hasRelative && hasVariable) {
            holder.error("RELATIVE files require fixed-length records", statement)
        }
    }

    private fun annotateCloseStatement(statement: TiBasicCloseStatement, holder: AnnotationHolder) {
        val hashNode = statement.node.firstChildOfType(TiBasicTokenTypes.HASH)
        if (hashNode == null) {
            holder.error(INCORRECT_STATEMENT_RUNTIME_ERROR, statement)
            return
        }
        val expressions = statement.node.childrenOfType(TiBasicNodeTypes.EXPRESSION)
        val fileNumberExpr = expressions.getOrNull(0)?.psi as? TiBasicExpression
        if (fileNumberExpr == null) {
            holder.error(INCORRECT_STATEMENT_RUNTIME_ERROR, statement)
            return
        }
        if (isStringExpression(fileNumberExpr)) {
            holder.error("Numeric expression expected", fileNumberExpr)
        } else {
            annotateFileNumberRange(fileNumberExpr, holder)
        }
        val colonNode = statement.node.firstChildOfType(TiBasicTokenTypes.COLON)
        val deleteNode = statement.node.firstChildOfType(TiBasicTokenTypes.DELETE_KEYWORD)
        val trailingGarbage = statement.node.childrenAfter(TiBasicTokenTypes.HASH)
            .filter {
                it.elementType != TokenType.WHITE_SPACE
                    && it.elementType != TiBasicNodeTypes.EXPRESSION
                    && it.elementType != TiBasicTokenTypes.COLON
                    && it.elementType != TiBasicTokenTypes.DELETE_KEYWORD
            }
        if (trailingGarbage.isNotEmpty() || (colonNode != null) != (deleteNode != null)) {
            holder.error(INCORRECT_STATEMENT_RUNTIME_ERROR, statement)
        }
    }

    private fun annotateFileNumberRange(fileNumberExpr: TiBasicExpression, holder: AnnotationHolder) {
        val children = fileNumberExpr.node.nonWhitespaceChildren
        if (children.size != 1 || children[0].elementType != TiBasicTokenTypes.NUMERIC_LITERAL) return
        val value = children[0].text.toDoubleOrNull()?.let { Math.round(it).toInt() } ?: return
        when {
            value == 0 -> holder.error("File number 0 is reserved for screen", fileNumberExpr)
            value !in FILE_NUMBER_RANGE -> holder.error("File number must be between 1 and 255", fileNumberExpr)
        }
    }

}

private data class DefSignature(val lineNumber: Int, val hasParameter: Boolean)

private val FILE_NUMBER_RANGE = 1..255

private val COMMANDS_UPPERCASE = TiBasicKeywords.getCommands().map { it.uppercase() }.toSet()

private val KEYWORDS_UPPERCASE = TiBasicKeywords.getKeywords()
    .flatMap { it.split(Regex("\\s+")) }
    .map { it.uppercase() }
    .toSet()

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
