package com.github.mmrsic.idea.plugins.tibasic.findusages

import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicVariableAccess
import com.github.mmrsic.idea.plugins.tibasic.toolwindow.AccessType
import com.github.mmrsic.idea.plugins.tibasic.toolwindow.TiBasicVariableCollector
import com.intellij.codeInsight.highlighting.ReadWriteAccessDetector
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference

class TiBasicReadWriteAccessDetector : ReadWriteAccessDetector() {

    override fun isReadWriteAccessible(element: PsiElement): Boolean = element is TiBasicVariableAccess

    override fun isDeclarationWriteAccess(element: PsiElement): Boolean = false

    override fun getReferenceAccess(referencedElement: PsiElement, reference: PsiReference): Access =
        accessFor(reference.element as? TiBasicVariableAccess)

    override fun getExpressionAccess(expression: PsiElement): Access =
        accessFor(expression as? TiBasicVariableAccess)

    private fun accessFor(varAccess: TiBasicVariableAccess?): Access =
        when (varAccess?.let { TiBasicVariableCollector.determineAccessType(it) }) {
            AccessType.WRITE -> Access.Write
            AccessType.READ -> Access.Read
            else -> Access.Read
        }
}
