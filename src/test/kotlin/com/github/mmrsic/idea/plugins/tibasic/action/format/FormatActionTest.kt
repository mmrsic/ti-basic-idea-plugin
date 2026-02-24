package com.github.mmrsic.idea.plugins.tibasic.action.format

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class FormatActionTest : BasePlatformTestCase() {

    fun testWholeFileFormattedWithoutSelection() {
        myFixture.configureByText("test.tibasic", "100 print \"a\"\n200 print \"b\"")
        myFixture.testAction(FormatAction())
        assertEquals("100 PRINT \"a\"\n200 PRINT \"b\"", myFixture.editor.document.text)
    }

    fun testOnlySelectedLineIsFormatted() {
        myFixture.configureByText("test.tibasic", "100 print \"a\"\n200 print \"b\"\n300 print \"c\"")
        val doc = myFixture.editor.document
        myFixture.editor.selectionModel.setSelection(
            doc.getLineStartOffset(1),
            doc.getLineEndOffset(1),
        )
        myFixture.testAction(FormatAction())
        assertEquals("100 print \"a\"\n200 PRINT \"b\"\n300 print \"c\"", myFixture.editor.document.text)
    }

    fun testMultipleSelectedLinesAreFormatted() {
        myFixture.configureByText("test.tibasic", "100 print \"a\"\n200 print \"b\"\n300 print \"c\"")
        val doc = myFixture.editor.document
        myFixture.editor.selectionModel.setSelection(
            doc.getLineStartOffset(1),
            doc.getLineEndOffset(2),
        )
        myFixture.testAction(FormatAction())
        assertEquals("100 print \"a\"\n200 PRINT \"b\"\n300 PRINT \"c\"", myFixture.editor.document.text)
    }

    fun testPartialLineSelectionFormatsEntireLine() {
        myFixture.configureByText("test.tibasic", "100 print \"a\"\n200 print \"b\"\n300 print \"c\"")
        val doc = myFixture.editor.document
        val lineStart = doc.getLineStartOffset(1)
        myFixture.editor.selectionModel.setSelection(lineStart + 4, lineStart + 9)
        myFixture.testAction(FormatAction())
        assertEquals("100 print \"a\"\n200 PRINT \"b\"\n300 print \"c\"", myFixture.editor.document.text)
    }

    fun testSelectionSpanningCommentLinePreservesComment() {
        myFixture.configureByText("test.tibasic", "100 print \"a\"\nthis is a comment\n300 print \"c\"")
        val doc = myFixture.editor.document
        myFixture.editor.selectionModel.setSelection(
            doc.getLineStartOffset(1),
            doc.getLineEndOffset(2),
        )
        myFixture.testAction(FormatAction())
        assertEquals("100 print \"a\"\nthis is a comment\n300 PRINT \"c\"", myFixture.editor.document.text)
    }

    fun testUnselectedLinesAreNotAffected() {
        myFixture.configureByText("test.tibasic", "100 print \"a\"\n200 print \"b\"\n300 print \"c\"")
        val doc = myFixture.editor.document
        myFixture.editor.selectionModel.setSelection(
            doc.getLineStartOffset(0),
            doc.getLineEndOffset(0),
        )
        myFixture.testAction(FormatAction())
        assertEquals("100 PRINT \"a\"\n200 print \"b\"\n300 print \"c\"", myFixture.editor.document.text)
    }
}
