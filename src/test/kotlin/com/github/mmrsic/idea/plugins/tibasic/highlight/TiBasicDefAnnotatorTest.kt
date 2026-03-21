package com.github.mmrsic.idea.plugins.tibasic.highlight

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase

class TiBasicDefAnnotatorTest : TiBasicTestBase() {

    // --- Happy paths ---

    fun `test DEF numeric function with numeric body no error`() {
        configureFile("100 DEF DOUBLE(X) = 2*X")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test DEF string function with string body no error`() {
        configureFile("100 DEF GREET\$(N\$) = \"HI \"&N\$")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test DEF without parameter no error`() {
        configureFile("100 DEF PI = 3.14159")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test DEF used in expression no error`() {
        configureFile("100 DEF DOUBLE(X) = 2*X\n200 LET Y = DOUBLE(5)")
        myFixture.checkHighlighting(true, false, true)
    }

    // --- Check 1: missing function name ---

    fun `test DEF with no name gives Incorrect statement error`() {
        configureFile("100 <error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">DEF</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    // --- Check 2: invalid function name ---

    fun `test DEF with invalid function name gives Bad variable name error`() {
        // 17-character name exceeds the 15-character maximum for numeric variables
        configureFile("100 DEF <error descr=\"Bad variable name\">ABCDEFGHIJKLMNOPQ</error>(X) = X")
        myFixture.checkHighlighting(true, false, true)
    }

    // --- Check 3: missing = ---

    fun `test DEF with missing equals gives Incorrect statement error`() {
        configureFile("100 <error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">DEF F(X)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    // --- Check 4: missing body expression ---

    fun `test DEF with missing body expression gives Incorrect statement error`() {
        configureFile("100 <error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">DEF F(X) =</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    // --- Check 5: type mismatch name <-> body ---

    fun `test DEF numeric name with string body gives String-number mismatch error`() {
        configureFile("100 <error descr=\"String-number mismatch\">DEF F(X) = \"HELLO\"</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test DEF string name with numeric body gives String-number mismatch error`() {
        configureFile("100 <error descr=\"String-number mismatch\">DEF F\$(X) = X+1</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    // --- Check 6: invalid parameter name ---

    fun `test DEF with invalid parameter name gives Bad variable name error`() {
        // 17-character name exceeds the 15-character maximum for numeric variables
        configureFile("100 DEF F(<error descr=\"Bad variable name\">ABCDEFGHIJKLMNOPQ</error>) = 0")
        myFixture.checkHighlighting(true, false, true)
    }

    // --- Check 7: parameter used as array in body ---

    fun `test DEF with parameter used as array in body gives Incorrect statement error`() {
        configureFile("100 DEF F(X) = <error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">X(1)</error>+1")
        myFixture.checkHighlighting(true, false, true)
    }

    // --- Check 8: duplicate function names ---

    fun `test two DEF statements with same name give duplicate warning`() {
        configureFile(
            "100 <warning descr=\"Duplicate DEF for function name DOUBLE\">DEF DOUBLE(X) = 2*X</warning>\n" +
                    "200 <warning descr=\"Duplicate DEF for function name DOUBLE\">DEF DOUBLE(X) = X*2</warning>"
        )
        myFixture.checkHighlighting(true, false, true)
    }

    // --- Check 9: direct self-reference ---

    fun `test DEF referencing itself gives self-reference warning`() {
        configureFile("100 DEF F(X) = <warning descr=\"DEF function may not reference itself\">F(X)</warning>")
        myFixture.checkHighlighting(true, false, true)
    }

    // --- Check 10: DEF function name used without parens ---

    fun `test DEF function name used without parens gives Name conflict error`() {
        configureFile(
            "100 DEF SQUARE(X)=X*X\n" +
                    "110 PRINT <error descr=\"Name conflict with line 100\">SQUARE</error>",
        )
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test DEF function name used with parens no error`() {
        configureFile(
            "100 DEF SQUARE(X)=X*X\n" +
                    "110 PRINT SQUARE(4)",
        )
        myFixture.checkHighlighting(true, false, true)
    }

    // --- Check 11: DEF constant name used with parens ---

    fun `test DEF constant name used with parens gives Name conflict error`() {
        configureFile(
            "120 DEF PI=3.1416\n" +
                    "130 PRINT <error descr=\"Name conflict with line 120\">PI(2)</error>",
        )
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test DEF constant name used without parens no error`() {
        configureFile(
            "100 DEF PI=3.14159\n" +
                    "200 LET Y=PI",
        )
        myFixture.checkHighlighting(true, false, true)
    }
}
