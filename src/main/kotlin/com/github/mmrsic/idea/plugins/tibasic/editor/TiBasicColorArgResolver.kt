package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.language.model.BadValue
import com.github.mmrsic.idea.plugins.tibasic.language.model.TiColor
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicExpression

internal fun colorFromArg(expr: TiBasicExpression?, file: TiBasicFile?): TiColor {
    val value = resolveConstantNumericValue(expr, file)
    return tiColorAt(value) ?: TiColor.Transparent
}

internal fun tiColorAt(value: Int?): TiColor? {
    value ?: return null
    return try {
        TiColor.at(value)
    } catch (_: BadValue) {
        null
    }
}
