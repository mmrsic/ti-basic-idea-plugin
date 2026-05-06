package com.github.mmrsic.idea.plugins.tibasic.editor

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TiBasicPairedCharacterTypedHandlerTest : BasePlatformTestCase() {

    fun testTypingLetterAfterLineNumberOnlyInsertsSingleSpace() {
        myFixture.configureByText("test.tibasic", "100<caret>")

        myFixture.type('P')

        myFixture.checkResult("100 P")
        assertEquals("100 P".length, myFixture.editor.caretModel.offset)
    }

    fun testTypingDigitAfterLineNumberOnlyDoesNotInsertSpace() {
        myFixture.configureByText("test.tibasic", "100<caret>")

        myFixture.type('1')

        myFixture.checkResult("1001")
    }

    fun testTypingSpaceAfterLineNumberOnlyDoesNotInsertExtraSpace() {
        myFixture.configureByText("test.tibasic", "100<caret>")

        myFixture.type(' ')

        myFixture.checkResult("100 ")
    }

    fun testTypingLetterAfterLineNumberWithExistingSpaceDoesNotInsertExtraSpace() {
        myFixture.configureByText("test.tibasic", "100 <caret>")

        myFixture.type('P')

        myFixture.checkResult("100 P")
    }

    fun testTypingLetterAfterExistingStatementTextDoesNotInsertSpace() {
        myFixture.configureByText("test.tibasic", "100 P<caret>")

        myFixture.type('R')

        myFixture.checkResult("100 PR")
    }

    fun testTypingLetterAfterLineNumberOnlyInNonTiBasicFileDoesNotInsertSpace() {
        myFixture.configureByText("test.txt", "100<caret>")

        myFixture.type('P')

        myFixture.checkResult("100P")
    }

    fun testTypingOpeningParenCreatesMatchingClosingParen() {
        myFixture.configureByText("test.tibasic", "1130 CALL HCHAR<caret>")

        myFixture.type('(')

        myFixture.checkResult("1130 CALL HCHAR()")
        assertEquals("1130 CALL HCHAR(".length, myFixture.editor.caretModel.offset)
    }

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

    fun testTypingClosingParenSkipsExistingParenInRemLine() {
        myFixture.configureByText("test.tibasic", "100 rem d(5<caret>)")

        myFixture.type(')')

        myFixture.checkResult("100 rem d(5)")
        assertEquals("100 rem d(5)".length, myFixture.editor.caretModel.offset)
    }

    fun testTypingClosingParenInsertsParenInRemLineWhenNoClosingParenExists() {
        myFixture.configureByText("test.tibasic", "100 rem d(5<caret>")

        myFixture.type(')')

        myFixture.checkResult("100 rem d(5)")
    }

    fun testTypingClosingParenSkipsExistingParenInsideStringLiteral() {
        myFixture.configureByText("test.tibasic", "490 INPUT \"Noch einmal? (J/N<caret>)\"\"")

        myFixture.type(')')

        myFixture.checkResult("490 INPUT \"Noch einmal? (J/N)\"\"")
        assertEquals("490 INPUT \"Noch einmal? (J/N)".length, myFixture.editor.caretModel.offset)
    }

    fun testTypingOpeningQuoteCreatesMatchingClosingQuote() {
        myFixture.configureByText("test.tibasic", "100 PRINT <caret>")

        myFixture.type('"')

        myFixture.checkResult("100 PRINT \"\"")
        assertEquals("100 PRINT \"".length, myFixture.editor.caretModel.offset)
    }

    fun testTypingClosingQuoteSkipsExistingQuote() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"<caret>\"")

        myFixture.type('"')

        myFixture.checkResult("100 PRINT \"\"")
        assertEquals("100 PRINT \"\"".length, myFixture.editor.caretModel.offset)
    }

    fun testTypingQuoteInsideStringInsertsEscapedQuotePair() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"A<caret>B\"")

        myFixture.type('"')

        myFixture.checkResult("100 PRINT \"A\"\"B\"")
        assertEquals("100 PRINT \"A\"\"".length, myFixture.editor.caretModel.offset)
    }

    fun testTypingQuoteInOpenStringClosesStringWithSingleQuote() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"A<caret>")

        myFixture.type('"')

        myFixture.checkResult("100 PRINT \"A\"")
        assertEquals("100 PRINT \"A\"".length, myFixture.editor.caretModel.offset)
    }

    fun testBackspaceInEmptyStringDeletesBothQuotes() {
        myFixture.configureByText("test.tibasic", "100 A$=\"<caret>\"")

        myFixture.performEditorAction("EditorBackSpace")

        myFixture.checkResult("100 A$=")
        assertEquals("100 A$=".length, myFixture.editor.caretModel.offset)
    }

    fun testBackspaceBeforeStringContentDeletesOnlyOpeningQuote() {
        myFixture.configureByText("test.tibasic", "100 A$=\"<caret>X\"")

        myFixture.performEditorAction("EditorBackSpace")

        myFixture.checkResult("100 A$=X\"")
    }
}
