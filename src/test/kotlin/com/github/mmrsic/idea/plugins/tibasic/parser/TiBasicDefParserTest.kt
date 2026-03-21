package com.github.mmrsic.idea.plugins.tibasic.parser

import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicDefStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicLine
import com.intellij.testFramework.ParsingTestCase

class TiBasicDefParserTest : ParsingTestCase("", "tibasic", TiBasicParserDefinition()) {

    override fun getTestDataPath(): String = "src/test/testData"

    override fun skipSpaces(): Boolean = true

    private fun parseCode(code: String): TiBasicFile = createPsiFile("test", code) as TiBasicFile

    fun `test numeric DEF with parameter parses as DEF statement`() {
        val file = parseCode("100 DEF DOUBLE(X) = 2*X")
        val defStmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicDefStatement>()
        assertEquals(1, defStmt.size)
        assertEquals("DOUBLE", defStmt[0].functionName())
        assertEquals("X", defStmt[0].parameterNode()?.text?.uppercase())
        assertNotNull(defStmt[0].bodyExpression())
    }

    fun `test string DEF with parameter parses as DEF statement`() {
        val file = parseCode("100 DEF GREET\$(N\$) = \"HI \"&N\$")
        val defStmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicDefStatement>()
        assertEquals(1, defStmt.size)
        assertEquals("GREET\$", defStmt[0].functionName())
        assertEquals("N\$", defStmt[0].parameterNode()?.text?.uppercase())
        assertNotNull(defStmt[0].bodyExpression())
    }

    fun `test DEF without parameter parses as DEF statement`() {
        val file = parseCode("100 DEF PI = 3.14159")
        val defStmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicDefStatement>()
        assertEquals(1, defStmt.size)
        assertEquals("PI", defStmt[0].functionName())
        assertNull(defStmt[0].parameterNode())
        assertNotNull(defStmt[0].bodyExpression())
    }

    fun `test DEF with lowercase name parses and uppercases function name`() {
        val file = parseCode("100 DEF halve(x) = x/2")
        val defStmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicDefStatement>()
        assertEquals(1, defStmt.size)
        assertEquals("HALVE", defStmt[0].functionName())
    }

    fun `test DEF without equals sign still produces DEF statement node`() {
        val file = parseCode("100 DEF F")
        val defStmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicDefStatement>()
        assertEquals(1, defStmt.size)
        assertNull(defStmt[0].bodyExpression())
    }
}

