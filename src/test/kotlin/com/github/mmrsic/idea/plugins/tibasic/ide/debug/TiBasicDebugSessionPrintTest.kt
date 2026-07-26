package com.github.mmrsic.idea.plugins.tibasic.ide.debug

internal class TiBasicDebugSessionPrintTest : TiBasicDebugSessionBaseTest() {


    fun `test PRINT writes evaluated string output into row 24 from column 3`() {
        var session = startSession("100 PRINT \"HI\"")

        session = session.step()

        assertEquals("  HI ", screenText(session, 23, 1, 5))
    }

    fun `test PRINT without list inserts a single blank line`() {
        var session = startSession(
            """
            100 PRINT "A"
            110 PRINT
            120 PRINT "B"
            """.trimIndent(),
        )

        repeat(3) {
            session = session.step()
        }

        assertEquals("A", screenText(session, 21, 3, 1))
        assertEquals("     ", screenText(session, 22, 3, 5))
        assertEquals("B", screenText(session, 23, 3, 1))
    }

    fun `test PRINT with exactly twenty eight characters does not add a blank line`() {
        var session = startSession("100 PRINT \"1234567890123456789012345678\";")

        session = session.step()

        assertEquals("1234567890123456789012345678", screenText(session, 24, 3, 28))
        assertEquals(" >run", screenText(session, 23, 1, 5))
    }

    fun `test PRINT wraps after twenty eight characters and leaves outer columns unchanged`() {
        var session = startSession("100 PRINT \"ABCDEFGHIJKLMNOPQRSTUVWXYZ12!\"")

        session = session.step()

        assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXYZ12", screenText(session, 22, 3, 28))
        assertEquals("!", screenText(session, 23, 3, 1))
        assertEquals("  ", screenText(session, 24, 1, 2))
        assertEquals("  ", screenText(session, 24, 31, 2))
    }

    fun `test PRINT colon separator performs a line feed`() {
        var session = startSession("100 PRINT \"A\":\"B\"")

        session = session.step()

        assertEquals("A", screenText(session, 22, 3, 1))
        assertEquals("B", screenText(session, 23, 3, 1))
    }

    fun `test PRINT semicolon separator keeps the next item on the same line`() {
        var session = startSession("100 PRINT \"A\";\"B\"")

        session = session.step()

        assertEquals("AB", screenText(session, 23, 3, 2))
    }

    fun `test PRINT comma separator jumps to zone 2`() {
        var session = startSession("100 PRINT \"A\",\"B\"")

        session = session.step()

        assertEquals("A             ", screenText(session, 23, 3, 14))
        assertEquals("B", screenText(session, 23, 17, 1))
    }

    fun `test PRINT comma separator moves from zone 2 to the next line`() {
        var session = startSession("100 PRINT \"A\",\"B\",\"C\"")

        session = session.step()

        assertEquals("A             ", screenText(session, 22, 3, 14))
        assertEquals("B", screenText(session, 22, 17, 1))
        assertEquals("C", screenText(session, 23, 3, 1))
    }

    fun `test PRINT with semicolon separator is the same as PRINT TAB(15)`() {
        var session = startSession("100 PRINT ,\"A\"")

        session = session.step()

        assertEquals("                A               ", screenText(session, 23, 1, 32))
    }

    fun `test PRINT TAB moves to specified column`() {
        var session = startSession("100 PRINT TAB(10);\"A\"")

        session = session.step()

        // Column 10 means 10-1+3 = 12th position in the last row
        assertEquals("A", screenText(session, 23, 12, 1))
    }

    fun `test PRINT TAB with value less than one treats it as one`() {
        var session = startSession("100 PRINT TAB(-5);\"A\"")

        session = session.step()

        assertEquals("A", screenText(session, 23, 3, 1))
    }

    fun `test PRINT TAB with value greater than twenty eight uses modulo`() {
        var session = startSession("100 PRINT TAB(33);\"A\"")

        session = session.step()

        // 33 -> ((33-1) % 28) + 1 = 5. Index (5-1)+2 = 6 (index 6).
        assertEquals("A", screenText(session, 23, 7, 1))
    }

    fun `test PRINT multiple TAB calls in one line`() {
        var session = startSession("100 PRINT TAB(5);\"A\";TAB(15);\"B\"")

        session = session.step()

        assertEquals("A", screenText(session, 23, 7, 1))
        assertEquals("B", screenText(session, 23, 17, 1))
    }

    fun `test PRINT numeric value adds TI-Basic padding spaces`() {
        var session = startSession("100 PRINT 12")

        session = session.step()

        assertEquals(" 12 ", screenText(session, 23, 3, 4))
    }

    fun `test PRINT negative numeric value replaces the leading blank with a minus sign`() {
        var session = startSession("100 PRINT -12")

        session = session.step()

        assertEquals("-12 ", screenText(session, 23, 3, 4))
    }

    fun `test PRINT rounds normal decimal output at the tenth displayed digit`() {
        var session = startSession("100 PRINT 1.23456789056")

        session = session.step()

        assertEquals(" 1.234567891 ", screenText(session, 23, 3, 13))
    }

    fun `test PRINT uses scientific notation for large values`() {
        var session = startSession("100 PRINT 100000000000")

        session = session.step()

        assertEquals(" 1.0E11 ", screenText(session, 23, 3, 8))
    }

    fun `test PRINT rounds scientific notation to six significant digits`() {
        var session = startSession("100 PRINT 1234567890123")

        session = session.step()

        assertEquals(" 1.23457E12 ", screenText(session, 23, 3, 12))
    }

    fun `test PRINT uses scientific notation for very small values`() {
        var session = startSession("100 PRINT .0000000000123456")

        session = session.step()

        assertEquals(" 1.23456E-11 ", screenText(session, 23, 3, 13))
    }

    fun `test PRINT starts a string longer than twenty eight characters on the next line`() {
        var session = startSession("100 PRINT \"A\";\"12345678901234567890123456789\"")

        session = session.step()

        assertEquals("A", screenText(session, 21, 3, 1))
        assertEquals("1234567890123456789012345678", screenText(session, 22, 3, 28))
        assertEquals("9", screenText(session, 23, 3, 1))
    }

    fun `test PRINT moves a shorter string as a whole to the next line when it no longer fits`() {
        var session = startSession("100 PRINT \"12345678901234567890\";\"ABCDEFGHIJ\"")

        session = session.step()

        assertEquals("12345678901234567890", screenText(session, 22, 3, 20))
        assertEquals("ABCDEFGHIJ", screenText(session, 23, 3, 10))
    }

    fun `test PRINT omits the trailing numeric blank when it is the only overflowing character`() {
        var session = startSession("100 PRINT \"12345678901234567890123456\";6")

        session = session.step()

        assertEquals("12345678901234567890123456 6", screenText(session, 23, 3, 28))
    }

    fun `test PRINT without trailing separator performs implicit line feed before next print`() {
        var session = startSession(
            """
            100 PRINT "A"
            110 PRINT "B"
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()

        assertEquals("A", screenText(session, 22, 3, 1))
        assertEquals("B", screenText(session, 23, 3, 1))
    }

    fun `test PRINT without separator and trailing colon produce the same screen state`() {
        val withoutTrailingSeparator = startSession("10 PRINT \"A\"").step()
        val withTrailingColon = startSession("10 PRINT \"A\":").step()

        assertEquals(withoutTrailingSeparator.screenContents, withTrailingColon.screenContents)
    }

    fun `test PRINT with trailing semicolon`() {
        var session = startSession("10 PRINT \"A\";")

        session = session.step()

        assertEquals(" >run ", screenText(session, 23, 1, 6))
        assertEquals("  A ", screenText(session, 24, 1, 4))
    }

    fun `test PRINT with trailing semicolon and colon`() {
        var session = startSession("10 PRINT \"A\";:")

        session = session.step()

        assertEquals(" >run ", screenText(session, 22, 1, 6))
        assertEquals("A ", screenText(session, 23, 3, 2))
        assertEquals("  ", screenText(session, 24, 3, 2))
    }

    fun `test PRINT with trailing colon`() {
        var session = startSession("10 PRINT \"A\":")

        session = session.step()

        assertEquals("run ", screenText(session, 22, 3, 4))
        assertEquals("A ", screenText(session, 23, 3, 2))
        assertEquals("  ", screenText(session, 24, 3, 2))
    }

    fun `test PRINT with trailing colon and semicolon`() {
        var session = startSession("10 PRINT \"A\":;")

        session = session.step()

        assertEquals(" >run ", screenText(session, 22, 1, 6))
        assertEquals("A ", screenText(session, 23, 3, 2))
        assertEquals("  ", screenText(session, 24, 3, 2))
    }

    fun `test PRINT ending with semicolon keeps the next PRINT on the same line`() {
        var session = startSession(
            """
            100 PRINT "A";
            110 PRINT "B"
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()

        assertEquals("AB", screenText(session, 23, 3, 2))
    }

    fun `test PRINT ending with comma keeps the next PRINT in zone 2`() {
        var session = startSession(
            """
            100 PRINT "A",
            110 PRINT "B"
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()

        assertEquals("A             ", screenText(session, 23, 3, 14))
        assertEquals("B", screenText(session, 23, 17, 1))
    }

    fun `test PRINT with two trailing colons leaves one blank bottom line`() {
        var session = startSession("100 PRINT \"HELLO\"::")

        session = session.step()

        assertEquals("HELLO", screenText(session, 22, 3, 5))
        assertEquals("     ", screenText(session, 23, 3, 5))
        assertEquals("     ", screenText(session, 24, 3, 5))
    }

    fun `test PRINT with three trailing colons leaves two blank bottom lines`() {
        var session = startSession("100 PRINT \"HELLO\":::")

        session = session.step()

        assertEquals("HELLO", screenText(session, 21, 3, 5))
        assertEquals("     ", screenText(session, 22, 3, 5))
        assertEquals("     ", screenText(session, 23, 3, 5))
        assertEquals("     ", screenText(session, 24, 3, 5))
    }

    fun `test PRINT evaluates string array element reference`() {
        var session = startSession(
            """
            100 LET I=1
            110 LET R$(I)="READY"
            120 PRINT STR$(I);". ";R$(I)
            130 END
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()
        session = session.step()

        assertEquals("1. READY", screenText(session, 23, 3, 8))
        assertEquals(130, session.currentProgramLine?.lineNumber)
    }

    fun `test PRINT colons and semicolons are evaluated correctly`() {
        var session = startSession(
            """
            650 PRINT "BITTE GEBEN SIE DIE",,"SEITENLAENGE A EIN (cm):",,,
            660 INPUT "":X
            670 PRINT :,,:"VOLUMEN :":,,X^3;"ccm":,,;"OBERFLAECHE :":,,6*X^2;"qcm":,,:,,
            """.trimIndent(),
        )

        session = session.step()
        session = session.copy(pendingInputValues = mapOf("X" to "4"))
        session = session.step()
        session = session.step()

        assertEquals(" >run ", screenText(session, 4, 1, 6))
        assertEquals("BITTE GEBEN SIE DIE     ", screenText(session, 5, 3, 24))
        assertEquals("             ", screenText(session, 6, 3, 13))
        assertEquals("SEITENLAENGE A EIN (cm):", screenText(session, 7, 3, 24))
        assertEquals("             ", screenText(session, 8, 3, 13))
        assertEquals("4            ", screenText(session, 9, 3, 13))
        assertEquals("             ", screenText(session, 10, 3, 13))
        assertEquals("             ", screenText(session, 11, 3, 13))
        assertEquals("             ", screenText(session, 12, 3, 13))
        assertEquals("VOLUMEN :    ", screenText(session, 13, 3, 13))
        assertEquals("             ", screenText(session, 14, 3, 13))
        assertEquals(" 64 ccm      ", screenText(session, 15, 3, 13))
        assertEquals("             ", screenText(session, 16, 3, 13))
        assertEquals("             ", screenText(session, 17, 3, 13))
        assertEquals("OBERFLAECHE :", screenText(session, 18, 3, 13))
        assertEquals("             ", screenText(session, 19, 3, 13))
        assertEquals(" 96 qcm      ", screenText(session, 20, 3, 13))
    }

}