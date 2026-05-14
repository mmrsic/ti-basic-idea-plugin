package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase
import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicKeywords
import com.intellij.codeInsight.lookup.LookupElementPresentation

class TiBasicCompletionTest : TiBasicTestBase() {

    fun testKeywordsPrintExists() {
        val keywords = TiBasicKeywords.getKeywords()
        assert(keywords.contains("PRINT"))
    }

    fun testKeywordsOptionBaseExists() {
        assertTrue("OPTION BASE must be in keywords", TiBasicKeywords.getKeywords().contains("OPTION BASE"))
    }

    fun testKeywordsOptionAloneNotPresent() {
        assertFalse("bare OPTION must not be in keywords", TiBasicKeywords.getKeywords().contains("OPTION"))
    }

    fun testKeywordsNotEmpty() {
        val keywords = TiBasicKeywords.getKeywords()
        assert(keywords.isNotEmpty())
    }

    fun testCompletionSuggestsPrintForLowercasePrefix() {
        myFixture.configureByText("test.tibasic", "100 pr<caret>")
        myFixture.completeBasic()
        val appliedText = myFixture.editor.document.text
        val popupItems = myFixture.lookupElementStrings ?: emptyList()
        assertTrue(
            "PRINT must be applied or suggested when typing 'pr'",
            appliedText.contains("PRINT") || popupItems.contains("PRINT"),
        )
    }

    fun testCompletionSuggestsPrintForSingleLowercaseLetter() {
        myFixture.configureByText("test.tibasic", "100 p<caret>")
        myFixture.completeBasic()
        val appliedText = myFixture.editor.document.text
        val popupItems = myFixture.lookupElementStrings ?: emptyList()
        assertTrue(
            "PRINT must be applied or suggested when typing 'p'",
            appliedText.contains("PRINT") || popupItems.contains("PRINT"),
        )
    }

    fun testCompletionSuggestsPrintForFullLowercaseKeyword() {
        myFixture.configureByText("test.tibasic", "100 print<caret>")
        myFixture.completeBasic()
        val appliedText = myFixture.editor.document.text
        val popupItems = myFixture.lookupElementStrings ?: emptyList()
        assertTrue(
            "PRINT must be applied or suggested when typing 'print'",
            appliedText.contains("PRINT") || popupItems.contains("PRINT"),
        )
    }

    fun testCompletionSuggestsPrintForMixedCasePrefix() {
        myFixture.configureByText("test.tibasic", "100 Pr<caret>")
        myFixture.completeBasic()
        val appliedText = myFixture.editor.document.text
        val popupItems = myFixture.lookupElementStrings ?: emptyList()
        assertTrue(
            "PRINT must be applied or suggested when typing 'Pr'",
            appliedText.contains("PRINT") || popupItems.contains("PRINT"),
        )
    }

    fun testCompletionSuggestsGeneratedLineNumberAtStartOfEligibleUnnumberedLastLine() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"A\"\n<caret>PRINT \"B\"")
        myFixture.completeBasic()
        val popupItems = myFixture.lookupElementStrings ?: emptyList()
        assertTrue("Generated line number 110 must be suggested", popupItems.contains("110"))
    }

    fun testCompletionShowsGeneratedLineNumberFirstOnBlankEligibleLastLine() {
        myFixture.configureByText("test.tibasic", "100 LET A=1\n<caret>")
        myFixture.completeBasic()
        assertEquals("110", myFixture.lookup?.items?.firstOrNull()?.lookupString)
    }

    fun testCompletionShowsGeneratedLineNumberFirstForMatchingTypedDigits() {
        myFixture.configureByText("test.tibasic", "100 LET A=1\n11<caret>PRINT A")
        myFixture.completeBasic()
        val appliedText = myFixture.editor.document.text
        val firstLookupItem = myFixture.lookup?.items?.firstOrNull()?.lookupString
        assertTrue(
            "Generated line number 110 must be inserted or shown as the first suggestion for matching typed digits",
            appliedText == "100 LET A=1\n110 PRINT A" || firstLookupItem == "110",
        )
    }

    fun testCompletionDoesNotSuggestGeneratedLineNumberForNonMatchingTypedDigits() {
        myFixture.configureByText("test.tibasic", "100 LET A=1\n12<caret>PRINT A")
        myFixture.completeBasic()
        val popupItems = myFixture.lookupElementStrings ?: emptyList()
        assertFalse("Generated line number must not be suggested for non-matching typed digits", popupItems.contains("110"))
    }

    fun testCompletionInsertsGeneratedLineNumberWithTrailingSpace() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"A\"\n<caret>PRINT \"B\"")
        myFixture.completeBasic()
        myFixture.lookup?.let { lookup ->
            lookup.currentItem = lookup.items.firstOrNull { it.lookupString == "110" }
            myFixture.finishLookup('\n')
        }
        assertEquals("100 PRINT \"A\"\n110 PRINT \"B\"", myFixture.editor.document.text)
        assertEquals("100 PRINT \"A\"\n110 ".length, myFixture.editor.caretModel.offset)
    }

    fun testCompletionSuggestsGeneratedLineNumberUsingRoundedSettings() {
        val settings = TiBasicAutoLineNumberSettings.getInstance()
        val previousRoundToTens = settings.roundToTens
        try {
            settings.roundToTens = true
            myFixture.configureByText("test.tibasic", "105 PRINT \"A\"\n<caret>")
            myFixture.completeBasic()
            val popupItems = myFixture.lookupElementStrings ?: emptyList()
            assertTrue("Rounded generated line number 120 must be suggested", popupItems.contains("120"))
        } finally {
            settings.roundToTens = previousRoundToTens
        }
    }

    fun testCompletionSuggestsNumericVariable() {
        myFixture.configureByText("test.tibasic", "100 LET A=5\n200 <caret>")
        myFixture.completeBasic()
        val popupItems = myFixture.lookupElementStrings ?: emptyList()
        assertTrue("Numeric variable A must be suggested", popupItems.contains("A"))
    }

    fun testCompletionSuggestsStringVariable() {
        myFixture.configureByText("test.tibasic", "100 LET A$=\"HELLO\"\n200 <caret>")
        myFixture.completeBasic()
        val popupItems = myFixture.lookupElementStrings ?: emptyList()
        assertTrue("String variable A$ must be suggested", popupItems.contains("A$"))
    }

    fun testCompletionSuggestsNumericArrayWithParentheses() {
        myFixture.configureByText("test.tibasic", "100 DIM ARR(10)\n200 <caret>")
        myFixture.completeBasic()
        val popupItems = myFixture.lookupElementStrings ?: emptyList()
        assertTrue("Numeric array ARR must be suggested with parentheses", popupItems.contains("ARR()"))
    }

    fun testCompletionSuggestsStringArrayWithParentheses() {
        myFixture.configureByText("test.tibasic", "100 DIM ARR$(10)\n200 <caret>")
        myFixture.completeBasic()
        val popupItems = myFixture.lookupElementStrings ?: emptyList()
        assertTrue("String array ARR$ must be suggested with parentheses", popupItems.contains("ARR$()"))
    }

    fun testCompletionInsertsArrayParenthesesAndPlacesCaretInside() {
        myFixture.configureByText("test.tibasic", "100 DIM ARRAY(10)\n200 ARR<caret>")
        myFixture.completeBasic()
        assertNull("Single array completion should not show a lookup popup", myFixture.lookup)
        assertEquals("100 DIM ARRAY(10)\n200 ARRAY()", myFixture.editor.document.text)
        assertEquals("100 DIM ARRAY(10)\n200 ARRAY(".length, myFixture.editor.caretModel.offset)
    }


    fun testCompletionSuggestsVariableInEmptyArraySubscript() {
        myFixture.configureByText("test.tibasic", "100 LET INDEX=1\n200 FELD$(<caret>)")
        myFixture.completeBasic()
        val popupItems = myFixture.lookupElementStrings ?: emptyList()
        assertTrue("Variable INDEX must be suggested inside empty array subscript", popupItems.contains("INDEX"))
        assertTrue("Function SIN must be suggested inside empty array subscript", popupItems.contains("SIN"))
    }

    fun testCompletionAcceptedInExistingArrayParensKeepsSingleClosingParen() {
        myFixture.configureByText("test.tibasic", "100 LET INDEX=1\n200 FELD$(IN<caret>)")
        myFixture.completeBasic()
        myFixture.lookup?.let { lookup ->
            lookup.currentItem = lookup.items.firstOrNull { it.lookupString == "INDEX" }
            myFixture.finishLookup('\n')
        }
        assertEquals("100 LET INDEX=1\n200 FELD$(INDEX)", myFixture.editor.document.text)
        assertEquals("100 LET INDEX=1\n200 FELD$(INDEX".length, myFixture.editor.caretModel.offset)
    }

    fun testCompletionInNestedArraySubscriptKeepsOuterClosingParen() {
        myFixture.configureByText("test.tibasic", "100 DIM FZ(10)\n200 FELD$(1,F<caret>)")
        myFixture.completeBasic()
        myFixture.lookup?.let { lookup ->
            lookup.currentItem = lookup.items.firstOrNull { it.lookupString == "FZ()" }
            myFixture.finishLookup('\n')
        }
        assertEquals("100 DIM FZ(10)\n200 FELD$(1,FZ())", myFixture.editor.document.text)
        assertEquals("100 DIM FZ(10)\n200 FELD$(1,FZ(".length, myFixture.editor.caretModel.offset)
    }

    fun testCompletionSuggestsOnlyDistinctVariables() {
        myFixture.configureByText("test.tibasic", "100 LET A=1\n200 LET A=2\n300 <caret>")
        myFixture.completeBasic()
        val popupItems = myFixture.lookupElementStrings ?: emptyList()
        assertEquals("Variable A must appear exactly once", 1, popupItems.count { it == "A" })
    }

    fun testCompletionSuggestsOptionBaseKeyword() {
        myFixture.configureByText("test.tibasic", "100 OPT<caret>")
        myFixture.completeBasic()
        assertEquals("100 OPTION BASE", myFixture.editor.document.text)
        assertNull("Single OPTION BASE completion should not show a lookup popup", myFixture.lookup)
    }

    fun testCompletionSuggestsBaseAfterOptionWithSpace() {
        myFixture.configureByText("test.tibasic", "100 OPTION <caret>")
        myFixture.completeBasic()
        val appliedText = myFixture.editor.document.text
        val popupItems = myFixture.lookupElementStrings ?: emptyList()
        assertTrue(
            "BASE must be applied or suggested after 'OPTION '",
            appliedText.contains("BASE") || popupItems.contains("BASE"),
        )
    }

    fun testCompletionSuggestsBaseAfterOptionWithPartialPrefix() {
        myFixture.configureByText("test.tibasic", "100 OPTION B<caret>")
        myFixture.completeBasic()
        val appliedText = myFixture.editor.document.text
        val popupItems = myFixture.lookupElementStrings ?: emptyList()
        assertTrue(
            "BASE must be applied or suggested after 'OPTION B'",
            appliedText.contains("BASE") || popupItems.contains("BASE"),
        )
    }

    fun testCompletionSuggestsVariableInCallArgument() {
        myFixture.configureByText("test.tibasic", "100 LET A=5\n200 CALL SCREEN(<caret>)")
        myFixture.completeBasic()
        val popupItems = myFixture.lookupElementStrings ?: emptyList()
        assertTrue("Variable A must be suggested inside CALL argument list", popupItems.contains("A"))
    }

    fun testCompletionSuggestsVariableInCallArgumentWithExistingText() {
        myFixture.configureByText("test.tibasic", "100 LET ROW=1\n200 CALL HCHAR(ROW,<caret>,42)")
        myFixture.completeBasic()
        val popupItems = myFixture.lookupElementStrings ?: emptyList()
        assertTrue("Variable ROW must be suggested inside CALL argument list", popupItems.contains("ROW"))
    }

    fun testCompletionSuggestsVariableInArraySubscript() {
        myFixture.configureByText("test.tibasic", "100 LET INDEX=1\n200 LET ARR(IN<caret>)=0")
        myFixture.completeBasic()
        val popupItems = myFixture.lookupElementStrings ?: emptyList()
        assertTrue("Variable INDEX must be suggested inside array subscript", popupItems.contains("INDEX"))
    }

    fun testCompletionSuggestsFunctionInArraySubscript() {
        myFixture.configureByText("test.tibasic", "100 LET ARR(S<caret>)=0")
        myFixture.completeBasic()
        val popupItems = myFixture.lookupElementStrings ?: emptyList()
        assertTrue("Function SIN must be suggested inside array subscript", popupItems.contains("SIN"))
    }

    fun testFunctionCompletionShowsParenthesesInLookupPopup() {
        myFixture.configureByText("test.tibasic", "100 LET X=A<caret>")
        myFixture.completeBasic()
        val item = myFixture.lookup?.items?.firstOrNull { it.lookupString == "ABS" }
        assertNotNull("ABS must be offered in the lookup popup", item)
        val presentation = LookupElementPresentation()
        item!!.renderElement(presentation)
        assertEquals("()", presentation.tailText)
    }

    fun testRndCompletionDoesNotShowParenthesesInLookupPopup() {
        myFixture.configureByText("test.tibasic", "100 LET X=R<caret>")
        myFixture.completeBasic()
        val item = myFixture.lookup?.items?.firstOrNull { it.lookupString == "RND" }
        assertNotNull("RND must be offered in the lookup popup", item)
        val presentation = LookupElementPresentation()
        item!!.renderElement(presentation)
        assertNull(presentation.tailText)
    }

    fun testCallSubprogramCompletionShowsParenthesesInLookupPopup() {
        myFixture.configureByText("test.tibasic", "100 CALL S<caret>")
        myFixture.completeBasic()
        val item = myFixture.lookup?.items?.firstOrNull { it.lookupString == "SCREEN" }
        assertNotNull("SCREEN must be offered in the lookup popup", item)
        val presentation = LookupElementPresentation()
        item!!.renderElement(presentation)
        assertEquals("()", presentation.tailText)
    }

    fun testCallClearCompletionDoesNotShowParenthesesInLookupPopup() {
        myFixture.configureByText("test.tibasic", "100 CALL C<caret>")
        myFixture.completeBasic()
        val item = myFixture.lookup?.items?.firstOrNull { it.lookupString == "CLEAR" }
        assertNotNull("CLEAR must be offered in the lookup popup", item)
        val presentation = LookupElementPresentation()
        item!!.renderElement(presentation)
        assertNull(presentation.tailText)
    }

    fun testTabCompletionShowsParenthesesInLookupPopup() {
        myFixture.configureByText("test.tibasic", "100 PRINT T<caret>")
        myFixture.completeBasic()
        val item = myFixture.lookup?.items?.firstOrNull { it.lookupString == "TAB" }
        assertNotNull("TAB must be offered in the lookup popup", item)
        val presentation = LookupElementPresentation()
        item!!.renderElement(presentation)
        assertEquals("()", presentation.tailText)
    }

    fun testCompletionInsertsParenthesesForSelectedFunction() {
        myFixture.configureByText("test.tibasic", "100 LET X=<caret>")
        myFixture.completeBasic()
        myFixture.lookup?.let { lookup ->
            lookup.currentItem = lookup.items.firstOrNull { it.lookupString == "ABS" }
            myFixture.finishLookup('\n')
        }
        assertEquals("100 LET X=ABS()", myFixture.editor.document.text)
        assertEquals("100 LET X=ABS(".length, myFixture.editor.caretModel.offset)
    }

    fun testCompletionKeepsOuterClosingParenWhenSelectingFunctionCall() {
        myFixture.configureByText("test.tibasic", "100 LET X=ARR(<caret>)")
        myFixture.completeBasic()
        myFixture.lookup?.let { lookup ->
            lookup.currentItem = lookup.items.firstOrNull { it.lookupString == "ABS" }
            myFixture.finishLookup('\n')
        }
        assertEquals("100 LET X=ARR(ABS())", myFixture.editor.document.text)
        assertEquals("100 LET X=ARR(ABS(".length, myFixture.editor.caretModel.offset)
    }

    fun testCompletionDoesNotInsertParenthesesForSelectedRnd() {
        myFixture.configureByText("test.tibasic", "100 LET X=<caret>")
        myFixture.completeBasic()
        myFixture.lookup?.let { lookup ->
            lookup.currentItem = lookup.items.firstOrNull { it.lookupString == "RND" }
            myFixture.finishLookup('\n')
        }
        assertEquals("100 LET X=RND", myFixture.editor.document.text)
        assertEquals("100 LET X=RND".length, myFixture.editor.caretModel.offset)
    }

    fun testCompletionInsertsParenthesesForTabKeyword() {
        myFixture.configureByText("test.tibasic", "100 PRINT T<caret>")
        myFixture.completeBasic()
        myFixture.lookup?.let { lookup ->
            lookup.currentItem = lookup.items.firstOrNull { it.lookupString == "TAB" }
            myFixture.finishLookup('\n')
        }
        assertEquals("100 PRINT TAB()", myFixture.editor.document.text)
        assertEquals("100 PRINT TAB(".length, myFixture.editor.caretModel.offset)
    }

    fun testCompletionDoesNotSuggestSubprogramInCallArgument() {
        myFixture.configureByText("test.tibasic", "100 CALL SCREEN(<caret>)")
        myFixture.completeBasic()
        val popupItems = myFixture.lookupElementStrings ?: emptyList()
        assertFalse("Subprogram SCREEN must not be suggested inside CALL argument list", popupItems.contains("SCREEN"))
    }

    fun testCompletionSuggestsSubprogramAtCallNamePosition() {
        myFixture.configureByText("test.tibasic", "100 CALL <caret>")
        myFixture.completeBasic()
        val popupItems = myFixture.lookupElementStrings ?: emptyList()
        assertTrue("Subprogram SCREEN must be suggested at CALL name position", popupItems.contains("SCREEN"))
    }

    fun testCompletionSuggestsSubprogramAtCallNamePositionWithPartialName() {
        myFixture.configureByText("test.tibasic", "100 CALL SCR<caret>")
        myFixture.completeBasic()
        assertEquals("100 CALL SCREEN()", myFixture.editor.document.text)
        assertEquals("100 CALL SCREEN(".length, myFixture.editor.caretModel.offset)
        assertNull("Single CALL subprogram completion should not show a lookup popup", myFixture.lookup)
    }

    fun testCompletionSuggestsNextKeywordAndOpenLoopVariables() {
        myFixture.configureByText(
            "test.tibasic",
            """
            100 FOR I=1 TO 3
            110 FOR J=1 TO 2
            120 NEX<caret>
            """.trimIndent(),
        )
        myFixture.completeBasic()

        val appliedText = myFixture.editor.document.text
        val popupItems = myFixture.lookupElementStrings ?: emptyList()
        assertEquals(
            listOf("NEXT", "NEXT J", "NEXT I"),
            popupItems.filter { it == "NEXT" || it.startsWith("NEXT ") }.ifEmpty {
                listOfNotNull(
                    "NEXT".takeIf { appliedText.contains("120 NEXT") },
                    "NEXT J".takeIf { appliedText.contains("120 NEXT J") },
                    "NEXT I".takeIf { appliedText.contains("120 NEXT I") },
                )
            },
        )
    }

    fun testCompletionAfterNextSpaceSuggestsOnlyOpenLoopVariables() {
        myFixture.configureByText(
            "test.tibasic",
            """
            100 FOR I=1 TO 3
            110 FOR J=1 TO 2
            120 NEXT <caret>
            """.trimIndent(),
        )
        myFixture.completeBasic()

        val popupItems = myFixture.lookupElementStrings ?: emptyList()
        assertEquals(
            listOf("NEXT J", "NEXT I"),
            popupItems.filter { it.startsWith("NEXT ") },
        )
        assertFalse("Bare NEXT must not be suggested again after 'NEXT '", popupItems.contains("NEXT"))
    }

    fun testCompletionAfterNextSpaceInsertsSelectedLoopVariableWithoutDuplicatingNext() {
        myFixture.configureByText(
            "test.tibasic",
            """
            100 FOR I=1 TO 3
            110 NEXT <caret>
            """.trimIndent(),
        )
        myFixture.completeBasic()
        myFixture.lookup?.let { lookup ->
            lookup.currentItem = lookup.items.firstOrNull { it.lookupString == "NEXT I" }
            myFixture.finishLookup('\n')
        }

        assertEquals(
            """
            100 FOR I=1 TO 3
            110 NEXT I
            """.trimIndent(),
            myFixture.editor.document.text,
        )
        assertEquals("100 FOR I=1 TO 3\n110 NEXT I".length, myFixture.editor.caretModel.offset)
    }

    fun testCompletionFiltersNextLoopVariablesByTypedPrefix() {
        myFixture.configureByText(
            "test.tibasic",
            """
            100 FOR I=1 TO 3
            110 FOR INDEX=1 TO 2
            120 NEXT IN<caret>
            """.trimIndent(),
        )
        myFixture.completeBasic()

        val appliedText = myFixture.editor.document.text
        val popupItems = myFixture.lookupElementStrings ?: emptyList()
        assertEquals(
            listOf("NEXT INDEX"),
            popupItems.filter { it.startsWith("NEXT ") }.ifEmpty {
                listOfNotNull("NEXT INDEX".takeIf { appliedText.contains("120 NEXT INDEX") })
            },
        )
    }

    fun testCompletionDoesNotSuggestClosedLoopVariableForNext() {
        myFixture.configureByText(
            "test.tibasic",
            """
            100 FOR I=1 TO 3
            110 NEXT I
            120 NEX<caret>
            """.trimIndent(),
        )
        myFixture.completeBasic()

        val appliedText = myFixture.editor.document.text
        val popupItems = myFixture.lookupElementStrings ?: emptyList()
        assertEquals(
            listOf("NEXT"),
            popupItems.filter { it == "NEXT" || it.startsWith("NEXT ") }.ifEmpty {
                listOfNotNull("NEXT".takeIf { appliedText.contains("120 NEXT") })
            },
        )
    }

    fun testCompletionDoesNotRepeatNextVariableSuggestions() {
        myFixture.configureByText(
            "test.tibasic",
            """
            100 FOR I=1 TO 3
            110 FOR I=1 TO 2
            120 NEX<caret>
            """.trimIndent(),
        )
        myFixture.completeBasic()

        val popupItems = myFixture.lookupElementStrings ?: emptyList()
        assertEquals(1, popupItems.count { it == "NEXT I" })
    }

    fun testCompletionAfterRestoreSpaceSuggestsOnlyDataStatementLineNumbers() {
        myFixture.configureByText(
            "test.tibasic",
            """
            100 DATA 1
            200 PRINT "A"
            300 DATA
            400 RESTORE <caret>
            """.trimIndent(),
        )
        myFixture.completeBasic()

        val popupItems = myFixture.lookupElementStrings ?: emptyList()
        assertTrue("DATA line number 100 must be suggested after RESTORE", popupItems.contains("100"))
        assertTrue("DATA line number 300 must be suggested after RESTORE", popupItems.contains("300"))
        assertFalse("Non-DATA line number 200 must not be suggested after RESTORE", popupItems.contains("200"))
        assertFalse("Current RESTORE line number 400 must not be suggested after RESTORE", popupItems.contains("400"))
    }

    fun testCompletionAfterFileRestoreDoesNotSuggestDataStatementLineNumbers() {
        myFixture.configureByText(
            "test.tibasic",
            """
            100 DATA 1
            200 RESTORE #<caret>
            """.trimIndent(),
        )
        myFixture.completeBasic()

        val popupItems = myFixture.lookupElementStrings ?: emptyList()
        assertFalse("DATA line numbers must not be suggested for file RESTORE", popupItems.contains("100"))
    }

    fun testCompletionDoesNotSuggestGeneratedLineNumberInMiddleOfLine() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"A\"\nPR<caret>INT \"B\"")
        myFixture.completeBasic()
        val popupItems = myFixture.lookupElementStrings ?: emptyList()
        assertFalse("Generated line number must not be suggested in the middle of a line", popupItems.contains("110"))
    }

    fun testCompletionDoesNotSuggestGeneratedLineNumberWhenCurrentLineAlreadyStartsWithNumber() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"A\"\n<caret>110 PRINT \"B\"")
        myFixture.completeBasic()
        val popupItems = myFixture.lookupElementStrings ?: emptyList()
        assertFalse("Generated line number must not be suggested for an already numbered line", popupItems.contains("120"))
    }

    fun testCompletionDoesNotSuggestGeneratedLineNumberBeforeLaterNumberedLine() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"A\"\n<caret>\n200 PRINT \"B\"")
        myFixture.completeBasic()
        val popupItems = myFixture.lookupElementStrings ?: emptyList()
        assertFalse("Generated line number must not be suggested before later numbered lines", popupItems.contains("210"))
    }

    fun testCompletionInsertsParenthesesForSelectedCallSubprogram() {
        myFixture.configureByText("test.tibasic", "100 CALL C<caret>")
        myFixture.completeBasic()
        myFixture.lookup?.let { lookup ->
            lookup.currentItem = lookup.items.firstOrNull { it.lookupString == "COLOR" }
            myFixture.finishLookup('\n')
        }
        assertEquals("100 CALL COLOR()", myFixture.editor.document.text)
        assertEquals("100 CALL COLOR(".length, myFixture.editor.caretModel.offset)
    }

    fun testCompletionKeepsExistingCallParensWhenSelectingSubprogram() {
        myFixture.configureByText("test.tibasic", "100 CALL SCR<caret>()")
        myFixture.completeBasic()
        assertEquals("100 CALL SCREEN()", myFixture.editor.document.text)
        assertEquals("100 CALL SCREEN(".length, myFixture.editor.caretModel.offset)
    }

    fun testCompletionShowsLookupWhenMultipleMatchesRemain() {
        myFixture.configureByText("test.tibasic", "100 S<caret>")
        myFixture.completeBasic()
        val popupItems = myFixture.lookupElementStrings ?: emptyList()
        assertNotNull("Multiple matches must still show the lookup popup", myFixture.lookup)
        assertTrue("SIN must still be offered when multiple matches remain", popupItems.contains("SIN"))
        assertTrue("SGN must still be offered when multiple matches remain", popupItems.contains("SGN"))
    }
}
