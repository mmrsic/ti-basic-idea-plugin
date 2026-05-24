package com.github.mmrsic.idea.plugins.tibasic.debug

import com.github.mmrsic.idea.plugins.tibasic.editor.UNARY_EXPRESSION_OPERATOR_TYPES
import com.github.mmrsic.idea.plugins.tibasic.editor.firstTopLevelBinaryOperatorIndex
import com.github.mmrsic.idea.plugins.tibasic.editor.isFullyParenthesized
import com.github.mmrsic.idea.plugins.tibasic.editor.lastTopLevelBinaryOperatorIndex
import com.github.mmrsic.idea.plugins.tibasic.ext.childrenOfType
import com.github.mmrsic.idea.plugins.tibasic.ext.nonWhitespaceChildren
import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicFileType
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.expression.TiBasicExpression
import com.github.mmrsic.idea.plugins.tibasic.psi.expression.TiBasicFunctionCall
import com.github.mmrsic.idea.plugins.tibasic.psi.expression.TiBasicVariableAccess
import com.github.mmrsic.idea.plugins.tibasic.psi.statement.TiBasicPrintStatement
import com.github.mmrsic.idea.plugins.tibasic.util.parseTiBasicDecimalLiteral
import com.github.mmrsic.idea.plugins.tibasic.util.tiBasicDecimalString
import com.intellij.lang.ASTNode
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import java.math.BigDecimal
import java.math.MathContext

internal data class TiBasicDebugInspectResult(val displayText: String)

internal fun inspectExpression(
    project: Project,
    session: TiBasicDebugSession,
    expressionText: String,
): TiBasicDebugInspectResult? =
    ReadAction.compute<TiBasicDebugInspectResult?, RuntimeException> {
        val expression = parseInspectExpression(project, expressionText) ?: return@compute null
        session.inspect(expression)
    }

private fun parseInspectExpression(project: Project, expressionText: String): TiBasicExpression? {
    val file = PsiFileFactory.getInstance(project)
        .createFileFromText(INSPECT_FILE_NAME, TiBasicFileType, "$INSPECT_LINE_NUMBER PRINT $expressionText") as? TiBasicFile
        ?: return null
    val statement = file.lines().singleOrNull()?.statement() as? TiBasicPrintStatement ?: return null
    val significantChildren = statement.node.nonWhitespaceChildren
        .drop(PRINT_ARGUMENTS_OFFSET)
    if (significantChildren.size != SINGLE_INSPECT_CHILD_COUNT || significantChildren.single().elementType != TiBasicNodeTypes.EXPRESSION) {
        return null
    }
    return statement.node.childrenOfType(TiBasicNodeTypes.EXPRESSION)
        .singleOrNull()
        ?.psi as? TiBasicExpression
}

private fun TiBasicDebugSession.inspect(expression: TiBasicExpression): TiBasicDebugInspectResult? {
    val nodes = expression.node.nonWhitespaceChildren
    if (nodes.isEmpty()) return null
    return if (isStringExpression(nodes)) {
        evaluateInspectStringNodes(nodes)?.toInspectResult()
    } else {
        evaluateInspectNumericNodes(nodes)?.let { value ->
            TiBasicDebugInspectResult(tiBasicDecimalString(value))
        }
    }
}

private fun TiBasicDebugSession.evaluateInspectStringNodes(nodes: List<ASTNode>): TiBasicDebugStringEvaluation? {
    if (nodes.isEmpty()) return null
    if (isFullyParenthesized(nodes)) {
        return evaluateInspectStringNodes(nodes.subList(FIRST_INNER_NODE_INDEX, nodes.lastIndex))
    }
    lastTopLevelBinaryOperatorIndex(nodes, STRING_CONCAT_OPERATOR_TYPES)?.let { operatorIndex ->
        val left = evaluateInspectStringNodes(nodes.subList(0, operatorIndex)) ?: return null
        val right = evaluateInspectStringNodes(nodes.subList(operatorIndex + 1, nodes.size)) ?: return null
        return TiBasicDebugStringEvaluation.fromLiteral(left.value.text + right.value.text)
            .mergeInspectResults(left, right)
            .truncateForInspectReuse()
    }
    if (nodes.size != SINGLE_INSPECT_CHILD_COUNT) return null
    return (when (val node = nodes.single()) {
        else -> when (node.elementType) {
            TiBasicTokenTypes.STRING_LITERAL ->
                TiBasicDebugStringEvaluation.fromLiteral(node.text.removePrefix("\"").removeSuffix("\""))

            TiBasicNodeTypes.VARIABLE_ACCESS ->
                resolveInspectStringVariable(node.psi as? TiBasicVariableAccess)

            TiBasicNodeTypes.FUNCTION_CALL ->
                evaluateInspectStringFunction(node.psi as? TiBasicFunctionCall)

            else -> null
        }
    })?.truncateForInspectReuse()
}

private fun TiBasicDebugSession.resolveInspectStringVariable(variableAccess: TiBasicVariableAccess?): TiBasicDebugStringEvaluation? {
    variableAccess ?: return null
    if (variableAccess.hasSubscriptParens()) return null
    val variableName = variableAccess.name
        ?.takeIf { it.endsWith(STRING_VARIABLE_SUFFIX) }
        ?: return null
    val existingValue = stringVariables[variableName]
    return if (existingValue != null) {
        TiBasicDebugStringEvaluation.fromExistingValue(existingValue)
    } else {
        val initializedValue = TiBasicDebugStringValue.fromText(EMPTY_STRING)
        TiBasicDebugStringEvaluation(
            value = initializedValue,
            initializedStringVariables = mapOf(variableName to initializedValue),
        )
    }
}

private fun TiBasicDebugSession.evaluateInspectStringFunction(functionCall: TiBasicFunctionCall?): TiBasicDebugStringEvaluation? {
    functionCall ?: return null
    return when (functionCall.functionName()) {
        CHR_DOLLAR_FUNCTION -> functionCall.arguments()
            .singleOrNull()
            ?.let { evaluateInspectNumericNodes(it.node.nonWhitespaceChildren) }
            ?.toIntExactOrNull()
            ?.toChar()
            ?.toString()
            ?.let(TiBasicDebugStringEvaluation::fromLiteral)

        STR_DOLLAR_FUNCTION -> functionCall.arguments()
            .singleOrNull()
            ?.let { evaluateInspectNumericNodes(it.node.nonWhitespaceChildren) }
            ?.let(::tiBasicDecimalString)
            ?.let(TiBasicDebugStringEvaluation::fromLiteral)

        SEG_DOLLAR_FUNCTION -> {
            val arguments = functionCall.arguments()
            if (arguments.size != SEG_DOLLAR_ARG_COUNT) return null
            val source = evaluateInspectStringNodes(arguments[SEG_SOURCE_ARG_INDEX].node.nonWhitespaceChildren) ?: return null
            val start = evaluateInspectNumericNodes(arguments[SEG_START_ARG_INDEX].node.nonWhitespaceChildren)?.toIntExactOrNull() ?: return null
            val length = evaluateInspectNumericNodes(arguments[SEG_LENGTH_ARG_INDEX].node.nonWhitespaceChildren)?.toIntExactOrNull() ?: return null
            TiBasicDebugStringEvaluation.fromLiteral(source.value.text.segment(start, length))
                .mergeInspectResults(source)
        }

        else -> null
    }
}

private fun TiBasicDebugSession.evaluateInspectNumericNodes(nodes: List<ASTNode>): BigDecimal? {
    if (nodes.isEmpty()) return null
    if (isFullyParenthesized(nodes)) {
        return evaluateInspectNumericNodes(nodes.subList(FIRST_INNER_NODE_INDEX, nodes.lastIndex))
    }
    lastTopLevelBinaryOperatorIndex(nodes, ADDITIVE_OPERATOR_TYPES)?.let { operatorIndex ->
        val left = evaluateInspectNumericNodes(nodes.subList(0, operatorIndex)) ?: return null
        val right = evaluateInspectNumericNodes(nodes.subList(operatorIndex + 1, nodes.size)) ?: return null
        return when (nodes[operatorIndex].elementType) {
            TiBasicTokenTypes.PLUS_OP -> left + right
            TiBasicTokenTypes.MINUS_OP -> left - right
            else -> null
        }
    }
    lastTopLevelBinaryOperatorIndex(nodes, MULTIPLICATIVE_OPERATOR_TYPES)?.let { operatorIndex ->
        val left = evaluateInspectNumericNodes(nodes.subList(0, operatorIndex)) ?: return null
        val right = evaluateInspectNumericNodes(nodes.subList(operatorIndex + 1, nodes.size)) ?: return null
        return when (nodes[operatorIndex].elementType) {
            TiBasicTokenTypes.MUL_OP -> left * right
            TiBasicTokenTypes.DIV_OP -> if (right.compareTo(BigDecimal.ZERO) == 0) null else left.divide(right, INSPECT_MATH_CONTEXT)
            else -> null
        }
    }
    firstTopLevelBinaryOperatorIndex(nodes, POWER_OPERATOR_TYPES)?.let { operatorIndex ->
        val left = evaluateInspectNumericNodes(nodes.subList(0, operatorIndex)) ?: return null
        val right = evaluateInspectNumericNodes(nodes.subList(operatorIndex + 1, nodes.size)) ?: return null
        val exponent = right.toIntExactOrNull() ?: return null
        return left.pow(exponent, INSPECT_MATH_CONTEXT)
    }
    return resolveSignedNumericValue(nodes)
}

private fun TiBasicDebugSession.resolveSignedNumericValue(nodes: List<ASTNode>): BigDecimal? {
    if (nodes.isEmpty()) return null
    var sign = BigDecimal.ONE
    var index = 0
    while (index < nodes.size && nodes[index].elementType in UNARY_EXPRESSION_OPERATOR_TYPES) {
        if (nodes[index].elementType == TiBasicTokenTypes.MINUS_OP) {
            sign = sign.negate()
        }
        index++
    }
    if (index > 0) {
        val resolvedValue = evaluateInspectNumericNodes(nodes.subList(index, nodes.size)) ?: return null
        return sign.multiply(resolvedValue)
    }
    if (nodes.size != SINGLE_INSPECT_CHILD_COUNT) return null
    val node = nodes.single()
    return when (node.elementType) {
        TiBasicTokenTypes.NUMERIC_LITERAL -> parseTiBasicDecimalLiteral(node.text)
        TiBasicNodeTypes.VARIABLE_ACCESS ->
            (node.psi as? TiBasicVariableAccess)
                ?.takeIf { variableAccess -> !variableAccess.hasSubscriptParens() && variableAccess.name?.endsWith(STRING_VARIABLE_SUFFIX) == false }
                ?.name
                ?.let(numericVariables::get)
                ?.value
                ?: (node.psi as? TiBasicVariableAccess)
                    ?.takeIf { variableAccess -> !variableAccess.hasSubscriptParens() && variableAccess.name?.endsWith(STRING_VARIABLE_SUFFIX) == false }
                    ?.let { BigDecimal.ZERO }
        TiBasicNodeTypes.FUNCTION_CALL -> evaluateInspectNumericFunction(node.psi as? TiBasicFunctionCall)
        else -> null
    }
}

private fun TiBasicDebugSession.evaluateInspectNumericFunction(functionCall: TiBasicFunctionCall?): BigDecimal? {
    functionCall ?: return null
    val arguments = functionCall.arguments()
    return when (functionCall.functionName()) {
        LEN_FUNCTION -> arguments
            .singleOrNull()
            ?.let { evaluateInspectStringNodes(it.node.nonWhitespaceChildren) }
            ?.value
            ?.text
            ?.length
            ?.toBigDecimal()

        ASC_FUNCTION -> arguments
            .singleOrNull()
            ?.let { evaluateInspectStringNodes(it.node.nonWhitespaceChildren) }
            ?.value
            ?.text
            ?.firstOrNull()
            ?.code
            ?.toBigDecimal()

        VAL_FUNCTION -> arguments
            .singleOrNull()
            ?.let { evaluateInspectStringNodes(it.node.nonWhitespaceChildren) }
            ?.value
            ?.text
            ?.let(::parseTiBasicDecimalLiteral)

        POS_FUNCTION -> {
            if (arguments.size != POS_ARG_COUNT) return null
            val source = evaluateInspectStringNodes(arguments[POS_SOURCE_ARG_INDEX].node.nonWhitespaceChildren)?.value?.text ?: return null
            val target = evaluateInspectStringNodes(arguments[POS_TARGET_ARG_INDEX].node.nonWhitespaceChildren)?.value?.text ?: return null
            val startIndex = evaluateInspectNumericNodes(arguments[POS_START_ARG_INDEX].node.nonWhitespaceChildren)?.toIntExactOrNull() ?: return null
            source.pos(target, startIndex).toBigDecimal()
        }

        else -> null
    }
}

private fun isStringExpression(nodes: List<ASTNode>): Boolean {
    if (nodes.any { it.elementType in COMPARISON_OPERATOR_TYPES }) return false
    if (nodes.any { it.elementType == TiBasicTokenTypes.CONCAT_OP }) return true
    if (isFullyParenthesized(nodes)) {
        return isStringExpression(nodes.subList(FIRST_INNER_NODE_INDEX, nodes.lastIndex))
    }
    val first = nodes.firstOrNull() ?: return false
    return first.elementType == TiBasicTokenTypes.STRING_LITERAL ||
        (first.elementType == TiBasicNodeTypes.VARIABLE_ACCESS && first.firstChildNode?.elementType == TiBasicTokenTypes.STRING_VARIABLE) ||
        (first.elementType == TiBasicNodeTypes.FUNCTION_CALL && first.firstChildNode?.elementType == TiBasicTokenTypes.STRING_FUNCTION_KEYWORD)
}

private fun TiBasicDebugStringEvaluation.toInspectResult(): TiBasicDebugInspectResult {
    val baseResult = "\"${value.text}\" = ${value.internalDisplay}"
    return TiBasicDebugInspectResult(
        displayText = warningMessage
            ?.let { "$it | $baseResult" }
            ?: baseResult,
    )
}

private fun TiBasicDebugStringEvaluation.mergeInspectResults(
    left: TiBasicDebugStringEvaluation,
    right: TiBasicDebugStringEvaluation,
): TiBasicDebugStringEvaluation =
    copy(
        initializedStringVariables = left.initializedStringVariables + right.initializedStringVariables,
    ).mergeWarnings(left.warningMessage, right.warningMessage)

private fun TiBasicDebugStringEvaluation.mergeInspectResults(
    source: TiBasicDebugStringEvaluation,
): TiBasicDebugStringEvaluation =
    copy(
        initializedStringVariables = source.initializedStringVariables,
    ).mergeWarnings(source.warningMessage)

private fun TiBasicDebugStringEvaluation.truncateForInspectReuse(): TiBasicDebugStringEvaluation {
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

private fun TiBasicDebugStringEvaluation.mergeWarnings(vararg warnings: String?): TiBasicDebugStringEvaluation =
    copy(
        warningMessage = (listOfNotNull(warningMessage) + warnings.filterNotNull())
            .distinct()
            .joinToString(WARNING_SEPARATOR)
            .ifEmpty { null },
    )

private fun BigDecimal.toIntExactOrNull(): Int? =
    runCatching { intValueExact() }.getOrNull()

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
private val COMPARISON_OPERATOR_TYPES = setOf(
    TiBasicTokenTypes.EQ_OP,
    TiBasicTokenTypes.LT_OP,
    TiBasicTokenTypes.GT_OP,
    TiBasicTokenTypes.NEQ_OP,
    TiBasicTokenTypes.LE_OP,
    TiBasicTokenTypes.GE_OP,
)

private const val ASC_FUNCTION = "ASC"
private const val CHR_DOLLAR_FUNCTION = "CHR$"
private const val EMPTY_STRING = ""
private const val INSPECT_FILE_NAME = "inspect.tibasic"
private const val INSPECT_LINE_NUMBER = 100
private const val LEN_FUNCTION = "LEN"
private const val MAX_TI_BASIC_STRING_LENGTH = 255
private const val POS_FUNCTION = "POS"
private const val POS_ARG_COUNT = 3
private const val POS_SOURCE_ARG_INDEX = 0
private const val POS_START_ARG_INDEX = 2
private const val POS_TARGET_ARG_INDEX = 1
private const val PRINT_ARGUMENTS_OFFSET = 1
private const val SEG_DOLLAR_ARG_COUNT = 3
private const val SEG_DOLLAR_FUNCTION = "SEG$"
private const val SEG_LENGTH_ARG_INDEX = 2
private const val SEG_SOURCE_ARG_INDEX = 0
private const val SEG_START_ARG_INDEX = 1
private const val SINGLE_INSPECT_CHILD_COUNT = 1
private const val STRING_VARIABLE_SUFFIX = "$"
private const val STR_DOLLAR_FUNCTION = "STR$"
private const val VAL_FUNCTION = "VAL"
private const val WARNING_SEPARATOR = " | "
private const val FIRST_INNER_NODE_INDEX = 1
private val INSPECT_MATH_CONTEXT = MathContext.DECIMAL64
