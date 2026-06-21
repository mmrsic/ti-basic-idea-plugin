package com.github.mmrsic.idea.plugins.tibasic.language.syntax.parser

import com.github.mmrsic.idea.plugins.tibasic.common.ext.firstChildType
import com.github.mmrsic.idea.plugins.tibasic.common.ext.nonWhitespaceChildren
import com.github.mmrsic.idea.plugins.tibasic.ide.language.TiBasicParserDefinition
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicFunctionCall
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicLetStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicLine
import com.intellij.testFramework.ParsingTestCase

class TiBasicFunctionCallParserTest : ParsingTestCase("", "tibasic", TiBasicParserDefinition()) {

    override fun getTestDataPath(): String = "src/test/testData"

    override fun skipSpaces(): Boolean = true

    private fun parseCode(code: String): TiBasicFile = createPsiFile("test", code) as TiBasicFile

    fun `test ABS with one numeric argument parses as function call`() {
        val file = parseCode("100 LET Y=ABS(X)")
        val letStmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicLetStatement>()[0]
        val funcCall = letStmt.node.psi.children.flatMap { it.children.toList() }
            .filterIsInstance<TiBasicFunctionCall>()
        assertEquals(1, funcCall.size)
        assertEquals("ABS", funcCall[0].functionName())
        assertEquals(1, funcCall[0].arguments().size)
    }

    fun `test ABS with no parentheses does not produce function call node`() {
        val file = parseCode("100 LET Y=ABS")
        val letStmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicLetStatement>()
        assertTrue(letStmt.isNotEmpty())
    }

    fun `test lowercase abs parses as function call`() {
        val file = parseCode("100 LET Y=abs(X)")
        val letStmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicLetStatement>()[0]
        val funcCall = letStmt.node.psi.children.flatMap { it.children.toList() }
            .filterIsInstance<TiBasicFunctionCall>()
        assertEquals(1, funcCall.size)
        assertEquals("ABS", funcCall[0].functionName())
    }

    fun `test ASC with string argument parses as function call`() {
        val file = parseCode("100 LET Y=ASC(A$)")
        val letStmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicLetStatement>()[0]
        val funcCall = letStmt.node.psi.children.flatMap { it.children.toList() }
            .filterIsInstance<TiBasicFunctionCall>()
        assertEquals(1, funcCall.size)
        assertEquals("ASC", funcCall[0].functionName())
        assertEquals(1, funcCall[0].arguments().size)
    }

    fun `test CHR$ with numeric argument parses as function call`() {
        val file = parseCode("100 LET A$=CHR$(65)")
        val letStmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicLetStatement>()[0]
        val funcCall = letStmt.node.psi.children.flatMap { it.children.toList() }
            .filterIsInstance<TiBasicFunctionCall>()
        assertEquals(1, funcCall.size)
        assertEquals("CHR$", funcCall[0].functionName())
        assertEquals(1, funcCall[0].arguments().size)
    }

    fun `test CHR$ lowercase parses as function call`() {
        val file = parseCode("100 LET A$=chr$(65)")
        val letStmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicLetStatement>()[0]
        val funcCall = letStmt.node.psi.children.flatMap { it.children.toList() }
            .filterIsInstance<TiBasicFunctionCall>()
        assertEquals(1, funcCall.size)
        assertEquals("CHR$", funcCall[0].functionName())
    }

    fun `test CHR$ concatenation keeps numeric mismatch operand inside expression`() {
        val file = parseCode("1640 s$=chr$(130)&\" X=\"&X1")
        val letStmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicLetStatement>()[0]
        val expression = letStmt.assignedExpression()
        assertNotNull(expression)
        val children = expression!!.node.nonWhitespaceChildren

        assertEquals(
            listOf(
                TiBasicNodeTypes.FUNCTION_CALL,
                TiBasicTokenTypes.CONCAT_OP,
                TiBasicTokenTypes.STRING_LITERAL,
                TiBasicTokenTypes.CONCAT_OP,
                TiBasicNodeTypes.VARIABLE_ACCESS,
            ),
            children.map { it.elementType },
        )
        assertEquals(TiBasicTokenTypes.NUMERIC_VARIABLE, children.last().firstChildType)
    }

    fun `test EOF with numeric literal argument parses as function call`() {
        val file = parseCode("100 LET X=EOF(1)")
        val letStmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicLetStatement>()[0]
        val funcCall = letStmt.node.psi.children.flatMap { it.children.toList() }
            .filterIsInstance<TiBasicFunctionCall>()
        assertEquals(1, funcCall.size)
        assertEquals("EOF", funcCall[0].functionName())
        assertEquals(1, funcCall[0].arguments().size)
    }

    fun `test EOF with expression argument parses as function call`() {
        val file = parseCode("100 LET X=EOF(N+1)")
        val letStmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicLetStatement>()[0]
        val funcCall = letStmt.node.psi.children.flatMap { it.children.toList() }
            .filterIsInstance<TiBasicFunctionCall>()
        assertEquals(1, funcCall.size)
        assertEquals("EOF", funcCall[0].functionName())
        assertEquals(1, funcCall[0].arguments().size)
    }

    fun `test lowercase eof parses as function call`() {
        val file = parseCode("100 LET X=eof(1)")
        val letStmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicLetStatement>()[0]
        val funcCall = letStmt.node.psi.children.flatMap { it.children.toList() }
            .filterIsInstance<TiBasicFunctionCall>()
        assertEquals(1, funcCall.size)
        assertEquals("EOF", funcCall[0].functionName())
    }
}
