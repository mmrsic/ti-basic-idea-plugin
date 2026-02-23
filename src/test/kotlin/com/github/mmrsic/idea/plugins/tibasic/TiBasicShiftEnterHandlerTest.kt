package com.github.mmrsic.idea.plugins.tibasic

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TiBasicShiftEnterHandlerTest : BasePlatformTestCase() {

    fun testShiftEnterAfterSingleLineInserts110() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"A\"<caret>")
        myFixture.performEditorAction("EditorStartNewLine")
        assertEquals("100 PRINT \"A\"\n110 ", myFixture.editor.document.text)
    }

    fun testShiftEnterAfterLineNotMultipleOfTenRoundsUp() {
        myFixture.configureByText("test.tibasic", "105 PRINT \"A\"<caret>")
        myFixture.performEditorAction("EditorStartNewLine")
        assertEquals("105 PRINT \"A\"\n110 ", myFixture.editor.document.text)
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
}
