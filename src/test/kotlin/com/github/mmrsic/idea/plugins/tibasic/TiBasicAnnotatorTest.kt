package com.github.mmrsic.idea.plugins.tibasic

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TiBasicAnnotatorTest : BasePlatformTestCase() {

    fun testNoWarningForAscendingLineNumbers() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"A\"\n200 PRINT \"B\"\n300 PRINT \"C\"")
        myFixture.checkHighlighting(false, false, true)
    }

    fun testWarningForEqualLineNumber() {
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT \"A\"\n<warning descr=\"Line number 100 does not follow ascending order (previous: 100)\">100 PRINT \"B\"</warning>"
        )
        myFixture.checkHighlighting(false, false, true)
    }

    fun testWarningForDescendingLineNumber() {
        myFixture.configureByText(
            "test.tibasic",
            "200 PRINT \"A\"\n<warning descr=\"Line number 100 does not follow ascending order (previous: 200)\">100 PRINT \"B\"</warning>"
        )
        myFixture.checkHighlighting(false, false, true)
    }

    fun testWarningOnlyForNonAscendingLineInSequence() {
        myFixture.configureByText(
            "test.tibasic",
            "100 PRINT \"A\"\n200 PRINT \"B\"\n<warning descr=\"Line number 150 does not follow ascending order (previous: 200)\">150 PRINT \"C\"</warning>"
        )
        myFixture.checkHighlighting(false, false, true)
    }

    fun testNoWarningForSingleLine() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"A\"")
        myFixture.checkHighlighting(false, false, true)
    }
}

