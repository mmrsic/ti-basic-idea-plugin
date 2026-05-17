package com.github.mmrsic.idea.plugins.tibasic.action.preview

import com.github.mmrsic.idea.plugins.tibasic.action.TiBasicFileAction
import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicBundle
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project

open class TiBasicScreenPreviewAction : TiBasicFileAction() {

    init {
        templatePresentation.text = TiBasicBundle.message(TiBasicScreenPreviewActionMetadata.textKey)
        templatePresentation.description = TiBasicBundle.message(TiBasicScreenPreviewActionMetadata.descriptionKey)
    }

    override fun update(event: AnActionEvent) {
        super.update(event)
        if (!event.presentation.isEnabledAndVisible) return
        val file = event.getData(CommonDataKeys.PSI_FILE) as? TiBasicFile ?: return
        val editor = event.getData(CommonDataKeys.EDITOR)
        event.presentation.isEnabled = editor != null && hasPreviewableSelection(file, editor.selectionModel)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val file = event.getData(CommonDataKeys.PSI_FILE) as? TiBasicFile ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val selectedLines = selectedPreviewLines(file, editor.selectionModel)
        if (selectedLines.isEmpty()) return
        showPreview(project, evaluateSelectedScreenPreview(selectedLines))
    }

    protected open fun showPreview(project: Project, preview: TiBasicScreenPreview) {
        TiBasicScreenPreviewDialog(project, preview).show()
    }
}
