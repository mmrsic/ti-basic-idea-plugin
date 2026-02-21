package com.github.mmrsic.idea.plugins.tibasic

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

class TiBasicSyntaxHighlighter : SyntaxHighlighterBase() {
    companion object {
        val KEYWORD = TextAttributesKey.createTextAttributesKey(
            "TI_BASIC_KEYWORD",
            DefaultLanguageHighlighterColors.KEYWORD
        )
    }

    override fun getHighlightingLexer() = TiBasicLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> = when (tokenType) {
        TiBasicTokenTypes.KEYWORD -> arrayOf(KEYWORD)
        TiBasicTokenTypes.IDENTIFIER -> arrayOf(DefaultLanguageHighlighterColors.IDENTIFIER)
        TokenType.BAD_CHARACTER -> arrayOf(DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE)
        else -> emptyArray()
    }
}
