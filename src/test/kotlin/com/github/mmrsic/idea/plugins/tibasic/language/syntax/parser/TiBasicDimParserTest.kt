package com.github.mmrsic.idea.plugins.tibasic.language.syntax.parser

import com.github.mmrsic.idea.plugins.tibasic.ide.language.TiBasicParserDefinition
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicDimStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicLine
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicOptionBaseStatement
import com.intellij.testFramework.ParsingTestCase

class TiBasicDimParserTest : ParsingTestCase("", "tibasic", TiBasicParserDefinition()) {

    override fun getTestDataPath(): String = "src/test/testData"

    override fun skipSpaces(): Boolean = true

    private fun parseCode(code: String): TiBasicFile = createPsiFile("test", code) as TiBasicFile

    fun `test single numeric DIM parses as DIM statement`() {
        val file = parseCode("100 DIM A(10)")
        val dimStmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicDimStatement>()
        assertEquals(1, dimStmts.size)
        assertEquals(1, dimStmts[0].dimVariableAccesses().size)
        assertEquals("A", dimStmts[0].dimVariableAccesses()[0].node.firstChildNode.text.uppercase())
    }

    fun `test single string DIM parses as DIM statement`() {
        val file = parseCode("100 DIM B$(5)")
        val dimStmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicDimStatement>()
        assertEquals(1, dimStmts.size)
        assertEquals("B$", dimStmts[0].dimVariableAccesses()[0].node.firstChildNode.text.uppercase())
    }

    fun `test multiple DIM entries parse as single DIM statement`() {
        val file = parseCode("100 DIM A(10),B$(5),C(3)")
        val dimStmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicDimStatement>()
        assertEquals(1, dimStmts.size)
        assertEquals(3, dimStmts[0].dimVariableAccesses().size)
    }

    fun `test two-dimensional DIM parses as DIM statement`() {
        val file = parseCode("100 DIM A(5,3)")
        val dimStmt = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicDimStatement>()[0]
        val varAccess = dimStmt.dimVariableAccesses()[0]
        assertEquals(2, varAccess.subscriptDimCount())
    }

    fun `test DIM without entries still produces DIM statement node`() {
        val file = parseCode("100 DIM")
        val dimStmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicDimStatement>()
        assertEquals(1, dimStmts.size)
        assertEquals(0, dimStmts[0].dimVariableAccesses().size)
    }

    fun `test OPTION BASE 0 parses as OPTION BASE statement`() {
        val file = parseCode("100 OPTION BASE 0")
        val optStmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicOptionBaseStatement>()
        assertEquals(1, optStmts.size)
    }

    fun `test OPTION BASE 1 parses as OPTION BASE statement`() {
        val file = parseCode("100 OPTION BASE 1")
        val optStmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicOptionBaseStatement>()
        assertEquals(1, optStmts.size)
    }

    fun `test OPTION BASE with spaces parses as OPTION BASE statement`() {
        val file = parseCode("100 OPTION  BASE 1")
        val optStmts = file.children.filterIsInstance<TiBasicLine>()[0]
            .children.filterIsInstance<TiBasicOptionBaseStatement>()
        assertEquals(1, optStmts.size)
    }
}

