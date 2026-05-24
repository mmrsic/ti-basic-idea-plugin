package com.github.mmrsic.idea.plugins.tibasic.debug

import com.github.mmrsic.idea.plugins.tibasic.editor.UNARY_EXPRESSION_OPERATOR_TYPES
import com.github.mmrsic.idea.plugins.tibasic.editor.firstTopLevelBinaryOperatorIndex
import com.github.mmrsic.idea.plugins.tibasic.editor.isFullyParenthesized
import com.github.mmrsic.idea.plugins.tibasic.editor.lastTopLevelBinaryOperatorIndex
import com.github.mmrsic.idea.plugins.tibasic.ext.allChildren
import com.github.mmrsic.idea.plugins.tibasic.ext.nonWhitespaceChildren
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.common.VALID_LINE_NUMBER_RANGE
import com.github.mmrsic.idea.plugins.tibasic.psi.expression.TiBasicCallStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.expression.TiBasicFunctionCall
import com.github.mmrsic.idea.plugins.tibasic.psi.expression.TiBasicVariableAccess
import com.github.mmrsic.idea.plugins.tibasic.psi.statement.TiBasicEndStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.statement.TiBasicGosubStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.statement.TiBasicGotoStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.statement.TiBasicLetStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.statement.TiBasicReturnStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.statement.TiBasicStopStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.statement.TiBasicUnknownStatement
import com.github.mmrsic.idea.plugins.tibasic.util.parseTiBasicDecimalLiteral
import com.github.mmrsic.idea.plugins.tibasic.util.tiBasicDecimalString
import com.github.mmrsic.idea.plugins.tibasic.util.tiBasicRadix100Number
import com.intellij.lang.ASTNode
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.regex.Pattern

internal data class TiBasicDebugProgramSnapshot(
    val fileName: String,
    val filePath: String,
    val sourceLines: List<String>,
    val programLines: List<TiBasicDebugProgramLine>,
) {
    val lineNumberToProgramIndex: Map<Int, Int> = buildMap {
        programLines.forEachIndexed { index, line ->
            putIfAbsent(line.lineNumber, index)
        }
    }

    fun initialSession(): TiBasicDebugSession =
        if (programLines.isEmpty()) {
            TiBasicDebugSession(
                snapshot = this,
                status = TiBasicDebugSessionStatus.Stopped,
                statusMessage = TiBasicDebugMetadata.message(TiBasicDebugMetadata.noExecutableLineKey),
            )
        } else {
            TiBasicDebugSession(
                snapshot = this,
                status = TiBasicDebugSessionStatus.Paused,
                currentProgramIndex = FIRST_PROGRAM_INDEX,
                keyboardScanInput = defaultKeyboardScanInput(programLines[FIRST_PROGRAM_INDEX].semantics),
            )
        }

    fun nextHigherProgramIndex(afterLineNumber: Int): Int? =
        programLines.indexOfFirst { it.lineNumber > afterLineNumber }
            .takeIf { it >= 0 }

    companion object {
        fun create(file: TiBasicFile, document: Document?): TiBasicDebugProgramSnapshot {
            val text = document?.text ?: file.text
            val sourceLines = splitSourceLines(text)
            val programLines = file.lines()
                .mapNotNull { line ->
                    val lineNumber = line.lineNumber().takeIf { it in VALID_LINE_NUMBER_RANGE } ?: return@mapNotNull null
                    TiBasicDebugProgramLine(
                        sourceLineIndex = document?.getLineNumber(line.textOffset) ?: countSourceLineIndex(text, line.textOffset),
                        lineNumber = lineNumber,
                        sourceText = line.text,
                        semantics = line.statement()?.let(::createSemantics) ?: TiBasicDebugLineSemantics.Sequential,
                        referencedNumericVariableNames = line.statement()?.let(::referencedNumericVariableNames).orEmpty(),
                    )
                }
                .sortedWith(compareBy(TiBasicDebugProgramLine::lineNumber, TiBasicDebugProgramLine::sourceLineIndex))
            return TiBasicDebugProgramSnapshot(
                fileName = file.name,
                filePath = file.virtualFile?.path ?: file.name,
                sourceLines = sourceLines,
                programLines = programLines,
            )
        }

        private fun createSemantics(statement: PsiElement): TiBasicDebugLineSemantics = when (statement) {
            is TiBasicUnknownStatement -> TiBasicDebugLineSemantics.IncorrectStatement
            is TiBasicGotoStatement -> TiBasicDebugLineSemantics.Goto(createJumpTarget(statement.node, TiBasicTokenTypes.GOTO_KEYWORD))
            is TiBasicGosubStatement -> TiBasicDebugLineSemantics.Gosub(createJumpTarget(statement.node, TiBasicTokenTypes.GOSUB_KEYWORD))
            is TiBasicReturnStatement -> TiBasicDebugLineSemantics.Return(isStandaloneKeyword(statement.node, TiBasicTokenTypes.RETURN_KEYWORD))
            is TiBasicEndStatement -> TiBasicDebugLineSemantics.End(isStandaloneKeyword(statement.node, TiBasicTokenTypes.END_KEYWORD))
            is TiBasicStopStatement -> TiBasicDebugLineSemantics.Stop(isStandaloneKeyword(statement.node, TiBasicTokenTypes.STOP_KEYWORD))
            is TiBasicLetStatement -> createLetSemantics(statement)
            is TiBasicCallStatement -> createCallSemantics(statement)
            else -> TiBasicDebugLineSemantics.Sequential
        }

        private fun createCallSemantics(statement: TiBasicCallStatement): TiBasicDebugLineSemantics {
            if (statement.subprogramName() != KEY_SUBPROGRAM_NAME) {
                return TiBasicDebugLineSemantics.Sequential
            }
            if (!statement.hasArgumentParens() || !statement.hasClosingArgumentParen()) {
                return TiBasicDebugLineSemantics.IncorrectStatement
            }
            val arguments = statement.arguments()
            if (arguments.size != CALL_KEY_ARG_COUNT) {
                return TiBasicDebugLineSemantics.IncorrectStatement
            }
            val modeAssignment = createNumericAssignmentFromExpression(arguments[KEYBOARD_MODE_ARG_INDEX].node)
                ?: return TiBasicDebugLineSemantics.IncorrectStatement
            val keyCodeVariableName = arguments[KEY_CODE_ARG_INDEX]
                .numericVariableTarget()
                ?.takeUnless(TiBasicVariableAccess::hasSubscriptParens)
                ?.name
                ?: return TiBasicDebugLineSemantics.IncorrectStatement
            val statusVariableName = arguments[KEY_STATUS_ARG_INDEX]
                .numericVariableTarget()
                ?.takeUnless(TiBasicVariableAccess::hasSubscriptParens)
                ?.name
                ?: return TiBasicDebugLineSemantics.IncorrectStatement
            return TiBasicDebugLineSemantics.CallKey(
                modeAssignment = modeAssignment,
                keyCodeVariableName = keyCodeVariableName,
                statusVariableName = statusVariableName,
            )
        }

        private fun createLetSemantics(statement: TiBasicLetStatement): TiBasicDebugLineSemantics {
            if (statement.isMalformedForDebugger()) {
                return TiBasicDebugLineSemantics.IncorrectStatement
            }
            val targetVariable = statement.targetVariableAccess() ?: return TiBasicDebugLineSemantics.IncorrectStatement
            if (targetVariable.hasSubscriptParens()) return TiBasicDebugLineSemantics.Sequential
            val targetName = targetVariable.name ?: return TiBasicDebugLineSemantics.IncorrectStatement
            val expression = statement.assignedExpression() ?: return TiBasicDebugLineSemantics.IncorrectStatement
            return if (targetName.endsWith(STRING_VARIABLE_SUFFIX)) {
                val assignment = createStringAssignmentFromExpression(expression.node) ?: return TiBasicDebugLineSemantics.Sequential
                TiBasicDebugLineSemantics.LetString(targetName, assignment)
            } else {
                val assignment = createNumericAssignmentFromExpression(expression.node) ?: return TiBasicDebugLineSemantics.IncorrectStatement
                TiBasicDebugLineSemantics.LetNumeric(targetName, assignment)
            }
        }

        private fun createStringAssignmentFromExpression(expressionNode: ASTNode): TiBasicDebugStringAssignment? {
            val children = expressionNode.nonWhitespaceChildren
            if (children.isEmpty()) return null
            if (children.size == SINGLE_OPERAND_CHILD_COUNT) {
                return createStringAssignment(children.single())
            }
            if (children.size.isEven() || children.indices.any { index ->
                    if (index.isEven()) {
                        createStringAssignment(children[index]) == null
                    } else {
                        children[index].elementType != TiBasicTokenTypes.CONCAT_OP
                    }
                }
            ) {
                return null
            }
            var assignment = createStringAssignment(children.first()) ?: return null
            var index = FIRST_CONCAT_RIGHT_OPERAND_INDEX
            while (index < children.size) {
                val rightOperand = createStringAssignment(children[index]) ?: return null
                assignment = TiBasicDebugStringAssignment.Concat(assignment, rightOperand)
                index += CONCAT_GROUP_SIZE
            }
            return assignment
        }

        private fun createStringAssignment(node: ASTNode): TiBasicDebugStringAssignment? =
            when (node.elementType) {
                TiBasicTokenTypes.STRING_LITERAL ->
                    TiBasicDebugStringAssignment.StringLiteral(node.text.removePrefix("\"").removeSuffix("\""))

                TiBasicNodeTypes.VARIABLE_ACCESS ->
                    (node.psi as? TiBasicVariableAccess)
                        ?.takeIf { variableAccess ->
                            variableAccess.name?.endsWith(STRING_VARIABLE_SUFFIX) == true && !variableAccess.hasSubscriptParens()
                        }
                        ?.name
                        ?.let(TiBasicDebugStringAssignment::StringVariableReference)

                TiBasicNodeTypes.FUNCTION_CALL -> createStringFunctionAssignment(node)
                TiBasicNodeTypes.EXPRESSION -> createStringAssignmentFromExpression(node)
                else -> null
            }

        private fun createStringFunctionAssignment(functionCallNode: ASTNode): TiBasicDebugStringAssignment? {
            val children = functionCallNode.nonWhitespaceChildren
            val functionKeyword = children.firstOrNull()?.takeIf { it.elementType == TiBasicTokenTypes.STRING_FUNCTION_KEYWORD }
                ?: return null
            val arguments = children.filter { it.elementType == TiBasicNodeTypes.EXPRESSION }
            return when {
                functionKeyword.text.equals(CHR_DOLLAR_FUNCTION, ignoreCase = true) -> arguments
                    .singleOrNull()
                    ?.let(::createNumericAssignment)
                    ?.let(TiBasicDebugStringAssignment::CharacterCode)

                functionKeyword.text.equals(STR_DOLLAR_FUNCTION, ignoreCase = true) -> arguments
                    .singleOrNull()
                    ?.let(::createNumericAssignment)
                    ?.let(TiBasicDebugStringAssignment::StringRepresentation)

                functionKeyword.text.equals(SEG_DOLLAR_FUNCTION, ignoreCase = true) -> {
                    if (arguments.size != SEG_DOLLAR_ARG_COUNT) return null
                    val source = createStringAssignmentFromExpression(arguments[SEG_SOURCE_ARG_INDEX]) ?: return null
                    val start = createNumericAssignment(arguments[SEG_START_ARG_INDEX]) ?: return null
                    val length = createNumericAssignment(arguments[SEG_LENGTH_ARG_INDEX]) ?: return null
                    TiBasicDebugStringAssignment.Segment(source, start, length)
                }

                else -> null
            }
        }

        private fun createNumericAssignment(expressionNode: ASTNode): TiBasicDebugNumericAssignment? =
            createNumericAssignmentFromExpression(expressionNode)

        private fun createNumericAssignmentFromExpression(expressionNode: ASTNode): TiBasicDebugNumericAssignment? {
            val children = expressionNode.nonWhitespaceChildren
            if (children.isEmpty()) return null
            if (isFullyParenthesized(children)) {
                return createNumericAssignmentFromNodes(children.subList(FIRST_INNER_NODE_INDEX, children.lastIndex))
            }
            return createNumericAssignmentFromNodes(children)
        }

        private fun createNumericAssignmentFromNodes(nodes: List<ASTNode>): TiBasicDebugNumericAssignment? {
            if (nodes.isEmpty()) return null
            if (isFullyParenthesized(nodes)) {
                return createNumericAssignmentFromNodes(nodes.subList(FIRST_INNER_NODE_INDEX, nodes.lastIndex))
            }
            lastTopLevelBinaryOperatorIndex(nodes, ADDITIVE_OPERATOR_TYPES)?.let { operatorIndex ->
                val left = createNumericAssignmentFromNodes(nodes.subList(0, operatorIndex)) ?: return null
                val right = createNumericAssignmentFromNodes(nodes.subList(operatorIndex + 1, nodes.size)) ?: return null
                return TiBasicDebugNumericAssignment.Binary(left, nodes[operatorIndex].elementType, right)
            }
            lastTopLevelBinaryOperatorIndex(nodes, MULTIPLICATIVE_OPERATOR_TYPES)?.let { operatorIndex ->
                val left = createNumericAssignmentFromNodes(nodes.subList(0, operatorIndex)) ?: return null
                val right = createNumericAssignmentFromNodes(nodes.subList(operatorIndex + 1, nodes.size)) ?: return null
                return TiBasicDebugNumericAssignment.Binary(left, nodes[operatorIndex].elementType, right)
            }
            firstTopLevelBinaryOperatorIndex(nodes, POWER_OPERATOR_TYPES)?.let { operatorIndex ->
                val left = createNumericAssignmentFromNodes(nodes.subList(0, operatorIndex)) ?: return null
                val right = createNumericAssignmentFromNodes(nodes.subList(operatorIndex + 1, nodes.size)) ?: return null
                return TiBasicDebugNumericAssignment.Binary(left, nodes[operatorIndex].elementType, right)
            }
            return createSignedNumericAssignment(nodes)
        }

        private fun createSignedNumericAssignment(nodes: List<ASTNode>): TiBasicDebugNumericAssignment? {
            if (nodes.isEmpty()) return null
            var unaryOperatorsEndIndex = 0
            while (unaryOperatorsEndIndex < nodes.size && nodes[unaryOperatorsEndIndex].elementType in UNARY_EXPRESSION_OPERATOR_TYPES) {
                unaryOperatorsEndIndex++
            }
            if (unaryOperatorsEndIndex > 0) {
                val operand = createNumericAssignmentFromNodes(nodes.subList(unaryOperatorsEndIndex, nodes.size)) ?: return null
                return nodes.take(unaryOperatorsEndIndex)
                    .reversed()
                    .fold(operand) { assignment, operatorNode ->
                        TiBasicDebugNumericAssignment.Unary(operatorNode.elementType, assignment)
                    }
            }
            if (nodes.size != SINGLE_NUMERIC_CHILD_COUNT) return null
            return when (val node = nodes.single()) {
                else -> when (node.elementType) {
                    TiBasicTokenTypes.NUMERIC_LITERAL ->
                        node.text
                            .let(::parseTiBasicDecimalLiteral)
                            ?.let(TiBasicDebugNumericAssignment::Literal)

                    TiBasicNodeTypes.VARIABLE_ACCESS ->
                        (node.psi as? TiBasicVariableAccess)
                            ?.takeIf { variableAccess ->
                                variableAccess.name?.endsWith(STRING_VARIABLE_SUFFIX) == false && !variableAccess.hasSubscriptParens()
                            }
                            ?.name
                            ?.let(TiBasicDebugNumericAssignment::VariableReference)

                    TiBasicNodeTypes.FUNCTION_CALL -> createNumericFunctionAssignment(node.psi as? TiBasicFunctionCall)
                    else -> null
                }
            }
        }

        private fun createNumericFunctionAssignment(functionCall: TiBasicFunctionCall?): TiBasicDebugNumericAssignment? {
            functionCall ?: return null
            val arguments = functionCall.arguments()
            return when (functionCall.functionName()) {
                LEN_FUNCTION -> arguments.singleOrNull()
                    ?.let { createStringAssignmentFromExpression(it.node) }
                    ?.let(TiBasicDebugNumericAssignment::StringLength)

                ASC_FUNCTION -> arguments.singleOrNull()
                    ?.let { createStringAssignmentFromExpression(it.node) }
                    ?.let(TiBasicDebugNumericAssignment::AsciiCode)

                VAL_FUNCTION -> arguments.singleOrNull()
                    ?.let { createStringAssignmentFromExpression(it.node) }
                    ?.let(TiBasicDebugNumericAssignment::StringToNumber)

                POS_FUNCTION -> {
                    if (arguments.size != POS_ARG_COUNT) return null
                    val source = createStringAssignmentFromExpression(arguments[POS_SOURCE_ARG_INDEX].node) ?: return null
                    val target = createStringAssignmentFromExpression(arguments[POS_TARGET_ARG_INDEX].node) ?: return null
                    val start = createNumericAssignmentFromExpression(arguments[POS_START_ARG_INDEX].node) ?: return null
                    TiBasicDebugNumericAssignment.StringPosition(source, target, start)
                }

                else -> null
            }
        }

        private fun referencedNumericVariableNames(statement: PsiElement): Set<String> {
            val writeTargets = buildSet {
                (statement as? TiBasicLetStatement)?.targetVariableAccess()?.let(::add)
                val callStatement = statement as? TiBasicCallStatement
                if (callStatement?.subprogramName() == KEY_SUBPROGRAM_NAME) {
                    callStatement.arguments()
                        .drop(FIRST_CALL_KEY_WRITE_ARG_INDEX)
                        .mapNotNull { argument -> argument.numericVariableTarget()?.takeUnless(TiBasicVariableAccess::hasSubscriptParens) }
                        .forEach(::add)
                }
            }
            return PsiTreeUtil.findChildrenOfType(statement, TiBasicVariableAccess::class.java)
                .asSequence()
                .filterNot(writeTargets::contains)
                .filterNot(TiBasicVariableAccess::hasSubscriptParens)
                .mapNotNull { variableAccess -> variableAccess.name }
                .filterNot { variableName -> variableName.endsWith(STRING_VARIABLE_SUFFIX) }
                .toSet()
        }

        private fun createJumpTarget(statementNode: ASTNode, keywordType: IElementType): TiBasicJumpTarget =
            extractSingleLineNumberText(statementNode.nonWhitespaceChildren.filter { it.elementType != keywordType })
                ?.let { TiBasicJumpTarget.SyntaxValid(it) }
                ?: TiBasicJumpTarget.IncorrectStatement

        private fun isStandaloneKeyword(statementNode: ASTNode, keywordType: IElementType): Boolean =
            statementNode.nonWhitespaceChildren.singleOrNull()?.elementType == keywordType

        private fun extractSingleLineNumberText(contentNodes: List<ASTNode>): String? {
            if (contentNodes.size != SINGLE_LINE_NUMBER_NODE_COUNT) return null
            val contentNode = contentNodes.single()
            return when (contentNode.elementType) {
                TiBasicTokenTypes.NUMERIC_LITERAL -> contentNode.text
                TiBasicNodeTypes.EXPRESSION -> {
                    contentNode.nonWhitespaceChildren
                        .singleOrNull()
                        ?.takeIf { it.elementType == TiBasicTokenTypes.NUMERIC_LITERAL }
                        ?.text
                }

                else -> null
            }
        }

        private fun splitSourceLines(text: String): List<String> =
            Pattern.compile(LINE_SEPARATOR_REGEX).split(text, PRESERVE_TRAILING_EMPTY_LINES).toList()

        private fun countSourceLineIndex(text: String, offset: Int): Int =
            text.take(offset.coerceAtMost(text.length)).count { it == '\n' }
    }
}

internal data class TiBasicDebugProgramLine(
    val sourceLineIndex: Int,
    val lineNumber: Int,
    val sourceText: String,
    val semantics: TiBasicDebugLineSemantics,
    val referencedNumericVariableNames: Set<String> = emptySet(),
)

internal enum class TiBasicDebugSessionStatus(val bundleKey: String) {
    Paused(TiBasicDebugMetadata.toolWindowStatusPausedKey),
    PendingStop(TiBasicDebugMetadata.toolWindowStatusPendingStopKey),
    Stopped(TiBasicDebugMetadata.toolWindowStatusStoppedKey),
}

internal data class TiBasicDebugSession(
    val snapshot: TiBasicDebugProgramSnapshot,
    val status: TiBasicDebugSessionStatus,
    val currentProgramIndex: Int? = null,
    val gosubOriginLineNumbers: List<Int> = emptyList(),
    val numericVariables: Map<String, TiBasicDebugNumericValue> = emptyMap(),
    val stringVariables: Map<String, TiBasicDebugStringValue> = emptyMap(),
    val statusMessage: String? = null,
    val keyboardScanInput: String = EMPTY_STRING,
    val lastKeyboardMode: Int? = null,
) {
    val currentProgramLine: TiBasicDebugProgramLine?
        get() = currentProgramIndex?.let(snapshot.programLines::get)

    val currentSourceLineIndex: Int?
        get() = currentProgramLine?.sourceLineIndex

    val keyboardRequest: TiBasicDebugKeyboardRequest?
        get() {
            if (status != TiBasicDebugSessionStatus.Paused) return null
            val programLine = currentProgramLine ?: return null
            val semantics = programLine.semantics as? TiBasicDebugLineSemantics.CallKey ?: return null
            val preparedSession = initializeReferencedNumericVariables(programLine.referencedNumericVariableNames)
            val modeEvaluation = preparedSession.evaluateNumericAssignment(semantics.modeAssignment) ?: return null
            val roundedMode = modeEvaluation.value.value.roundToWholeNumberIntOrNull() ?: return null
            val resolvedMode = preparedSession.resolveCallKeyMode(roundedMode) ?: return null
            return TiBasicDebugKeyboardRequest(
                mode = resolvedMode,
                allowedCodesDisplay = callKeyAllowedCodesDisplay(resolvedMode),
                scanInput = preparedSession.keyboardScanInput.ifEmpty { defaultKeyboardScanInput(programLine.semantics) },
            )
        }

    fun step(): TiBasicDebugSession = when (status) {
        TiBasicDebugSessionStatus.Paused -> stepPaused()
        TiBasicDebugSessionStatus.PendingStop ->
            copy(status = TiBasicDebugSessionStatus.Stopped, currentProgramIndex = null, keyboardScanInput = EMPTY_STRING)

        TiBasicDebugSessionStatus.Stopped -> this
    }

    fun stop(): TiBasicDebugSession =
        copy(status = TiBasicDebugSessionStatus.Stopped, currentProgramIndex = null, keyboardScanInput = EMPTY_STRING)

    private fun stepPaused(): TiBasicDebugSession {
        val programLine = currentProgramLine ?: return stop()
        val sessionWithInitializedNumericVariables = initializeReferencedNumericVariables(programLine.referencedNumericVariableNames)
        return when (val semantics = programLine.semantics) {
            TiBasicDebugLineSemantics.Sequential -> sessionWithInitializedNumericVariables.continueAfter(programLine.lineNumber)
            is TiBasicDebugLineSemantics.Goto -> sessionWithInitializedNumericVariables.jumpTo(programLine, semantics.target)
            is TiBasicDebugLineSemantics.Gosub -> sessionWithInitializedNumericVariables.jumpTo(programLine, semantics.target, rememberOrigin = true)
            is TiBasicDebugLineSemantics.Return -> sessionWithInitializedNumericVariables.returnFrom(semantics.isStandaloneKeyword)
            is TiBasicDebugLineSemantics.End -> sessionWithInitializedNumericVariables.pendingStopIf(semantics.isStandaloneKeyword)
            is TiBasicDebugLineSemantics.Stop -> sessionWithInitializedNumericVariables.pendingStopIf(semantics.isStandaloneKeyword)
            is TiBasicDebugLineSemantics.LetString -> sessionWithInitializedNumericVariables.applyStringLet(programLine.lineNumber, semantics)
            is TiBasicDebugLineSemantics.LetNumeric -> sessionWithInitializedNumericVariables.applyNumericLet(programLine.lineNumber, semantics)
            is TiBasicDebugLineSemantics.CallKey -> sessionWithInitializedNumericVariables.applyCallKey(programLine.lineNumber, semantics)
            TiBasicDebugLineSemantics.IncorrectStatement -> incorrectStatement()
        }
    }

    private fun initializeReferencedNumericVariables(variableNames: Set<String>): TiBasicDebugSession {
        if (variableNames.isEmpty()) return this
        val initializedVariables = variableNames
            .filterNot(numericVariables::containsKey)
            .associateWith { TiBasicDebugNumericValue.fromValue(BigDecimal.ZERO) }
        return if (initializedVariables.isEmpty()) this else copy(numericVariables = numericVariables + initializedVariables)
    }

    private fun continueAfter(currentLineNumber: Int, statusMessage: String? = null): TiBasicDebugSession =
        snapshot.nextHigherProgramIndex(currentLineNumber)?.let { nextIndex ->
            moveTo(nextIndex, statusMessage)
        } ?: copy(
            status = TiBasicDebugSessionStatus.PendingStop,
            statusMessage = statusMessage,
            keyboardScanInput = EMPTY_STRING,
        )

    private fun moveTo(programIndex: Int, statusMessage: String? = null): TiBasicDebugSession =
        copy(
            status = TiBasicDebugSessionStatus.Paused,
            currentProgramIndex = programIndex,
            statusMessage = statusMessage,
            keyboardScanInput = defaultKeyboardScanInput(snapshot.programLines[programIndex].semantics),
        )

    private fun jumpTo(
        currentProgramLine: TiBasicDebugProgramLine,
        target: TiBasicJumpTarget,
        rememberOrigin: Boolean = false,
    ): TiBasicDebugSession =
        when (target) {
            TiBasicJumpTarget.IncorrectStatement -> incorrectStatement()
            is TiBasicJumpTarget.SyntaxValid -> {
                val targetLineNumber = target.lineNumberText.toIntOrNull()
                val targetIndex = targetLineNumber
                    ?.takeIf { it in VALID_LINE_NUMBER_RANGE }
                    ?.let(snapshot.lineNumberToProgramIndex::get)
                if (targetIndex == null) {
                    badLineNumber()
                } else {
                    copy(
                        currentProgramIndex = targetIndex,
                        gosubOriginLineNumbers = if (rememberOrigin) gosubOriginLineNumbers + currentProgramLine.lineNumber else gosubOriginLineNumbers,
                        statusMessage = null,
                        keyboardScanInput = defaultKeyboardScanInput(snapshot.programLines[targetIndex].semantics),
                    )
                }
            }
        }

    private fun applyStringLet(
        currentLineNumber: Int,
        semantics: TiBasicDebugLineSemantics.LetString,
    ): TiBasicDebugSession {
        val evaluation = evaluateStringAssignment(semantics.assignment) ?: return continueAfter(currentLineNumber)
        val updatedNumericVariables = numericVariables + evaluation.initializedNumericVariables
        val updatedStringVariables = stringVariables + evaluation.initializedStringVariables + (semantics.targetVariableName to evaluation.value)
        return continueAfter(currentLineNumber, evaluation.warningMessage).copy(
            numericVariables = updatedNumericVariables,
            stringVariables = updatedStringVariables,
        )
    }

    private fun applyNumericLet(
        currentLineNumber: Int,
        semantics: TiBasicDebugLineSemantics.LetNumeric,
    ): TiBasicDebugSession {
        val evaluation = evaluateNumericAssignment(semantics.assignment) ?: return continueAfter(currentLineNumber)
        val updatedNumericVariables = numericVariables + evaluation.initializedNumericVariables + (semantics.targetVariableName to evaluation.value)
        val updatedStringVariables = stringVariables + evaluation.initializedStringVariables
        return continueAfter(currentLineNumber, evaluation.warningMessage).copy(
            numericVariables = updatedNumericVariables,
            stringVariables = updatedStringVariables,
        )
    }

    private fun applyCallKey(
        currentLineNumber: Int,
        semantics: TiBasicDebugLineSemantics.CallKey,
    ): TiBasicDebugSession {
        val modeEvaluation = evaluateNumericAssignment(semantics.modeAssignment) ?: return incorrectStatement()
        val updatedNumericVariables = numericVariables + modeEvaluation.initializedNumericVariables
        val updatedStringVariables = stringVariables + modeEvaluation.initializedStringVariables
        val roundedMode = modeEvaluation.value.value.roundToWholeNumberIntOrNull()
            ?: return badValue(modeEvaluation.value.usualDisplay).copy(
                numericVariables = updatedNumericVariables,
                stringVariables = updatedStringVariables,
            )
        val resolvedMode = resolveCallKeyMode(roundedMode) ?: return badValue(modeEvaluation.value.usualDisplay).copy(
            numericVariables = updatedNumericVariables,
            stringVariables = updatedStringVariables,
        )
        val scanInputText = keyboardScanInput.trim().ifEmpty { defaultKeyboardScanInput(semantics) }
        val scanInputValue = parseTiBasicDecimalLiteral(scanInputText)?.roundToWholeNumberIntOrNull()
            ?: return copy(
                numericVariables = updatedNumericVariables,
                stringVariables = updatedStringVariables,
                statusMessage = TiBasicDebugMetadata.message(TiBasicDebugMetadata.badValueKey, scanInputText),
            )
        if (scanInputValue != NO_KEY_CODE && !isAllowedCallKeyCode(resolvedMode, scanInputValue)) {
            return copy(
                numericVariables = updatedNumericVariables,
                stringVariables = updatedStringVariables,
                statusMessage = TiBasicDebugMetadata.message(TiBasicDebugMetadata.badValueKey, scanInputValue),
            )
        }
        return continueAfter(currentLineNumber).copy(
            numericVariables = updatedNumericVariables + mapOf(
                semantics.keyCodeVariableName to TiBasicDebugNumericValue.fromValue(scanInputValue.toBigDecimal()),
                semantics.statusVariableName to TiBasicDebugNumericValue.fromValue(
                    if (scanInputValue == NO_KEY_CODE) BigDecimal.ZERO else BigDecimal.ONE,
                ),
            ),
            stringVariables = updatedStringVariables,
            lastKeyboardMode = resolvedMode,
        )
    }

    private fun resolveCallKeyMode(roundedMode: Int): Int? =
        when (roundedMode) {
            !in VALID_CALL_KEY_MODES -> null
            REUSE_LAST_KEYBOARD_MODE -> lastKeyboardMode ?: DEFAULT_KEYBOARD_MODE
            else -> roundedMode
        }

    private fun evaluateStringAssignment(assignment: TiBasicDebugStringAssignment): TiBasicDebugStringEvaluation? =
        (when (assignment) {
            is TiBasicDebugStringAssignment.StringLiteral -> TiBasicDebugStringEvaluation.fromLiteral(assignment.text)
            is TiBasicDebugStringAssignment.StringVariableReference -> {
                val existingValue = stringVariables[assignment.variableName]
                if (existingValue != null) {
                    TiBasicDebugStringEvaluation.fromExistingValue(existingValue)
                } else {
                    val initializedValue = TiBasicDebugStringValue.fromText(EMPTY_STRING)
                    TiBasicDebugStringEvaluation(
                        value = initializedValue,
                        initializedStringVariables = mapOf(assignment.variableName to initializedValue),
                    )
                }
            }

            is TiBasicDebugStringAssignment.CharacterCode ->
                evaluateNumericAssignment(assignment.code)
                    ?.let { numericEvaluation ->
                        numericEvaluation.value.value
                            .toIntExactOrNull()
                            ?.toChar()
                            ?.toString()
                            ?.let(TiBasicDebugStringEvaluation::fromLiteral)
                            ?.mergeWith(numericEvaluation)
                    }

            is TiBasicDebugStringAssignment.StringRepresentation ->
                evaluateNumericAssignment(assignment.value)
                    ?.let { numericEvaluation ->
                        TiBasicDebugStringEvaluation.fromLiteral(tiBasicDecimalString(numericEvaluation.value.value))
                            .mergeWith(numericEvaluation)
                    }

            is TiBasicDebugStringAssignment.Segment -> {
                val source = evaluateStringAssignment(assignment.source) ?: return null
                val start = evaluateNumericAssignment(assignment.start) ?: return null
                val length = evaluateNumericAssignment(assignment.length) ?: return null
                val startValue = start.value.value.toIntExactOrNull() ?: return null
                val lengthValue = length.value.value.toIntExactOrNull() ?: return null
                TiBasicDebugStringEvaluation.fromLiteral(source.value.text.segment(startValue, lengthValue))
                    .mergeWith(source)
                    .mergeWith(start, length)
            }

            is TiBasicDebugStringAssignment.Concat -> {
                val left = evaluateStringAssignment(assignment.left) ?: return null
                val right = evaluateStringAssignment(assignment.right) ?: return null
                TiBasicDebugStringEvaluation.fromLiteral(left.value.text + right.value.text)
                    .mergeWith(left, right)
            }
        })?.truncateForReuse()

    private fun evaluateNumericAssignment(assignment: TiBasicDebugNumericAssignment): TiBasicDebugNumericEvaluation? =
        when (assignment) {
            is TiBasicDebugNumericAssignment.Literal ->
                TiBasicDebugNumericEvaluation(TiBasicDebugNumericValue.fromValue(assignment.value))

            is TiBasicDebugNumericAssignment.VariableReference -> {
                val existingValue = numericVariables[assignment.variableName]
                if (existingValue != null) {
                    TiBasicDebugNumericEvaluation(existingValue)
                } else {
                    val initializedValue = TiBasicDebugNumericValue.fromValue(BigDecimal.ZERO)
                    TiBasicDebugNumericEvaluation(
                        value = initializedValue,
                        initializedNumericVariables = mapOf(assignment.variableName to initializedValue),
                    )
                }
            }

            is TiBasicDebugNumericAssignment.Unary ->
                evaluateNumericAssignment(assignment.operand)
                    ?.let { operandEvaluation ->
                        val unaryValue = when (assignment.operatorType) {
                            TiBasicTokenTypes.PLUS_OP -> operandEvaluation.value.value
                            TiBasicTokenTypes.MINUS_OP -> operandEvaluation.value.value.negate()
                            else -> return null
                        }
                        operandEvaluation.mapValue { unaryValue }
                    }

            is TiBasicDebugNumericAssignment.Binary -> {
                val left = evaluateNumericAssignment(assignment.left) ?: return null
                val right = evaluateNumericAssignment(assignment.right) ?: return null
                val resultValue = when (assignment.operatorType) {
                    TiBasicTokenTypes.PLUS_OP -> left.value.value + right.value.value
                    TiBasicTokenTypes.MINUS_OP -> left.value.value - right.value.value
                    TiBasicTokenTypes.MUL_OP -> left.value.value * right.value.value
                    TiBasicTokenTypes.DIV_OP -> {
                        if (right.value.value.compareTo(BigDecimal.ZERO) == 0) return null
                        left.value.value.divide(right.value.value, DEBUG_MATH_CONTEXT)
                    }

                    TiBasicTokenTypes.POW_OP -> {
                        val exponent = right.value.value.toIntExactOrNull() ?: return null
                        left.value.value.pow(exponent, DEBUG_MATH_CONTEXT)
                    }

                    else -> return null
                }
                TiBasicDebugNumericEvaluation(TiBasicDebugNumericValue.fromValue(resultValue))
                    .mergeWith(left, right)
            }

            is TiBasicDebugNumericAssignment.StringLength ->
                evaluateStringAssignment(assignment.source)
                    ?.let { stringEvaluation ->
                        TiBasicDebugNumericEvaluation(TiBasicDebugNumericValue.fromValue(stringEvaluation.value.text.length.toBigDecimal()))
                            .mergeWith(stringEvaluation)
                    }

            is TiBasicDebugNumericAssignment.AsciiCode ->
                evaluateStringAssignment(assignment.source)
                    ?.let { stringEvaluation ->
                        stringEvaluation.value.text.firstOrNull()
                            ?.code
                            ?.toBigDecimal()
                            ?.let { code ->
                                TiBasicDebugNumericEvaluation(TiBasicDebugNumericValue.fromValue(code))
                                    .mergeWith(stringEvaluation)
                            }
                    }

            is TiBasicDebugNumericAssignment.StringToNumber ->
                evaluateStringAssignment(assignment.source)
                    ?.let { stringEvaluation ->
                        parseTiBasicDecimalLiteral(stringEvaluation.value.text)
                            ?.let { number ->
                                TiBasicDebugNumericEvaluation(TiBasicDebugNumericValue.fromValue(number))
                                    .mergeWith(stringEvaluation)
                            }
                    }

            is TiBasicDebugNumericAssignment.StringPosition -> {
                val source = evaluateStringAssignment(assignment.source) ?: return null
                val target = evaluateStringAssignment(assignment.target) ?: return null
                val start = evaluateNumericAssignment(assignment.start) ?: return null
                val startValue = start.value.value.toIntExactOrNull() ?: return null
                TiBasicDebugNumericEvaluation(
                    TiBasicDebugNumericValue.fromValue(source.value.text.pos(target.value.text, startValue).toBigDecimal()),
                ).mergeWith(source, target, start)
            }
        }

    private fun returnFrom(isStandaloneKeyword: Boolean): TiBasicDebugSession {
        if (!isStandaloneKeyword) return incorrectStatement()
        val originLineNumber = gosubOriginLineNumbers.lastOrNull() ?: return copy(
            status = TiBasicDebugSessionStatus.PendingStop,
            statusMessage = TiBasicDebugMetadata.message(TiBasicDebugMetadata.cantDoThatKey),
        )
        val nextIndex = snapshot.nextHigherProgramIndex(originLineNumber)
        return if (nextIndex == null) {
            copy(
                status = TiBasicDebugSessionStatus.PendingStop,
                gosubOriginLineNumbers = gosubOriginLineNumbers.dropLast(SINGLE_STACK_FRAME),
                statusMessage = null,
                keyboardScanInput = EMPTY_STRING,
            )
        } else {
            moveTo(
                programIndex = nextIndex,
                statusMessage = null,
            ).copy(
                gosubOriginLineNumbers = gosubOriginLineNumbers.dropLast(SINGLE_STACK_FRAME),
            )
        }
    }

    private fun pendingStopIf(isStandaloneKeyword: Boolean): TiBasicDebugSession =
        if (isStandaloneKeyword) {
            copy(status = TiBasicDebugSessionStatus.PendingStop, statusMessage = null, keyboardScanInput = EMPTY_STRING)
        } else {
            incorrectStatement()
        }

    private fun badLineNumber(): TiBasicDebugSession =
        copy(
            status = TiBasicDebugSessionStatus.PendingStop,
            statusMessage = TiBasicDebugMetadata.message(TiBasicDebugMetadata.badLineNumberKey),
            keyboardScanInput = EMPTY_STRING,
        )

    private fun badValue(value: Any): TiBasicDebugSession =
        copy(
            status = TiBasicDebugSessionStatus.PendingStop,
            statusMessage = TiBasicDebugMetadata.message(TiBasicDebugMetadata.badValueKey, value),
            keyboardScanInput = EMPTY_STRING,
        )

    private fun incorrectStatement(): TiBasicDebugSession =
        copy(
            status = TiBasicDebugSessionStatus.PendingStop,
            statusMessage = TiBasicDebugMetadata.message(TiBasicDebugMetadata.incorrectStatementKey),
            keyboardScanInput = EMPTY_STRING,
        )
}

internal sealed interface TiBasicDebugLineSemantics {
    data object Sequential : TiBasicDebugLineSemantics
    data object IncorrectStatement : TiBasicDebugLineSemantics
    data class Goto(val target: TiBasicJumpTarget) : TiBasicDebugLineSemantics
    data class Gosub(val target: TiBasicJumpTarget) : TiBasicDebugLineSemantics
    data class Return(val isStandaloneKeyword: Boolean) : TiBasicDebugLineSemantics
    data class End(val isStandaloneKeyword: Boolean) : TiBasicDebugLineSemantics
    data class Stop(val isStandaloneKeyword: Boolean) : TiBasicDebugLineSemantics
    data class LetString(
        val targetVariableName: String,
        val assignment: TiBasicDebugStringAssignment,
    ) : TiBasicDebugLineSemantics

    data class LetNumeric(
        val targetVariableName: String,
        val assignment: TiBasicDebugNumericAssignment,
    ) : TiBasicDebugLineSemantics

    data class CallKey(
        val modeAssignment: TiBasicDebugNumericAssignment,
        val keyCodeVariableName: String,
        val statusVariableName: String,
    ) : TiBasicDebugLineSemantics
}

internal sealed interface TiBasicJumpTarget {
    data object IncorrectStatement : TiBasicJumpTarget
    data class SyntaxValid(val lineNumberText: String) : TiBasicJumpTarget
}

internal sealed interface TiBasicDebugStringAssignment {
    data class StringLiteral(val text: String) : TiBasicDebugStringAssignment
    data class StringVariableReference(val variableName: String) : TiBasicDebugStringAssignment
    data class CharacterCode(val code: TiBasicDebugNumericAssignment) : TiBasicDebugStringAssignment
    data class StringRepresentation(val value: TiBasicDebugNumericAssignment) : TiBasicDebugStringAssignment
    data class Segment(
        val source: TiBasicDebugStringAssignment,
        val start: TiBasicDebugNumericAssignment,
        val length: TiBasicDebugNumericAssignment,
    ) : TiBasicDebugStringAssignment

    data class Concat(
        val left: TiBasicDebugStringAssignment,
        val right: TiBasicDebugStringAssignment,
    ) : TiBasicDebugStringAssignment
}

internal sealed interface TiBasicDebugNumericAssignment {
    data class Literal(val value: BigDecimal) : TiBasicDebugNumericAssignment
    data class VariableReference(val variableName: String) : TiBasicDebugNumericAssignment
    data class Unary(val operatorType: IElementType, val operand: TiBasicDebugNumericAssignment) : TiBasicDebugNumericAssignment
    data class Binary(
        val left: TiBasicDebugNumericAssignment,
        val operatorType: IElementType,
        val right: TiBasicDebugNumericAssignment,
    ) : TiBasicDebugNumericAssignment

    data class StringLength(val source: TiBasicDebugStringAssignment) : TiBasicDebugNumericAssignment
    data class AsciiCode(val source: TiBasicDebugStringAssignment) : TiBasicDebugNumericAssignment
    data class StringToNumber(val source: TiBasicDebugStringAssignment) : TiBasicDebugNumericAssignment
    data class StringPosition(
        val source: TiBasicDebugStringAssignment,
        val target: TiBasicDebugStringAssignment,
        val start: TiBasicDebugNumericAssignment,
    ) : TiBasicDebugNumericAssignment
}

internal data class TiBasicDebugNumericValue(
    val value: BigDecimal,
    val internalBytes: List<Int>,
) {
    val usualDisplay: String
        get() = tiBasicDecimalString(value)

    val internalDisplay: String
        get() = internalBytes.joinToString(BYTE_SEPARATOR) { byte ->
            NUMERIC_BYTE_PREFIX + byte.toString(BYTE_RADIX).uppercase().padStart(BYTE_HEX_WIDTH, BYTE_PADDING)
        }

    companion object {
        fun fromValue(value: BigDecimal): TiBasicDebugNumericValue =
            TiBasicDebugNumericValue(
                value = value,
                internalBytes = tiBasicRadix100Number(value)?.bytes ?: ZERO_NUMERIC_BYTES,
            )
    }
}

internal data class TiBasicDebugStringValue(
    val text: String,
    val internalBytes: List<Int>,
) {
    val internalDisplay: String
        get() = internalBytes.mapIndexed { index, byte ->
            if (index > LENGTH_BYTE_INDEX && byte in PRINTABLE_ASCII_RANGE) {
                byte.toChar().toString()
            } else {
                byte.toString(BYTE_RADIX)
                    .uppercase()
                    .padStart(BYTE_HEX_WIDTH, BYTE_PADDING)
            }
        }
            .joinToString(BYTE_SEPARATOR)

    companion object {
        fun fromText(text: String): TiBasicDebugStringValue {
            val truncatedText = text.take(MAX_TI_BASIC_STRING_LENGTH)
            val bytes = buildList {
                add(truncatedText.length)
                truncatedText.forEach { character ->
                    add(character.code and BYTE_MASK)
                }
            }
            return TiBasicDebugStringValue(
                text = truncatedText,
                internalBytes = bytes,
            )
        }
    }
}

internal data class TiBasicDebugStringEvaluation(
    val value: TiBasicDebugStringValue,
    val warningMessage: String? = null,
    val initializedStringVariables: Map<String, TiBasicDebugStringValue> = emptyMap(),
    val initializedNumericVariables: Map<String, TiBasicDebugNumericValue> = emptyMap(),
) {
    companion object {
        fun fromLiteral(text: String): TiBasicDebugStringEvaluation {
            val truncatedText = text.take(MAX_TI_BASIC_STRING_LENGTH)
            return TiBasicDebugStringEvaluation(
                value = TiBasicDebugStringValue.fromText(truncatedText),
                warningMessage = if (text.length > MAX_TI_BASIC_STRING_LENGTH) {
                    TiBasicDebugMetadata.message(TiBasicDebugMetadata.stringCutTo255CharactersKey)
                } else {
                    null
                },
            )
        }

        fun fromExistingValue(value: TiBasicDebugStringValue): TiBasicDebugStringEvaluation =
            TiBasicDebugStringEvaluation(value = value)
    }
}

internal data class TiBasicDebugNumericEvaluation(
    val value: TiBasicDebugNumericValue,
    val warningMessage: String? = null,
    val initializedNumericVariables: Map<String, TiBasicDebugNumericValue> = emptyMap(),
    val initializedStringVariables: Map<String, TiBasicDebugStringValue> = emptyMap(),
)

internal data class TiBasicDebugKeyboardRequest(
    val mode: Int,
    val allowedCodesDisplay: String,
    val scanInput: String,
)

private fun TiBasicDebugStringEvaluation.mergeWarnings(vararg warnings: String?): TiBasicDebugStringEvaluation =
    copy(
        warningMessage = (listOfNotNull(warningMessage) + warnings.filterNotNull())
            .distinct()
            .joinToString(WARNING_SEPARATOR)
            .ifEmpty { null },
    )

private fun TiBasicDebugStringEvaluation.mergeWith(
    left: TiBasicDebugStringEvaluation,
    right: TiBasicDebugStringEvaluation,
): TiBasicDebugStringEvaluation =
    copy(
        initializedStringVariables = left.initializedStringVariables + right.initializedStringVariables,
        initializedNumericVariables = left.initializedNumericVariables + right.initializedNumericVariables,
    ).mergeWarnings(left.warningMessage, right.warningMessage)

private fun TiBasicDebugStringEvaluation.mergeWith(
    source: TiBasicDebugStringEvaluation,
): TiBasicDebugStringEvaluation =
    copy(
        initializedStringVariables = source.initializedStringVariables,
        initializedNumericVariables = source.initializedNumericVariables,
    ).mergeWarnings(source.warningMessage)

private fun TiBasicDebugStringEvaluation.mergeWith(
    vararg numericEvaluations: TiBasicDebugNumericEvaluation,
): TiBasicDebugStringEvaluation =
    copy(
        initializedNumericVariables = numericEvaluations
            .flatMap { it.initializedNumericVariables.entries }
            .associate { it.toPair() } + initializedNumericVariables,
        warningMessage = (listOfNotNull(warningMessage) + numericEvaluations.mapNotNull(TiBasicDebugNumericEvaluation::warningMessage))
            .distinct()
            .joinToString(WARNING_SEPARATOR)
            .ifEmpty { null },
    )

private fun TiBasicDebugStringEvaluation.truncateForReuse(): TiBasicDebugStringEvaluation {
    val truncationWarning =
        if (value.text.length > MAX_TI_BASIC_STRING_LENGTH) {
            TiBasicDebugMetadata.message(TiBasicDebugMetadata.stringCutTo255CharactersKey)
        } else {
            null
        }
    return copy(
        value = TiBasicDebugStringValue.fromText(value.text),
    ).mergeWarnings(truncationWarning)
}

private fun TiBasicDebugNumericEvaluation.mapValue(transform: (TiBasicDebugNumericValue) -> BigDecimal): TiBasicDebugNumericEvaluation =
    copy(value = TiBasicDebugNumericValue.fromValue(transform(value)))

private fun TiBasicDebugNumericEvaluation.mergeWith(
    left: TiBasicDebugNumericEvaluation,
    right: TiBasicDebugNumericEvaluation,
): TiBasicDebugNumericEvaluation =
    copy(
        initializedNumericVariables = left.initializedNumericVariables + right.initializedNumericVariables,
        initializedStringVariables = left.initializedStringVariables + right.initializedStringVariables,
        warningMessage = (listOfNotNull(warningMessage, left.warningMessage, right.warningMessage))
            .distinct()
            .joinToString(WARNING_SEPARATOR)
            .ifEmpty { null },
    )

private fun TiBasicDebugNumericEvaluation.mergeWith(
    stringEvaluation: TiBasicDebugStringEvaluation,
): TiBasicDebugNumericEvaluation =
    copy(
        initializedNumericVariables = initializedNumericVariables + stringEvaluation.initializedNumericVariables,
        initializedStringVariables = initializedStringVariables + stringEvaluation.initializedStringVariables,
        warningMessage = (listOfNotNull(warningMessage, stringEvaluation.warningMessage))
            .distinct()
            .joinToString(WARNING_SEPARATOR)
            .ifEmpty { null },
    )

private fun TiBasicDebugNumericEvaluation.mergeWith(
    source: TiBasicDebugStringEvaluation,
    target: TiBasicDebugStringEvaluation,
    start: TiBasicDebugNumericEvaluation,
): TiBasicDebugNumericEvaluation =
    copy(
        initializedNumericVariables = initializedNumericVariables +
                source.initializedNumericVariables +
                target.initializedNumericVariables +
                start.initializedNumericVariables,
        initializedStringVariables = initializedStringVariables +
                source.initializedStringVariables +
                target.initializedStringVariables +
                start.initializedStringVariables,
        warningMessage = (listOfNotNull(warningMessage, source.warningMessage, target.warningMessage, start.warningMessage))
            .distinct()
            .joinToString(WARNING_SEPARATOR)
            .ifEmpty { null },
    )

private fun TiBasicLetStatement.isMalformedForDebugger(): Boolean {
    if (targetVariableAccess() == null || assignedExpression() == null) return true
    return node.allChildren.any { child ->
        child.elementType !in setOf(
            TiBasicTokenTypes.LET_KEYWORD,
            TiBasicNodeTypes.VARIABLE_ACCESS,
            TiBasicTokenTypes.EQ_OP,
            TiBasicNodeTypes.EXPRESSION,
            TokenType.WHITE_SPACE,
        )
    }
}

private const val FIRST_PROGRAM_INDEX = 0
private const val FIRST_CALL_KEY_WRITE_ARG_INDEX = 1
private const val FIRST_CONCAT_RIGHT_OPERAND_INDEX = 2
private const val CONCAT_GROUP_SIZE = 2
private const val CALL_KEY_ARG_COUNT = 3
private const val KEYBOARD_MODE_ARG_INDEX = 0
private const val KEY_CODE_ARG_INDEX = 1
private const val KEY_STATUS_ARG_INDEX = 2
private const val SEG_DOLLAR_ARG_COUNT = 3
private const val SEG_SOURCE_ARG_INDEX = 0
private const val SEG_START_ARG_INDEX = 1
private const val SEG_LENGTH_ARG_INDEX = 2
private const val LENGTH_BYTE_INDEX = 0
private const val SINGLE_LINE_NUMBER_NODE_COUNT = 1
private const val SINGLE_NUMERIC_CHILD_COUNT = 1
private const val SINGLE_OPERAND_CHILD_COUNT = 1
private const val SINGLE_STACK_FRAME = 1
private const val PRESERVE_TRAILING_EMPTY_LINES = -1
private const val LINE_SEPARATOR_REGEX = "\n"
private const val STRING_VARIABLE_SUFFIX = "$"
private const val NUMERIC_BYTE_PREFIX = ">"
private const val CHR_DOLLAR_FUNCTION = "CHR$"
private const val KEY_SUBPROGRAM_NAME = "KEY"
private const val SEG_DOLLAR_FUNCTION = "SEG$"
private const val STR_DOLLAR_FUNCTION = "STR$"
private const val ASC_FUNCTION = "ASC"
private const val LEN_FUNCTION = "LEN"
private const val POS_FUNCTION = "POS"
private const val VAL_FUNCTION = "VAL"
private const val POS_ARG_COUNT = 3
private const val POS_SOURCE_ARG_INDEX = 0
private const val POS_TARGET_ARG_INDEX = 1
private const val POS_START_ARG_INDEX = 2
private const val FIRST_INNER_NODE_INDEX = 1
private const val MAX_TI_BASIC_STRING_LENGTH = 255
private const val BYTE_MASK = 0xFF
private const val BYTE_RADIX = 16
private const val BYTE_HEX_WIDTH = 2
private const val BYTE_PADDING = '0'
private const val BYTE_SEPARATOR = " "
private const val EMPTY_STRING = ""
private const val DEFAULT_CALL_KEY_SCAN_INPUT = "-1"
private const val DEFAULT_KEYBOARD_MODE = 5
private const val NO_KEY_CODE = -1
private const val REUSE_LAST_KEYBOARD_MODE = 0
private const val WARNING_SEPARATOR = " | "
private val ZERO_NUMERIC_BYTES = List(8) { 0 }
private val PRINTABLE_ASCII_RANGE = 32..126
private val DEBUG_MATH_CONTEXT = MathContext.DECIMAL64
private val ADDITIVE_OPERATOR_TYPES = setOf(
    TiBasicTokenTypes.PLUS_OP,
    TiBasicTokenTypes.MINUS_OP,
)
private val MULTIPLICATIVE_OPERATOR_TYPES = setOf(
    TiBasicTokenTypes.MUL_OP,
    TiBasicTokenTypes.DIV_OP,
)
private val POWER_OPERATOR_TYPES = setOf(TiBasicTokenTypes.POW_OP)
private val VALID_CALL_KEY_MODES = 0..5
private val CALL_KEY_MODE_1_AND_2_CODES = 0..19
private val CALL_KEY_MODE_3_CODES = (1..15) + (32..95)
private val CALL_KEY_MODE_4_CODES = 1..143
private val CALL_KEY_MODE_5_CODES = (1..15) + (32..159) + listOf(187)

private fun Int.isEven(): Boolean = this % 2 == 0

private fun BigDecimal.toIntExactOrNull(): Int? =
    runCatching { intValueExact() }.getOrNull()

private fun BigDecimal.roundToWholeNumberIntOrNull(): Int? =
    setScale(0, RoundingMode.HALF_UP).toIntExactOrNull()

private fun defaultKeyboardScanInput(semantics: TiBasicDebugLineSemantics): String =
    if (semantics is TiBasicDebugLineSemantics.CallKey) DEFAULT_CALL_KEY_SCAN_INPUT else EMPTY_STRING

private fun isAllowedCallKeyCode(mode: Int, code: Int): Boolean =
    when (mode) {
        1, 2 -> code in CALL_KEY_MODE_1_AND_2_CODES
        3 -> code in CALL_KEY_MODE_3_CODES
        4 -> code in CALL_KEY_MODE_4_CODES
        5 -> code in CALL_KEY_MODE_5_CODES
        else -> false
    }

private fun callKeyAllowedCodesDisplay(mode: Int): String =
    when (mode) {
        1, 2 -> "0-19"
        3 -> "1-15, 32-95"
        4 -> "1-143"
        5 -> "1-15, 32-159, 187"
        else -> ""
    }

private fun String.segment(start: Int, length: Int): String =
    if (start <= 0 || start > this.length || length <= 0) {
        EMPTY_STRING
    } else {
        drop(start - 1).take(length)
    }

private fun String.pos(target: String, start: Int): Int {
    val fromIndex = (start - 1).coerceAtLeast(0)
    if (fromIndex >= length) return 0
    val index = indexOf(target, fromIndex)
    return if (index >= 0) index + 1 else 0
}
