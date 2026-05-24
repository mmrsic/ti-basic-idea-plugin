package com.github.mmrsic.idea.plugins.tibasic.ide.editor.markers

import com.github.mmrsic.idea.plugins.tibasic.editor.TiBasicScreenColorIcon
import com.github.mmrsic.idea.plugins.tibasic.editor.screenColorFromArg
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.TiBasicFile
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement

class TiBasicCallScreenLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val callStatement = callStatementForSubprogram(element, "SCREEN") ?: return null
        val file = callStatement.containingFile as? TiBasicFile
        val color = screenColorFromArg(callStatement.arguments().getOrNull(0), file)
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
