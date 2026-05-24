package com.github.mmrsic.idea.plugins.tibasic.ide.actions.resequence

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase

class ResequenceQuickFixTest : TiBasicTestBase() {

    fun testQuickFixAvailableForDuplicateLineNumber() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"A\"\n<caret>100 PRINT \"B\"")
        val intentions = myFixture.getAllQuickFixes()
        assertTrue(
            "Resequence quick fix should be available for duplicate line numbers",
            intentions.any { it.text.contains("Resequence") },
        )
    }

    fun testQuickFixAvailableForNonAscendingLineNumber() {
        myFixture.configureByText("test.tibasic", "200 PRINT \"A\"\n<caret>100 PRINT \"B\"")
        val intentions = myFixture.getAllQuickFixes()
        assertTrue(
            "Resequence quick fix should be available for non-ascending line numbers",
            intentions.any { it.text.contains("Resequence") },
        )
    }

    fun testQuickFixResequencesDuplicates() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"A\"\n100 PRINT \"B\"")
        val fix = myFixture.getAllQuickFixes().first { it.text.contains("Resequence") }
        myFixture.launchAction(fix)
        myFixture.checkResult("100 PRINT \"A\"\n110 PRINT \"B\"")
    }

    fun testQuickFixResequencesNonAscending() {
        myFixture.configureByText("test.tibasic", "200 PRINT \"A\"\n100 PRINT \"B\"\n300 PRINT \"C\"")
        val fix = myFixture.getAllQuickFixes().first { it.text.contains("Resequence") }
        myFixture.launchAction(fix)
        myFixture.checkResult("100 PRINT \"A\"\n110 PRINT \"B\"\n120 PRINT \"C\"")
    }
}

