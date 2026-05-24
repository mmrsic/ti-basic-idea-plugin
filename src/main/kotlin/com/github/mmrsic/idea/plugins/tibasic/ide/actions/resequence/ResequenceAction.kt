package com.github.mmrsic.idea.plugins.tibasic.ide.actions.resequence

import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.common.util.replaceFileText
import com.github.mmrsic.idea.plugins.tibasic.ide.actions.TiBasicFileAction
import com.github.mmrsic.idea.plugins.tibasic.language.format.resequencedText
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
