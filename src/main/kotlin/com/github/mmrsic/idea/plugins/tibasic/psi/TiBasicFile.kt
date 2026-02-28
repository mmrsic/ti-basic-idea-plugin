package com.github.mmrsic.idea.plugins.tibasic.psi

import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicFileType
import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicLanguage
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import com.intellij.psi.util.PsiTreeUtil

class TiBasicFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, TiBasicLanguage) {
    override fun getFileType(): FileType = TiBasicFileType
    override fun toString(): String = "TI-Basic File"
    fun lines(): List<TiBasicLine> = children.filterIsInstance<TiBasicLine>()
    fun forStatements(): List<TiBasicForStatement> = lines().flatMap { it.children.filterIsInstance<TiBasicForStatement>() }
    fun nextStatements(): List<TiBasicNextStatement> = lines().flatMap { it.children.filterIsInstance<TiBasicNextStatement>() }
    fun variableAccesses(): List<TiBasicVariableAccess> = PsiTreeUtil.findChildrenOfType(this, TiBasicVariableAccess::class.java).toList()
}

