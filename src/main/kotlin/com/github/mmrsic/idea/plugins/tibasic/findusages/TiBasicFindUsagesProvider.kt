package com.github.mmrsic.idea.plugins.tibasic.findusages

import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicLexer
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicVariableAccess
import com.intellij.lang.cacheBuilder.WordOccurrence
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.util.Processor

class TiBasicFindUsagesProvider : FindUsagesProvider {

    override fun getWordsScanner(): WordsScanner = TiBasicWordsScanner()

    override fun canFindUsagesFor(element: PsiElement): Boolean = element is TiBasicVariableAccess

    override fun getHelpId(element: PsiElement): String? = null

    override fun getType(element: PsiElement): String = when {
        element !is TiBasicVariableAccess -> ""
        element.node.firstChildNode?.elementType == TiBasicTokenTypes.STRING_VARIABLE && element.hasSubscriptParens() -> "string array"
        element.node.firstChildNode?.elementType == TiBasicTokenTypes.STRING_VARIABLE -> "string variable"
        element.hasSubscriptParens() -> "numeric array"
        else -> "numeric variable"
    }

    override fun getDescriptiveName(element: PsiElement): String =
        (element as? TiBasicVariableAccess)?.name ?: element.text

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
