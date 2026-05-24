package com.github.mmrsic.idea.plugins.tibasic.language.syntax

import junit.framework.TestCase

class ParenUtilsTest : TestCase() {

    fun `test no parens returns zero`() {
        assertEquals(0, countUnclosedParens("100 PRINT \"HELLO\""))
    }

    fun `test balanced parens returns zero`() {
        assertEquals(0, countUnclosedParens("100 PRINT ABS(X)"))
    }

    fun `test one unclosed paren`() {
        assertEquals(1, countUnclosedParens("100 PRINT ABS(X"))
    }

    fun `test two unclosed parens`() {
        assertEquals(2, countUnclosedParens("100 LET X=SIN(COS(Y"))
    }

    fun `test more closing than opening returns zero`() {
        assertEquals(0, countUnclosedParens("100 PRINT X)"))
    }

    fun `test paren inside string literal is ignored`() {
        assertEquals(0, countUnclosedParens("100 PRINT \"(hello)\""))
    }

    fun `test unclosed paren with string literal`() {
        assertEquals(1, countUnclosedParens("100 PRINT SEG$(A\$,1"))
    }

    fun `test escaped double quote inside string does not break scan`() {
        assertEquals(1, countUnclosedParens("100 PRINT \"A\"\"B\" & ABS(X"))
    }

    fun `test REM line is always zero regardless of parens`() {
        assertEquals(0, countUnclosedParens("100 REM (unclosed paren here"))
    }

    fun `test rem keyword case insensitive`() {
        assertEquals(0, countUnclosedParens("100 rem (still a comment"))
    }

    fun `test empty string returns zero`() {
        assertEquals(0, countUnclosedParens(""))
    }
}
