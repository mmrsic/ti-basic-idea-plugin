package com.github.mmrsic.idea.plugins.tibasic.ide.toolwindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.JComponent

abstract class TiBasicToolWindowFactory<CONTENT> : ToolWindowFactory
    where CONTENT : JComponent, CONTENT : Disposable {

    final override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val content = createContentComponent(project)
        val uiContent = ContentFactory.getInstance().createContent(content, "", false)
        Disposer.register(uiContent, content)
        toolWindow.contentManager.addContent(uiContent)
    }

    protected abstract fun createContentComponent(project: Project): CONTENT
}
