package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.psi.containingTiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.expression.TiBasicCallStatement
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement

class TiBasicCallScreenLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element.node.elementType != TiBasicTokenTypes.CALL_SUBPROGRAM_NAME) return null
        val callStatement = element.parent as? TiBasicCallStatement ?: return null
        if (callStatement.subprogramName() != "SCREEN") return null
        val file = callStatement.containingTiBasicFile
        val color = colorFromArg(callStatement.arguments().getOrNull(0), file)
        val icon = TiBasicScreenColorIcon(color)
        return LineMarkerInfo(
            element,
            element.textRange,
            icon,
            { "CALL SCREEN: ${color.name}" },
            null,
            GutterIconRenderer.Alignment.LEFT,
            { "CALL SCREEN color preview" },
        )
    }
}
