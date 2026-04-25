package com.github.mmrsic.idea.plugins.tibasic.editor

class TiBasicShiftEnterHandlerTest : TiBasicAutoLineNumberTestBase() {

    override fun setUp() {
        super.setUp()
        TiBasicParenAutoCloseSettings.getInstance().autoCloseOnShiftEnter = true
    }

    override fun tearDown() {
        TiBasicParenAutoCloseSettings.getInstance().autoCloseOnShiftEnter = true
        super.tearDown()
    }

    fun testShiftEnterAfterSingleLineInserts110() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"A\"<caret>")
        myFixture.performEditorAction("EditorStartNewLine")
        assertEquals("100 PRINT \"A\"\n110 ", myFixture.editor.document.text)
    }

    fun testShiftEnterAfterLineNotMultipleOfTenAddsConfiguredDelta() {
        myFixture.configureByText("test.tibasic", "105 PRINT \"A\"<caret>")
        myFixture.performEditorAction("EditorStartNewLine")
        assertEquals("105 PRINT \"A\"\n115 ", myFixture.editor.document.text)
    }

    fun testShiftEnterAfterMultipleLinesUsesLargestLineNumber() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"A\"\n200 PRINT \"B\"<caret>")
        myFixture.performEditorAction("EditorStartNewLine")
        assertEquals("100 PRINT \"A\"\n200 PRINT \"B\"\n210 ", myFixture.editor.document.text)
    }

    fun testShiftEnterInMiddleOfLastLinePutsInsertionAtLineEnd() {
        myFixture.configureByText("test.tibasic", "100 PRINT<caret> \"A\"")
        myFixture.performEditorAction("EditorStartNewLine")
        assertEquals("100 PRINT \"A\"\n110 ", myFixture.editor.document.text)
    }

    fun testShiftEnterWithNoLinesInserts10() {
        myFixture.configureByText("test.tibasic", "<caret>")
        myFixture.performEditorAction("EditorStartNewLine")
        assertEquals("\n10 ", myFixture.editor.document.text)
    }

    fun testShiftEnterUsesConfiguredDelta() {
        TiBasicAutoLineNumberSettings.getInstance().autoLineNumberDelta = 5
        myFixture.configureByText("test.tibasic", "100 PRINT \"A\"<caret>")
        myFixture.performEditorAction("EditorStartNewLine")
        assertEquals("100 PRINT \"A\"\n105 ", myFixture.editor.document.text)
    }

    fun testShiftEnterRoundsGeneratedLineNumberToTensWhenEnabled() {
        TiBasicAutoLineNumberSettings.getInstance().roundToTens = true
        myFixture.configureByText("test.tibasic", "105 PRINT \"A\"<caret>")
        myFixture.performEditorAction("EditorStartNewLine")
        assertEquals("105 PRINT \"A\"\n120 ", myFixture.editor.document.text)
    }

    fun testShiftEnterBetweenNumberedLinesDoesNotInsertLineNumber() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"A\"<caret>\n200 PRINT \"B\"")
        myFixture.performEditorAction("EditorStartNewLine")
        assertFalse(myFixture.editor.document.text.contains("110"))
    }

    fun testShiftEnterAfterLastNumberedLineFollowedByCommentInsertLineNumber() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"A\"<caret>\nThis is a comment")
        myFixture.performEditorAction("EditorStartNewLine")
        assertTrue(myFixture.editor.document.text.contains("110 "))
    }

    fun testCursorPlacedAfterInsertedLineNumberAndSpace() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"A\"<caret>")
        myFixture.performEditorAction("EditorStartNewLine")
        val expectedOffset = "100 PRINT \"A\"\n110 ".length
        assertEquals(expectedOffset, myFixture.editor.caretModel.offset)
    }

    fun testShiftEnterClosesOneUnclosedParen() {
        myFixture.configureByText("test.tibasic", "100 PRINT ABS(X<caret>")
        myFixture.performEditorAction("EditorStartNewLine")
        assertTrue(myFixture.editor.document.text.startsWith("100 PRINT ABS(X)"))
    }

    fun testShiftEnterClosesTwoUnclosedParens() {
        myFixture.configureByText("test.tibasic", "100 LET X=SIN(COS(Y<caret>")
        myFixture.performEditorAction("EditorStartNewLine")
        assertTrue(myFixture.editor.document.text.startsWith("100 LET X=SIN(COS(Y))"))
    }

    fun testShiftEnterDoesNotCloseParensWhenSettingDisabled() {
        TiBasicParenAutoCloseSettings.getInstance().autoCloseOnShiftEnter = false
        myFixture.configureByText("test.tibasic", "100 PRINT ABS(X<caret>")
        myFixture.performEditorAction("EditorStartNewLine")
        assertFalse(myFixture.editor.document.text.startsWith("100 PRINT ABS(X)"))
    }

    fun testShiftEnterClosesParensOnNonLastLine() {
        myFixture.configureByText("test.tibasic", "100 PRINT ABS(X<caret>\n200 PRINT Y")
        myFixture.performEditorAction("EditorStartNewLine")
        assertTrue(myFixture.editor.document.text.startsWith("100 PRINT ABS(X)"))
    }

    fun testShiftEnterDoesNotCloseParensOnNonLastLineWhenSettingDisabled() {
        TiBasicParenAutoCloseSettings.getInstance().autoCloseOnShiftEnter = false
        myFixture.configureByText("test.tibasic", "100 PRINT ABS(X<caret>\n200 PRINT Y")
        myFixture.performEditorAction("EditorStartNewLine")
        assertFalse(myFixture.editor.document.text.startsWith("100 PRINT ABS(X)"))
    }

    fun testShiftEnterDoesNotCloseParensInRemLine() {
        myFixture.configureByText("test.tibasic", "100 REM (unclosed<caret>")
        myFixture.performEditorAction("EditorStartNewLine")
        assertFalse(myFixture.editor.document.text.startsWith("100 REM (unclosed)"))
    }
}
