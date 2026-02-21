package com.github.mmrsic.idea.plugins.tibasic

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

class TiBasicSyntaxHighlighter : SyntaxHighlighterBase() {
    companion object {
        val KEYWORD =
            TextAttributesKey.createTextAttributesKey("TI_BASIC_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
        val LINE_NUMBER =
            TextAttributesKey.createTextAttributesKey("TI_BASIC_LINE_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
        val PRINT_ARGUMENT =
            TextAttributesKey.createTextAttributesKey("TI_BASIC_PRINT_ARGUMENT", DefaultLanguageHighlighterColors.STRING)
        val COMMENT =
            TextAttributesKey.createTextAttributesKey("TI_BASIC_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
    }

    override fun getHighlightingLexer() = TiBasicLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> =
        when (tokenType) {
            TiBasicTokenTypes.KEYWORD -> arrayOf(KEYWORD)
            TiBasicTokenTypes.PRINT_KEYWORD -> arrayOf(KEYWORD)
            TiBasicTokenTypes.IDENTIFIER -> arrayOf(DefaultLanguageHighlighterColors.IDENTIFIER)
            TiBasicTokenTypes.LINE_NUMBER -> arrayOf(LINE_NUMBER)
            TiBasicTokenTypes.PRINT_ARGUMENT -> arrayOf(PRINT_ARGUMENT)
            TiBasicTokenTypes.COMMENT -> arrayOf(COMMENT)
            TokenType.BAD_CHARACTER -> arrayOf(DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE)
            else -> emptyArray()
        }
}
