package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class TiBasicCharacterCodeDocumentationProvider : AbstractDocumentationProvider() {

    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        targetOffset: Int,
    ): PsiElement? {
        if (file !is TiBasicFile) return null
        val targetElement = contextElement ?: file.findElementAt(targetOffset) ?: return null
        return resolveCharacterCodeUsage(targetElement)?.expression
    }

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        val usage = resolveCharacterCodeUsage(originalElement ?: element) ?: return null
        return buildCharacterCodeDocumentation(usage)
    }
}
