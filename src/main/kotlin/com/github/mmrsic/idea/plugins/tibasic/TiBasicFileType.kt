package com.github.mmrsic.idea.plugins.tibasic

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object TiBasicFileType : LanguageFileType(TiBasicLanguage) {
    override fun getName(): String = "TI-Basic File"
    override fun getDescription(): String = "TI-Basic source file"
    override fun getDefaultExtension(): String = fileTypeExtensions.first()
    override fun getIcon(): Icon? = null
}

val fileTypeExtensions = listOf("ti-basic", "tibasic", "ti.bas")
