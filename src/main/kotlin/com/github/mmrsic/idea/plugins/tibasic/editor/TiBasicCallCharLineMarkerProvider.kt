package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.ext.firstChildOfType
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicCallStatement
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement

class TiBasicCallCharLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element.node.elementType != TiBasicTokenTypes.CALL_SUBPROGRAM_NAME) return null
        val callStatement = element.parent as? TiBasicCallStatement ?: return null
        if (callStatement.subprogramName() != "CHAR") return null
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
        val rawText = callStatement.arguments().getOrNull(1)
            ?.node?.firstChildOfType(TiBasicTokenTypes.STRING_LITERAL)?.text
            ?: return null
        val pattern = rawText.removePrefix("\"").removeSuffix("\"")
        return if (isValidHexPattern(pattern)) pattern.uppercase().padEnd(16, '0') else null
    }

    private fun isValidHexPattern(pattern: String): Boolean =
        pattern.length in 0..16 && pattern.all { it in '0'..'9' || it.uppercaseChar() in 'A'..'F' }
}
