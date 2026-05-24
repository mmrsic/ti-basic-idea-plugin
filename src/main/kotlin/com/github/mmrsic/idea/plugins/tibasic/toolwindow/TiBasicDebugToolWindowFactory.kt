package com.github.mmrsic.idea.plugins.tibasic.toolwindow

import com.intellij.openapi.project.Project

class TiBasicDebugToolWindowFactory : TiBasicToolWindowFactory<TiBasicDebugToolWindowContent>() {

    override fun createContentComponent(project: Project): TiBasicDebugToolWindowContent =
        TiBasicDebugToolWindowContent(project)
}
