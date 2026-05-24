package com.github.mmrsic.idea.plugins.tibasic.ide.editor.markers

import com.github.mmrsic.idea.plugins.tibasic.editor.CALL_CHAR_SUBPROGRAM
import com.github.mmrsic.idea.plugins.tibasic.editor.TiBasicCharPatternIcon
import com.github.mmrsic.idea.plugins.tibasic.language.analysis.calls.resolveCallCharDefinition
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.TiBasicFile
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement

class TiBasicCallCharLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val callStatement = callStatementForSubprogram(element, CALL_CHAR_SUBPROGRAM) ?: return null
        val file = callStatement.containingFile as? TiBasicFile ?: return null
        val definition = resolveCallCharDefinition(callStatement, file) ?: return null
        val icon = TiBasicCharPatternIcon(definition.pattern)
        return LineMarkerInfo(
            element,
            element.textRange,
            icon,
            { "CALL CHAR: ${definition.pattern}" },
            null,
            GutterIconRenderer.Alignment.LEFT,
            { "CALL CHAR character preview" },
        )
    }
}
