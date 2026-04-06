package com.github.mmrsic.idea.plugins.tibasic.editor

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TiBasicPastePreProcessorTest : BasePlatformTestCase() {

    private val processor = TiBasicPastePreProcessor()

    fun testPasteAtEndRenumbersSingleLine() {
        myFixture.configureByText("test.tibasic", "100 PRINT<caret>")
        val result = processor.preprocessOnPaste(project, myFixture.file, myFixture.editor, "100 PRINT", null)
        assertEquals("110 PRINT", result)
    }

    fun testPasteAtEndRenumbersWhenSourceIsNotMultipleOfTen() {
        myFixture.configureByText("test.tibasic", "105 PRINT<caret>")
        val result = processor.preprocessOnPaste(project, myFixture.file, myFixture.editor, "100 PRINT", null)
        assertEquals("110 PRINT", result)
    }

    fun testPasteAtEndRenumbersMultipleLines() {
        myFixture.configureByText("test.tibasic", "100 PRINT\n200 PRINT<caret>")
        val result = processor.preprocessOnPaste(project, myFixture.file, myFixture.editor, "100 PRINT\n200 PRINT", null)
        assertEquals("210 PRINT\n220 PRINT", result)
    }

    fun testPasteAtEndUsesLargestLineNumber() {
        myFixture.configureByText("test.tibasic", "100 PRINT\n200 PRINT<caret>")
        val result = processor.preprocessOnPaste(project, myFixture.file, myFixture.editor, "100 PRINT", null)
        assertEquals("210 PRINT", result)
    }

    fun testPasteInMiddleDoesNotRenumber() {
        myFixture.configureByText("test.tibasic", "100 PRINT<caret>\n200 PRINT")
        val result = processor.preprocessOnPaste(project, myFixture.file, myFixture.editor, "100 PRINT", null)
        assertEquals("100 PRINT", result)
    }

    fun testPasteInNonTiBasicFileDoesNotRenumber() {
        myFixture.configureByText("test.txt", "100 PRINT<caret>")
        val result = processor.preprocessOnPaste(project, myFixture.file, myFixture.editor, "100 PRINT", null)
        assertEquals("100 PRINT", result)
    }

    fun testPasteTextWithoutLineNumberIsNotModified() {
        myFixture.configureByText("test.tibasic", "100 PRINT<caret>")
        val result = processor.preprocessOnPaste(project, myFixture.file, myFixture.editor, "PRINT", null)
        assertEquals("PRINT", result)
    }

    fun testPasteTextWithMixedLinesOnlyRenumbersValidLineNumberLines() {
        myFixture.configureByText("test.tibasic", "100 PRINT<caret>")
        val result = processor.preprocessOnPaste(project, myFixture.file, myFixture.editor, "200 PRINT\nsome comment\n300 PRINT", null)
        assertEquals("110 PRINT\nsome comment\n120 PRINT", result)
    }

    fun testPasteAtEndWithGotoUpdatesReference() {
        myFixture.configureByText("test.tibasic", "100 PRINT<caret>")
        val result = processor.preprocessOnPaste(project, myFixture.file, myFixture.editor, "100 GOTO 100", null)
        assertEquals("110 GOTO 110", result)
    }

    fun testPasteAtEndWithIfThenElseUpdatesAllReferences() {
        myFixture.configureByText("test.tibasic", "100 PRINT<caret>")
        val result = processor.preprocessOnPaste(project, myFixture.file, myFixture.editor, "100 IF X>0 THEN 100 ELSE 100", null)
        assertEquals("110 IF X>0 THEN 110 ELSE 110", result)
    }

    fun testPasteAtEndWithOnGotoUpdatesAllReferences() {
        myFixture.configureByText("test.tibasic", "100 PRINT<caret>")
        val result = processor.preprocessOnPaste(project, myFixture.file, myFixture.editor, "100 ON X GOTO 100,100", null)
        assertEquals("110 ON X GOTO 110,110", result)
    }

    fun testPasteAtEndMultipleLinesWithGotoUpdatesByCorrectDelta() {
        myFixture.configureByText("test.tibasic", "200 PRINT<caret>")
        val result = processor.preprocessOnPaste(project, myFixture.file, myFixture.editor, "100 GOTO 100\n200 GOTO 100", null)
        assertEquals("210 GOTO 210\n220 GOTO 120", result)
    }
}
