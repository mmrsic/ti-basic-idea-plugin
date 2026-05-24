package com.github.mmrsic.idea.plugins.tibasic.language.analysis.calls

import com.github.mmrsic.idea.plugins.tibasic.editor.callColorCharacterSetRange
import com.github.mmrsic.idea.plugins.tibasic.editor.displayedScreenBackground
import com.github.mmrsic.idea.plugins.tibasic.editor.roundedScreenColorAt
import com.github.mmrsic.idea.plugins.tibasic.editor.tiColorAt
import com.github.mmrsic.idea.plugins.tibasic.language.analysis.resolveDecimalExpressionValue
import com.github.mmrsic.idea.plugins.tibasic.language.analysis.variables.TiBasicVariableCollector
import com.github.mmrsic.idea.plugins.tibasic.language.model.TiColor
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicCallStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicVariableAccess
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicLine
import com.github.mmrsic.idea.plugins.tibasic.language.values.parseTiBasicDecimalLiteral
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import java.math.BigDecimal

internal data class TiBasicCallColorAssignment(
    val set: Int,
    val codeRange: IntRange,
    val fg: TiColor,
    val bg: TiColor,
    val screenBackground: TiColor,
    val lineNumber: Int,
    val offset: Int,
)

internal fun collectCallColorAssignments(file: TiBasicFile): List<TiBasicCallColorAssignment> =
    CachedValuesManager.getCachedValue(file) {
        CachedValueProvider.Result.create(
            buildList {
                var screenBackground = DEFAULT_SCREEN_BACKGROUND
                collectStaticallyTraceableCallStatements(file).forEach { statement ->
                    resolveCallScreenBackground(statement.callStatement, file, statement.staticValues)?.let { resolved ->
                        screenBackground = resolved
                    }
                    resolveCallColorAssignment(
                        callStatement = statement.callStatement,
                        file = file,
                        staticValues = statement.staticValues,
                        screenBackground = screenBackground,
                    )?.let(::add)
                }
            },
            file,
        )
    }

internal fun resolveCallColorAssignment(
    callStatement: TiBasicCallStatement,
    file: TiBasicFile,
    staticValues: StaticValueSnapshot = StaticValueSnapshot(),
    screenBackground: TiColor = DEFAULT_SCREEN_BACKGROUND,
): TiBasicCallColorAssignment? {
    if (callStatement.subprogramName() != CALL_COLOR_SUBPROGRAM_NAME) return null
    val args = callStatement.arguments()
    val set = resolveStaticNumericValue(args.getOrNull(CALL_COLOR_SET_INDEX), file, staticValues) ?: return null
    val codeRange = callColorCharacterSetRange(set) ?: return null
    val fgValue = resolveStaticNumericValue(args.getOrNull(CALL_COLOR_FOREGROUND_INDEX), file, staticValues)
    val bgValue = resolveStaticNumericValue(args.getOrNull(CALL_COLOR_BACKGROUND_INDEX), file, staticValues)
    val fg = tiColorAt(fgValue) ?: return null
    val bg = tiColorAt(bgValue) ?: return null
    val line = PsiTreeUtil.getParentOfType(callStatement, TiBasicLine::class.java) ?: return null
    return TiBasicCallColorAssignment(
        set = set,
        codeRange = codeRange,
        fg = fg,
        bg = bg,
        screenBackground = screenBackground,
        lineNumber = line.lineNumber(),
        offset = line.textOffset,
    )
}

internal fun resolveCallScreenBackground(
    callStatement: TiBasicCallStatement,
    file: TiBasicFile,
    staticValues: StaticValueSnapshot = StaticValueSnapshot(),
): TiColor? {
    if (callStatement.subprogramName() != CALL_SCREEN_SUBPROGRAM_NAME) return null
    val color = roundedScreenColorAt(
        resolveDecimalExpressionValue(callStatement.arguments().getOrNull(CALL_SCREEN_COLOR_INDEX)) { variableAccess ->
            resolveStaticDecimalValue(variableAccess, file, staticValues)
        },
    ) ?: return null
    return displayedScreenBackground(color)
}

private fun resolveStaticDecimalValue(
    variableAccess: TiBasicVariableAccess,
    file: TiBasicFile,
    staticValues: StaticValueSnapshot,
): BigDecimal? =
    if (variableAccess.hasSubscriptParens()) {
        null
    } else {
        staticValues.numericVariables[variableAccess.name]
            ?.toBigDecimal()
            ?: TiBasicVariableCollector.constantValueOf(variableAccess, file)?.let(::parseTiBasicDecimalLiteral)
    }

private const val CALL_COLOR_SUBPROGRAM_NAME = "COLOR"
private const val CALL_COLOR_SET_INDEX = 0
private const val CALL_COLOR_FOREGROUND_INDEX = 1
private const val CALL_COLOR_BACKGROUND_INDEX = 2
private const val CALL_SCREEN_SUBPROGRAM_NAME = "SCREEN"
private const val CALL_SCREEN_COLOR_INDEX = 0
private val DEFAULT_SCREEN_BACKGROUND = TiColor.White
