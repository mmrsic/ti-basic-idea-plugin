package com.github.mmrsic.idea.plugins.tibasic.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class TiBasicVariableToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val content = TiBasicVariableToolWindowContent(project)
        val uiContent = ContentFactory.getInstance().createContent(content, "", false)
        Disposer.register(uiContent, content)
        toolWindow.contentManager.addContent(uiContent)
    }
}
