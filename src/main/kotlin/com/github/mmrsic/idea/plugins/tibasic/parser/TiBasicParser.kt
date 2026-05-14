package com.github.mmrsic.idea.plugins.tibasic.parser

import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.CALL_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.CALL_SUBPROGRAM_NAME
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.CLOSE_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.COLON
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.COMMA
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.CONCAT_OP
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.DATA_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.DEF_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.DELETE_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.DIM_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.DISPLAY_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.DIV_OP
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.DOT
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.ELSE_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.END_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.EQ_OP
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.FOR_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.GE_OP
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.GOSUB_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.GOTO_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.GT_OP
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.HASH
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
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.NUMERIC_FUNCTION_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.NUMERIC_LITERAL
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.NUMERIC_VARIABLE
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.ON_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.OPEN_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.OPTION_BASE_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.PLUS_OP
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.POW_OP
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.PRINT_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.RANDOMIZE_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.READ_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.REC_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.REM_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.RESTORE_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.RETURN_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.RPAREN
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.STEP_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.STOP_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.STRING_FUNCTION_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.STRING_LITERAL
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.STRING_VARIABLE
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.TAB_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.THEN_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.TO_KEYWORD
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes.UNKNOWN_STATEMENT_TEXT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.CALL_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.CLOSE_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.DATA_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.DEF_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.DELETE_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.DIM_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.DISPLAY_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.END_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.EXPRESSION
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.FOR_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.FUNCTION_CALL
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.GOSUB_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.GOTO_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.IF_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.INPUT_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.INVALID_LINE
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.LET_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.LINE
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.LINE_NUMBER_LIST_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.NEXT_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.ON_GOSUB_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.ON_GOTO_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.OPEN_OPTION
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.OPEN_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.OPTION_BASE_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.PRINT_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.RANDOMIZE_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.READ_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.REM_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.RESTORE_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.RETURN_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.STOP_STATEMENT
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes.TAB_FUNCTION
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
 * file                    ::= line*
 * line                    ::= numberedLine | invalidLine
 * numberedLine            ::= LINE_NUMBER WHITE_SPACE? statement?
 * statement               ::= printStatement | inputStatement | readStatement | dataStatement | restoreStatement | letStatement
 *                           | gotoStatement | onGotoStatement | ifStatement
 *                           | forStatement | nextStatement
 *                           | deleteStatement | lineNumberListStatement | unknownStatement
 * printStatement          ::= PRINT_KEYWORD (WHITE_SPACE printArgList)?
 *                           | PRINT_KEYWORD WHITE_SPACE? HASH WHITE_SPACE? expression
 *                             (DOT REC_KEYWORD WHITE_SPACE? expression)?
 *                             WHITE_SPACE? COLON printArgList
 * displayStatement        ::= DISPLAY_KEYWORD (WHITE_SPACE printArgList)?
 * printArgList            ::= (printSep | WHITE_SPACE | tabFunction | expression)*
 * tabFunction             ::= TAB_KEYWORD (LPAREN expression? RPAREN)?
 * printSep                ::= COLON | SEMICOLON | COMMA
 * inputStatement          ::= INPUT_KEYWORD (WHITE_SPACE? stringExpr COLON)? WHITE_SPACE? variablesList
 *                           | INPUT_KEYWORD WHITE_SPACE? HASH WHITE_SPACE? expression
 *                             (DOT REC_KEYWORD WHITE_SPACE? expression)?
 *                             WHITE_SPACE? COLON WHITE_SPACE? fileInputVariableList
 * fileInputVariableList   ::= variableAccess (COMMA variableAccess)* COMMA?
 * readStatement           ::= READ_KEYWORD WHITE_SPACE? variablesList
 * dataStatement           ::= DATA_KEYWORD WHITE_SPACE? dataList
 * dataList                ::= dataItem (COMMA dataItem)*
 * dataItem                ::= STRING_LITERAL | NUMERIC_LITERAL | PRINT_ARGUMENT | ε
 * restoreStatement        ::= RESTORE_KEYWORD (WHITE_SPACE NUMERIC_LITERAL)?
 * letStatement            ::= LET_KEYWORD? WHITE_SPACE? variableAccess EQ expression
 * remStatement            ::= REM_KEYWORD (WHITE_SPACE REM_TEXT)?
 * endStatement            ::= END_KEYWORD
 * stopStatement           ::= STOP_KEYWORD
 * gotoStatement           ::= GOTO_KEYWORD WHITE_SPACE? NUMERIC_LITERAL
 * onGotoStatement         ::= ON_KEYWORD WHITE_SPACE? expression WHITE_SPACE? GOTO_KEYWORD (WHITE_SPACE? NUMERIC_LITERAL (COMMA NUMERIC_LITERAL)*)?
 * ifStatement             ::= IF_KEYWORD WHITE_SPACE? expression WHITE_SPACE? THEN_KEYWORD WHITE_SPACE? NUMERIC_LITERAL (WHITE_SPACE? ELSE_KEYWORD WHITE_SPACE? NUMERIC_LITERAL)?
 * forStatement            ::= FOR_KEYWORD WHITE_SPACE? variableAccess EQ expression TO_KEYWORD expression (STEP_KEYWORD expression)?
 * nextStatement           ::= NEXT_KEYWORD WHITE_SPACE? variableAccess
 * callStatement           ::= CALL_KEYWORD WHITE_SPACE? CALL_SUBPROGRAM_NAME (WHITE_SPACE? LPAREN expressionList? RPAREN)? trailingTokens*
 * deleteStatement         ::= DELETE_KEYWORD (WHITE_SPACE? expression)?
 * lineNumberListStatement ::= LINE_NUMBER_LIST_KEYWORD (WHITE_SPACE? NUMERIC_LITERAL (COMMA NUMERIC_LITERAL)*)?
 * openStatement           ::= OPEN_KEYWORD WHITE_SPACE? HASH WHITE_SPACE? expression COLON expression
 *                             (WHITE_SPACE? COMMA WHITE_SPACE? openOption)*
 * openOption              ::= (SEQUENTIAL_KEYWORD | RELATIVE_KEYWORD) (WHITE_SPACE? expression)?
 *                           | DISPLAY_KEYWORD | INTERNAL_KEYWORD
 *                           | INPUT_KEYWORD | OUTPUT_KEYWORD | APPEND_KEYWORD | UPDATE_KEYWORD
 *                           | FIXED_KEYWORD | VARIABLE_KEYWORD
 *                           | PERMANENT_KEYWORD
 * closeStatement          ::= CLOSE_KEYWORD WHITE_SPACE? HASH WHITE_SPACE? expression
 * unknownStatement        ::= UNKNOWN_STATEMENT_TEXT
 * invalidLine             ::= NO_LINE_NUMBER_TEXT
 * variablesList           ::= variableAccess (COMMA variableAccess)*
 * expression              ::= numericCmp
 * stringExpr              ::= stringOperand (CONCAT_OP stringOperand)*
 * stringOperand           ::= STRING_LITERAL | variableAccess(STRING_VARIABLE)
 * numericCmp              ::= comparable (cmpOp comparable)*   -- left-to-right; result always numeric
 * comparable              ::= stringExpr | numericAdd          -- string operands valid in comparisons
 * cmpOp                   ::= '=' | '<' | '>' | '<>' | '<=' | '>='
 * numericAdd              ::= numericMul ( ('+' | '-') numericMul )*
 * numericMul              ::= numericPow ( ('*' | '/') numericPow )*
 * numericPow              ::= numericUnary ('^' numericUnary)*        -- right-associative
 * numericUnary            ::= ('+' | '-') numericUnary | numericPrimary
 * numericPrimary          ::= NUMERIC_LITERAL
 *                           | variableAccess
 *                           | STRING_LITERAL | variableAccess(STRING_VARIABLE)   -- mismatch, parsed for error reporting
 *                           | '(' numericCmp ')'
 * variableAccess          ::= (NUMERIC_VARIABLE | STRING_VARIABLE) ( '(' subscriptList ')' )?
 * subscriptList           ::= numericCmp (',' numericCmp)*           -- 1-3 validated by annotator
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
            GOSUB_KEYWORD -> parseGosubStatement(builder)
            RETURN_KEYWORD -> parseReturnStatement(builder)
            ON_KEYWORD -> parseOnBranchStatement(builder)
            IF_KEYWORD -> parseIfStatement(builder)
            FOR_KEYWORD -> parseForStatement(builder)
            NEXT_KEYWORD -> parseNextStatement(builder)
            INPUT_KEYWORD -> parseInputStatement(builder)
            READ_KEYWORD -> parseReadStatement(builder)
            DATA_KEYWORD -> parseDataStatement(builder)
            RESTORE_KEYWORD -> parseRestoreStatement(builder)
            PRINT_KEYWORD -> parsePrintStatement(builder)
            DISPLAY_KEYWORD -> parseDisplayStatement(builder)
            CALL_KEYWORD -> parseCallStatement(builder)
            RANDOMIZE_KEYWORD -> parseRandomizeStatement(builder)
            DEF_KEYWORD -> parseDefStatement(builder)
            DIM_KEYWORD -> parseDimStatement(builder)
            OPTION_BASE_KEYWORD -> parseOptionBaseStatement(builder)
            OPEN_KEYWORD -> parseOpenStatement(builder)
            CLOSE_KEYWORD -> parseCloseStatement(builder)
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

    private fun parseRandomizeStatement(builder: PsiBuilder) {
        val stmtMarker = builder.mark()
        builder.advanceLexer() // consume RANDOMIZE_KEYWORD
        skipWhitespace(builder)
        if (!isLineEnd(builder)) parseExpression(builder)
        while (!isLineEnd(builder)) builder.advanceLexer()
        stmtMarker.done(RANDOMIZE_STATEMENT)
    }

    private fun parseDefStatement(builder: PsiBuilder) {
        val stmtMarker = builder.mark()
        builder.advanceLexer() // consume DEF_KEYWORD
        skipWhitespace(builder)
        if (isVariableStart(builder)) {
            builder.advanceLexer() // consume function name — direct token, not wrapped in VARIABLE_ACCESS
        }
        skipIntraLineWhitespace(builder)
        if (builder.tokenType == LPAREN) {
            builder.advanceLexer() // consume (
            skipIntraLineWhitespace(builder)
            if (isVariableStart(builder)) builder.advanceLexer() // consume parameter token
            skipIntraLineWhitespace(builder)
            if (builder.tokenType == RPAREN) builder.advanceLexer() // consume )
        }
        skipIntraLineWhitespace(builder)
        if (builder.tokenType == EQ_OP) builder.advanceLexer() // consume =
        skipIntraLineWhitespace(builder)
        if (isExpressionStart(builder)) parseExpression(builder)
        while (!isLineEnd(builder)) builder.advanceLexer()
        stmtMarker.done(DEF_STATEMENT)
    }

    private fun parseDimStatement(builder: PsiBuilder) {
        val stmtMarker = builder.mark()
        builder.advanceLexer() // consume DIM_KEYWORD
        skipWhitespace(builder)
        if (isVariableStart(builder)) {
            parseVariableAccess(builder)
            skipIntraLineWhitespace(builder)
            while (builder.tokenType == COMMA) {
                builder.advanceLexer() // consume COMMA
                skipIntraLineWhitespace(builder)
                if (isVariableStart(builder)) parseVariableAccess(builder)
            }
        }
        while (!isLineEnd(builder)) builder.advanceLexer()
        stmtMarker.done(DIM_STATEMENT)
    }

    private fun parseOptionBaseStatement(builder: PsiBuilder) {
        val stmtMarker = builder.mark()
        builder.advanceLexer() // consume OPTION_BASE_KEYWORD
        skipWhitespace(builder)
        while (!isLineEnd(builder)) builder.advanceLexer()
        stmtMarker.done(OPTION_BASE_STATEMENT)
    }

    private fun parseOpenStatement(builder: PsiBuilder) {
        val stmtMarker = builder.mark()
        builder.advanceLexer() // consume OPEN_KEYWORD
        skipIntraLineWhitespace(builder)
        if (builder.tokenType == HASH) builder.advanceLexer()
        skipIntraLineWhitespace(builder)
        if (isExpressionStart(builder)) {
            val exprMarker = builder.mark()
            parseNumericCmp(builder)
            exprMarker.done(EXPRESSION)
        }
        skipIntraLineWhitespace(builder)
        if (builder.tokenType == COLON) builder.advanceLexer()
        skipIntraLineWhitespace(builder)
        if (isExpressionStart(builder)) {
            val exprMarker = builder.mark()
            parseNumericCmp(builder)
            exprMarker.done(EXPRESSION)
        }
        while (!isLineEnd(builder) && builder.tokenType == COMMA) {
            val optionMarker = builder.mark()
            builder.advanceLexer() // consume COMMA
            skipIntraLineWhitespace(builder)
            val optType = builder.tokenType
            if (optType in TiBasicTokenTypes.OPEN_OPTION_KEYWORDS) {
                builder.advanceLexer() // consume option keyword
                if (optType in TiBasicTokenTypes.OPEN_ORGANIZATION_KEYWORDS || optType in TiBasicTokenTypes.OPEN_RECORD_FORMAT_KEYWORDS) {
                    skipIntraLineWhitespace(builder)
                    if (isExpressionStart(builder)) {
                        val exprMarker = builder.mark()
                        parseNumericCmp(builder)
                        exprMarker.done(EXPRESSION)
                    }
                }
            } else {
                while (!isLineEnd(builder) && builder.tokenType != COMMA) builder.advanceLexer()
            }
            optionMarker.done(OPEN_OPTION)
        }
        while (!isLineEnd(builder)) builder.advanceLexer()
        stmtMarker.done(OPEN_STATEMENT)
    }

    private fun parseCloseStatement(builder: PsiBuilder) {
        val stmtMarker = builder.mark()
        builder.advanceLexer() // consume CLOSE_KEYWORD
        skipIntraLineWhitespace(builder)
        if (builder.tokenType == HASH) builder.advanceLexer()
        skipIntraLineWhitespace(builder)
        if (isExpressionStart(builder)) {
            val exprMarker = builder.mark()
            parseNumericCmp(builder)
            exprMarker.done(EXPRESSION)
        }
        skipIntraLineWhitespace(builder)
        if (builder.tokenType == COLON) {
            builder.advanceLexer() // consume COLON
            skipIntraLineWhitespace(builder)
            if (builder.tokenType == DELETE_KEYWORD) builder.advanceLexer() // consume DELETE
        }
        while (!isLineEnd(builder)) builder.advanceLexer()
        stmtMarker.done(CLOSE_STATEMENT)
    }

    private fun parseGotoStatement(builder: PsiBuilder) {
        val stmtMarker = builder.mark()
        builder.advanceLexer() // consume GOTO_KEYWORD
        while (!isLineEnd(builder)) builder.advanceLexer()
        stmtMarker.done(GOTO_STATEMENT)
    }

    private fun parseGosubStatement(builder: PsiBuilder) {
        val stmtMarker = builder.mark()
        builder.advanceLexer() // consume GOSUB_KEYWORD
        while (!isLineEnd(builder)) builder.advanceLexer()
        stmtMarker.done(GOSUB_STATEMENT)
    }

    private fun parseReturnStatement(builder: PsiBuilder) {
        val stmtMarker = builder.mark()
        builder.advanceLexer() // consume RETURN_KEYWORD
        while (!isLineEnd(builder)) builder.advanceLexer()
        stmtMarker.done(RETURN_STATEMENT)
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
        if (builder.tokenType == HASH) {
            parseFileInputBody(builder)
        } else {
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
        }
        while (!isLineEnd(builder)) builder.advanceLexer()
        stmtMarker.done(INPUT_STATEMENT)
    }

    private fun parseFileInputBody(builder: PsiBuilder) {
        builder.advanceLexer() // consume HASH
        skipIntraLineWhitespace(builder)
        if (isExpressionStart(builder)) {
            val exprMarker = builder.mark()
            parseNumericCmp(builder)
            exprMarker.done(EXPRESSION)
        }
        skipIntraLineWhitespace(builder)
        if (builder.tokenType == DOT) {
            builder.advanceLexer() // consume DOT
            if (builder.tokenType == REC_KEYWORD) {
                builder.advanceLexer() // consume REC_KEYWORD
                skipIntraLineWhitespace(builder)
                if (isExpressionStart(builder)) {
                    val exprMarker = builder.mark()
                    parseNumericCmp(builder)
                    exprMarker.done(EXPRESSION)
                }
            }
        }
        skipIntraLineWhitespace(builder)
        if (builder.tokenType == COLON) builder.advanceLexer() // consume COLON
        skipIntraLineWhitespace(builder)
        parseInputVariableList(builder, trailingCommaAllowed = true)
    }

    private fun parseReadStatement(builder: PsiBuilder) {
        val stmtMarker = builder.mark()
        builder.advanceLexer() // consume READ_KEYWORD
        skipWhitespace(builder)
        parseInputVariableList(builder)
        while (!isLineEnd(builder)) builder.advanceLexer()
        stmtMarker.done(READ_STATEMENT)
    }

    private fun parseDataStatement(builder: PsiBuilder) {
        val stmtMarker = builder.mark()
        builder.advanceLexer() // consume DATA_KEYWORD
        while (!isLineEnd(builder)) builder.advanceLexer()
        stmtMarker.done(DATA_STATEMENT)
    }

    private fun parseRestoreStatement(builder: PsiBuilder) {
        val stmtMarker = builder.mark()
        builder.advanceLexer() // consume RESTORE_KEYWORD
        skipWhitespace(builder)
        if (builder.tokenType == HASH) {
            parseRestoreFileBody(builder)
        } else {
            if (isExpressionStart(builder)) {
                val exprMarker = builder.mark()
                parseNumericCmp(builder)
                exprMarker.done(EXPRESSION)
            }
            while (!isLineEnd(builder)) builder.advanceLexer()
        }
        stmtMarker.done(RESTORE_STATEMENT)
    }

    private fun parseRestoreFileBody(builder: PsiBuilder) {
        builder.advanceLexer() // consume HASH
        skipIntraLineWhitespace(builder)
        if (isExpressionStart(builder)) {
            val exprMarker = builder.mark()
            parseNumericCmp(builder)
            exprMarker.done(EXPRESSION)
        }
        skipIntraLineWhitespace(builder)
        if (builder.tokenType == COMMA) {
            builder.advanceLexer() // consume COMMA
            skipIntraLineWhitespace(builder)
            if (builder.tokenType == REC_KEYWORD) {
                builder.advanceLexer() // consume REC_KEYWORD
                skipIntraLineWhitespace(builder)
                if (isExpressionStart(builder)) {
                    val exprMarker = builder.mark()
                    parseNumericCmp(builder)
                    exprMarker.done(EXPRESSION)
                }
            }
        }
        while (!isLineEnd(builder)) builder.advanceLexer()
    }

    private fun parseInputVariableList(builder: PsiBuilder, trailingCommaAllowed: Boolean = false) {
        if (!isVariableStart(builder)) return
        parseVariableAccess(builder)
        while (true) {
            val cp = builder.mark()
            if (builder.tokenType != COMMA) {
                cp.rollbackTo(); break
            }
            builder.advanceLexer()
            if (!isVariableStart(builder)) {
                if (trailingCommaAllowed) {
                    cp.drop(); break
                } else {
                    cp.rollbackTo(); break
                }
            }
            cp.drop()
            parseVariableAccess(builder)
        }
    }

    private fun isVariableStart(builder: PsiBuilder): Boolean =
        builder.tokenType == NUMERIC_VARIABLE ||
                builder.tokenType == STRING_VARIABLE ||
                builder.tokenType == INVALID_VARIABLE_NAME

    private fun parseOnBranchStatement(builder: PsiBuilder) {
        val stmtMarker = builder.mark()
        builder.advanceLexer() // consume ON_KEYWORD
        skipWhitespace(builder)
        if (isExpressionStart(builder)) {
            parseExpression(builder)
            skipIntraLineWhitespace(builder)
        }
        val isGosub = builder.tokenType == GOSUB_KEYWORD
        while (!isLineEnd(builder)) builder.advanceLexer()
        stmtMarker.done(if (isGosub) ON_GOSUB_STATEMENT else ON_GOTO_STATEMENT)
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
        builder.advanceLexer() // consume PRINT_KEYWORD
        skipIntraLineWhitespace(builder)
        if (builder.tokenType == HASH) {
            parsePrintFileOutputBody(builder)
        } else {
            parsePrintArgList(builder)
        }
        stmtMarker.done(PRINT_STATEMENT)
    }

    private fun parsePrintFileOutputBody(builder: PsiBuilder) {
        builder.advanceLexer() // consume HASH
        skipIntraLineWhitespace(builder)
        if (isExpressionStart(builder)) {
            val exprMarker = builder.mark()
            parseNumericCmp(builder)
            exprMarker.done(EXPRESSION)
        }
        skipIntraLineWhitespace(builder)
        if (builder.tokenType == DOT) {
            builder.advanceLexer() // consume DOT
            if (builder.tokenType == REC_KEYWORD) {
                builder.advanceLexer() // consume REC_KEYWORD
                skipIntraLineWhitespace(builder)
                if (isExpressionStart(builder)) {
                    val exprMarker = builder.mark()
                    parseNumericCmp(builder)
                    exprMarker.done(EXPRESSION)
                }
            }
        }
        skipIntraLineWhitespace(builder)
        if (builder.tokenType == COLON) builder.advanceLexer() // consume delimiter colon
        parsePrintArgList(builder)
    }

    private fun parseDisplayStatement(builder: PsiBuilder) {
        val stmtMarker = builder.mark()
        builder.advanceLexer() // consume DISPLAY_KEYWORD
        parsePrintArgList(builder)
        stmtMarker.done(DISPLAY_STATEMENT)
    }

    private fun parseCallStatement(builder: PsiBuilder) {
        val stmtMarker = builder.mark()
        builder.advanceLexer() // consume CALL_KEYWORD
        skipWhitespace(builder)
        if (builder.tokenType == CALL_SUBPROGRAM_NAME) builder.advanceLexer()
        skipWhitespace(builder)
        if (builder.tokenType == LPAREN) {
            builder.advanceLexer() // consume LPAREN
            skipWhitespace(builder)
            if (builder.tokenType != RPAREN && !isLineEnd(builder)) {
                parseExpression(builder)
                skipWhitespace(builder)
                while (builder.tokenType == COMMA) {
                    builder.advanceLexer() // consume COMMA
                    skipWhitespace(builder)
                    parseExpression(builder)
                    skipWhitespace(builder)
                }
            }
            if (builder.tokenType == RPAREN) builder.advanceLexer()
        }
        while (!isLineEnd(builder)) builder.advanceLexer()
        stmtMarker.done(CALL_STATEMENT)
    }

    private fun parsePrintArgList(builder: PsiBuilder) {
        skipWhitespace(builder)
        while (!isLineEnd(builder)) {
            when {
                isPrintSeparator(builder) -> builder.advanceLexer()
                builder.tokenType == TokenType.WHITE_SPACE -> builder.advanceLexer()
                builder.tokenType == TAB_KEYWORD -> parseTabFunction(builder)
                isExpressionStart(builder) -> parseExpression(builder)
                else -> builder.advanceLexer() // unexpected token — consume to prevent infinite loop
            }
        }
    }

    private fun parseTabFunction(builder: PsiBuilder) {
        val marker = builder.mark()
        builder.advanceLexer() // consume TAB_KEYWORD
        skipIntraLineWhitespace(builder)
        if (builder.tokenType == LPAREN) {
            builder.advanceLexer() // consume (
            skipIntraLineWhitespace(builder)
            if (isExpressionStart(builder)) parseExpression(builder)
            skipIntraLineWhitespace(builder)
            if (builder.tokenType == RPAREN) builder.advanceLexer()
        }
        marker.done(TAB_FUNCTION)
    }

    private fun isPrintSeparator(builder: PsiBuilder): Boolean =
        builder.tokenType in TiBasicTokenTypes.PRINT_SEPARATORS

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
        when (builder.tokenType) {
            STRING_VARIABLE -> parseVariableAccess(builder)
            STRING_FUNCTION_KEYWORD -> parseFunctionCall(builder)
            else -> builder.advanceLexer() // STRING_LITERAL
        }
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
            NUMERIC_FUNCTION_KEYWORD -> parseFunctionCall(builder)
            LPAREN -> {
                builder.advanceLexer()
                skipIntraLineWhitespace(builder)
                if (isNumericPrimaryStart(builder)) parseNumericCmp(builder)
                skipIntraLineWhitespace(builder)
                if (builder.tokenType == RPAREN) builder.advanceLexer()
            }
        }
    }

    private fun parseFunctionCall(builder: PsiBuilder) {
        val marker = builder.mark()
        builder.advanceLexer() // consume NUMERIC_FUNCTION_KEYWORD or STRING_FUNCTION_KEYWORD
        skipIntraLineWhitespace(builder)
        if (builder.tokenType == LPAREN) {
            builder.advanceLexer() // consume (
            parseFunctionArgList(builder)
            skipIntraLineWhitespace(builder)
            if (builder.tokenType == RPAREN) builder.advanceLexer()
        }
        marker.done(FUNCTION_CALL)
    }

    private fun parseFunctionArgList(builder: PsiBuilder) {
        skipIntraLineWhitespace(builder)
        if (builder.tokenType == RPAREN || isLineEnd(builder)) return
        parseFunctionArg(builder)
        while (true) {
            val cp = builder.mark()
            skipIntraLineWhitespace(builder)
            if (builder.tokenType != COMMA) {
                cp.rollbackTo(); break
            }
            builder.advanceLexer()
            skipIntraLineWhitespace(builder)
            if (!isExpressionStart(builder)) {
                cp.rollbackTo(); break
            }
            cp.drop()
            parseFunctionArg(builder)
        }
    }

    private fun parseFunctionArg(builder: PsiBuilder) {
        val marker = builder.mark()
        parseNumericCmp(builder)
        marker.done(EXPRESSION)
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
        builder.tokenType == STRING_LITERAL || builder.tokenType == STRING_VARIABLE ||
                builder.tokenType == STRING_FUNCTION_KEYWORD

    private fun isNumericPrimaryStart(builder: PsiBuilder): Boolean =
        builder.tokenType in setOf(
            NUMERIC_LITERAL,
            NUMERIC_VARIABLE,
            PLUS_OP,
            MINUS_OP,
            LPAREN,
            STRING_LITERAL,
            STRING_VARIABLE,
            NUMERIC_FUNCTION_KEYWORD,
            STRING_FUNCTION_KEYWORD
        )

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
