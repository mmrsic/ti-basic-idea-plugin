package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.language.model.BadValue
import com.github.mmrsic.idea.plugins.tibasic.language.model.TiColor
import com.github.mmrsic.idea.plugins.tibasic.language.analysis.resolveDecimalExpressionValue
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicExpression
import com.github.mmrsic.idea.plugins.tibasic.language.analysis.variables.TiBasicVariableCollector
import com.github.mmrsic.idea.plugins.tibasic.language.values.parseTiBasicDecimalLiteral
import java.math.BigDecimal
import java.math.RoundingMode

internal fun colorFromArg(expr: TiBasicExpression?, file: TiBasicFile?): TiColor {
    val value = resolveConstantNumericValue(expr, file)
    return tiColorAt(value) ?: TiColor.Transparent
}

internal fun screenColorFromArg(expr: TiBasicExpression?, file: TiBasicFile?): TiColor =
    resolveRoundedScreenColor(expr, file)
        ?.let(::displayedScreenBackground)
        ?: TiColor.Transparent

internal fun resolveRoundedScreenColor(expr: TiBasicExpression?, file: TiBasicFile?): TiColor? =
    roundedScreenColorAt(
        resolveConstantNumericDecimalValue(expr, file),
    )

internal fun displayedScreenBackground(color: TiColor): TiColor =
    if (color == TiColor.Transparent) {
        TiColor.Black
    } else {
        color
    }

internal fun tiColorAt(value: Int?): TiColor? {
    value ?: return null
    return try {
        TiColor.at(value)
    } catch (_: BadValue) {
        null
    }
}

internal fun roundedScreenColorAt(value: BigDecimal?): TiColor? =
    value?.roundToWholeNumberIntOrNull()?.let { roundedValue ->
        try {
            TiColor.at(roundedValue)
        } catch (_: BadValue) {
            null
        }
    }

private fun resolveConstantNumericDecimalValue(expression: TiBasicExpression?, file: TiBasicFile?): BigDecimal? {
    file ?: return null
    return resolveDecimalExpressionValue(expression) { variableAccess ->
        TiBasicVariableCollector.constantValueOf(variableAccess, file)?.let(::parseTiBasicDecimalLiteral)
    }
}

private fun BigDecimal.roundToWholeNumberIntOrNull(): Int? =
    runCatching { setScale(0, RoundingMode.HALF_UP).intValueExact() }.getOrNull()
