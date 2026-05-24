package com.github.mmrsic.idea.plugins.tibasic.language.analysis.calls

import com.github.mmrsic.idea.plugins.tibasic.editor.callColorCharacterSetRange
import com.github.mmrsic.idea.plugins.tibasic.editor.tiColorAt
import com.github.mmrsic.idea.plugins.tibasic.language.model.TiColor
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicCallStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicLine
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil

internal data class TiBasicCallColorAssignment(
    val set: Int,
    val codeRange: IntRange,
    val fg: TiColor,
    val bg: TiColor,
    val lineNumber: Int,
    val offset: Int,
)

internal fun collectCallColorAssignments(file: TiBasicFile): List<TiBasicCallColorAssignment> =
    CachedValuesManager.getCachedValue(file) {
        CachedValueProvider.Result.create(
            collectStaticallyTraceableCallStatements(file)
                .mapNotNull { statement ->
                    resolveCallColorAssignment(statement.callStatement, file, statement.staticValues)
                },
            file,
        )
    }

internal fun resolveCallColorAssignment(
    callStatement: TiBasicCallStatement,
    file: TiBasicFile,
    staticValues: StaticValueSnapshot = StaticValueSnapshot(),
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
        lineNumber = line.lineNumber(),
        offset = line.textOffset,
    )
}

private const val CALL_COLOR_SUBPROGRAM_NAME = "COLOR"
private const val CALL_COLOR_SET_INDEX = 0
private const val CALL_COLOR_FOREGROUND_INDEX = 1
private const val CALL_COLOR_BACKGROUND_INDEX = 2
