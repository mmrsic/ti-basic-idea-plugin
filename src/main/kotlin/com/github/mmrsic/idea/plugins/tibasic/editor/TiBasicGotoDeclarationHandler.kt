package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.ext.resolveReferencedLine
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement

class TiBasicGotoDeclarationHandler : GotoDeclarationHandler {

    override fun getGotoDeclarationTargets(
        element: PsiElement?,
        offset: Int,
        editor: Editor,
    ): Array<PsiElement>? =
        element?.resolveReferencedLine()?.let { targetLine ->
            arrayOf(targetLine.node.firstChildNode?.psi ?: targetLine)
        }

    override fun getActionText(context: DataContext): String? = null
}
