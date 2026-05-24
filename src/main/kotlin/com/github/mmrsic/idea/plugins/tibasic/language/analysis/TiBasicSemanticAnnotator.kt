package com.github.mmrsic.idea.plugins.tibasic.language.analysis

import com.github.mmrsic.idea.plugins.tibasic.ide.actions.resequence.ResequenceQuickFix
import com.github.mmrsic.idea.plugins.tibasic.common.ext.allChildren
import com.github.mmrsic.idea.plugins.tibasic.common.ext.childrenAfter
import com.github.mmrsic.idea.plugins.tibasic.common.ext.childrenOfType
import com.github.mmrsic.idea.plugins.tibasic.common.ext.error
import com.github.mmrsic.idea.plugins.tibasic.common.ext.firstChildOfType
import com.github.mmrsic.idea.plugins.tibasic.common.ext.firstChildType
import com.github.mmrsic.idea.plugins.tibasic.common.ext.nonWhitespaceChildren
import com.github.mmrsic.idea.plugins.tibasic.common.ext.warning
import com.github.mmrsic.idea.plugins.tibasic.language.model.BAD_NAME_RUNTIME_ERROR
import com.github.mmrsic.idea.plugins.tibasic.language.model.BAD_VALUE_RUNTIME_ERROR
import com.github.mmrsic.idea.plugins.tibasic.language.model.CallArgType
import com.github.mmrsic.idea.plugins.tibasic.language.model.INCORRECT_STATEMENT_RUNTIME_ERROR
import com.github.mmrsic.idea.plugins.tibasic.language.model.TiBasicBuiltInFunctions
import com.github.mmrsic.idea.plugins.tibasic.language.model.TiBasicCallSubprograms
import com.github.mmrsic.idea.plugins.tibasic.language.model.TiBasicKeywords
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.parser.TiBasicNodeTypes
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.common.VALID_LINE_NUMBER_RANGE
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.containingTiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.contracts.TiBasicFileNumberStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.contracts.TiBasicRecordNumberStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicCallStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicExpression
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicFunctionCall
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicTabFunction
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicVariableAccess
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicCloseStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicDefStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicDeleteStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicDimStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicEndStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicForStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicGosubStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicGotoStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicIfStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicInputStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicInvalidLine
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicLetStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicLine
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicLineNumberListStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicNextStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicOnGosubStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicOnGotoStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicOpenStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicOptionBaseStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicPrintStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicRandomizeStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicReadStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicRestoreStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicReturnStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicScreenPrintStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicStopStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicUnknownStatement
import com.intellij.lang.ASTNode
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import kotlin.math.roundToInt

open class TiBasicSemanticAnnotator : Annotator {

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
        val trailingCommaNode = trailingVariableListComma(statement, TiBasicTokenTypes.DIM_KEYWORD)
        if (trailingCommaNode != null) {
            holder.error(INCORRECT_STATEMENT_RUNTIME_ERROR, trailingCommaNode.textRange)
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
        if (statement.isFileInput()) {
            annotateFileInputStatement(statement, holder)
        } else {
            annotateScreenInputStatement(statement, holder)
        }
    }

    private fun annotateScreenInputStatement(statement: TiBasicInputStatement, holder: AnnotationHolder) {
        val children = statement.node.nonWhitespaceChildren
        if (children.any { it.elementType == TiBasicTokenTypes.PRINT_ARGUMENT }) {
            holder.error("Incorrect statement", statement)
            return
        }
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

    private fun annotateFileInputStatement(statement: TiBasicInputStatement, holder: AnnotationHolder) {
        val hashNode = statement.node.firstChildOfType(TiBasicTokenTypes.HASH)
        if (hashNode == null) {
            holder.error("Incorrect statement", statement)
            return
        }
        if (annotateFileNumberExpression(statement, holder)) return
        val recNode = statement.recKeywordNode()
        if (statement.node.firstChildOfType(TiBasicTokenTypes.DOT) != null && recNode == null) {
            holder.error("Incorrect statement", statement)
            return
        }
        if (recNode != null) {
            val recordNumberExpr = statement.recordNumberExpr()
            if (recordNumberExpr == null) {
                holder.error("Incorrect statement", statement)
                return
            }
            if (isStringExpression(recordNumberExpr)) {
                holder.error("Numeric expression expected", recordNumberExpr)
            }
        }
        val colonNode = statement.node.firstChildOfType(TiBasicTokenTypes.COLON)
        if (colonNode == null) {
            holder.error("Incorrect statement", statement)
            return
        }
        annotateVariableList(
            statement.inputVariableAccesses().map { it.node },
            statement,
            holder,
        )
    }

    private fun annotateFileNumberExpression(
        statement: TiBasicFileNumberStatement,
        holder: AnnotationHolder
    ): Boolean {
        val fileNumberExpr = statement.fileNumberExpr()
        if (fileNumberExpr == null) {
            holder.error("Incorrect statement", statement)
            return true
        }
        if (isStringExpression(fileNumberExpr)) {
            holder.error("Numeric expression expected", fileNumberExpr)
        } else {
            annotateFileNumberRange(fileNumberExpr, holder)
        }
        return false
    }

    private fun annotateReadStatement(statement: TiBasicReadStatement, holder: AnnotationHolder) {
        val trailingCommaNode = trailingVariableListComma(statement, TiBasicTokenTypes.READ_KEYWORD)
        if (trailingCommaNode != null) {
            holder.error("Incorrect statement", trailingCommaNode.textRange)
            return
        }
        annotateVariableList(
            statement.node.nonWhitespaceChildren.filter { it.elementType == TiBasicNodeTypes.VARIABLE_ACCESS },
            statement,
            holder,
        )
    }

    private fun trailingVariableListComma(statement: PsiElement, keywordType: IElementType): ASTNode? =
        statement.node.nonWhitespaceChildren
            .lastOrNull { it.elementType != keywordType }
            ?.takeIf { it.elementType == TiBasicTokenTypes.COMMA }

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

    private fun annotateRestoreStatement(statement: TiBasicRestoreStatement, holder: AnnotationHolder) {
        if (statement.isFileRestore()) {
            annotateFileRestoreStatement(statement, holder)
        } else {
            val contentNodes = statement.node.nonWhitespaceChildren
                .filter { it.elementType != TiBasicTokenTypes.RESTORE_KEYWORD }
            if (contentNodes.isEmpty()) return
            annotateLineNumber(contentNodes, holder, statement)
        }
    }

    private fun annotateFileRestoreStatement(statement: TiBasicRestoreStatement, holder: AnnotationHolder) {
        val fileNumberExpr = statement.fileNumberExpr()
        if (fileNumberExpr == null) {
            holder.error("Incorrect statement", statement)
            return
        }
        if (isStringExpression(fileNumberExpr)) {
            holder.error("Numeric expression expected", fileNumberExpr)
        } else {
            annotateFileNumberRange(fileNumberExpr, holder)
        }
        if (annotateRecNode(statement, statement.node.firstChildOfType(TiBasicTokenTypes.COMMA), holder)) return
        val validChildTypes = setOf(
            TiBasicTokenTypes.RESTORE_KEYWORD,
            TokenType.WHITE_SPACE,
            TiBasicTokenTypes.HASH,
            TiBasicNodeTypes.EXPRESSION,
            TiBasicTokenTypes.COMMA,
            TiBasicTokenTypes.REC_KEYWORD,
        )
        if (statement.node.allChildren.any { it.elementType !in validChildTypes }) {
            holder.error("Incorrect statement", statement)
        }
    }

    private fun annotateLineNumber(contentNodes: List<ASTNode>, holder: AnnotationHolder, statement: PsiElement) {
        val lineNumberNode = extractSingleLineNumberNode(contentNodes)
        if (lineNumberNode == null) {
            holder.error("Incorrect statement", statement)
            return
        }
        val lineNumber = lineNumberNode.text.toLongOrNull()?.toInt()
        val definedLineNumbers = statement.containingTiBasicFile
            ?.lines()?.map { it.lineNumber() }?.filter { it in VALID_LINE_NUMBER_RANGE }?.toSet()
        validateLineNumberExists(lineNumberNode, lineNumber, definedLineNumbers, holder)
    }

    private fun extractSingleLineNumberNode(contentNodes: List<ASTNode>): ASTNode? {
        if (contentNodes.size != 1) return null
        val node = contentNodes.single()
        return when (node.elementType) {
            TiBasicTokenTypes.NUMERIC_LITERAL -> node
            TiBasicNodeTypes.EXPRESSION -> node
                .nonWhitespaceChildren
                .singleOrNull()
                ?.takeIf { it.elementType == TiBasicTokenTypes.NUMERIC_LITERAL }
            else -> null
        }
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
        if (children.any { it.elementType !in ALLOWED_FOR_STATEMENT_CHILD_TYPES }) {
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
        val forsByVar = file.forStatements()
            .filter { it.controlVariableName() != null }
            .groupBy { it.controlVariableName()!! }
        val nextsByVar = file.nextStatements()
            .filter { it.controlVariableName() != null }
            .groupBy { it.controlVariableName()!! }
        for (varName in (forsByVar.keys + nextsByVar.keys).toSet()) {
            val fors = forsByVar[varName].orEmpty()
            val nexts = nextsByVar[varName].orEmpty()
            if (fors.size == nexts.size) continue
            val message = "FOR-NEXT mismatch for $varName: ${fors.size} FOR, ${nexts.size} NEXT"
            fors.forEach { holder.warning(message, it) }
            nexts.forEach { holder.warning(message, it) }
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
        annotateLineNumber(contentNodes, holder, statement)
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

        val mismatchRange = numericContextMismatchRange(expression)
        if (mismatchRange != null) {
            holder.error("String-number mismatch", mismatchRange)
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

        val exprIdx = children.indexOfFirst { it === expression.node }
        val thenIdx = children.indexOfFirst { it.elementType == TiBasicTokenTypes.THEN_KEYWORD }
        if (thenIdx - exprIdx > 1) {
            holder.error("Incorrect statement", statement)
            return
        }

        val mismatchRange = numericContextMismatchRange(expression)
        if (mismatchRange != null) {
            holder.error("String-number mismatch", mismatchRange)
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
        val expression = statement.firstChildOfType<TiBasicExpression>()
        if (expression == null) {
            val hasEqOp = statement.node.firstChildOfType(TiBasicTokenTypes.EQ_OP) != null
            if (hasEqOp) {
                val nonWhitespaceAfterEq = statement.node.childrenAfter(TiBasicTokenTypes.EQ_OP)
                    .filter { it.elementType != TokenType.WHITE_SPACE }
                if (nonWhitespaceAfterEq.firstOrNull()?.elementType != TiBasicTokenTypes.TAB_KEYWORD) {
                    holder.error("Incorrect statement", statement)
                }
            }
            return
        }
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
        if (statement is TiBasicPrintStatement && statement.isFileOutput()) {
            annotateFilePrintStatement(statement, holder)
        } else {
            annotatePrintArgNodes(
                nodes = statement.node.allChildren.toList(),
                holder = holder,
                contextExpr = statement.firstChildOfType<TiBasicExpression>(),
            )
        }
    }

    private fun annotateFilePrintStatement(statement: TiBasicPrintStatement, holder: AnnotationHolder) {
        if (annotateFileNumberExpression(statement, holder)) return
        val dotNode = statement.node.firstChildOfType(TiBasicTokenTypes.DOT)
        if (annotateRecNode(statement, dotNode, holder)) return
        val colonNode = statement.node.firstChildOfType(TiBasicTokenTypes.COLON)
        if (colonNode == null) {
            holder.error("Incorrect statement", statement)
            return
        }
        val lastSpecExprNode = statement.recordNumberExpr()?.node ?: statement.fileNumberExpr()?.node
        if (lastSpecExprNode != null) {
            var passedSpec = false
            for (child in statement.node.allChildren) {
                if (child === lastSpecExprNode) {
                    passedSpec = true; continue
                }
                if (!passedSpec) continue
                if (child === colonNode) break
                if (child.elementType != TokenType.WHITE_SPACE) {
                    holder.error("Incorrect statement", statement)
                    return
                }
            }
        }
        val argNodes = statement.node.allChildren.toList()
            .dropWhile { it.elementType != TiBasicTokenTypes.COLON }
            .drop(1)
        val firstArgExpr = argNodes
            .firstOrNull { it.elementType == TiBasicNodeTypes.EXPRESSION }
            ?.psi as? TiBasicExpression
        annotatePrintArgNodes(argNodes, holder, firstArgExpr)
    }

    private fun annotateRecNode(
        statement: TiBasicRecordNumberStatement,
        dotNode: ASTNode?,
        holder: AnnotationHolder
    ): Boolean {
        val recNode = statement.recKeywordNode()
        if (dotNode != null && recNode == null) {
            holder.error("Incorrect statement", statement)
            return true
        }
        if (recNode != null) {
            val recordNumberExpr = statement.recordNumberExpr()
            if (recordNumberExpr == null) {
                holder.error("Incorrect statement", statement)
                return true
            }
            if (isStringExpression(recordNumberExpr)) {
                holder.error("Numeric expression expected", recordNumberExpr)
            }
        }
        return false
    }

    private fun annotatePrintArgNodes(
        nodes: List<ASTNode>,
        holder: AnnotationHolder,
        contextExpr: TiBasicExpression?,
    ) {
        var previousWasExpression = false
        for (child in nodes) {
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
                    previousWasExpression = false
                    val isNumericExpr = contextExpr != null && !isStringExpression(contextExpr)
                    val isStringExpr = contextExpr != null && isStringExpression(contextExpr)
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
        if (!varAccess.hasClosingSubscriptParen()) {
            if (varAccess.parent !is TiBasicDimStatement) {
                holder.error(INCORRECT_STATEMENT_RUNTIME_ERROR, varAccess)
            }
            return
        }
        if (hasMalformedSubscriptSyntax(varAccess)) {
            holder.error("Bad subscript definition", varAccess)
            return
        }
        val dimCount = varAccess.subscriptDimCount()
        if (dimCount == 0 || dimCount > 3) {
            holder.error("Bad subscript definition", varAccess)
        }
    }

    private fun hasMalformedSubscriptSyntax(varAccess: TiBasicVariableAccess): Boolean {
        if (!varAccess.hasSubscriptParens()) return false
        return hasMalformedParenthesizedExpressionList(varAccess.node.nonWhitespaceChildren)
    }

    private fun hasMalformedCallSyntax(statement: TiBasicCallStatement): Boolean {
        if (!statement.hasArgumentParens()) return false
        return hasMalformedParenthesizedExpressionList(statement.node.nonWhitespaceChildren)
    }

    private fun hasMalformedParenthesizedExpressionList(children: List<ASTNode>): Boolean {
        val openingParenIndex = children.indexOfFirst { it.elementType == TiBasicTokenTypes.LPAREN }
        if (openingParenIndex == -1) return false
        val closingParenIndex = children.indexOfLast { it.elementType == TiBasicTokenTypes.RPAREN }
        if (closingParenIndex <= openingParenIndex) return true
        if (children.drop(closingParenIndex + 1).isNotEmpty()) return true
        val expressionListNodes = children.subList(openingParenIndex + 1, closingParenIndex)
        if (expressionListNodes.isEmpty()) return false
        var expectsExpression = true
        expressionListNodes.forEach { child ->
            if (expectsExpression) {
                if (child.elementType != TiBasicNodeTypes.EXPRESSION) return true
                val expression = child.psi as? TiBasicExpression ?: return true
                if (expression.node.nonWhitespaceChildren.isEmpty()) return true
                expectsExpression = false
            } else {
                if (child.elementType != TiBasicTokenTypes.COMMA) return true
                expectsExpression = true
            }
        }
        return expectsExpression
    }

    private fun annotateExpression(expr: TiBasicExpression, holder: AnnotationHolder) {
        val children = expr.node.nonWhitespaceChildren
        if (children.isEmpty()) return
        val openParens = children.count { it.elementType == TiBasicTokenTypes.LPAREN }
        val closeParens = children.count { it.elementType == TiBasicTokenTypes.RPAREN }
        if (openParens > closeParens) {
            holder.error(INCORRECT_STATEMENT_RUNTIME_ERROR, expr)
            return
        }
        val invalidRange = invalidExpressionRange(children, expr.textRange)
        if (invalidRange != null) {
            holder.error(INCORRECT_STATEMENT_RUNTIME_ERROR, invalidRange)
            return
        }
        if (children.isNotEmpty() && children.none { it.elementType in EXPRESSION_VALUE_NODE_TYPES }) {
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

    private fun invalidExpressionRange(children: List<ASTNode>, expressionRange: TextRange): TextRange? =
        parseExpressionSyntax(children, startIndex = 0, expressionRange = expressionRange).invalidRange

    private fun parseExpressionSyntax(
        children: List<ASTNode>,
        startIndex: Int,
        expressionRange: TextRange,
        openingParenIndex: Int? = null,
    ): ExpressionSyntaxResult {
        var index = startIndex
        var expectsOperand = true
        var consumedOperand = false
        while (index < children.size) {
            val child = children[index]
            if (expectsOperand) {
                when {
                    child.elementType in UNARY_EXPRESSION_OPERATOR_TYPES -> index++
                    child.elementType == TiBasicTokenTypes.LPAREN -> {
                        val nested = parseExpressionSyntax(children, index + 1, expressionRange, index)
                        if (nested.invalidRange != null) return nested
                        index = nested.nextIndex
                        expectsOperand = false
                        consumedOperand = true
                    }

                    child.elementType == TiBasicTokenTypes.RPAREN -> {
                        return ExpressionSyntaxResult(
                            nextIndex = index + 1,
                            invalidRange = invalidParenthesizedRange(children, openingParenIndex, index),
                        )
                    }

                    child.isExpressionOperand() -> {
                        index++
                        expectsOperand = false
                        consumedOperand = true
                    }

                    else -> return ExpressionSyntaxResult(index + 1, child.textRange)
                }
            } else {
                when {
                    child.elementType == TiBasicTokenTypes.RPAREN -> {
                        return if (openingParenIndex != null) {
                            ExpressionSyntaxResult(nextIndex = index + 1)
                        } else {
                            ExpressionSyntaxResult(nextIndex = index + 1, invalidRange = child.textRange)
                        }
                    }

                    child.elementType in BINARY_EXPRESSION_OPERATOR_TYPES -> {
                        index++
                        expectsOperand = true
                    }

                    child.elementType == TiBasicTokenTypes.LPAREN || child.isExpressionOperand() ->
                        return ExpressionSyntaxResult(index + 1, child.textRange)

                    else -> return ExpressionSyntaxResult(index + 1, child.textRange)
                }
            }
        }
        if (openingParenIndex != null) {
            val openingParen = children[openingParenIndex]
            return ExpressionSyntaxResult(
                nextIndex = index,
                invalidRange = TextRange(openingParen.startOffset, expressionRange.endOffset),
            )
        }
        return when {
            !consumedOperand -> ExpressionSyntaxResult(nextIndex = index, invalidRange = expressionRange)
            expectsOperand -> ExpressionSyntaxResult(nextIndex = index, invalidRange = children.last().textRange)
            else -> ExpressionSyntaxResult(nextIndex = index)
        }
    }

    private fun invalidParenthesizedRange(
        children: List<ASTNode>,
        openingParenIndex: Int?,
        closingParenIndex: Int,
    ): TextRange =
        if (openingParenIndex == null) {
            children[closingParenIndex].textRange
        } else {
            val openingParen = children[openingParenIndex]
            val closingParen = children[closingParenIndex]
            TextRange(openingParen.startOffset, closingParen.startOffset + closingParen.textLength)
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
        val hasOpenParen = statement.node.firstChildOfType(TiBasicTokenTypes.LPAREN) != null
        val hasCloseParen = statement.node.firstChildOfType(TiBasicTokenTypes.RPAREN) != null
        if (hasOpenParen && !hasCloseParen) {
            holder.error(subprogram.syntaxViolationError ?: INCORRECT_STATEMENT_RUNTIME_ERROR, statement)
            return
        }
        val lparenCount = statement.node.childrenOfType(TiBasicTokenTypes.LPAREN).size
        val rparenCount = statement.node.childrenOfType(TiBasicTokenTypes.RPAREN).size
        if (lparenCount > rparenCount) {
            holder.error(subprogram.syntaxViolationError ?: INCORRECT_STATEMENT_RUNTIME_ERROR, statement)
            return
        }
        if (hasMalformedCallSyntax(statement)) {
            holder.error(subprogram.syntaxViolationError ?: INCORRECT_STATEMENT_RUNTIME_ERROR, statement)
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
            val argRule = subprogram.argRuleAt(index)
            if (argRule.requiresNumericVariableTarget && expr.numericVariableTarget() == null) {
                holder.error(subprogram.syntaxViolationError ?: INCORRECT_STATEMENT_RUNTIME_ERROR, statement)
                return
            }
            val isString = isStringExpression(expr)
            val mismatch = when (argRule.type) {
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
        if (name == "CHAR") {
            annotateCallCharPattern(statement, holder)
        }
    }

    private fun annotateCallCharPattern(statement: TiBasicCallStatement, holder: AnnotationHolder) {
        val patternArgNode = statement.arguments().getOrNull(1) ?: return
        val literal = patternArgNode.node.firstChildOfType(TiBasicTokenTypes.STRING_LITERAL) ?: return
        val pattern = literal.text.removePrefix("\"").removeSuffix("\"")
        if (pattern.length > 16 || pattern.any { it !in '0'..'9' && it.uppercaseChar() !in 'A'..'F' }) {
            holder.warning(BAD_VALUE_RUNTIME_ERROR, patternArgNode.textRange)
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

    private fun numericContextMismatchRange(expr: TiBasicExpression): TextRange? =
        numericContextMismatchRange(expr.node.nonWhitespaceChildren)

    private fun numericContextMismatchRange(children: List<ASTNode>): TextRange? {
        val significantChildren = children.dropWhile { it.elementType in UNARY_EXPRESSION_OPERATOR_TYPES }
        if (significantChildren.isEmpty()) return null
        val arithmeticOperatorIndex = firstTopLevelBinaryOperatorIndex(significantChildren, ARITHMETIC_OPERATOR_TYPES)
        if (arithmeticOperatorIndex != null) {
            return numericContextMismatchRange(significantChildren.subList(0, arithmeticOperatorIndex))
                ?: numericContextMismatchRange(significantChildren.subList(arithmeticOperatorIndex + 1, significantChildren.size))
        }
        if (isFullyParenthesized(significantChildren)) {
            return numericContextMismatchRange(significantChildren.subList(1, significantChildren.lastIndex))
        }
        if (firstTopLevelBinaryOperatorIndex(significantChildren, COMPARISON_OP_TYPES) != null) return null
        val concatOperatorIndex = firstTopLevelBinaryOperatorIndex(significantChildren, setOf(TiBasicTokenTypes.CONCAT_OP))
        if (concatOperatorIndex != null) {
            return significantChildren[concatOperatorIndex].textRange
        }
        val first = significantChildren.first()
        return when (first.elementType) {
            TiBasicTokenTypes.STRING_LITERAL -> first.textRange
            TiBasicNodeTypes.VARIABLE_ACCESS if first.firstChildType == TiBasicTokenTypes.STRING_VARIABLE -> first.textRange
            TiBasicNodeTypes.FUNCTION_CALL if first.firstChildType == TiBasicTokenTypes.STRING_FUNCTION_KEYWORD -> first.textRange
            else -> null
        }
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
        annotateNameConflicts(stringAccesses.filterNot(::isEmptySubscriptAccess), holder, ::variableAccessName, ::variableAccessDimCount)
        annotateNameConflicts(numericAccesses.filterNot(::isEmptySubscriptAccess), holder, ::variableAccessName, ::variableAccessDimCount)
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

    private fun isEmptySubscriptAccess(node: ASTNode): Boolean =
        node.childrenOfType(TiBasicNodeTypes.EXPRESSION).isEmpty() &&
                node.firstChildOfType(TiBasicTokenTypes.LPAREN) != null

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
        if (annotateFileNumberExpression(statement, holder)) return
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
            when (val keywordType = option.optionKeywordType()) {
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
        if (annotateFileNumberExpression(statement, holder)) return
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
        val value = children[0].text.toDoubleOrNull()?.roundToInt() ?: return
        when (value) {
            0 -> holder.error("File number 0 is reserved for screen", fileNumberExpr)
            !in FILE_NUMBER_RANGE -> holder.error("File number must be between 1 and 255", fileNumberExpr)
        }
    }
}

private data class DefSignature(val lineNumber: Int, val hasParameter: Boolean)

private data class ExpressionSyntaxResult(
    val nextIndex: Int,
    val invalidRange: TextRange? = null,
)

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

private val BINARY_EXPRESSION_OPERATOR_TYPES = setOf(
    TiBasicTokenTypes.PLUS_OP,
    TiBasicTokenTypes.MINUS_OP,
    TiBasicTokenTypes.MUL_OP,
    TiBasicTokenTypes.DIV_OP,
    TiBasicTokenTypes.POW_OP,
    TiBasicTokenTypes.CONCAT_OP,
    TiBasicTokenTypes.EQ_OP,
    TiBasicTokenTypes.LT_OP,
    TiBasicTokenTypes.GT_OP,
    TiBasicTokenTypes.NEQ_OP,
    TiBasicTokenTypes.LE_OP,
    TiBasicTokenTypes.GE_OP,
)

private val EXPRESSION_VALUE_NODE_TYPES = setOf(
    TiBasicTokenTypes.NUMERIC_LITERAL,
    TiBasicTokenTypes.STRING_LITERAL,
    TiBasicNodeTypes.VARIABLE_ACCESS,
    TiBasicNodeTypes.FUNCTION_CALL,
)

private fun ASTNode.isExpressionOperand(): Boolean = elementType in EXPRESSION_VALUE_NODE_TYPES

private val ALLOWED_FOR_STATEMENT_CHILD_TYPES = setOf(
    TiBasicTokenTypes.FOR_KEYWORD,
    TiBasicNodeTypes.VARIABLE_ACCESS,
    TiBasicTokenTypes.EQ_OP,
    TiBasicNodeTypes.EXPRESSION,
    TiBasicTokenTypes.TO_KEYWORD,
    TiBasicTokenTypes.STEP_KEYWORD,
)
