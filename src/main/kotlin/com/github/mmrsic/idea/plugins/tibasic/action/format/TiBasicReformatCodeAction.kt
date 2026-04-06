package com.github.mmrsic.idea.plugins.tibasic.action.format

import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicBundle
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.intellij.codeInsight.actions.ReformatCodeAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class TiBasicReformatCodeAction : ReformatCodeAction() {

    init {
        templatePresentation.text = TiBasicBundle.message(TiBasicReformatCodeActionMetadata.textKey)
        templatePresentation.description = TiBasicBundle.message(TiBasicReformatCodeActionMetadata.descriptionKey)
    }

    override fun actionPerformed(event: AnActionEvent) {
        if (event.getData(CommonDataKeys.PSI_FILE) is TiBasicFile) {
            FormatAction().actionPerformed(event)
        } else {
            super.actionPerformed(event)
        }
    }
}
