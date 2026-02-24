package com.github.mmrsic.idea.plugins.tibasic

import com.github.mmrsic.idea.plugins.tibasic.TiBasicNodeTypes.COMMENT_LINE
import com.github.mmrsic.idea.plugins.tibasic.TiBasicNodeTypes.EXPRESSION
import com.github.mmrsic.idea.plugins.tibasic.TiBasicNodeTypes.LINE
import com.github.mmrsic.idea.plugins.tibasic.TiBasicNodeTypes.LINE_NUMBER_LIST_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.TiBasicNodeTypes.PRINT_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.TiBasicNodeTypes.VARIABLE_ACCESS
import com.github.mmrsic.idea.plugins.tibasic.TiBasicTokenTypes.COMMA
import com.github.mmrsic.idea.plugins.tibasic.TiBasicTokenTypes.COMMENT
import com.github.mmrsic.idea.plugins.tibasic.TiBasicTokenTypes.CONCAT_OP
import com.github.mmrsic.idea.plugins.tibasic.TiBasicTokenTypes.DIV_OP
import com.github.mmrsic.idea.plugins.tibasic.TiBasicTokenTypes.EQ_OP
import com.github.mmrsic.idea.plugins.tibasic.TiBasicTokenTypes.GE_OP
import com.github.mmrsic.idea.plugins.tibasic.TiBasicTokenTypes.GT_OP
import com.github.mmrsic.idea.plugins.tibasic.TiBasicTokenTypes.LE_OP
import com.github.mmrsic.idea.plugins.tibasic.TiBasicTokenTypes.LINE_NUMBER
import com.github.mmrsic.idea.plugins.tibasic.TiBasicTokenTypes.LINE_NUMBER_LIST_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.TiBasicTokenTypes.LT_OP
import com.github.mmrsic.idea.plugins.tibasic.TiBasicTokenTypes.LPAREN
import com.github.mmrsic.idea.plugins.tibasic.TiBasicTokenTypes.MINUS_OP
import com.github.mmrsic.idea.plugins.tibasic.TiBasicTokenTypes.MUL_OP
import com.github.mmrsic.idea.plugins.tibasic.TiBasicTokenTypes.NEQ_OP
import com.github.mmrsic.idea.plugins.tibasic.TiBasicTokenTypes.NUMERIC_LITERAL
import com.github.mmrsic.idea.plugins.tibasic.TiBasicTokenTypes.NUMERIC_VARIABLE
import com.github.mmrsic.idea.plugins.tibasic.TiBasicTokenTypes.PLUS_OP
import com.github.mmrsic.idea.plugins.tibasic.TiBasicTokenTypes.POW_OP
import com.github.mmrsic.idea.plugins.tibasic.TiBasicTokenTypes.PRINT_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.TiBasicTokenTypes.RPAREN
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
 * expression        ::= numericCmp
 * stringExpr        ::= stringOperand (CONCAT_OP stringOperand)*
 * stringOperand     ::= STRING_LITERAL | variableAccess(STRING_VARIABLE)
 * numericExpr       ::= numericCmp
 * numericCmp        ::= comparable (cmpOp comparable)*   -- left-to-right; result always numeric
 * comparable        ::= stringExpr | numericAdd          -- string operands valid in comparisons
 * cmpOp             ::= '=' | '<' | '>' | '<>' | '<=' | '>='
 * numericAdd        ::= numericMul ( ('+' | '-') numericMul )*
 * numericMul        ::= numericPow ( ('*' | '/') numericPow )*
 * numericPow        ::= numericUnary ('^' numericUnary)*        -- right-associative
 * numericUnary      ::= ('+' | '-') numericUnary | numericPrimary
 * numericPrimary    ::= NUMERIC_LITERAL
 *                     | variableAccess
 *                     | STRING_LITERAL | variableAccess(STRING_VARIABLE)   -- mismatch, parsed for error reporting
 *                     | '(' numericCmp ')'
 * variableAccess    ::= (NUMERIC_VARIABLE | STRING_VARIABLE) ( '(' subscriptList ')' )?
 * subscriptList     ::= numericCmp (',' numericCmp)*           -- 1-3 validated by annotator
 * commentLine       ::= COMMENT
 * ```
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
        if (builder.tokenType == LINE_NUMBER_LIST_KEYWORD) parseLineNumberListStatement(builder)
        else parsePrintStatement(builder)
        lineMarker.done(LINE)
    }

    private fun parseLineNumberListStatement(builder: PsiBuilder) {
        val stmtMarker = builder.mark()
        builder.advanceLexer() // consume LINE_NUMBER_LIST_KEYWORD
        while (!isLineEnd(builder)) builder.advanceLexer()
        stmtMarker.done(LINE_NUMBER_LIST_STATEMENT)
    }

    private fun parsePrintStatement(builder: PsiBuilder) {
        val stmtMarker = builder.mark()
        if (builder.tokenType == PRINT_KEYWORD) builder.advanceLexer()
        skipWhitespace(builder)
        while (!isLineEnd(builder) && !isExpressionStart(builder)) builder.advanceLexer()
        if (isExpressionStart(builder)) parseExpression(builder)
        while (!isLineEnd(builder)) builder.advanceLexer()
        stmtMarker.done(PRINT_STATEMENT)
    }

    private fun parseExpression(builder: PsiBuilder) {
        val exprMarker = builder.mark()
        parseNumericCmp(builder)
        exprMarker.done(EXPRESSION)
    }

    // --- String expression ---

    private fun parseStringExpr(builder: PsiBuilder) {
        parseStringOperand(builder)
        while (true) {
            val cp = builder.mark()
            skipIntraLineWhitespace(builder)
            if (builder.tokenType != CONCAT_OP) {
                cp.rollbackTo(); break
            }
            builder.advanceLexer()
            skipIntraLineWhitespace(builder)
            if (!isStringOperand(builder)) {
                cp.rollbackTo(); break
            }
            cp.drop()
            parseStringOperand(builder)
        }
    }

    private fun parseStringOperand(builder: PsiBuilder) {
        if (builder.tokenType == STRING_VARIABLE) parseVariableAccess(builder)
        else builder.advanceLexer() // STRING_LITERAL
    }

    // --- Numeric expression (with semi-permissive mismatch handling) ---

    private fun parseNumericCmp(builder: PsiBuilder) {
        parseComparable(builder)
        while (true) {
            val cp = builder.mark()
            skipIntraLineWhitespace(builder)
            if (!isComparisonOp(builder)) {
                cp.rollbackTo(); break
            }
            builder.advanceLexer()
            skipIntraLineWhitespace(builder)
            if (!isNumericPrimaryStart(builder)) {
                cp.rollbackTo(); break
            }
            cp.drop()
            parseComparable(builder)
        }
    }

    private fun parseComparable(builder: PsiBuilder) {
        if (isStringOperand(builder)) parseStringExpr(builder) else parseNumericAdd(builder)
    }

    private fun isComparisonOp(builder: PsiBuilder): Boolean =
        builder.tokenType in setOf(EQ_OP, LT_OP, GT_OP, NEQ_OP, LE_OP, GE_OP)

    private fun parseNumericAdd(builder: PsiBuilder) {
        parseNumericMul(builder)
        while (true) {
            val cp = builder.mark()
            skipIntraLineWhitespace(builder)
            if (builder.tokenType != PLUS_OP && builder.tokenType != MINUS_OP) {
                cp.rollbackTo(); break
            }
            builder.advanceLexer()
            skipIntraLineWhitespace(builder)
            if (!isNumericPrimaryStart(builder)) {
                cp.rollbackTo(); break
            }
            cp.drop()
            parseNumericMul(builder)
        }
    }

    private fun parseNumericMul(builder: PsiBuilder) {
        parseNumericPow(builder)
        while (true) {
            val cp = builder.mark()
            skipIntraLineWhitespace(builder)
            if (builder.tokenType != MUL_OP && builder.tokenType != DIV_OP) {
                cp.rollbackTo(); break
            }
            builder.advanceLexer()
            skipIntraLineWhitespace(builder)
            if (!isNumericPrimaryStart(builder)) {
                cp.rollbackTo(); break
            }
            cp.drop()
            parseNumericPow(builder)
        }
    }

    private fun parseNumericPow(builder: PsiBuilder) {
        parseNumericUnary(builder)
        val cp = builder.mark()
        skipIntraLineWhitespace(builder)
        if (builder.tokenType != POW_OP) {
            cp.rollbackTo(); return
        }
        builder.advanceLexer()
        skipIntraLineWhitespace(builder)
        if (!isNumericPrimaryStart(builder)) {
            cp.rollbackTo(); return
        }
        cp.drop()
        parseNumericPow(builder) // right-associative
    }

    private fun parseNumericUnary(builder: PsiBuilder) {
        if (builder.tokenType == PLUS_OP || builder.tokenType == MINUS_OP) {
            builder.advanceLexer()
            skipIntraLineWhitespace(builder)
            parseNumericUnary(builder)
            return
        }
        parseNumericPrimary(builder)
    }

    private fun parseNumericPrimary(builder: PsiBuilder) {
        when (builder.tokenType) {
            NUMERIC_LITERAL -> builder.advanceLexer()
            NUMERIC_VARIABLE, STRING_VARIABLE -> parseVariableAccess(builder) // STRING_VARIABLE = mismatch
            STRING_LITERAL -> builder.advanceLexer() // mismatch but consume for error reporting
            LPAREN -> {
                builder.advanceLexer()
                skipIntraLineWhitespace(builder)
                if (isNumericPrimaryStart(builder)) parseNumericCmp(builder)
                skipIntraLineWhitespace(builder)
                if (builder.tokenType == RPAREN) builder.advanceLexer()
            }
        }
    }

    // --- Variable access (shared by string and numeric paths) ---

    private fun parseVariableAccess(builder: PsiBuilder) {
        val marker = builder.mark()
        builder.advanceLexer() // consume NUMERIC_VARIABLE or STRING_VARIABLE
        val cp = builder.mark()
        skipIntraLineWhitespace(builder)
        if (builder.tokenType == LPAREN) {
            cp.drop()
            builder.advanceLexer() // consume (
            parseSubscriptList(builder)
            skipIntraLineWhitespace(builder)
            if (builder.tokenType == RPAREN) builder.advanceLexer()
        } else {
            cp.rollbackTo()
        }
        marker.done(VARIABLE_ACCESS)
    }

    private fun parseSubscriptList(builder: PsiBuilder) {
        skipIntraLineWhitespace(builder)
        if (builder.tokenType == RPAREN || isLineEnd(builder)) return
        parseSubscriptExpr(builder)
        while (true) {
            val cp = builder.mark()
            skipIntraLineWhitespace(builder)
            if (builder.tokenType != COMMA) {
                cp.rollbackTo(); break
            }
            builder.advanceLexer()
            skipIntraLineWhitespace(builder)
            if (!isNumericPrimaryStart(builder)) {
                cp.rollbackTo(); break
            }
            cp.drop()
            parseSubscriptExpr(builder)
        }
    }

    private fun parseSubscriptExpr(builder: PsiBuilder) {
        val marker = builder.mark()
        parseNumericCmp(builder)
        marker.done(EXPRESSION)
    }

    // --- Helpers ---

    private fun isExpressionStart(builder: PsiBuilder): Boolean =
        isStringOperand(builder) || isNumericPrimaryStart(builder)

    private fun isStringOperand(builder: PsiBuilder): Boolean =
        builder.tokenType == STRING_LITERAL || builder.tokenType == STRING_VARIABLE

    private fun isNumericPrimaryStart(builder: PsiBuilder): Boolean =
        builder.tokenType in setOf(NUMERIC_LITERAL, NUMERIC_VARIABLE, PLUS_OP, MINUS_OP, LPAREN, STRING_LITERAL, STRING_VARIABLE)

    private fun parseCommentLine(builder: PsiBuilder) {
        val marker = builder.mark()
        builder.advanceLexer()
        marker.done(COMMENT_LINE)
    }

    private fun skipNewlines(builder: PsiBuilder) {
        while (!builder.eof() && builder.tokenType == TokenType.WHITE_SPACE) builder.advanceLexer()
    }

    private fun skipWhitespace(builder: PsiBuilder) {
        if (!builder.eof() && builder.tokenType == TokenType.WHITE_SPACE) builder.advanceLexer()
    }

    private fun skipIntraLineWhitespace(builder: PsiBuilder) {
        if (!isLineEnd(builder) && builder.tokenType == TokenType.WHITE_SPACE) builder.advanceLexer()
    }

    private fun isLineEnd(builder: PsiBuilder): Boolean =
        builder.eof() || builder.tokenType == LINE_NUMBER || builder.tokenType == COMMENT
}



