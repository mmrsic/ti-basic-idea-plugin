package com.github.mmrsic.idea.plugins.tibasic

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType

object TiBasicTokenTypes {
    val KEYWORD = TiBasicElementType("KEYWORD")
    val IDENTIFIER = TiBasicElementType("IDENTIFIER")
    val LINE_NUMBER = TiBasicElementType("LINE_NUMBER")
    val PRINT_KEYWORD = TiBasicElementType("PRINT_KEYWORD")
    val PRINT_ARGUMENT = TiBasicElementType("PRINT_ARGUMENT")
    val STRING_LITERAL = TiBasicElementType("STRING_LITERAL")
    val STRING_VARIABLE = TiBasicElementType("STRING_VARIABLE")
    val INVALID_VARIABLE_NAME = TiBasicElementType("INVALID_VARIABLE_NAME")
    val INVALID_SUBSCRIPT = TiBasicElementType("INVALID_SUBSCRIPT")
    val CONCAT_OP = TiBasicElementType("CONCAT_OP")
    val COMMENT = TiBasicElementType("COMMENT")
}

object TiBasicNodeTypes {
    val FILE = IFileElementType(TiBasicLanguage)
    val LINE = TiBasicElementType("LINE")
    val PRINT_STATEMENT = TiBasicElementType("PRINT_STATEMENT")
    val EXPRESSION = TiBasicElementType("EXPRESSION")
    val COMMENT_LINE = TiBasicElementType("COMMENT_LINE")
}

class TiBasicElementType(debugName: String) : IElementType(debugName, TiBasicLanguage)
