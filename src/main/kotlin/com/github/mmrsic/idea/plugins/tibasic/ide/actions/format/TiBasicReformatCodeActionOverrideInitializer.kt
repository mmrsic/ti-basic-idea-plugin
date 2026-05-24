package com.github.mmrsic.idea.plugins.tibasic.ide.actions.format

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class TiBasicReformatCodeActionOverrideInitializer : ProjectActivity {

    override suspend fun execute(project: Project) {
        val actionManager = ActionManager.getInstance()
        if (actionManager.getAction(TiBasicReformatCodeActionMetadata.actionId) is TiBasicReformatCodeAction) {
            return
        }
        actionManager.replaceAction(TiBasicReformatCodeActionMetadata.actionId, TiBasicReformatCodeAction())
    }
}
