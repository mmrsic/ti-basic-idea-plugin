package com.github.mmrsic.idea.plugins.tibasic

import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicCommentLine
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicExpression
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicLine
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicPrintStatement
import com.intellij.testFramework.ParsingTestCase

class TiBasicParserTest : ParsingTestCase("", "tibasic", TiBasicParserDefinition()) {

    override fun getTestDataPath(): String = "src/test/testData"

    override fun skipSpaces(): Boolean = true

    fun testSingleValidLine() {
        val file = parseCode("100 PRINT \"Hello\"")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        assertEquals(100, lines[0].lineNumber())
        val stmts = lines[0].children.filterIsInstance<TiBasicPrintStatement>()
        assertEquals(1, stmts.size)
    }

    fun testLineNumberBoundaryMin() {
        val file = parseCode("1 PRINT")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        assertEquals(1, lines[0].lineNumber())
    }

    fun testLineNumberBoundaryMax() {
        val file = parseCode("32767 PRINT")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        assertEquals(32767, lines[0].lineNumber())
    }

    fun testLineNumberAboveMaxIsComment() {
        val file = parseCode("32768 PRINT")
        val comments = file.children.filterIsInstance<TiBasicCommentLine>()
        assertEquals(1, comments.size)
        assertEquals("32768 PRINT", comments[0].commentText())
    }

    fun testMultipleValidLines() {
        val file = parseCode("100 PRINT \"A\"\n200 PRINT \"B\"")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(2, lines.size)
        assertEquals(100, lines[0].lineNumber())
        assertEquals(200, lines[1].lineNumber())
    }

    fun testInvalidLineIsComment() {
        val file = parseCode("this is not valid")
        val comments = file.children.filterIsInstance<TiBasicCommentLine>()
        assertEquals(1, comments.size)
        assertEquals("this is not valid", comments[0].commentText())
    }

    fun testMixedValidAndCommentLines() {
        val source = "100 PRINT \"Hello\"\nNOT A LINE\n200 PRINT \"World\""
        val file = parseCode(source)
        val lines = file.children.filterIsInstance<TiBasicLine>()
        val comments = file.children.filterIsInstance<TiBasicCommentLine>()
        assertEquals(2, lines.size)
        assertEquals(1, comments.size)
        assertEquals("NOT A LINE", comments[0].commentText())
    }

    fun testPrintWithoutArgumentIsValid() {
        val file = parseCode("500 PRINT")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        assertEquals(500, lines[0].lineNumber())
    }

    fun testPrintKeywordIsCaseInsensitive() {
        val file = parseCode("100 print \"hello\"")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
    }

    fun testPrintWithMultipleWhitespaces() {
        val file = parseCode("100   PRINT   \"hello\"")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
    }

    fun testLeadingWhitespaceIsIgnoredForValidLine() {
        val file = parseCode("   100 PRINT")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        assertEquals(100, lines[0].lineNumber())
    }

    fun testTrailingWhitespaceIsIgnoredForValidLine() {
        val file = parseCode("100 PRINT   ")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        assertEquals(100, lines[0].lineNumber())
    }

    fun testLeadingAndTrailingWhitespaceIsIgnoredForValidLine() {
        val file = parseCode("\t100 PRINT \"hello\" \t")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        assertEquals(100, lines[0].lineNumber())
    }

    fun testEmptyFileProducesNoNodes() {
        val file = parseCode("")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        val comments = file.children.filterIsInstance<TiBasicCommentLine>()
        assertEquals(0, lines.size)
        assertEquals(0, comments.size)
    }

    fun testPrintWithStringLiteralCreatesExpression() {
        val file = parseCode("100 PRINT \"hello\"")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testExpressionContainsStringLiteralText() {
        val file = parseCode("100 PRINT \"hello\"")
        val expr = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
            .children.filterIsInstance<TiBasicExpression>()[0]
        assertEquals("\"hello\"", expr.text)
    }

    fun testPrintWithNonStringArgumentProducesNoPrintExpression() {
        val file = parseCode("100 PRINT 42")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(0, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testPrintWithMultipleStringsJoinedByConcatOpCreatesExpression() {
        val file = parseCode("100 PRINT \"a\" & \"b\"")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testExpressionTextIncludesAllConcatenatedParts() {
        val file = parseCode("100 PRINT \"a\" & \"b\"")
        val expr = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
            .children.filterIsInstance<TiBasicExpression>()[0]
        assertEquals("\"a\" & \"b\"", expr.text)
    }

    fun testConcatenationOfThreeStringsCreatesExpression() {
        val file = parseCode("100 PRINT \"a\" & \"b\" & \"c\"")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testConcatenationWithoutSpacesCreatesExpression() {
        val file = parseCode("100 PRINT \"a\"&\"b\"")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testTrailingConcatOpProducesNoFullExpression() {
        // "a" & has no right-hand operand → rollback; EXPRESSION contains only "a"
        val file = parseCode("100 PRINT \"a\" &")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        val expressions = stmt.children.filterIsInstance<TiBasicExpression>()
        assertEquals(1, expressions.size)
        assertEquals("\"a\"", expressions[0].text)
    }

    fun testPrintWithInvalidSeparatorCreatesPartialExpression() {
        // "a";"b" – semicolon is not & → EXPRESSION for "a" only
        val file = parseCode("100 PRINT \"a\";\"b\"")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        val expressions = stmt.children.filterIsInstance<TiBasicExpression>()
        assertEquals(1, expressions.size)
        assertEquals("\"a\"", expressions[0].text)
    }

    fun testPrintWithEmptyStringLiteralCreatesExpression() {
        val file = parseCode("100 PRINT \"\"")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testPrintWithStringContainingEscapedQuoteCreatesExpression() {
        val file = parseCode("100 PRINT \"say \"\"hi\"\"\"")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testPrintWithUnclosedStringIsNotExpression() {
        val file = parseCode("100 PRINT \"unclosed")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(0, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testPrintWithStringVariableCreatesExpression() {
        val file = parseCode("100 PRINT Y$")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testStringVariableExpressionText() {
        val file = parseCode("100 PRINT Y$")
        val expr = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
            .children.filterIsInstance<TiBasicExpression>()[0]
        assertEquals("Y$", expr.text)
    }

    fun testPrintWithLongStringVariableNameCreatesExpression() {
        val file = parseCode("100 PRINT STATES$")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testStringVariableWithHyphenInNameIsNotExpression() {
        val file = parseCode("100 PRINT LAST-N$")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(0, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testStringVariableConcatenatedWithLiteralCreatesExpression() {
        val file = parseCode("100 PRINT Y$ & \"hello\"")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        val expressions = stmt.children.filterIsInstance<TiBasicExpression>()
        assertEquals(1, expressions.size)
        assertEquals("Y$ & \"hello\"", expressions[0].text)
    }

    fun testStringLiteralConcatenatedWithVariableCreatesExpression() {
        val file = parseCode("100 PRINT \"hello\" & Y$")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        val expressions = stmt.children.filterIsInstance<TiBasicExpression>()
        assertEquals(1, expressions.size)
        assertEquals("\"hello\" & Y$", expressions[0].text)
    }

    fun testTooLongStringVariableNameIsNotExpression() {
        // 16 chars including $ (TOOLONGVARIABLE = 15 letters + $) → PRINT_ARGUMENT, no EXPRESSION
        val file = parseCode("100 PRINT TOOLONGVARIABLE$")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(0, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testStringVariableWithDigitsInNameCreatesExpression() {
        val file = parseCode("100 PRINT A1B2$")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testStringVariableWithAtSignAsFirstCharCreatesExpression() {
        val file = parseCode("100 PRINT @$")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testStringVariableWithUnderscoreAsFirstCharCreatesExpression() {
        val file = parseCode("100 PRINT _$")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testStringVariableWithLeftBracketAsFirstCharCreatesExpression() {
        val file = parseCode("100 PRINT [$")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testStringVariableWithRightBracketAsFirstCharCreatesExpression() {
        val file = parseCode("100 PRINT ]$")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testStringVariableWithBackslashAsFirstCharCreatesExpression() {
        val file = parseCode("100 PRINT \\\$")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testStringVariableWithAtSignInMiddleCreatesExpression() {
        val file = parseCode("100 PRINT A@B$")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testStringVariableWithUnderscoreInMiddleCreatesExpression() {
        val file = parseCode("100 PRINT A_B$")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testStringVariableAtMaxLengthCreatesExpression() {
        // 14 uppercase letters + $ = 15 chars total (maximum allowed)
        val file = parseCode("100 PRINT ABCDEFGHIJKLMN$")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testStringVariableWithLowercaseFirstCharCreatesExpression() {
        val file = parseCode("100 PRINT a$")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testStringVariableWithDigitAsFirstCharIsNotExpression() {
        val file = parseCode("100 PRINT 1A$")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(0, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testStringVariableWithLowercaseInMiddleCreatesExpression() {
        val file = parseCode("100 PRINT Aa$")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testStringVariableWithDigitInMiddleCreatesExpression() {
        val file = parseCode("100 PRINT A1$")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testStringVariableWithBracketInMiddleIsNotExpression() {
        // [ and ] are only valid as first character, not within the name
        val file = parseCode("100 PRINT A[$")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(0, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testStringVariableOneDimensionalArrayCreatesExpression() {
        val file = parseCode("100 PRINT A$(1)")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        val expressions = stmt.children.filterIsInstance<TiBasicExpression>()
        assertEquals(1, expressions.size)
        assertEquals("A\$(1)", expressions[0].text)
    }

    fun testStringVariableTwoDimensionalArrayCreatesExpression() {
        val file = parseCode("100 PRINT B$(2,3)")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        val expressions = stmt.children.filterIsInstance<TiBasicExpression>()
        assertEquals(1, expressions.size)
        assertEquals("B\$(2,3)", expressions[0].text)
    }

    fun testStringVariableThreeDimensionalArrayCreatesExpression() {
        val file = parseCode("100 PRINT C$(4,5,6)")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        val expressions = stmt.children.filterIsInstance<TiBasicExpression>()
        assertEquals(1, expressions.size)
        assertEquals("C\$(4,5,6)", expressions[0].text)
    }

    fun testStringVariableArrayWithFourDimensionsIsNotExpression() {
        val file = parseCode("100 PRINT A$(1,2,3,4)")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(0, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testStringVariableArrayWithEmptySubscriptIsNotExpression() {
        val file = parseCode("100 PRINT A$()")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(0, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testStringVariableArrayWithUnclosedSubscriptIsNotExpression() {
        val file = parseCode("100 PRINT A$(1")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(0, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testStringVariableArrayConcatenatedCreatesExpression() {
        val file = parseCode("100 PRINT A$(1) & B$(2,3)")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        val expressions = stmt.children.filterIsInstance<TiBasicExpression>()
        assertEquals(1, expressions.size)
        assertEquals("A\$(1) & B\$(2,3)", expressions[0].text)
    }

    fun testStringVariableArrayWithSpacesAroundParenCreatesExpression() {
        val file = parseCode("100 PRINT A$ ( 1 )")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testStringVariableArrayWithSpacesAroundCommasCreatesExpression() {
        val file = parseCode("100 PRINT B$ ( 2 , 3 )")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testStringVariableThreeDimensionalArrayWithSpacesCreatesExpression() {
        val file = parseCode("100 PRINT C$ ( 4 , 5 , 6 )")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testStringVariableArrayWithSpaceBeforeParenStillConcatenates() {
        val file = parseCode("100 PRINT A$ ( 1 ) & B$ ( 2 , 3 )")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    private fun parseCode(code: String): TiBasicFile = createPsiFile("test", code) as TiBasicFile
}



