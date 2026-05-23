package com.github.mmrsic.idea.plugins.tibasic.highlight

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase

class TiBasicAnnotatorTest : TiBasicTestBase() {

    fun testNoWarningForAscendingLineNumbers() {
        configureFile("100 PRINT \"A\"\n200 PRINT \"B\"\n300 PRINT \"C\"")
        myFixture.checkHighlighting(false, false, true)
    }

    fun testWarningForEqualLineNumber() {
        configureFile(
            "100 PRINT \"A\"\n<error descr=\"Duplicate line number 100\">100 PRINT \"B\"</error>"
        )
        myFixture.checkHighlighting(true, false, true)
    }

    fun testWarningForDescendingLineNumber() {
        configureFile(
            "200 PRINT \"A\"\n<warning descr=\"Line number 100 does not follow ascending order (previous: 200)\">100 PRINT \"B\"</warning>"
        )
        myFixture.checkHighlighting(false, false, true)
    }

    fun testWarningOnlyForNonAscendingLineInSequence() {
        configureFile(
            "100 PRINT \"A\"\n200 PRINT \"B\"\n<warning descr=\"Line number 150 does not follow ascending order (previous: 200)\">150 PRINT \"C\"</warning>"
        )
        myFixture.checkHighlighting(false, false, true)
    }

    fun testNoWarningForSingleLine() {
        configureFile("100 PRINT \"A\"")
        myFixture.checkHighlighting(false, false, true)
    }

    fun testErrorForDuplicateLineNumber() {
        configureFile(
            "100 PRINT \"A\"\n<error descr=\"Duplicate line number 100\">100 PRINT \"B\"</error>"
        )
        myFixture.checkHighlighting(true, false, true)
    }

    fun testErrorForTriplicateLineNumber() {
        configureFile(
            "100 PRINT \"A\"\n<error descr=\"Duplicate line number 100\">100 PRINT \"B\"</error>\n<error descr=\"Duplicate line number 100\">100 PRINT \"C\"</error>"
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorAndWarningForDuplicateNonAscendingLineNumber() {
        configureFile(
            "200 PRINT \"A\"\n<error descr=\"Duplicate line number 200\">200 PRINT \"B\"</error>"
        )
        myFixture.checkHighlighting(true, false, true)
    }

    fun testNoErrorForStringLiteralPrintArgument() {
        configureFile("100 PRINT \"hello\"")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForStringWithEscapedQuote() {
        configureFile("100 PRINT \"say \"\"hi\"\"\"")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForEmptyStringLiteral() {
        configureFile("100 PRINT \"\"")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForPrintWithoutArgument() {
        configureFile("100 PRINT")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForImplicitLetNumericArrayWithWhitespaceBeforeSubscript() {
        configureFile("38 A (8) = 7")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForConcatenationOfTwoStringLiterals() {
        configureFile("100 PRINT \"hello\" & \"world\"")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForConcatenationOfThreeStringLiterals() {
        configureFile("100 PRINT \"a\" & \"b\" & \"c\"")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForTrailingConcatOpWithNoRightOperand() {
        configureFile(
            "100 PRINT \"a\" <error descr=\"PRINT argument must be an expression\">&</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForLeadingConcatOpWithNoLeftOperand() {
        configureFile(
            "100 PRINT <error descr=\"PRINT argument must be an expression\">&</error> \"b\"",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForSemicolonSeparatedStringLiterals() {
        configureFile("100 PRINT \"a\";\"b\"")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForCommaSeparatedVariables() {
        configureFile("100 PRINT A,B")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForColonSeparatedVariables() {
        configureFile("100 PRINT X:Y")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForMixedSeparators() {
        configureFile("100 PRINT \"X=\";X,\" Y=\";Y")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForLeadingSeparator() {
        configureFile("100 PRINT ,\"RECHTS\"")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForTrailingSeparator() {
        configureFile("100 PRINT \"HALLO\";")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForMultipleSeparatorsOnly() {
        configureFile("100 PRINT :;,")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForTwoExpressionsWithoutSeparator() {
        configureFile("100 PRINT \"a\" <error descr=\"Separator expected between expressions\">\"b\"</error>")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForNumericLiteralPrintArgument() {
        configureFile("100 PRINT 42")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForConcatWithNonStringRightOperand() {
        configureFile(
            "100 PRINT \"a\" " +
                    "<error descr=\"PRINT argument must be an expression\">&</error> " +
                    "42",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForStringVariable() {
        configureFile("100 PRINT Y$")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForStringVariableArray() {
        configureFile("100 PRINT A$(1)")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForStringVariableWithSpacesAroundSubscript() {
        configureFile("100 PRINT A$ ( 1 , 2 )")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForDigitAsFirstCharInVariableName() {
        // 1 is a numeric literal, A$ is a string variable after numeric expr → separator expected
        configureFile("100 PRINT 1<error descr=\"Separator expected between expressions\">A$</error>")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForTooLongVariableName() {
        configureFile("100 PRINT <error descr=\"Bad variable name\">TOOLONGVARIABLE$</error>")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForFourDimensionalSubscript() {
        configureFile("100 PRINT <error descr=\"Bad subscript definition\">A$(1,2,3,4)</error>")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForEmptySubscript() {
        configureFile("100 PRINT <error descr=\"Bad subscript definition\">A$()</error>")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForStringVariableMissingClosingSubscriptParen() {
        configureFile("100 PRINT <error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">A$(1</error>")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoConflictForSameSimpleVariableUsedTwice() {
        configureFile("100 PRINT Y$\n200 PRINT Y$")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoConflictForSameArrayUsedTwice() {
        configureFile("100 PRINT A$(1)\n200 PRINT A$(2)")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoConflictForSameTwoDimensionalArrayUsedTwice() {
        configureFile("100 PRINT A$(1,2)\n200 PRINT A$(3,4)")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testConflictBetweenSimpleVariableAndArray() {
        configureFile(
            "100 PRINT <error descr=\"Name conflict\">A$</error>\n" +
                    "200 PRINT <error descr=\"Name conflict\">A$(1)</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testConflictBetweenArraysWithDifferentDimensions() {
        configureFile(
            "100 PRINT <error descr=\"Name conflict\">A$(1)</error>\n" +
                    "200 PRINT <error descr=\"Name conflict\">A$(1,2)</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testConflictAnnotatedOnAllThreeOccurrences() {
        configureFile(
            "100 PRINT <error descr=\"Name conflict\">A$</error>\n" +
                    "200 PRINT <error descr=\"Name conflict\">A$(1)</error>\n" +
                    "300 PRINT <error descr=\"Name conflict\">A$</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoConflictBetweenDifferentVariableNames() {
        configureFile("100 PRINT A$\n200 PRINT B$(1)")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForLineNumberOne() {
        configureFile("1 PRINT")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForLineNumberMax() {
        configureFile("32767 PRINT")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForLineNumberWithLeadingZeros() {
        configureFile("0100 PRINT")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForLineNumberWithManyLeadingZeros() {
        configureFile("000000100 PRINT")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForLineNumberZero() {
        configureFile("<error descr=\"Bad line number\">000</error> PRINT")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForLineNumberAboveMax() {
        configureFile("<error descr=\"Bad line number\">32768</error> PRINT")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForVeryLargeLineNumber() {
        configureFile("<error descr=\"Bad line number\">99999</error> PRINT")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testBadLineNumberDoesNotTriggerDuplicateError() {
        // Two lines with out-of-range numbers should not be flagged as duplicates of each other
        configureFile(
            "<error descr=\"Bad line number\">32768</error> PRINT\n" +
                    "<error descr=\"Bad line number\">32769</error> PRINT",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForUnknownStatementWithValidLineNumber() {
        configureFile(
            "100 <error descr=\"Incorrect statement\">BEEP</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForUnknownStatementWithLeadingZerosLineNumber() {
        configureFile(
            "0100 <error descr=\"Incorrect statement\">BEEP</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForUnknownStatementWithLineNumberAboveMax() {
        configureFile(
            "<error descr=\"Bad line number\">32768</error> <error descr=\"Incorrect statement\">BEEP</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForUnknownStatementWithLineNumberZero() {
        configureFile(
            "<error descr=\"Bad line number\">0</error> <error descr=\"Incorrect statement\">BEEP</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForLineWithNoLineNumber() {
        configureFile(
            "<error descr=\"Line number expected\">this is not valid</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForLineWithLeadingWhitespaceAndNoLineNumber() {
        configureFile(
            "<error descr=\"Line number expected\">   NOT A LINE</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForRemWithoutText() {
        configureFile("100 REM")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForRemWithText() {
        configureFile("100 REM This is a comment")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForLineNumberOnly() {
        configureFile("100")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForNumericVariableInPrint() {
        configureFile("100 PRINT A")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForNumericVariableWithMixedCase() {
        configureFile("100 PRINT myVar")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForNumericVariableWithDigitsAndSpecialFirstChars() {
        configureFile("100 PRINT _A1@B")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForNumericVariableWithHyphen() {
        // A-B is valid subtraction
        configureFile("100 PRINT A-B")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForTokenStartingWithDigitNotNumericLiteral() {
        // 1 is a numeric literal expression; A is a second expression without separator
        configureFile(
            "100 PRINT 1<error descr=\"Separator expected between expressions\">A</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForDollarSignOnlyInStringVariable() {
        // A$ is a valid string variable ($ only at end)
        configureFile("100 PRINT A$")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForNumericVariableWithValidSubscript1D() {
        configureFile("100 PRINT A(1)")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForNumericVariableWithValidSubscript2D() {
        configureFile("100 PRINT A(1,2)")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForNumericVariableWithValidSubscript3D() {
        configureFile("100 PRINT A(1,2,3)")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForNumericVariableWithSpacesAroundSubscript() {
        configureFile("100 PRINT A ( 1 , 2 )")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForNumericVariableWithTooManySubscripts() {
        configureFile(
            "100 PRINT <error descr=\"Bad subscript definition\">A(1,2,3,4)</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForNumericVariableWithEmptySubscript() {
        configureFile(
            "100 PRINT <error descr=\"Bad subscript definition\">A()</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoNameConflictBetweenArrayAndEmptySubscriptSameName() {
        configureFile("10 PRINT A(1),<error descr=\"Bad subscript definition\">A()</error>")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForNumericVariableMissingClosingSubscriptParen() {
        configureFile(
            "100 PRINT <error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">A(1</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForNonEmptyParenthesizedExpressionAfterMultiplication() {
        configureFile("3110 GANG(GN)=(GANG(GN)+1)*(GN)")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForEmptyParenthesizedExpressionAfterMultiplication() {
        configureFile(
            "3110 GANG(GN)=(GANG(GN)+1)*<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">()</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForNegatedParenthesizedExpressionAfterMultiplication() {
        configureFile("3110 GANG(GN)=(GANG(GN)+1)*(-GN)")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForUnaryMinusOnlyParenthesizedExpressionAfterMultiplication() {
        configureFile(
            "3110 GANG(GN)=(GANG(GN)+1)*<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">(-)</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForUnaryPlusOnlyParenthesizedExpressionAfterMultiplication() {
        configureFile(
            "3110 GANG(GN)=(GANG(GN)+1)*<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">(+)</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForNumericVariableTooLong() {
        configureFile(
            "100 PRINT <error descr=\"Bad variable name\">ABCDEFGHIJKLMNOP</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForNumericVariableExactly15Chars() {
        configureFile("100 PRINT ABCDEFGHIJKLMNO")
        myFixture.checkHighlighting(true, false, false)
    }

    // --- Numeric variable name conflict tests ---

    fun testNameConflictSimpleNumericAndArraySameName() {
        configureFile(
            "100 PRINT <error descr=\"Name conflict\">A</error>\n" +
                    "200 PRINT <error descr=\"Name conflict\">A(1)</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNameConflictTwoNumericArraysDifferentDims() {
        configureFile(
            "100 PRINT <error descr=\"Name conflict\">A(1)</error>\n" +
                    "200 PRINT <error descr=\"Name conflict\">A(1,2)</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoNameConflictTwoNumericSimpleVarsWithSameName() {
        configureFile("100 PRINT A\n200 PRINT A")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoNameConflictTwoNumericArraysSameDimCount() {
        configureFile("100 PRINT A(1)\n200 PRINT A(2)")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoNameConflictNumericAndStringVarSameName() {
        configureFile("100 PRINT A\n200 PRINT A$")
        myFixture.checkHighlighting(true, false, false)
    }

    // --- Keyword conflict tests ---

    // --- Command-as-statement tests ---

    fun testErrorForCommandByeUsedAsStatement() {
        configureFile(
            "100 <error descr=\"Command must not be used as statement\">BYE</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForCommandListUsedAsStatement() {
        configureFile(
            "100 <error descr=\"Command must not be used as statement\">LIST</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForCommandNewUsedAsStatement() {
        configureFile(
            "100 <error descr=\"Command must not be used as statement\">NEW</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForCommandRunUsedAsStatement() {
        configureFile(
            "100 <error descr=\"Command must not be used as statement\">RUN</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForCommandNumberUsedAsStatement() {
        configureFile(
            "100 <error descr=\"Command must not be used as statement\">NUMBER</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForCommandNumUsedAsStatement() {
        configureFile(
            "100 <error descr=\"Command must not be used as statement\">NUM</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForCommandCaseInsensitiveUsedAsStatement() {
        configureFile(
            "100 <error descr=\"Command must not be used as statement\">bye</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForPrintUsedAsStatement() {
        configureFile("100 PRINT")
        myFixture.checkHighlighting(true, false, false)
    }

    // --- Keyword conflict tests ---

    fun testErrorForNumericVariableMatchingKeyword() {
        configureFile(
            "100 PRINT <error descr=\"Keyword cannot be used as variable name\">PRINT</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForStringVariableWithSameNameAsKeywordButDifferentSuffix() {
        configureFile("100 PRINT PRINT$")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForNumericVariableMatchingKeywordCaseInsensitive() {
        configureFile(
            "100 PRINT <error descr=\"Keyword cannot be used as variable name\">print</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForVariableWithKeywordAsPrefix() {
        configureFile("100 PRINT PRINTER")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForVariableWithKeywordAsSuffix() {
        configureFile("100 PRINT APRINT")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForNumericArrayMatchingKeyword() {
        configureFile(
            "100 PRINT <error descr=\"Keyword cannot be used as variable name\">PRINT(1)</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForNumericVariableMatchingKeywordNew() {
        configureFile(
            "100 PRINT <error descr=\"Command must not be used as variable name\">NEW</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForNumericVariableMatchingKeywordList() {
        configureFile(
            "100 PRINT <error descr=\"Command must not be used as variable name\">LIST</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForNumericVariableMatchingKeywordRun() {
        configureFile(
            "100 PRINT <error descr=\"Command must not be used as variable name\">RUN</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForNumericVariableMatchingKeywordBye() {
        configureFile(
            "100 PRINT <error descr=\"Command must not be used as variable name\">BYE</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForNumericVariableMatchingCommandNumber() {
        configureFile(
            "100 PRINT <error descr=\"Command must not be used as variable name\">NUMBER</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForNumericVariableMatchingCommandNum() {
        configureFile(
            "100 PRINT <error descr=\"Command must not be used as variable name\">NUM</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForNumericVariableMatchingCommandRes() {
        configureFile(
            "100 PRINT <error descr=\"Command must not be used as variable name\">RES</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForNumericVariableMatchingCommandResequence() {
        configureFile(
            "100 PRINT <error descr=\"Command must not be used as variable name\">RESEQUENCE</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForNumericVariableMatchingCommandCon() {
        configureFile(
            "100 PRINT <error descr=\"Command must not be used as variable name\">CON</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForNumericVariableMatchingCommandContinue() {
        configureFile(
            "100 PRINT <error descr=\"Command must not be used as variable name\">CONTINUE</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForNumericVariableMatchingCommandEdit() {
        configureFile(
            "100 PRINT <error descr=\"Command must not be used as variable name\">EDIT</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForNumericVariableMatchingCommandSave() {
        configureFile(
            "100 PRINT <error descr=\"Command must not be used as variable name\">SAVE</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForNumericVariableMatchingCommandOld() {
        configureFile(
            "100 PRINT <error descr=\"Command must not be used as variable name\">OLD</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForCommandResUsedAsStatement() {
        configureFile(
            "100 <error descr=\"Command must not be used as statement\">RES</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForCommandResequenceUsedAsStatement() {
        configureFile(
            "100 <error descr=\"Command must not be used as statement\">RESEQUENCE</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForCommandConUsedAsStatement() {
        configureFile(
            "100 <error descr=\"Command must not be used as statement\">CON</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForCommandContinueUsedAsStatement() {
        configureFile(
            "100 <error descr=\"Command must not be used as statement\">CONTINUE</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForCommandEditUsedAsStatement() {
        configureFile(
            "100 <error descr=\"Command must not be used as statement\">EDIT</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForCommandSaveUsedAsStatement() {
        configureFile(
            "100 <error descr=\"Command must not be used as statement\">SAVE</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForCommandOldUsedAsStatement() {
        configureFile(
            "100 <error descr=\"Command must not be used as statement\">OLD</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    // --- Arithmetic operator tests ---

    fun testNoErrorForAddition() {
        configureFile("100 PRINT A+B")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForSubtraction() {
        configureFile("100 PRINT A-B")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForMultiplication() {
        configureFile("100 PRINT A*B")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForDivision() {
        configureFile("100 PRINT A/B")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForPower() {
        configureFile("100 PRINT A^B")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForUnaryMinus() {
        configureFile("100 PRINT -A")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForMultipleUnaryPrefixes() {
        configureFile("100 PRINT +-+-1")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForParenthesizedExpression() {
        configureFile("100 PRINT (A+B)")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForComplexArithmeticExpression() {
        configureFile("100 PRINT A+B*C-D/E^F")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForSubscriptWithExpression() {
        configureFile("100 PRINT A(B+1)")
        myFixture.checkHighlighting(true, false, false)
    }

    // --- String-number mismatch tests ---

    fun testStringNumberMismatchNumericVarInNumericExpressionIsNotMismatch() {
        configureFile("100 PRINT A+B")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testStringNumberMismatchStringLiteralInNumericExpression() {
        configureFile(
            "100 PRINT A+<error descr=\"String-number mismatch\">\"hello\"</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testStringNumberMismatchStringVarAfterNumericExpression() {
        configureFile(
            "100 PRINT 1<error descr=\"Separator expected between expressions\">A$</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testStringNumberMismatchConcatOpAfterNumericExpression() {
        configureFile(
            "100 PRINT A<error descr=\"String-number mismatch\">&</error>B",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testStringNumberMismatchNumericLiteralAfterStringExpression() {
        configureFile(
            "100 PRINT \"hello\" <error descr=\"PRINT argument must be an expression\">&</error> 42",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForStringVariableComparedToStringLiteral() {
        configureFile("100 PRINT A$=\"HI!\"")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForStringVariablesComparedWithNotEqual() {
        configureFile("100 PRINT A$<>B$")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForParenthesizedStringConcatComparedToStringLiteral() {
        configureFile("100 PRINT (A$&B$)=\"HI!\"")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForBreakWithoutArgument() {
        configureFile("100 BREAK")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForUnbreakWithoutArgument() {
        configureFile("100 UNBREAK")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForTraceWithoutArgument() {
        configureFile("100 TRACE")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForUntraceWithoutArgument() {
        configureFile("100 UNTRACE")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForBreakWithSingleLineNumber() {
        configureFile("100 BREAK 200\n200 PRINT")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForBreakWithMultipleLineNumbers() {
        configureFile("100 BREAK 100,200,300\n200 PRINT\n300 PRINT")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForBreakWithMinLineNumber() {
        configureFile("1 PRINT\n100 BREAK 1")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForBreakWithMaxLineNumber() {
        configureFile("100 BREAK 32767\n32767 PRINT")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForTraceWithMultipleLineNumbers() {
        configureFile("100 TRACE 100,200\n200 PRINT")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForUntraceWithMultipleLineNumbers() {
        configureFile("100 UNTRACE 100,200\n200 PRINT")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForBreakWithLineNumberZero() {
        configureFile(
            "100 BREAK <error descr=\"Line number must be between 1 and 32767\">0</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForBreakWithLineNumberAboveMax() {
        configureFile(
            "100 BREAK <error descr=\"Line number must be between 1 and 32767\">32768</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForBreakWithNonNumericArgument() {
        configureFile(
            "100 BREAK <error descr=\"Line number expected\">A</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForBreakWithLeadingComma() {
        configureFile(
            "100 BREAK <error descr=\"Line number expected\">,</error>200",
        )
        myFixture.checkHighlighting(false, false, false)
    }

    fun testErrorForBreakWithTrailingComma() {
        configureFile(
            "100 BREAK 200<error descr=\"Line number expected\">,</error>",
        )
        myFixture.checkHighlighting(false, false, false)
    }

    fun testErrorForBreakWithConsecutiveCommas() {
        configureFile(
            "100 BREAK 200,<error descr=\"Line number expected\">,</error>",
        )
        myFixture.checkHighlighting(false, false, false)
    }

    fun testErrorForBreakWithMissingComma() {
        configureFile(
            "100 BREAK 200 <error descr=\"Comma expected\">300</error>",
        )
        myFixture.checkHighlighting(false, false, false)
    }

    fun testWarningForBreakWithUndefinedLineNumber() {
        configureFile(
            "100 BREAK <warning descr=\"Bad line number\">200</warning>",
        )
        myFixture.checkHighlighting(true, false, true)
    }

    fun testNoWarningForBreakWithDefinedLineNumber() {
        configureFile("100 BREAK 200\n200 PRINT")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testWarningOnlyForUndefinedLineNumbersInList() {
        configureFile(
            "100 BREAK 200,<warning descr=\"Bad line number\">300</warning>\n200 PRINT",
        )
        myFixture.checkHighlighting(true, false, true)
    }

    fun testWarningForTraceWithUndefinedLineNumber() {
        configureFile(
            "100 TRACE <warning descr=\"Bad line number\">500</warning>",
        )
        myFixture.checkHighlighting(true, false, true)
    }

    fun testNoErrorForDeleteWithEmptyStringLiteral() {
        configureFile("100 DELETE \"\"")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForDeleteWithStringVariable() {
        configureFile("100 DELETE A$")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForDeleteWithStringConcatenation() {
        configureFile("100 DELETE A$&B$")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForDeleteWithParenthesizedConcatenation() {
        configureFile("100 DELETE (A$&B$)")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForDeleteWithParenthesizedStringLiteral() {
        configureFile("100 DELETE (\"CS1\")")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForDeleteWithStringLiteral() {
        configureFile("100 DELETE \"DSK1.STAR\"")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForDeleteWithoutArgument() {
        configureFile(
            "100 <error descr=\"String expression expected\">DELETE</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForDeleteWithNumericLiteral() {
        configureFile(
            "100 DELETE <error descr=\"String expression expected\">42</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForDeleteWithNumericVariable() {
        configureFile(
            "100 DELETE <error descr=\"String expression expected\">X</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForExplicitLetWithNumericAssignment() {
        configureFile("100 LET A = 5")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForImplicitLetWithNumericAssignment() {
        configureFile("100 A = 5")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForImplicitLetWithStringAssignment() {
        configureFile("100 A$ = \"hello\"")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForImplicitLetWithComparisonAssignment() {
        configureFile("100 A = 1<2")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForImplicitLetWithUnaryPlusAssignmentToAtVariable() {
        configureFile("10 @1=+1")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForImplicitLetWithNestedArraySubscriptExpression() {
        configureFile("1100 C(D(1),2)=1")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForImplicitLetWithMalformedNestedArraySubscriptExpression() {
        configureFile("1100 C(D(,2))=1")
        val annotations = myFixture.doHighlighting()
        assertTrue(annotations.any { it.description == "Bad subscript definition" })
    }

    fun testErrorForStringExpressionAssignedToNumericVariable() {
        configureFile(
            "100 <error descr=\"String-number mismatch\">A = \"hello\"</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForNumericExpressionAssignedToStringVariable() {
        configureFile(
            "100 <error descr=\"String-number mismatch\">A$ = 5</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForStringExpressionAssignedToNumericVariableWithLetKeyword() {
        configureFile(
            "100 LET <error descr=\"String-number mismatch\">A = \"hello\"</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForImplicitLetWithTrailingTokens() {
        configureFile(
            "190 <error descr=\"Incorrect statement\">p = i n t</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForImplicitLetWithInvalidRhsCharacter() {
        configureFile(
            "160 <error descr=\"Incorrect statement\">XDIR=!</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForImplicitLetWithLoneUnaryPlus() {
        configureFile(
            "160 XDIR=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">+</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForImplicitLetWithLoneUnaryMinus() {
        configureFile(
            "160 XDIR=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">-</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForExplicitLetWithLoneUnaryPlus() {
        configureFile(
            "160 LET XDIR=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">+</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForImplicitLetWithMissingRhsExpression() {
        configureFile(
            "790 <error descr=\"Incorrect statement\">T1=</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForImplicitLetWithIncompleteComparisonRhs() {
        configureFile(
            "520 <error descr=\"Incorrect statement\">MIN=<</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForExplicitLetWithMissingRhsExpression() {
        configureFile(
            "790 <error descr=\"Incorrect statement\">LET T1=</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForImplicitLetWithUnaryPlusOperand() {
        configureFile("160 XDIR=+5")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForExplicitLetWithUnaryPlusOperand() {
        configureFile("160 LET XDIR=+5")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForUnaryMinusWithOperand() {
        configureFile("160 XDIR=-5")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForEndStatement() {
        configureFile("100 END")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForEndAsLastLine() {
        configureFile("100 PRINT \"OK\"\n200 END")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForMultipleEndStatements() {
        configureFile("100 END\n200 END")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForStopStatement() {
        configureFile("100 STOP")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForStopInMiddleOfProgram() {
        configureFile("100 PRINT \"A\"\n200 STOP\n300 PRINT \"B\"")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testWarningForTrailingContentAfterEnd() {
        configureFile(
            "100 END<warning descr=\"Everything after END statement is ignored\">=5</warning>",
        )
        myFixture.checkHighlighting(true, false, true)
    }

    fun testWarningForTrailingContentAfterEndWithSpace() {
        configureFile(
            "100 END <warning descr=\"Everything after END statement is ignored\">X</warning>",
        )
        myFixture.checkHighlighting(true, false, true)
    }

    fun testWarningForTrailingContentAfterStop() {
        configureFile(
            "100 STOP<warning descr=\"Everything after STOP statement is ignored\">*3</warning>",
        )
        myFixture.checkHighlighting(true, false, true)
    }

    fun testWarningForTrailingContentAfterStopWithSpace() {
        configureFile(
            "100 STOP <warning descr=\"Everything after STOP statement is ignored\">A</warning>",
        )
        myFixture.checkHighlighting(true, false, true)
    }

    fun testNoErrorForGotoWithExistingLineNumber() {
        configureFile("100 GOTO 200\n200 PRINT \"OK\"")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForGoToTwoWordsWithExistingLineNumber() {
        configureFile("100 GO TO 200\n200 PRINT \"OK\"")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testWarningForGotoWithUndefinedLineNumber() {
        configureFile(
            "100 GOTO <warning descr=\"Bad line number\">999</warning>",
        )
        myFixture.checkHighlighting(true, false, true)
    }

    fun testWarningForGoToTwoWordsWithUndefinedLineNumber() {
        configureFile(
            "100 GO TO <warning descr=\"Bad line number\">999</warning>",
        )
        myFixture.checkHighlighting(true, false, true)
    }

    fun testErrorForGotoWithLineNumberZero() {
        configureFile(
            "100 GOTO <error descr=\"Bad line number\">0</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForGotoWithLineNumberTooLarge() {
        configureFile(
            "100 GOTO <error descr=\"Bad line number\">99999</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForGotoWithoutLineNumber() {
        configureFile(
            "100 <error descr=\"Incorrect statement\">GOTO</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForGotoWithNonNumericArgument() {
        configureFile(
            "100 <error descr=\"Incorrect statement\">GOTO ABC</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForGotoWithMultipleLineNumbers() {
        configureFile(
            "100 <error descr=\"Incorrect statement\">GOTO 200 300</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForGoWithoutTo() {
        configureFile(
            "100 <error descr=\"Incorrect statement\">GO 200</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForOnGotoWithExistingLineNumber() {
        configureFile(
            "100 ON X GOTO 200\n200 PRINT \"OK\"",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForOnGoToTwoWordsWithExistingLineNumber() {
        configureFile(
            "100 ON X GO TO 200\n200 PRINT \"OK\"",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForOnGotoWithMultipleExistingLineNumbers() {
        configureFile(
            "100 ON X GOTO 200,300,400\n200 PRINT \"A\"\n300 PRINT \"B\"\n400 PRINT \"C\"",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testWarningForOnGotoWithUndefinedLineNumber() {
        configureFile(
            "100 ON X GOTO <warning descr=\"Bad line number\">200</warning>,300\n300 PRINT \"OK\"",
        )
        myFixture.checkHighlighting(true, false, true)
    }

    fun testWarningForEachUndefinedLineNumberInOnGoto() {
        configureFile(
            "100 ON X GOTO <warning descr=\"Bad line number\">200</warning>,<warning descr=\"Bad line number\">300</warning>",
        )
        myFixture.checkHighlighting(true, false, true)
    }

    fun testErrorForOnGotoWithLineNumberZero() {
        configureFile(
            "100 ON X GOTO <error descr=\"Bad line number\">0</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForOnGotoWithLineNumberTooLarge() {
        configureFile(
            "100 ON X GOTO <error descr=\"Bad line number\">32768</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForOnGotoWithoutExpression() {
        configureFile(
            "100 <error descr=\"Incorrect statement\">ON GOTO 200</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForOnGotoWithoutGotoKeyword() {
        configureFile(
            "100 <error descr=\"Incorrect statement\">ON X 200</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForOnGotoWithoutLineNumbers() {
        configureFile(
            "100 <error descr=\"Incorrect statement\">ON X GOTO</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForOnGotoWithStringExpression() {
        configureFile(
            "100 ON <error descr=\"String-number mismatch\">A$</error> GOTO 200\n200 PRINT \"OK\"",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForOnGotoWithTrailingComma() {
        configureFile(
            "100 <error descr=\"Incorrect statement\">ON X GOTO ,</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForValidIfThen() {
        configureFile("100 IF X>0 THEN 200\n200 PRINT \"OK\"")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForValidIfThenElse() {
        configureFile("100 IF X>0 THEN 200 ELSE 300\n200 PRINT \"A\"\n300 PRINT \"B\"")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testWarningForIfThenWithUndefinedLineNumber() {
        configureFile(
            "100 IF X>0 THEN <warning descr=\"Bad line number\">999</warning>",
        )
        myFixture.checkHighlighting(false, false, true)
    }

    fun testWarningForIfThenElseWithUndefinedElseLineNumber() {
        configureFile(
            "100 IF X>0 THEN 200 ELSE <warning descr=\"Bad line number\">999</warning>\n200 PRINT \"OK\"",
        )
        myFixture.checkHighlighting(false, false, true)
    }

    fun testErrorForIfWithoutThen() {
        configureFile(
            "100 <error descr=\"Incorrect statement\">IF X>0</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForIfWithInvalidOperatorSequence() {
        configureFile(
            "100 <error descr=\"Incorrect statement\">IF C><120 THEN 2320</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForIfWithNotEqualOperator() {
        configureFile("100 IF C<>120 THEN 200\n200 PRINT \"OK\"")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForIfThenWithoutLineNumber() {
        configureFile(
            "100 <error descr=\"Incorrect statement\">IF X>0 THEN</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForIfThenWithElseButNoElseLineNumber() {
        configureFile(
            "100 <error descr=\"Incorrect statement\">IF X>0 THEN 200 ELSE</error>\n200 PRINT \"OK\"",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForIfWithStringExpression() {
        configureFile(
            "100 IF <error descr=\"String-number mismatch\">A$</error> THEN 200\n200 PRINT \"OK\"",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForIfWithStringOperandInArithmeticCondition() {
        configureFile(
            "950 IF (C$(I,2)=\"BUBE\")*(<error descr=\"String-number mismatch\">Q$</error>)THEN 960 ELSE 1230\n" +
                    "960 PRINT \"OK\"\n" +
                    "1230 PRINT \"END\"",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForIfThenWithInvalidLineNumber() {
        configureFile(
            "100 IF X>0 THEN <error descr=\"Bad line number\">99999</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForIfThenElseWithInvalidElseLineNumber() {
        configureFile(
            "100 IF X>0 THEN 200 ELSE <error descr=\"Bad line number\">99999</error>\n200 PRINT \"OK\"",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testWarningForIfThenElseWithUndefinedThenLineNumber() {
        configureFile(
            "100 IF X>0 THEN <warning descr=\"Bad line number\">999</warning> ELSE 200\n200 PRINT \"OK\"",
        )
        myFixture.checkHighlighting(false, false, true)
    }

    fun testNoErrorForValidFor() {
        configureFile("100 FOR I = 1 TO 10\n200 NEXT I")
        val annotations = myFixture.doHighlighting()
        assertTrue(annotations.none { it.description != null && it.description.startsWith("FOR-NEXT") })
    }

    fun testNoErrorForValidForWithStep() {
        configureFile("100 FOR I = 1 TO 10 STEP 2\n200 NEXT I")
        val annotations = myFixture.doHighlighting()
        assertTrue(annotations.none { it.description != null && it.description.startsWith("FOR-NEXT") })
    }

    fun testErrorForForWithStringControlVariable() {
        configureFile("100 FOR A$ = 1 TO 10\n200 NEXT A$")
        val annotations = myFixture.doHighlighting()
        assertTrue(annotations.any { it.description == "Numeric variable expected" })
    }

    fun testErrorForNextWithStringControlVariable() {
        configureFile("100 FOR I = 1 TO 10\n200 NEXT A$")
        val annotations = myFixture.doHighlighting()
        assertTrue(annotations.any { it.description == "Numeric variable expected" })
    }

    fun testErrorForForWithStringInitialValue() {
        configureFile("100 FOR I = A$ TO 10\n200 NEXT I")
        val annotations = myFixture.doHighlighting()
        assertTrue(annotations.any { it.description == "String-number mismatch" })
    }

    fun testErrorForForWithStringLimit() {
        configureFile("100 FOR I = 1 TO A$\n200 NEXT I")
        val annotations = myFixture.doHighlighting()
        assertTrue(annotations.any { it.description == "String-number mismatch" })
    }

    fun testErrorForForWithStringStep() {
        configureFile("100 FOR I = 1 TO 10 STEP A$\n200 NEXT I")
        val annotations = myFixture.doHighlighting()
        assertTrue(annotations.any { it.description == "String-number mismatch" })
    }

    fun testErrorForForMissingTo() {
        configureFile("100 FOR I = 1 10\n200 NEXT I")
        val annotations = myFixture.doHighlighting()
        assertTrue(annotations.any { it.description == "Incorrect statement" })
    }

    fun testErrorForForMissingEquals() {
        configureFile("100 FOR I 1 TO 10\n200 NEXT I")
        val annotations = myFixture.doHighlighting()
        assertTrue(annotations.any { it.description == "Incorrect statement" })
    }

    fun testErrorForNextWithoutVariable() {
        configureFile("100 FOR I = 1 TO 10\n200 NEXT")
        val annotations = myFixture.doHighlighting()
        assertTrue(annotations.any { it.description == "Incorrect statement" })
    }

    fun testWarningForMoreForsThanNexts() {
        configureFile("100 FOR I = 1 TO 10\n200 FOR J = 1 TO 5\n300 NEXT I")
        val annotations = myFixture.doHighlighting()
        assertTrue(annotations.any { it.description == "FOR-NEXT mismatch for J: 1 FOR, 0 NEXT" })
        assertTrue(annotations.none { it.description != null && it.description.startsWith("FOR-NEXT mismatch for I") })
    }

    fun testWarningForMoreNextsThanFors() {
        configureFile("100 FOR I = 1 TO 10\n200 NEXT I\n300 NEXT I")
        val annotations = myFixture.doHighlighting()
        val forNextWarnings = annotations.filter { it.description == "FOR-NEXT mismatch for I: 1 FOR, 2 NEXT" }
        assertEquals(3, forNextWarnings.size)
    }

    fun testBalanceWarningOnSurplusForNotFirst() {
        configureFile("100 FOR I = 1 TO 5\n200 FOR J = 1 TO 5\n300 NEXT I")
        val annotations = myFixture.doHighlighting()
        val balanceWarnings = annotations.filter {
            it.description == "FOR-NEXT mismatch for J: 1 FOR, 0 NEXT"
        }
        assertEquals(1, balanceWarnings.size)
        assertTrue(balanceWarnings[0].startOffset > 0)
    }

    fun testNoBalanceWarningForBalancedForNext() {
        configureFile("100 FOR I = 1 TO 10\n200 FOR J = 1 TO 5\n300 NEXT J\n400 NEXT I")
        val annotations = myFixture.doHighlighting()
        assertTrue(annotations.none { it.description != null && it.description.startsWith("FOR-NEXT") })
    }

    fun testPerVariableOnlyUnbalancedVariableIsWarned() {
        configureFile("100 FOR A = 1 TO 5\n200 NEXT A\n300 FOR B = 1 TO 3\n400 NEXT B\n500 FOR C = 1 TO 2")
        val annotations = myFixture.doHighlighting()
        assertTrue(annotations.none { it.description != null && it.description.startsWith("FOR-NEXT mismatch for A") })
        assertTrue(annotations.none { it.description != null && it.description.startsWith("FOR-NEXT mismatch for B") })
        assertTrue(annotations.any { it.description == "FOR-NEXT mismatch for C: 1 FOR, 0 NEXT" })
    }

    fun testCompensatingVariablesBothWarned() {
        configureFile("100 FOR I = 1 TO 10\n200 FOR I = 1 TO 5\n300 NEXT J\n400 NEXT J")
        val annotations = myFixture.doHighlighting()
        val iWarnings = annotations.filter { it.description == "FOR-NEXT mismatch for I: 2 FOR, 0 NEXT" }
        val jWarnings = annotations.filter { it.description == "FOR-NEXT mismatch for J: 0 FOR, 2 NEXT" }
        assertEquals(2, iWarnings.size)
        assertEquals(2, jWarnings.size)
    }

    fun testNextOnlyVariableIsWarned() {
        configureFile("100 FOR I = 1 TO 10\n200 NEXT I\n300 NEXT K")
        val annotations = myFixture.doHighlighting()
        assertTrue(annotations.any { it.description == "FOR-NEXT mismatch for K: 0 FOR, 1 NEXT" })
        assertTrue(annotations.none { it.description != null && it.description.startsWith("FOR-NEXT mismatch for I") })
    }

    fun testForNextMismatchAllStatementsMarked() {
        configureFile("100 FOR I = 1 TO 10\n200 NEXT I\n300 NEXT I")
        val annotations = myFixture.doHighlighting()
        val forNextWarnings = annotations.filter { it.description == "FOR-NEXT mismatch for I: 1 FOR, 2 NEXT" }
        assertEquals(3, forNextWarnings.size)
        val offsets = forNextWarnings.map { it.startOffset }.sorted()
        assertTrue(offsets[0] < offsets[1])
        assertTrue(offsets[1] < offsets[2])
    }

    fun testForWithLeadingDotValuesNoError() {
        configureFile("110 FOR X= .1 TO 1 STEP .2\n120 NEXT X")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testErrorForForWithTrailingClosingParenthesis() {
        configureFile("130 <error descr=\"Incorrect statement\">FOR S=2 TO 16)</error>\n140 NEXT S")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testInputWithValidVariablesNoError() {
        configureFile("100 INPUT A,B$")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testInputWithNoVariablesIsError() {
        configureFile(
            "100 <error descr=\"Incorrect statement\">INPUT</error>"
        )
        myFixture.checkHighlighting(true, false, true)
    }

    fun testInputWithInvalidVariableNameIsError() {
        configureFile(
            "100 INPUT <error descr=\"Bad variable name\">AVERYLONGNAMES1$</error>"
        )
        myFixture.checkHighlighting(true, false, true)
    }

    fun testInputWithStringPromptNoError() {
        configureFile("100 INPUT \"Enter value\": A")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testInputWithNumericPromptResultsInIncorrectStatement() {
        configureFile(
            "100 <error descr=\"Incorrect statement\">INPUT 42: A</error>"
        )
        myFixture.checkHighlighting(true, false, true)
    }

    fun testInputWithInvalidCharAfterVariableGivesIncorrectStatement() {
        configureFile("241 <error descr=\"Incorrect statement\">INPUT A §</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testInputWithInvalidCharDirectlyAfterVariableNameGivesIncorrectStatement() {
        configureFile("241 <error descr=\"Incorrect statement\">INPUT A§</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testFileInputMinimalNoError() {
        configureFile("100 INPUT #1:A")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testFileInputWithMultipleVariablesNoError() {
        configureFile("100 INPUT #2:A,B$,C")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testFileInputWithTrailingCommaNoError() {
        configureFile("100 INPUT #1:A,B,")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testFileInputWithRecordNumberNoError() {
        configureFile("100 INPUT #1.REC 5:A")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testFileInputWithExpressionFileNumberNoError() {
        configureFile("100 INPUT #X+5:A")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testFileInputWithRecordNumberExpressionNoError() {
        configureFile("100 INPUT #1.REC N*2:A,B$")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testFileInputMissingColonIsError() {
        configureFile("100 <error descr=\"Incorrect statement\">INPUT #1 A</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testFileInputMissingVariablesIsError() {
        configureFile("100 <error descr=\"Incorrect statement\">INPUT #1:</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testFileInputFileNumberZeroIsError() {
        configureFile("100 INPUT #<error descr=\"File number 0 is reserved for screen\">0</error>:A")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testFileInputFileNumberOutOfRangeIsError() {
        configureFile("100 INPUT #<error descr=\"File number must be between 1 and 255\">256</error>:A")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testFileInputStringExpressionAsFileNumberIsError() {
        configureFile("100 INPUT #<error descr=\"Numeric expression expected\">A$</error>:B")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testFileInputStringExpressionAsRecordNumberIsError() {
        configureFile("100 INPUT #1.REC <error descr=\"Numeric expression expected\">A$</error>:B")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testFileInputDotWithoutRecIsError() {
        configureFile("100 <error descr=\"Incorrect statement\">INPUT #1.FOO:A</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testFileInputWithInvalidVariableNameIsError() {
        configureFile("100 INPUT #1:<error descr=\"Bad variable name\">AVERYLONGNAMES1$</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testFilePrintMinimalNoError() {
        configureFile("100 PRINT #1:")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testFilePrintWithStringLiteralNoError() {
        configureFile("100 PRINT #1:\"hello\"")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testFilePrintWithVariablesNoError() {
        configureFile("100 PRINT #1:A;B$")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testFilePrintWithRecordNumberNoError() {
        configureFile("100 PRINT #2.REC 1:\"hello\"")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testFilePrintWithMaxFileNumberNoError() {
        configureFile("100 PRINT #255:")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testFilePrintFileNumberZeroIsError() {
        configureFile("100 PRINT #<error descr=\"File number 0 is reserved for screen\">0</error>:\"x\"")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testFilePrintFileNumberOutOfRangeIsError() {
        configureFile("100 PRINT #<error descr=\"File number must be between 1 and 255\">256</error>:\"x\"")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testFilePrintStringExpressionAsFileNumberIsError() {
        configureFile("100 PRINT #<error descr=\"Numeric expression expected\">A$</error>:\"x\"")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testFilePrintStringExpressionAsRecordNumberIsError() {
        configureFile("100 PRINT #1.REC <error descr=\"Numeric expression expected\">A$</error>:\"x\"")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testFilePrintDotWithoutRecIsError() {
        configureFile("100 <error descr=\"Incorrect statement\">PRINT #1.FOO:\"x\"</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testFilePrintMissingColonIsError() {
        configureFile("100 <error descr=\"Incorrect statement\">PRINT #1</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testFilePrintMissingRecordNumberIsError() {
        configureFile("100 <error descr=\"Incorrect statement\">PRINT #1.REC</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testFilePrintTrailingTokensAfterRecordNumberIsError() {
        configureFile("100 <error descr=\"Incorrect statement\">PRINT #1.REC 3(47+1):A;B;C;E;F</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testFilePrintTrailingTokensAfterFileNumberIsError() {
        configureFile("100 <error descr=\"Incorrect statement\">PRINT #1(2):A</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testReadWithValidVariablesNoError() {
        configureFile("100 READ A,B$,C")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testReadWithTrailingCommaIsError() {
        configureFile("2050 READ DD<error descr=\"Incorrect statement\">,</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testReadWithNoVariablesIsError() {
        configureFile(
            "100 <error descr=\"Incorrect statement\">READ</error>"
        )
        myFixture.checkHighlighting(true, false, true)
    }

    fun testReadWithInvalidVariableNameIsError() {
        configureFile(
            "100 READ <error descr=\"Bad variable name\">AVERYLONGNAMES1$</error>"
        )
        myFixture.checkHighlighting(true, false, true)
    }

    fun testDataWithNumericItemNoError() {
        configureFile("100 DATA 42")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testDataWithStringLiteralNoError() {
        configureFile("100 DATA \"hello\"")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testDataWithUnquotedStringNoError() {
        configureFile("100 DATA world")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testDataWithMixedItemsNoError() {
        configureFile("100 DATA 1,\"two\",three")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testDataWithConsecutiveCommasNoError() {
        configureFile("100 DATA 1,,3")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testDataWithNoItemsNoError() {
        configureFile("100 DATA")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testRestoreWithNoArgumentNoError() {
        configureFile("100 RESTORE")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testRestoreWithDefinedLineNumberNoError() {
        configureFile("100 RESTORE 200\n200 DATA 1,2,3")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testRestoreWithUndefinedLineNumberIsWarning() {
        configureFile(
            "100 RESTORE <warning descr=\"Bad line number\">999</warning>",
        )
        myFixture.checkHighlighting(true, false, true)
    }

    fun testRestoreWithOutOfRangeLineNumberIsError() {
        configureFile(
            "100 RESTORE <error descr=\"Bad line number\">99999</error>",
        )
        myFixture.checkHighlighting(true, false, true)
    }

    fun testRestoreWithNonNumericArgIsError() {
        configureFile(
            "100 <error descr=\"Incorrect statement\">RESTORE A</error>",
        )
        myFixture.checkHighlighting(true, false, true)
    }

    fun testRestoreWithMultipleNumbersIsError() {
        configureFile(
            "100 <error descr=\"Incorrect statement\">RESTORE 100 200</error>",
        )
        myFixture.checkHighlighting(true, false, true)
    }

    fun testRestoreFileVariantNoError() {
        configureFile("100 RESTORE #1")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testRestoreFileVariantWithRecordNumberNoError() {
        configureFile("100 RESTORE #2,REC 5")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testRestoreFileVariantBoundaryFileNumberNoError() {
        configureFile("100 RESTORE #255")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testRestoreFileVariantFileNumberZeroIsError() {
        configureFile("100 RESTORE #<error descr=\"File number 0 is reserved for screen\">0</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testRestoreFileVariantFileNumberTooLargeIsError() {
        configureFile("100 RESTORE #<error descr=\"File number must be between 1 and 255\">256</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testRestoreFileVariantStringFileNumberIsError() {
        configureFile("100 RESTORE #<error descr=\"Numeric expression expected\">\"A\"</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testRestoreFileVariantStringRecordNumberIsError() {
        configureFile("100 RESTORE #1,REC <error descr=\"Numeric expression expected\">\"A\"</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testRestoreFileVariantCommaWithoutRecIsError() {
        configureFile("100 <error descr=\"Incorrect statement\">RESTORE #1,5</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testRestoreFileVariantRecWithoutRecordNumberIsError() {
        configureFile("100 <error descr=\"Incorrect statement\">RESTORE #1,REC</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testRestoreFileVariantTrailingTokensIsError() {
        configureFile("100 <error descr=\"Incorrect statement\">RESTORE #X+1,REC 3(47+1)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testNoErrorForTabWithNumericLiteralArgument() {
        configureFile("100 PRINT TAB(5);\"TEXT\"")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testNoErrorForTabAloneInPrint() {
        configureFile("100 PRINT TAB(5)")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testNoErrorForMultipleTabsWithSeparators() {
        configureFile("100 PRINT TAB(5);\"A\";TAB(10);\"B\"")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testNoErrorForTabWithVariableArgument() {
        configureFile("100 PRINT TAB(N)")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testErrorForTabWithoutParentheses() {
        configureFile("100 PRINT <error descr=\"TAB requires a numeric argument in parentheses\">TAB</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testErrorForTabWithEmptyParentheses() {
        configureFile("100 PRINT <error descr=\"TAB requires a numeric argument\">TAB()</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testErrorForTabOutsidePrintInLetStatement() {
        configureFile("100 LET X = <error descr=\"TAB is only valid in a PRINT or DISPLAY statement\">TAB</error>(5)")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testNoErrorForTabAfterSeparatorInPrint() {
        configureFile("100 PRINT ;TAB(5);\"X\"")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testErrorForTwoTabsWithoutSeparator() {
        configureFile("100 PRINT TAB(5)<error descr=\"Separator expected between expressions\">TAB(10)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testNoErrorForDisplayWithStringLiteral() {
        configureFile("100 DISPLAY \"HELLO\"")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testNoErrorForDisplayWithNoArgument() {
        configureFile("100 DISPLAY")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testNoErrorForDisplayWithTabFunction() {
        configureFile("100 DISPLAY TAB(5);\"TEXT\"")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testErrorForDisplayWithTwoExpressionsWithoutSeparator() {
        configureFile("100 DISPLAY TAB(5)<error descr=\"Separator expected between expressions\">TAB(10)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun testErrorForTabOutsidePrintOrDisplay() {
        configureFile("100 LET X = <error descr=\"TAB is only valid in a PRINT or DISPLAY statement\">TAB</error>(5)")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test RANDOMIZE without argument no error`() {
        configureFile("100 RANDOMIZE")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test RANDOMIZE with numeric literal no error`() {
        configureFile("100 RANDOMIZE 42")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test RANDOMIZE with numeric expression no error`() {
        configureFile("100 RANDOMIZE A+B*3")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test RANDOMIZE with string argument gives INCORRECT STATEMENT error`() {
        configureFile("100 <error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">RANDOMIZE \"HELLO\"</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test RANDOMIZE with unbalanced paren gives INCORRECT STATEMENT error`() {
        configureFile("100 RANDOMIZE <error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">(A+B</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test RANDOMIZE with trailing comma and extra content gives INCORRECT STATEMENT error`() {
        configureFile("155 <error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">RANDOMIZE 6/6+123, dsaas</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test GOSUB with valid defined target no error`() {
        configureFile("100 GOSUB 200\n200 PRINT \"OK\"\n300 RETURN")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test GO SUB with valid defined target no error`() {
        configureFile("100 GO SUB 200\n200 PRINT \"OK\"\n300 RETURN")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test GOSUB with undefined target gives warning`() {
        configureFile("100 GOSUB <warning descr=\"Bad line number\">999</warning>\n200 RETURN")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test GOSUB with out-of-range line number gives error`() {
        configureFile("100 GOSUB <error descr=\"Bad line number\">0</error>\n200 RETURN")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test GOSUB without line number gives Incorrect statement error`() {
        configureFile("100 <error descr=\"Incorrect statement\">GOSUB</error>\n200 RETURN")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test RETURN no error when used after GOSUB`() {
        configureFile("100 GOSUB 200\n200 PRINT \"OK\"\n300 RETURN")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test RETURN lowercase no error`() {
        configureFile("100 GOSUB 200\n200 PRINT \"OK\"\n300 return")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test RETURN with trailing content gives warning`() {
        configureFile("100 <warning descr=\"Everything after RETURN statement is ignored\">RETURN EXTRA</warning>")
        myFixture.checkHighlighting(false, false, true)
    }

    fun `test ON GOSUB with valid defined targets no error`() {
        configureFile("100 ON X GOSUB 200,300\n200 PRINT \"A\"\n300 PRINT \"B\"\n400 RETURN")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test ON GO SUB with valid defined targets no error`() {
        configureFile("100 ON X GO SUB 200,300\n200 PRINT \"A\"\n300 PRINT \"B\"\n400 RETURN")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test ON GOSUB with undefined target gives warning`() {
        configureFile("100 ON X GOSUB <warning descr=\"Bad line number\">999</warning>\n200 RETURN")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test ON GOSUB with out-of-range line number gives error`() {
        configureFile("100 ON X GOSUB <error descr=\"Bad line number\">0</error>\n200 RETURN")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test ON GOSUB with string expression gives string-number mismatch error`() {
        configureFile("100 ON <error descr=\"String-number mismatch\">X$</error> GOSUB 200\n200 RETURN")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test ON GOSUB missing line number list gives Incorrect statement error`() {
        configureFile("100 <error descr=\"Incorrect statement\">ON X GOSUB</error>\n200 RETURN")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test ON GOSUB with trailing comma gives Bad line number error`() {
        configureFile("100 ON X GOSUB 200,<error descr=\"Bad line number\">,</error>\n200 RETURN")
        myFixture.checkHighlighting(true, false, false)
    }
}
