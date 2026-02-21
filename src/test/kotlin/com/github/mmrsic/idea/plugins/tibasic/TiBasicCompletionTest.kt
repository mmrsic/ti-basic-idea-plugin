package com.github.mmrsic.idea.plugins.tibasic

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
}


