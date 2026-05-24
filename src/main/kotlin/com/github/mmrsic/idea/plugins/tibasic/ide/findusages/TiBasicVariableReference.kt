package com.github.mmrsic.idea.plugins.tibasic.ide.findusages

import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicVariableAccess
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase

class TiBasicVariableReference(varAccess: TiBasicVariableAccess) : PsiReferenceBase<TiBasicVariableAccess>(
    varAccess,
    TextRange(0, varAccess.node.firstChildNode?.textLength ?: varAccess.textLength),
) {
    override fun resolve(): PsiElement? = null

    override fun isReferenceTo(target: PsiElement): Boolean {
        if (target !is TiBasicVariableAccess) return false
        return sameVariable(element, target)
    }

    override fun getVariants(): Array<Any> = emptyArray()

    companion object {
        fun sameVariable(a: TiBasicVariableAccess, b: TiBasicVariableAccess): Boolean =
            a.name?.uppercase() == b.name?.uppercase() &&
                    a.node.firstChildNode?.elementType == b.node.firstChildNode?.elementType &&
                    a.hasSubscriptParens() == b.hasSubscriptParens()
    }
}
