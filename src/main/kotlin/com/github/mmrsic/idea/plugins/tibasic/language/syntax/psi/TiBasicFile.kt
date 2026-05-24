package com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi

import com.github.mmrsic.idea.plugins.tibasic.ide.language.TiBasicFileType
import com.github.mmrsic.idea.plugins.tibasic.ide.language.TiBasicLanguage
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicCallStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicVariableAccess
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicDefStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicDimStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicForStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicLine
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicNextStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicOptionBaseStatement
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import com.intellij.psi.util.PsiTreeUtil

class TiBasicFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, TiBasicLanguage) {
    override fun getFileType(): FileType = TiBasicFileType
    override fun toString(): String = "TI-Basic File"
    fun lines(): List<TiBasicLine> = children.filterIsInstance<TiBasicLine>()
    fun lineByNumber(lineNumber: Int): TiBasicLine? = lines().firstOrNull { it.lineNumber() == lineNumber }
    fun forStatements(): List<TiBasicForStatement> = lines().flatMap { it.children.filterIsInstance<TiBasicForStatement>() }
    fun nextStatements(): List<TiBasicNextStatement> = lines().flatMap { it.children.filterIsInstance<TiBasicNextStatement>() }
    fun variableAccesses(): List<TiBasicVariableAccess> = PsiTreeUtil.findChildrenOfType(this, TiBasicVariableAccess::class.java).toList()
    fun callStatements(): List<TiBasicCallStatement> = PsiTreeUtil.findChildrenOfType(this, TiBasicCallStatement::class.java).toList()
    fun defStatements(): List<TiBasicDefStatement> = lines().flatMap { it.children.filterIsInstance<TiBasicDefStatement>() }
    fun dimStatements(): List<TiBasicDimStatement> = lines().flatMap { it.children.filterIsInstance<TiBasicDimStatement>() }
    fun optionBaseStatements(): List<TiBasicOptionBaseStatement> = lines().flatMap { it.children.filterIsInstance<TiBasicOptionBaseStatement>() }
}
