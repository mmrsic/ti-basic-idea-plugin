package com.github.mmrsic.idea.plugins.tibasic.highlight

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase

class TiBasicDimAnnotatorTest : TiBasicTestBase() {

    // --- DIM happy paths ---

    fun `test DIM numeric array no error`() {
        configureFile("100 DIM A(10)")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test DIM string array no error`() {
        configureFile("100 DIM B$(5)")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test DIM multiple entries no error`() {
        configureFile("100 DIM A(10),B$(5),C(3)")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test DIM two-dimensional array no error`() {
        configureFile("100 DIM A(5,3)")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test DIM before first use no warning`() {
        configureFile("100 DIM A(10)\n200 LET A(1) = 5")
        myFixture.checkHighlighting(true, false, true)
    }

    // --- DIM Check 1: no entries ---

    fun `test DIM without entries gives Incorrect statement error`() {
        configureFile("100 <error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">DIM</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    // --- DIM Check 2: invalid variable name ---

    fun `test DIM with invalid variable name gives Bad variable name error`() {
        configureFile("100 DIM <error descr=\"Bad variable name\">ABCDEFGHIJKLMNOPQ(5)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    // --- DIM Check 3: missing parens ---

    fun `test DIM without parens gives Incorrect statement error`() {
        configureFile("100 DIM <error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">A</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test DIM with unclosed parenthesis gives Incorrect statement error`() {
        configureFile("100 DIM <error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">A(8</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    // --- DIM Check 4: variable as dimension ---

    fun `test DIM with variable as dimension gives error`() {
        configureFile("100 DIM A(<error descr=\"Variable not allowed as DIM dimension\">X</error>)")
        myFixture.checkHighlighting(true, false, true)
    }

    // --- DIM Check 5: float as dimension ---

    fun `test DIM with float dimension gives error`() {
        configureFile("100 DIM A(<error descr=\"Float not allowed as DIM dimension\">2.5</error>)")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test DIM with scientific notation dimension gives error`() {
        configureFile("100 DIM A(<error descr=\"Float not allowed as DIM dimension\">1E2</error>)")
        myFixture.checkHighlighting(true, false, true)
    }

    // --- DIM Check 6: expression as dimension ---

    fun `test DIM with arithmetic expression as dimension gives error`() {
        configureFile("100 DIM A(<error descr=\"Integer expected as DIM dimension\">1+2</error>)")
        myFixture.checkHighlighting(true, false, true)
    }

    // --- DIM Check 7: duplicate DIM for same array ---

    fun `test duplicate DIM for same array gives error`() {
        configureFile(
            "100 <error descr=\"Duplicate DIM for array name A\">DIM A(10)</error>\n" +
                    "200 <error descr=\"Duplicate DIM for array name A\">DIM A(5)</error>",
        )
        myFixture.checkHighlighting(true, false, true)
    }

    // --- DIM Check 8: DIM after first use ---

    fun `test DIM after first use gives warning`() {
        configureFile(
            "100 LET A(1) = 5\n" +
                    "200 <warning descr=\"DIM for A must appear before first use at line 100\">DIM A(10)</warning>",
        )
        myFixture.checkHighlighting(true, false, true)
    }

    // --- OPTION BASE happy paths ---

    fun `test OPTION BASE 0 no error`() {
        configureFile("100 OPTION BASE 0")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test OPTION BASE 1 no error`() {
        configureFile("100 OPTION BASE 1")
        myFixture.checkHighlighting(true, false, true)
    }

    // --- OPTION BASE Check 1: missing value ---

    fun `test OPTION BASE without value gives Incorrect statement error`() {
        configureFile("100 <error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">OPTION BASE</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    // --- OPTION BASE Check 2: invalid value ---

    fun `test OPTION BASE with value 2 gives error`() {
        configureFile("100 OPTION BASE <error descr=\"OPTION BASE value must be 0 or 1\">2</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test OPTION BASE with negative value gives error`() {
        configureFile("100 OPTION BASE <error descr=\"OPTION BASE value must be 0 or 1\">5</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    // --- OPTION BASE Check 3: variable as value ---

    fun `test OPTION BASE with variable gives error`() {
        configureFile("100 OPTION BASE <error descr=\"Variable not allowed as OPTION BASE value\">X</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    // --- OPTION BASE Check 4: float as value ---

    fun `test OPTION BASE with float gives error`() {
        configureFile("100 OPTION BASE <error descr=\"Float not allowed as OPTION BASE value\">0.5</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    // --- OPTION BASE placement: happy paths ---

    fun `test OPTION BASE before DIM and array use no warning`() {
        configureFile("100 OPTION BASE 1\n200 DIM A(10)\n300 LET A(1) = 5")
        myFixture.checkHighlighting(true, false, true)
    }

    // --- OPTION BASE placement Check 1: duplicate ---

    fun `test duplicate OPTION BASE gives error`() {
        configureFile(
            "100 <error descr=\"Duplicate OPTION BASE\">OPTION BASE 0</error>\n" +
                    "200 <error descr=\"Duplicate OPTION BASE\">OPTION BASE 1</error>",
        )
        myFixture.checkHighlighting(true, false, true)
    }

    // --- OPTION BASE placement Check 2: after DIM ---

    fun `test OPTION BASE after DIM gives warning`() {
        configureFile(
            "100 DIM A(10)\n" +
                    "200 <warning descr=\"OPTION BASE must appear before DIM at line 100\">OPTION BASE 1</warning>",
        )
        myFixture.checkHighlighting(true, false, true)
    }

    // --- OPTION BASE placement Check 3: after array use ---

    fun `test OPTION BASE after array use gives warning`() {
        configureFile(
            "100 LET A(1) = 5\n" +
                    "200 <warning descr=\"OPTION BASE must appear before first array use at line 100\">OPTION BASE 1</warning>",
        )
        myFixture.checkHighlighting(true, false, true)
    }
}

