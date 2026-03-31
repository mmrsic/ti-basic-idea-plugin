package com.github.mmrsic.idea.plugins.tibasic.findusages

import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFunctionCall
import com.intellij.codeInsight.TargetElementEvaluatorEx2
import com.intellij.psi.PsiElement

class TiBasicTargetElementEvaluator : TargetElementEvaluatorEx2() {

    override fun getNamedElement(element: PsiElement): PsiElement? = when {
        element.node?.elementType in STATEMENT_KEYWORD_TYPES -> element
        element.node?.elementType == TiBasicTokenTypes.CALL_SUBPROGRAM_NAME -> element
        element.node?.elementType in FUNCTION_KEYWORD_TYPES && element.parent is TiBasicFunctionCall -> element
        isFunctionNameInDef(element) -> element
        else -> null
    }
}
