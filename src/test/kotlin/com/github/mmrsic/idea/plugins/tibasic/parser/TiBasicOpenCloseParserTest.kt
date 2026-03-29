package com.github.mmrsic.idea.plugins.tibasic.parser

import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.psi.*
import com.intellij.testFramework.ParsingTestCase

class TiBasicOpenCloseParserTest : ParsingTestCase("", "tibasic", TiBasicParserDefinition()) {

    override fun getTestDataPath(): String = "src/test/testData"

    override fun skipSpaces(): Boolean = true

    private fun parseCode(code: String): TiBasicFile = createPsiFile("test", code) as TiBasicFile

    private fun parseSingleOpenStatement(code: String): TiBasicOpenStatement {
        val lines = parseCode(code).children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        val statements = lines[0].children.filterIsInstance<TiBasicOpenStatement>()
        assertEquals(1, statements.size)
        return statements[0]
    }

    fun testOpenWithLiteralFileNumberAndStringLiteral() {
        val file = parseCode("100 OPEN #1:\"DSK1.FILE\"")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        assertEquals(1, lines[0].children.filterIsInstance<TiBasicOpenStatement>().size)
    }

    fun testCloseWithLiteralFileNumber() {
        val file = parseCode("100 CLOSE #1")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        assertEquals(1, lines[0].children.filterIsInstance<TiBasicCloseStatement>().size)
    }

    fun testCloseWithDeleteModifier() {
        val file = parseCode("100 CLOSE #1:DELETE")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        val stmt = lines[0].children.filterIsInstance<TiBasicCloseStatement>().single()
        assertTrue(stmt.hasDeleteModifier())
    }

    fun testCloseWithoutDeleteModifierHasNoDeleteFlag() {
        val file = parseCode("100 CLOSE #1")
        val stmt = file.children.filterIsInstance<TiBasicLine>().single()
            .children.filterIsInstance<TiBasicCloseStatement>().single()
        assertFalse(stmt.hasDeleteModifier())
    }

    fun testCloseWithDeleteModifierAndVariableFileNumber() {
        val file = parseCode("100 CLOSE #N:DELETE")
        val stmt = file.children.filterIsInstance<TiBasicLine>().single()
            .children.filterIsInstance<TiBasicCloseStatement>().single()
        assertTrue(stmt.hasDeleteModifier())
    }

    fun testOpenWithVariableFileNumberAndStringVariable() {
        val file = parseCode("100 OPEN #N:F\$")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        assertEquals(1, lines[0].children.filterIsInstance<TiBasicOpenStatement>().size)
    }

    fun testOpenWithExpressionFileNumber() {
        val file = parseCode("100 OPEN #(A+1):\"FILE\"")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        assertEquals(1, lines[0].children.filterIsInstance<TiBasicOpenStatement>().size)
    }

    fun testCloseWithExpressionFileNumber() {
        val file = parseCode("100 CLOSE #(N-1)")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(1, lines.size)
        assertEquals(1, lines[0].children.filterIsInstance<TiBasicCloseStatement>().size)
    }

    fun testOpenAndCloseOnSeparateLines() {
        val file = parseCode("100 OPEN #1:\"FILE\"\n200 CLOSE #1")
        val lines = file.children.filterIsInstance<TiBasicLine>()
        assertEquals(2, lines.size)
        assertEquals(1, lines[0].children.filterIsInstance<TiBasicOpenStatement>().size)
        assertEquals(1, lines[1].children.filterIsInstance<TiBasicCloseStatement>().size)
    }

    fun testOpenWithSequential() {
        val stmt = parseSingleOpenStatement("100 OPEN #1:\"FILE\",SEQUENTIAL")
        val options = stmt.options()
        assertEquals(1, options.size)
        assertEquals(TiBasicTokenTypes.SEQUENTIAL_KEYWORD, options[0].optionKeywordType())
        assertNull(options[0].optionExpression())
    }

    fun testOpenWithSequentialAndRecordCount() {
        val stmt = parseSingleOpenStatement("100 OPEN #1:\"FILE\",SEQUENTIAL 50")
        val options = stmt.options()
        assertEquals(1, options.size)
        assertEquals(TiBasicTokenTypes.SEQUENTIAL_KEYWORD, options[0].optionKeywordType())
        assertNotNull(options[0].optionExpression())
    }

    fun testOpenWithRelative() {
        val stmt = parseSingleOpenStatement("100 OPEN #1:\"FILE\",RELATIVE")
        val options = stmt.options()
        assertEquals(1, options.size)
        assertEquals(TiBasicTokenTypes.RELATIVE_KEYWORD, options[0].optionKeywordType())
        assertNull(options[0].optionExpression())
    }

    fun testOpenWithRelativeAndRecordCount() {
        val stmt = parseSingleOpenStatement("100 OPEN #1:\"FILE\",RELATIVE 100")
        val options = stmt.options()
        assertEquals(1, options.size)
        assertEquals(TiBasicTokenTypes.RELATIVE_KEYWORD, options[0].optionKeywordType())
        assertNotNull(options[0].optionExpression())
    }

    fun testOpenWithDisplay() {
        val stmt = parseSingleOpenStatement("100 OPEN #1:\"FILE\",DISPLAY")
        val options = stmt.options()
        assertEquals(1, options.size)
        assertEquals(TiBasicTokenTypes.DISPLAY_KEYWORD, options[0].optionKeywordType())
    }

    fun testOpenWithInternal() {
        val stmt = parseSingleOpenStatement("100 OPEN #1:\"FILE\",INTERNAL")
        val options = stmt.options()
        assertEquals(1, options.size)
        assertEquals(TiBasicTokenTypes.INTERNAL_KEYWORD, options[0].optionKeywordType())
    }

    fun testOpenWithInput() {
        val stmt = parseSingleOpenStatement("100 OPEN #1:\"FILE\",INPUT")
        val options = stmt.options()
        assertEquals(1, options.size)
        assertEquals(TiBasicTokenTypes.INPUT_KEYWORD, options[0].optionKeywordType())
    }

    fun testOpenWithOutput() {
        val stmt = parseSingleOpenStatement("100 OPEN #1:\"FILE\",OUTPUT")
        val options = stmt.options()
        assertEquals(1, options.size)
        assertEquals(TiBasicTokenTypes.OUTPUT_KEYWORD, options[0].optionKeywordType())
    }

    fun testOpenWithAppend() {
        val stmt = parseSingleOpenStatement("100 OPEN #1:\"FILE\",APPEND")
        val options = stmt.options()
        assertEquals(1, options.size)
        assertEquals(TiBasicTokenTypes.APPEND_KEYWORD, options[0].optionKeywordType())
    }

    fun testOpenWithUpdate() {
        val stmt = parseSingleOpenStatement("100 OPEN #1:\"FILE\",UPDATE")
        val options = stmt.options()
        assertEquals(1, options.size)
        assertEquals(TiBasicTokenTypes.UPDATE_KEYWORD, options[0].optionKeywordType())
    }

    fun testOpenWithFixed() {
        val stmt = parseSingleOpenStatement("100 OPEN #1:\"FILE\",FIXED")
        val options = stmt.options()
        assertEquals(1, options.size)
        assertEquals(TiBasicTokenTypes.FIXED_KEYWORD, options[0].optionKeywordType())
    }

    fun testOpenWithVariable() {
        val stmt = parseSingleOpenStatement("100 OPEN #1:\"FILE\",VARIABLE")
        val options = stmt.options()
        assertEquals(1, options.size)
        assertEquals(TiBasicTokenTypes.VARIABLE_KEYWORD, options[0].optionKeywordType())
    }

    fun testOpenWithPermanent() {
        val stmt = parseSingleOpenStatement("100 OPEN #1:\"FILE\",PERMANENT")
        val options = stmt.options()
        assertEquals(1, options.size)
        assertEquals(TiBasicTokenTypes.PERMANENT_KEYWORD, options[0].optionKeywordType())
    }

    fun testOpenWithAllFiveCategories() {
        val stmt = parseSingleOpenStatement("100 OPEN #1:\"FILE\",RELATIVE,INTERNAL,UPDATE,FIXED,PERMANENT")
        val options = stmt.options()
        assertEquals(5, options.size)
        assertEquals(TiBasicTokenTypes.RELATIVE_KEYWORD, options[0].optionKeywordType())
        assertEquals(TiBasicTokenTypes.INTERNAL_KEYWORD, options[1].optionKeywordType())
        assertEquals(TiBasicTokenTypes.UPDATE_KEYWORD, options[2].optionKeywordType())
        assertEquals(TiBasicTokenTypes.FIXED_KEYWORD, options[3].optionKeywordType())
        assertEquals(TiBasicTokenTypes.PERMANENT_KEYWORD, options[4].optionKeywordType())
    }

    fun testOpenWithOptionsInAlternativeOrder() {
        val stmt = parseSingleOpenStatement("100 OPEN #1:\"FILE\",OUTPUT,DISPLAY,SEQUENTIAL,PERMANENT,VARIABLE")
        val options = stmt.options()
        assertEquals(5, options.size)
        assertEquals(TiBasicTokenTypes.OUTPUT_KEYWORD, options[0].optionKeywordType())
        assertEquals(TiBasicTokenTypes.DISPLAY_KEYWORD, options[1].optionKeywordType())
        assertEquals(TiBasicTokenTypes.SEQUENTIAL_KEYWORD, options[2].optionKeywordType())
        assertEquals(TiBasicTokenTypes.PERMANENT_KEYWORD, options[3].optionKeywordType())
        assertEquals(TiBasicTokenTypes.VARIABLE_KEYWORD, options[4].optionKeywordType())
    }

    fun testOpenOptionParsedAsOpenOptionNode() {
        val stmt = parseSingleOpenStatement("100 OPEN #1:\"FILE\",INPUT")
        assertEquals(1, stmt.children.filterIsInstance<TiBasicOpenOption>().size)
    }

    fun testOpenWithFixedAndRecordLength() {
        val stmt = parseSingleOpenStatement("100 OPEN #1:\"FILE\",FIXED 128")
        val options = stmt.options()
        assertEquals(1, options.size)
        assertEquals(TiBasicTokenTypes.FIXED_KEYWORD, options[0].optionKeywordType())
        assertNotNull(options[0].optionExpression())
    }

    fun testOpenWithVariableAndRecordLength() {
        val stmt = parseSingleOpenStatement("100 OPEN #1:\"FILE\",VARIABLE 64")
        val options = stmt.options()
        assertEquals(1, options.size)
        assertEquals(TiBasicTokenTypes.VARIABLE_KEYWORD, options[0].optionKeywordType())
        assertNotNull(options[0].optionExpression())
    }

    fun testOpenWithFixedRecordLengthAndFollowingOption() {
        val stmt = parseSingleOpenStatement("100 OPEN #2:\"CS1\",SEQUENTIAL,INTERNAL,INPUT,FIXED 128,PERMANENT")
        val options = stmt.options()
        assertEquals(5, options.size)
        assertEquals(TiBasicTokenTypes.SEQUENTIAL_KEYWORD, options[0].optionKeywordType())
        assertEquals(TiBasicTokenTypes.INTERNAL_KEYWORD, options[1].optionKeywordType())
        assertEquals(TiBasicTokenTypes.INPUT_KEYWORD, options[2].optionKeywordType())
        assertEquals(TiBasicTokenTypes.FIXED_KEYWORD, options[3].optionKeywordType())
        assertNotNull(options[3].optionExpression())
        assertEquals(TiBasicTokenTypes.PERMANENT_KEYWORD, options[4].optionKeywordType())
    }
}
