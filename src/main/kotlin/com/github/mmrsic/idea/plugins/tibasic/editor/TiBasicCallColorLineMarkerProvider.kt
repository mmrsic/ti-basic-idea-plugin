package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.ext.nonWhitespaceChildren
import com.github.mmrsic.idea.plugins.tibasic.lang.BadValue
import com.github.mmrsic.idea.plugins.tibasic.lang.TiColor
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicCallStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicExpression
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.containingTiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.toolwindow.TiBasicVariableCollector
import com.github.mmrsic.idea.plugins.tibasic.toolwindow.TiBasicVariableType
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement

class TiBasicCallColorLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element.node.elementType != TiBasicTokenTypes.CALL_SUBPROGRAM_NAME) return null
        val callStatement = element.parent as? TiBasicCallStatement ?: return null
        if (callStatement.subprogramName() != "COLOR") return null
        val file = callStatement.containingTiBasicFile
        val args = callStatement.arguments()
        val fg = colorFromArg(args.getOrNull(1), file)
        val bg = colorFromArg(args.getOrNull(2), file)
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

    private fun colorFromArg(expr: TiBasicExpression?, file: TiBasicFile?): TiColor {
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
}
