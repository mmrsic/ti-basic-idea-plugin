package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.expression.TiBasicCallStatement
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement

class TiBasicCallCharLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val callStatement = callStatementForSubprogram(element, "CHAR") ?: return null
        val pattern = extractValidHexPattern(callStatement) ?: return null
        val icon = TiBasicCharPatternIcon(pattern)
        return LineMarkerInfo(
            element,
            element.textRange,
            icon,
            { "CALL CHAR: $pattern" },
            null,
            GutterIconRenderer.Alignment.LEFT,
            { "CALL CHAR character preview" },
        )
    }

    private fun extractValidHexPattern(callStatement: TiBasicCallStatement): String? {
        val file = callStatement.containingFile as? TiBasicFile ?: return null
        return normalizeHexPattern(
            resolveConstantStringValue(
                callStatement.arguments().getOrNull(1),
                file,
            ),
        )
    }
}
