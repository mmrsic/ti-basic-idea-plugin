package com.github.mmrsic.idea.plugins.tibasic.language.analysis.calls

import com.github.mmrsic.idea.plugins.tibasic.common.ext.nonWhitespaceChildren
import com.github.mmrsic.idea.plugins.tibasic.editor.constantNumericVariableValue
import com.github.mmrsic.idea.plugins.tibasic.editor.resolveConstantStringValue
import com.github.mmrsic.idea.plugins.tibasic.language.analysis.resolveNumericExpressionValue
import com.github.mmrsic.idea.plugins.tibasic.language.analysis.resolveNumericExpressionNodes
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicCallStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicExpression
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicVariableAccess
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicForStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicIfStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicInputStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicLetStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicLine
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicNextStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicReadStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicRestoreStatement
import com.github.mmrsic.idea.plugins.tibasic.language.analysis.variables.AccessType
import com.github.mmrsic.idea.plugins.tibasic.language.analysis.variables.TiBasicVariableCollector
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil

internal data class TiBasicStaticCallStatement(
    val callStatement: TiBasicCallStatement,
    val staticValues: StaticValueSnapshot,
) {
    val readDataVariableValues: Map<String, String>
        get() = staticValues.numericVariables.mapValues { (_, value) -> value.toString() } + staticValues.stringVariables
}

internal data class TiBasicStaticStatementSnapshot(
    val statement: PsiElement,
    val staticValues: StaticValueSnapshot,
)

internal fun collectStaticallyTraceableCallStatements(file: TiBasicFile): List<TiBasicStaticCallStatement> =
    CachedValuesManager.getCachedValue(file) {
        CachedValueProvider.Result.create(buildStaticTraversalResult(file).callStatements, file)
    }

internal fun collectStaticallyTraceableStatementSnapshots(file: TiBasicFile): List<TiBasicStaticStatementSnapshot> =
    CachedValuesManager.getCachedValue(file) {
        CachedValueProvider.Result.create(buildStaticTraversalResult(file).statementSnapshots, file)
    }

internal fun resolveStaticNumericValue(
    expression: TiBasicExpression?,
    file: TiBasicFile,
    staticValues: StaticValueSnapshot = StaticValueSnapshot(),
): Int? =
    resolveNumericExpressionValue(expression) { variableAccess ->
        staticValues.resolveNumericValue(variableAccess, file)
    }

internal fun resolveStaticStringValue(
    expression: TiBasicExpression?,
    file: TiBasicFile,
    staticValues: StaticValueSnapshot = StaticValueSnapshot(),
): String? =
    when (val variableAccess = expression?.variableAccess()) {
        null -> resolveConstantStringValue(expression, file)
        else -> staticValues.resolveStringValue(variableAccess, file) ?: resolveConstantStringValue(expression, file)
    }

private fun buildStaticTraversalResult(file: TiBasicFile): StaticTraversalResult {
    val callStatements = mutableListOf<TiBasicStaticCallStatement>()
    val statementSnapshots = mutableListOf<TiBasicStaticStatementSnapshot>()
    with(callStatements) {
        val lines = file.lines()
        val lineNumberToIndex = lines.mapIndexed { index, line -> line.lineNumber() to index }.toMap()
        val highestProgramLineNumber = lines.maxOfOrNull(TiBasicLine::lineNumber) ?: NO_PROGRAM_LINE_NUMBER
        val dataItems = lines.flatMap { line ->
            line.dataStatement()
                ?.dataItems()
                ?.map { item -> TiBasicDataItem(line.lineNumber(), item) }
                .orEmpty()
        }
        val loopTargets = buildLoopTargets(lines)
        val staticValues = StaticValueState()
        var readDataState = ReadDataState()
        val loopFrames = ArrayDeque<LoopFrame>()
        val visitedStates = mutableSetOf<TraversalState>()
        var lineIndex = 0

        while (lineIndex < lines.size) {
            if (!visitedStates.add(
                    TraversalState(
                        lineIndex = lineIndex,
                        readDataState = readDataState,
                        staticValues = staticValues.snapshot(),
                        loopFrames = loopFrames.map(LoopFrame::toState),
                    ),
                )
            ) {
                break
            }
            val line = lines[lineIndex]
            val statement = line.statement()
            when (statement) {
                is TiBasicCallStatement -> add(TiBasicStaticCallStatement(statement, staticValues.snapshot()))

                is TiBasicReadStatement -> {
                    val readResult = applyReadStatement(
                        statement = statement,
                        file = file,
                        dataItems = dataItems,
                        readDataState = readDataState,
                        staticValues = staticValues,
                    )
                    readDataState = readResult.readDataState
                    if (readResult.shouldAbort) {
                        break
                    }
                }

                is TiBasicRestoreStatement -> {
                    val restoreResult = applyRestoreStatement(
                        statement = statement,
                        file = file,
                        highestProgramLineNumber = highestProgramLineNumber,
                        dataItems = dataItems,
                        readDataState = readDataState,
                        staticValues = staticValues.snapshot(),
                    )
                    readDataState = restoreResult.readDataState
                    if (restoreResult.shouldAbort) {
                        break
                    }
                }

                is TiBasicIfStatement -> {
                    val jumpTargetIndex = resolveIfJumpTargetIndex(
                        statement = statement,
                        file = file,
                        staticValues = staticValues.snapshot(),
                        lineNumberToIndex = lineNumberToIndex,
                    )
                    if (jumpTargetIndex != null) {
                        lineIndex = jumpTargetIndex
                        continue
                    }
                }

                is TiBasicLetStatement -> applyLetStatement(statement, file, staticValues)

                is TiBasicForStatement -> {
                    val loopFrame = createLoopFrame(
                        forStatement = statement,
                        currentLineIndex = lineIndex,
                        file = file,
                        staticValues = staticValues.snapshot(),
                        nextLineIndex = loopTargets.forToNext[lineIndex],
                    )
                    invalidateWrittenReadDataVariables(statement, file, staticValues)
                    if (loopFrame?.remainingIterations == 0) {
                        staticValues.numericVariables.remove(loopFrame.variableName)
                        lineIndex = ((loopTargets.forToNext[lineIndex] ?: lineIndex) + 1)
                        continue
                    }
                    if (loopFrame != null) {
                        staticValues.numericVariables[loopFrame.variableName] = loopFrame.currentValue
                        if (loopFrame.remainingIterations > 1) {
                            loopFrames.addLast(loopFrame)
                        }
                    }
                }

                is TiBasicInputStatement -> invalidateWrittenReadDataVariables(statement, file, staticValues)

                is TiBasicNextStatement -> {
                    val frame = loopFrames.lastOrNull()
                    if (frame != null && frame.nextLineIndex == lineIndex) {
                        if (frame.remainingIterations > 1) {
                            frame.remainingIterations -= 1
                            frame.currentValue += frame.step
                            staticValues.numericVariables[frame.variableName] = frame.currentValue
                            lineIndex = frame.bodyStartLineIndex
                            continue
                        }
                        staticValues.numericVariables.remove(frame.variableName)
                        loopFrames.removeLast()
                    }
                }
            }
            if (statement is TiBasicCallStatement) {
                invalidateWrittenReadDataVariables(statement, file, staticValues)
            }
            if (statement != null) {
                statementSnapshots += TiBasicStaticStatementSnapshot(statement, staticValues.snapshot())
            }
            lineIndex++
        }
    }
    return StaticTraversalResult(callStatements, statementSnapshots)
}

private data class StaticTraversalResult(
    val callStatements: List<TiBasicStaticCallStatement>,
    val statementSnapshots: List<TiBasicStaticStatementSnapshot>,
)

private data class TiBasicDataItem(
    val lineNumber: Int,
    val value: String,
)

private data class LoopTargets(
    val forToNext: Map<Int, Int>,
    val nextToFor: Map<Int, Int>,
)

private data class LoopFrame(
    val variableName: String,
    val nextLineIndex: Int,
    val bodyStartLineIndex: Int,
    val step: Int,
    var currentValue: Int,
    var remainingIterations: Int,
)

private data class ReadDataState(
    val nextDataItemIndex: Int = FIRST_DATA_ITEM_INDEX,
    val pendingRestoreTargetLineNumber: Int? = null,
)

private data class TraversalState(
    val lineIndex: Int,
    val readDataState: ReadDataState,
    val staticValues: StaticValueSnapshot,
    val loopFrames: List<LoopFrameState>,
)

private data class LoopFrameState(
    val variableName: String,
    val nextLineIndex: Int,
    val bodyStartLineIndex: Int,
    val step: Int,
    val currentValue: Int,
    val remainingIterations: Int,
)

internal data class StaticArrayElementKey(
    val name: String,
    val subscripts: List<Int>,
)

internal data class StaticValueSnapshot(
    val numericVariables: Map<String, Int> = emptyMap(),
    val stringVariables: Map<String, String> = emptyMap(),
    val numericArrays: Map<StaticArrayElementKey, Int> = emptyMap(),
    val stringArrays: Map<StaticArrayElementKey, String> = emptyMap(),
) {
    fun resolveNumericValue(variableAccess: TiBasicVariableAccess, file: TiBasicFile): Int? {
        val name = variableAccess.name ?: return null
        return if (variableAccess.hasSubscriptParens()) {
            resolveArrayElementKey(variableAccess, file)?.let(numericArrays::get)
                ?: TiBasicVariableCollector.constantValueOf(variableAccess, file)?.toIntOrNull()
        } else {
            numericVariables[name] ?: constantNumericVariableValue(name, file)
        }
    }

    fun resolveStringValue(variableAccess: TiBasicVariableAccess, file: TiBasicFile): String? {
        val name = variableAccess.name ?: return null
        if (!name.endsWith('$')) return null
        return if (variableAccess.hasSubscriptParens()) {
            resolveArrayElementKey(variableAccess, file)?.let(stringArrays::get)
                ?: TiBasicVariableCollector.constantValueOf(variableAccess, file)
                    ?.removePrefix("\"")
                    ?.removeSuffix("\"")
        } else {
            stringVariables[name]
                ?: TiBasicVariableCollector.constantValueOf(variableAccess, file)
                    ?.removePrefix("\"")
                    ?.removeSuffix("\"")
        }
    }

    fun resolveArrayElementKey(variableAccess: TiBasicVariableAccess, file: TiBasicFile): StaticArrayElementKey? {
        val name = variableAccess.name ?: return null
        val subscripts = variableAccess.subscriptExpressions()
            .map { expression -> resolveStaticNumericValue(expression, file, this) }
        if (subscripts.any { it == null }) return null
        return StaticArrayElementKey(name, subscripts.filterNotNull())
    }
}

private class StaticValueState {
    val numericVariables = mutableMapOf<String, Int>()
    val stringVariables = mutableMapOf<String, String>()
    val numericArrays = mutableMapOf<StaticArrayElementKey, Int>()
    val stringArrays = mutableMapOf<StaticArrayElementKey, String>()

    fun snapshot(): StaticValueSnapshot =
        StaticValueSnapshot(
            numericVariables = numericVariables.toMap(),
            stringVariables = stringVariables.toMap(),
            numericArrays = numericArrays.toMap(),
            stringArrays = stringArrays.toMap(),
        )

    fun invalidate(variableAccess: TiBasicVariableAccess, file: TiBasicFile) {
        val name = variableAccess.name ?: return
        if (variableAccess.hasSubscriptParens()) {
            val arrayElementKey = snapshot().resolveArrayElementKey(variableAccess, file)
            if (arrayElementKey != null) {
                numericArrays.remove(arrayElementKey)
                stringArrays.remove(arrayElementKey)
            } else {
                numericArrays.keys.removeIf { key -> key.name == name }
                stringArrays.keys.removeIf { key -> key.name == name }
            }
        } else {
            numericVariables.remove(name)
            stringVariables.remove(name)
        }
    }
}

private fun LoopFrame.toState(): LoopFrameState =
    LoopFrameState(
        variableName = variableName,
        nextLineIndex = nextLineIndex,
        bodyStartLineIndex = bodyStartLineIndex,
        step = step,
        currentValue = currentValue,
        remainingIterations = remainingIterations,
    )

private fun buildLoopTargets(lines: List<TiBasicLine>): LoopTargets {
    val forStack = ArrayDeque<Pair<Int, String?>>()
    val forToNext = mutableMapOf<Int, Int>()
    val nextToFor = mutableMapOf<Int, Int>()
    lines.forEachIndexed { index, line ->
        when (val statement = line.statement()) {
            is TiBasicForStatement -> forStack.addLast(index to statement.controlVariableName())
            is TiBasicNextStatement -> {
                val nextVar = statement.controlVariableName()
                val top = forStack.lastOrNull()
                if (top != null && top.second == nextVar) {
                    forStack.removeLast()
                    forToNext[top.first] = index
                    nextToFor[index] = top.first
                }
            }
        }
    }
    return LoopTargets(forToNext, nextToFor)
}

private fun createLoopFrame(
    forStatement: TiBasicForStatement,
    currentLineIndex: Int,
    file: TiBasicFile,
    staticValues: StaticValueSnapshot,
    nextLineIndex: Int?,
): LoopFrame? {
    nextLineIndex ?: return null
    val variableName = forStatement.controlVariableName() ?: return null
    val start = resolveLoopIterationValue(forStatement.startExpression(), file, staticValues) ?: return null
    val end = resolveLoopIterationValue(forStatement.endExpression(), file, staticValues) ?: return null
    val step = forStatement.stepExpression()
        ?.let { expr -> resolveLoopIterationValue(expr, file, staticValues) }
        ?: DEFAULT_FOR_STEP
    if (step == 0) return null
    val iterations = when {
        step > 0 && start > end -> 0
        step < 0 && start < end -> 0
        step > 0 -> ((end - start) / step) + 1
        else -> ((start - end) / -step) + 1
    }
    return LoopFrame(
        variableName = variableName,
        nextLineIndex = nextLineIndex,
        bodyStartLineIndex = currentLineIndex + 1,
        step = step,
        currentValue = start,
        remainingIterations = iterations,
    )
}

private fun resolveLoopIterationValue(
    expression: TiBasicExpression?,
    file: TiBasicFile,
    staticValues: StaticValueSnapshot,
): Int? =
    resolveStaticNumericValue(expression, file, staticValues)

private fun applyReadStatement(
    statement: TiBasicReadStatement,
    file: TiBasicFile,
    dataItems: List<TiBasicDataItem>,
    readDataState: ReadDataState,
    staticValues: StaticValueState,
): ReadStatementResult {
    val preparedReadDataState = prepareReadDataStateForRead(readDataState, dataItems)
        ?: return ReadStatementResult(readDataState = readDataState, shouldAbort = true)
    var currentIndex = preparedReadDataState.nextDataItemIndex
    statement.variableAccesses().forEach { variable ->
        val dataItem = dataItems.getOrNull(currentIndex)
        assignDataItem(variable, dataItem?.value, file, staticValues)
        if (dataItem != null) currentIndex++
    }
    return ReadStatementResult(
        readDataState = preparedReadDataState.copy(nextDataItemIndex = currentIndex),
        shouldAbort = false,
    )
}

private data class ReadStatementResult(
    val readDataState: ReadDataState,
    val shouldAbort: Boolean,
)

private fun prepareReadDataStateForRead(
    readDataState: ReadDataState,
    dataItems: List<TiBasicDataItem>,
): ReadDataState? {
    val restoreTargetLineNumber = readDataState.pendingRestoreTargetLineNumber ?: return readDataState
    val restoredDataItemIndex = dataItems.indexOfFirst { item -> item.lineNumber >= restoreTargetLineNumber }
    if (restoredDataItemIndex < 0) {
        return null
    }
    return readDataState.copy(
        nextDataItemIndex = restoredDataItemIndex,
        pendingRestoreTargetLineNumber = null,
    )
}

private fun applyRestoreStatement(
    statement: TiBasicRestoreStatement,
    file: TiBasicFile,
    highestProgramLineNumber: Int,
    dataItems: List<TiBasicDataItem>,
    readDataState: ReadDataState,
    staticValues: StaticValueSnapshot,
): RestoreStatementResult {
    if (statement.isFileRestore()) {
        return RestoreStatementResult(
            readDataState = readDataState.copy(
                nextDataItemIndex = dataItems.size,
                pendingRestoreTargetLineNumber = null,
            ),
            shouldAbort = false,
        )
    }
    if (statement.recordNumberExpr() == null) {
        return RestoreStatementResult(
            readDataState = readDataState.copy(
                nextDataItemIndex = FIRST_DATA_ITEM_INDEX,
                pendingRestoreTargetLineNumber = null,
            ),
            shouldAbort = false,
        )
    }
    val targetLineNumber = resolveStaticNumericValue(statement.recordNumberExpr(), file, staticValues)
        ?: FIRST_DATA_LINE_NUMBER
    if (targetLineNumber > highestProgramLineNumber) {
        return RestoreStatementResult(readDataState = readDataState, shouldAbort = true)
    }
    return RestoreStatementResult(
        readDataState = readDataState.copy(pendingRestoreTargetLineNumber = targetLineNumber),
        shouldAbort = false,
    )
}

private data class RestoreStatementResult(
    val readDataState: ReadDataState,
    val shouldAbort: Boolean,
)

private fun applyLetStatement(
    statement: TiBasicLetStatement,
    file: TiBasicFile,
    staticValues: StaticValueState,
) {
    val lhs = statement.targetVariableAccess() ?: return
    val rhs = statement.assignedExpression()
    val snapshot = staticValues.snapshot()
    when {
        lhs.name?.endsWith('$') == true -> assignResolvedString(lhs, resolveStaticStringValue(rhs, file, snapshot), file, staticValues)
        else -> assignResolvedNumeric(lhs, resolveStaticNumericValue(rhs, file, snapshot), file, staticValues)
    }
}

private fun assignDataItem(
    variableAccess: TiBasicVariableAccess,
    rawValue: String?,
    file: TiBasicFile,
    staticValues: StaticValueState,
) {
    when {
        variableAccess.name?.endsWith('$') == true -> assignResolvedString(variableAccess, rawValue, file, staticValues)
        else -> assignResolvedNumeric(variableAccess, rawValue?.toIntOrNull(), file, staticValues)
    }
}

private fun assignResolvedNumeric(
    variableAccess: TiBasicVariableAccess,
    value: Int?,
    file: TiBasicFile,
    staticValues: StaticValueState,
) {
    val name = variableAccess.name ?: return
    if (variableAccess.hasSubscriptParens()) {
        val arrayElementKey = staticValues.snapshot().resolveArrayElementKey(variableAccess, file)
        if (arrayElementKey == null || value == null) {
            staticValues.invalidate(variableAccess, file)
            return
        }
        staticValues.numericArrays[arrayElementKey] = value
        staticValues.stringArrays.remove(arrayElementKey)
    } else if (value != null) {
        staticValues.numericVariables[name] = value
        staticValues.stringVariables.remove(name)
    } else {
        staticValues.invalidate(variableAccess, file)
    }
}

private fun assignResolvedString(
    variableAccess: TiBasicVariableAccess,
    value: String?,
    file: TiBasicFile,
    staticValues: StaticValueState,
) {
    val name = variableAccess.name ?: return
    if (variableAccess.hasSubscriptParens()) {
        val arrayElementKey = staticValues.snapshot().resolveArrayElementKey(variableAccess, file)
        if (arrayElementKey == null || value == null) {
            staticValues.invalidate(variableAccess, file)
            return
        }
        staticValues.stringArrays[arrayElementKey] = value
        staticValues.numericArrays.remove(arrayElementKey)
    } else if (value != null) {
        staticValues.stringVariables[name] = value
        staticValues.numericVariables.remove(name)
    } else {
        staticValues.invalidate(variableAccess, file)
    }
}

private fun invalidateWrittenReadDataVariables(
    statement: PsiElement,
    file: TiBasicFile,
    staticValues: StaticValueState,
) {
    PsiTreeUtil.findChildrenOfType(statement, TiBasicVariableAccess::class.java)
        .filter { variableAccess -> TiBasicVariableCollector.determineAccessType(variableAccess) == AccessType.WRITE }
        .forEach { variableAccess -> staticValues.invalidate(variableAccess, file) }
}

private const val DEFAULT_FOR_STEP = 1
private const val FIRST_DATA_ITEM_INDEX = 0
private const val FIRST_DATA_LINE_NUMBER = 0
private const val NO_PROGRAM_LINE_NUMBER = 0

private fun resolveIfJumpTargetIndex(
    statement: TiBasicIfStatement,
    file: TiBasicFile,
    staticValues: StaticValueSnapshot,
    lineNumberToIndex: Map<Int, Int>,
): Int? {
    val conditionResult = evaluateIfCondition(statement.conditionExpression(), file, staticValues) ?: return null
    val targetLineNumber = if (conditionResult) statement.thenLineNumber() else statement.elseLineNumber()
    return targetLineNumber?.let(lineNumberToIndex::get)
}

private fun evaluateIfCondition(
    expression: TiBasicExpression?,
    file: TiBasicFile,
    staticValues: StaticValueSnapshot,
): Boolean? {
    expression ?: return null
    val children = expression.node.nonWhitespaceChildren
    val operatorIndex = children.indexOfFirst { it.elementType in RELATIONAL_OPERATORS }
    if (operatorIndex <= 0 || operatorIndex >= children.lastIndex) return null
    val left = resolveNumericExpressionNodes(children.subList(0, operatorIndex)) { variableAccess ->
        staticValues.resolveNumericValue(variableAccess, file)
    } ?: return null
    val right = resolveNumericExpressionNodes(children.subList(operatorIndex + 1, children.size)) { variableAccess ->
        staticValues.resolveNumericValue(variableAccess, file)
    } ?: return null
    return when (children[operatorIndex].elementType) {
        TiBasicTokenTypes.EQ_OP -> left == right
        TiBasicTokenTypes.LT_OP -> left < right
        TiBasicTokenTypes.GT_OP -> left > right
        TiBasicTokenTypes.NEQ_OP -> left != right
        TiBasicTokenTypes.LE_OP -> left <= right
        TiBasicTokenTypes.GE_OP -> left >= right
        else -> null
    }
}

private val RELATIONAL_OPERATORS = setOf(
    TiBasicTokenTypes.EQ_OP,
    TiBasicTokenTypes.LT_OP,
    TiBasicTokenTypes.GT_OP,
    TiBasicTokenTypes.NEQ_OP,
    TiBasicTokenTypes.LE_OP,
    TiBasicTokenTypes.GE_OP,
)
