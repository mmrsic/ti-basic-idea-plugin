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
        assertEquals("100 LET A=5", formattedText(file))
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

    fun testOnGotoFormatted() {
        val file = configureFile("100 ON X GOTO 200,300\n200 PRINT \"A\"\n300 PRINT \"B\"")
        assertEquals("100 ON X GOTO 200,300\n200 PRINT \"A\"\n300 PRINT \"B\"", formattedText(file))
    }

    fun testOnGotoKeywordUppercased() {
        val file = configureFile("100 on x goto 200\n200 PRINT \"OK\"")
        assertEquals("100 ON X GOTO 200\n200 PRINT \"OK\"", formattedText(file))
    }

    fun testOnGoToTwoWordsPreserved() {
        val file = configureFile("100 ON X GO TO 200,300\n200 PRINT \"A\"\n300 PRINT \"B\"")
        assertEquals("100 ON X GO TO 200,300\n200 PRINT \"A\"\n300 PRINT \"B\"", formattedText(file))
    }

    fun testOnGotoSpacesAroundCommaRemoved() {
        val file = configureFile("100 ON X GOTO 200 , 300\n200 PRINT \"A\"\n300 PRINT \"B\"")
        assertEquals("100 ON X GOTO 200,300\n200 PRINT \"A\"\n300 PRINT \"B\"", formattedText(file))
    }

    fun testOnGotoExpressionSpacesRemoved() {
        val file = configureFile("100 ON X + Y GOTO 200\n200 PRINT \"OK\"")
        assertEquals("100 ON X+Y GOTO 200\n200 PRINT \"OK\"", formattedText(file))
    }

    fun testForToFormattedCorrectly() {
        val file = configureFile("100 for i = 1 to 10\n200 next i")
        assertEquals("100 FOR I=1 TO 10\n200 NEXT I", formattedText(file))
    }

    fun testForToStepFormattedCorrectly() {
        val file = configureFile("100 for i = 1 to 10 step 2\n200 next i")
        assertEquals("100 FOR I=1 TO 10 STEP 2\n200 NEXT I", formattedText(file))
    }

    fun testForExtraSpacesNormalized() {
        val file = configureFile("100 FOR   I   =   1   TO   10\n200 NEXT I")
        assertEquals("100 FOR I=1 TO 10\n200 NEXT I", formattedText(file))
    }

    fun testForExpressionInLimitFormatted() {
        val file = configureFile("100 FOR I = 1 TO N + 1\n200 NEXT I")
        assertEquals("100 FOR I=1 TO N+1\n200 NEXT I", formattedText(file))
    }

    fun testNextVariableUppercased() {
        val file = configureFile("100 FOR I = 1 TO 10\n200 next   i")
        assertEquals("100 FOR I=1 TO 10\n200 NEXT I", formattedText(file))
    }

    fun testInputWithSingleVariableFormatted() {
        val file = configureFile("100 INPUT A")
        assertEquals("100 INPUT A", formattedText(file))
    }

    fun testInputWithMultipleVariablesSpacesRemoved() {
        val file = configureFile("100 INPUT A , B , C")
        assertEquals("100 INPUT A,B,C", formattedText(file))
    }

    fun testInputVariablesUppercased() {
        val file = configureFile("100 input a,b$")
        assertEquals("100 INPUT A,B$", formattedText(file))
    }

    fun testInputWithPromptFormatted() {
        val file = configureFile("100 INPUT \"Enter\": A")
        assertEquals("100 INPUT \"Enter\":A", formattedText(file))
    }

    fun testInputWithPromptSpacesNormalized() {
        val file = configureFile("100 INPUT \"Enter\" :  A , B")
        assertEquals("100 INPUT \"Enter\":A,B", formattedText(file))
    }

    fun testInputWithPromptVariablesUppercased() {
        val file = configureFile("100 input \"Name: \" : a$")
        assertEquals("100 INPUT \"Name: \":A$", formattedText(file))
    }

    fun testReadWithSingleVariableFormatted() {
        val file = configureFile("100 READ A")
        assertEquals("100 READ A", formattedText(file))
    }

    fun testReadWithMultipleVariablesSpacesRemoved() {
        val file = configureFile("100 READ A , B , C")
        assertEquals("100 READ A,B,C", formattedText(file))
    }

    fun testReadVariablesUppercased() {
        val file = configureFile("100 read a,b$")
        assertEquals("100 READ A,B$", formattedText(file))
    }

    fun testDataWithSingleNumericItemFormatted() {
        val file = configureFile("100 DATA 42")
        assertEquals("100 DATA 42", formattedText(file))
    }

    fun testDataWithStringLiteralPreserved() {
        val file = configureFile("100 DATA \"hello\"")
        assertEquals("100 DATA \"hello\"", formattedText(file))
    }

    fun testDataWithSpacesAroundCommasRemoved() {
        val file = configureFile("100 DATA 1 , 2 , 3")
        assertEquals("100 DATA 1,2,3", formattedText(file))
    }

    fun testDataUnquotedItemUppercased() {
        val file = configureFile("100 DATA hello")
        assertEquals("100 DATA HELLO", formattedText(file))
    }

    fun testDataWithMixedItemsFormatted() {
        val file = configureFile("100 data  \"World\" , 42 , hello")
        assertEquals("100 DATA \"World\",42,HELLO", formattedText(file))
    }

    fun testDataWithConsecutiveCommasPreserved() {
        val file = configureFile("100 DATA 1,,3")
        assertEquals("100 DATA 1,,3", formattedText(file))
    }

    fun testDataSpacesBetweenConsecutiveCommasRemoved() {
        val file = configureFile("100 DATA , , ")
        assertEquals("100 DATA ,,", formattedText(file))
    }

    fun testDataWithQuotedStringContainingComma() {
        val file = configureFile("100 DATA \"a,b\"")
        assertEquals("100 DATA \"a,b\"", formattedText(file))
    }

    fun testRestoreWithNoArgumentFormatted() {
        val file = configureFile("100 RESTORE")
        assertEquals("100 RESTORE", formattedText(file))
    }

    fun testRestoreWithLineNumberFormatted() {
        val file = configureFile("100 RESTORE 200")
        assertEquals("100 RESTORE 200", formattedText(file))
    }

    fun testRestoreKeywordUppercased() {
        val file = configureFile("100 restore  200")
        assertEquals("100 RESTORE 200", formattedText(file))
    }

    fun testTabKeywordInPrintIsUppercased() {
        val file = configureFile("100 PRINT tab(5);\"text\"")
        assertEquals("100 PRINT TAB(5);\"text\"", formattedText(file))
    }

    fun testTabKeywordLowercaseInPrintIsUppercased() {
        val file = configureFile("100 print tab(5);\"hello\"")
        assertEquals("100 PRINT TAB(5);\"hello\"", formattedText(file))
    }

    fun testTabWithSpacesInPrintFormattedCorrectly() {
        val file = configureFile("100 PRINT TAB( 5 ) ; \"text\"")
        assertEquals("100 PRINT TAB(5);\"text\"", formattedText(file))
    }
}

