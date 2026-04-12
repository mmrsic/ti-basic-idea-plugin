package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.ext.nonWhitespaceChildren
import com.github.mmrsic.idea.plugins.tibasic.lang.BadValue
import com.github.mmrsic.idea.plugins.tibasic.lang.TiColor
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.expression.TiBasicExpression
import com.github.mmrsic.idea.plugins.tibasic.toolwindow.TiBasicVariableCollector
import com.github.mmrsic.idea.plugins.tibasic.toolwindow.TiBasicVariableType

internal fun colorFromArg(expr: TiBasicExpression?, file: TiBasicFile?): TiColor {
    expr ?: return TiColor.Transparent
    val children = expr.node.nonWhitespaceChildren
    if (children.size != 1) return TiColor.Transparent
    val child = children.first()
    val value = when (child.elementType) {
        TiBasicTokenTypes.NUMERIC_LITERAL -> child.text.toIntOrNull() ?: return TiColor.Transparent
        TiBasicNodeTypes.VARIABLE_ACCESS -> {
            if (file == null) return TiColor.Transparent
            val varName = child.firstChildNode?.text?.uppercase() ?: return TiColor.Transparent
            TiBasicVariableCollector.collectCached(file)
                .find { it.name == varName && it.type == TiBasicVariableType.NUMERIC }
                ?.constValue
                ?.toIntOrNull() ?: return TiColor.Transparent
        }

        else -> return TiColor.Transparent
    }
    return try {
        TiColor.at(value)
    } catch (_: BadValue) {
        TiColor.Transparent
    }
}
