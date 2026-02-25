package com.github.mmrsic.idea.plugins.tibasic.lang

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object TiBasicFileType : LanguageFileType(TiBasicLanguage) {
    override fun getName(): String = "TI-Basic File"
    override fun getDescription(): String = "TI-Basic source file"
    override fun getDefaultExtension(): String = fileTypeExtensions.first()
    override fun getIcon(): Icon = IconLoader.getIcon("/icons/ti99_4a_icon_small.svg", TiBasicFileType::class.java)
}

val fileTypeExtensions = listOf("ti-basic", "tibasic", "ti.bas")
