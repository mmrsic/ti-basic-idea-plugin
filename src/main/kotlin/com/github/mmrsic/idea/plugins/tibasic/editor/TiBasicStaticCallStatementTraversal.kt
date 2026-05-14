package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.ext.childrenAfter
import com.github.mmrsic.idea.plugins.tibasic.ext.firstChildOfType
import com.github.mmrsic.idea.plugins.tibasic.ext.nonWhitespaceChildren
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.expression.TiBasicCallStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.expression.TiBasicExpression
import com.github.mmrsic.idea.plugins.tibasic.psi.expression.TiBasicVariableAccess
import com.github.mmrsic.idea.plugins.tibasic.psi.statement.TiBasicDataStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.statement.TiBasicForStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.statement.TiBasicIfStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.statement.TiBasicInputStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.statement.TiBasicLetStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.statement.TiBasicLine
import com.github.mmrsic.idea.plugins.tibasic.psi.statement.TiBasicNextStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.statement.TiBasicReadStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.statement.TiBasicRestoreStatement
import com.github.mmrsic.idea.plugins.tibasic.toolwindow.AccessType
import com.github.mmrsic.idea.plugins.tibasic.toolwindow.TiBasicVariableCollector
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil

internal data class TiBasicStaticCallStatement(
    val callStatement: TiBasicCallStatement,
    val readDataVariableValues: Map<String, String>,
)

internal fun collectStaticallyTraceableCallStatements(file: TiBasicFile): List<TiBasicStaticCallStatement> =
    CachedValuesManager.getCachedValue(file) {
        CachedValueProvider.Result.create(buildStaticallyTraceableCallStatements(file), file)
    }

internal fun resolveStaticNumericValue(
    expression: TiBasicExpression?,
    file: TiBasicFile,
    readDataVariableValues: Map<String, String>,
): Int? =
    resolveNumericExpressionValue(expression) { variableAccess ->
        variableAccess.name?.let { variableName ->
            readDataVariableValues[variableName]?.toIntOrNull()
                ?: constantNumericVariableValue(variableName, file)
        }
    }

internal fun resolveStaticStringValue(
    expression: TiBasicExpression?,
    file: TiBasicFile,
    readDataVariableValues: Map<String, String>,
): String? =
    resolveConstantStringValue(expression, file)
        ?: expression?.variableAccess()?.name?.let(readDataVariableValues::get)

private fun buildStaticallyTraceableCallStatements(file: TiBasicFile): List<TiBasicStaticCallStatement> =
    buildList {
        val lines = file.lines()
        val lineNumberToIndex = lines.mapIndexed { index, line -> line.lineNumber() to index }.toMap()
        val dataItems = lines.flatMap { line ->
            line.dataStatement()
                ?.dataItems()
                ?.map { item -> TiBasicDataItem(line.lineNumber(), item) }
                .orEmpty()
        }
        val loopTargets = buildLoopTargets(lines)
        val readDataVariableValues = mutableMapOf<String, String>()
        var nextDataItemIndex = 0
        val loopFrames = ArrayDeque<LoopFrame>()
        val visitedStates = mutableSetOf<TraversalState>()
        var lineIndex = 0

        while (lineIndex < lines.size) {
            if (!visitedStates.add(
                    TraversalState(
                        lineIndex = lineIndex,
                        nextDataItemIndex = nextDataItemIndex,
                        readDataVariableValues = readDataVariableValues.toMap(),
                        loopFrames = loopFrames.map(LoopFrame::toState),
                    ),
                )
            ) {
                break
            }
            val line = lines[lineIndex]
            val statement = line.statement()
            when (statement) {
                is TiBasicCallStatement -> add(TiBasicStaticCallStatement(statement, readDataVariableValues.toMap()))

                is TiBasicReadStatement -> {
                    nextDataItemIndex = applyReadStatement(
                        statement = statement,
                        dataItems = dataItems,
                        nextDataItemIndex = nextDataItemIndex,
                        readDataVariableValues = readDataVariableValues,
                    )
                }

                is TiBasicRestoreStatement -> {
                    nextDataItemIndex = restoreDataItemIndex(
                        statement = statement,
                        file = file,
                        dataItems = dataItems,
                        readDataVariableValues = readDataVariableValues,
                    )
                }

                is TiBasicIfStatement -> {
                    val jumpTargetIndex = resolveIfJumpTargetIndex(
                        statement = statement,
                        readDataVariableValues = readDataVariableValues,
                        lineNumberToIndex = lineNumberToIndex,
                    )
                    if (jumpTargetIndex != null) {
                        lineIndex = jumpTargetIndex
                        continue
                    }
                }

                is TiBasicLetStatement -> applyLetStatement(statement, readDataVariableValues)

                is TiBasicForStatement -> {
                    val loopFrame = createLoopFrame(
                        forStatement = statement,
                        currentLineIndex = lineIndex,
                        file = file,
                        readDataVariableValues = readDataVariableValues,
                        nextLineIndex = loopTargets.forToNext[lineIndex],
                    )
                    invalidateWrittenReadDataVariables(statement, readDataVariableValues)
                    if (loopFrame?.remainingIterations == 0) {
                        loopFrame.variableName.let(readDataVariableValues::remove)
                        lineIndex = ((loopTargets.forToNext[lineIndex] ?: lineIndex) + 1)
                        continue
                    }
                    if (loopFrame != null) {
                        readDataVariableValues[loopFrame.variableName] = loopFrame.currentValue.toString()
                        if (loopFrame.remainingIterations > 1) {
                            loopFrames.addLast(loopFrame)
                        }
                    }
                }

                is TiBasicInputStatement -> invalidateWrittenReadDataVariables(statement, readDataVariableValues)

                is TiBasicNextStatement -> {
                    val frame = loopFrames.lastOrNull()
                    if (frame != null && frame.nextLineIndex == lineIndex) {
                        if (frame.remainingIterations > 1) {
                            frame.remainingIterations -= 1
                            frame.currentValue += frame.step
                            readDataVariableValues[frame.variableName] = frame.currentValue.toString()
                            lineIndex = frame.bodyStartLineIndex
                            continue
                        }
                        readDataVariableValues.remove(frame.variableName)
                        loopFrames.removeLast()
                    }
                }
            }
            if (statement is TiBasicCallStatement) {
                invalidateWrittenReadDataVariables(statement, readDataVariableValues)
            }
            lineIndex++
        }
    }

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

private data class TraversalState(
    val lineIndex: Int,
    val nextDataItemIndex: Int,
    val readDataVariableValues: Map<String, String>,
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
    readDataVariableValues: Map<String, String>,
    nextLineIndex: Int?,
): LoopFrame? {
    nextLineIndex ?: return null
    val variableName = forStatement.controlVariableName() ?: return null
    val start = resolveLoopIterationValue(forStatement.startExpression(), file, readDataVariableValues) ?: return null
    val end = resolveLoopIterationValue(forStatement.endExpression(), file, readDataVariableValues) ?: return null
    val step = forStatement.stepExpression()
        ?.let { expr -> resolveLoopIterationValue(expr, file, readDataVariableValues) }
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
    readDataVariableValues: Map<String, String>,
): Int? =
    resolveStaticNumericValue(expression, file, readDataVariableValues)

private fun applyReadStatement(
    statement: TiBasicReadStatement,
    dataItems: List<TiBasicDataItem>,
    nextDataItemIndex: Int,
    readDataVariableValues: MutableMap<String, String>,
): Int {
    var currentIndex = nextDataItemIndex
    statement.variableAccesses().forEach { variable ->
        val variableName = variable.name ?: return@forEach
        val dataItem = dataItems.getOrNull(currentIndex)
        if (dataItem == null) {
            readDataVariableValues.remove(variableName)
        } else {
            readDataVariableValues[variableName] = dataItem.value
            currentIndex++
        }
    }
    return currentIndex
}

private fun restoreDataItemIndex(
    statement: TiBasicRestoreStatement,
    file: TiBasicFile,
    dataItems: List<TiBasicDataItem>,
    readDataVariableValues: Map<String, String>,
): Int {
    if (statement.isFileRestore()) return dataItems.size
    val targetLineNumber = resolveStaticNumericValue(statement.recordNumberExpr(), file, readDataVariableValues) ?: return 0
    return dataItems.indexOfFirst { item -> item.lineNumber >= targetLineNumber }
        .takeIf { it >= 0 }
        ?: dataItems.size
}

private fun applyLetStatement(
    statement: TiBasicLetStatement,
    readDataVariableValues: MutableMap<String, String>,
) {
    val lhs = statement.node.firstChildOfType(TiBasicNodeTypes.VARIABLE_ACCESS)?.psi as? TiBasicVariableAccess ?: return
    val variableName = lhs.name ?: return
    if (lhs.hasSubscriptParens()) {
        readDataVariableValues.remove(variableName)
        return
    }
    val rhs = statement.node
        .childrenAfter(TiBasicTokenTypes.EQ_OP)
        .firstOrNull { it.elementType == TiBasicNodeTypes.EXPRESSION }
        ?.psi as? TiBasicExpression
    val propagatedValue = rhs?.variableAccess()?.name?.let(readDataVariableValues::get)
    if (propagatedValue != null) {
        readDataVariableValues[variableName] = propagatedValue
    } else {
        readDataVariableValues.remove(variableName)
    }
}

private fun invalidateWrittenReadDataVariables(
    statement: PsiElement,
    readDataVariableValues: MutableMap<String, String>,
) {
    PsiTreeUtil.findChildrenOfType(statement, TiBasicVariableAccess::class.java)
        .filter { variableAccess -> TiBasicVariableCollector.determineAccessType(variableAccess) == AccessType.WRITE }
        .mapNotNull(TiBasicVariableAccess::getName)
        .forEach(readDataVariableValues::remove)
}

private fun TiBasicLine.statement(): PsiElement? =
    children.firstOrNull()

private fun TiBasicLine.dataStatement(): TiBasicDataStatement? =
    children.filterIsInstance<TiBasicDataStatement>().firstOrNull()

private const val DEFAULT_FOR_STEP = 1

private fun resolveIfJumpTargetIndex(
    statement: TiBasicIfStatement,
    readDataVariableValues: Map<String, String>,
    lineNumberToIndex: Map<Int, Int>,
): Int? {
    val conditionResult = evaluateIfCondition(statement.conditionExpression(), readDataVariableValues) ?: return null
    val targetLineNumber = if (conditionResult) statement.thenLineNumber() else statement.elseLineNumber()
    return targetLineNumber?.let(lineNumberToIndex::get)
}

private fun evaluateIfCondition(
    expression: TiBasicExpression?,
    readDataVariableValues: Map<String, String>,
): Boolean? {
    expression ?: return null
    val children = expression.node.nonWhitespaceChildren
    val operatorIndex = children.indexOfFirst { it.elementType in RELATIONAL_OPERATORS }
    if (operatorIndex <= 0 || operatorIndex >= children.lastIndex) return null
    val left = resolveNumericExpressionNodes(children.subList(0, operatorIndex)) { variableAccess ->
        variableAccess.name?.let { variableName ->
            readDataVariableValues[variableName]?.toIntOrNull()
        }
    } ?: return null
    val right = resolveNumericExpressionNodes(children.subList(operatorIndex + 1, children.size)) { variableAccess ->
        variableAccess.name?.let { variableName ->
            readDataVariableValues[variableName]?.toIntOrNull()
        }
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
