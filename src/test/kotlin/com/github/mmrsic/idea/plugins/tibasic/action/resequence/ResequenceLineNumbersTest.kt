package com.github.mmrsic.idea.plugins.tibasic.action.resequence

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase

class ResequenceLineNumbersTest : TiBasicTestBase() {

    fun testResequenceAscendingLines() {
        val file = configureFile("100 PRINT \"A\"\n200 PRINT \"B\"\n300 PRINT \"C\"")
        val result = resequencedText(file)
        assertEquals("100 PRINT \"A\"\n110 PRINT \"B\"\n120 PRINT \"C\"", result)
    }

    fun testResequenceNonAscendingLines() {
        val file = configureFile("100 PRINT \"A\"\n50 PRINT \"B\"\n200 PRINT \"C\"")
        val result = resequencedText(file)
        assertEquals("100 PRINT \"A\"\n110 PRINT \"B\"\n120 PRINT \"C\"", result)
    }

    fun testResequenceDuplicateLineNumbers() {
        val file = configureFile("100 PRINT \"A\"\n100 PRINT \"B\"\n100 PRINT \"C\"")
        val result = resequencedText(file)
        assertEquals("100 PRINT \"A\"\n110 PRINT \"B\"\n120 PRINT \"C\"", result)
    }

    fun testResequenceSingleLine() {
        val file = configureFile("500 PRINT \"X\"")
        val result = resequencedText(file)
        assertEquals("100 PRINT \"X\"", result)
    }

    fun testResequencePreservesStatementContent() {
        val file = configureFile("10 PRINT \"Hello World\"\n20 PRINT \"Goodbye\"")
        val result = resequencedText(file)
        assertEquals("100 PRINT \"Hello World\"\n110 PRINT \"Goodbye\"", result)
    }

    fun testResequenceNumberLengthChange() {
        val file = configureFile("9 PRINT \"A\"\n10 PRINT \"B\"\n11 PRINT \"C\"")
        val result = resequencedText(file)
        assertEquals("100 PRINT \"A\"\n110 PRINT \"B\"\n120 PRINT \"C\"", result)
    }

    fun testResequenceWithCustomStep() {
        val file = configureFile("100 PRINT \"A\"\n200 PRINT \"B\"\n300 PRINT \"C\"")
        val result = resequencedText(file, step = 5)
        assertEquals("100 PRINT \"A\"\n105 PRINT \"B\"\n110 PRINT \"C\"", result)
    }

    fun testResequenceWithCustomStart() {
        val file = configureFile("100 PRINT \"A\"\n200 PRINT \"B\"\n300 PRINT \"C\"")
        val result = resequencedText(file, start = 50)
        assertEquals("50 PRINT \"A\"\n60 PRINT \"B\"\n70 PRINT \"C\"", result)
    }

    fun testResequenceWithCustomStartAndStep() {
        val file = configureFile("100 PRINT \"A\"\n200 PRINT \"B\"\n300 PRINT \"C\"")
        val result = resequencedText(file, start = 20, step = 7)
        assertEquals("20 PRINT \"A\"\n27 PRINT \"B\"\n34 PRINT \"C\"", result)
    }

    fun testResequenceUpdatesGotoTarget() {
        val file = configureFile("100 GOTO 200\n200 PRINT \"OK\"")
        val result = resequencedText(file)
        assertEquals("100 GOTO 110\n110 PRINT \"OK\"", result)
    }

    fun testResequenceUpdatesOnGotoTargets() {
        val file = configureFile("100 ON X GOTO 200,300\n200 PRINT \"A\"\n300 PRINT \"B\"")
        val result = resequencedText(file)
        assertEquals("100 ON X GOTO 110,120\n110 PRINT \"A\"\n120 PRINT \"B\"", result)
    }

    fun testResequenceUpdatesLineNumberListStatement() {
        val file = configureFile("100 BREAK 200,300\n200 PRINT \"A\"\n300 PRINT \"B\"")
        val result = resequencedText(file)
        assertEquals("100 BREAK 110,120\n110 PRINT \"A\"\n120 PRINT \"B\"", result)
    }

    fun testResequenceUndefinedGotoTargetIsLeftUnchanged() {
        val file = configureFile("100 GOTO 999\n200 PRINT \"OK\"")
        val result = resequencedText(file)
        assertEquals("100 GOTO 999\n110 PRINT \"OK\"", result)
    }

    fun testResequenceUpdatesRestoreTarget() {
        val file = configureFile("100 DATA 1,2\n200 RESTORE 100\n300 END")
        val result = resequencedText(file)
        assertEquals("100 DATA 1,2\n110 RESTORE 100\n120 END", result)
    }

    fun testResequenceUpdatesRestoreTargetWhenLineNumberChanges() {
        val file = configureFile("10 DATA 1,2\n20 RESTORE 10\n30 END")
        val result = resequencedText(file)
        assertEquals("100 DATA 1,2\n110 RESTORE 100\n120 END", result)
    }

    fun testResequenceRestoreWithoutLineNumberIsUnchanged() {
        val file = configureFile("100 DATA 1,2\n200 RESTORE\n300 END")
        val result = resequencedText(file)
        assertEquals("100 DATA 1,2\n110 RESTORE\n120 END", result)
    }
}
