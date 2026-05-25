package com.github.mmrsic.idea.plugins.tibasic.ide.debug

import com.github.mmrsic.idea.plugins.tibasic.common.ext.allChildren
import com.github.mmrsic.idea.plugins.tibasic.common.ext.nonWhitespaceChildren
import com.github.mmrsic.idea.plugins.tibasic.editor.CALL_SOUND_SUBPROGRAM
import com.github.mmrsic.idea.plugins.tibasic.editor.MAX_SOUND_VOLUME
import com.github.mmrsic.idea.plugins.tibasic.editor.SOUND_TONE3_CHANNEL_INDEX
import com.github.mmrsic.idea.plugins.tibasic.editor.TiBasicNoiseShiftRate
import com.github.mmrsic.idea.plugins.tibasic.editor.TiBasicSoundNoise
import com.github.mmrsic.idea.plugins.tibasic.editor.TiBasicSoundPlayback
import com.github.mmrsic.idea.plugins.tibasic.editor.TiBasicSoundTone
import com.github.mmrsic.idea.plugins.tibasic.editor.callColorCharacterSetRange
import com.github.mmrsic.idea.plugins.tibasic.editor.displayedScreenBackground
import com.github.mmrsic.idea.plugins.tibasic.editor.isPlayableSoundPlayback
import com.github.mmrsic.idea.plugins.tibasic.editor.roundedScreenColorAt
import com.github.mmrsic.idea.plugins.tibasic.language.model.TiColor
import com.github.mmrsic.idea.plugins.tibasic.language.analysis.UNARY_EXPRESSION_OPERATOR_TYPES
import com.github.mmrsic.idea.plugins.tibasic.language.analysis.firstTopLevelBinaryOperatorIndex
import com.github.mmrsic.idea.plugins.tibasic.language.analysis.isFullyParenthesized
import com.github.mmrsic.idea.plugins.tibasic.language.analysis.lastTopLevelBinaryOperatorIndex
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.parser.TiBasicNodeTypes
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.common.VALID_LINE_NUMBER_RANGE
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicCallStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicFunctionCall
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicVariableAccess
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicEndStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicForStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicGosubStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicGotoStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicIfStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicPrintStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicLetStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicNextStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicRemStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicReturnStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicStopStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicUnknownStatement
import com.github.mmrsic.idea.plugins.tibasic.language.runtime.screen.TI_BASIC_SCREEN_COLUMNS
import com.github.mmrsic.idea.plugins.tibasic.language.runtime.screen.TI_BASIC_SCREEN_ROWS
import com.github.mmrsic.idea.plugins.tibasic.language.runtime.screen.TI_BASIC_SPACE_CHARACTER_CODE
import com.github.mmrsic.idea.plugins.tibasic.language.values.parseTiBasicDecimalLiteral
import com.github.mmrsic.idea.plugins.tibasic.language.values.tiBasicDecimalString
import com.github.mmrsic.idea.plugins.tibasic.language.values.tiBasicRadix100Number
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
import kotlin.math.abs

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
                screenContents = TiBasicDebugScreenContents(),
            )
        }

    fun nextHigherProgramIndex(afterLineNumber: Int): Int? =
        programLines.indexOfFirst { it.lineNumber > afterLineNumber }
            .takeIf { it >= 0 }

    fun nextHigherNonRemProgramIndex(afterLineNumber: Int): Int? =
        programLines.indexOfFirst { line ->
            line.lineNumber > afterLineNumber && line.semantics != TiBasicDebugLineSemantics.Rem
        }.takeIf { it >= 0 }

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
            is TiBasicRemStatement -> TiBasicDebugLineSemantics.Rem
            is TiBasicIfStatement -> createIfSemantics(statement)
            is TiBasicForStatement -> createForSemantics(statement)
            is TiBasicNextStatement -> createNextSemantics(statement)
            is TiBasicPrintStatement -> createPrintSemantics(statement)
            is TiBasicLetStatement -> createLetSemantics(statement)
            is TiBasicCallStatement -> createCallSemantics(statement)
            else -> TiBasicDebugLineSemantics.Sequential
        }

        private fun createCallSemantics(statement: TiBasicCallStatement): TiBasicDebugLineSemantics {
            return when (statement.subprogramName()) {
                KEY_SUBPROGRAM_NAME -> createCallKeySemantics(statement)
                CLEAR_SUBPROGRAM_NAME -> if (statement.arguments().isEmpty()) TiBasicDebugLineSemantics.CallClear else TiBasicDebugLineSemantics.IncorrectStatement
                COLOR_SUBPROGRAM_NAME -> createCallColorSemantics(statement)
                SCREEN_SUBPROGRAM_NAME -> createCallScreenSemantics(statement)
                CALL_SOUND_SUBPROGRAM -> createCallSoundSemantics(statement)
                else -> TiBasicDebugLineSemantics.Sequential
            }
        }

        private fun createCallScreenSemantics(statement: TiBasicCallStatement): TiBasicDebugLineSemantics {
            if (!statement.hasArgumentParens() || !statement.hasClosingArgumentParen()) {
                return TiBasicDebugLineSemantics.IncorrectStatement
            }
            val argument = statement.arguments().singleOrNull() ?: return TiBasicDebugLineSemantics.IncorrectStatement
            val colorAssignment = when (val result = createRequiredNumericAssignment(argument.node)) {
                is TiBasicDebugParseResult.Valid -> result.value
                TiBasicDebugParseResult.StringNumberMismatch -> return TiBasicDebugLineSemantics.StringNumberMismatch
                TiBasicDebugParseResult.Invalid -> return TiBasicDebugLineSemantics.IncorrectStatement
            }
            return TiBasicDebugLineSemantics.CallScreen(colorAssignment)
        }

        private fun createCallColorSemantics(statement: TiBasicCallStatement): TiBasicDebugLineSemantics {
            if (!statement.hasArgumentParens() || !statement.hasClosingArgumentParen()) {
                return TiBasicDebugLineSemantics.IncorrectStatement
            }
            val arguments = statement.arguments()
            if (arguments.size != CALL_COLOR_ARG_COUNT) {
                return TiBasicDebugLineSemantics.IncorrectStatement
            }
            val characterSetAssignment = when (val result = createRequiredNumericAssignment(arguments[CALL_COLOR_SET_ARG_INDEX].node)) {
                is TiBasicDebugParseResult.Valid -> result.value
                TiBasicDebugParseResult.StringNumberMismatch -> return TiBasicDebugLineSemantics.StringNumberMismatch
                TiBasicDebugParseResult.Invalid -> return TiBasicDebugLineSemantics.IncorrectStatement
            }
            val foregroundAssignment = when (val result = createRequiredNumericAssignment(arguments[CALL_COLOR_FOREGROUND_ARG_INDEX].node)) {
                is TiBasicDebugParseResult.Valid -> result.value
                TiBasicDebugParseResult.StringNumberMismatch -> return TiBasicDebugLineSemantics.StringNumberMismatch
                TiBasicDebugParseResult.Invalid -> return TiBasicDebugLineSemantics.IncorrectStatement
            }
            val backgroundAssignment = when (val result = createRequiredNumericAssignment(arguments[CALL_COLOR_BACKGROUND_ARG_INDEX].node)) {
                is TiBasicDebugParseResult.Valid -> result.value
                TiBasicDebugParseResult.StringNumberMismatch -> return TiBasicDebugLineSemantics.StringNumberMismatch
                TiBasicDebugParseResult.Invalid -> return TiBasicDebugLineSemantics.IncorrectStatement
            }
            return TiBasicDebugLineSemantics.CallColor(
                characterSetAssignment = characterSetAssignment,
                foregroundAssignment = foregroundAssignment,
                backgroundAssignment = backgroundAssignment,
            )
        }

        private fun createCallSoundSemantics(statement: TiBasicCallStatement): TiBasicDebugLineSemantics {
            if (!statement.hasArgumentParens() || !statement.hasClosingArgumentParen()) {
                return TiBasicDebugLineSemantics.IncorrectStatement
            }
            val arguments = statement.arguments()
            if (arguments.size !in VALID_SOUND_ARGUMENT_COUNTS) {
                return TiBasicDebugLineSemantics.IncorrectStatement
            }
            val durationAssignment = when (val result = createRequiredNumericAssignment(arguments[CALL_SOUND_DURATION_ARG_INDEX].node)) {
                is TiBasicDebugParseResult.Valid -> result.value
                TiBasicDebugParseResult.StringNumberMismatch -> return TiBasicDebugLineSemantics.StringNumberMismatch
                TiBasicDebugParseResult.Invalid -> return TiBasicDebugLineSemantics.IncorrectStatement
            }
            val channels = arguments
                .drop(CALL_SOUND_CHANNEL_ARGS_START_INDEX)
                .chunked(CALL_SOUND_CHANNEL_ARGUMENT_COUNT)
                .map { channelArgs ->
                    val pitchAssignment = when (val result = channelArgs.getOrNull(CALL_SOUND_PITCH_ARG_OFFSET)?.node?.let(::createRequiredNumericAssignment)) {
                        is TiBasicDebugParseResult.Valid -> result.value
                        TiBasicDebugParseResult.StringNumberMismatch -> return TiBasicDebugLineSemantics.StringNumberMismatch
                        else -> return TiBasicDebugLineSemantics.IncorrectStatement
                    }
                    val volumeAssignment = when (val result = channelArgs.getOrNull(CALL_SOUND_VOLUME_ARG_OFFSET)?.node?.let(::createRequiredNumericAssignment)) {
                        is TiBasicDebugParseResult.Valid -> result.value
                        TiBasicDebugParseResult.StringNumberMismatch -> return TiBasicDebugLineSemantics.StringNumberMismatch
                        else -> return TiBasicDebugLineSemantics.IncorrectStatement
                    }
                    TiBasicDebugSoundChannelAssignment(
                        pitchAssignment = pitchAssignment,
                        volumeAssignment = volumeAssignment,
                    )
                }
            return TiBasicDebugLineSemantics.CallSound(
                durationAssignment = durationAssignment,
                channels = channels,
            )
        }

        private fun createCallKeySemantics(statement: TiBasicCallStatement): TiBasicDebugLineSemantics {
            if (!statement.hasArgumentParens() || !statement.hasClosingArgumentParen()) {
                return TiBasicDebugLineSemantics.IncorrectStatement
            }
            val arguments = statement.arguments()
            if (arguments.size != CALL_KEY_ARG_COUNT) {
                return TiBasicDebugLineSemantics.IncorrectStatement
            }
            val modeAssignment = when (val result = createRequiredNumericAssignment(arguments[KEYBOARD_MODE_ARG_INDEX].node)) {
                is TiBasicDebugParseResult.Valid -> result.value
                TiBasicDebugParseResult.StringNumberMismatch -> return TiBasicDebugLineSemantics.StringNumberMismatch
                TiBasicDebugParseResult.Invalid -> return TiBasicDebugLineSemantics.IncorrectStatement
            }
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
                val assignment = when (val result = createRequiredStringAssignment(expression.node)) {
                    is TiBasicDebugParseResult.Valid -> result.value
                    TiBasicDebugParseResult.StringNumberMismatch -> return TiBasicDebugLineSemantics.StringNumberMismatch
                    TiBasicDebugParseResult.Invalid -> return TiBasicDebugLineSemantics.Sequential
                }
                TiBasicDebugLineSemantics.LetString(targetName, assignment)
            } else {
                val assignment = when (val result = createRequiredNumericAssignment(expression.node)) {
                    is TiBasicDebugParseResult.Valid -> result.value
                    TiBasicDebugParseResult.StringNumberMismatch -> return TiBasicDebugLineSemantics.StringNumberMismatch
                    TiBasicDebugParseResult.Invalid -> return TiBasicDebugLineSemantics.IncorrectStatement
                }
                TiBasicDebugLineSemantics.LetNumeric(targetName, assignment)
            }
        }

        private fun createIfSemantics(statement: TiBasicIfStatement): TiBasicDebugLineSemantics {
            val conditionExpression = statement.conditionExpression() ?: return TiBasicDebugLineSemantics.IncorrectStatement
            val thenLineNumber = statement.thenLineNumber() ?: return TiBasicDebugLineSemantics.IncorrectStatement
            val elseKeywordPresent = statement.node.nonWhitespaceChildren.any { it.elementType == TiBasicTokenTypes.ELSE_KEYWORD }
            val elseLineNumber = statement.elseLineNumber()
            if (elseKeywordPresent && elseLineNumber == null) {
                return TiBasicDebugLineSemantics.IncorrectStatement
            }
            val condition = when (val result = createCondition(conditionExpression.node)) {
                is TiBasicDebugParseResult.Valid -> result.value
                TiBasicDebugParseResult.StringNumberMismatch -> return TiBasicDebugLineSemantics.StringNumberMismatch
                TiBasicDebugParseResult.Invalid -> return TiBasicDebugLineSemantics.IncorrectStatement
            }
            return TiBasicDebugLineSemantics.If(
                condition = condition,
                thenLineNumber = thenLineNumber,
                elseLineNumber = elseLineNumber,
            )
        }

        private fun createForSemantics(statement: TiBasicForStatement): TiBasicDebugLineSemantics {
            val initialValueAssignment = when (val result = statement.startExpression()?.node?.let(::createRequiredNumericAssignment)) {
                is TiBasicDebugParseResult.Valid -> result.value
                TiBasicDebugParseResult.StringNumberMismatch -> return TiBasicDebugLineSemantics.StringNumberMismatch
                else -> return TiBasicDebugLineSemantics.IncorrectStatement
            }
            val limitAssignment = when (val result = statement.endExpression()?.node?.let(::createRequiredNumericAssignment)) {
                is TiBasicDebugParseResult.Valid -> result.value
                TiBasicDebugParseResult.StringNumberMismatch -> return TiBasicDebugLineSemantics.StringNumberMismatch
                else -> return TiBasicDebugLineSemantics.IncorrectStatement
            }
            val incrementAssignment = when (val stepExpression = statement.stepExpression()) {
                null -> TiBasicDebugNumericAssignment.Literal(BigDecimal.ONE)
                else -> when (val result = createRequiredNumericAssignment(stepExpression.node)) {
                    is TiBasicDebugParseResult.Valid -> result.value
                    TiBasicDebugParseResult.StringNumberMismatch -> return TiBasicDebugLineSemantics.StringNumberMismatch
                    TiBasicDebugParseResult.Invalid -> return TiBasicDebugLineSemantics.IncorrectStatement
                }
            }
            return TiBasicDebugLineSemantics.For(
                controlVariableName = statement.controlVariableName() ?: return TiBasicDebugLineSemantics.IncorrectStatement,
                initialValueAssignment = initialValueAssignment,
                limitAssignment = limitAssignment,
                incrementAssignment = incrementAssignment,
            )
        }

        private fun createNextSemantics(statement: TiBasicNextStatement): TiBasicDebugLineSemantics =
            statement.controlVariableName()
                ?.let(TiBasicDebugLineSemantics::Next)
                ?: TiBasicDebugLineSemantics.Sequential

        private fun createPrintSemantics(statement: TiBasicPrintStatement): TiBasicDebugLineSemantics {
            if (statement.isFileOutput()) return TiBasicDebugLineSemantics.Sequential
            val items = statement.node.nonWhitespaceChildren
                .drop(1)
                .mapNotNull(::createPrintItem)
            return TiBasicDebugLineSemantics.Print(items)
        }

        private fun createCondition(expressionNode: ASTNode): TiBasicDebugParseResult<TiBasicDebugCondition> {
            val children = expressionNode.nonWhitespaceChildren
            if (children.isEmpty()) return TiBasicDebugParseResult.Invalid
            firstTopLevelBinaryOperatorIndex(children, RELATIONAL_OPERATOR_TYPES)?.let { operatorIndex ->
                val operatorType = children[operatorIndex].elementType
                val leftNodes = children.subList(0, operatorIndex)
                val rightNodes = children.subList(operatorIndex + 1, children.size)
                val leftNumeric = createNumericAssignmentFromNodes(leftNodes)
                val rightNumeric = createNumericAssignmentFromNodes(rightNodes)
                if (leftNumeric != null && rightNumeric != null) {
                    return TiBasicDebugParseResult.Valid(
                        TiBasicDebugCondition.NumericComparison(
                        left = leftNumeric,
                        operatorType = operatorType,
                        right = rightNumeric,
                        ),
                    )
                }
                val leftString = createStringAssignmentFromNodes(leftNodes)
                val rightString = createStringAssignmentFromNodes(rightNodes)
                if (leftString != null && rightString != null) {
                    return TiBasicDebugParseResult.Valid(
                        TiBasicDebugCondition.StringComparison(
                        left = leftString,
                        operatorType = operatorType,
                        right = rightString,
                        ),
                    )
                }
                return if (
                    hasNumericContextMismatch(leftNodes) ||
                    hasNumericContextMismatch(rightNodes) ||
                    hasStringContextMismatch(leftNodes) ||
                    hasStringContextMismatch(rightNodes)
                ) {
                    TiBasicDebugParseResult.StringNumberMismatch
                } else {
                    TiBasicDebugParseResult.Invalid
                }
            }
            return when (val result = createRequiredNumericAssignmentFromNodes(children)) {
                is TiBasicDebugParseResult.Valid -> TiBasicDebugParseResult.Valid(TiBasicDebugCondition.NumericValue(result.value))
                TiBasicDebugParseResult.StringNumberMismatch -> TiBasicDebugParseResult.StringNumberMismatch
                TiBasicDebugParseResult.Invalid -> TiBasicDebugParseResult.Invalid
            }
        }

        private fun createPrintItem(node: ASTNode): TiBasicDebugPrintItem? =
            when {
                node.elementType in TiBasicTokenTypes.PRINT_SEPARATORS ->
                    TiBasicDebugPrintItem.Separator(node.elementType)

                node.elementType == TiBasicNodeTypes.EXPRESSION ->
                    createStringAssignmentFromExpression(node)?.let(TiBasicDebugPrintItem::StringValue)
                        ?: createNumericAssignmentFromExpression(node)?.let(TiBasicDebugPrintItem::NumericValue)

                else -> null
            }

        private fun createStringAssignmentFromExpression(expressionNode: ASTNode): TiBasicDebugStringAssignment? {
            val children = expressionNode.nonWhitespaceChildren
            return createStringAssignmentFromNodes(children)
        }

        private fun createStringAssignmentFromNodes(children: List<ASTNode>): TiBasicDebugStringAssignment? {
            if (children.isEmpty()) return null
            if (isFullyParenthesized(children)) {
                return createStringAssignmentFromNodes(children.subList(FIRST_INNER_NODE_INDEX, children.lastIndex))
            }
            if (children.size == SINGLE_OPERAND_CHILD_COUNT) {
                return createStringAssignment(children.single())
            }
            if (children.size.isEven() || children.indices.any { index ->
                    if (index.isEven()) {
                        createStringAssignmentFromNodes(listOf(children[index])) == null
                    } else {
                        children[index].elementType != TiBasicTokenTypes.CONCAT_OP
                    }
                }
            ) {
                return null
            }
            var assignment = createStringAssignmentFromNodes(listOf(children.first())) ?: return null
            var index = FIRST_CONCAT_RIGHT_OPERAND_INDEX
            while (index < children.size) {
                val rightOperand = createStringAssignmentFromNodes(listOf(children[index])) ?: return null
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

        private fun createRequiredNumericAssignment(expressionNode: ASTNode): TiBasicDebugParseResult<TiBasicDebugNumericAssignment> =
            createRequiredNumericAssignmentFromNodes(expressionNode.nonWhitespaceChildren)

        private fun createRequiredNumericAssignmentFromNodes(nodes: List<ASTNode>): TiBasicDebugParseResult<TiBasicDebugNumericAssignment> =
            createNumericAssignmentFromNodes(nodes)
                ?.let { TiBasicDebugParseResult.Valid(it) }
                ?: if (hasNumericContextMismatch(nodes)) {
                    TiBasicDebugParseResult.StringNumberMismatch
                } else {
                    TiBasicDebugParseResult.Invalid
                }

        private fun createRequiredStringAssignment(expressionNode: ASTNode): TiBasicDebugParseResult<TiBasicDebugStringAssignment> =
            createStringAssignmentFromExpression(expressionNode)
                ?.let { TiBasicDebugParseResult.Valid(it) }
                ?: if (hasStringContextMismatch(expressionNode.nonWhitespaceChildren)) {
                    TiBasicDebugParseResult.StringNumberMismatch
                } else {
                    TiBasicDebugParseResult.Invalid
                }

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
                (statement as? TiBasicForStatement)
                    ?.node
                    ?.allChildren
                    ?.firstOrNull { node -> node.elementType == TiBasicNodeTypes.VARIABLE_ACCESS }
                    ?.psi
                    ?.let { psi -> psi as? TiBasicVariableAccess }
                    ?.takeUnless(TiBasicVariableAccess::hasSubscriptParens)
                    ?.let(::add)
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
    val lastSoundTone3Pitch: Int? = null,
    val screenContents: TiBasicDebugScreenContents = TiBasicDebugScreenContents(),
) {
    val currentProgramLine: TiBasicDebugProgramLine?
        get() = currentProgramIndex?.let(snapshot.programLines::get)

    val currentSourceLineIndex: Int?
        get() = currentProgramLine?.sourceLineIndex

    val currentArgumentDisplays: List<String>
        get() = currentProgramLine?.let(::argumentDisplaysFor).orEmpty()

    val currentArgumentsDisplay: String
        get() = currentArgumentDisplays.joinToString(ARGUMENT_DISPLAY_SEPARATOR)

    val keyboardRequest: TiBasicDebugKeyboardRequest?
        get() = keyboardRequestForInput(keyboardScanInput)

    internal fun keyboardRequestForInput(scanInputText: String): TiBasicDebugKeyboardRequest? {
        if (status != TiBasicDebugSessionStatus.Paused) return null
        val programLine = currentProgramLine ?: return null
        val semantics = programLine.semantics as? TiBasicDebugLineSemantics.CallKey ?: return null
        val preparedSession = initializeReferencedNumericVariables(programLine.referencedNumericVariableNames)
        val modeEvaluation = preparedSession.evaluateNumericAssignment(semantics.modeAssignment) ?: return null
        val roundedMode = modeEvaluation.value.value.roundToWholeNumberIntOrNull() ?: return null
        val resolvedMode = preparedSession.resolveCallKeyMode(roundedMode) ?: return null
        val effectiveScanInput = scanInputText.ifEmpty { defaultKeyboardScanInput(programLine.semantics) }
        val roundedScanInput = parseTiBasicDecimalLiteral(effectiveScanInput)?.roundToWholeNumberIntOrNull()
        return TiBasicDebugKeyboardRequest(
            keyUnit = resolvedMode,
            allowedCodesDisplay = callKeyAllowedCodesDisplay(resolvedMode),
            keyCodeVariableName = semantics.keyCodeVariableName,
            statusVariableName = semantics.statusVariableName,
            scanInput = effectiveScanInput,
            statusValueDisplay = when {
                roundedScanInput == null -> UNKNOWN_KEYBOARD_STATUS_DISPLAY
                roundedScanInput == NO_KEY_CODE -> ZERO_KEYBOARD_STATUS_DISPLAY
                isAllowedCallKeyCode(resolvedMode, roundedScanInput) -> ONE_KEYBOARD_STATUS_DISPLAY
                else -> UNKNOWN_KEYBOARD_STATUS_DISPLAY
            },
        )
    }

    private fun argumentDisplaysFor(programLine: TiBasicDebugProgramLine): List<String> =
        when {
            programLine.isCallScreenLine() -> callScreenArgumentDisplays(programLine)
            programLine.isCallColorLine() -> callColorArgumentDisplays(programLine)
            programLine.isIfLine() -> ifArgumentDisplays(programLine)
            programLine.isForLine() -> forArgumentDisplays(programLine)
            programLine.isNextLine() -> nextArgumentDisplays(programLine)
            else -> emptyList()
        }

    private fun callScreenArgumentDisplays(programLine: TiBasicDebugProgramLine): List<String> =
        when (val semantics = programLine.semantics) {
            is TiBasicDebugLineSemantics.CallScreen -> {
                val preparedSession = initializeReferencedNumericVariables(programLine.referencedNumericVariableNames)
                listOf(
                    preparedSession.numericArgumentDisplay(
                        label = SCREEN_COLOR_ARGUMENT_NAME,
                        assignment = semantics.colorAssignment,
                        showsColorName = true,
                        showsLabelForIncorrectExpression = false,
                    ),
                )
            }

            TiBasicDebugLineSemantics.StringNumberMismatch ->
                listOf("$INCORRECT_EXPRESSION_DISPLAY ($STRING_NUMBER_MISMATCH_DISPLAY)")

            TiBasicDebugLineSemantics.IncorrectStatement -> listOf(INCORRECT_EXPRESSION_DISPLAY)
            else -> emptyList()
        }

    private fun forArgumentDisplays(programLine: TiBasicDebugProgramLine): List<String> =
        when (val semantics = programLine.semantics) {
            is TiBasicDebugLineSemantics.For -> {
                val preparedSession = initializeReferencedNumericVariables(programLine.referencedNumericVariableNames)
                listOf(
                    preparedSession.numericArgumentDisplay(
                        label = FOR_INITIAL_VALUE_ARGUMENT_NAME,
                        assignment = semantics.initialValueAssignment,
                    ),
                    preparedSession.numericArgumentDisplay(
                        label = FOR_LIMIT_ARGUMENT_NAME,
                        assignment = semantics.limitAssignment,
                    ),
                    preparedSession.numericArgumentDisplay(
                        label = FOR_INCREMENT_ARGUMENT_NAME,
                        assignment = semantics.incrementAssignment,
                    ),
                ) + preparedSession.forIterationCountDisplay(semantics)
            }

            TiBasicDebugLineSemantics.StringNumberMismatch ->
                listOf("$INCORRECT_EXPRESSION_DISPLAY ($STRING_NUMBER_MISMATCH_DISPLAY)")

            TiBasicDebugLineSemantics.IncorrectStatement -> listOf(INCORRECT_EXPRESSION_DISPLAY)
            else -> emptyList()
        }

    private fun nextArgumentDisplays(programLine: TiBasicDebugProgramLine): List<String> =
        when (val semantics = programLine.semantics) {
            is TiBasicDebugLineSemantics.Next -> {
                val preparedSession = initializeReferencedNumericVariables(programLine.referencedNumericVariableNames)
                val matchingForContext = preparedSession.matchingForContext(programLine.lineNumber, semantics.controlVariableName)
                    ?: return emptyList()
                val nextPreview = preparedSession.nextPreview(semantics.controlVariableName, matchingForContext)
                    ?: return listOf(INCORRECT_EXPRESSION_DISPLAY)
                listOf(
                    preparedSession.numericArgumentDisplay(
                        label = FOR_INCREMENT_ARGUMENT_NAME,
                        assignment = matchingForContext.semantics.incrementAssignment,
                    ),
                    "$NEXT_CONTROL_VARIABLE_ARGUMENT_NAME ${semantics.controlVariableName} = ${nextPreview.updatedValueDisplay}",
                    "$FOR_LIMIT_ARGUMENT_NAME = ${nextPreview.limitDisplay} (${nextPreview.decisionDisplay})",
                )
            }

            TiBasicDebugLineSemantics.StringNumberMismatch ->
                listOf("$INCORRECT_EXPRESSION_DISPLAY ($STRING_NUMBER_MISMATCH_DISPLAY)")

            TiBasicDebugLineSemantics.IncorrectStatement -> listOf(INCORRECT_EXPRESSION_DISPLAY)
            else -> emptyList()
        }

    private fun callColorArgumentDisplays(programLine: TiBasicDebugProgramLine): List<String> =
        when (val semantics = programLine.semantics) {
            is TiBasicDebugLineSemantics.CallColor -> {
                val preparedSession = initializeReferencedNumericVariables(programLine.referencedNumericVariableNames)
                listOf(
                    preparedSession.numericArgumentDisplay(
                        label = CHARACTER_SET_ARGUMENT_NAME,
                        assignment = semantics.characterSetAssignment,
                    ),
                    preparedSession.numericArgumentDisplay(
                        label = FOREGROUND_COLOR_ARGUMENT_NAME,
                        assignment = semantics.foregroundAssignment,
                        showsColorName = true,
                    ),
                    preparedSession.numericArgumentDisplay(
                        label = BACKGROUND_COLOR_ARGUMENT_NAME,
                        assignment = semantics.backgroundAssignment,
                        showsColorName = true,
                    ),
                )
            }

            TiBasicDebugLineSemantics.StringNumberMismatch ->
                listOf("$INCORRECT_EXPRESSION_DISPLAY ($STRING_NUMBER_MISMATCH_DISPLAY)")

            TiBasicDebugLineSemantics.IncorrectStatement -> listOf(INCORRECT_EXPRESSION_DISPLAY)
            else -> emptyList()
        }

    private fun ifArgumentDisplays(programLine: TiBasicDebugProgramLine): List<String> =
        when (val semantics = programLine.semantics) {
            is TiBasicDebugLineSemantics.If -> {
                val preparedSession = initializeReferencedNumericVariables(programLine.referencedNumericVariableNames)
                preparedSession.conditionTrace(semantics.condition)?.lines ?: listOf(INCORRECT_EXPRESSION_DISPLAY)
            }

            TiBasicDebugLineSemantics.StringNumberMismatch ->
                listOf("$INCORRECT_EXPRESSION_DISPLAY ($STRING_NUMBER_MISMATCH_DISPLAY)")

            TiBasicDebugLineSemantics.IncorrectStatement -> listOf(INCORRECT_EXPRESSION_DISPLAY)
            else -> emptyList()
        }

    private fun numericArgumentDisplay(
        label: String,
        assignment: TiBasicDebugNumericAssignment,
        showsColorName: Boolean = false,
        showsLabelForIncorrectExpression: Boolean = true,
    ): String {
        val incorrectExpressionDisplay =
            if (showsLabelForIncorrectExpression) "$label = $INCORRECT_EXPRESSION_DISPLAY" else INCORRECT_EXPRESSION_DISPLAY
        val evaluation = evaluateNumericAssignment(assignment) ?: return incorrectExpressionDisplay
        val roundedCode = evaluation.value.value.roundToWholeNumberIntOrNull() ?: return incorrectExpressionDisplay
        val valueDisplay = roundedCode.twoDigitDisplay()
        if (!showsColorName) {
            return "$label = $valueDisplay"
        }
        val colorName = runCatching { TiColor.at(roundedCode).displayName }
            .getOrElse { INVALID_COLOR_CODE_DISPLAY }
        return "$label = $valueDisplay ($colorName)"
    }

    private fun forIterationCountDisplay(semantics: TiBasicDebugLineSemantics.For): String {
        val initialEvaluation = evaluateNumericAssignment(semantics.initialValueAssignment)
        val limitEvaluation = evaluateNumericAssignment(semantics.limitAssignment)
        val incrementEvaluation = evaluateNumericAssignment(semantics.incrementAssignment)
        val initialValue = initialEvaluation?.value?.value?.roundToWholeNumberIntOrNull()
        val limitValue = limitEvaluation?.value?.value?.roundToWholeNumberIntOrNull()
        val incrementValue = incrementEvaluation?.value?.value?.roundToWholeNumberIntOrNull()
        val iterationCount = if (initialValue == null || limitValue == null || incrementValue == null || incrementValue == 0) {
            INCORRECT_EXPRESSION_DISPLAY
        } else {
            forIterationCount(initialValue, limitValue, incrementValue).toString()
        }
        return "($FOR_ITERATION_COUNT_ARGUMENT_NAME = $iterationCount)"
    }

    private fun nextPreview(
        controlVariableName: String,
        matchingForContext: TiBasicDebugForContext,
    ): TiBasicDebugNextPreview? {
        val currentValue = evaluateNumericAssignment(TiBasicDebugNumericAssignment.VariableReference(controlVariableName)) ?: return null
        val incrementValue = evaluateNumericAssignment(matchingForContext.semantics.incrementAssignment) ?: return null
        val limitValue = evaluateNumericAssignment(matchingForContext.semantics.limitAssignment) ?: return null
        val updatedValue = currentValue.value.value + incrementValue.value.value
        val continuesLoop = continuesForLoop(updatedValue, limitValue.value.value, incrementValue.value.value)
        return TiBasicDebugNextPreview(
            updatedValue = updatedValue,
            updatedValueDisplay = tiBasicDecimalString(updatedValue),
            limitDisplay = limitValue.value.value.roundToWholeNumberIntOrNull()?.twoDigitDisplay()
                ?: INCORRECT_EXPRESSION_DISPLAY,
            continuesLoop = continuesLoop,
            decisionDisplay = if (continuesLoop) {
                matchingForContext.returnLineNumber?.let { returnLineNumber -> "$NEXT_DECISION_JUMP_PREFIX $returnLineNumber" }
                    ?: NEXT_DECISION_END_DISPLAY
            } else {
                NEXT_DECISION_END_DISPLAY
            },
            warningMessage = mergeWarningMessages(
                currentValue.warningMessage,
                incrementValue.warningMessage,
                limitValue.warningMessage,
            ),
            initializedNumericVariables = currentValue.initializedNumericVariables +
                incrementValue.initializedNumericVariables +
                limitValue.initializedNumericVariables,
            initializedStringVariables = currentValue.initializedStringVariables +
                incrementValue.initializedStringVariables +
                limitValue.initializedStringVariables,
        )
    }

    private fun matchingForContext(
        nextLineNumber: Int,
        controlVariableName: String,
    ): TiBasicDebugForContext? =
        snapshot.programLines
            .asSequence()
            .filter { programLine -> programLine.lineNumber < nextLineNumber }
            .filter { programLine ->
                (programLine.semantics as? TiBasicDebugLineSemantics.For)?.controlVariableName == controlVariableName
            }
            .lastOrNull()
            ?.let { forProgramLine ->
                TiBasicDebugForContext(
                    semantics = forProgramLine.semantics as TiBasicDebugLineSemantics.For,
                    forLineNumber = forProgramLine.lineNumber,
                    returnLineNumber = snapshot.nextHigherNonRemProgramIndex(forProgramLine.lineNumber)
                        ?.let(snapshot.programLines::get)
                        ?.lineNumber,
                )
            }

    private fun conditionTrace(condition: TiBasicDebugCondition): TiBasicDebugConditionTrace? =
        when (condition) {
            is TiBasicDebugCondition.NumericValue -> {
                val numericTrace = numericTrace(condition.assignment) ?: return null
                TiBasicDebugConditionTrace(
                    lines = numericTrace.lines + tracedEvaluationLine(
                        expressionDisplay = numericTrace.valueDisplay,
                        resultDisplay = conditionResultDisplay(numericTrace.evaluation.value.value.compareTo(BigDecimal.ZERO) != 0),
                    ),
                )
            }

            is TiBasicDebugCondition.NumericComparison -> {
                val leftTrace = numericTrace(condition.left) ?: return null
                val rightTrace = numericTrace(condition.right) ?: return null
                val conditionResult = compareNumericValues(
                    leftTrace.evaluation.value.value,
                    condition.operatorType,
                    rightTrace.evaluation.value.value,
                ) ?: return null
                TiBasicDebugConditionTrace(
                    lines = leftTrace.lines +
                        rightTrace.lines +
                        tracedEvaluationLine(
                            expressionDisplay = "${leftTrace.valueDisplay} ${operatorDisplay(condition.operatorType)} ${rightTrace.valueDisplay}",
                            resultDisplay = conditionResultDisplay(conditionResult),
                        ),
                )
            }

            is TiBasicDebugCondition.StringComparison -> {
                val leftTrace = stringTrace(condition.left) ?: return null
                val rightTrace = stringTrace(condition.right) ?: return null
                val conditionResult = compareStringValues(
                    leftTrace.evaluation.value.text,
                    condition.operatorType,
                    rightTrace.evaluation.value.text,
                ) ?: return null
                TiBasicDebugConditionTrace(
                    lines = leftTrace.lines +
                        rightTrace.lines +
                        tracedEvaluationLine(
                            expressionDisplay = "${leftTrace.valueDisplay} ${operatorDisplay(condition.operatorType)} ${rightTrace.valueDisplay}",
                            resultDisplay = conditionResultDisplay(conditionResult),
                        ),
                )
            }
        }

    private fun stringTrace(assignment: TiBasicDebugStringAssignment): TiBasicDebugStringTrace? {
        val evaluation = evaluateStringAssignment(assignment) ?: return null
        return when (assignment) {
            is TiBasicDebugStringAssignment.StringLiteral,
            is TiBasicDebugStringAssignment.StringVariableReference,
                -> TiBasicDebugStringTrace(
                lines = emptyList(),
                valueDisplay = stringLiteralDisplay(evaluation.value.text),
                evaluation = evaluation,
            )

            is TiBasicDebugStringAssignment.CharacterCode -> {
                val codeTrace = numericTrace(assignment.code) ?: return null
                TiBasicDebugStringTrace(
                    lines = codeTrace.lines + tracedEvaluationLine(
                        expressionDisplay = "$CHR_DOLLAR_FUNCTION(${codeTrace.valueDisplay})",
                        resultDisplay = stringLiteralDisplay(evaluation.value.text),
                    ),
                    valueDisplay = stringLiteralDisplay(evaluation.value.text),
                    evaluation = evaluation,
                )
            }

            is TiBasicDebugStringAssignment.StringRepresentation -> {
                val valueTrace = numericTrace(assignment.value) ?: return null
                TiBasicDebugStringTrace(
                    lines = valueTrace.lines + tracedEvaluationLine(
                        expressionDisplay = "$STR_DOLLAR_FUNCTION(${valueTrace.valueDisplay})",
                        resultDisplay = stringLiteralDisplay(evaluation.value.text),
                    ),
                    valueDisplay = stringLiteralDisplay(evaluation.value.text),
                    evaluation = evaluation,
                )
            }

            is TiBasicDebugStringAssignment.Segment -> {
                val sourceTrace = stringTrace(assignment.source) ?: return null
                val startTrace = numericTrace(assignment.start) ?: return null
                val lengthTrace = numericTrace(assignment.length) ?: return null
                TiBasicDebugStringTrace(
                    lines = sourceTrace.lines +
                        startTrace.lines +
                        lengthTrace.lines +
                        tracedEvaluationLine(
                            expressionDisplay = "$SEG_DOLLAR_FUNCTION(${sourceTrace.valueDisplay}, ${startTrace.valueDisplay}, ${lengthTrace.valueDisplay})",
                            resultDisplay = stringLiteralDisplay(evaluation.value.text),
                        ),
                    valueDisplay = stringLiteralDisplay(evaluation.value.text),
                    evaluation = evaluation,
                )
            }

            is TiBasicDebugStringAssignment.Concat -> {
                val leftTrace = stringTrace(assignment.left) ?: return null
                val rightTrace = stringTrace(assignment.right) ?: return null
                TiBasicDebugStringTrace(
                    lines = leftTrace.lines +
                        rightTrace.lines +
                        tracedEvaluationLine(
                            expressionDisplay = "${leftTrace.valueDisplay} ${operatorDisplay(TiBasicTokenTypes.CONCAT_OP)} ${rightTrace.valueDisplay}",
                            resultDisplay = stringLiteralDisplay(evaluation.value.text),
                        ),
                    valueDisplay = stringLiteralDisplay(evaluation.value.text),
                    evaluation = evaluation,
                )
            }
        }
    }

    private fun numericTrace(assignment: TiBasicDebugNumericAssignment): TiBasicDebugNumericTrace? {
        val evaluation = evaluateNumericAssignment(assignment) ?: return null
        return when (assignment) {
            is TiBasicDebugNumericAssignment.Literal,
            is TiBasicDebugNumericAssignment.VariableReference,
                -> TiBasicDebugNumericTrace(
                lines = emptyList(),
                valueDisplay = evaluation.value.usualDisplay,
                evaluation = evaluation,
            )

            is TiBasicDebugNumericAssignment.Unary -> {
                val operandTrace = numericTrace(assignment.operand) ?: return null
                TiBasicDebugNumericTrace(
                    lines = operandTrace.lines + tracedEvaluationLine(
                        expressionDisplay = "${operatorDisplay(assignment.operatorType)}${operandTrace.valueDisplay}",
                        resultDisplay = evaluation.value.usualDisplay,
                    ),
                    valueDisplay = evaluation.value.usualDisplay,
                    evaluation = evaluation,
                )
            }

            is TiBasicDebugNumericAssignment.Binary -> {
                val leftTrace = numericTrace(assignment.left) ?: return null
                val rightTrace = numericTrace(assignment.right) ?: return null
                TiBasicDebugNumericTrace(
                    lines = leftTrace.lines +
                        rightTrace.lines +
                        tracedEvaluationLine(
                            expressionDisplay = "${leftTrace.valueDisplay} ${operatorDisplay(assignment.operatorType)} ${rightTrace.valueDisplay}",
                            resultDisplay = evaluation.value.usualDisplay,
                        ),
                    valueDisplay = evaluation.value.usualDisplay,
                    evaluation = evaluation,
                )
            }

            is TiBasicDebugNumericAssignment.StringLength -> {
                val sourceTrace = stringTrace(assignment.source) ?: return null
                TiBasicDebugNumericTrace(
                    lines = sourceTrace.lines + tracedEvaluationLine(
                        expressionDisplay = "$LEN_FUNCTION(${sourceTrace.valueDisplay})",
                        resultDisplay = evaluation.value.usualDisplay,
                    ),
                    valueDisplay = evaluation.value.usualDisplay,
                    evaluation = evaluation,
                )
            }

            is TiBasicDebugNumericAssignment.AsciiCode -> {
                val sourceTrace = stringTrace(assignment.source) ?: return null
                TiBasicDebugNumericTrace(
                    lines = sourceTrace.lines + tracedEvaluationLine(
                        expressionDisplay = "$ASC_FUNCTION(${sourceTrace.valueDisplay})",
                        resultDisplay = evaluation.value.usualDisplay,
                    ),
                    valueDisplay = evaluation.value.usualDisplay,
                    evaluation = evaluation,
                )
            }

            is TiBasicDebugNumericAssignment.StringToNumber -> {
                val sourceTrace = stringTrace(assignment.source) ?: return null
                TiBasicDebugNumericTrace(
                    lines = sourceTrace.lines + tracedEvaluationLine(
                        expressionDisplay = "$VAL_FUNCTION(${sourceTrace.valueDisplay})",
                        resultDisplay = evaluation.value.usualDisplay,
                    ),
                    valueDisplay = evaluation.value.usualDisplay,
                    evaluation = evaluation,
                )
            }

            is TiBasicDebugNumericAssignment.StringPosition -> {
                val sourceTrace = stringTrace(assignment.source) ?: return null
                val targetTrace = stringTrace(assignment.target) ?: return null
                val startTrace = numericTrace(assignment.start) ?: return null
                TiBasicDebugNumericTrace(
                    lines = sourceTrace.lines +
                        targetTrace.lines +
                        startTrace.lines +
                        tracedEvaluationLine(
                            expressionDisplay = "$POS_FUNCTION(${sourceTrace.valueDisplay}, ${targetTrace.valueDisplay}, ${startTrace.valueDisplay})",
                            resultDisplay = evaluation.value.usualDisplay,
                        ),
                    valueDisplay = evaluation.value.usualDisplay,
                    evaluation = evaluation,
                )
            }
        }
    }

    fun step(): TiBasicDebugSession = stepWithEffects().session

    internal fun stepWithEffects(): TiBasicDebugStepResult = when (status) {
        TiBasicDebugSessionStatus.Paused -> stepPaused()
        TiBasicDebugSessionStatus.PendingStop ->
            TiBasicDebugStepResult(
                copy(status = TiBasicDebugSessionStatus.Stopped, currentProgramIndex = null, keyboardScanInput = EMPTY_STRING),
            )

        TiBasicDebugSessionStatus.Stopped -> TiBasicDebugStepResult(this)
    }

    fun stop(): TiBasicDebugSession =
        copy(status = TiBasicDebugSessionStatus.Stopped, currentProgramIndex = null, keyboardScanInput = EMPTY_STRING)

    private fun stepPaused(): TiBasicDebugStepResult {
        val programLine = currentProgramLine ?: return stop()
            .let(::TiBasicDebugStepResult)
        val sessionWithInitializedNumericVariables = initializeReferencedNumericVariables(programLine.referencedNumericVariableNames)
        return when (val semantics = programLine.semantics) {
            TiBasicDebugLineSemantics.Sequential -> TiBasicDebugStepResult(sessionWithInitializedNumericVariables.continueAfter(programLine.lineNumber))
            TiBasicDebugLineSemantics.Rem -> TiBasicDebugStepResult(sessionWithInitializedNumericVariables.continueAfter(programLine.lineNumber))
            is TiBasicDebugLineSemantics.Goto -> TiBasicDebugStepResult(sessionWithInitializedNumericVariables.jumpTo(programLine, semantics.target))
            is TiBasicDebugLineSemantics.Gosub -> TiBasicDebugStepResult(sessionWithInitializedNumericVariables.jumpTo(programLine, semantics.target, rememberOrigin = true))
            is TiBasicDebugLineSemantics.If -> TiBasicDebugStepResult(sessionWithInitializedNumericVariables.applyIf(programLine.lineNumber, semantics))
            is TiBasicDebugLineSemantics.For -> TiBasicDebugStepResult(sessionWithInitializedNumericVariables.applyFor(programLine.lineNumber, semantics))
            is TiBasicDebugLineSemantics.Next -> TiBasicDebugStepResult(sessionWithInitializedNumericVariables.applyNext(programLine.lineNumber, semantics))
            is TiBasicDebugLineSemantics.Return -> TiBasicDebugStepResult(sessionWithInitializedNumericVariables.returnFrom(semantics.isStandaloneKeyword))
            is TiBasicDebugLineSemantics.End -> TiBasicDebugStepResult(sessionWithInitializedNumericVariables.pendingStopIf(semantics.isStandaloneKeyword))
            is TiBasicDebugLineSemantics.Stop -> TiBasicDebugStepResult(sessionWithInitializedNumericVariables.pendingStopIf(semantics.isStandaloneKeyword))
            is TiBasicDebugLineSemantics.LetString -> TiBasicDebugStepResult(sessionWithInitializedNumericVariables.applyStringLet(programLine.lineNumber, semantics))
            is TiBasicDebugLineSemantics.LetNumeric -> TiBasicDebugStepResult(sessionWithInitializedNumericVariables.applyNumericLet(programLine.lineNumber, semantics))
            is TiBasicDebugLineSemantics.Print -> TiBasicDebugStepResult(sessionWithInitializedNumericVariables.applyPrint(programLine.lineNumber, semantics))
            is TiBasicDebugLineSemantics.CallKey -> TiBasicDebugStepResult(sessionWithInitializedNumericVariables.applyCallKey(programLine.lineNumber, semantics))
            TiBasicDebugLineSemantics.CallClear -> TiBasicDebugStepResult(sessionWithInitializedNumericVariables.applyCallClear(programLine.lineNumber))
            is TiBasicDebugLineSemantics.CallColor -> TiBasicDebugStepResult(sessionWithInitializedNumericVariables.applyCallColor(programLine.lineNumber, semantics))
            is TiBasicDebugLineSemantics.CallScreen -> TiBasicDebugStepResult(sessionWithInitializedNumericVariables.applyCallScreen(programLine.lineNumber, semantics))
            is TiBasicDebugLineSemantics.CallSound -> sessionWithInitializedNumericVariables.applyCallSound(programLine.lineNumber, semantics)
            TiBasicDebugLineSemantics.StringNumberMismatch -> TiBasicDebugStepResult(stringNumberMismatch())
            TiBasicDebugLineSemantics.IncorrectStatement -> TiBasicDebugStepResult(incorrectStatement())
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
        snapshot.nextHigherNonRemProgramIndex(currentLineNumber)?.let { nextIndex ->
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

    private fun applyIf(
        currentLineNumber: Int,
        semantics: TiBasicDebugLineSemantics.If,
    ): TiBasicDebugSession {
        val conditionEvaluation = evaluateCondition(semantics.condition) ?: return incorrectStatement()
        val sessionAfterCondition = mergeEvaluations(conditionEvaluation)
        return if (conditionEvaluation.value) {
            sessionAfterCondition.jumpToLineNumber(semantics.thenLineNumber)
        } else {
            semantics.elseLineNumber
                ?.let(sessionAfterCondition::jumpToLineNumber)
                ?: sessionAfterCondition.continueAfter(currentLineNumber)
        }
    }

    private fun applyFor(
        currentLineNumber: Int,
        semantics: TiBasicDebugLineSemantics.For,
    ): TiBasicDebugSession {
        val evaluation = evaluateNumericAssignment(semantics.initialValueAssignment) ?: return continueAfter(currentLineNumber)
        val updatedNumericVariables = numericVariables + evaluation.initializedNumericVariables + (semantics.controlVariableName to evaluation.value)
        val updatedStringVariables = stringVariables + evaluation.initializedStringVariables
        return continueAfter(currentLineNumber, evaluation.warningMessage).copy(
            numericVariables = updatedNumericVariables,
            stringVariables = updatedStringVariables,
        )
    }

    private fun applyNext(
        currentLineNumber: Int,
        semantics: TiBasicDebugLineSemantics.Next,
    ): TiBasicDebugSession {
        val matchingForContext = matchingForContext(currentLineNumber, semantics.controlVariableName)
            ?: return continueAfter(currentLineNumber)
        val nextPreview = nextPreview(semantics.controlVariableName, matchingForContext) ?: return continueAfter(currentLineNumber)
        val updatedNumericVariables = numericVariables +
            nextPreview.initializedNumericVariables +
            (semantics.controlVariableName to TiBasicDebugNumericValue.fromValue(nextPreview.updatedValue))
        val updatedStringVariables = stringVariables +
            nextPreview.initializedStringVariables
        val sessionAfterUpdate = copy(
            numericVariables = updatedNumericVariables,
            stringVariables = updatedStringVariables,
            statusMessage = nextPreview.warningMessage ?: statusMessage,
        )
        return if (nextPreview.continuesLoop) {
            matchingForContext.returnLineNumber
                ?.let(sessionAfterUpdate::jumpToLineNumber)
                ?.copy(statusMessage = nextPreview.warningMessage)
                ?: sessionAfterUpdate.continueAfter(currentLineNumber, nextPreview.warningMessage)
        } else {
            sessionAfterUpdate.continueAfter(currentLineNumber, nextPreview.warningMessage)
        }
    }

    private fun applyPrint(
        currentLineNumber: Int,
        semantics: TiBasicDebugLineSemantics.Print,
    ): TiBasicDebugSession {
        var currentSession = this
        var trailingSeparator: IElementType? = null
        semantics.items.forEach { item ->
            when (item) {
                is TiBasicDebugPrintItem.StringValue -> {
                    val evaluation = currentSession.evaluateStringAssignment(item.assignment) ?: return currentSession.continueAfter(currentLineNumber)
                    currentSession = currentSession
                        .mergeEvaluations(evaluation)
                        .writePrintText(evaluation.value.text)
                    trailingSeparator = null
                }

                is TiBasicDebugPrintItem.NumericValue -> {
                    val evaluation = currentSession.evaluateNumericAssignment(item.assignment) ?: return currentSession.continueAfter(currentLineNumber)
                    currentSession = currentSession
                        .mergeEvaluations(evaluation)
                        .writePrintText(evaluation.value.usualDisplay)
                    trailingSeparator = null
                }

                is TiBasicDebugPrintItem.Separator -> {
                    currentSession = currentSession.applyPrintSeparator(item.tokenType)
                    trailingSeparator = item.tokenType
                }
            }
        }
        if (trailingSeparator !in TiBasicTokenTypes.PRINT_SEPARATORS) {
            currentSession = currentSession.applyPrintSeparator(TiBasicTokenTypes.COLON)
        }
        return currentSession.continueAfter(currentLineNumber)
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

    private fun applyCallClear(currentLineNumber: Int): TiBasicDebugSession =
        continueAfter(currentLineNumber).copy(
            screenContents = screenContents.copy(
                characterCodes = blankDebugScreenCharacterCodes(),
                printCursorRow = INITIAL_PRINT_CURSOR_ROW,
                printCursorColumn = INITIAL_PRINT_CURSOR_COLUMN,
            ),
        )

    private fun applyCallColor(
        currentLineNumber: Int,
        semantics: TiBasicDebugLineSemantics.CallColor,
    ): TiBasicDebugSession {
        var currentSession = this
        val characterSet = currentSession.evaluateColorArgument(
            argumentName = CHARACTER_SET_ARGUMENT_NAME,
            assignment = semantics.characterSetAssignment,
            allowedValues = VALID_CALL_COLOR_CHARACTER_SETS,
        )
        if (characterSet.session.status == TiBasicDebugSessionStatus.PendingStop) return characterSet.session
        currentSession = characterSet.session
        val foreground = currentSession.evaluateColorArgument(
            argumentName = FOREGROUND_COLOR_ARGUMENT_NAME,
            assignment = semantics.foregroundAssignment,
            allowedValues = VALID_CALL_COLOR_COLOR_CODES,
        )
        if (foreground.session.status == TiBasicDebugSessionStatus.PendingStop) return foreground.session
        currentSession = foreground.session
        val background = currentSession.evaluateColorArgument(
            argumentName = BACKGROUND_COLOR_ARGUMENT_NAME,
            assignment = semantics.backgroundAssignment,
            allowedValues = VALID_CALL_COLOR_COLOR_CODES,
        )
        if (background.session.status == TiBasicDebugSessionStatus.PendingStop) return background.session
        currentSession = background.session
        return currentSession.continueAfter(currentLineNumber).copy(
            screenContents = currentSession.screenContents.copy(
                characterSetColors = currentSession.screenContents.characterSetColors +
                    (characterSet.value to TiBasicDebugCharacterSetColors(
                        fg = TiColor.at(foreground.value),
                        bg = TiColor.at(background.value),
                    )),
            ),
        )
    }

    private fun applyCallScreen(
        currentLineNumber: Int,
        semantics: TiBasicDebugLineSemantics.CallScreen,
    ): TiBasicDebugSession {
        val evaluation = evaluateNumericAssignment(semantics.colorAssignment) ?: return incorrectStatement()
        val updatedNumericVariables = numericVariables + evaluation.initializedNumericVariables
        val updatedStringVariables = stringVariables + evaluation.initializedStringVariables
        val screenBackground = roundedScreenColorAt(evaluation.value.value)
            ?.let(::displayedScreenBackground)
            ?: return badValue(evaluation.value.usualDisplay).copy(
                numericVariables = updatedNumericVariables,
                stringVariables = updatedStringVariables,
            )
        return continueAfter(currentLineNumber).copy(
            numericVariables = updatedNumericVariables,
            stringVariables = updatedStringVariables,
            screenContents = screenContents.copy(screenBackground = screenBackground),
        )
    }

    private fun applyCallSound(
        currentLineNumber: Int,
        semantics: TiBasicDebugLineSemantics.CallSound,
    ): TiBasicDebugStepResult {
        var currentSession = this
        val durationEvaluation = currentSession.evaluateNumericAssignment(semantics.durationAssignment)
            ?: return TiBasicDebugStepResult(currentSession.incorrectStatement())
        currentSession = currentSession.mergeEvaluations(durationEvaluation)
        val duration = durationEvaluation.value.value.roundToWholeNumberIntOrNull()
            ?: return TiBasicDebugStepResult(currentSession.badValue(durationEvaluation.value.usualDisplay))
        if (abs(duration) < MIN_SOUND_DURATION) {
            return TiBasicDebugStepResult(currentSession.badValue(durationEvaluation.value.usualDisplay))
        }

        val tones = mutableListOf<TiBasicSoundTone>()
        var explicitTone3Pitch: Int? = null
        var noise: TiBasicSoundNoise? = null
        semantics.channels.forEach { channel ->
            val pitchEvaluation = currentSession.evaluateNumericAssignment(channel.pitchAssignment)
                ?: return TiBasicDebugStepResult(currentSession.incorrectStatement())
            currentSession = currentSession.mergeEvaluations(pitchEvaluation)
            val pitch = pitchEvaluation.value.value.roundToWholeNumberIntOrNull()
                ?: return TiBasicDebugStepResult(currentSession.badValue(pitchEvaluation.value.usualDisplay))

            val volumeEvaluation = currentSession.evaluateNumericAssignment(channel.volumeAssignment)
                ?: return TiBasicDebugStepResult(currentSession.incorrectStatement())
            currentSession = currentSession.mergeEvaluations(volumeEvaluation)
            val volume = volumeEvaluation.value.value.roundToWholeNumberIntOrNull()
                ?: return TiBasicDebugStepResult(currentSession.badValue(volumeEvaluation.value.usualDisplay))
            if (volume !in 0..MAX_SOUND_VOLUME) {
                return TiBasicDebugStepResult(currentSession.badValue(volumeEvaluation.value.usualDisplay))
            }

            when {
                pitch >= MIN_SOUND_PITCH -> {
                    if (tones.size >= MAX_SOUND_TONE_CHANNEL_COUNT) {
                        return TiBasicDebugStepResult(currentSession.badValue(pitchEvaluation.value.usualDisplay))
                    }
                    tones += TiBasicSoundTone(pitch, volume)
                    if (tones.size - 1 == SOUND_TONE3_CHANNEL_INDEX) {
                        explicitTone3Pitch = pitch
                    }
                }

                pitch in MIN_SOUND_NOISE_SELECTOR..MAX_SOUND_NOISE_SELECTOR && noise == null -> {
                    noise = TiBasicSoundNoise(pitch, volume)
                }

                else -> return TiBasicDebugStepResult(currentSession.badValue(pitchEvaluation.value.usualDisplay))
            }
        }

        val resolvedNoise = noise?.let { currentNoise ->
            if (currentNoise.shiftRate == TiBasicNoiseShiftRate.TONE3) {
                currentNoise.copy(tone3Pitch = explicitTone3Pitch ?: currentSession.lastSoundTone3Pitch)
            } else {
                currentNoise
            }
        }
        val playback = TiBasicSoundPlayback(duration = duration, tones = tones.toList(), noise = resolvedNoise)
        if (!isPlayableSoundPlayback(playback)) {
            return TiBasicDebugStepResult(currentSession.badValue(durationEvaluation.value.usualDisplay))
        }
        return TiBasicDebugStepResult(
            session = currentSession.continueAfter(currentLineNumber).copy(
                lastSoundTone3Pitch = explicitTone3Pitch ?: currentSession.lastSoundTone3Pitch,
            ),
            soundPlayback = playback,
        )
    }

    private fun mergeEvaluations(evaluation: TiBasicDebugStringEvaluation): TiBasicDebugSession =
        copy(
            numericVariables = numericVariables + evaluation.initializedNumericVariables,
            stringVariables = stringVariables + evaluation.initializedStringVariables,
            statusMessage = evaluation.warningMessage ?: statusMessage,
        )

    private fun mergeEvaluations(evaluation: TiBasicDebugNumericEvaluation): TiBasicDebugSession =
        copy(
            numericVariables = numericVariables + evaluation.initializedNumericVariables,
            stringVariables = stringVariables + evaluation.initializedStringVariables,
            statusMessage = evaluation.warningMessage ?: statusMessage,
        )

    private fun mergeEvaluations(evaluation: TiBasicDebugConditionEvaluation): TiBasicDebugSession =
        copy(
            numericVariables = numericVariables + evaluation.initializedNumericVariables,
            stringVariables = stringVariables + evaluation.initializedStringVariables,
            statusMessage = evaluation.warningMessage ?: statusMessage,
        )

    private fun applyPrintSeparator(tokenType: IElementType): TiBasicDebugSession =
        when (tokenType) {
            TiBasicTokenTypes.COLON -> copy(screenContents = screenContents.lineFeed())
            TiBasicTokenTypes.SEMICOLON, TiBasicTokenTypes.COMMA -> this
            else -> this
        }

    private fun writePrintText(text: String): TiBasicDebugSession =
        text.fold(this) { session, character -> session.writePrintCharacter(character) }

    private fun writePrintCharacter(character: Char): TiBasicDebugSession {
        val normalizedContents = screenContents.normalizePrintCursor()
        val updatedCodes = normalizedContents.characterCodes.map(List<Int>::toMutableList)
        val rowIndex = normalizedContents.printCursorRow - 1
        val columnIndex = normalizedContents.printCursorColumn - 1
        updatedCodes[rowIndex][columnIndex] = printableScreenCode(character)
        val updatedContents = normalizedContents.copy(
            characterCodes = updatedCodes.map(List<Int>::toList),
        ).advancePrintCursor()
        return copy(screenContents = updatedContents)
    }

    private fun resolveCallKeyMode(roundedMode: Int): Int? =
        when (roundedMode) {
            !in VALID_CALL_KEY_MODES -> null
            REUSE_LAST_KEYBOARD_MODE -> lastKeyboardMode ?: DEFAULT_KEYBOARD_MODE
            else -> roundedMode
        }

    private fun evaluateCondition(condition: TiBasicDebugCondition): TiBasicDebugConditionEvaluation? =
        when (condition) {
            is TiBasicDebugCondition.NumericValue ->
                evaluateNumericAssignment(condition.assignment)
                    ?.let { numericEvaluation ->
                        TiBasicDebugConditionEvaluation(
                            value = numericEvaluation.value.value.compareTo(BigDecimal.ZERO) != 0,
                            initializedNumericVariables = numericEvaluation.initializedNumericVariables,
                            initializedStringVariables = numericEvaluation.initializedStringVariables,
                            warningMessage = numericEvaluation.warningMessage,
                        )
                    }

            is TiBasicDebugCondition.NumericComparison -> {
                val left = evaluateNumericAssignment(condition.left) ?: return null
                val right = evaluateNumericAssignment(condition.right) ?: return null
                TiBasicDebugConditionEvaluation(
                    value = compareNumericValues(left.value.value, condition.operatorType, right.value.value) ?: return null,
                    initializedNumericVariables = left.initializedNumericVariables + right.initializedNumericVariables,
                    initializedStringVariables = left.initializedStringVariables + right.initializedStringVariables,
                    warningMessage = mergeWarningMessages(left.warningMessage, right.warningMessage),
                )
            }

            is TiBasicDebugCondition.StringComparison -> {
                val left = evaluateStringAssignment(condition.left) ?: return null
                val right = evaluateStringAssignment(condition.right) ?: return null
                TiBasicDebugConditionEvaluation(
                    value = compareStringValues(left.value.text, condition.operatorType, right.value.text) ?: return null,
                    initializedNumericVariables = left.initializedNumericVariables + right.initializedNumericVariables,
                    initializedStringVariables = left.initializedStringVariables + right.initializedStringVariables,
                    warningMessage = mergeWarningMessages(left.warningMessage, right.warningMessage),
                )
            }
        }

    private fun evaluateColorArgument(
        argumentName: String,
        assignment: TiBasicDebugNumericAssignment,
        allowedValues: IntRange,
    ): TiBasicDebugValidatedInt {
        val evaluation = evaluateNumericAssignment(assignment)
            ?: return TiBasicDebugValidatedInt(incorrectStatement(), Int.MIN_VALUE)
        val sessionAfterEvaluation = mergeEvaluations(evaluation)
        val roundedValue = evaluation.value.value.roundToWholeNumberIntOrNull()
            ?: return TiBasicDebugValidatedInt(sessionAfterEvaluation.badValue(argumentName, evaluation.value.usualDisplay), Int.MIN_VALUE)
        if (roundedValue !in allowedValues) {
            return TiBasicDebugValidatedInt(sessionAfterEvaluation.badValue(argumentName, roundedValue), roundedValue)
        }
        return TiBasicDebugValidatedInt(sessionAfterEvaluation, roundedValue)
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
        val nextIndex = snapshot.nextHigherNonRemProgramIndex(originLineNumber)
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

    private fun jumpToLineNumber(targetLineNumber: Int): TiBasicDebugSession =
    if (targetLineNumber !in VALID_LINE_NUMBER_RANGE) {
        badLineNumber()
        } else {
        snapshot.lineNumberToProgramIndex[targetLineNumber]
            ?.let { targetIndex -> moveTo(targetIndex) }
            ?: badLineNumber()
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

    private fun badValue(argumentName: String, value: Any): TiBasicDebugSession =
        badValue("$argumentName=$value")

    private fun stringNumberMismatch(): TiBasicDebugSession =
        copy(
            status = TiBasicDebugSessionStatus.PendingStop,
            statusMessage = TiBasicDebugMetadata.message(TiBasicDebugMetadata.stringNumberMismatchKey),
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
    data object Rem : TiBasicDebugLineSemantics
    data object CallClear : TiBasicDebugLineSemantics
    data object StringNumberMismatch : TiBasicDebugLineSemantics
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

    data class If(
        val condition: TiBasicDebugCondition,
        val thenLineNumber: Int,
        val elseLineNumber: Int?,
    ) : TiBasicDebugLineSemantics

    data class For(
        val controlVariableName: String,
        val initialValueAssignment: TiBasicDebugNumericAssignment,
        val limitAssignment: TiBasicDebugNumericAssignment,
        val incrementAssignment: TiBasicDebugNumericAssignment,
    ) : TiBasicDebugLineSemantics

    data class Next(
        val controlVariableName: String,
    ) : TiBasicDebugLineSemantics

    data class Print(
        val items: List<TiBasicDebugPrintItem>,
    ) : TiBasicDebugLineSemantics

    data class CallKey(
        val modeAssignment: TiBasicDebugNumericAssignment,
        val keyCodeVariableName: String,
        val statusVariableName: String,
    ) : TiBasicDebugLineSemantics

    data class CallColor(
        val characterSetAssignment: TiBasicDebugNumericAssignment,
        val foregroundAssignment: TiBasicDebugNumericAssignment,
        val backgroundAssignment: TiBasicDebugNumericAssignment,
    ) : TiBasicDebugLineSemantics

    data class CallScreen(
        val colorAssignment: TiBasicDebugNumericAssignment,
    ) : TiBasicDebugLineSemantics

    data class CallSound(
        val durationAssignment: TiBasicDebugNumericAssignment,
        val channels: List<TiBasicDebugSoundChannelAssignment>,
    ) : TiBasicDebugLineSemantics
}

private fun TiBasicDebugProgramLine.isCallScreenLine(): Boolean =
    sourceText.contains(CALL_SCREEN_LINE_MARKER, ignoreCase = true)

private fun TiBasicDebugProgramLine.isCallColorLine(): Boolean =
    sourceText.contains(CALL_COLOR_LINE_MARKER, ignoreCase = true)

private fun TiBasicDebugProgramLine.isIfLine(): Boolean =
    IF_LINE_REGEX.containsMatchIn(sourceText)

private fun TiBasicDebugProgramLine.isForLine(): Boolean =
    FOR_LINE_REGEX.containsMatchIn(sourceText)

private fun TiBasicDebugProgramLine.isNextLine(): Boolean =
    NEXT_LINE_REGEX.containsMatchIn(sourceText)

private fun Int.twoDigitDisplay(): String = toString().padStart(TWO_DIGIT_DISPLAY_WIDTH, '0')

internal sealed interface TiBasicDebugPrintItem {
    data class StringValue(val assignment: TiBasicDebugStringAssignment) : TiBasicDebugPrintItem
    data class NumericValue(val assignment: TiBasicDebugNumericAssignment) : TiBasicDebugPrintItem
    data class Separator(val tokenType: IElementType) : TiBasicDebugPrintItem
}

internal sealed interface TiBasicDebugCondition {
    data class NumericValue(val assignment: TiBasicDebugNumericAssignment) : TiBasicDebugCondition
    data class NumericComparison(
        val left: TiBasicDebugNumericAssignment,
        val operatorType: IElementType,
        val right: TiBasicDebugNumericAssignment,
    ) : TiBasicDebugCondition

    data class StringComparison(
        val left: TiBasicDebugStringAssignment,
        val operatorType: IElementType,
        val right: TiBasicDebugStringAssignment,
    ) : TiBasicDebugCondition
}

internal data class TiBasicDebugSoundChannelAssignment(
    val pitchAssignment: TiBasicDebugNumericAssignment,
    val volumeAssignment: TiBasicDebugNumericAssignment,
)

internal data class TiBasicDebugStepResult(
    val session: TiBasicDebugSession,
    val soundPlayback: TiBasicSoundPlayback? = null,
)

internal data class TiBasicDebugConditionEvaluation(
    val value: Boolean,
    val initializedNumericVariables: Map<String, TiBasicDebugNumericValue> = emptyMap(),
    val initializedStringVariables: Map<String, TiBasicDebugStringValue> = emptyMap(),
    val warningMessage: String? = null,
)

internal sealed interface TiBasicDebugParseResult<out T> {
    data class Valid<T>(val value: T) : TiBasicDebugParseResult<T>
    data object StringNumberMismatch : TiBasicDebugParseResult<Nothing>
    data object Invalid : TiBasicDebugParseResult<Nothing>
}

internal data class TiBasicDebugValidatedInt(
    val session: TiBasicDebugSession,
    val value: Int,
)

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
    val keyUnit: Int,
    val allowedCodesDisplay: String,
    val keyCodeVariableName: String,
    val statusVariableName: String,
    val scanInput: String,
    val statusValueDisplay: String,
) {
    val mode: Int
        get() = keyUnit
}

internal data class TiBasicDebugConditionTrace(
    val lines: List<String>,
)

internal data class TiBasicDebugStringTrace(
    val lines: List<String>,
    val valueDisplay: String,
    val evaluation: TiBasicDebugStringEvaluation,
)

internal data class TiBasicDebugNumericTrace(
    val lines: List<String>,
    val valueDisplay: String,
    val evaluation: TiBasicDebugNumericEvaluation,
)

internal data class TiBasicDebugForContext(
    val semantics: TiBasicDebugLineSemantics.For,
    val forLineNumber: Int,
    val returnLineNumber: Int?,
)

internal data class TiBasicDebugNextPreview(
    val updatedValue: BigDecimal,
    val updatedValueDisplay: String,
    val limitDisplay: String,
    val continuesLoop: Boolean,
    val decisionDisplay: String,
    val warningMessage: String?,
    val initializedNumericVariables: Map<String, TiBasicDebugNumericValue>,
    val initializedStringVariables: Map<String, TiBasicDebugStringValue>,
)

private fun tracedEvaluationLine(
    expressionDisplay: String,
    resultDisplay: String,
): String = "$expressionDisplay$TRACE_DISPLAY_SEPARATOR$resultDisplay"

private fun conditionResultDisplay(conditionResult: Boolean): String =
    if (conditionResult) TRUE_DISPLAY else FALSE_DISPLAY

private fun stringLiteralDisplay(text: String): String =
    "\"${text.replace("\"", "\"\"")}\""

private fun operatorDisplay(operatorType: IElementType): String =
    when (operatorType) {
        TiBasicTokenTypes.PLUS_OP -> "+"
        TiBasicTokenTypes.MINUS_OP -> "-"
        TiBasicTokenTypes.MUL_OP -> "*"
        TiBasicTokenTypes.DIV_OP -> "/"
        TiBasicTokenTypes.POW_OP -> "^"
        TiBasicTokenTypes.CONCAT_OP -> "&"
        TiBasicTokenTypes.EQ_OP -> "="
        TiBasicTokenTypes.LT_OP -> "<"
        TiBasicTokenTypes.GT_OP -> ">"
        TiBasicTokenTypes.LE_OP -> "<="
        TiBasicTokenTypes.GE_OP -> ">="
        TiBasicTokenTypes.NEQ_OP -> "<>"
        else -> operatorType.toString()
    }

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
private const val CLEAR_SUBPROGRAM_NAME = "CLEAR"
private const val COLOR_SUBPROGRAM_NAME = "COLOR"
private const val SCREEN_SUBPROGRAM_NAME = "SCREEN"
private const val CALL_COLOR_ARG_COUNT = 3
private const val CALL_COLOR_SET_ARG_INDEX = 0
private const val CALL_COLOR_FOREGROUND_ARG_INDEX = 1
private const val CALL_COLOR_BACKGROUND_ARG_INDEX = 2
private const val CALL_SOUND_DURATION_ARG_INDEX = 0
private const val CALL_SOUND_CHANNEL_ARGS_START_INDEX = 1
private const val CALL_SOUND_CHANNEL_ARGUMENT_COUNT = 2
private const val CALL_SOUND_PITCH_ARG_OFFSET = 0
private const val CALL_SOUND_VOLUME_ARG_OFFSET = 1
private const val MAX_SOUND_TONE_CHANNEL_COUNT = 3
private val VALID_SOUND_ARGUMENT_COUNTS = setOf(3, 5, 7, 9)
private const val MIN_SOUND_DURATION = 1
private const val MIN_SOUND_PITCH = 1
private const val MIN_SOUND_NOISE_SELECTOR = -8
private const val MAX_SOUND_NOISE_SELECTOR = -1
private const val SEG_DOLLAR_FUNCTION = "SEG$"
private const val STR_DOLLAR_FUNCTION = "STR$"
private const val ASC_FUNCTION = "ASC"
private const val LEN_FUNCTION = "LEN"
private const val POS_FUNCTION = "POS"
private const val VAL_FUNCTION = "VAL"
private const val CALL_SCREEN_LINE_MARKER = "CALL SCREEN"
private const val CALL_COLOR_LINE_MARKER = "CALL COLOR"
private const val INCORRECT_EXPRESSION_DISPLAY = "<incorrect expression>"
private const val STRING_NUMBER_MISMATCH_DISPLAY = "string-number-mismatch"
private const val INVALID_COLOR_CODE_DISPLAY = "<invalid color code>"
private const val TRACE_DISPLAY_SEPARATOR = " -> "
private const val TRUE_DISPLAY = "true"
private const val FALSE_DISPLAY = "false"
private const val POS_ARG_COUNT = 3
private const val POS_SOURCE_ARG_INDEX = 0
private const val POS_TARGET_ARG_INDEX = 1
private const val POS_START_ARG_INDEX = 2
private const val FIRST_INNER_NODE_INDEX = 1
private const val MAX_TI_BASIC_STRING_LENGTH = 255
private const val ARGUMENT_DISPLAY_SEPARATOR = "\n"
private const val BYTE_MASK = 0xFF
private const val BYTE_RADIX = 16
private const val BYTE_HEX_WIDTH = 2
private const val BYTE_PADDING = '0'
private const val BYTE_SEPARATOR = " "
private const val TWO_DIGIT_DISPLAY_WIDTH = 2
private const val EMPTY_STRING = ""
private const val SCREEN_COLOR_ARGUMENT_NAME = "color-code"
private const val DEFAULT_CALL_KEY_SCAN_INPUT = "-1"
private const val DEFAULT_KEYBOARD_MODE = 5
private const val UNKNOWN_KEYBOARD_STATUS_DISPLAY = "?"
private const val ZERO_KEYBOARD_STATUS_DISPLAY = "0"
private const val ONE_KEYBOARD_STATUS_DISPLAY = "1"
private const val NO_KEY_CODE = -1
private const val REUSE_LAST_KEYBOARD_MODE = 0
private const val PRINT_AREA_END_COLUMN = 30
private const val WARNING_SEPARATOR = " | "
private const val CHARACTER_SET_ARGUMENT_NAME = "character set"
private const val FOREGROUND_COLOR_ARGUMENT_NAME = "foreground color"
private const val BACKGROUND_COLOR_ARGUMENT_NAME = "background color"
private const val FOR_INITIAL_VALUE_ARGUMENT_NAME = "initial-value"
private const val FOR_LIMIT_ARGUMENT_NAME = "limit"
private const val FOR_INCREMENT_ARGUMENT_NAME = "increment"
private const val FOR_ITERATION_COUNT_ARGUMENT_NAME = "iterations"
private const val NEXT_CONTROL_VARIABLE_ARGUMENT_NAME = "control-variable"
private const val NEXT_DECISION_JUMP_PREFIX = "jump to"
private const val NEXT_DECISION_END_DISPLAY = "loop end"
private val ZERO_NUMERIC_BYTES = List(8) { 0 }
private val PRINTABLE_ASCII_RANGE = 32..126
private val DEBUG_MATH_CONTEXT = MathContext.DECIMAL64
private val IF_LINE_REGEX = Regex("""^\s*\d+\s+IF\b""", RegexOption.IGNORE_CASE)
private val FOR_LINE_REGEX = Regex("""^\s*\d+\s+FOR\b""", RegexOption.IGNORE_CASE)
private val NEXT_LINE_REGEX = Regex("""^\s*\d+\s+NEXT\b""", RegexOption.IGNORE_CASE)
private val ADDITIVE_OPERATOR_TYPES = setOf(
    TiBasicTokenTypes.PLUS_OP,
    TiBasicTokenTypes.MINUS_OP,
)
private val MULTIPLICATIVE_OPERATOR_TYPES = setOf(
    TiBasicTokenTypes.MUL_OP,
    TiBasicTokenTypes.DIV_OP,
)
private val POWER_OPERATOR_TYPES = setOf(TiBasicTokenTypes.POW_OP)
private val STRING_CONCAT_OPERATOR_TYPES = setOf(TiBasicTokenTypes.CONCAT_OP)
private val RELATIONAL_OPERATOR_TYPES = setOf(
    TiBasicTokenTypes.EQ_OP,
    TiBasicTokenTypes.LT_OP,
    TiBasicTokenTypes.GT_OP,
    TiBasicTokenTypes.NEQ_OP,
    TiBasicTokenTypes.LE_OP,
    TiBasicTokenTypes.GE_OP,
)
private val VALID_CALL_KEY_MODES = 0..5
private val VALID_CALL_COLOR_CHARACTER_SETS = 1..16
private val VALID_CALL_COLOR_COLOR_CODES = 1..16
private val CALL_KEY_MODE_1_AND_2_CODES = 0..19
private val CALL_KEY_MODE_3_CODES = (1..15) + (32..95)
private val CALL_KEY_MODE_4_CODES = 1..143
private val CALL_KEY_MODE_5_CODES = (1..15) + (32..159) + listOf(187)

private fun Int.isEven(): Boolean = this % 2 == 0

private fun BigDecimal.toIntExactOrNull(): Int? =
    runCatching { intValueExact() }.getOrNull()

private fun BigDecimal.roundToWholeNumberIntOrNull(): Int? =
    setScale(0, RoundingMode.HALF_UP).toIntExactOrNull()

private fun hasNumericContextMismatch(nodes: List<ASTNode>): Boolean {
    if (nodes.isEmpty()) return false
    if (isFullyParenthesized(nodes)) {
        return hasNumericContextMismatch(nodes.subList(FIRST_INNER_NODE_INDEX, nodes.lastIndex))
    }
    lastTopLevelBinaryOperatorIndex(nodes, ADDITIVE_OPERATOR_TYPES + MULTIPLICATIVE_OPERATOR_TYPES)?.let { operatorIndex ->
        return hasNumericContextMismatch(nodes.subList(0, operatorIndex)) ||
            hasNumericContextMismatch(nodes.subList(operatorIndex + 1, nodes.size))
    }
    firstTopLevelBinaryOperatorIndex(nodes, POWER_OPERATOR_TYPES)?.let { operatorIndex ->
        return hasNumericContextMismatch(nodes.subList(0, operatorIndex)) ||
            hasNumericContextMismatch(nodes.subList(operatorIndex + 1, nodes.size))
    }
    return when {
        nodes.size == SINGLE_NUMERIC_CHILD_COUNT -> isStringLikeNode(nodes.single())
        nodes.firstOrNull()?.elementType in UNARY_EXPRESSION_OPERATOR_TYPES ->
            hasNumericContextMismatch(nodes.dropWhile { it.elementType in UNARY_EXPRESSION_OPERATOR_TYPES })

        else -> nodes.any(::isStringLikeNode)
    }
}

private fun hasStringContextMismatch(nodes: List<ASTNode>): Boolean {
    if (nodes.isEmpty()) return false
    if (isFullyParenthesized(nodes)) {
        return hasStringContextMismatch(nodes.subList(FIRST_INNER_NODE_INDEX, nodes.lastIndex))
    }
    lastTopLevelBinaryOperatorIndex(nodes, STRING_CONCAT_OPERATOR_TYPES)?.let { operatorIndex ->
        return hasStringContextMismatch(nodes.subList(0, operatorIndex)) ||
            hasStringContextMismatch(nodes.subList(operatorIndex + 1, nodes.size))
    }
    if (createNumericAssignmentFromNodesForMismatch(nodes) != null) return true
    return when {
        nodes.size == SINGLE_OPERAND_CHILD_COUNT -> isNumericLikeNode(nodes.single())
        else -> nodes.any(::isNumericLikeNode)
    }
}

private fun createNumericAssignmentFromNodesForMismatch(nodes: List<ASTNode>): TiBasicDebugNumericAssignment? {
    if (nodes.isEmpty()) return null
    if (isFullyParenthesized(nodes)) {
        return createNumericAssignmentFromNodesForMismatch(nodes.subList(FIRST_INNER_NODE_INDEX, nodes.lastIndex))
    }
    lastTopLevelBinaryOperatorIndex(nodes, ADDITIVE_OPERATOR_TYPES)?.let { operatorIndex ->
        val left = createNumericAssignmentFromNodesForMismatch(nodes.subList(0, operatorIndex)) ?: return null
        val right = createNumericAssignmentFromNodesForMismatch(nodes.subList(operatorIndex + 1, nodes.size)) ?: return null
        return TiBasicDebugNumericAssignment.Binary(left, nodes[operatorIndex].elementType, right)
    }
    lastTopLevelBinaryOperatorIndex(nodes, MULTIPLICATIVE_OPERATOR_TYPES)?.let { operatorIndex ->
        val left = createNumericAssignmentFromNodesForMismatch(nodes.subList(0, operatorIndex)) ?: return null
        val right = createNumericAssignmentFromNodesForMismatch(nodes.subList(operatorIndex + 1, nodes.size)) ?: return null
        return TiBasicDebugNumericAssignment.Binary(left, nodes[operatorIndex].elementType, right)
    }
    firstTopLevelBinaryOperatorIndex(nodes, POWER_OPERATOR_TYPES)?.let { operatorIndex ->
        val left = createNumericAssignmentFromNodesForMismatch(nodes.subList(0, operatorIndex)) ?: return null
        val right = createNumericAssignmentFromNodesForMismatch(nodes.subList(operatorIndex + 1, nodes.size)) ?: return null
        return TiBasicDebugNumericAssignment.Binary(left, nodes[operatorIndex].elementType, right)
    }
    var unaryOperatorsEndIndex = 0
    while (unaryOperatorsEndIndex < nodes.size && nodes[unaryOperatorsEndIndex].elementType in UNARY_EXPRESSION_OPERATOR_TYPES) {
        unaryOperatorsEndIndex++
    }
    if (unaryOperatorsEndIndex > 0) {
        val operand = createNumericAssignmentFromNodesForMismatch(nodes.subList(unaryOperatorsEndIndex, nodes.size)) ?: return null
        return nodes.take(unaryOperatorsEndIndex)
            .reversed()
            .fold(operand) { assignment, operatorNode ->
                TiBasicDebugNumericAssignment.Unary(operatorNode.elementType, assignment)
            }
    }
    return if (nodes.size == SINGLE_NUMERIC_CHILD_COUNT && isNumericLikeNode(nodes.single())) {
        TiBasicDebugNumericAssignment.Literal(BigDecimal.ZERO)
    } else {
        null
    }
}

private fun isStringLikeNode(node: ASTNode): Boolean =
    when (node.elementType) {
        TiBasicTokenTypes.STRING_LITERAL -> true
        TiBasicNodeTypes.VARIABLE_ACCESS -> node.firstChildNode?.elementType == TiBasicTokenTypes.STRING_VARIABLE
        TiBasicNodeTypes.FUNCTION_CALL -> node.firstChildNode?.elementType == TiBasicTokenTypes.STRING_FUNCTION_KEYWORD
        TiBasicNodeTypes.EXPRESSION -> hasNumericContextMismatch(node.nonWhitespaceChildren)
        else -> false
    }

private fun isNumericLikeNode(node: ASTNode): Boolean =
    when (node.elementType) {
        TiBasicTokenTypes.NUMERIC_LITERAL -> true
        TiBasicNodeTypes.VARIABLE_ACCESS -> node.firstChildNode?.elementType == TiBasicTokenTypes.NUMERIC_VARIABLE
        TiBasicNodeTypes.FUNCTION_CALL -> node.firstChildNode?.elementType == TiBasicTokenTypes.NUMERIC_FUNCTION_KEYWORD
        TiBasicNodeTypes.EXPRESSION -> createNumericAssignmentFromNodesForMismatch(node.nonWhitespaceChildren) != null
        else -> false
    }

private fun compareNumericValues(left: BigDecimal, operatorType: IElementType, right: BigDecimal): Boolean? =
    when (operatorType) {
        TiBasicTokenTypes.EQ_OP -> left.compareTo(right) == 0
        TiBasicTokenTypes.LT_OP -> left < right
        TiBasicTokenTypes.GT_OP -> left > right
        TiBasicTokenTypes.NEQ_OP -> left.compareTo(right) != 0
        TiBasicTokenTypes.LE_OP -> left <= right
        TiBasicTokenTypes.GE_OP -> left >= right
        else -> null
    }

private fun compareStringValues(left: String, operatorType: IElementType, right: String): Boolean? =
    when (operatorType) {
        TiBasicTokenTypes.EQ_OP -> left == right
        TiBasicTokenTypes.LT_OP -> left < right
        TiBasicTokenTypes.GT_OP -> left > right
        TiBasicTokenTypes.NEQ_OP -> left != right
        TiBasicTokenTypes.LE_OP -> left <= right
        TiBasicTokenTypes.GE_OP -> left >= right
        else -> null
    }

private fun forIterationCount(
    initialValue: Int,
    limitValue: Int,
    incrementValue: Int,
): Int =
    when {
        incrementValue > 0 && initialValue > limitValue -> 0
        incrementValue < 0 && initialValue < limitValue -> 0
        incrementValue > 0 -> ((limitValue - initialValue) / incrementValue) + 1
        else -> ((initialValue - limitValue) / -incrementValue) + 1
    }

private fun continuesForLoop(
    updatedValue: BigDecimal,
    limitValue: BigDecimal,
    incrementValue: BigDecimal,
): Boolean =
    when {
        incrementValue > BigDecimal.ZERO -> updatedValue <= limitValue
        incrementValue < BigDecimal.ZERO -> updatedValue >= limitValue
        else -> false
    }

private fun mergeWarningMessages(vararg warnings: String?): String? =
    warnings.filterNotNull()
        .distinct()
        .joinToString(WARNING_SEPARATOR)
        .ifEmpty { null }

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

private fun printableScreenCode(character: Char): Int =
    character.code.takeIf { it in PRINTABLE_ASCII_RANGE } ?: TI_BASIC_SPACE_CHARACTER_CODE

private fun TiBasicDebugScreenContents.normalizePrintCursor(): TiBasicDebugScreenContents {
    var normalized = this
    while (normalized.printCursorRow > TI_BASIC_SCREEN_ROWS) {
        normalized = normalized.scrollPrintArea()
    }
    return normalized
}

private fun TiBasicDebugScreenContents.advancePrintCursor(): TiBasicDebugScreenContents =
    if (printCursorColumn < PRINT_AREA_END_COLUMN) {
        copy(printCursorColumn = printCursorColumn + 1)
    } else {
        lineFeed()
    }

private fun TiBasicDebugScreenContents.lineFeed(): TiBasicDebugScreenContents =
    if (printCursorRow <= TI_BASIC_SCREEN_ROWS) {
        copy(
            printCursorRow = printCursorRow + 1,
            printCursorColumn = INITIAL_PRINT_CURSOR_COLUMN,
        )
    } else {
        scrollPrintArea().copy(
            printCursorRow = TI_BASIC_SCREEN_ROWS + 1,
            printCursorColumn = INITIAL_PRINT_CURSOR_COLUMN,
        )
    }

private fun TiBasicDebugScreenContents.scrollPrintArea(): TiBasicDebugScreenContents =
    copy(
        characterCodes = characterCodes
            .drop(1) + listOf(blankPrintRow()),
        printCursorRow = TI_BASIC_SCREEN_ROWS,
        printCursorColumn = INITIAL_PRINT_CURSOR_COLUMN,
    )

private fun blankPrintRow(): List<Int> =
    List(TI_BASIC_SCREEN_COLUMNS) { columnIndex ->
        when (columnIndex + 1) {
            1, 2, 31, 32 -> TI_BASIC_SPACE_CHARACTER_CODE
            else -> TI_BASIC_SPACE_CHARACTER_CODE
        }
    }

private fun String.pos(target: String, start: Int): Int {
    val fromIndex = (start - 1).coerceAtLeast(0)
    if (fromIndex >= length) return 0
    val index = indexOf(target, fromIndex)
    return if (index >= 0) index + 1 else 0
}
