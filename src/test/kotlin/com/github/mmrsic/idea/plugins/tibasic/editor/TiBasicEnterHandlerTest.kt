package com.github.mmrsic.idea.plugins.tibasic.editor

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TiBasicEnterHandlerTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        TiBasicParenAutoCloseSettings.getInstance().autoCloseOnEnter = true
    }

    override fun tearDown() {
        TiBasicParenAutoCloseSettings.getInstance().autoCloseOnEnter = false
        super.tearDown()
    }

    fun testEnterClosesOneUnclosedParenAtLineEnd() {
        myFixture.configureByText("test.tibasic", "100 PRINT ABS(X<caret>")
        myFixture.performEditorAction("EditorEnter")
        assertTrue(myFixture.editor.document.text.startsWith("100 PRINT ABS(X)"))
    }

    fun testEnterClosesTwoUnclosedParensAtLineEnd() {
        myFixture.configureByText("test.tibasic", "100 LET X=SIN(COS(Y<caret>")
        myFixture.performEditorAction("EditorEnter")
        assertTrue(myFixture.editor.document.text.startsWith("100 LET X=SIN(COS(Y))"))
    }

    fun testEnterDoesNotCloseParensWhenCursorMidLine() {
        myFixture.configureByText("test.tibasic", "100 PRINT ABS(<caret>X)")
        myFixture.performEditorAction("EditorEnter")
        assertFalse(myFixture.editor.document.text.startsWith("100 PRINT ABS()"))
    }

    fun testEnterDoesNotCloseParensWhenSettingDisabled() {
        TiBasicParenAutoCloseSettings.getInstance().autoCloseOnEnter = false
        myFixture.configureByText("test.tibasic", "100 PRINT ABS(X<caret>")
        myFixture.performEditorAction("EditorEnter")
        assertFalse(myFixture.editor.document.text.startsWith("100 PRINT ABS(X)"))
    }

    fun testEnterDoesNotCloseParensInRemLine() {
        myFixture.configureByText("test.tibasic", "100 REM (unclosed<caret>")
        myFixture.performEditorAction("EditorEnter")
        assertFalse(myFixture.editor.document.text.startsWith("100 REM (unclosed)"))
    }

    fun testEnterBalancedParensNoChange() {
        myFixture.configureByText("test.tibasic", "100 PRINT ABS(X)<caret>")
        myFixture.performEditorAction("EditorEnter")
        assertTrue(myFixture.editor.document.text.startsWith("100 PRINT ABS(X)\n"))
    }
}
