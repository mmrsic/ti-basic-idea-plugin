package com.github.mmrsic.idea.plugins.tibasic.parser

import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.expression.TiBasicCallStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.statement.TiBasicLine
import com.intellij.testFramework.ParsingTestCase

class TiBasicCallParserTest : ParsingTestCase("", "tibasic", TiBasicParserDefinition()) {

    override fun getTestDataPath(): String = "src/test/testData"

    override fun skipSpaces(): Boolean = true

    private fun parseCode(code: String): TiBasicFile = createPsiFile("test", code) as TiBasicFile

    fun `test CALL CLEAR parses as call statement`() {
        val file = parseCode("100 CALL CLEAR")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicCallStatement>()
        assertEquals(1, stmt.size)
        assertEquals("CLEAR", stmt[0].subprogramName())
        assertEquals(0, stmt[0].arguments().size)
    }

    fun `test CALL SCREEN with one argument`() {
        val file = parseCode("100 CALL SCREEN(2)")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicCallStatement>()[0]
        assertEquals("SCREEN", stmt.subprogramName())
        assertEquals(1, stmt.arguments().size)
    }

    fun `test CALL HCHAR with three arguments`() {
        val file = parseCode("100 CALL HCHAR(1,2,42)")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicCallStatement>()[0]
        assertEquals("HCHAR", stmt.subprogramName())
        assertEquals(3, stmt.arguments().size)
    }

    fun `test CALL HCHAR with four arguments`() {
        val file = parseCode("100 CALL HCHAR(1,2,42,5)")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicCallStatement>()[0]
        assertEquals("HCHAR", stmt.subprogramName())
        assertEquals(4, stmt.arguments().size)
    }

    fun `test CALL SOUND with three arguments`() {
        val file = parseCode("100 CALL SOUND(100,440,2)")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicCallStatement>()[0]
        assertEquals("SOUND", stmt.subprogramName())
        assertEquals(3, stmt.arguments().size)
    }

    fun `test CALL CHAR with two arguments`() {
        val file = parseCode("100 CALL CHAR(96,\"FFFFFFFFFFFFFFFF\")")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicCallStatement>()[0]
        assertEquals("CHAR", stmt.subprogramName())
        assertEquals(2, stmt.arguments().size)
    }

    fun `test lowercase call subprogram is parsed`() {
        val file = parseCode("100 CALL clear")
        val stmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicCallStatement>()
        assertEquals(1, stmt.size)
    }
}
