package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.psi.containingTiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.expression.TiBasicCallStatement
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

}
