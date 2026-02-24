package com.github.mmrsic.idea.plugins.tibasic.lang

import com.intellij.ide.FileIconProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

class TiBasicFileIconProvider : FileIconProvider {

    override fun getIcon(file: VirtualFile, flags: Int, project: Project?): Icon? =
        if (file.extension in fileTypeExtensions) {
            IconLoader.getIcon("/icons/ti99_4a_icon_small.svg", javaClass)
        } else {
            null
        }
}
