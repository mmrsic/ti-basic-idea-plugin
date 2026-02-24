package com.github.mmrsic.idea.plugins.tibasic.action.resequence

import com.github.mmrsic.idea.plugins.tibasic.action.TiBasicFileAction
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.util.replaceFileText
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class ResequenceAction : TiBasicFileAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val file = event.getData(CommonDataKeys.PSI_FILE) as? TiBasicFile ?: return
        val dialog = ResequenceOptionsDialog(project)
        if (!dialog.showAndGet()) return
        val newText = resequencedText(file, start = dialog.chosenStart, step = dialog.chosenStep)
        replaceFileText(project, file, newText)
    }
}
