package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.ext.nonWhitespaceChildren
import com.github.mmrsic.idea.plugins.tibasic.lang.BadValue
import com.github.mmrsic.idea.plugins.tibasic.lang.TiColor
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicCallStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicExpression
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement

class TiBasicCallColorLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element.node.elementType != TiBasicTokenTypes.CALL_SUBPROGRAM_NAME) return null
        val callStatement = element.parent as? TiBasicCallStatement ?: return null
        if (callStatement.subprogramName() != "COLOR") return null
        val args = callStatement.arguments()
        val fg = colorFromArg(args.getOrNull(1))
        val bg = colorFromArg(args.getOrNull(2))
        val icon = TiBasicColorPreviewIcon(fg, bg)
        return LineMarkerInfo(
            element,
            element.textRange,
            icon,
            { "CALL COLOR: fg=${fg.name}, bg=${bg.name}" },
            null,
            GutterIconRenderer.Alignment.LEFT,
            { "CALL COLOR preview" },
        )
    }

    private fun colorFromArg(expr: TiBasicExpression?): TiColor {
        expr ?: return TiColor.Transparent
        val children = expr.node.nonWhitespaceChildren
        if (children.size != 1) return TiColor.Transparent
        val literal = children.first()
            .takeIf { it.elementType == TiBasicTokenTypes.NUMERIC_LITERAL }
            ?: return TiColor.Transparent
        val value = literal.text.toIntOrNull() ?: return TiColor.Transparent
        return try {
            TiColor.at(value)
        } catch (_: BadValue) {
            TiColor.Transparent
        }
    }
}
