package com.github.mmrsic.idea.plugins.tibasic

import com.github.mmrsic.idea.plugins.tibasic.TiBasicNodeTypes.COMMENT_LINE
import com.github.mmrsic.idea.plugins.tibasic.TiBasicNodeTypes.EXPRESSION
import com.github.mmrsic.idea.plugins.tibasic.TiBasicNodeTypes.LINE
import com.github.mmrsic.idea.plugins.tibasic.TiBasicNodeTypes.PRINT_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.TiBasicTokenTypes.COMMENT
import com.github.mmrsic.idea.plugins.tibasic.TiBasicTokenTypes.CONCAT_OP
import com.github.mmrsic.idea.plugins.tibasic.TiBasicTokenTypes.LINE_NUMBER
import com.github.mmrsic.idea.plugins.tibasic.TiBasicTokenTypes.NUMERIC_LITERAL
import com.github.mmrsic.idea.plugins.tibasic.TiBasicTokenTypes.NUMERIC_VARIABLE
import com.github.mmrsic.idea.plugins.tibasic.TiBasicTokenTypes.PRINT_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.TiBasicTokenTypes.STRING_LITERAL
import com.github.mmrsic.idea.plugins.tibasic.TiBasicTokenTypes.STRING_VARIABLE
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
 * file              ::= line*
 * line              ::= numberedLine | commentLine
 * numberedLine      ::= LINE_NUMBER WHITE_SPACE? printStatement
 * printStatement    ::= PRINT_KEYWORD (WHITE_SPACE expression?)?
 * expression        ::= stringExpression | numericExpression
 * stringExpression  ::= stringOperand (CONCAT_OP stringOperand)*
 * stringOperand     ::= STRING_LITERAL | STRING_VARIABLE
 * numericExpression ::= NUMERIC_LITERAL | NUMERIC_VARIABLE
 * commentLine       ::= COMMENT
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
        // Consume any invalid tokens that precede the first string operand.
        while (!isLineEnd(builder) && !isExpressionStart(builder)) {
            builder.advanceLexer()
        }
        if (isExpressionStart(builder)) {
            parseExpression(builder)
        }
        // Consume any remaining intra-line tokens (invalid content left after expression parsing).
        while (!isLineEnd(builder)) {
            builder.advanceLexer()
        }
        stmtMarker.done(PRINT_STATEMENT)
    }

    private fun parseExpression(builder: PsiBuilder) {
        val exprMarker = builder.mark()
        if (builder.tokenType == NUMERIC_LITERAL || builder.tokenType == NUMERIC_VARIABLE) {
            builder.advanceLexer()
            exprMarker.done(EXPRESSION)
            return
        }
        builder.advanceLexer() // consume first string operand
        while (true) {
            val checkpoint = builder.mark()
            skipIntraLineWhitespace(builder)
            if (builder.tokenType != CONCAT_OP) {
                checkpoint.rollbackTo()
                break
            }
            builder.advanceLexer() // consume CONCAT_OP
            skipIntraLineWhitespace(builder)
            if (!isStringOperand(builder)) {
                checkpoint.rollbackTo()
                break
            }
            checkpoint.drop()
            builder.advanceLexer() // consume string operand
        }
        exprMarker.done(EXPRESSION)
    }

    private fun isExpressionStart(builder: PsiBuilder) =
        isStringOperand(builder) || builder.tokenType == NUMERIC_LITERAL || builder.tokenType == NUMERIC_VARIABLE

    private fun isStringOperand(builder: PsiBuilder) =
        builder.tokenType == STRING_LITERAL || builder.tokenType == STRING_VARIABLE

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

    private fun skipIntraLineWhitespace(builder: PsiBuilder) {
        if (!isLineEnd(builder) && builder.tokenType == TokenType.WHITE_SPACE) {
            builder.advanceLexer()
        }
    }

    private fun isLineEnd(builder: PsiBuilder): Boolean {
        if (builder.eof()) return true
        return builder.tokenType == LINE_NUMBER || builder.tokenType == COMMENT
    }
}

