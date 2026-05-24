package com.github.mmrsic.idea.plugins.tibasic.ide.editor.markers

import com.github.mmrsic.idea.plugins.tibasic.editor.CALL_SOUND_SUBPROGRAM
import com.github.mmrsic.idea.plugins.tibasic.editor.callSoundTooltip
import com.github.mmrsic.idea.plugins.tibasic.editor.resolveSoundPlayback
import com.github.mmrsic.idea.plugins.tibasic.editor.tiBasicSoundPlaybackService
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.TiBasicFile
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
