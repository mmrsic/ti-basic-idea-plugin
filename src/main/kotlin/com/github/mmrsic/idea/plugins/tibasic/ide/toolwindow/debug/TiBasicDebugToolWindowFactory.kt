package com.github.mmrsic.idea.plugins.tibasic.ide.toolwindow.debug

import com.github.mmrsic.idea.plugins.tibasic.ide.toolwindow.TiBasicToolWindowFactory
import com.intellij.openapi.project.Project

class TiBasicDebugToolWindowFactory : TiBasicToolWindowFactory<TiBasicDebugToolWindowContent>() {

    override fun createContentComponent(project: Project): TiBasicDebugToolWindowContent =
        TiBasicDebugToolWindowContent(project)
}
