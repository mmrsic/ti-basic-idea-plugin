package com.github.mmrsic.idea.plugins.tibasic.toolwindow

import com.intellij.openapi.project.Project

class TiBasicCharacterDefinitionsToolWindowFactory :
    TiBasicToolWindowFactory<TiBasicCharacterDefinitionsToolWindowContent>() {

    override fun createContentComponent(project: Project): TiBasicCharacterDefinitionsToolWindowContent =
        TiBasicCharacterDefinitionsToolWindowContent(project)
}
