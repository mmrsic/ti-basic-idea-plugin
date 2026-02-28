package com.github.mmrsic.idea.plugins.tibasic.highlight

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase

class TiBasicCallAnnotatorTest : TiBasicTestBase() {

    fun `test CALL CLEAR no error`() {
        configureFile("100 CALL CLEAR")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test CALL SCREEN with valid argument no error`() {
        configureFile("100 CALL SCREEN(2)")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test CALL HCHAR with three arguments no error`() {
        configureFile("100 CALL HCHAR(1,2,42)")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test CALL HCHAR with four arguments no error`() {
        configureFile("100 CALL HCHAR(1,2,42,5)")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test CALL SOUND with three arguments no error`() {
        configureFile("100 CALL SOUND(100,440,2)")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test CALL SOUND with five arguments no error`() {
        configureFile("100 CALL SOUND(100,440,2,200,880)")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test CALL unknown name gives error`() {
        configureFile("100 CALL <error descr=\"Unknown subprogram: BEEPER\">BEEPER</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test CALL CLEAR with argument gives error`() {
        configureFile("100 <error descr=\"Wrong number of arguments for CLEAR: expected 0, got 1\">CALL CLEAR(1)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test CALL CLEAR with trailing text gives BAD NAME error`() {
        configureFile("100 CALL CLEAR <error descr=\"Will cause run-time error 'BAD NAME'\">A</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test CALL CLEAR with trailing number gives BAD NAME error`() {
        configureFile("100 CALL CLEAR <error descr=\"Will cause run-time error 'BAD NAME'\">1</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test CALL SCREEN without argument gives error`() {
        configureFile("100 <error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">CALL SCREEN</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test CALL SCREEN with wrong argument type gives error`() {
        configureFile("100 <error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">CALL SCREEN(\"A\")</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test CALL HCHAR with wrong argument count gives error`() {
        configureFile("100 <error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">CALL HCHAR(1,2)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test CALL HCHAR with wrong argument type gives error`() {
        configureFile("100 <error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">CALL HCHAR(1,2,\"A\")</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test CALL VCHAR with wrong argument count gives error`() {
        configureFile("100 <error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">CALL VCHAR(1,2)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test CALL GCHAR with wrong argument count gives error`() {
        configureFile("100 <error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">CALL GCHAR(1,2)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test CALL COLOR with wrong argument count gives error`() {
        configureFile("100 <error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">CALL COLOR(1,2)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test CALL CHAR with correct types no warning`() {
        configureFile("100 CALL CHAR(96,\"FFFFFFFFFFFFFFFF\")")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test CALL CHAR with swapped types gives warnings`() {
        configureFile("100 CALL CHAR(<warning descr=\"Type mismatch at argument 1 of CHAR\">\"X\"</warning>,<warning descr=\"Type mismatch at argument 2 of CHAR\">96</warning>)")
        myFixture.checkHighlighting(true, false, true)
    }
}
