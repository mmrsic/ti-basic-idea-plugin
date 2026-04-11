package com.github.mmrsic.idea.plugins.tibasic.editor

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class TiBasicDisplayColumnGuideInitializer : ProjectActivity {

    override suspend fun execute(project: Project) {
        val editorFactory = EditorFactory.getInstance()
        val listenerLifetime = TiBasicDisplayColumnGuideLifetime.getInstance(project)
        editorFactory.addEditorFactoryListener(
            object : EditorFactoryListener {
                override fun editorCreated(event: EditorFactoryEvent) {
                    if (event.editor.project != project) {
                        return
                    }
                    TiBasicDisplayColumnGuideController.install(event.editor)
                }

                override fun editorReleased(event: EditorFactoryEvent) {
                    if (event.editor.project != project) {
                        return
                    }
                    TiBasicDisplayColumnGuideController.uninstall(event.editor)
                }
            },
            listenerLifetime,
        )
        editorFactory.allEditors
            .filter { editor -> editor.project == project }
            .forEach(TiBasicDisplayColumnGuideController::install)
    }
}

