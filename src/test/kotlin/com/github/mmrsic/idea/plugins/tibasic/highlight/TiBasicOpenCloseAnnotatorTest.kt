package com.github.mmrsic.idea.plugins.tibasic.highlight

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase

class TiBasicOpenCloseAnnotatorTest : TiBasicTestBase() {

    fun `test valid OPEN with literal file number and string literal`() {
        configureFile("100 OPEN #1:\"DSK1.FILE\"")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test valid CLOSE with literal file number`() {
        configureFile("100 CLOSE #1")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test valid OPEN with variable file number`() {
        configureFile("100 OPEN #N:\"FILE\"")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test valid CLOSE with variable file number`() {
        configureFile("100 CLOSE #N")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test OPEN file number 0 is reserved for screen`() {
        configureFile("100 OPEN #<error descr=\"File number 0 is reserved for screen\">0</error>:\"FILE\"")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test CLOSE file number 0 is reserved for screen`() {
        configureFile("100 CLOSE #<error descr=\"File number 0 is reserved for screen\">0</error>")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test OPEN file number above 255 is out of range`() {
        configureFile("100 OPEN #<error descr=\"File number must be between 1 and 255\">300</error>:\"FILE\"")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test CLOSE file number above 255 is out of range`() {
        configureFile("100 CLOSE #<error descr=\"File number must be between 1 and 255\">256</error>")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test OPEN missing hash prefix`() {
        configureFile("100 <error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">OPEN 1:\"FILE\"</error>")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test CLOSE missing hash prefix`() {
        configureFile("100 <error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">CLOSE 1</error>")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test OPEN string expression as file number`() {
        configureFile("100 OPEN #<error descr=\"Numeric expression expected\">\"X\"</error>:\"FILE\"")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test OPEN numeric expression as file name`() {
        configureFile("100 OPEN #1:<error descr=\"String expression expected\">42</error>")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test OPEN missing colon and file name`() {
        configureFile("100 <error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">OPEN #1</error>")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test valid OPEN with file number 255`() {
        configureFile("100 OPEN #255:\"FILE\"")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test valid OPEN with file number 1`() {
        configureFile("100 OPEN #1:\"FILE\"")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test OPEN with trailing garbage after file name`() {
        configureFile("100 <error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">OPEN #1:\"CS1\"a dda</error>")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test CLOSE with trailing garbage directly after file number`() {
        configureFile("110 <error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">CLOSE #1asd</error>")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test CLOSE with trailing garbage separated by space`() {
        configureFile("110 <error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">CLOSE #1 asd</error>")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test valid CLOSE with DELETE modifier`() {
        configureFile("100 CLOSE #1:DELETE")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test valid CLOSE with DELETE modifier and variable file number`() {
        configureFile("100 CLOSE #N:DELETE")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test CLOSE with colon but no DELETE is invalid`() {
        configureFile("100 <error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">CLOSE #1:</error>")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test CLOSE with DELETE but no colon is invalid`() {
        configureFile("100 <error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">CLOSE #1 DELETE</error>")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test CLOSE with colon and non-DELETE keyword is invalid`() {
        configureFile("100 <error descr=\"Will cause run-time error 'INCORRECT STATEMENT'\">CLOSE #1:INPUT</error>")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test valid OPEN with SEQUENTIAL option`() {
        configureFile("100 OPEN #1:\"FILE\",SEQUENTIAL")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test valid OPEN with SEQUENTIAL and record count`() {
        configureFile("100 OPEN #1:\"FILE\",SEQUENTIAL 50")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test valid OPEN with RELATIVE option`() {
        configureFile("100 OPEN #1:\"FILE\",RELATIVE")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test valid OPEN with RELATIVE and record count`() {
        configureFile("100 OPEN #1:\"FILE\",RELATIVE 100")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test valid OPEN with DISPLAY option`() {
        configureFile("100 OPEN #1:\"FILE\",DISPLAY")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test valid OPEN with INTERNAL option`() {
        configureFile("100 OPEN #1:\"FILE\",INTERNAL")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test valid OPEN with INPUT option`() {
        configureFile("100 OPEN #1:\"FILE\",INPUT")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test valid OPEN with OUTPUT option`() {
        configureFile("100 OPEN #1:\"FILE\",OUTPUT")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test valid OPEN with APPEND option`() {
        configureFile("100 OPEN #1:\"FILE\",APPEND")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test valid OPEN with UPDATE option`() {
        configureFile("100 OPEN #1:\"FILE\",UPDATE")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test valid OPEN with FIXED option`() {
        configureFile("100 OPEN #1:\"FILE\",FIXED")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test valid OPEN with VARIABLE option`() {
        configureFile("100 OPEN #1:\"FILE\",VARIABLE")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test valid OPEN with PERMANENT option`() {
        configureFile("100 OPEN #1:\"FILE\",PERMANENT")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test valid OPEN with all five option categories`() {
        configureFile("100 OPEN #1:\"FILE\",RELATIVE,INTERNAL,UPDATE,FIXED,PERMANENT")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test valid OPEN with options in alternative order`() {
        configureFile("100 OPEN #1:\"FILE\",OUTPUT,SEQUENTIAL,DISPLAY,VARIABLE,PERMANENT")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test OPEN RELATIVE combined with VARIABLE is invalid`() {
        configureFile("100 <error descr=\"RELATIVE files require fixed-length records\">OPEN #1:\"FILE\",RELATIVE,VARIABLE</error>")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test OPEN duplicate organization option is invalid`() {
        configureFile("100 <error descr=\"Duplicate file organization option\">OPEN #1:\"FILE\",SEQUENTIAL,RELATIVE</error>")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test OPEN duplicate file type option is invalid`() {
        configureFile("100 <error descr=\"Duplicate file type option\">OPEN #1:\"FILE\",DISPLAY,INTERNAL</error>")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test OPEN duplicate open mode option is invalid`() {
        configureFile("100 <error descr=\"Duplicate open mode option\">OPEN #1:\"FILE\",INPUT,OUTPUT</error>")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test OPEN duplicate record format option is invalid`() {
        configureFile("100 <error descr=\"Duplicate record format option\">OPEN #1:\"FILE\",FIXED,VARIABLE</error>")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test OPEN duplicate lifetime option is invalid`() {
        configureFile("100 <error descr=\"Duplicate lifetime option\">OPEN #1:\"FILE\",PERMANENT,PERMANENT</error>")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test OPEN string expression as organization record count is invalid`() {
        configureFile("100 OPEN #1:\"FILE\",RELATIVE <error descr=\"Numeric expression expected\">\"X\"</error>")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test OPEN FIXED with numeric record length is valid`() {
        configureFile("100 OPEN #1:\"FILE\",FIXED 128")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test OPEN VARIABLE with numeric record length is valid`() {
        configureFile("100 OPEN #1:\"FILE\",VARIABLE 64")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test OPEN FIXED with string expression as record length is invalid`() {
        configureFile("100 OPEN #1:\"FILE\",FIXED <error descr=\"Numeric expression expected\">\"X\"</error>")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test OPEN VARIABLE with string expression as record length is invalid`() {
        configureFile("100 OPEN #1:\"FILE\",VARIABLE <error descr=\"Numeric expression expected\">\"X\"</error>")
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test OPEN all options including FIXED with record length is valid`() {
        configureFile("100 OPEN #2:\"CS1\",SEQUENTIAL,INTERNAL,INPUT,FIXED 128,PERMANENT")
        myFixture.checkHighlighting(true, false, false)
    }
}
