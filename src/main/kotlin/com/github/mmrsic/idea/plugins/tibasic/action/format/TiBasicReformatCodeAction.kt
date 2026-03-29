package com.github.mmrsic.idea.plugins.tibasic.action.format

import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.intellij.codeInsight.actions.ReformatCodeAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class TiBasicReformatCodeAction : ReformatCodeAction() {

    override fun actionPerformed(event: AnActionEvent) {
        if (event.getData(CommonDataKeys.PSI_FILE) is TiBasicFile) {
            FormatAction().actionPerformed(event)
        } else {
            super.actionPerformed(event)
        }
    }
}
