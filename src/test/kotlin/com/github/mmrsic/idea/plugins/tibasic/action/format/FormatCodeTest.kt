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

    fun testImplicitLetVariableStartingWithToKeywordNotSplit() {
        val file = configureFile("550 TOTAL=TOTAL+40")
        assertEquals("550 TOTAL=TOTAL+40", formattedText(file))
    }

    fun testImplicitLetVariableStartingWithForKeywordNotSplit() {
        val file = configureFile("100 forever=10")
        assertEquals("100 FOREVER=10", formattedText(file))
    }

    fun testImplicitLetVariableStartingWithStepKeywordNotSplit() {
        val file = configureFile("100 stepcount=0")
        assertEquals("100 STEPCOUNT=0", formattedText(file))
    }

    fun testImplicitLetVariableStartingWithIfKeywordNotSplit() {
        val file = configureFile("100 iflag=1")
        assertEquals("100 IFLAG=1", formattedText(file))
    }

    fun testImplicitLetVariableStartingWithOnKeywordNotSplit() {
        val file = configureFile("100 ontime=5")
        assertEquals("100 ONTIME=5", formattedText(file))
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

    fun testImplicitLetNumericArrayWithWhitespaceBeforeSubscriptIsFormatted() {
        val file = configureFile("38 a (8) = 7")
        assertEquals("38 A(8)=7", formattedText(file))
    }

    fun testImplicitLetStringArrayStartingWithListCommandIsNotSplit() {
        val file = configureFile("3820 LIST$(J+1)=LIST$(J)")
        assertEquals("3820 LIST$(J+1)=LIST$(J)", formattedText(file))
    }

    fun testExplicitLetStringArrayStartingWithListCommandIsFormatted() {
        val file = configureFile("3820 let LIST$(J + 1) = LIST$(J)")
        assertEquals("3820 LET LIST$(J+1)=LIST$(J)", formattedText(file))
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
        assertEquals("100 REM hello world", formattedText(file))
    }

    fun testRemLowercaseKeywordIsUppercased() {
        val file = configureFile("100 rem  hello world")
        assertEquals("100 REM  hello world", formattedText(file))
    }

    fun testRemCommentTextRemainsUntouched() {
        val file = configureFile("100 rem MiXeD  case ;  stays")
        assertEquals("100 REM MiXeD  case ;  stays", formattedText(file))
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

    fun testOnGotoWithClosingParenBeforeKeywordHasNoSpace() {
        val file = configureFile("100 ON X(1) GOTO 200\n200 PRINT \"OK\"")
        assertEquals("100 ON X(1)GOTO 200\n200 PRINT \"OK\"", formattedText(file))
    }

    fun testOnGoToWithClosingParenBeforeKeywordHasNoSpace() {
        val file = configureFile("100 ON X(1) GO TO 200\n200 PRINT \"OK\"")
        assertEquals("100 ON X(1)GO TO 200\n200 PRINT \"OK\"", formattedText(file))
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

    fun testDisplayKeywordLowercaseIsUppercased() {
        val file = configureFile("100 display \"hello\"")
        assertEquals("100 DISPLAY \"hello\"", formattedText(file))
    }

    fun testDisplayWithArgumentFormatted() {
        val file = configureFile("100 DISPLAY  \"HELLO\"")
        assertEquals("100 DISPLAY \"HELLO\"", formattedText(file))
    }

    fun testDisplayWithTabFunctionUppercased() {
        val file = configureFile("100 display tab(5);\"text\"")
        assertEquals("100 DISPLAY TAB(5);\"text\"", formattedText(file))
    }

    fun testOptionBaseSpacePreserved() {
        val file = configureFile("2 OPTION BASE 0")
        assertEquals("2 OPTION BASE 0", formattedText(file))
    }

    fun testOptionBaseLowercaseUppercased() {
        val file = configureFile("2 option base 1")
        assertEquals("2 OPTION BASE 1", formattedText(file))
    }

    fun testOptionBaseExtraSpaceNormalized() {
        val file = configureFile("2 OPTION  BASE 0")
        assertEquals("2 OPTION BASE 0", formattedText(file))
    }

    fun testOptionBaseSpaceBetweenKeywordAndValueNormalized() {
        val file = configureFile("2 OPTION BASE  1")
        assertEquals("2 OPTION BASE 1", formattedText(file))
    }

    fun testGosubUppercased() {
        val file = configureFile("100 gosub 200\n200 PRINT \"OK\"\n300 RETURN")
        assertEquals("100 GOSUB 200\n200 PRINT \"OK\"\n300 RETURN", formattedText(file))
    }

    fun testGosubExtraSpaceNormalized() {
        val file = configureFile("100 GOSUB  200\n200 PRINT \"OK\"\n300 RETURN")
        assertEquals("100 GOSUB 200\n200 PRINT \"OK\"\n300 RETURN", formattedText(file))
    }

    fun testGoSubTwoWordsPreserved() {
        val file = configureFile("100 GO SUB 200\n200 PRINT \"OK\"\n300 RETURN")
        assertEquals("100 GO SUB 200\n200 PRINT \"OK\"\n300 RETURN", formattedText(file))
    }

    fun testGoSubTwoWordsLowercaseUppercased() {
        val file = configureFile("100 go sub 200\n200 PRINT \"OK\"\n300 RETURN")
        assertEquals("100 GO SUB 200\n200 PRINT \"OK\"\n300 RETURN", formattedText(file))
    }

    fun testGoSubTwoWordsExtraSpaceNormalized() {
        val file = configureFile("100 GO  SUB 200\n200 PRINT \"OK\"\n300 RETURN")
        assertEquals("100 GO SUB 200\n200 PRINT \"OK\"\n300 RETURN", formattedText(file))
    }

    fun testReturnUppercased() {
        val file = configureFile("100 return")
        assertEquals("100 RETURN", formattedText(file))
    }

    fun testReturnWithExtraSpaceNormalized() {
        val file = configureFile("100 RETURN  ")
        assertEquals("100 RETURN", formattedText(file))
    }

    fun testOnGosubFormatted() {
        val file = configureFile("100 on x gosub 200,300\n200 PRINT \"A\"\n300 PRINT \"B\"\n400 RETURN")
        assertEquals("100 ON X GOSUB 200,300\n200 PRINT \"A\"\n300 PRINT \"B\"\n400 RETURN", formattedText(file))
    }

    fun testOnGosubExtraSpacesNormalized() {
        val file = configureFile("100 ON  X  GOSUB  200 , 300\n200 PRINT \"A\"\n300 PRINT \"B\"\n400 RETURN")
        assertEquals("100 ON X GOSUB 200,300\n200 PRINT \"A\"\n300 PRINT \"B\"\n400 RETURN", formattedText(file))
    }

    fun testOnGoSubTwoWordsPreserved() {
        val file = configureFile("100 ON X GO SUB 200,300\n200 PRINT \"A\"\n300 PRINT \"B\"\n400 RETURN")
        assertEquals("100 ON X GO SUB 200,300\n200 PRINT \"A\"\n300 PRINT \"B\"\n400 RETURN", formattedText(file))
    }

    fun testOnGoSubTwoWordsLowercaseUppercased() {
        val file = configureFile("100 on x go sub 200\n200 PRINT \"OK\"\n300 RETURN")
        assertEquals("100 ON X GO SUB 200\n200 PRINT \"OK\"\n300 RETURN", formattedText(file))
    }

    fun testOnGoSubTwoWordsExtraSpaceNormalized() {
        val file = configureFile("100 ON X GO  SUB 200\n200 PRINT \"OK\"\n300 RETURN")
        assertEquals("100 ON X GO SUB 200\n200 PRINT \"OK\"\n300 RETURN", formattedText(file))
    }

    fun testOnGosubWithClosingParenBeforeKeywordHasNoSpace() {
        val file = configureFile("100 ON X(1) GOSUB 200\n200 PRINT \"OK\"\n300 RETURN")
        assertEquals("100 ON X(1)GOSUB 200\n200 PRINT \"OK\"\n300 RETURN", formattedText(file))
    }

    fun testOpenFormatted() {
        val file = configureFile("100 OPEN #1:\"DSK1.FILE\"")
        assertEquals("100 OPEN #1:\"DSK1.FILE\"", formattedText(file))
    }

    fun testOpenExtraSpacesNormalized() {
        val file = configureFile("100  OPEN  # 1 : \"DSK1.FILE\"")
        assertEquals("100 OPEN #1:\"DSK1.FILE\"", formattedText(file))
    }

    fun testOpenLowercaseUppercased() {
        val file = configureFile("100 open #1:\"file\"")
        assertEquals("100 OPEN #1:\"file\"", formattedText(file))
    }

    fun testOpenVariableFileNumber() {
        val file = configureFile("100 OPEN # N : \"FILE\"")
        assertEquals("100 OPEN #N:\"FILE\"", formattedText(file))
    }

    fun testCloseFormatted() {
        val file = configureFile("100 CLOSE #1")
        assertEquals("100 CLOSE #1", formattedText(file))
    }

    fun testCloseExtraSpacesNormalized() {
        val file = configureFile("100  CLOSE  # 1  ")
        assertEquals("100 CLOSE #1", formattedText(file))
    }

    fun testCloseLowercaseUppercased() {
        val file = configureFile("100 close #1")
        assertEquals("100 CLOSE #1", formattedText(file))
    }

    fun testEofFunctionLowercaseUppercased() {
        val file = configureFile("100 LET X= eof (1 ) ")
        assertEquals("100 LET X=EOF(1)", formattedText(file))
    }

    fun testFilePrintFormattedCorrectly() {
        val file = configureFile("140 PRINT #1:A;B;C")
        assertEquals("140 PRINT #1:A;B;C", formattedText(file))
    }

    fun testFilePrintExtraSpacesNormalized() {
        val file = configureFile("140 PRINT  # 1 : A ; B")
        assertEquals("140 PRINT #1:A;B", formattedText(file))
    }

    fun testFilePrintWithRecordNumberSpacePreserved() {
        val file = configureFile("140 PRINT #1.REC  47:A;B;C;E;F")
        assertEquals("140 PRINT #1.REC 47:A;B;C;E;F", formattedText(file))
    }

    fun testFilePrintWithRecordNumberLowercaseUppercased() {
        val file = configureFile("140 print #1.rec 5:\"hello\"")
        assertEquals("140 PRINT #1.REC 5:\"hello\"", formattedText(file))
    }

    fun testFilePrintWithEmptyArgList() {
        val file = configureFile("100 PRINT #2:")
        assertEquals("100 PRINT #2:", formattedText(file))
    }

    fun testFilePrintWithRecordNumberAndEmptyArgList() {
        val file = configureFile("100 PRINT # 3 . REC  10 :")
        assertEquals("100 PRINT #3.REC 10:", formattedText(file))
    }

    fun testRestoreFileVariantFormattedCorrectly() {
        val file = configureFile("100 RESTORE #1")
        assertEquals("100 RESTORE #1", formattedText(file))
    }

    fun testRestoreFileVariantExtraSpacesNormalized() {
        val file = configureFile("100 RESTORE  # 2")
        assertEquals("100 RESTORE #2", formattedText(file))
    }

    fun testRestoreFileVariantWithRecordNumberFormattedCorrectly() {
        val file = configureFile("100 RESTORE #1,REC 5")
        assertEquals("100 RESTORE #1,REC 5", formattedText(file))
    }

    fun testRestoreFileVariantWithRecordNumberExtraSpacesNormalized() {
        val file = configureFile("100 RESTORE # 1 , REC  5")
        assertEquals("100 RESTORE #1,REC 5", formattedText(file))
    }

    fun testRestoreFileVariantLowercaseUppercased() {
        val file = configureFile("100 restore #1,rec 5")
        assertEquals("100 RESTORE #1,REC 5", formattedText(file))
    }

    fun testIfWithTypoInThenKeywordPreservesSpaces() {
        val file = configureFile("1060 IF K>=A-1 RHEN 1080")
        assertEquals("1060 IF K>=A-1 RHEN 1080", formattedText(file))
    }

    fun testIfWithTypoInThenKeywordLowercaseUppercased() {
        val file = configureFile("1060 if k>=a-1 rhen 1080")
        assertEquals("1060 IF K>=A-1 RHEN 1080", formattedText(file))
    }

    fun testIfWithoutThenKeywordPreservesSpaces() {
        val file = configureFile("100 IF X>5 PRINT \"YES\"")
        assertEquals("100 IF X>5 PRINT \"YES\"", formattedText(file))
    }

    fun testIfWithClosingParenBeforeThenHasNoSpace() {
        val file = configureFile("100 IF (X>5) THEN 200")
        assertEquals("100 IF (X>5)THEN 200", formattedText(file))
    }

    fun testIfWithMultipleSpacesBetweenParenAndThenAllRemoved() {
        val file = configureFile("100 IF (X>5)   THEN 200")
        assertEquals("100 IF (X>5)THEN 200", formattedText(file))
    }

    fun testIfWithoutClosingParenBeforeThenKeepsOneSpace() {
        val file = configureFile("100 IF X>5 THEN 200")
        assertEquals("100 IF X>5 THEN 200", formattedText(file))
    }

    fun testIfWithNestedParensBeforeThenHasNoSpace() {
        val file = configureFile("100 IF (X>(Y+1)) THEN 300")
        assertEquals("100 IF (X>(Y+1))THEN 300", formattedText(file))
    }

    fun testIfWithClosingParenBeforeThenWithElse() {
        val file = configureFile("100 IF (X>5) THEN 200 ELSE 300")
        assertEquals("100 IF (X>5)THEN 200 ELSE 300", formattedText(file))
    }

    fun testIfLowercaseWithClosingParenBeforeThenFormatted() {
        val file = configureFile("100 if (x>5) then 200")
        assertEquals("100 IF (X>5)THEN 200", formattedText(file))
    }
}
