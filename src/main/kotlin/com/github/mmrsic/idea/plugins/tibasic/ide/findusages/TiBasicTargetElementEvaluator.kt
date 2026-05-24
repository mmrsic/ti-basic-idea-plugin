package com.github.mmrsic.idea.plugins.tibasic.ide.findusages

import com.github.mmrsic.idea.plugins.tibasic.language.syntax.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicFunctionCall
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
