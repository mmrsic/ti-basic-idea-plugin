package com.github.mmrsic.idea.plugins.tibasic.editor

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class TiBasicDisplayColumnGuideLifetime : Disposable {

    override fun dispose() = Unit

    companion object {
        fun getInstance(project: Project): TiBasicDisplayColumnGuideLifetime =
            project.getService(TiBasicDisplayColumnGuideLifetime::class.java)
    }
}

