package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.lang.BadValue
import com.github.mmrsic.idea.plugins.tibasic.lang.TiColor
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.expression.TiBasicExpression

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
