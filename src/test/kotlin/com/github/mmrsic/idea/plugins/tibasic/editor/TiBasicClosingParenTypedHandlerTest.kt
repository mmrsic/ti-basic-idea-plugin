package com.github.mmrsic.idea.plugins.tibasic.editor

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TiBasicClosingParenTypedHandlerTest : BasePlatformTestCase() {

    fun testTypingClosingParenSkipsExistingParenInTiBasicFile() {
        myFixture.configureByText("test.tibasic", "1130 FELD$(1,FZ(<caret>))")

        myFixture.type(')')

        myFixture.checkResult("1130 FELD$(1,FZ())")
        assertEquals("1130 FELD$(1,FZ())".indexOf("))") + 1, myFixture.editor.caretModel.offset)
    }

    fun testTypingClosingParenInsertsParenWhenNoClosingParenExists() {
        myFixture.configureByText("test.tibasic", "1130 FELD$(1,FZ(<caret>)")

        myFixture.type(')')

        myFixture.checkResult("1130 FELD$(1,FZ())")
    }
}
