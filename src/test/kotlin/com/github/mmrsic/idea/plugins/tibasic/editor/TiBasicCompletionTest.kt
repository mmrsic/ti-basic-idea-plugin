package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicKeywords
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TiBasicCompletionTest : BasePlatformTestCase() {

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
}


