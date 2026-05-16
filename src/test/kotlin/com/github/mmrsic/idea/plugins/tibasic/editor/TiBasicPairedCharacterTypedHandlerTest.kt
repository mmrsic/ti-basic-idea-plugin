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

    fun testTypingLetterAfterNumericLiteralInStatementInsertsSpace() {
        myFixture.configureByText("test.tibasic", "100 IF X=10<caret>")

        myFixture.type('T')

        myFixture.checkResult("100 IF X=10 T")
        assertEquals("100 IF X=10 T".length, myFixture.editor.caretModel.offset)
    }

    fun testTypingExponentMarkerAfterNumericLiteralDoesNotInsertSpace() {
        myFixture.configureByText("test.tibasic", "100 LET X=1<caret>")

        myFixture.type('E')

        myFixture.checkResult("100 LET X=1E")
    }

    fun testTypingDecimalSeparatorAfterNumericLiteralDoesNotInsertSpace() {
        myFixture.configureByText("test.tibasic", "100 LET X=1<caret>")

        myFixture.type('.')

        myFixture.checkResult("100 LET X=1.")
    }

    fun testTypingOperatorAfterNumericLiteralInsertsSpace() {
        myFixture.configureByText("test.tibasic", "100 LET X=10<caret>")

        myFixture.type('+')

        myFixture.checkResult("100 LET X=10 +")
        assertEquals("100 LET X=10 +".length, myFixture.editor.caretModel.offset)
    }

    fun testTypingOperatorAfterLineNumberOnlyInsertsSingleSpace() {
        myFixture.configureByText("test.tibasic", "100<caret>")

        myFixture.type('+')

        myFixture.checkResult("100 +")
        assertEquals("100 +".length, myFixture.editor.caretModel.offset)
    }

    fun testTypingExponentMarkerAfterReferencedLineNumberInsertsSpace() {
        myFixture.configureByText("test.tibasic", "560 IF K=74 THEN 570<caret>")

        myFixture.type('E')

        myFixture.checkResult("560 IF K=74 THEN 570 E")
        assertEquals("560 IF K=74 THEN 570 E".length, myFixture.editor.caretModel.offset)
    }

    fun testTypingLetterAfterDigitInVariableNameDoesNotInsertSpace() {
        myFixture.configureByText("test.tibasic", "100 LET A1<caret>=2")

        myFixture.type('B')

        myFixture.checkResult("100 LET A1B=2")
    }

    fun testTypingLetterAfterDigitInsideStringLiteralDoesNotInsertSpace() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"1<caret>\"")

        myFixture.type('A')

        myFixture.checkResult("100 PRINT \"1A\"")
    }

    fun testTypingLetterAfterDigitInDataLineDoesNotInsertSpace() {
        myFixture.configureByText("test.tibasic", "100 DATA 1<caret>")

        myFixture.type('A')

        myFixture.checkResult("100 DATA 1A")
    }

    fun testTypingLetterAfterDigitInRemLineDoesNotInsertSpace() {
        myFixture.configureByText("test.tibasic", "100 REM 1<caret>")

        myFixture.type('A')

        myFixture.checkResult("100 REM 1A")
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

    fun testTypingNumericStringCharacterTriggerReplacesSequenceWithCharacterCode() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"\\15<caret>\"")

        myFixture.type('1')

        myFixture.checkResult(printLineWithCharacterCode(151))
    }

    fun testTypingThreeDigitAsciiTriggerReplacesSequenceWithAsciiCharacter() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"\\06<caret>\"")

        myFixture.type('5')

        myFixture.checkResult("100 PRINT \"A\"")
    }

    fun testTypingThreeDigitQuoteTriggerInsertsEscapedQuoteInStringLiteral() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"\\03<caret>\"")

        myFixture.type('4')

        myFixture.checkResult("100 PRINT \"\"\"\"")
    }

    fun testTypingThreeDigitControlTriggerInsertsRawControlCharacter() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"\\00<caret>\"")

        myFixture.type('1')

        myFixture.checkResult(printLineWithCharacterCode(1))
    }

    fun testTypingControlAcronymAliasesCoverFullControlKeyTable() {
        controlAliasMappings().forEach { (key, code) ->
            myFixture.configureByText("test.tibasic", "100 PRINT \"\\C<caret>\"")

            myFixture.type(key)

            myFixture.checkResult(printLineWithCharacterCode(code))
        }
    }

    fun testTypingControlAcronymAliasReplacesSequenceWithCharacterCode() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"\\C<caret>\"")

        myFixture.type('W')

        myFixture.checkResult(printLineWithCharacterCode(151))
    }

    fun testTypingCaretControlAliasReplacesSequenceWithCharacterCode() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"\\^<caret>\"")

        myFixture.type('w')

        myFixture.checkResult(printLineWithCharacterCode(151))
    }

    fun testTypingLongControlAliasReplacesSequenceWithCharacterCode() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"\\CTRL-<caret>\"")

        myFixture.type('W')

        myFixture.checkResult(printLineWithCharacterCode(151))
    }

    fun testTypingFunctionAcronymAliasReplacesSequenceWithCharacterCode() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"\\F<caret>\"")

        myFixture.type('S')

        myFixture.checkResult(printLineWithCharacterCode(8))
    }

    fun testTypingLongFunctionAliasReplacesSequenceWithCharacterCode() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"\\FCTN-<caret>\"")

        myFixture.type('7')

        myFixture.checkResult(printLineWithCharacterCode(1))
    }

    fun testTypingLongFunctionAliasesCoverRequestedCodesExceptEnter() {
        functionAliasMappingsWithoutEnter().forEach { (key, code) ->
            myFixture.configureByText("test.tibasic", "100 PRINT \"\\FCTN-<caret>\"")

            myFixture.type(key)

            myFixture.checkResult(printLineWithCharacterCode(code))
        }
    }

    fun testTypingControlAtAliasReplacesSequenceWithCharacterCode128() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"\\CTRL-<caret>\"")

        myFixture.type('@')

        myFixture.checkResult(printLineWithCharacterCode(128))
    }

    fun testTypingControlVAliasReplacesSequenceWithCharacterCode150() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"\\C<caret>\"")

        myFixture.type('V')

        myFixture.checkResult(printLineWithCharacterCode(150))
    }

    fun testTypingControlWAliasReplacesSequenceWithCharacterCode151() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"\\C<caret>\"")

        myFixture.type('W')

        myFixture.checkResult(printLineWithCharacterCode(151))
    }

    fun testTypingControlSlashAliasReplacesSequenceWithCharacterCode187() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"\\C<caret>\"")

        myFixture.type('/')

        myFixture.checkResult(printLineWithCharacterCode(187))
    }

    fun testTypingUnsupportedControlAliasInsideStringKeepsLiteralText() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"\\CTRL-<caret>\"")

        myFixture.type('?')

        myFixture.checkResult("100 PRINT \"\\CTRL-?\"")
    }

    fun testTypingNumericStringCharacterTriggerOutsideStringKeepsLiteralText() {
        myFixture.configureByText("test.tibasic", "100 PRINT \\15<caret>")

        myFixture.type('1')

        myFixture.checkResult("100 PRINT \\151")
    }

    fun testTypingTwoDigitNumericPrefixInsideStringDoesNotReplaceEarly() {
        myFixture.configureByText("test.tibasic", "100 PRINT \"\\0<caret>\"")

        myFixture.type('6')

        myFixture.checkResult("100 PRINT \"\\06\"")
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

    private fun printLineWithCharacterCode(code: Int): String = buildString {
        append("100 PRINT \"")
        append(code.toChar())
        append('"')
    }

    private fun controlAliasMappings(): List<Pair<Char, Int>> = buildList {
        add('@' to 128)
        ('A'..'Z').forEachIndexed { index, char ->
            add(char to (129 + index))
        }
        add('.' to 155)
        add(';' to 156)
        add('=' to 157)
        add('8' to 158)
        add('9' to 159)
        add('/' to 187)
    }

    private fun functionAliasMappingsWithoutEnter(): List<Pair<Char, Int>> = listOf(
        '7' to 1,
        '4' to 2,
        '1' to 3,
        '2' to 4,
        '=' to 5,
        '8' to 6,
        '3' to 7,
        'S' to 8,
        'D' to 9,
        'X' to 10,
        'E' to 11,
        '6' to 12,
        '5' to 14,
        '9' to 15,
    )
}
