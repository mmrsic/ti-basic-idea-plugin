package com.github.mmrsic.idea.plugins.tibasic.parser

import com.github.mmrsic.idea.plugins.tibasic.psi.*
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

    fun testLineNumberAboveMaxIsLineNotComment() {
        val file = parseCode("32768 PRINT")
        assertEquals(1, file.children.filterIsInstance<TiBasicLine>().size)
        assertEquals(0, file.children.filterIsInstance<TiBasicInvalidLine>().size)
    }

    fun testLineNumberWithLeadingZerosIsValidLine() {
        val file = parseCode("0100 PRINT")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        assertEquals(100, lines[0].lineNumber())
    }

    fun testLineNumberManyLeadingZerosIsValidLine() {
        val file = parseCode("000000100 PRINT")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        assertEquals(100, lines[0].lineNumber())
    }

    fun testLineNumberAllZerosIsLine() {
        // 0 is outside 1..32767 → still parsed as TiBasicLine, annotator flags it
        val file = parseCode("000 PRINT")
        assertEquals(1, file.children.filterIsInstance<TiBasicLine>().size)
        assertEquals(0, file.children.filterIsInstance<TiBasicInvalidLine>().size)
    }

    fun testMultipleValidLines() {
        val file = parseCode("100 PRINT \"A\"\n200 PRINT \"B\"")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(2, lines.size)
        assertEquals(100, lines[0].lineNumber())
        assertEquals(200, lines[1].lineNumber())
    }

    fun testInvalidLineProducesInvalidLineNode() {
        val file = parseCode("this is not valid")
        val invalidLines = file.children.filterIsInstance<TiBasicInvalidLine>()
        assertEquals(1, invalidLines.size)
        assertEquals("this is not valid", invalidLines[0].text)
    }

    fun testMixedValidAndInvalidLines() {
        val source = "100 PRINT \"Hello\"\nNOT A LINE\n200 PRINT \"World\""
        val file = parseCode(source)
        val lines = file.children.filterIsInstance<TiBasicLine>()
        val invalidLines = file.children.filterIsInstance<TiBasicInvalidLine>()
        assertEquals(2, lines.size)
        assertEquals(1, invalidLines.size)
        assertEquals("NOT A LINE", invalidLines[0].text)
    }

    fun testRemWithoutTextCreatesRemStatement() {
        val file = parseCode("100 REM")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        val remStatements = lines[0].children.filterIsInstance<TiBasicRemStatement>()
        assertEquals(1, remStatements.size)
    }

    fun testRemWithTextCreatesRemStatement() {
        val file = parseCode("100 REM This is a comment")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        val remStatements = lines[0].children.filterIsInstance<TiBasicRemStatement>()
        assertEquals(1, remStatements.size)
    }

    fun testLineNumberOnlyProducesValidLine() {
        val file = parseCode("100")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        assertEquals(100, lines[0].lineNumber())
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
        assertEquals(0, file.children.filterIsInstance<TiBasicLine>().size)
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

    fun testPrintWithNumericLiteralCreatesExpression() {
        val file = parseCode("100 PRINT 42")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
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

    fun testPrintWithSemicolonSeparatorCreatesTwoExpressions() {
        val file = parseCode("100 PRINT \"a\";\"b\"")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        val expressions = stmt.children.filterIsInstance<TiBasicExpression>()
        assertEquals(2, expressions.size)
        assertEquals("\"a\"", expressions[0].text)
        assertEquals("\"b\"", expressions[1].text)
    }

    fun testPrintWithCommaSeparatorCreatesTwoExpressions() {
        val file = parseCode("100 PRINT A,B")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(2, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testPrintWithColonSeparatorCreatesTwoExpressions() {
        val file = parseCode("100 PRINT X:Y")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(2, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testPrintWithMixedSeparatorsCreatesThreeExpressions() {
        val file = parseCode("100 PRINT \"X=\";X,\" Y=\";Y")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(4, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testPrintWithLeadingSeparatorCreatesOneExpression() {
        val file = parseCode("100 PRINT ,\"RECHTS\"")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testPrintWithTrailingSeparatorCreatesOneExpression() {
        val file = parseCode("100 PRINT \"HALLO\";")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testPrintWithOnlySeparatorsCreatesNoExpression() {
        val file = parseCode("100 PRINT :;,")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(0, stmt.children.filterIsInstance<TiBasicExpression>().size)
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
        // LAST-N$ tokenizes as LAST (numeric var) minus N$ (string var) → 1 expression with mismatch
        val file = parseCode("100 PRINT LAST-N$")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
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
        val file = parseCode("100 PRINT \\$")
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

    fun testStringVariableWithDigitAsFirstCharCreatesTwoExpressions() {
        // 1 is a numeric literal, A$ is a string variable — two separate expressions without separator
        val file = parseCode("100 PRINT 1A$")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(2, stmt.children.filterIsInstance<TiBasicExpression>().size)
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

    fun testStringVariableWithBracketInMiddleCreatesTwoExpressions() {
        // A is a numeric var; [$ is a string variable — two separate expressions without separator
        val file = parseCode("100 PRINT A[$")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(2, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testStringVariableOneDimensionalArrayCreatesExpression() {
        val file = parseCode("100 PRINT A$(1)")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        val expressions = stmt.children.filterIsInstance<TiBasicExpression>()
        assertEquals(1, expressions.size)
        assertEquals("A$(1)", expressions[0].text)
    }

    fun testStringVariableTwoDimensionalArrayCreatesExpression() {
        val file = parseCode("100 PRINT B$(2,3)")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        val expressions = stmt.children.filterIsInstance<TiBasicExpression>()
        assertEquals(1, expressions.size)
        assertEquals("B$(2,3)", expressions[0].text)
    }

    fun testStringVariableThreeDimensionalArrayCreatesExpression() {
        val file = parseCode("100 PRINT C$(4,5,6)")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        val expressions = stmt.children.filterIsInstance<TiBasicExpression>()
        assertEquals(1, expressions.size)
        assertEquals("C$(4,5,6)", expressions[0].text)
    }

    fun testStringVariableArrayWithFourDimensionsIsNotExpression() {
        // 4 subscripts → expression created, annotator flags as "Bad subscript definition"
        val file = parseCode("100 PRINT A$(1,2,3,4)")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testStringVariableArrayWithEmptySubscriptIsNotExpression() {
        // empty subscript → expression created, annotator flags as "Bad subscript definition"
        val file = parseCode("100 PRINT A$()")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testStringVariableArrayWithUnclosedSubscriptIsNotExpression() {
        // unclosed subscript → expression still created (best-effort parsing)
        val file = parseCode("100 PRINT A$(1")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testStringVariableArrayConcatenatedCreatesExpression() {
        val file = parseCode("100 PRINT A$(1) & B$(2,3)")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        val expressions = stmt.children.filterIsInstance<TiBasicExpression>()
        assertEquals(1, expressions.size)
        assertEquals("A$(1) & B$(2,3)", expressions[0].text)
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

    fun testPrintWithIntegerCreatesExpression() {
        val file = parseCode("100 PRINT 3")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testIntegerExpressionText() {
        val file = parseCode("100 PRINT 3")
        val expr = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
            .children.filterIsInstance<TiBasicExpression>()[0]
        assertEquals("3", expr.text)
    }

    fun testPrintWithNegativeRealCreatesExpression() {
        val file = parseCode("100 PRINT -6.783452")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testPrintWithLargeRealCreatesExpression() {
        val file = parseCode("100 PRINT 1258948567.236")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testPrintWithScientificNotationPositiveExponentCreatesExpression() {
        val file = parseCode("100 PRINT 2.36958E15")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testPrintWithScientificNotationNegativeExponentCreatesExpression() {
        val file = parseCode("100 PRINT 8.254689E-12")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testNumericLiteralAndStringLiteralWithInvalidOpCreateTwoExpressions() {
        // 42 & "hello": & is invalid (not a separator), "hello" becomes a second expression
        val file = parseCode("100 PRINT 42 & \"hello\"")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        val exprs = stmt.children.filterIsInstance<TiBasicExpression>()
        assertEquals(2, exprs.size)
        assertEquals("42", exprs[0].text)
    }

    // --- Numeric variable tests ---

    fun testPrintWithSingleLetterNumericVariableCreatesExpression() {
        val file = parseCode("100 PRINT A")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testNumericVariableExpressionText() {
        val file = parseCode("100 PRINT A")
        val expr = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
            .children.filterIsInstance<TiBasicExpression>()[0]
        assertEquals("A", expr.text)
    }

    fun testNumericVariableLowercaseCreatesExpression() {
        val file = parseCode("100 PRINT a")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testNumericVariableWithDigitsInMiddleCreatesExpression() {
        val file = parseCode("100 PRINT A1B2")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testNumericVariableWithAtSignFirstCharCreatesExpression() {
        val file = parseCode("100 PRINT @VAR")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testNumericVariableWithUnderscoreFirstCharCreatesExpression() {
        val file = parseCode("100 PRINT _VAR")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testNumericVariableWithAtSignInMiddleCreatesExpression() {
        val file = parseCode("100 PRINT A@B")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testNumericVariableWithUnderscoreInMiddleCreatesExpression() {
        val file = parseCode("100 PRINT A_B")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testNumericVariableDoesNotContainDollarSign() {
        // A$ is a string variable, not a numeric variable → expression but as STRING_VARIABLE
        val file = parseCode("100 PRINT A$")
        val exprs = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
            .children.filterIsInstance<TiBasicExpression>()
        assertEquals(1, exprs.size)
        assertEquals("A$", exprs[0].text)
    }

    fun testNumericVariableStartingWithDigitCreatesTwoExpressions() {
        // 1 is a numeric literal, A is a separate numeric variable — two expressions without separator
        val file = parseCode("100 PRINT 1A")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(2, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testNumericVariableWithHyphenIsNowSubtraction() {
        // A-B is subtraction → 1 expression
        val file = parseCode("100 PRINT A-B")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testNumericVariableAndStringLiteralWithInvalidOpCreateTwoExpressions() {
        // A & "hello": & is invalid (not a separator), "hello" becomes a second expression
        val file = parseCode("100 PRINT A & \"hello\"")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        val exprs = stmt.children.filterIsInstance<TiBasicExpression>()
        assertEquals(2, exprs.size)
        assertEquals("A", exprs[0].text)
    }

    fun testNumericVariableExactly15CharsCreatesExpression() {
        val file = parseCode("100 PRINT ABCDEFGHIJKLMNO")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testNumericVariableWith16CharsCreatesNoExpression() {
        // 16-char name → INVALID_VARIABLE_NAME, not an expression start
        val file = parseCode("100 PRINT ABCDEFGHIJKLMNOP")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(0, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testNumericVariableWithSubscript1DCreatesExpression() {
        val file = parseCode("100 PRINT A(1)")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testNumericVariableWithSubscript2DCreatesExpression() {
        val file = parseCode("100 PRINT A(1,2)")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testNumericVariableWithSubscript3DCreatesExpression() {
        val file = parseCode("100 PRINT A(1,2,3)")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testNumericVariableWithSubscript4DCreatesExpression() {
        // 4 subscripts → expression created (annotator flags bad subscript)
        val file = parseCode("100 PRINT A(1,2,3,4)")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testNumericVariableWithEmptySubscriptCreatesExpression() {
        // empty subscript → expression created (annotator flags bad subscript)
        val file = parseCode("100 PRINT A()")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testNumericVariableWithSpacesAroundSubscriptCreatesExpression() {
        val file = parseCode("100 PRINT A ( 1 , 2 )")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testNumericVariableSubscriptExpressionText() {
        val file = parseCode("100 PRINT A(1,2)")
        val expr = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
            .children.filterIsInstance<TiBasicExpression>()[0]
        assertEquals("A(1,2)", expr.text)
    }

    // --- Arithmetic operator tests ---

    fun testAdditionCreatesExpression() {
        val file = parseCode("100 PRINT A+B")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testSubtractionCreatesExpression() {
        val file = parseCode("100 PRINT A-B")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testMultiplicationCreatesExpression() {
        val file = parseCode("100 PRINT A*B")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testDivisionCreatesExpression() {
        val file = parseCode("100 PRINT A/B")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testPowerCreatesExpression() {
        val file = parseCode("100 PRINT A^B")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testUnaryMinusCreatesExpression() {
        val file = parseCode("100 PRINT -A")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testUnaryPlusCreatesExpression() {
        val file = parseCode("100 PRINT +A")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testMultipleUnaryPrefixesCreatesExpression() {
        val file = parseCode("100 PRINT +-+-1")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testParenthesizedExpressionCreatesExpression() {
        val file = parseCode("100 PRINT (A+B)")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testComplexArithmeticExpressionCreatesExpression() {
        val file = parseCode("100 PRINT A+B*C-D/E^F")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testArithmeticExpressionText() {
        val file = parseCode("100 PRINT A+B*C")
        val expr = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
            .children.filterIsInstance<TiBasicExpression>()[0]
        assertEquals("A+B*C", expr.text)
    }

    fun testArithmeticExpressionWithSpacesCreatesExpression() {
        val file = parseCode("100 PRINT A + B * C")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testNumericSubscriptWithExpressionCreatesExpression() {
        val file = parseCode("100 PRINT A(B+1)")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testNumericSubscriptWithExpressionText() {
        val file = parseCode("100 PRINT A(B+1)")
        val expr = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
            .children.filterIsInstance<TiBasicExpression>()[0]
        assertEquals("A(B+1)", expr.text)
    }

    fun testEqualityComparisonCreatesExpression() {
        val file = parseCode("100 PRINT A=B")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testLessThanComparisonCreatesExpression() {
        val file = parseCode("100 PRINT A<B")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testGreaterThanComparisonCreatesExpression() {
        val file = parseCode("100 PRINT A>B")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testNotEqualComparisonCreatesExpression() {
        val file = parseCode("100 PRINT A<>B")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testLessOrEqualComparisonCreatesExpression() {
        val file = parseCode("100 PRINT A<=B")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testGreaterOrEqualComparisonCreatesExpression() {
        val file = parseCode("100 PRINT A>=B")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testChainedComparisonIsLeftToRight() {
        val file = parseCode("100 PRINT A=B=C")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testComparisonWithArithmeticCreatesExpression() {
        val file = parseCode("100 PRINT A+B=C-D")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testComparisonWithArithmeticText() {
        val file = parseCode("100 PRINT A+B=C-D")
        val expr = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
            .children.filterIsInstance<TiBasicExpression>()[0]
        assertEquals("A+B=C-D", expr.text)
    }

    fun testComparisonInParenthesesBeforeArithmetic() {
        val file = parseCode("100 PRINT (A=B)+1")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testComparisonWithSpacesCreatesExpression() {
        val file = parseCode("100 PRINT A <= B")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testParenthesizedNumericVarConcatBreaksIntoMultipleExpressions() {
        // (A4&B4)="HI!": A4 and B4 are numeric variables; & stops the numeric parse inside the paren,
        // producing three separate expressions: "(A4", "B4", and "\"HI!\""
        val file = parseCode("100 PRINT (A4&B4)=\"HI!\"")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(3, stmt.children.filterIsInstance<TiBasicExpression>().size)
        val exprs = stmt.children.filterIsInstance<TiBasicExpression>()
        assertEquals("(A4", exprs[0].text)
        assertEquals("B4", exprs[1].text)
    }

    fun testParenthesizedStringVarConcatComparedToStringLiteralCreatesExpression() {
        val file = parseCode("100 PRINT (A$&B$)=\"HI!\"")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
        val expr = stmt.children.filterIsInstance<TiBasicExpression>()[0]
        assertEquals("(A$&B$)=\"HI!\"", expr.text)
    }

    fun testStringVariableComparedToStringLiteralCreatesExpression() {
        val file = parseCode("100 PRINT A$=\"HI!\"")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
        val expr = stmt.children.filterIsInstance<TiBasicExpression>()[0]
        assertEquals("A$=\"HI!\"", expr.text)
    }

    fun testStringVariablesComparedWithNotEqualCreatesExpression() {
        val file = parseCode("100 PRINT A$<>B$")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testStringLiteralsComparedWithLessThanCreatesExpression() {
        val file = parseCode("100 PRINT \"A\"<\"B\"")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testBreakWithoutArgumentIsValid() {
        val file = parseCode("100 BREAK")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        val stmts = lines[0].children.filterIsInstance<TiBasicLineNumberListStatement>()
        assertEquals(1, stmts.size)
    }

    fun testUnbreakWithoutArgumentIsValid() {
        val file = parseCode("100 UNBREAK")
        val stmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicLineNumberListStatement>()
        assertEquals(1, stmts.size)
    }

    fun testTraceWithoutArgumentIsValid() {
        val file = parseCode("100 TRACE")
        val stmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicLineNumberListStatement>()
        assertEquals(1, stmts.size)
    }

    fun testUntraceWithoutArgumentIsValid() {
        val file = parseCode("100 UNTRACE")
        val stmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicLineNumberListStatement>()
        assertEquals(1, stmts.size)
    }

    fun testBreakWithSingleLineNumber() {
        val file = parseCode("100 BREAK 200")
        val stmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicLineNumberListStatement>()
        assertEquals(1, stmts.size)
    }

    fun testBreakWithMultipleLineNumbers() {
        val file = parseCode("100 BREAK 200,300,400")
        val stmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicLineNumberListStatement>()
        assertEquals(1, stmts.size)
    }

    fun testBreakKeywordIsCaseInsensitive() {
        val file = parseCode("100 break 200")
        val stmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicLineNumberListStatement>()
        assertEquals(1, stmts.size)
    }

    fun testBreakDoesNotProducePrintStatement() {
        val file = parseCode("100 BREAK 200")
        val printStmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()
        assertEquals(0, printStmts.size)
    }

    fun testDeleteProducesDeleteStatement() {
        val file = parseCode("100 DELETE \"\"")
        val stmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicDeleteStatement>()
        assertEquals(1, stmts.size)
    }

    fun testDeleteWithStringVariable() {
        val file = parseCode("100 DELETE A$")
        val stmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicDeleteStatement>()
        assertEquals(1, stmts.size)
    }

    fun testDeleteWithoutArgumentProducesDeleteStatement() {
        val file = parseCode("100 DELETE")
        val stmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicDeleteStatement>()
        assertEquals(1, stmts.size)
    }

    fun testDeleteDoesNotProducePrintStatement() {
        val file = parseCode("100 DELETE \"DSK1.STAR\"")
        val printStmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()
        assertEquals(0, printStmts.size)
    }

    fun testDeleteKeywordIsCaseInsensitive() {
        val file = parseCode("100 delete \"\"")
        val stmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicDeleteStatement>()
        assertEquals(1, stmts.size)
    }

    fun testExplicitLetProducesLetStatement() {
        val file = parseCode("100 LET A = 5")
        val stmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicLetStatement>()
        assertEquals(1, stmts.size)
    }

    fun testImplicitLetProducesLetStatement() {
        val file = parseCode("100 A = 5")
        val stmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicLetStatement>()
        assertEquals(1, stmts.size)
    }

    fun testImplicitLetWithStringVariableProducesLetStatement() {
        val file = parseCode("100 A$ = \"hello\"")
        val stmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicLetStatement>()
        assertEquals(1, stmts.size)
    }

    fun testExplicitLetWithSubscriptedVariableProducesLetStatement() {
        val file = parseCode("100 LET A(1) = 5")
        val stmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicLetStatement>()
        assertEquals(1, stmts.size)
    }

    fun testLetKeywordIsCaseInsensitive() {
        val file = parseCode("100 let A = 5")
        val stmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicLetStatement>()
        assertEquals(1, stmts.size)
    }

    fun testLetDoesNotProducePrintStatement() {
        val file = parseCode("100 LET A = 5")
        val printStmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()
        assertEquals(0, printStmts.size)
    }

    fun testEndProducesEndStatement() {
        val file = parseCode("100 END")
        val stmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicEndStatement>()
        assertEquals(1, stmts.size)
    }

    fun testEndKeywordIsCaseInsensitive() {
        val file = parseCode("100 end")
        val stmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicEndStatement>()
        assertEquals(1, stmts.size)
    }

    fun testEndDoesNotProducePrintStatement() {
        val file = parseCode("100 END")
        val printStmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()
        assertEquals(0, printStmts.size)
    }

    fun testEndCanAppearMultipleTimes() {
        val file = parseCode("100 END\n200 END")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(2, lines.size)
        assertEquals(1, lines[0].children.filterIsInstance<TiBasicEndStatement>().size)
        assertEquals(1, lines[1].children.filterIsInstance<TiBasicEndStatement>().size)
    }

    fun testStopProducesStopStatement() {
        val file = parseCode("100 STOP")
        val stmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicStopStatement>()
        assertEquals(1, stmts.size)
    }

    fun testStopKeywordIsCaseInsensitive() {
        val file = parseCode("100 stop")
        val stmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicStopStatement>()
        assertEquals(1, stmts.size)
    }

    fun testStopDoesNotProducePrintStatement() {
        val file = parseCode("100 STOP")
        val printStmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()
        assertEquals(0, printStmts.size)
    }

    fun testStopCanAppearMultipleTimes() {
        val file = parseCode("100 STOP\n200 STOP")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(2, lines.size)
        assertEquals(1, lines[0].children.filterIsInstance<TiBasicStopStatement>().size)
        assertEquals(1, lines[1].children.filterIsInstance<TiBasicStopStatement>().size)
    }

    fun testEndAndStopCanCoexist() {
        val file = parseCode("100 STOP\n200 PRINT \"OK\"\n300 END")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(3, lines.size)
        assertEquals(1, lines[0].children.filterIsInstance<TiBasicStopStatement>().size)
        assertEquals(1, lines[2].children.filterIsInstance<TiBasicEndStatement>().size)
    }

    fun testGotoProducesGotoStatement() {
        val file = parseCode("100 GOTO 200\n200 PRINT \"OK\"")
        val stmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicGotoStatement>()
        assertEquals(1, stmts.size)
    }

    fun testGotoKeywordIsCaseInsensitive() {
        val file = parseCode("100 goto 200\n200 PRINT \"OK\"")
        val stmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicGotoStatement>()
        assertEquals(1, stmts.size)
    }

    fun testGoToTwoWordsProducesGotoStatement() {
        val file = parseCode("100 GO TO 200\n200 PRINT \"OK\"")
        val stmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicGotoStatement>()
        assertEquals(1, stmts.size)
    }

    fun testGoToTwoWordsCaseInsensitive() {
        val file = parseCode("100 go to 200\n200 PRINT \"OK\"")
        val stmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicGotoStatement>()
        assertEquals(1, stmts.size)
    }

    fun testGotoDoesNotProducePrintStatement() {
        val file = parseCode("100 GOTO 200\n200 PRINT \"OK\"")
        val printStmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()
        assertEquals(0, printStmts.size)
    }

    fun testGotoWithoutLineNumberProducesGotoStatement() {
        val file = parseCode("100 GOTO")
        val stmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicGotoStatement>()
        assertEquals(1, stmts.size)
    }

    fun testOnGotoProducesOnGotoStatement() {
        val file = parseCode("100 ON X GOTO 200\n200 PRINT \"OK\"")
        val stmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicOnGotoStatement>()
        assertEquals(1, stmts.size)
    }

    fun testOnGotoKeywordIsCaseInsensitive() {
        val file = parseCode("100 on x goto 200\n200 PRINT \"OK\"")
        val stmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicOnGotoStatement>()
        assertEquals(1, stmts.size)
    }

    fun testOnGoToTwoWordsProducesOnGotoStatement() {
        val file = parseCode("100 ON X GO TO 200\n200 PRINT \"OK\"")
        val stmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicOnGotoStatement>()
        assertEquals(1, stmts.size)
    }

    fun testOnGotoWithMultipleLineNumbersProducesOnGotoStatement() {
        val file = parseCode("100 ON X GOTO 200,300,400\n200 PRINT \"A\"\n300 PRINT \"B\"\n400 PRINT \"C\"")
        val stmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicOnGotoStatement>()
        assertEquals(1, stmts.size)
    }

    fun testOnGotoContainsExpression() {
        val file = parseCode("100 ON X GOTO 200\n200 PRINT \"OK\"")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicOnGotoStatement>()[0]
        val exprs = stmt.children.filterIsInstance<TiBasicExpression>()
        assertEquals(1, exprs.size)
    }

    fun testOnGotoDoesNotProduceGotoStatement() {
        val file = parseCode("100 ON X GOTO 200\n200 PRINT \"OK\"")
        val gotoStmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicGotoStatement>()
        assertEquals(0, gotoStmts.size)
    }

    fun testIfThenProducesIfStatement() {
        val file = parseCode("100 IF X>0 THEN 200\n200 PRINT \"OK\"")
        val stmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicIfStatement>()
        assertEquals(1, stmts.size)
    }

    fun testIfThenElseProducesIfStatement() {
        val file = parseCode("100 IF X>0 THEN 200 ELSE 300\n200 PRINT \"A\"\n300 PRINT \"B\"")
        val stmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicIfStatement>()
        assertEquals(1, stmts.size)
    }

    fun testIfStatementContainsExpression() {
        val file = parseCode("100 IF X>0 THEN 200\n200 PRINT \"OK\"")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicIfStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testIfKeywordIsCaseInsensitive() {
        val file = parseCode("100 if x>0 then 200\n200 PRINT \"OK\"")
        val stmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicIfStatement>()
        assertEquals(1, stmts.size)
    }

    fun testForProducesForStatement() {
        val file = parseCode("100 FOR I = 1 TO 10")
        val line = file.children.filterIsInstance<TiBasicLine>()[0]
        assertEquals(1, line.children.filterIsInstance<TiBasicForStatement>().size)
    }

    fun testForWithStepProducesForStatement() {
        val file = parseCode("100 FOR I = 1 TO 10 STEP 2")
        val line = file.children.filterIsInstance<TiBasicLine>()[0]
        assertEquals(1, line.children.filterIsInstance<TiBasicForStatement>().size)
    }

    fun testForContainsVariableAccess() {
        val file = parseCode("100 FOR I = 1 TO 10")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicForStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicVariableAccess>().size)
    }

    fun testForContainsTwoExpressions() {
        val file = parseCode("100 FOR I = 1 TO 10")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicForStatement>()[0]
        assertEquals(2, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testForWithStepContainsThreeExpressions() {
        val file = parseCode("100 FOR I = 1 TO 10 STEP 2")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicForStatement>()[0]
        assertEquals(3, stmt.children.filterIsInstance<TiBasicExpression>().size)
    }

    fun testForKeywordIsCaseInsensitive() {
        val file = parseCode("100 for i = 1 to 10")
        val line = file.children.filterIsInstance<TiBasicLine>()[0]
        assertEquals(1, line.children.filterIsInstance<TiBasicForStatement>().size)
    }

    fun testForDoesNotProducePrintStatement() {
        val file = parseCode("100 FOR I = 1 TO 10")
        val line = file.children.filterIsInstance<TiBasicLine>()[0]
        assertEquals(0, line.children.filterIsInstance<TiBasicPrintStatement>().size)
    }

    fun testNextProducesNextStatement() {
        val file = parseCode("100 NEXT I")
        val line = file.children.filterIsInstance<TiBasicLine>()[0]
        assertEquals(1, line.children.filterIsInstance<TiBasicNextStatement>().size)
    }

    fun testNextContainsVariableAccess() {
        val file = parseCode("100 NEXT I")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicNextStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicVariableAccess>().size)
    }

    fun testNextKeywordIsCaseInsensitive() {
        val file = parseCode("100 next i")
        val line = file.children.filterIsInstance<TiBasicLine>()[0]
        assertEquals(1, line.children.filterIsInstance<TiBasicNextStatement>().size)
    }

    fun testNextDoesNotProducePrintStatement() {
        val file = parseCode("100 NEXT I")
        val line = file.children.filterIsInstance<TiBasicLine>()[0]
        assertEquals(0, line.children.filterIsInstance<TiBasicPrintStatement>().size)
    }

    fun testForAndNextInFile() {
        val file = parseCode("100 FOR I = 1 TO 10\n200 NEXT I")
        assertEquals(1, file.forStatements().size)
        assertEquals(1, file.nextStatements().size)
    }

    fun testForWithLeadingDotInitialValue() {
        val file = parseCode("110 FOR X = .1 TO 1")
        val stmts = file.forStatements()
        assertEquals(1, stmts.size)
    }

    fun testForWithLeadingDotStepValue() {
        val file = parseCode("110 FOR X = .1 TO 1 STEP .2")
        val stmts = file.forStatements()
        assertEquals(1, stmts.size)
    }

    fun testInputStatementWithSingleVariable() {
        val file = parseCode("100 INPUT A")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        val stmts = lines[0].children.filterIsInstance<TiBasicInputStatement>()
        assertEquals(1, stmts.size)
    }

    fun testInputStatementWithMultipleVariables() {
        val file = parseCode("100 INPUT A,B,C")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        val stmts = lines[0].children.filterIsInstance<TiBasicInputStatement>()
        assertEquals(1, stmts.size)
    }

    fun testInputStatementWithStringVariable() {
        val file = parseCode("100 INPUT A$")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        val stmts = lines[0].children.filterIsInstance<TiBasicInputStatement>()
        assertEquals(1, stmts.size)
    }

    fun testInputStatementWithPrompt() {
        val file = parseCode("100 INPUT \"Enter value\": A")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        val stmts = lines[0].children.filterIsInstance<TiBasicInputStatement>()
        assertEquals(1, stmts.size)
    }

    fun testInputStatementWithPromptAndMultipleVariables() {
        val file = parseCode("100 INPUT \"Name: \": A$,B")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        val stmts = lines[0].children.filterIsInstance<TiBasicInputStatement>()
        assertEquals(1, stmts.size)
    }

    fun testInputStatementLowercaseIsRecognized() {
        val file = parseCode("100 input a,b$")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        val stmts = lines[0].children.filterIsInstance<TiBasicInputStatement>()
        assertEquals(1, stmts.size)
    }

    fun testReadStatementWithSingleVariable() {
        val file = parseCode("100 READ A")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        val stmts = lines[0].children.filterIsInstance<TiBasicReadStatement>()
        assertEquals(1, stmts.size)
    }

    fun testReadStatementWithMultipleVariables() {
        val file = parseCode("100 READ A,B,C")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        val stmts = lines[0].children.filterIsInstance<TiBasicReadStatement>()
        assertEquals(1, stmts.size)
    }

    fun testReadStatementWithMixedVariables() {
        val file = parseCode("100 READ A,B$,C")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        val stmts = lines[0].children.filterIsInstance<TiBasicReadStatement>()
        assertEquals(1, stmts.size)
    }

    fun testReadStatementLowercaseIsRecognized() {
        val file = parseCode("100 read a,b$")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        val stmts = lines[0].children.filterIsInstance<TiBasicReadStatement>()
        assertEquals(1, stmts.size)
    }

    fun testDataStatementWithNumericItem() {
        val file = parseCode("100 DATA 42")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        assertEquals(1, lines[0].children.filterIsInstance<TiBasicDataStatement>().size)
    }

    fun testDataStatementWithStringLiteral() {
        val file = parseCode("100 DATA \"hello\"")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        assertEquals(1, lines[0].children.filterIsInstance<TiBasicDataStatement>().size)
    }

    fun testDataStatementWithMixedItems() {
        val file = parseCode("100 DATA 1,\"two\",three")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        assertEquals(1, lines[0].children.filterIsInstance<TiBasicDataStatement>().size)
    }

    fun testDataStatementWithEmptyItemsFromConsecutiveCommas() {
        val file = parseCode("100 DATA 1,,3")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        assertEquals(1, lines[0].children.filterIsInstance<TiBasicDataStatement>().size)
    }

    fun testDataStatementLowercaseIsRecognized() {
        val file = parseCode("100 data 42")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        assertEquals(1, lines[0].children.filterIsInstance<TiBasicDataStatement>().size)
    }

    fun testRestoreStatementWithNoArgument() {
        val file = parseCode("100 RESTORE")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        assertEquals(1, lines[0].children.filterIsInstance<TiBasicRestoreStatement>().size)
    }

    fun testRestoreStatementWithLineNumber() {
        val file = parseCode("100 RESTORE 200")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        assertEquals(1, lines[0].children.filterIsInstance<TiBasicRestoreStatement>().size)
    }

    fun testRestoreStatementLowercaseIsRecognized() {
        val file = parseCode("100 restore 200")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        assertEquals(1, lines[0].children.filterIsInstance<TiBasicRestoreStatement>().size)
    }

    fun testPrintWithTabFunctionCreatesTabFunction() {
        val file = parseCode("100 PRINT TAB(5);\"TEXT\"")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicTabFunction>().size)
    }

    fun testPrintWithTabFunctionAloneIsValid() {
        val file = parseCode("100 PRINT TAB(5)")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicTabFunction>().size)
    }

    fun testPrintWithMultipleTabFunctionsCreatesMultipleTabFunctions() {
        val file = parseCode("100 PRINT TAB(5);\"A\";TAB(10);\"B\"")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(2, stmt.children.filterIsInstance<TiBasicTabFunction>().size)
    }

    fun testTabFunctionContainsExpression() {
        val file = parseCode("100 PRINT TAB(5)")
        val tab = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
            .children.filterIsInstance<TiBasicTabFunction>()[0]
        assertEquals(1, tab.children.filterIsInstance<TiBasicExpression>().size)
        assertEquals("5", tab.children.filterIsInstance<TiBasicExpression>()[0].text)
    }

    fun testTabWithVariableExpressionCreatesTabFunction() {
        val file = parseCode("100 PRINT TAB(N)")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicTabFunction>().size)
    }

    fun testTabWithoutParenthesesInPrintCreatesTabFunction() {
        val file = parseCode("100 PRINT TAB")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicPrintStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicTabFunction>().size)
    }

    fun testDisplayStatementWithStringLiteralIsParsed() {
        val file = parseCode("100 DISPLAY \"HELLO\"")
        val stmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicDisplayStatement>()
        assertEquals(1, stmts.size)
    }

    fun testDisplayStatementWithNoArgumentIsParsed() {
        val file = parseCode("100 DISPLAY")
        val stmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicDisplayStatement>()
        assertEquals(1, stmts.size)
    }

    fun testDisplayStatementWithMultipleArgumentsIsParsed() {
        val file = parseCode("100 DISPLAY \"A\";\"B\"")
        val stmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicDisplayStatement>()
        assertEquals(1, stmts.size)
    }

    fun testDisplayStatementWithTabFunctionIsParsed() {
        val file = parseCode("100 DISPLAY TAB(5);\"TEXT\"")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicDisplayStatement>()[0]
        assertEquals(1, stmt.children.filterIsInstance<TiBasicTabFunction>().size)
    }

    fun testDisplayStatementLowercaseIsParsed() {
        val file = parseCode("100 display \"HELLO\"")
        val stmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicDisplayStatement>()
        assertEquals(1, stmts.size)
    }

    private fun parseCode(code: String): TiBasicFile = createPsiFile("test", code) as TiBasicFile
}



