package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.psi.expression.TiBasicCallStatement
import com.intellij.psi.PsiElement

internal fun callStatementForSubprogram(element: PsiElement, subprogramName: String): TiBasicCallStatement? {
    if (element.node.elementType != TiBasicTokenTypes.CALL_SUBPROGRAM_NAME) return null
    return (element.parent as? TiBasicCallStatement)
        ?.takeIf { it.subprogramName() == subprogramName }
}
