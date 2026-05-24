package com.github.mmrsic.idea.plugins.tibasic.ide.debug

import com.github.mmrsic.idea.plugins.tibasic.editor.TiBasicSoundPlayback
import com.github.mmrsic.idea.plugins.tibasic.editor.tiBasicSoundPlaybackService
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.util.EventDispatcher
import java.util.EventListener

@Service(Service.Level.PROJECT)
class TiBasicDebugSessionService(private val project: Project) {

    private val eventDispatcher = EventDispatcher.create(TiBasicDebugSessionListener::class.java)
    private var currentSession: TiBasicDebugSession? = null
    internal var soundPlaybackHandler: (Project, TiBasicSoundPlayback) -> Unit =
        { currentProject, playback -> tiBasicSoundPlaybackService.playSound(currentProject, playback) }

    internal fun addListener(listener: TiBasicDebugSessionListener, parentDisposable: com.intellij.openapi.Disposable) {
        eventDispatcher.addListener(listener, parentDisposable)
    }

    internal fun currentSession(): TiBasicDebugSession? = currentSession

    internal fun startSession(snapshot: TiBasicDebugProgramSnapshot): TiBasicDebugSession {
        currentSession = snapshot.initialSession()
        notifyListeners()
        return currentSession!!
    }

    internal fun step() {
        val stepResult = currentSession?.stepWithEffects() ?: return
        currentSession = stepResult.session
        stepResult.soundPlayback?.let { playback -> soundPlaybackHandler(project, playback) }
        notifyListeners()
    }

    internal fun updateKeyboardScanInput(input: String) {
        val updatedSession = currentSession?.copy(keyboardScanInput = input) ?: return
        currentSession = updatedSession
        notifyListeners()
    }

    internal fun stop() {
        val updatedSession = currentSession?.stop() ?: return
        currentSession = updatedSession
        notifyListeners()
    }

    private fun notifyListeners() {
        eventDispatcher.multicaster.sessionChanged(project, currentSession)
    }
}

internal fun interface TiBasicDebugSessionListener : EventListener {
    fun sessionChanged(project: Project, session: TiBasicDebugSession?)
}
