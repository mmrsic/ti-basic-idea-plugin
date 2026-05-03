package com.github.mmrsic.idea.plugins.tibasic.toolwindow

import com.intellij.openapi.project.Project

class TiBasicVariableToolWindowFactory : TiBasicToolWindowFactory<TiBasicVariableToolWindowContent>() {

    override fun createContentComponent(project: Project): TiBasicVariableToolWindowContent =
        TiBasicVariableToolWindowContent(project)
}
