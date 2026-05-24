package com.github.mmrsic.idea.plugins.tibasic.debug

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.util.EventDispatcher
import java.util.EventListener

@Service(Service.Level.PROJECT)
class TiBasicDebugSessionService(private val project: Project) {

    private val eventDispatcher = EventDispatcher.create(TiBasicDebugSessionListener::class.java)
    private var currentSession: TiBasicDebugSession? = null

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
        val updatedSession = currentSession?.step() ?: return
        currentSession = updatedSession
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
