package com.github.mmrsic.idea.plugins.tibasic.parser

import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFunctionCall
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicLetStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicLine
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
