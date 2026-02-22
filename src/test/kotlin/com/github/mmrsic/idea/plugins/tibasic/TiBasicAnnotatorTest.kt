package com.github.mmrsic.idea.plugins.tibasic

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TiBasicAnnotatorTest : BasePlatformTestCase() {

    fun testNoWarningForAscendingLineNumbers() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"A\"\n200 PRINT \"B\"\n300 PRINT \"C\"")
        myFixture.checkHighlighting(false, false, true)
    }

    fun testWarningForEqualLineNumber() {
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT \"A\"\n<error descr=\"Duplicate line number 100\">100 PRINT \"B\"</error>"
        )
        myFixture.checkHighlighting(true, false, true)
    }

    fun testWarningForDescendingLineNumber() {
        myFixture.configureByText(
            "test.tibasic",
            "200 PRINT \"A\"\n<warning descr=\"Line number 100 does not follow ascending order (previous: 200)\">100 PRINT \"B\"</warning>"
        )
        myFixture.checkHighlighting(false, false, true)
    }

    fun testWarningOnlyForNonAscendingLineInSequence() {
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT \"A\"\n200 PRINT \"B\"\n<warning descr=\"Line number 150 does not follow ascending order (previous: 200)\">150 PRINT \"C\"</warning>"
        )
        myFixture.checkHighlighting(false, false, true)
    }

    fun testNoWarningForSingleLine() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"A\"")
        myFixture.checkHighlighting(false, false, true)
    }

    fun testErrorForDuplicateLineNumber() {
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT \"A\"\n<error descr=\"Duplicate line number 100\">100 PRINT \"B\"</error>"
        )
        myFixture.checkHighlighting(true, false, true)
    }

    fun testErrorForTriplicateLineNumber() {
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT \"A\"\n<error descr=\"Duplicate line number 100\">100 PRINT \"B\"</error>\n<error descr=\"Duplicate line number 100\">100 PRINT \"C\"</error>"
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorAndWarningForDuplicateNonAscendingLineNumber() {
        myFixture.configureByText(
            "test.tibasic",
            "200 PRINT \"A\"\n<error descr=\"Duplicate line number 200\">200 PRINT \"B\"</error>"
        )
        myFixture.checkHighlighting(true, false, true)
    }

    fun testNoErrorForStringLiteralPrintArgument() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"hello\"")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForStringWithEscapedQuote() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"say \"\"hi\"\"\"")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForEmptyStringLiteral() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"\"")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForPrintWithoutArgument() {
        myFixture.configureByText("test.tibasic", "100 PRINT")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForConcatenationOfTwoStringLiterals() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"hello\" & \"world\"")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForConcatenationOfThreeStringLiterals() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"a\" & \"b\" & \"c\"")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForTrailingConcatOpWithNoRightOperand() {
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT \"a\" <error descr=\"PRINT argument must be an expression\">&</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForLeadingConcatOpWithNoLeftOperand() {
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT <error descr=\"PRINT argument must be an expression\">&</error> \"b\"",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForInvalidSeparatorBetweenStringLiterals() {
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT \"a\"<error descr=\"PRINT argument must be an expression\">;</error>" +
                    "<error descr=\"PRINT argument must be an expression\">\"b\"</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForNumericLiteralPrintArgument() {
        myFixture.configureByText("test.tibasic", "100 PRINT 42")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForConcatWithNonStringRightOperand() {
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT \"a\" " +
                    "<error descr=\"PRINT argument must be an expression\">&</error> " +
                    "<error descr=\"String-Number-Mismatch\">42</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForStringVariable() {
        myFixture.configureByText("test.tibasic", "100 PRINT Y$")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForStringVariableArray() {
        myFixture.configureByText("test.tibasic", "100 PRINT A$(1)")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForStringVariableWithSpacesAroundSubscript() {
        myFixture.configureByText("test.tibasic", "100 PRINT A$ ( 1 , 2 )")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForDigitAsFirstCharInVariableName() {
        // 1 is a numeric literal, A$ is a string variable after numeric expr → String-Number-Mismatch
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT 1<error descr=\"String-Number-Mismatch\">A$</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForTooLongVariableName() {
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT <error descr=\"Bad variable name\">TOOLONGVARIABLE$</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForFourDimensionalSubscript() {
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT <error descr=\"Bad subscript definition\">A$(1,2,3,4)</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForEmptySubscript() {
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT <error descr=\"Bad subscript definition\">A$()</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoConflictForSameSimpleVariableUsedTwice() {
        myFixture.configureByText("test.tibasic", "100 PRINT Y$\n200 PRINT Y$")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoConflictForSameArrayUsedTwice() {
        myFixture.configureByText("test.tibasic", "100 PRINT A$(1)\n200 PRINT A$(2)")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoConflictForSameTwoDimensionalArrayUsedTwice() {
        myFixture.configureByText("test.tibasic", "100 PRINT A$(1,2)\n200 PRINT A$(3,4)")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testConflictBetweenSimpleVariableAndArray() {
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT <error descr=\"Name conflict\">A$</error>\n" +
                    "200 PRINT <error descr=\"Name conflict\">A$(1)</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testConflictBetweenArraysWithDifferentDimensions() {
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT <error descr=\"Name conflict\">A$(1)</error>\n" +
                    "200 PRINT <error descr=\"Name conflict\">A$(1,2)</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testConflictAnnotatedOnAllThreeOccurrences() {
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT <error descr=\"Name conflict\">A$</error>\n" +
                    "200 PRINT <error descr=\"Name conflict\">A$(1)</error>\n" +
                    "300 PRINT <error descr=\"Name conflict\">A$</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoConflictBetweenDifferentVariableNames() {
        myFixture.configureByText("test.tibasic", "100 PRINT A$\n200 PRINT B$(1)")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForLineNumberOne() {
        myFixture.configureByText("test.tibasic", "1 PRINT")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForLineNumberMax() {
        myFixture.configureByText("test.tibasic", "32767 PRINT")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForLineNumberWithLeadingZeros() {
        myFixture.configureByText("test.tibasic", "0100 PRINT")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForLineNumberWithManyLeadingZeros() {
        myFixture.configureByText("test.tibasic", "000000100 PRINT")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForLineNumberZero() {
        myFixture.configureByText("test.tibasic", "<error descr=\"Bad line number\">000</error> PRINT")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForLineNumberAboveMax() {
        myFixture.configureByText("test.tibasic", "<error descr=\"Bad line number\">32768</error> PRINT")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForVeryLargeLineNumber() {
        myFixture.configureByText("test.tibasic", "<error descr=\"Bad line number\">99999</error> PRINT")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testBadLineNumberDoesNotTriggerDuplicateError() {
        // Two lines with out-of-range numbers should not be flagged as duplicates of each other
        myFixture.configureByText(
            "test.tibasic",
            "<error descr=\"Bad line number\">32768</error> PRINT\n" +
                    "<error descr=\"Bad line number\">32769</error> PRINT",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForCommentLineWithValidLineNumber() {
        myFixture.configureByText("test.tibasic", "100 CALL CLEAR")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForCommentLineWithValidLineNumberAndLeadingZeros() {
        myFixture.configureByText("test.tibasic", "0100 CALL CLEAR")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForCommentLineWithLineNumberAboveMax() {
        myFixture.configureByText(
            "test.tibasic",
            "<error descr=\"Bad line number\">32768</error> CALL CLEAR",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForCommentLineWithLineNumberZero() {
        myFixture.configureByText(
            "test.tibasic",
            "<error descr=\"Bad line number\">0</error> CALL CLEAR",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForCommentLineWithNoLineNumber() {
        myFixture.configureByText(
            "test.tibasic",
            "<error descr=\"Bad line number\">this is not valid</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForCommentLineWithLeadingWhitespaceAndNoLineNumber() {
        myFixture.configureByText(
            "test.tibasic",
            "<error descr=\"Bad line number\">   NOT A LINE</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForNumericVariableInPrint() {
        myFixture.configureByText("test.tibasic", "100 PRINT A")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForNumericVariableWithMixedCase() {
        myFixture.configureByText("test.tibasic", "100 PRINT myVar")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForNumericVariableWithDigitsAndSpecialFirstChars() {
        myFixture.configureByText("test.tibasic", "100 PRINT _A1@B")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForNumericVariableWithHyphen() {
        // A-B is valid subtraction
        myFixture.configureByText("test.tibasic", "100 PRINT A-B")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForTokenStartingWithDigitNotNumericLiteral() {
        // 1 is a numeric literal expression; A is a trailing token
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT 1<error descr=\"PRINT argument must be an expression\">A</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForDollarSignOnlyInStringVariable() {
        // A$ is a valid string variable ($ only at end)
        myFixture.configureByText("test.tibasic", "100 PRINT A$")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForNumericVariableWithValidSubscript1D() {
        myFixture.configureByText("test.tibasic", "100 PRINT A(1)")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForNumericVariableWithValidSubscript2D() {
        myFixture.configureByText("test.tibasic", "100 PRINT A(1,2)")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForNumericVariableWithValidSubscript3D() {
        myFixture.configureByText("test.tibasic", "100 PRINT A(1,2,3)")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForNumericVariableWithSpacesAroundSubscript() {
        myFixture.configureByText("test.tibasic", "100 PRINT A ( 1 , 2 )")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForNumericVariableWithTooManySubscripts() {
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT <error descr=\"Bad subscript definition\">A(1,2,3,4)</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForNumericVariableWithEmptySubscript() {
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT <error descr=\"Bad subscript definition\">A()</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForNumericVariableTooLong() {
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT <error descr=\"Bad variable name\">ABCDEFGHIJKLMNOP</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForNumericVariableExactly15Chars() {
        myFixture.configureByText("test.tibasic", "100 PRINT ABCDEFGHIJKLMNO")
        myFixture.checkHighlighting(true, false, false)
    }

    // --- Numeric variable name conflict tests ---

    fun testNameConflictSimpleNumericAndArraySameName() {
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT <error descr=\"Name conflict\">A</error>\n" +
                    "200 PRINT <error descr=\"Name conflict\">A(1)</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNameConflictTwoNumericArraysDifferentDims() {
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT <error descr=\"Name conflict\">A(1)</error>\n" +
                    "200 PRINT <error descr=\"Name conflict\">A(1,2)</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoNameConflictTwoNumericSimpleVarsWithSameName() {
        myFixture.configureByText("test.tibasic", "100 PRINT A\n200 PRINT A")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoNameConflictTwoNumericArraysSameDimCount() {
        myFixture.configureByText("test.tibasic", "100 PRINT A(1)\n200 PRINT A(2)")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoNameConflictNumericAndStringVarSameName() {
        myFixture.configureByText("test.tibasic", "100 PRINT A\n200 PRINT A$")
        myFixture.checkHighlighting(true, false, false)
    }

    // --- Keyword conflict tests ---

    fun testErrorForNumericVariableMatchingKeyword() {
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT <error descr=\"Keyword cannot be used as variable name\">PRINT</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForStringVariableWithSameNameAsKeywordButDifferentSuffix() {
        myFixture.configureByText("test.tibasic", "100 PRINT PRINT$")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForNumericVariableMatchingKeywordCaseInsensitive() {
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT <error descr=\"Keyword cannot be used as variable name\">print</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForVariableWithKeywordAsPrefix() {
        myFixture.configureByText("test.tibasic", "100 PRINT PRINTER")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForVariableWithKeywordAsSuffix() {
        myFixture.configureByText("test.tibasic", "100 PRINT APRINT")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForNumericArrayMatchingKeyword() {
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT <error descr=\"Keyword cannot be used as variable name\">PRINT(1)</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    // --- Arithmetic operator tests ---

    fun testNoErrorForAddition() {
        myFixture.configureByText("test.tibasic", "100 PRINT A+B")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForSubtraction() {
        myFixture.configureByText("test.tibasic", "100 PRINT A-B")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForMultiplication() {
        myFixture.configureByText("test.tibasic", "100 PRINT A*B")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForDivision() {
        myFixture.configureByText("test.tibasic", "100 PRINT A/B")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForPower() {
        myFixture.configureByText("test.tibasic", "100 PRINT A^B")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForUnaryMinus() {
        myFixture.configureByText("test.tibasic", "100 PRINT -A")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForMultipleUnaryPrefixes() {
        myFixture.configureByText("test.tibasic", "100 PRINT +-+-1")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForParenthesizedExpression() {
        myFixture.configureByText("test.tibasic", "100 PRINT (A+B)")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForComplexArithmeticExpression() {
        myFixture.configureByText("test.tibasic", "100 PRINT A+B*C-D/E^F")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoErrorForSubscriptWithExpression() {
        myFixture.configureByText("test.tibasic", "100 PRINT A(B+1)")
        myFixture.checkHighlighting(true, false, false)
    }

    // --- String-Number-Mismatch tests ---

    fun testStringNumberMismatchNumericVarInNumericExpressionIsNotMismatch() {
        myFixture.configureByText("test.tibasic", "100 PRINT A+B")
        myFixture.checkHighlighting(true, false, false)
    }

    fun testStringNumberMismatchStringLiteralInNumericExpression() {
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT A+<error descr=\"String-Number-Mismatch\">\"hello\"</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testStringNumberMismatchStringVarAfterNumericExpression() {
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT 1<error descr=\"String-Number-Mismatch\">A$</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testStringNumberMismatchConcatOpAfterNumericExpression() {
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT A<error descr=\"String-Number-Mismatch\">&</error><error descr=\"PRINT argument must be an expression\">B</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testStringNumberMismatchNumericLiteralAfterStringExpression() {
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT \"hello\" <error descr=\"PRINT argument must be an expression\">&</error> <error descr=\"String-Number-Mismatch\">42</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }
}

