package com.github.mmrsic.idea.plugins.tibasic.ide.findusages

import com.github.mmrsic.idea.plugins.tibasic.common.ext.firstChildOfType
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicCallStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicFunctionCall
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicDefStatement
import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.openapi.application.ReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor

internal val STATEMENT_KEYWORD_TYPES = setOf(
    TiBasicTokenTypes.PRINT_KEYWORD,
    TiBasicTokenTypes.DISPLAY_KEYWORD,
    TiBasicTokenTypes.GOTO_KEYWORD,
    TiBasicTokenTypes.GOSUB_KEYWORD,
    TiBasicTokenTypes.RETURN_KEYWORD,
    TiBasicTokenTypes.ON_KEYWORD,
    TiBasicTokenTypes.IF_KEYWORD,
    TiBasicTokenTypes.FOR_KEYWORD,
    TiBasicTokenTypes.NEXT_KEYWORD,
    TiBasicTokenTypes.INPUT_KEYWORD,
    TiBasicTokenTypes.READ_KEYWORD,
    TiBasicTokenTypes.DATA_KEYWORD,
    TiBasicTokenTypes.RESTORE_KEYWORD,
    TiBasicTokenTypes.LET_KEYWORD,
    TiBasicTokenTypes.END_KEYWORD,
    TiBasicTokenTypes.STOP_KEYWORD,
    TiBasicTokenTypes.REM_KEYWORD,
    TiBasicTokenTypes.RANDOMIZE_KEYWORD,
    TiBasicTokenTypes.DEF_KEYWORD,
    TiBasicTokenTypes.DIM_KEYWORD,
    TiBasicTokenTypes.OPTION_BASE_KEYWORD,
    TiBasicTokenTypes.OPEN_KEYWORD,
    TiBasicTokenTypes.CLOSE_KEYWORD,
    TiBasicTokenTypes.CALL_KEYWORD,
    TiBasicTokenTypes.DELETE_KEYWORD,
)

internal val FUNCTION_KEYWORD_TYPES = setOf(
    TiBasicTokenTypes.NUMERIC_FUNCTION_KEYWORD,
    TiBasicTokenTypes.STRING_FUNCTION_KEYWORD,
)

internal fun isFunctionNameInDef(element: PsiElement): Boolean {
    val def = element.parent as? TiBasicDefStatement ?: return false
    return element.node == def.functionNameNode()
}

class TiBasicFindUsagesHandler(element: PsiElement) : FindUsagesHandler(element) {

    override fun processElementUsages(
        element: PsiElement,
        processor: Processor<in UsageInfo>,
        options: FindUsagesOptions,
    ): Boolean = ReadAction.compute<Boolean, RuntimeException> {
        val file = element.containingFile as? TiBasicFile ?: return@compute true
        findMatchingElements(element, file).all { processor.process(UsageInfo(it)) }
    }

    private fun findMatchingElements(element: PsiElement, file: TiBasicFile): List<PsiElement> {
        if (isFunctionNameInDef(element)) {
            val def = element.parent as TiBasicDefStatement
            return userDefinedFunctionUsages(def, file)
        }
        val funcCallParent = element.parent as? TiBasicFunctionCall
        if (funcCallParent != null && element.node?.elementType in FUNCTION_KEYWORD_TYPES) {
            return builtInFunctionUsages(funcCallParent.functionName() ?: return emptyList(), file)
        }
        return when {
            element is TiBasicFunctionCall ->
                builtInFunctionUsages(element.functionName() ?: return emptyList(), file)

            element is TiBasicDefStatement ->
                userDefinedFunctionUsages(element, file)

            element.node?.elementType == TiBasicTokenTypes.CALL_SUBPROGRAM_NAME -> {
                val name = element.text.uppercase()
                PsiTreeUtil.findChildrenOfType(file, TiBasicCallStatement::class.java)
                    .filter { it.subprogramName() == name }
                    .mapNotNull { it.node.firstChildOfType(TiBasicTokenTypes.CALL_SUBPROGRAM_NAME)?.psi }
            }

            element.node?.elementType in STATEMENT_KEYWORD_TYPES -> {
                val statementNodeType = element.parent?.node?.elementType ?: return emptyList()
                val keywordType = element.node!!.elementType
                file.lines().flatMap { line ->
                    line.children
                        .filter { it.node?.elementType == statementNodeType }
                        .mapNotNull { it.node.firstChildOfType(keywordType)?.psi }
                }
            }

            else -> emptyList()
        }
    }

    private fun userDefinedFunctionUsages(def: TiBasicDefStatement, file: TiBasicFile): List<PsiElement> {
        val funcName = def.functionName() ?: return emptyList()
        val callSites: List<PsiElement> = file.variableAccesses()
            .filter { it.name == funcName && it.hasSubscriptParens() }
        return listOf(def) + callSites
    }

    private fun builtInFunctionUsages(funcName: String, file: TiBasicFile): List<PsiElement> =
        PsiTreeUtil.findChildrenOfType(file, TiBasicFunctionCall::class.java)
            .filter { it.functionName() == funcName }
}
