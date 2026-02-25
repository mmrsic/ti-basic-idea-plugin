package com.github.mmrsic.idea.plugins.tibasic.action.format

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase

class FormatCodeTest : TiBasicTestBase() {

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

    fun testImplicitLetVariableIsUppercasedAndSpacesRemoved() {
        val file = configureFile("100 a = 5")
        assertEquals("100 A=5", formattedText(file))
    }

    fun testImplicitLetWithExtraSpacesAroundEquals() {
        val file = configureFile("100 a  =  5")
        assertEquals("100 A=5", formattedText(file))
    }

    fun testImplicitLetStringVariablePreservesStringContent() {
        val file = configureFile("100 a$ = \"hello world\"")
        assertEquals("100 A$=\"hello world\"", formattedText(file))
    }

    fun testExplicitLetKeywordIsUppercased() {
        val file = configureFile("100 let a = 5")
        assertEquals("100 LET a=5", formattedText(file))
    }

    fun testRemPreservesSpacesInComment() {
        val file = configureFile("100 REM  GOTO Beispiel")
        assertEquals("100 REM  GOTO Beispiel", formattedText(file))
    }

    fun testRemPreservesSingleSpaceInComment() {
        val file = configureFile("100 REM hello world")
        assertEquals("100 REM  hello world", formattedText(file))
    }

    fun testRemLowercaseKeywordIsUppercased() {
        val file = configureFile("100 rem  hello world")
        assertEquals("100 REM  hello world", formattedText(file))
    }

    fun testGotoKeywordIsUppercased() {
        val file = configureFile("100 goto 200\n200 PRINT \"OK\"")
        assertEquals("100 GOTO 200\n200 PRINT \"OK\"", formattedText(file))
    }

    fun testGotoPreservesSingleWordForm() {
        val file = configureFile("100 GOTO 200\n200 PRINT \"OK\"")
        assertEquals("100 GOTO 200\n200 PRINT \"OK\"", formattedText(file))
    }

    fun testGoToPreservesTwoWordForm() {
        val file = configureFile("100 GO TO 200\n200 PRINT \"OK\"")
        assertEquals("100 GO TO 200\n200 PRINT \"OK\"", formattedText(file))
    }

    fun testGoToLowercaseKeywordIsUppercased() {
        val file = configureFile("100 go to 200\n200 PRINT \"OK\"")
        assertEquals("100 GO TO 200\n200 PRINT \"OK\"", formattedText(file))
    }

    fun testGoToWithDoubleSpaceNormalizesToSingleSpace() {
        val file = configureFile("100 go  to 200\n200 PRINT \"OK\"")
        assertEquals("100 GO TO 200\n200 PRINT \"OK\"", formattedText(file))
    }

    fun testGotoWithExtraSpaceBeforeLineNumberNormalized() {
        val file = configureFile("100 GOTO  200\n200 PRINT \"OK\"")
        assertEquals("100 GOTO 200\n200 PRINT \"OK\"", formattedText(file))
    }
}

