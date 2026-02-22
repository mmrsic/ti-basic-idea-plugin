package com.github.mmrsic.idea.plugins.tibasic

import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class FormatCodeTest : BasePlatformTestCase() {

    fun testLowercaseKeywordIsUppercased() {
        val file = configureFile("100 print \"hello\"")
        assertEquals("100 PRINT \"hello\"", formattedText(file))
    }

    fun testMixedCaseKeywordIsUppercased() {
        val file = configureFile("100 Print \"hello\"")
        assertEquals("100 PRINT \"hello\"", formattedText(file))
    }

    fun testLeadingWhitespaceIsRemoved() {
        val file = configureFile("   100 PRINT \"x\"")
        assertEquals("100 PRINT \"x\"", formattedText(file))
    }

    fun testTrailingWhitespaceIsRemoved() {
        val file = configureFile("100 PRINT \"x\"   ")
        assertEquals("100 PRINT \"x\"", formattedText(file))
    }

    fun testMultipleSpacesAfterLineNumberReducedToOne() {
        val file = configureFile("100   PRINT \"x\"")
        assertEquals("100 PRINT \"x\"", formattedText(file))
    }

    fun testMultipleSpacesAfterPrintReducedToOne() {
        val file = configureFile("100 PRINT   \"x\"")
        assertEquals("100 PRINT \"x\"", formattedText(file))
    }

    fun testWhitespaceOutsideStringInArgumentIsRemoved() {
        val file = configureFile("100 PRINT \"a\" ; \"b\"")
        assertEquals("100 PRINT \"a\";\"b\"", formattedText(file))
    }

    fun testCommentLineRemainsUnchanged() {
        val file = configureFile("this is a comment line")
        assertEquals("this is a comment line", formattedText(file))
    }

    fun testStringContentsAreNotUppercased() {
        val file = configureFile("100 PRINT \"hello world\"")
        assertEquals("100 PRINT \"hello world\"", formattedText(file))
    }

    fun testMultipleLinesFormatted() {
        val file = configureFile("100 print \"hello\"\n200 PRINT \"world\"")
        assertEquals("100 PRINT \"hello\"\n200 PRINT \"world\"", formattedText(file))
    }

    fun testCommentLineBetweenCodeLinesUnchanged() {
        val file = configureFile("100 PRINT \"a\"\nthis is a comment\n200 PRINT \"b\"")
        assertEquals("100 PRINT \"a\"\nthis is a comment\n200 PRINT \"b\"", formattedText(file))
    }

    fun testPrintWithNoArgumentFormatted() {
        val file = configureFile("100 print")
        assertEquals("100 PRINT", formattedText(file))
    }

    fun testUppercaseOutsideStringsLeavesStringContentsIntact() {
        assertEquals("HELLO \"world\" END", uppercaseOutsideStrings("hello \"world\" end"))
    }

    fun testUppercaseOutsideStringsHandlesAdjacentStrings() {
        assertEquals("\"abc\"\"def\"", uppercaseOutsideStrings("\"abc\"\"def\""))
    }

    fun testUppercaseOutsideStringsPreservesEscapedQuoteInsideString() {
        assertEquals("\"say \"\"hi\"\"\"", uppercaseOutsideStrings("\"say \"\"hi\"\"\""))
    }

    fun testUppercaseOutsideStringsUppercasesTextAfterStringWithEscapedQuote() {
        assertEquals("\"a\"\"b\" END", uppercaseOutsideStrings("\"a\"\"b\" end"))
    }

    fun testRemoveWhitespaceOutsideStringsKeepsStringWhitespace() {
        assertEquals("\"a b\";\"c d\"", removeWhitespaceOutsideStrings("\"a b\" ; \"c d\""))
    }

    fun testRemoveWhitespaceOutsideStringsPreservesEscapedQuoteInsideString() {
        assertEquals("\"say \"\"hi\"\"\"", removeWhitespaceOutsideStrings("\"say \"\"hi\"\"\""))
    }

    fun testPrintWithStringContainingEscapedQuoteIsFormatted() {
        val file = configureFile("100 print \"say \"\"hi\"\"\"")
        assertEquals("100 PRINT \"say \"\"hi\"\"\"", formattedText(file))
    }

    private fun configureFile(text: String): TiBasicFile {
        myFixture.configureByText("test.tibasic", text)
        return myFixture.file as TiBasicFile
    }
}

