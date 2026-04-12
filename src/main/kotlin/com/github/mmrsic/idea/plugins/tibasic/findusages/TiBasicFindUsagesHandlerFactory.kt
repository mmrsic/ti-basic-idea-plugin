package com.github.mmrsic.idea.plugins.tibasic.findusages

import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.psi.expression.TiBasicFunctionCall
import com.github.mmrsic.idea.plugins.tibasic.psi.statement.TiBasicDefStatement
import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.psi.PsiElement

class TiBasicFindUsagesHandlerFactory : FindUsagesHandlerFactory() {

    override fun canFindUsages(element: PsiElement): Boolean = when {
        element.node?.elementType in STATEMENT_KEYWORD_TYPES -> true
        element.node?.elementType == TiBasicTokenTypes.CALL_SUBPROGRAM_NAME -> true
        element.node?.elementType in FUNCTION_KEYWORD_TYPES && element.parent is TiBasicFunctionCall -> true
        isFunctionNameInDef(element) -> true
        element is TiBasicFunctionCall -> true
        element is TiBasicDefStatement -> true
        else -> false
    }

    override fun createFindUsagesHandler(element: PsiElement, forHighlightUsages: Boolean): FindUsagesHandler =
        TiBasicFindUsagesHandler(element)
}
