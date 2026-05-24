package com.github.mmrsic.idea.plugins.tibasic.ide.editor.documentation

import com.github.mmrsic.idea.plugins.tibasic.editor.buildDocumentation
import com.github.mmrsic.idea.plugins.tibasic.editor.resolveDocumentationUsage
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.TiBasicFile
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
        return resolveDocumentationUsage(targetElement)?.documentationElement
    }

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        val usage = resolveDocumentationUsage(originalElement ?: element) ?: return null
        return buildDocumentation(usage)
    }
}
