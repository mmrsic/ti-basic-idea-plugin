package com.github.mmrsic.idea.plugins.tibasic.lexer

import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicLanguage
import com.intellij.psi.tree.IElementType

object TiBasicTokenTypes {
    val KEYWORD = TiBasicElementType("KEYWORD")
    val IDENTIFIER = TiBasicElementType("IDENTIFIER")
    val LINE_NUMBER = TiBasicElementType("LINE_NUMBER")
    val PRINT_KEYWORD = TiBasicElementType("PRINT_KEYWORD")
    val LINE_NUMBER_LIST_KEYWORD = TiBasicElementType("LINE_NUMBER_LIST_KEYWORD")
    val DELETE_KEYWORD = TiBasicElementType("DELETE_KEYWORD")
    val REM_KEYWORD = TiBasicElementType("REM_KEYWORD")
    val LET_KEYWORD = TiBasicElementType("LET_KEYWORD")
    val END_KEYWORD = TiBasicElementType("END_KEYWORD")
    val STOP_KEYWORD = TiBasicElementType("STOP_KEYWORD")
    val REM_TEXT = TiBasicElementType("REM_TEXT")
    val UNKNOWN_STATEMENT_TEXT = TiBasicElementType("UNKNOWN_STATEMENT_TEXT")
    val NO_LINE_NUMBER_TEXT = TiBasicElementType("NO_LINE_NUMBER_TEXT")
    val PRINT_ARGUMENT = TiBasicElementType("PRINT_ARGUMENT")
    val STRING_LITERAL = TiBasicElementType("STRING_LITERAL")
    val STRING_VARIABLE = TiBasicElementType("STRING_VARIABLE")
    val NUMERIC_LITERAL = TiBasicElementType("NUMERIC_LITERAL")
    val NUMERIC_VARIABLE = TiBasicElementType("NUMERIC_VARIABLE")
    val INVALID_VARIABLE_NAME = TiBasicElementType("INVALID_VARIABLE_NAME")
    val CONCAT_OP = TiBasicElementType("CONCAT_OP")
    val PLUS_OP = TiBasicElementType("PLUS_OP")
    val MINUS_OP = TiBasicElementType("MINUS_OP")
    val MUL_OP = TiBasicElementType("MUL_OP")
    val DIV_OP = TiBasicElementType("DIV_OP")
    val POW_OP = TiBasicElementType("POW_OP")
    val EQ_OP = TiBasicElementType("EQ_OP")
    val LT_OP = TiBasicElementType("LT_OP")
    val GT_OP = TiBasicElementType("GT_OP")
    val NEQ_OP = TiBasicElementType("NEQ_OP")
    val LE_OP = TiBasicElementType("LE_OP")
    val GE_OP = TiBasicElementType("GE_OP")
    val LPAREN = TiBasicElementType("LPAREN")
    val RPAREN = TiBasicElementType("RPAREN")
    val COMMA = TiBasicElementType("COMMA")
    val COMMENT = TiBasicElementType("COMMENT")
}

class TiBasicElementType(debugName: String) : IElementType(debugName, TiBasicLanguage)
