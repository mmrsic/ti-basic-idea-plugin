package com.github.mmrsic.idea.plugins.tibasic.language.analysis.calls

import com.github.mmrsic.idea.plugins.tibasic.editor.CALL_CHAR_SUBPROGRAM
import com.github.mmrsic.idea.plugins.tibasic.editor.asciiCharacterName
import com.github.mmrsic.idea.plugins.tibasic.editor.normalizeHexPattern
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicCallStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicLine
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil

internal data class TiBasicCallCharDefinition(
    val code: Int,
    val pattern: String,
    val lineNumber: Int,
    val offset: Int,
) {
    val ascii: String? get() = asciiCharacterName(code)
}

internal fun collectCallCharDefinitions(file: TiBasicFile): List<TiBasicCallCharDefinition> =
    CachedValuesManager.getCachedValue(file) {
        CachedValueProvider.Result.create(
            collectStaticallyTraceableCallStatements(file)
                .mapNotNull { statement ->
                    resolveCallCharDefinition(statement.callStatement, file, statement.staticValues)
                }
                .sortedWith(compareBy(TiBasicCallCharDefinition::code, TiBasicCallCharDefinition::lineNumber)),
            file,
        )
    }

internal fun resolveCallCharDefinition(
    callStatement: TiBasicCallStatement,
    file: TiBasicFile,
    staticValues: StaticValueSnapshot = StaticValueSnapshot(),
): TiBasicCallCharDefinition? {
    if (callStatement.subprogramName() != CALL_CHAR_SUBPROGRAM) return null
    val code = resolveCallCharNumericValue(callStatement.arguments().getOrNull(0), file, staticValues) ?: return null
    val pattern = normalizeHexPattern(
        resolveCallCharStringValue(callStatement.arguments().getOrNull(1), file, staticValues),
    ) ?: return null
    val line = PsiTreeUtil.getParentOfType(callStatement, TiBasicLine::class.java) ?: return null
    return TiBasicCallCharDefinition(
        code = code,
        pattern = pattern,
        lineNumber = line.lineNumber(),
        offset = line.textOffset,
    )
}

private fun resolveCallCharNumericValue(
    expression: com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicExpression?,
    file: TiBasicFile,
    staticValues: StaticValueSnapshot,
): Int? =
    resolveStaticNumericValue(expression, file, staticValues)

private fun resolveCallCharStringValue(
    expression: com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicExpression?,
    file: TiBasicFile,
    staticValues: StaticValueSnapshot,
): String? =
    resolveStaticStringValue(expression, file, staticValues)
