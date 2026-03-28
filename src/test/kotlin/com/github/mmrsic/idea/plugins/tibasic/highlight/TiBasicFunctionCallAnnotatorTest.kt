package com.github.mmrsic.idea.plugins.tibasic.highlight

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase

class TiBasicFunctionCallAnnotatorTest : TiBasicTestBase() {

    fun `test ABS with numeric argument no error`() {
        configureFile("100 LET Y=ABS(X)")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test ABS with numeric literal no error`() {
        configureFile("100 LET Y=ABS(-5)")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test ABS with no arguments gives INCORRECT STATEMENT error`() {
        configureFile("100 LET Y=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">ABS()</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test ABS with two arguments gives INCORRECT STATEMENT error`() {
        configureFile("100 LET Y=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">ABS(X,2)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test ABS with string argument gives INCORRECT STATEMENT error`() {
        configureFile("100 LET Y=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">ABS(A$)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test ABS with unbalanced opening paren gives INCORRECT STATEMENT error`() {
        configureFile("100 PRINT <error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">ABS((8)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test ABS with string literal gives INCORRECT STATEMENT error`() {
        configureFile("100 LET Y=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">ABS(\"HELLO\")</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test ABS used as variable name gives error`() {
        configureFile("100 LET <error descr=\"Function name cannot be used as variable\">ABS</error>=5")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test SIN with numeric argument no error`() {
        configureFile("100 LET Y=SIN(X)")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test SIN with two arguments gives INCORRECT STATEMENT error`() {
        configureFile("100 LET Y=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">SIN(X,2)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test SIN with string argument gives INCORRECT STATEMENT error`() {
        configureFile("100 LET Y=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">SIN(A$)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test COS with numeric argument no error`() {
        configureFile("100 LET Y=COS(X)")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test COS with two arguments gives INCORRECT STATEMENT error`() {
        configureFile("100 LET Y=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">COS(X,2)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test SIN used as variable name gives error`() {
        configureFile("100 LET <error descr=\"Function name cannot be used as variable\">SIN</error>=1")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test COS used as variable name gives error`() {
        configureFile("100 LET <error descr=\"Function name cannot be used as variable\">COS</error>=1")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test TAN with numeric argument no error`() {
        configureFile("100 LET Y=TAN(X)")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test TAN with two arguments gives INCORRECT STATEMENT error`() {
        configureFile("100 LET Y=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">TAN(X,2)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test TAN with string argument gives INCORRECT STATEMENT error`() {
        configureFile("100 LET Y=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">TAN(A$)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test ATN with numeric argument no error`() {
        configureFile("100 LET Y=ATN(X)")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test ATN with two arguments gives INCORRECT STATEMENT error`() {
        configureFile("100 LET Y=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">ATN(X,2)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test TAN used as variable name gives error`() {
        configureFile("100 LET <error descr=\"Function name cannot be used as variable\">TAN</error>=1")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test EXP with numeric argument no error`() {
        configureFile("100 LET Y=EXP(X)")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test EOF with numeric argument no error`() {
        configureFile("100 LET X=EOF(1)")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test EOF with numeric expression argument no error`() {
        configureFile("100 LET X=EOF(N+1)")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test EOF with no arguments gives INCORRECT STATEMENT error`() {
        configureFile("100 LET X=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">EOF()</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test EOF with two arguments gives INCORRECT STATEMENT error`() {
        configureFile("100 LET X=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">EOF(1,2)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test EOF with string argument gives INCORRECT STATEMENT error`() {
        configureFile("100 LET X=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">EOF(A$)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test EOF used as variable name gives error`() {
        configureFile("100 LET <error descr=\"Function name cannot be used as variable\">EOF</error>=1")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test EXP with two arguments gives INCORRECT STATEMENT error`() {
        configureFile("100 LET Y=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">EXP(X,2)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test EXP with string argument gives INCORRECT STATEMENT error`() {
        configureFile("100 LET Y=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">EXP(A$)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test LOG with numeric argument no error`() {
        configureFile("100 LET Y=LOG(X)")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test LOG with two arguments gives INCORRECT STATEMENT error`() {
        configureFile("100 LET Y=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">LOG(X,2)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test EXP used as variable name gives error`() {
        configureFile("100 LET <error descr=\"Function name cannot be used as variable\">EXP</error>=1")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test SGN with numeric argument no error`() {
        configureFile("100 LET Y=SGN(X)")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test SGN with two arguments gives INCORRECT STATEMENT error`() {
        configureFile("100 LET Y=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">SGN(X,2)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test SGN with string argument gives INCORRECT STATEMENT error`() {
        configureFile("100 LET Y=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">SGN(A$)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test SQR with numeric argument no error`() {
        configureFile("100 LET Y=SQR(X)")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test SQR with two arguments gives INCORRECT STATEMENT error`() {
        configureFile("100 LET Y=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">SQR(X,2)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test SGN used as variable name gives error`() {
        configureFile("100 LET <error descr=\"Function name cannot be used as variable\">SGN</error>=1")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test INT with numeric argument no error`() {
        configureFile("100 LET Y=INT(X)")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test INT with two arguments gives INCORRECT STATEMENT error`() {
        configureFile("100 LET Y=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">INT(X,2)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test INT with string argument gives INCORRECT STATEMENT error`() {
        configureFile("100 LET Y=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">INT(A$)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test RND without parentheses no error`() {
        configureFile("100 LET X=RND")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test RND with argument gives INCORRECT STATEMENT error`() {
        configureFile("100 LET X=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">RND(1)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test RND with empty parentheses gives INCORRECT STATEMENT error`() {
        configureFile("170 PRINT <error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">RND()</error>*1237")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test INT used as variable name gives error`() {
        configureFile("100 LET <error descr=\"Function name cannot be used as variable\">INT</error>=1")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test RND used as variable name gives error`() {
        configureFile("100 LET <error descr=\"Function name cannot be used as variable\">RND</error>=1")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test unbalanced opening paren in PRINT expression gives INCORRECT STATEMENT error`() {
        configureFile("170 PRINT <error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">(RND*1237</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test balanced parens in PRINT expression no error`() {
        configureFile("170 PRINT (RND*1237)")
        myFixture.checkHighlighting(true, false, true)
    }

    // --- ASC ---

    fun `test ASC with string variable argument no error`() {
        configureFile("100 LET Y=ASC(A$)")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test ASC with string literal argument no error`() {
        configureFile("100 LET Y=ASC(\"HELLO\")")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test ASC with numeric argument gives INCORRECT STATEMENT error`() {
        configureFile("100 LET Y=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">ASC(X)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test ASC with no arguments gives INCORRECT STATEMENT error`() {
        configureFile("100 LET Y=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">ASC()</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test ASC with two arguments gives INCORRECT STATEMENT error`() {
        configureFile("100 LET Y=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">ASC(A$,B$)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    // --- LEN ---

    fun `test LEN with string variable argument no error`() {
        configureFile("100 LET Y=LEN(A$)")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test LEN with numeric argument gives INCORRECT STATEMENT error`() {
        configureFile("100 LET Y=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">LEN(X)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test LEN with no arguments gives INCORRECT STATEMENT error`() {
        configureFile("100 LET Y=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">LEN()</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    // --- VAL ---

    fun `test VAL with string variable argument no error`() {
        configureFile("100 LET Y=VAL(A$)")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test VAL with numeric argument gives INCORRECT STATEMENT error`() {
        configureFile("100 LET Y=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">VAL(X)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    // --- POS ---

    fun `test POS with two string arguments and one numeric no error`() {
        configureFile("100 LET Y=POS(A$,B$,1)")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test POS with wrong argument count gives INCORRECT STATEMENT error`() {
        configureFile("100 LET Y=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">POS(A$,B$)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test POS with numeric first argument gives INCORRECT STATEMENT error`() {
        configureFile("100 LET Y=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">POS(X,B$,1)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    // --- CHR$ ---

    fun `test CHR$ with numeric argument assigned to string variable no error`() {
        configureFile("100 LET A$=CHR$(65)")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test CHR$ with numeric variable argument no error`() {
        configureFile("100 LET A$=CHR$(X)")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test CHR$ with string argument gives INCORRECT STATEMENT error`() {
        configureFile("100 LET A$=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">CHR$(B$)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test CHR$ with no arguments gives INCORRECT STATEMENT error`() {
        configureFile("100 LET A$=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">CHR$()</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test CHR$ with two arguments gives INCORRECT STATEMENT error`() {
        configureFile("100 LET A$=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">CHR$(65,66)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test CHR$ in PRINT statement no error`() {
        configureFile("100 PRINT CHR$(65)")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test CHR$ concatenated with string literal no error`() {
        configureFile("100 LET A$=CHR$(65)&\"!\"")
        myFixture.checkHighlighting(true, false, true)
    }

    // --- SEG$ ---

    fun `test SEG$ with correct arguments no error`() {
        configureFile("100 LET A$=SEG$(B$,1,3)")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test SEG$ with wrong argument count gives INCORRECT STATEMENT error`() {
        configureFile("100 LET A$=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">SEG$(B$,1)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test SEG$ with numeric first argument gives INCORRECT STATEMENT error`() {
        configureFile("100 LET A$=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">SEG$(X,1,3)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    // --- STR$ ---

    fun `test STR$ with numeric argument assigned to string variable no error`() {
        configureFile("100 LET A$=STR$(X)")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test STR$ with string argument gives INCORRECT STATEMENT error`() {
        configureFile("100 LET A$=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">STR$(B$)</error>")
        myFixture.checkHighlighting(true, false, true)
    }

    fun `test STR$ with no arguments gives INCORRECT STATEMENT error`() {
        configureFile("100 LET A$=<error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">STR$()</error>")
        myFixture.checkHighlighting(true, false, true)
    }
}
