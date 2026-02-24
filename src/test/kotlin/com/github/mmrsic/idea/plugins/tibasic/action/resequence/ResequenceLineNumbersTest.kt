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
}
