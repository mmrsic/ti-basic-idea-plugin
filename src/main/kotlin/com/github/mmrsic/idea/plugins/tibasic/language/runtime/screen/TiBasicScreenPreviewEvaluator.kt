package com.github.mmrsic.idea.plugins.tibasic.language.runtime.screen

import com.github.mmrsic.idea.plugins.tibasic.editor.asciiCharacterName
import com.github.mmrsic.idea.plugins.tibasic.editor.callColorCharacterSetRange
import com.github.mmrsic.idea.plugins.tibasic.editor.normalizeHexPattern
import com.github.mmrsic.idea.plugins.tibasic.editor.tiColorAt
import com.github.mmrsic.idea.plugins.tibasic.language.analysis.resolveNumericExpressionValue
import com.github.mmrsic.idea.plugins.tibasic.language.model.TiColor
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicCallStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicExpression
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicVariableAccess
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicLetStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicLine
import com.github.mmrsic.idea.plugins.tibasic.language.analysis.variables.AccessType
import com.github.mmrsic.idea.plugins.tibasic.language.analysis.variables.TiBasicVariableCollector
import com.intellij.openapi.editor.SelectionModel
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

private const val SCREEN_COLUMNS = 32
private const val SCREEN_ROWS = 24
private const val SCREEN_CELLS = SCREEN_COLUMNS * SCREEN_ROWS
private const val SPACE_CHARACTER_CODE = 32
private const val DEFAULT_REPEAT_COUNT = 1
private val PREVIEWABLE_SUBPROGRAMS = setOf("HCHAR", "VCHAR", "CHAR", "COLOR", "SCREEN", "CLEAR")
private val SCREEN_WRITER_SUBPROGRAMS = setOf("HCHAR", "VCHAR")
private val DEFAULT_SCREEN_COLORS = TiBasicScreenColors(
    fg = TiColor.Black,
    bg = TiColor.White,
)

data class TiBasicScreenPreview(
    val cells: List<List<TiBasicScreenPreviewCell>>,
    val warnings: List<String>,
) {
    val isPartial: Boolean
        get() = warnings.isNotEmpty()

    fun cellAt(row: Int, column: Int): TiBasicScreenPreviewCell =
        cells[row - 1][column - 1]
}

data class TiBasicScreenPreviewCell(
    val code: Int,
    val pattern: String?,
    val fg: TiColor,
    val bg: TiColor,
) {
    val displayText: String?
        get() = printableAsciiCharacter(code)
}

private data class TiBasicScreenPosition(
    val row: Int,
    val column: Int,
)

private data class TiBasicScreenColors(
    val fg: TiColor,
    val bg: TiColor,
)

private data class TiBasicMutableScreenState(
    val codes: Array<IntArray> = Array(SCREEN_ROWS) { IntArray(SCREEN_COLUMNS) { SPACE_CHARACTER_CODE } },
    val charOverrides: MutableMap<Int, String> = mutableMapOf(),
    val characterSetColors: MutableMap<Int, TiBasicScreenColors> = mutableMapOf(),
    var screenColor: TiColor = DEFAULT_SCREEN_COLORS.bg,
) {
    fun reset() {
        codes.indices.forEach { row ->
            codes[row].fill(SPACE_CHARACTER_CODE)
        }
        charOverrides.clear()
        characterSetColors.clear()
        screenColor = DEFAULT_SCREEN_COLORS.bg
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TiBasicMutableScreenState

        if (!codes.contentDeepEquals(other.codes)) return false
        if (charOverrides != other.charOverrides) return false
        if (characterSetColors != other.characterSetColors) return false
        if (screenColor != other.screenColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = codes.contentDeepHashCode()
        result = 31 * result + charOverrides.hashCode()
        result = 31 * result + characterSetColors.hashCode()
        result = 31 * result + screenColor.hashCode()
        return result
    }
}

private data class TiBasicPreviewEvaluationContext(
    val numericVariables: MutableMap<String, Int> = mutableMapOf(),
    val stringVariables: MutableMap<String, String> = mutableMapOf(),
    val warnings: MutableList<String> = mutableListOf(),
    val screenState: TiBasicMutableScreenState = TiBasicMutableScreenState(),
) {
    fun warning(line: TiBasicLine, reason: String) {
        warnings += "Line ${line.lineNumber()}: $reason"
    }
}

internal fun hasPreviewableSelection(file: TiBasicFile, selectionModel: SelectionModel): Boolean =
    selectedPreviewLines(file, selectionModel)
        .mapNotNull(TiBasicLine::statement)
        .filterIsInstance<TiBasicCallStatement>()
        .any { statement -> statement.subprogramName() in SCREEN_WRITER_SUBPROGRAMS }

internal fun selectedPreviewLines(file: TiBasicFile, selectionModel: SelectionModel): List<TiBasicLine> {
    if (!selectionModel.hasSelection()) return emptyList()
    val selectionStart = selectionModel.selectionStart
    val selectionEnd = selectionModel.selectionEnd
    return file.lines().filter { line ->
        line.textRange.startOffset < selectionEnd &&
            line.textRange.endOffset > selectionStart
    }
}

internal fun evaluateSelectedScreenPreview(lines: List<TiBasicLine>): TiBasicScreenPreview {
    val context = TiBasicPreviewEvaluationContext()
    lines.forEach { line ->
        when (val statement = line.statement()) {
            is TiBasicLetStatement -> applySelectedLetStatement(statement, context)
            is TiBasicCallStatement -> applyPreviewCallStatement(line, statement, context)
            else -> invalidateWrittenVariables(statement, context)
        }
    }
    return TiBasicScreenPreview(
        cells = buildPreviewCells(context.screenState),
        warnings = context.warnings.toList(),
    )
}

private fun buildPreviewCells(screenState: TiBasicMutableScreenState): List<List<TiBasicScreenPreviewCell>> =
    screenState.codes.map { row ->
        row.map { code ->
            val colors = colorsForCode(code, screenState)
            TiBasicScreenPreviewCell(
                code = code,
                pattern = screenState.charOverrides[code],
                fg = colors.fg,
                bg = colors.bg,
            )
        }
    }

private fun colorsForCode(code: Int, screenState: TiBasicMutableScreenState): TiBasicScreenColors {
    return screenState.characterSetColors.entries
        .firstOrNull { (set, _) -> code in (callColorCharacterSetRange(set) ?: IntRange.EMPTY) }
        ?.value
        ?: DEFAULT_SCREEN_COLORS.copy(bg = screenState.screenColor)
}

private fun printableAsciiCharacter(code: Int): String? =
    asciiCharacterName(code)
        ?.takeIf { name -> name.length == 1 }

private fun applySelectedLetStatement(
    statement: TiBasicLetStatement,
    context: TiBasicPreviewEvaluationContext,
) {
    val lhs = statement.targetVariableAccess() ?: return
    val variableName = lhs.name ?: return
    if (lhs.hasSubscriptParens()) {
        context.numericVariables.remove(variableName)
        context.stringVariables.remove(variableName)
        return
    }
    val rhs = statement.assignedExpression()
    if (variableName.endsWith('$')) {
        resolveSelectedStringValue(rhs, context)?.let { resolved ->
            context.stringVariables[variableName] = resolved
        } ?: context.stringVariables.remove(variableName)
    } else {
        resolveSelectedNumericValue(rhs, context)?.let { resolved ->
            context.numericVariables[variableName] = resolved
        } ?: context.numericVariables.remove(variableName)
    }
}

private fun applyPreviewCallStatement(
    line: TiBasicLine,
    statement: TiBasicCallStatement,
    context: TiBasicPreviewEvaluationContext,
) {
    val subprogramName = statement.subprogramName() ?: return
    if (subprogramName !in PREVIEWABLE_SUBPROGRAMS) {
        invalidateWrittenVariables(statement, context)
        return
    }
    when (subprogramName) {
        "HCHAR" -> applyScreenWrite(line, statement, context, horizontal = true)
        "VCHAR" -> applyScreenWrite(line, statement, context, horizontal = false)
        "CHAR" -> applyCallChar(line, statement, context)
        "COLOR" -> applyCallColor(line, statement, context)
        "SCREEN" -> applyCallScreen(line, statement, context)
        "CLEAR" -> context.screenState.reset()
    }
    invalidateWrittenVariables(statement, context)
}

private fun applyScreenWrite(
    line: TiBasicLine,
    statement: TiBasicCallStatement,
    context: TiBasicPreviewEvaluationContext,
    horizontal: Boolean,
) {
    val args = statement.arguments()
    val row = resolveSelectedNumericValue(args.getOrNull(0), context)
    val column = resolveSelectedNumericValue(args.getOrNull(1), context)
    val code = resolveSelectedNumericValue(args.getOrNull(2), context)
    val repeatCount = args.getOrNull(3)?.let { expr -> resolveSelectedNumericValue(expr, context) }
    if (row == null || column == null || code == null || (args.size > 3 && repeatCount == null)) {
        context.warning(
            line,
            "skipped CALL ${statement.subprogramName()} because its arguments are not statically resolvable in the selection",
        )
        return
    }
    val resolvedRepeatCount = repeatCount ?: DEFAULT_REPEAT_COUNT
    if (resolvedRepeatCount < 0) {
        context.warning(line, "skipped CALL ${statement.subprogramName()} because repeat count $resolvedRepeatCount is negative")
        return
    }
    repeat(resolvedRepeatCount) { offset ->
        screenPosition(row, column, offset, horizontal)?.let { target ->
            context.screenState.codes[target.row - 1][target.column - 1] = code
        }
    }
}

private fun screenPosition(
    row: Int,
    column: Int,
    offset: Int,
    horizontal: Boolean,
): TiBasicScreenPosition? {
    if (row !in 1..SCREEN_ROWS || column !in 1..SCREEN_COLUMNS) {
        return null
    }
    val wrappedIndex = if (horizontal) {
        ((row - 1) * SCREEN_COLUMNS + (column - 1) + offset) % SCREEN_CELLS
    } else {
        ((column - 1) * SCREEN_ROWS + (row - 1) + offset) % SCREEN_CELLS
    }
    return if (horizontal) {
        TiBasicScreenPosition(
            row = wrappedIndex / SCREEN_COLUMNS + 1,
            column = wrappedIndex % SCREEN_COLUMNS + 1,
        )
    } else {
        TiBasicScreenPosition(
            row = wrappedIndex % SCREEN_ROWS + 1,
            column = wrappedIndex / SCREEN_ROWS + 1,
        )
    }
}

private fun applyCallChar(
    line: TiBasicLine,
    statement: TiBasicCallStatement,
    context: TiBasicPreviewEvaluationContext,
) {
    val args = statement.arguments()
    val code = resolveSelectedNumericValue(args.getOrNull(0), context)
    val pattern = normalizeHexPattern(resolveSelectedStringValue(args.getOrNull(1), context))
    if (code == null || pattern == null) {
        context.warning(line, "skipped CALL CHAR because character code or pattern is not statically resolvable in the selection")
        return
    }
    context.screenState.charOverrides[code] = pattern
}

private fun applyCallColor(
    line: TiBasicLine,
    statement: TiBasicCallStatement,
    context: TiBasicPreviewEvaluationContext,
) {
    val args = statement.arguments()
    val set = resolveSelectedNumericValue(args.getOrNull(0), context)
    val fg = resolveSelectedNumericValue(args.getOrNull(1), context)?.let(::tiColorAt)
    val bg = resolveSelectedNumericValue(args.getOrNull(2), context)?.let(::tiColorAt)
    if (set == null || fg == null || bg == null || callColorCharacterSetRange(set) == null) {
        context.warning(line, "skipped CALL COLOR because set or colors are not statically resolvable in the selection")
        return
    }
    context.screenState.characterSetColors[set] = TiBasicScreenColors(fg = fg, bg = bg)
}

private fun applyCallScreen(
    line: TiBasicLine,
    statement: TiBasicCallStatement,
    context: TiBasicPreviewEvaluationContext,
) {
    val color = statement.arguments()
        .getOrNull(0)
        ?.let { expr -> resolveSelectedNumericValue(expr, context) }
        ?.let(::tiColorAt)
    if (color == null) {
        context.warning(line, "skipped CALL SCREEN because the color is not statically resolvable in the selection")
        return
    }
    context.screenState.screenColor = color
}

private fun resolveSelectedNumericValue(
    expression: TiBasicExpression?,
    context: TiBasicPreviewEvaluationContext,
): Int? =
    resolveNumericExpressionValue(expression) { variableAccess ->
        variableAccess.name?.let(context.numericVariables::get)
    }

private fun resolveSelectedStringValue(
    expression: TiBasicExpression?,
    context: TiBasicPreviewEvaluationContext,
): String? {
    expression ?: return null
    val variableAccess = expression.variableAccess()
    return when {
        variableAccess != null && !variableAccess.hasSubscriptParens() ->
            variableAccess.name?.let(context.stringVariables::get)

        else -> expression.text
            .takeIf { it.startsWith('"') && it.endsWith('"') && it.length >= 2 }
            ?.removePrefix("\"")
            ?.removeSuffix("\"")
    }
}

private fun invalidateWrittenVariables(
    statement: PsiElement?,
    context: TiBasicPreviewEvaluationContext,
) {
    statement ?: return
    PsiTreeUtil.findChildrenOfType(statement, TiBasicVariableAccess::class.java)
        .filter { variableAccess -> TiBasicVariableCollector.determineAccessType(variableAccess) == AccessType.WRITE }
        .mapNotNull(TiBasicVariableAccess::getName)
        .forEach { variableName ->
            context.numericVariables.remove(variableName)
            context.stringVariables.remove(variableName)
        }
}
