package com.github.mmrsic.idea.plugins.tibasic

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType

object TiBasicTokenTypes {
    val KEYWORD = TiBasicElementType("KEYWORD")
    val IDENTIFIER = TiBasicElementType("IDENTIFIER")
    val LINE_NUMBER = TiBasicElementType("LINE_NUMBER")
    val PRINT_KEYWORD = TiBasicElementType("PRINT_KEYWORD")
    val LINE_NUMBER_LIST_KEYWORD = TiBasicElementType("LINE_NUMBER_LIST_KEYWORD")
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

object TiBasicNodeTypes {
    val FILE = IFileElementType(TiBasicLanguage)
    val LINE = TiBasicElementType("LINE")
    val PRINT_STATEMENT = TiBasicElementType("PRINT_STATEMENT")
    val LINE_NUMBER_LIST_STATEMENT = TiBasicElementType("LINE_NUMBER_LIST_STATEMENT")
    val EXPRESSION = TiBasicElementType("EXPRESSION")
    val VARIABLE_ACCESS = TiBasicElementType("VARIABLE_ACCESS")
    val COMMENT_LINE = TiBasicElementType("COMMENT_LINE")
}

class TiBasicElementType(debugName: String) : IElementType(debugName, TiBasicLanguage)
