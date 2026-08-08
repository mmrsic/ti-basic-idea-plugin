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

    internal fun skip() {
        var session = currentSession ?: return
        if (!session.canSkip()) return

        when (session.currentProgramLine?.semantics) {
            is TiBasicDebugLineSemantics.Next -> {
                var iterations = 0
                while (iterations < SKIP_ITERATION_CAP) {
                    if (session.nextWouldContinueLoop() == false) break

                    val stepResult = session.stepWithEffects()
                    session = stepResult.session
                    stepResult.soundPlayback?.let { playback -> soundPlaybackHandler(project, playback) }

                    if (session.hasBlockingDebugRequest() || session.status != TiBasicDebugSessionStatus.Paused) break

                    iterations++
                }
            }

            is TiBasicDebugLineSemantics.Gosub -> {
                session = skipSubroutine(session)
            }

            is TiBasicDebugLineSemantics.OnGosub -> {
                session = skipSubroutine(session)
            }

            else -> return
        }
        currentSession = session
        notifyListeners()
    }

    internal fun updateKeyboardScanInput(input: String) {
        val updatedSession = currentSession?.copy(keyboardScanInput = input) ?: return
        currentSession = updatedSession
        notifyListeners()
    }

    internal fun updatePendingInputValue(variableName: String, rawValue: String) {
        updatePendingInputValues(mapOf(variableName to rawValue))
    }

    internal fun updatePendingInputValues(values: Map<String, String>) {
        if (values.isEmpty()) return
        val updatedSession = currentSession?.let {
            it.copy(pendingInputValues = it.pendingInputValues + values)
        } ?: return
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

    private fun skipSubroutine(initialSession: TiBasicDebugSession): TiBasicDebugSession {
        val originalDepth = initialSession.gosubOriginLineNumbers.size
        var session = initialSession
        var iterations = 0
        while (iterations < SKIP_ITERATION_CAP) {
            val stepResult = session.stepWithEffects()
            session = stepResult.session
            stepResult.soundPlayback?.let { playback -> soundPlaybackHandler(project, playback) }

            if (session.hasBlockingDebugRequest() || session.status != TiBasicDebugSessionStatus.Paused) break
            if (session.gosubOriginLineNumbers.size <= originalDepth) break

            iterations++
        }
        return session
    }
}

internal fun interface TiBasicDebugSessionListener : EventListener {
    fun sessionChanged(project: Project, session: TiBasicDebugSession?)
}

private const val SKIP_ITERATION_CAP = 1000

internal fun TiBasicDebugSession.canSkip(): Boolean =
    status == TiBasicDebugSessionStatus.Paused &&
            (currentProgramLine?.semantics is TiBasicDebugLineSemantics.Next ||
                    currentProgramLine?.semantics is TiBasicDebugLineSemantics.Gosub ||
                    currentProgramLine?.semantics is TiBasicDebugLineSemantics.OnGosub)

private fun TiBasicDebugSession.hasBlockingDebugRequest(): Boolean =
    keyboardRequest != null || joystickRequest != null || inputRequest != null
