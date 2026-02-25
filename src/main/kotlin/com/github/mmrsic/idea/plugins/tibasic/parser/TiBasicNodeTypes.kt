package com.github.mmrsic.idea.plugins.tibasic.parser

import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicLanguage
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicElementType
import com.intellij.psi.tree.IFileElementType

object TiBasicNodeTypes {
    val FILE = IFileElementType(TiBasicLanguage)
    val LINE = TiBasicElementType("LINE")
    val PRINT_STATEMENT = TiBasicElementType("PRINT_STATEMENT")
    val LINE_NUMBER_LIST_STATEMENT = TiBasicElementType("LINE_NUMBER_LIST_STATEMENT")
    val DELETE_STATEMENT = TiBasicElementType("DELETE_STATEMENT")
    val REM_STATEMENT = TiBasicElementType("REM_STATEMENT")
    val LET_STATEMENT = TiBasicElementType("LET_STATEMENT")
    val END_STATEMENT = TiBasicElementType("END_STATEMENT")
    val STOP_STATEMENT = TiBasicElementType("STOP_STATEMENT")
    val GOTO_STATEMENT = TiBasicElementType("GOTO_STATEMENT")
    val UNKNOWN_STATEMENT = TiBasicElementType("UNKNOWN_STATEMENT")
    val INVALID_LINE = TiBasicElementType("INVALID_LINE")
    val EXPRESSION = TiBasicElementType("EXPRESSION")
    val VARIABLE_ACCESS = TiBasicElementType("VARIABLE_ACCESS")
    val COMMENT_LINE = TiBasicElementType("COMMENT_LINE")
}
