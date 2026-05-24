package com.github.mmrsic.idea.plugins.tibasic.ide.toolwindow.characters

import com.github.mmrsic.idea.plugins.tibasic.ide.toolwindow.TiBasicToolWindowFactory
import com.intellij.openapi.project.Project

class TiBasicCharacterDefinitionsToolWindowFactory :
    TiBasicToolWindowFactory<TiBasicCharacterDefinitionsToolWindowContent>() {

    override fun createContentComponent(project: Project): TiBasicCharacterDefinitionsToolWindowContent =
        TiBasicCharacterDefinitionsToolWindowContent(project)
}
