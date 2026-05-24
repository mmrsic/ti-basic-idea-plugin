package com.github.mmrsic.idea.plugins.tibasic.debug.run

import com.github.mmrsic.idea.plugins.tibasic.debug.TiBasicDebugMetadata
import com.github.mmrsic.idea.plugins.tibasic.debug.TiBasicDebugProgramSnapshot
import com.github.mmrsic.idea.plugins.tibasic.debug.TiBasicDebugSessionService
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager

class TiBasicDebugRunProfileState(
    private val project: Project,
    private val configuration: TiBasicDebugRunConfiguration,
) : RunProfileState {

    override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult? {
        val filePath = configuration.filePath ?: throw ExecutionException(
            TiBasicDebugMetadata.message(TiBasicDebugMetadata.configurationEditorFileLabelKey),
        )
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath)
            ?: throw ExecutionException(filePath)
        val document = FileDocumentManager.getInstance().getDocument(virtualFile)
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        val snapshot = ReadAction.compute<TiBasicDebugProgramSnapshot, RuntimeException> {
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile) as? TiBasicFile
                ?: throw ExecutionException(filePath)
            TiBasicDebugProgramSnapshot.create(psiFile, document)
        }
        project.getService(TiBasicDebugSessionService::class.java)
            .startSession(snapshot)
        ToolWindowManager.getInstance(project)
            .getToolWindow(TiBasicDebugMetadata.toolWindowId)
            ?.show(null)
        return null
    }
}
