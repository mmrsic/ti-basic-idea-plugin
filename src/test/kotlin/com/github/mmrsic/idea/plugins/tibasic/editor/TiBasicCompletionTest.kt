package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase
import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicKeywords

class TiBasicCompletionTest : TiBasicTestBase() {

    fun testKeywordsPrintExists() {
        val keywords = TiBasicKeywords.getKeywords()
        assert(keywords.contains("PRINT"))
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
}


