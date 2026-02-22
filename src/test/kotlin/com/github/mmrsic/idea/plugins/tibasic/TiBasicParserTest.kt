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

    private fun parseCode(code: String): TiBasicFile = createPsiFile("test", code) as TiBasicFile
}



