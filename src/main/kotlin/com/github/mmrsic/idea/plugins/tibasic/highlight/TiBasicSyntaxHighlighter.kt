package com.github.mmrsic.idea.plugins.tibasic.highlight

import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicLexer
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
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
        val STRING_LITERAL =
            TextAttributesKey.createTextAttributesKey("TI_BASIC_STRING_LITERAL", DefaultLanguageHighlighterColors.STRING)
        val NUMERIC_LITERAL =
            TextAttributesKey.createTextAttributesKey("TI_BASIC_NUMERIC_LITERAL", DefaultLanguageHighlighterColors.NUMBER)
        val NUMERIC_VARIABLE =
            TextAttributesKey.createTextAttributesKey("TI_BASIC_NUMERIC_VARIABLE", DefaultLanguageHighlighterColors.LOCAL_VARIABLE)
        val STRING_VARIABLE =
            TextAttributesKey.createTextAttributesKey("TI_BASIC_STRING_VARIABLE", DefaultLanguageHighlighterColors.LOCAL_VARIABLE)
        val CONCAT_OP =
            TextAttributesKey.createTextAttributesKey("TI_BASIC_CONCAT_OP", DefaultLanguageHighlighterColors.OPERATION_SIGN)
        val ARITH_OP =
            TextAttributesKey.createTextAttributesKey("TI_BASIC_ARITH_OP", DefaultLanguageHighlighterColors.OPERATION_SIGN)
        val PAREN =
            TextAttributesKey.createTextAttributesKey("TI_BASIC_PAREN", DefaultLanguageHighlighterColors.PARENTHESES)
        val PRINT_ARGUMENT =
            TextAttributesKey.createTextAttributesKey("TI_BASIC_PRINT_ARGUMENT", DefaultLanguageHighlighterColors.STRING)
        val CALL_SUBPROGRAM =
            TextAttributesKey.createTextAttributesKey("TI_BASIC_CALL_SUBPROGRAM", DefaultLanguageHighlighterColors.FUNCTION_CALL)
        val COMMENT =
            TextAttributesKey.createTextAttributesKey("TI_BASIC_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
    }

    override fun getHighlightingLexer() = TiBasicLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> =
        when (tokenType) {
            TiBasicTokenTypes.PRINT_KEYWORD,
            TiBasicTokenTypes.LINE_NUMBER_LIST_KEYWORD,
            TiBasicTokenTypes.DELETE_KEYWORD,
            TiBasicTokenTypes.LET_KEYWORD,
            TiBasicTokenTypes.REM_KEYWORD,
            TiBasicTokenTypes.END_KEYWORD,
            TiBasicTokenTypes.STOP_KEYWORD,
            TiBasicTokenTypes.GOTO_KEYWORD,
            TiBasicTokenTypes.ON_KEYWORD,
            TiBasicTokenTypes.IF_KEYWORD,
            TiBasicTokenTypes.THEN_KEYWORD,
            TiBasicTokenTypes.ELSE_KEYWORD,
            TiBasicTokenTypes.FOR_KEYWORD,
            TiBasicTokenTypes.NEXT_KEYWORD,
            TiBasicTokenTypes.INPUT_KEYWORD,
            TiBasicTokenTypes.READ_KEYWORD,
            TiBasicTokenTypes.DATA_KEYWORD,
            TiBasicTokenTypes.RESTORE_KEYWORD,
            TiBasicTokenTypes.TAB_KEYWORD,
            TiBasicTokenTypes.DISPLAY_KEYWORD,
            TiBasicTokenTypes.CALL_KEYWORD,
            TiBasicTokenTypes.TO_KEYWORD,
            TiBasicTokenTypes.STEP_KEYWORD,
            TiBasicTokenTypes.NUMERIC_FUNCTION_KEYWORD,
            TiBasicTokenTypes.STRING_FUNCTION_KEYWORD,
            TiBasicTokenTypes.RANDOMIZE_KEYWORD,
            TiBasicTokenTypes.DEF_KEYWORD,
            TiBasicTokenTypes.DIM_KEYWORD,
            TiBasicTokenTypes.OPTION_BASE_KEYWORD -> arrayOf(KEYWORD)

            TiBasicTokenTypes.REM_TEXT -> arrayOf(COMMENT)
            TiBasicTokenTypes.LINE_NUMBER -> arrayOf(LINE_NUMBER)
            TiBasicTokenTypes.STRING_LITERAL -> arrayOf(STRING_LITERAL)
            TiBasicTokenTypes.NUMERIC_LITERAL -> arrayOf(NUMERIC_LITERAL)
            TiBasicTokenTypes.NUMERIC_VARIABLE -> arrayOf(NUMERIC_VARIABLE)
            TiBasicTokenTypes.STRING_VARIABLE -> arrayOf(STRING_VARIABLE)
            TiBasicTokenTypes.CONCAT_OP -> arrayOf(CONCAT_OP)
            TiBasicTokenTypes.PLUS_OP,
            TiBasicTokenTypes.MINUS_OP,
            TiBasicTokenTypes.MUL_OP,
            TiBasicTokenTypes.DIV_OP,
            TiBasicTokenTypes.POW_OP,
            TiBasicTokenTypes.EQ_OP,
            TiBasicTokenTypes.LT_OP,
            TiBasicTokenTypes.GT_OP,
            TiBasicTokenTypes.NEQ_OP,
            TiBasicTokenTypes.LE_OP,
            TiBasicTokenTypes.GE_OP,
            TiBasicTokenTypes.SEMICOLON,
            TiBasicTokenTypes.COLON,
            TiBasicTokenTypes.COMMA -> arrayOf(ARITH_OP)

            TiBasicTokenTypes.LPAREN,
            TiBasicTokenTypes.RPAREN -> arrayOf(PAREN)

            TiBasicTokenTypes.PRINT_ARGUMENT -> arrayOf(PRINT_ARGUMENT)
            TiBasicTokenTypes.CALL_SUBPROGRAM_NAME -> arrayOf(CALL_SUBPROGRAM)
            TokenType.BAD_CHARACTER -> arrayOf(DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE)
            else -> emptyArray()
        }
}
