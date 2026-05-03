package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement

class TiBasicCallColorLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val callStatement = callStatementForSubprogram(element, "COLOR") ?: return null
        val file = callStatement.containingFile as? TiBasicFile
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
