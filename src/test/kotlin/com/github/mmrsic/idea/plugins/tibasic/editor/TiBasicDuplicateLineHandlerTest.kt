package com.github.mmrsic.idea.plugins.tibasic.editor

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TiBasicDuplicateLineHandlerTest : BasePlatformTestCase() {

    fun testDuplicateSingleLineAtEndIncrements() {
        myFixture.configureByText("test.tibasic", "100 PRINT<caret>")
        myFixture.performEditorAction("EditorDuplicate")
        assertEquals("100 PRINT\n110 PRINT", myFixture.editor.document.text)
    }

    fun testDuplicateLineAtEndNotMultipleOfTenRoundsUp() {
        myFixture.configureByText("test.tibasic", "105 PRINT<caret>")
        myFixture.performEditorAction("EditorDuplicate")
        assertEquals("105 PRINT\n110 PRINT", myFixture.editor.document.text)
    }

    fun testDuplicateLastLineUsesLargestLineNumber() {
        myFixture.configureByText("test.tibasic", "100 PRINT\n200 PRINT<caret>")
        myFixture.performEditorAction("EditorDuplicate")
        assertEquals("100 PRINT\n200 PRINT\n210 PRINT", myFixture.editor.document.text)
    }

    fun testDuplicateLineNotAtEndUsesStandardBehavior() {
        myFixture.configureByText("test.tibasic", "100 PRINT<caret>\n200 PRINT")
        myFixture.performEditorAction("EditorDuplicate")
        assertEquals("100 PRINT\n100 PRINT\n200 PRINT", myFixture.editor.document.text)
    }

    fun testDuplicateCursorInMiddleOfLastLineRenumbersFullLine() {
        myFixture.configureByText("test.tibasic", "100 PRINT<caret> X")
        myFixture.performEditorAction("EditorDuplicate")
        assertEquals("100 PRINT X\n110 PRINT X", myFixture.editor.document.text)
    }

    fun testDuplicateWithSelectionOfTwoLinesAtEndRenumbersBoth() {
        myFixture.configureByText("test.tibasic", "<selection>100 PRINT\n200 PRINT\n</selection>")
        myFixture.performEditorAction("EditorDuplicate")
        val text = myFixture.editor.document.text
        assertTrue("Expected 210 in duplicated text, actual: '$text'", text.contains("210 PRINT"))
        assertTrue("Expected 220 in duplicated text, actual: '$text'", text.contains("220 PRINT"))
    }

    fun testDuplicateLineWithGotoUpdatesReference() {
        myFixture.configureByText("test.tibasic", "100 GOTO 100<caret>")
        myFixture.performEditorAction("EditorDuplicate")
        assertEquals("100 GOTO 100\n110 GOTO 110", myFixture.editor.document.text)
    }

    fun testDuplicateLineWithIfThenElseUpdatesAllReferences() {
        myFixture.configureByText("test.tibasic", "100 IF X>0 THEN 100 ELSE 100<caret>")
        myFixture.performEditorAction("EditorDuplicate")
        assertEquals("100 IF X>0 THEN 100 ELSE 100\n110 IF X>0 THEN 110 ELSE 110", myFixture.editor.document.text)
    }

    fun testDuplicateLineWithOnGotoUpdatesAllReferences() {
        myFixture.configureByText("test.tibasic", "100 ON X GOTO 100,100<caret>")
        myFixture.performEditorAction("EditorDuplicate")
        assertEquals("100 ON X GOTO 100,100\n110 ON X GOTO 110,110", myFixture.editor.document.text)
    }

    fun testDuplicateLineWithRestoreUpdatesReference() {
        myFixture.configureByText("test.tibasic", "100 RESTORE 100<caret>")
        myFixture.performEditorAction("EditorDuplicate")
        assertEquals("100 RESTORE 100\n110 RESTORE 110", myFixture.editor.document.text)
    }
}
