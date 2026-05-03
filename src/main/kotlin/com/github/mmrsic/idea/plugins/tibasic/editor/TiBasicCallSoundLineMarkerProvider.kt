package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement

class TiBasicCallSoundLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val callStatement = callStatementForSubprogram(element, CALL_SOUND_SUBPROGRAM) ?: return null
        val playback = resolveSoundPlayback(callStatement, callStatement.containingFile as? TiBasicFile) ?: return null
        return LineMarkerInfo(
            element,
            element.textRange,
            AllIcons.Actions.Execute,
            { callSoundTooltip(playback) },
            { _, clickedElement -> tiBasicSoundPlaybackService.playSound(clickedElement.project, playback) },
            GutterIconRenderer.Alignment.LEFT,
            { "Play CALL SOUND" },
        )
    }
}
