package com.github.mmrsic.idea.plugins.tibasic.psi.contracts

import com.github.mmrsic.idea.plugins.tibasic.psi.expression.TiBasicExpression
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

interface TiBasicFileNumberStatement : PsiElement {
    fun fileNumberExpr(): TiBasicExpression?
}

interface TiBasicRecordNumberStatement : PsiElement {
    fun recKeywordNode(): ASTNode?
    fun recordNumberExpr(): TiBasicExpression?
}

