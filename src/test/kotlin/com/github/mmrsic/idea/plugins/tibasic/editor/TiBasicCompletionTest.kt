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
        assertEquals("100 CALL SCREEN", myFixture.editor.document.text)
        assertNull("Single CALL subprogram completion should not show a lookup popup", myFixture.lookup)
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
