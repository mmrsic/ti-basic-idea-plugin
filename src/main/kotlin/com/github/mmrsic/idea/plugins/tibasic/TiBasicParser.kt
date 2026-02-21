package com.github.mmrsic.idea.plugins.tibasic

import com.github.mmrsic.idea.plugins.tibasic.TiBasicNodeTypes.COMMENT_LINE
import com.github.mmrsic.idea.plugins.tibasic.TiBasicNodeTypes.LINE
import com.github.mmrsic.idea.plugins.tibasic.TiBasicNodeTypes.PRINT_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.TiBasicTokenTypes.COMMENT
import com.github.mmrsic.idea.plugins.tibasic.TiBasicTokenTypes.LINE_NUMBER
import com.github.mmrsic.idea.plugins.tibasic.TiBasicTokenTypes.PRINT_ARGUMENT
import com.github.mmrsic.idea.plugins.tibasic.TiBasicTokenTypes.PRINT_KEYWORD
import com.intellij.lang.ASTNode
import com.intellij.lang.LightPsiParser
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

/**
 * Parser for TI-Basic source files.
 *
 * Grammar (one iteration per source line):
 * ```
 * file         ::= line*
 * line         ::= numberedLine | commentLine
 * numberedLine ::= LINE_NUMBER WHITE_SPACE? printStatement
 * printStatement ::= PRINT_KEYWORD (WHITE_SPACE PRINT_ARGUMENT?)?
 * commentLine  ::= COMMENT
 * ```
 * Newlines (WHITE_SPACE containing '\n') serve as line separators and are consumed between lines.
 */
class TiBasicParser : PsiParser, LightPsiParser {

    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        parseLight(root, builder)
        return builder.treeBuilt
    }

    override fun parseLight(root: IElementType, builder: PsiBuilder) {
        val fileMarker = builder.mark()
        while (!builder.eof()) {
            skipNewlines(builder)
            if (builder.eof()) break
            when (builder.tokenType) {
                LINE_NUMBER -> parseNumberedLine(builder)
                COMMENT -> parseCommentLine(builder)
                else -> builder.advanceLexer()
            }
        }
        fileMarker.done(root)
    }

    private fun parseNumberedLine(builder: PsiBuilder) {
        val lineMarker = builder.mark()
        builder.advanceLexer()
        skipWhitespace(builder)
        parsePrintStatement(builder)
        lineMarker.done(LINE)
    }

    private fun parsePrintStatement(builder: PsiBuilder) {
        val stmtMarker = builder.mark()
        if (builder.tokenType == PRINT_KEYWORD) {
            builder.advanceLexer()
        }
        if (builder.tokenType == TokenType.WHITE_SPACE) {
            builder.advanceLexer()
        }
        if (builder.tokenType == PRINT_ARGUMENT) {
            builder.advanceLexer()
        }
        stmtMarker.done(PRINT_STATEMENT)
    }

    private fun parseCommentLine(builder: PsiBuilder) {
        val marker = builder.mark()
        builder.advanceLexer()
        marker.done(COMMENT_LINE)
    }

    private fun skipNewlines(builder: PsiBuilder) {
        while (!builder.eof() && builder.tokenType == TokenType.WHITE_SPACE) {
            builder.advanceLexer()
        }
    }

    private fun skipWhitespace(builder: PsiBuilder) {
        if (!builder.eof() && builder.tokenType == TokenType.WHITE_SPACE) {
            builder.advanceLexer()
        }
    }
}

