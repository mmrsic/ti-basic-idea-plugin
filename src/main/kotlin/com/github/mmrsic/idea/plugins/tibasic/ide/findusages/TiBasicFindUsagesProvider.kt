package com.github.mmrsic.idea.plugins.tibasic.ide.findusages

import com.github.mmrsic.idea.plugins.tibasic.language.syntax.lexer.TiBasicLexer
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicFunctionCall
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicVariableAccess
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicDefStatement
import com.intellij.lang.cacheBuilder.WordOccurrence
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.util.Processor

class TiBasicFindUsagesProvider : FindUsagesProvider {

    override fun getWordsScanner(): WordsScanner = TiBasicWordsScanner()

    override fun canFindUsagesFor(element: PsiElement): Boolean = when {
        element is TiBasicVariableAccess -> true
        element.node?.elementType in STATEMENT_KEYWORD_TYPES -> true
        element.node?.elementType == TiBasicTokenTypes.CALL_SUBPROGRAM_NAME -> true
        element.node?.elementType in FUNCTION_KEYWORD_TYPES && element.parent is TiBasicFunctionCall -> true
        isFunctionNameInDef(element) -> true
        element is TiBasicFunctionCall -> true
        element is TiBasicDefStatement -> true
        else -> false
    }

    override fun getHelpId(element: PsiElement): String? = null

    override fun getType(element: PsiElement): String = when {
        element is TiBasicVariableAccess -> when {
            element.node.firstChildNode?.elementType == TiBasicTokenTypes.STRING_VARIABLE && element.hasSubscriptParens() -> "string array"
            element.node.firstChildNode?.elementType == TiBasicTokenTypes.STRING_VARIABLE -> "string variable"
            element.hasSubscriptParens() -> "numeric array"
            else -> "numeric variable"
        }

        element.node?.elementType in STATEMENT_KEYWORD_TYPES -> "statement"
        element.node?.elementType == TiBasicTokenTypes.CALL_SUBPROGRAM_NAME -> "subprogram"
        isFunctionNameInDef(element) -> "user-defined function"
        element is TiBasicDefStatement -> "user-defined function"
        element.node?.elementType in FUNCTION_KEYWORD_TYPES -> "function"
        element is TiBasicFunctionCall -> "function"
        else -> ""
    }

    override fun getDescriptiveName(element: PsiElement): String = when {
        element is TiBasicVariableAccess -> element.name ?: element.text
        element is TiBasicFunctionCall -> element.functionName() ?: element.text
        element is TiBasicDefStatement -> element.functionName() ?: element.text
        else -> element.text
    }

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String =
        element.parent?.text?.trim() ?: element.text
}

private val VARIABLE_TOKENS = TokenSet.create(TiBasicTokenTypes.NUMERIC_VARIABLE, TiBasicTokenTypes.STRING_VARIABLE)

private class TiBasicWordsScanner : WordsScanner {
    override fun processWords(fileText: CharSequence, processor: Processor<in WordOccurrence>) {
        val lexer = TiBasicLexer()
        lexer.start(fileText)
        val occurrence = WordOccurrence(fileText, 0, 0, WordOccurrence.Kind.CODE)
        while (lexer.tokenType != null) {
            if (lexer.tokenType in VARIABLE_TOKENS) {
                occurrence.init(fileText, lexer.tokenStart, lexer.tokenEnd, WordOccurrence.Kind.CODE)
                if (!processor.process(occurrence)) return
            }
            lexer.advance()
        }
    }
}
