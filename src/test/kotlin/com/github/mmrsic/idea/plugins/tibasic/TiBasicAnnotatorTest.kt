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
                    "<error descr=\"PRINT argument must be an expression\">42</error>",
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
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT <error descr=\"Bad variable name\">1A\$</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForTooLongVariableName() {
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT <error descr=\"Bad variable name\">TOOLONGVARIABLE\$</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForFourDimensionalSubscript() {
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT <error descr=\"Bad subscript definition\">A\$(1,2,3,4)</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testErrorForEmptySubscript() {
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT <error descr=\"Bad subscript definition\">A\$()</error>",
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
            "100 PRINT <error descr=\"Name conflict\">A\$</error>\n" +
                    "200 PRINT <error descr=\"Name conflict\">A\$(1)</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testConflictBetweenArraysWithDifferentDimensions() {
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT <error descr=\"Name conflict\">A\$(1)</error>\n" +
                    "200 PRINT <error descr=\"Name conflict\">A\$(1,2)</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testConflictAnnotatedOnAllThreeOccurrences() {
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT <error descr=\"Name conflict\">A\$</error>\n" +
                    "200 PRINT <error descr=\"Name conflict\">A\$(1)</error>\n" +
                    "300 PRINT <error descr=\"Name conflict\">A\$</error>",
        )
        myFixture.checkHighlighting(true, false, false)
    }

    fun testNoConflictBetweenDifferentVariableNames() {
        myFixture.configureByText("test.tibasic", "100 PRINT A$\n200 PRINT B$(1)")
        myFixture.checkHighlighting(true, false, false)
    }
}

