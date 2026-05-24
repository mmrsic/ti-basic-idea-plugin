package com.github.mmrsic.idea.plugins.tibasic.ide.toolwindow.variables

import com.github.mmrsic.idea.plugins.tibasic.ide.toolwindow.TiBasicToolWindowFactory
import com.intellij.openapi.project.Project

class TiBasicVariableToolWindowFactory : TiBasicToolWindowFactory<TiBasicVariableToolWindowContent>() {

    override fun createContentComponent(project: Project): TiBasicVariableToolWindowContent =
        TiBasicVariableToolWindowContent(project)
}
