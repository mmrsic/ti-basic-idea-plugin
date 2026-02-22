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

    fun testErrorForNonStringLiteralPrintArgument() {
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT <error descr=\"PRINT argument must be an expression\">42</error>",
        )
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
}

