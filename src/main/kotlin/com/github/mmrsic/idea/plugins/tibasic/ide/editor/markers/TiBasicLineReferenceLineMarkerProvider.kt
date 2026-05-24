package com.github.mmrsic.idea.plugins.tibasic.ide.editor.markers

import com.github.mmrsic.idea.plugins.tibasic.language.analysis.references.TiBasicInboundLineReferenceCollector
import com.github.mmrsic.idea.plugins.tibasic.language.analysis.references.referencedByTooltip
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.common.VALID_LINE_NUMBER_RANGE
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.containingTiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicLine
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiElement

class TiBasicLineReferenceLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element.node.elementType != TiBasicTokenTypes.LINE_NUMBER) return null
        val line = element.parent as? TiBasicLine ?: return null
        val lineNumber = line.lineNumber()
        if (lineNumber !in VALID_LINE_NUMBER_RANGE) return null
        val file = line.containingTiBasicFile ?: return null
        val inboundReferences = TiBasicInboundLineReferenceCollector.collectCached(file)[lineNumber]
            ?.filter { it.sourceLine != line }
            ?.distinctBy { it.sourceLine.textOffset }
            ?: return null
        if (inboundReferences.isEmpty()) return null
        return NavigationGutterIconBuilder
            .create(IconLoader.getIcon("/icons/line_reference_inbound.svg", javaClass))
            .setTargets(inboundReferences.map { it.sourceLine })
            .setTooltipText(referencedByTooltip(inboundReferences))
            .setAlignment(GutterIconRenderer.Alignment.RIGHT)
            .createLineMarkerInfo(element)
    }
}
