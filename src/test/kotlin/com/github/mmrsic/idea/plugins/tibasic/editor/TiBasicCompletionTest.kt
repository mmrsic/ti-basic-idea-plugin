package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase
import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicKeywords

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
        myFixture.configureByText("test.tibasic", "100 LET A\$=\"HELLO\"\n200 <caret>")
        myFixture.completeBasic()
        val popupItems = myFixture.lookupElementStrings ?: emptyList()
        assertTrue("String variable A\$ must be suggested", popupItems.contains("A\$"))
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
        val appliedText = myFixture.editor.document.text
        val popupItems = myFixture.lookupElementStrings ?: emptyList()
        assertTrue(
            "OPTION BASE must be applied or suggested when typing 'OPT'",
            appliedText.contains("OPTION BASE") || popupItems.contains("OPTION BASE"),
        )
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
        val appliedText = myFixture.editor.document.text
        val popupItems = myFixture.lookupElementStrings ?: emptyList()
        assertTrue(
            "Subprogram SCREEN must be applied or suggested when typing 'SCR' at CALL name position",
            appliedText.contains("SCREEN") || popupItems.contains("SCREEN"),
        )
    }
}


