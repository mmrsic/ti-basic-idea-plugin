package com.github.mmrsic.idea.plugins.tibasic.parser

import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.COLON
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.COMMA
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.CONCAT_OP
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.DELETE_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.DIV_OP
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.ELSE_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.END_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.EQ_OP
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.FOR_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.GE_OP
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.GOTO_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.GT_OP
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.IF_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.INPUT_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.INVALID_VARIABLE_NAME
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.LET_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.LE_OP
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.LINE_NUMBER
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.LINE_NUMBER_LIST_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.LPAREN
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.LT_OP
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.MINUS_OP
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.MUL_OP
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.NEQ_OP
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.NEXT_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.NO_LINE_NUMBER_TEXT
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.NUMERIC_LITERAL
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.NUMERIC_VARIABLE
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.ON_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.PLUS_OP
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.POW_OP
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.PRINT_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.REM_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.RPAREN
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.STEP_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.STOP_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.STRING_LITERAL
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.STRING_VARIABLE
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.THEN_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.TO_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.UNKNOWN_STATEMENT_TEXT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.DELETE_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.END_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.EXPRESSION
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.FOR_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.GOTO_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.IF_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.INPUT_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.INVALID_LINE
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.LET_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.LINE
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.LINE_NUMBER_LIST_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.NEXT_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.ON_GOTO_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.PRINT_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.REM_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.STOP_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.UNKNOWN_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.VARIABLE_ACCESS
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
 * numberedLine      ::= LINE_NUMBER WHITE_SPACE? (printStatement | endStatement | stopStatement | gotoStatement | onGotoStatement | ifStatement | ...)?
 * gotoStatement     ::= GOTO_KEYWORD WHITE_SPACE? NUMERIC_LITERAL
 * onGotoStatement   ::= ON_KEYWORD WHITE_SPACE? expression WHITE_SPACE? GOTO_KEYWORD (WHITE_SPACE? NUMERIC_LITERAL (COMMA NUMERIC_LITERAL)*)?
 * ifStatement       ::= IF_KEYWORD WHITE_SPACE? expression WHITE_SPACE? THEN_KEYWORD WHITE_SPACE? NUMERIC_LITERAL (WHITE_SPACE? ELSE_KEYWORD WHITE_SPACE? NUMERIC_LITERAL)?
 * printStatement    ::= PRINT_KEYWORD (WHITE_SPACE expression?)?
 * endStatement      ::= END_KEYWORD
 * stopStatement     ::= STOP_KEYWORD
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
                NO_LINE_NUMBER_TEXT -> parseInvalidLine(builder)
                else -> builder.advanceLexer()
            }
        }
        fileMarker.done(root)
    }

    private fun parseNumberedLine(builder: PsiBuilder) {
        val lineMarker = builder.mark()
        builder.advanceLexer()
        skipWhitespace(builder)
        when (builder.tokenType) {
            LINE_NUMBER_LIST_KEYWORD -> parseLineNumberListStatement(builder)
            DELETE_KEYWORD -> parseDeleteStatement(builder)
            REM_KEYWORD -> parseRemStatement(builder)
            END_KEYWORD -> parseEndStatement(builder)
            STOP_KEYWORD -> parseStopStatement(builder)
            GOTO_KEYWORD -> parseGotoStatement(builder)
            ON_KEYWORD -> parseOnGotoStatement(builder)
            IF_KEYWORD -> parseIfStatement(builder)
            FOR_KEYWORD -> parseForStatement(builder)
            NEXT_KEYWORD -> parseNextStatement(builder)
            INPUT_KEYWORD -> parseInputStatement(builder)
            PRINT_KEYWORD -> parsePrintStatement(builder)
            LET_KEYWORD, NUMERIC_VARIABLE, STRING_VARIABLE, INVALID_VARIABLE_NAME -> parseLetStatement(builder)
            UNKNOWN_STATEMENT_TEXT -> parseUnknownStatement(builder)
            else -> { /* line number only — valid, no statement */
            }
        }
        lineMarker.done(LINE)
    }

    private fun parseLetStatement(builder: PsiBuilder) {
        val stmtMarker = builder.mark()
        if (builder.tokenType == LET_KEYWORD) {
            builder.advanceLexer() // consume optional LET
            skipWhitespace(builder)
        }
        if (builder.tokenType == NUMERIC_VARIABLE || builder.tokenType == STRING_VARIABLE ||
            builder.tokenType == INVALID_VARIABLE_NAME
        ) {
            parseVariableAccess(builder)
            skipIntraLineWhitespace(builder)
            if (builder.tokenType == EQ_OP) builder.advanceLexer()
            skipIntraLineWhitespace(builder)
            if (isExpressionStart(builder)) parseExpression(builder)
        }
        while (!isLineEnd(builder)) builder.advanceLexer()
        stmtMarker.done(LET_STATEMENT)
    }

    private fun parseRemStatement(builder: PsiBuilder) {
        val stmtMarker = builder.mark()
        builder.advanceLexer() // consume REM_KEYWORD
        while (!isLineEnd(builder)) builder.advanceLexer()
        stmtMarker.done(REM_STATEMENT)
    }

    private fun parseEndStatement(builder: PsiBuilder) {
        val stmtMarker = builder.mark()
        builder.advanceLexer() // consume END_KEYWORD
        while (!isLineEnd(builder)) builder.advanceLexer()
        stmtMarker.done(END_STATEMENT)
    }

    private fun parseStopStatement(builder: PsiBuilder) {
        val stmtMarker = builder.mark()
        builder.advanceLexer() // consume STOP_KEYWORD
        while (!isLineEnd(builder)) builder.advanceLexer()
        stmtMarker.done(STOP_STATEMENT)
    }

    private fun parseGotoStatement(builder: PsiBuilder) {
        val stmtMarker = builder.mark()
        builder.advanceLexer() // consume GOTO_KEYWORD
        while (!isLineEnd(builder)) builder.advanceLexer()
        stmtMarker.done(GOTO_STATEMENT)
    }

    private fun parseIfStatement(builder: PsiBuilder) {
        val stmtMarker = builder.mark()
        builder.advanceLexer() // consume IF_KEYWORD
        skipWhitespace(builder)
        if (isExpressionStart(builder)) parseExpression(builder)
        skipIntraLineWhitespace(builder)
        if (builder.tokenType == THEN_KEYWORD) {
            builder.advanceLexer()
            skipIntraLineWhitespace(builder)
            if (builder.tokenType == NUMERIC_LITERAL) builder.advanceLexer()
            skipIntraLineWhitespace(builder)
            if (builder.tokenType == ELSE_KEYWORD) {
                builder.advanceLexer()
                skipIntraLineWhitespace(builder)
                if (builder.tokenType == NUMERIC_LITERAL) builder.advanceLexer()
            }
        }
        while (!isLineEnd(builder)) builder.advanceLexer()
        stmtMarker.done(IF_STATEMENT)
    }

    private fun parseForStatement(builder: PsiBuilder) {
        val stmtMarker = builder.mark()
        builder.advanceLexer() // consume FOR_KEYWORD
        skipWhitespace(builder)
        if (builder.tokenType == NUMERIC_VARIABLE || builder.tokenType == STRING_VARIABLE ||
            builder.tokenType == INVALID_VARIABLE_NAME
        ) {
            parseVariableAccess(builder)
        }
        skipIntraLineWhitespace(builder)
        if (builder.tokenType == EQ_OP) builder.advanceLexer()
        skipIntraLineWhitespace(builder)
        if (isExpressionStart(builder)) parseExpression(builder)
        skipIntraLineWhitespace(builder)
        if (builder.tokenType == TO_KEYWORD) builder.advanceLexer()
        skipIntraLineWhitespace(builder)
        if (isExpressionStart(builder)) parseExpression(builder)
        skipIntraLineWhitespace(builder)
        if (builder.tokenType == STEP_KEYWORD) {
            builder.advanceLexer()
            skipIntraLineWhitespace(builder)
            if (isExpressionStart(builder)) parseExpression(builder)
        }
        while (!isLineEnd(builder)) builder.advanceLexer()
        stmtMarker.done(FOR_STATEMENT)
    }

    private fun parseNextStatement(builder: PsiBuilder) {
        val stmtMarker = builder.mark()
        builder.advanceLexer() // consume NEXT_KEYWORD
        skipWhitespace(builder)
        if (builder.tokenType == NUMERIC_VARIABLE || builder.tokenType == STRING_VARIABLE ||
            builder.tokenType == INVALID_VARIABLE_NAME
        ) {
            parseVariableAccess(builder)
        }
        while (!isLineEnd(builder)) builder.advanceLexer()
        stmtMarker.done(NEXT_STATEMENT)
    }

    private fun parseInputStatement(builder: PsiBuilder) {
        val stmtMarker = builder.mark()
        builder.advanceLexer() // consume INPUT_KEYWORD
        skipWhitespace(builder)
        val promptCheckpoint = builder.mark()
        val hasPrompt = if (isStringOperand(builder)) {
            parseStringExpr(builder)
            skipIntraLineWhitespace(builder)
            if (builder.tokenType == COLON) {
                promptCheckpoint.drop()
                builder.advanceLexer() // consume COLON
                true
            } else {
                promptCheckpoint.rollbackTo()
                false
            }
        } else {
            promptCheckpoint.drop()
            false
        }
        if (hasPrompt) skipIntraLineWhitespace(builder)
        parseInputVariableList(builder)
        while (!isLineEnd(builder)) builder.advanceLexer()
        stmtMarker.done(INPUT_STATEMENT)
    }

    private fun parseInputVariableList(builder: PsiBuilder) {
        if (!isVariableStart(builder)) return
        parseVariableAccess(builder)
        while (true) {
            val cp = builder.mark()
            if (builder.tokenType != COMMA) {
                cp.rollbackTo(); break
            }
            builder.advanceLexer()
            if (!isVariableStart(builder)) {
                cp.rollbackTo(); break
            }
            cp.drop()
            parseVariableAccess(builder)
        }
    }

    private fun isVariableStart(builder: PsiBuilder): Boolean =
        builder.tokenType == NUMERIC_VARIABLE ||
                builder.tokenType == STRING_VARIABLE ||
                builder.tokenType == INVALID_VARIABLE_NAME

    private fun parseOnGotoStatement(builder: PsiBuilder) {
        val stmtMarker = builder.mark()
        builder.advanceLexer() // consume ON_KEYWORD
        skipWhitespace(builder)
        if (isExpressionStart(builder)) {
            parseExpression(builder)
            skipIntraLineWhitespace(builder)
        }
        while (!isLineEnd(builder)) builder.advanceLexer()
        stmtMarker.done(ON_GOTO_STATEMENT)
    }

    private fun parseUnknownStatement(builder: PsiBuilder) {
        val stmtMarker = builder.mark()
        builder.advanceLexer() // consume UNKNOWN_STATEMENT_TEXT
        while (!isLineEnd(builder)) builder.advanceLexer()
        stmtMarker.done(UNKNOWN_STATEMENT)
    }

    private fun parseInvalidLine(builder: PsiBuilder) {
        val marker = builder.mark()
        builder.advanceLexer() // consume NO_LINE_NUMBER_TEXT
        marker.done(INVALID_LINE)
    }

    private fun parseDeleteStatement(builder: PsiBuilder) {
        val stmtMarker = builder.mark()
        builder.advanceLexer() // consume DELETE_KEYWORD
        skipWhitespace(builder)
        while (!isLineEnd(builder) && !isExpressionStart(builder)) builder.advanceLexer()
        if (isExpressionStart(builder)) parseExpression(builder)
        while (!isLineEnd(builder)) builder.advanceLexer()
        stmtMarker.done(DELETE_STATEMENT)
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
        builder.eof() || builder.tokenType == LINE_NUMBER || builder.tokenType == NO_LINE_NUMBER_TEXT
}
