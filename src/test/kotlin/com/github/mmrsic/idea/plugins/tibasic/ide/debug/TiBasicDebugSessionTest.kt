package com.github.mmrsic.idea.plugins.tibasic.ide.debug

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase
import com.github.mmrsic.idea.plugins.tibasic.editor.TiBasicSoundNoise
import com.github.mmrsic.idea.plugins.tibasic.editor.TiBasicSoundPlayback
import com.github.mmrsic.idea.plugins.tibasic.editor.TiBasicSoundTone
import com.github.mmrsic.idea.plugins.tibasic.language.model.TiColor
import java.math.BigDecimal

class TiBasicDebugSessionTest : TiBasicTestBase() {

    fun `test initial session starts at lowest program line number`() {
        val session = startSession(
            """
            200 PRINT "SECOND"
            100 PRINT "FIRST"
            """.trimIndent(),
        )

        assertEquals(TiBasicDebugSessionStatus.Paused, session.status)
        assertEquals(100, session.currentProgramLine?.lineNumber)
        assertEquals(1, session.currentSourceLineIndex)
    }

    fun `test sequential stepping visits next higher line number`() {
        var session = startSession(
            """
            100 PRINT "A"
            140 PRINT "C"
            120 PRINT "B"
            """.trimIndent(),
        )

        session = session.step()
        assertEquals(120, session.currentProgramLine?.lineNumber)

        session = session.step()
        assertEquals(140, session.currentProgramLine?.lineNumber)
    }

    fun `test sequential stepping skips REM lines`() {
        var session = startSession(
            """
            100 PRINT "A"
            110 REM comment
            120 REM more
            130 PRINT "B"
            """.trimIndent(),
        )

        session = session.step()

        assertEquals(130, session.currentProgramLine?.lineNumber)
    }

    fun `test stepping on REM line continues to next non REM line`() {
        var session = startSession(
            """
            100 REM comment
            110 REM more
            120 PRINT "B"
            """.trimIndent(),
        )

        session = session.step()

        assertEquals(120, session.currentProgramLine?.lineNumber)
    }

    fun `test CALL CLEAR sets all screen positions to code 32`() {
        var session = startSession(
            """
            100 CALL CLEAR
            110 PRINT "A"
            """.trimIndent(),
        )

        session = session.step()

        assertTrue(session.screenContents.characterCodes.flatten().all { code -> code == 32 })
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test CALL CLEAR keeps current debug screen background`() {
        var session = startSession(
            """
            100 CALL SCREEN(2)
            110 CALL CLEAR
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()

        assertEquals(TiColor.Black, session.screenContents.screenBackground)
    }

    fun `test CALL SCREEN updates debug screen background`() {
        var session = startSession(
            """
            100 CALL SCREEN(2)
            110 PRINT "A"
            """.trimIndent(),
        )

        session = session.step()

        assertEquals(TiColor.Black, session.screenContents.screenBackground)
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test CALL CHAR updates debug character pattern override`() {
        var session = startSession(
            """
            100 CALL CHAR(65,"F0")
            110 PRINT "A"
            """.trimIndent(),
        )

        session = session.step()

        assertEquals("F000000000000000", session.screenContents.characterPatterns[65])
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test CALL CHAR current arguments display shows code pattern and pixel representation label`() {
        val session = startSession("100 CALL CHAR(65,\"F0\")")

        assertEquals(
            """
            ascii-code = 65
            pattern-string = F000000000000000
            (pixel-representation)
            """.trimIndent(),
            session.currentArgumentsDisplay,
        )
        assertEquals("F000000000000000", session.currentArgumentPatternPreview)
    }

    fun `test CALL CHAR ignores digits after the sixteenth`() {
        var session = startSession(
            """
            100 CALL CHAR(65,"1234567890ABCDEF99")
            110 PRINT "A"
            """.trimIndent(),
        )

        session = session.step()

        assertEquals("1234567890ABCDEF", session.screenContents.characterPatterns[65])
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test CALL CHAR current arguments display warns about ignored tail`() {
        val session = startSession("100 CALL CHAR(65,\"1234567890ABCDEF99\")")

        assertEquals(
            """
            ascii-code = 65
            pattern-string = 1234567890ABCDEF (ignored tail: 99)
            (pixel-representation)
            """.trimIndent(),
            session.currentArgumentsDisplay,
        )
        assertEquals("1234567890ABCDEF", session.currentArgumentPatternPreview)
    }

    fun `test CALL CHAR with lowercase pattern shows bad value`() {
        val previewSession = startSession("100 CALL CHAR(65,\"ff\")")

        assertEquals(
            """
            ascii-code = 65
            pattern-string = ff
            Bad Value: pattern-string=ff
            """.trimIndent(),
            previewSession.currentArgumentsDisplay,
        )
        assertNull(previewSession.currentArgumentPatternPreview)

        var session = previewSession

        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.PendingStop, session.status)
        assertEquals(TiBasicDebugMetadata.message(TiBasicDebugMetadata.badValueKey, "pattern-string=ff"), session.statusMessage)
    }

    fun `test CALL SCREEN current arguments display shows resolved color code and name`() {
        var session = startSession(
            """
            100 LET C=2
            110 CALL SCREEN(C+1)
            """.trimIndent(),
        )

        session = session.step()

        assertEquals("color-code = 03 (Medium Green)", session.currentArgumentsDisplay)
    }

    fun `test CALL SCREEN current arguments display shows incorrect expression`() {
        val session = startSession("100 CALL SCREEN(1/0)")

        assertEquals("<incorrect expression>", session.currentArgumentsDisplay)
    }

    fun `test CALL SCREEN current arguments display shows string number mismatch`() {
        val session = startSession("100 CALL SCREEN(\"A\")")

        assertEquals("<incorrect expression> (string-number-mismatch)", session.currentArgumentsDisplay)
    }

    fun `test CALL SCREEN treats transparent color as black in debugger`() {
        var session = startSession(
            """
            100 CALL SCREEN(1)
            110 PRINT "A"
            """.trimIndent(),
        )

        session = session.step()

        assertEquals(TiColor.Black, session.screenContents.screenBackground)
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test debug session initializes sixteen CALL COLOR character sets`() {
        val session = startSession("100 PRINT \"A\"")

        assertEquals((1..16).toSet(), session.screenContents.characterSetColors.keys)
        assertTrue(session.screenContents.characterSetColors.values.all { colors ->
            colors.fg == TiColor.Black && colors.bg == TiColor.Transparent
        })
    }

    fun `test CALL COLOR updates rounded character set colors`() {
        var session = startSession(
            """
            100 CALL COLOR(5.4,3.2,1.2)
            110 PRINT "A"
            """.trimIndent(),
        )

        session = session.step()

        assertEquals(TiColor.MediumGreen, session.screenContents.characterSetColors[5]?.fg)
        assertEquals(TiColor.Transparent, session.screenContents.characterSetColors[5]?.bg)
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test CALL COLOR with invalid rounded character set shows bad value`() {
        var session = startSession("100 CALL COLOR(16.6,2,1)")

        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.PendingStop, session.status)
        assertEquals(TiBasicDebugMetadata.message(TiBasicDebugMetadata.badValueKey, "character set=17"), session.statusMessage)
    }

    fun `test CALL COLOR with string argument shows string number mismatch`() {
        var session = startSession("100 CALL COLOR(\"A\",2,1)")

        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.PendingStop, session.status)
        assertEquals(TiBasicDebugMetadata.message(TiBasicDebugMetadata.stringNumberMismatchKey), session.statusMessage)
    }

    fun `test CALL HCHAR current arguments display shows resolved values`() {
        val session = startSession("100 CALL HCHAR(2,3,65,4)")

        assertEquals(
            """
            row = 02
            column = 03
            character-code = 65
            repeat = 04
            """.trimIndent(),
            session.currentArgumentsDisplay,
        )
    }

    fun `test CALL HCHAR writes repeated character codes across row wrap`() {
        var session = startSession(
            """
            100 CALL HCHAR(24,32,65,3)
            110 PRINT "A"
            """.trimIndent(),
        )

        session = session.step()

        assertEquals(65, session.screenContents.characterCodes[23][31])
        assertEquals(65, session.screenContents.characterCodes[0][0])
        assertEquals(65, session.screenContents.characterCodes[0][1])
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test CALL VCHAR writes repeated character codes across column wrap`() {
        var session = startSession(
            """
            100 CALL VCHAR(24,32,66,3)
            110 PRINT "A"
            """.trimIndent(),
        )

        session = session.step()

        assertEquals(66, session.screenContents.characterCodes[23][31])
        assertEquals(66, session.screenContents.characterCodes[0][0])
        assertEquals(66, session.screenContents.characterCodes[1][0])
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test CALL HCHAR caps screen writes after seven hundred sixty eight placements`() {
        var session = startSession(
            """
            100 CALL HCHAR(1,1,67,1000)
            110 PRINT "A"
            """.trimIndent(),
        )

        session = session.step()

        assertTrue(session.screenContents.characterCodes.flatten().all { code -> code == 67 })
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test numeric LET with string expression shows string number mismatch`() {
        var session = startSession("100 LET A=\"HELLO\"")

        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.PendingStop, session.status)
        assertEquals(TiBasicDebugMetadata.message(TiBasicDebugMetadata.stringNumberMismatchKey), session.statusMessage)
    }

    fun `test string LET with numeric expression shows string number mismatch`() {
        var session = startSession("100 LET A$=5")

        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.PendingStop, session.status)
        assertEquals(TiBasicDebugMetadata.message(TiBasicDebugMetadata.stringNumberMismatchKey), session.statusMessage)
    }

    fun `test CALL SOUND step resolves playback from current debugger variables`() {
        var session = startSession(
            """
            100 LET P=440
            110 CALL SOUND(120,P,2)
            """.trimIndent(),
        )

        session = session.step()
        val stepResult = session.stepWithEffects()

        assertEquals(
            TiBasicSoundPlayback(
                duration = 120,
                tones = listOf(TiBasicSoundTone(pitch = 440, volume = 2)),
            ),
            stepResult.soundPlayback,
        )
        assertEquals(TiBasicDebugSessionStatus.PendingStop, stepResult.session.status)
    }

    fun `test CALL SOUND noise with tone3 reuses previous debugger tone3 pitch`() {
        var session = startSession(
            """
            100 CALL SOUND(10,110,1,220,2,330,3)
            110 CALL SOUND(10,-4,4)
            """.trimIndent(),
        )

        session = session.stepWithEffects().session
        val stepResult = session.stepWithEffects()

        assertEquals(
            TiBasicSoundPlayback(
                duration = 10,
                tones = emptyList(),
                noise = TiBasicSoundNoise(selector = -4, volume = 4, tone3Pitch = 330),
            ),
            stepResult.soundPlayback,
        )
        assertEquals(TiBasicDebugSessionStatus.PendingStop, stepResult.session.status)
    }

    fun `test PRINT writes evaluated string output into row 24 from column 3`() {
        var session = startSession("100 PRINT \"HI\"")

        session = session.step()

        assertEquals(72, session.screenContents.characterCodes[23][2])
        assertEquals(73, session.screenContents.characterCodes[23][3])
        assertEquals(32, session.screenContents.characterCodes[23][0])
        assertEquals(32, session.screenContents.characterCodes[23][1])
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

        assertEquals("A", screenText(session, 22, 3, 1))
        assertEquals("     ", screenText(session, 23, 3, 5))
        assertEquals("B", screenText(session, 24, 3, 1))
    }

    fun `test PRINT with exactly twenty eight characters does not add a blank line`() {
        var session = startSession("100 PRINT \"1234567890123456789012345678\"")

        session = session.step()

        assertEquals("1234567890123456789012345678", screenText(session, 24, 3, 28))
        assertEquals("> run", screenText(session, 23, 3, 5))
    }

    fun `test PRINT wraps after twenty eight characters and leaves outer columns unchanged`() {
        var session = startSession("100 PRINT \"ABCDEFGHIJKLMNOPQRSTUVWXYZ12!\"")

        session = session.step()

        assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXYZ12", screenText(session, 23, 3, 28))
        assertEquals("!", session.screenContents.characterCodes[23][2].toChar().toString())
        assertEquals(32, session.screenContents.characterCodes[23][0])
        assertEquals(32, session.screenContents.characterCodes[23][1])
        assertEquals(32, session.screenContents.characterCodes[23][30])
        assertEquals(32, session.screenContents.characterCodes[23][31])
    }

    fun `test PRINT colon separator performs a line feed`() {
        var session = startSession("100 PRINT \"A\":\"B\"")

        session = session.step()

        assertEquals('A'.code, session.screenContents.characterCodes[22][2])
        assertEquals('B'.code, session.screenContents.characterCodes[23][2])
    }

    fun `test PRINT semicolon separator keeps the next item on the same line`() {
        var session = startSession("100 PRINT \"A\";\"B\"")

        session = session.step()

        assertEquals("AB", screenText(session, 24, 3, 2))
    }

    fun `test PRINT comma separator jumps to zone 2`() {
        var session = startSession("100 PRINT \"A\",\"B\"")

        session = session.step()

        assertEquals("A             ", screenText(session, 24, 3, 14))
        assertEquals("B", screenText(session, 24, 17, 1))
    }

    fun `test PRINT comma separator moves from zone 2 to the next line`() {
        var session = startSession("100 PRINT \"A\",\"B\",\"C\"")

        session = session.step()

        assertEquals("A             ", screenText(session, 23, 3, 14))
        assertEquals("B", screenText(session, 23, 17, 1))
        assertEquals("C", screenText(session, 24, 3, 1))
    }

    fun `test PRINT numeric value adds TI-Basic padding spaces`() {
        var session = startSession("100 PRINT 12")

        session = session.step()

        assertEquals(" 12 ", screenText(session, 24, 3, 4))
    }

    fun `test PRINT negative numeric value replaces the leading blank with a minus sign`() {
        var session = startSession("100 PRINT -12")

        session = session.step()

        assertEquals("-12 ", screenText(session, 24, 3, 4))
    }

    fun `test PRINT rounds normal decimal output at the tenth displayed digit`() {
        var session = startSession("100 PRINT 1.23456789056")

        session = session.step()

        assertEquals(" 1.234567891 ", screenText(session, 24, 3, 13))
    }

    fun `test PRINT uses scientific notation for large values`() {
        var session = startSession("100 PRINT 100000000000")

        session = session.step()

        assertEquals(" 1.0E11 ", screenText(session, 24, 3, 8))
    }

    fun `test PRINT rounds scientific notation to six significant digits`() {
        var session = startSession("100 PRINT 1234567890123")

        session = session.step()

        assertEquals(" 1.23457E12 ", screenText(session, 24, 3, 12))
    }

    fun `test PRINT uses scientific notation for very small values`() {
        var session = startSession("100 PRINT .0000000000123456")

        session = session.step()

        assertEquals(" 1.23456E-11 ", screenText(session, 24, 3, 13))
    }

    fun `test PRINT starts a string longer than twenty eight characters on the next line`() {
        var session = startSession("100 PRINT \"A\";\"12345678901234567890123456789\"")

        session = session.step()

        assertEquals("A", screenText(session, 22, 3, 1))
        assertEquals("1234567890123456789012345678", screenText(session, 23, 3, 28))
        assertEquals("9", screenText(session, 24, 3, 1))
    }

    fun `test PRINT moves a shorter string as a whole to the next line when it no longer fits`() {
        var session = startSession("100 PRINT \"12345678901234567890\";\"ABCDEFGHIJ\"")

        session = session.step()

        assertEquals("12345678901234567890", screenText(session, 23, 3, 20))
        assertEquals("ABCDEFGHIJ", screenText(session, 24, 3, 10))
    }

    fun `test PRINT omits the trailing numeric blank when it is the only overflowing character`() {
        var session = startSession("100 PRINT \"12345678901234567890123456\";6")

        session = session.step()

        assertEquals("12345678901234567890123456 6", screenText(session, 24, 3, 28))
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

        assertEquals('A'.code, session.screenContents.characterCodes[22][2])
        assertEquals('B'.code, session.screenContents.characterCodes[23][2])
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

        assertEquals("AB", screenText(session, 24, 3, 2))
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

        assertEquals("A             ", screenText(session, 24, 3, 14))
        assertEquals("B", screenText(session, 24, 17, 1))
    }

    fun `test PRINT with two trailing colons leaves one blank bottom line`() {
        var session = startSession("100 PRINT \"HELLO\"::")

        session = session.step()

        assertEquals("HELLO", screenText(session, 23, 3, 5))
        assertEquals("     ", screenText(session, 24, 3, 5))
    }

    fun `test PRINT with three trailing colons leaves two blank bottom lines`() {
        var session = startSession("100 PRINT \"HELLO\":::")

        session = session.step()

        assertEquals("HELLO", screenText(session, 22, 3, 5))
        assertEquals("     ", screenText(session, 23, 3, 5))
        assertEquals("     ", screenText(session, 24, 3, 5))
    }

    fun `test GOTO jumps to target line`() {
        var session = startSession(
            """
            100 GOTO 300
            200 PRINT "SKIP"
            300 PRINT "DONE"
            """.trimIndent(),
        )

        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.Paused, session.status)
        assertEquals(300, session.currentProgramLine?.lineNumber)
    }

    fun `test GOSUB and RETURN continue after calling line`() {
        var session = startSession(
            """
            100 GOSUB 300
            110 PRINT "AFTER"
            300 PRINT "IN SUB"
            310 RETURN
            """.trimIndent(),
        )

        session = session.step()
        assertEquals(300, session.currentProgramLine?.lineNumber)
        assertEquals(listOf(100), session.gosubOriginLineNumbers)

        session = session.step()
        assertEquals(310, session.currentProgramLine?.lineNumber)

        session = session.step()
        assertEquals(110, session.currentProgramLine?.lineNumber)
        assertTrue(session.gosubOriginLineNumbers.isEmpty())
    }

    fun `test IF with non-zero numeric expression jumps to THEN line`() {
        var session = startSession(
            """
            100 LET X=2
            110 IF X-1 THEN 300
            200 PRINT "NO"
            300 PRINT "YES"
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.Paused, session.status)
        assertEquals(300, session.currentProgramLine?.lineNumber)
    }

    fun `test IF with zero numeric expression uses implicit else continuation`() {
        var session = startSession(
            """
            100 LET X=1
            110 IF X-1 THEN 300
            200 PRINT "NO"
            300 PRINT "YES"
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.Paused, session.status)
        assertEquals(200, session.currentProgramLine?.lineNumber)
    }

    fun `test IF with false comparison jumps to explicit ELSE line`() {
        var session = startSession(
            """
            100 LET X=1
            110 IF X>5 THEN 300 ELSE 200
            200 PRINT "NO"
            300 PRINT "YES"
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.Paused, session.status)
        assertEquals(200, session.currentProgramLine?.lineNumber)
    }

    fun `test IF with true string comparison jumps to THEN line`() {
        var session = startSession(
            """
            100 LET A$="YES"
            110 IF A$="YES" THEN 300 ELSE 200
            200 PRINT "NO"
            300 PRINT "OK"
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.Paused, session.status)
        assertEquals(300, session.currentProgramLine?.lineNumber)
    }

    fun `test IF current arguments display shows evaluated numeric subexpressions in evaluation order`() {
        var session = startSession(
            """
            100 LET X=2
            110 IF X-1 THEN 300
            300 PRINT "YES"
            """.trimIndent(),
        )

        session = session.step()

        assertEquals(
            listOf(
                "2 - 1 -> 1",
                "1 -> true",
            ),
            session.currentArgumentDisplays,
        )
    }

    fun `test IF current arguments display resolves string subexpressions with variable values`() {
        var session = startSession(
            """
            100 LET A$="Y"
            110 IF A$&"ES"="YES" THEN 300
            300 PRINT "YES"
            """.trimIndent(),
        )

        session = session.step()

        assertEquals(
            listOf(
                "\"Y\" & \"ES\" -> \"YES\"",
                "\"YES\" = \"YES\" -> true",
            ),
            session.currentArgumentDisplays,
        )
    }

    fun `test FOR current arguments display shows evaluated expressions with explicit increment`() {
        var session = startSession(
            """
            100 LET A=2
            110 LET B=4
            120 FOR I=A+1 TO B*2 STEP A-1
            130 NEXT I
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()

        assertEquals(
            listOf(
                "initial-value = 03",
                "limit = 08",
                "increment = 01",
                "(iterations = 6)",
            ),
            session.currentArgumentDisplays,
        )
    }

    fun `test FOR current arguments display uses implicit increment one`() {
        val session = startSession(
            """
            100 FOR I=2 TO 5
            110 NEXT I
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                "initial-value = 02",
                "limit = 05",
                "increment = 01",
                "(iterations = 4)",
            ),
            session.currentArgumentDisplays,
        )
    }

    fun `test FOR step assigns initial value to control variable`() {
        var session = startSession(
            """
            100 LET A=2
            110 FOR I=A+1 TO 5
            120 PRINT I
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()

        assertEquals("3", session.numericVariables["I"]?.usualDisplay)
        assertEquals(120, session.currentProgramLine?.lineNumber)
    }

    fun `test NEXT current arguments display shows increment adjusted control variable and jump decision`() {
        var session = startSession(
            """
            100 LET A=2
            110 LET B=1
            120 FOR I=A+1 TO 5 STEP B
            130 NEXT I
            140 PRINT I
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()
        session = session.step()

        assertEquals(
            listOf(
                "increment = 01",
                "control-variable I = 4",
                "limit = 05 (jump to 130)",
            ),
            session.currentArgumentDisplays,
        )
    }

    fun `test NEXT step adjusts control variable and jumps back into loop`() {
        var session = startSession(
            """
            100 LET A=2
            110 LET B=1
            120 FOR I=A+1 TO 5 STEP B
            130 NEXT I
            140 PRINT I
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()
        session = session.step()
        session = session.step()

        assertEquals("4", session.numericVariables["I"]?.usualDisplay)
        assertEquals(130, session.currentProgramLine?.lineNumber)
    }

    fun `test NEXT current arguments display shows loop end decision when limit is exceeded`() {
        var session = startSession(
            """
            100 FOR I=4 TO 5
            110 NEXT I
            120 PRINT I
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()

        assertEquals(
            listOf(
                "increment = 01",
                "control-variable I = 6",
                "limit = 05 (loop end)",
            ),
            session.currentArgumentDisplays,
        )
    }

    fun `test NEXT step ends loop when incremented value exceeds limit`() {
        var session = startSession(
            """
            100 FOR I=4 TO 5
            110 NEXT I
            120 PRINT I
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()
        session = session.step()

        assertEquals("6", session.numericVariables["I"]?.usualDisplay)
        assertEquals(120, session.currentProgramLine?.lineNumber)
    }

    fun `test RETURN without GOSUB shows runtime error then stops on next step`() {
        var session = startSession("100 RETURN")

        session = session.step()
        assertEquals(TiBasicDebugSessionStatus.PendingStop, session.status)
        assertEquals(TiBasicDebugMetadata.message(TiBasicDebugMetadata.cantDoThatKey), session.statusMessage)
        assertEquals(100, session.currentProgramLine?.lineNumber)

        session = session.step()
        assertEquals(TiBasicDebugSessionStatus.Stopped, session.status)
        assertNull(session.currentProgramLine)
    }

    fun `test missing GOTO target line shows bad line number then stops on next step`() {
        var session = startSession("100 GOTO 999")

        session = session.step()
        assertEquals(TiBasicDebugSessionStatus.PendingStop, session.status)
        assertEquals(TiBasicDebugMetadata.message(TiBasicDebugMetadata.badLineNumberKey), session.statusMessage)

        session = session.step()
        assertEquals(TiBasicDebugSessionStatus.Stopped, session.status)
    }

    fun `test IF with missing THEN target line shows bad line number then stops on next step`() {
        var session = startSession("100 IF 1 THEN 999")

        session = session.step()
        assertEquals(TiBasicDebugSessionStatus.PendingStop, session.status)
        assertEquals(TiBasicDebugMetadata.message(TiBasicDebugMetadata.badLineNumberKey), session.statusMessage)

        session = session.step()
        assertEquals(TiBasicDebugSessionStatus.Stopped, session.status)
    }

    fun `test malformed supported statement shows incorrect statement`() {
        val malformedLines = listOf(
            "100 GOTO",
            "100 GOSUB X",
            "100 IF X>0",
            "100 IF X>0 THEN",
            "100 IF X>0 THEN 200 ELSE",
            "100 RETURN 1",
            "100 END 1",
            "100 STOP 1",
        )

        malformedLines.forEach { line ->
            val session = startSession(line).step()
            assertEquals(line, TiBasicDebugSessionStatus.PendingStop, session.status)
            assertEquals(line, TiBasicDebugMetadata.message(TiBasicDebugMetadata.incorrectStatementKey), session.statusMessage)
        }
    }

    fun `test valid but unsupported statements are ignored and step sequentially`() {
        var session = startSession(
            """
            100 PRINT "A"
            110 DATA 1
            120 PRINT "B"
            """.trimIndent(),
        )

        session = session.step()
        assertEquals(110, session.currentProgramLine?.lineNumber)

        session = session.step()
        assertEquals(120, session.currentProgramLine?.lineNumber)
    }

    fun `test unknown statement shows incorrect statement and stops on next step`() {
        var session = startSession("100 BLAH")

        session = session.step()
        assertEquals(TiBasicDebugSessionStatus.PendingStop, session.status)
        assertEquals(TiBasicDebugMetadata.message(TiBasicDebugMetadata.incorrectStatementKey), session.statusMessage)
        assertEquals(100, session.currentProgramLine?.lineNumber)

        session = session.step()
        assertEquals(TiBasicDebugSessionStatus.Stopped, session.status)
    }

    fun `test END enters pending stop and finishes on next step`() {
        var session = startSession("100 END")

        session = session.step()
        assertEquals(TiBasicDebugSessionStatus.PendingStop, session.status)
        assertNull(session.statusMessage)

        session = session.step()
        assertEquals(TiBasicDebugSessionStatus.Stopped, session.status)
    }

    fun `test unknown numeric references in program code are initialized to zero`() {
        var session = startSession(
            """
            100 PRINT X
            110 PRINT X
            """.trimIndent(),
        )

        session = session.step()

        assertEquals("0", session.numericVariables["X"]?.usualDisplay)
        assertEquals(listOf(0, 0, 0, 0, 0, 0, 0, 0), session.numericVariables["X"]?.internalBytes)
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test numeric LET evaluates expression and stores numeric variable`() {
        var session = startSession(
            """
            100 LET A=2
            110 LET B=A+3
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()

        assertEquals("2", session.numericVariables["A"]?.usualDisplay)
        assertEquals("5", session.numericVariables["B"]?.usualDisplay)
    }

    fun `test numeric LET initializes unknown numeric references before evaluation`() {
        var session = startSession(
            """
            100 LET B=A+2
            110 PRINT B
            """.trimIndent(),
        )

        session = session.step()

        assertEquals("0", session.numericVariables["A"]?.usualDisplay)
        assertEquals("2", session.numericVariables["B"]?.usualDisplay)
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test string LET stores TI-Basic internal string representation`() {
        var session = startSession(
            """
            100 LET A$="HELLO"
            110 PRINT A$
            """.trimIndent(),
        )

        session = session.step()

        assertEquals("HELLO", session.stringVariables["A$"]?.text)
        assertEquals("05 H E L L O", session.stringVariables["A$"]?.internalDisplay)
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test string LET can copy from known string variable`() {
        var session = startSession(
            """
            100 LET A$="HI"
            110 LET B$=A$
            120 PRINT B$
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()

        assertEquals("HI", session.stringVariables["B$"]?.text)
        assertEquals("02 H I", session.stringVariables["B$"]?.internalDisplay)
        assertEquals(120, session.currentProgramLine?.lineNumber)
    }

    fun `test unknown string reference on right side is initialized as empty string`() {
        var session = startSession(
            """
            100 LET B$=A$
            110 PRINT B$
            """.trimIndent(),
        )

        session = session.step()

        assertEquals("", session.stringVariables["A$"]?.text)
        assertEquals("00", session.stringVariables["A$"]?.internalDisplay)
        assertEquals("", session.stringVariables["B$"]?.text)
        assertEquals("00", session.stringVariables["B$"]?.internalDisplay)
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test unknown string reference in concatenation is initialized before assignment`() {
        var session = startSession(
            """
            100 LET B$=A$&"X"
            110 PRINT B$
            """.trimIndent(),
        )

        session = session.step()

        assertEquals("", session.stringVariables["A$"]?.text)
        assertEquals("00", session.stringVariables["A$"]?.internalDisplay)
        assertEquals("X", session.stringVariables["B$"]?.text)
        assertEquals("01 X", session.stringVariables["B$"]?.internalDisplay)
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test malformed LET statement shows incorrect statement and stops on next step`() {
        var session = startSession("100 LET A$=")

        session = session.step()
        assertEquals(TiBasicDebugSessionStatus.PendingStop, session.status)
        assertEquals(TiBasicDebugMetadata.message(TiBasicDebugMetadata.incorrectStatementKey), session.statusMessage)
        assertEquals(100, session.currentProgramLine?.lineNumber)

        session = session.step()
        assertEquals(TiBasicDebugSessionStatus.Stopped, session.status)
    }

    fun `test long string LET warns about truncation to 255 characters`() {
        val overlongString = "A".repeat(256)
        var session = startSession(
            """
            100 LET A$="$overlongString"
            110 PRINT A$
            """.trimIndent(),
        )

        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.Paused, session.status)
        assertEquals(TiBasicDebugMetadata.message(TiBasicDebugMetadata.stringCutTo255CharactersKey), session.statusMessage)
        assertEquals(255, session.stringVariables["A$"]?.text?.length)
        assertTrue(session.stringVariables["A$"]?.internalDisplay?.startsWith("FF A A A") == true)
    }

    fun `test intermediate string results are truncated before reuse`() {
        val overlongPrefix = "A".repeat(255)
        var session = startSession(
            """
            100 LET B$=SEG$("$overlongPrefix"&"BC",256,1)
            110 PRINT B$
            """.trimIndent(),
        )

        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.Paused, session.status)
        assertEquals(TiBasicDebugMetadata.message(TiBasicDebugMetadata.stringCutTo255CharactersKey), session.statusMessage)
        assertEquals("", session.stringVariables["B$"]?.text)
        assertEquals("00", session.stringVariables["B$"]?.internalDisplay)
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test string LET supports concatenation and CHR dollar`() {
        var session = startSession(
            """
            10 a$=""
            20 b$="1"
            30 c$=a$&b$
            40 d$=chr$(27)&c$&c$&chr$(48)
            """.trimIndent(),
        )

        repeat(4) { session = session.step() }

        assertEquals("1", session.stringVariables["C$"]?.text)
        assertEquals("01 1", session.stringVariables["C$"]?.internalDisplay)
        assertEquals("04 1B 1 1 0", session.stringVariables["D$"]?.internalDisplay)
        assertEquals("${27.toChar()}110", session.stringVariables["D$"]?.text)
    }

    fun `test string LET supports STR dollar`() {
        var session = startSession(
            """
            100 LET A$=STR$(23)
            110 LET B$=STR$(-5)
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()

        assertEquals("23", session.stringVariables["A$"]?.text)
        assertEquals(listOf(2, 50, 51), session.stringVariables["A$"]?.internalBytes)
        assertEquals("-5", session.stringVariables["B$"]?.text)
        assertEquals(listOf(2, 45, 53), session.stringVariables["B$"]?.internalBytes)
    }

    fun `test numeric LET supports INT function with positive decimal`() {
        var session = startSession(
            """
            100 LET A=INT(123.45)
            110 PRINT A
            """.trimIndent(),
        )

        session = session.step()

        assertEquals("123", session.numericVariables["A"]?.usualDisplay)
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test numeric LET supports INT function with negative decimal`() {
        var session = startSession(
            """
            100 LET A=INT(-123.45)
            110 PRINT A
            """.trimIndent(),
        )

        session = session.step()

        assertEquals("-124", session.numericVariables["A"]?.usualDisplay)
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test implicit numeric LET supports INT around RND without parentheses`() {
        var session = startSession(
            """
            920 K=INT(22*RND)+1
            930 PRINT K
            """.trimIndent(),
        )

        session = session.step()

        val value = session.numericVariables["K"]?.value
        assertEquals(930, session.currentProgramLine?.lineNumber)
        assertTrue(value != null && value >= BigDecimal.ONE && value <= BigDecimal("22"))
    }

    fun `test RND produces the same initial sequence in separate debugger sessions`() {
        var firstSession = startSession(
            """
            100 LET A=RND
            110 LET B=RND
            """.trimIndent(),
        )
        var secondSession = startSession(
            """
            100 LET A=RND
            110 LET B=RND
            """.trimIndent(),
        )

        firstSession = firstSession.step()
        firstSession = firstSession.step()
        secondSession = secondSession.step()
        secondSession = secondSession.step()

        val firstA = firstSession.numericVariables["A"]?.value
        val firstB = firstSession.numericVariables["B"]?.value
        val secondA = secondSession.numericVariables["A"]?.value
        val secondB = secondSession.numericVariables["B"]?.value
        assertEquals("0.52918778230732", firstSession.numericVariables["A"]?.usualDisplay)
        assertEquals("0.3913360723005", firstSession.numericVariables["B"]?.usualDisplay)
        assertEquals(firstA, secondA)
        assertEquals(firstB, secondB)
        assertTrue(firstA != null && firstA > BigDecimal.ZERO && firstA < BigDecimal.ONE)
        assertTrue(firstB != null && firstB > BigDecimal.ZERO && firstB < BigDecimal.ONE)
    }

    fun `test RANDOMIZE with equal seed reproduces the same RND sequence`() {
        var session = startSession(
            """
            100 RANDOMIZE 42.9
            110 LET A=RND
            120 LET B=RND
            130 RANDOMIZE 42
            140 LET C=RND
            150 LET D=RND
            """.trimIndent(),
        )

        repeat(6) {
            session = session.step()
        }

        assertEquals(session.numericVariables["A"]?.value, session.numericVariables["C"]?.value)
        assertEquals(session.numericVariables["B"]?.value, session.numericVariables["D"]?.value)
    }

    fun `test implicit numeric LET supports division by negative literal`() {
        var session = startSession(
            """
            1050 Y=8
            1060 Y=Y/-4
            1070 PRINT Y
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()

        assertEquals("-2", session.numericVariables["Y"]?.usualDisplay)
        assertEquals(1070, session.currentProgramLine?.lineNumber)
    }

    fun `test string LET supports SEG dollar`() {
        var session = startSession(
            """
            100 LET A$="HELLO"
            110 LET B$=SEG$(A$,2,3)
            """.trimIndent(),
        )

        session = session.step()
        session = session.step()

        assertEquals("ELL", session.stringVariables["B$"]?.text)
        assertEquals("03 E L L", session.stringVariables["B$"]?.internalDisplay)
    }

    fun `test SEG dollar initializes unknown string references as empty string`() {
        var session = startSession(
            """
            100 LET B$=SEG$(A$,1,3)
            110 PRINT B$
            """.trimIndent(),
        )

        session = session.step()

        assertEquals("", session.stringVariables["A$"]?.text)
        assertEquals("00", session.stringVariables["A$"]?.internalDisplay)
        assertEquals("", session.stringVariables["B$"]?.text)
        assertEquals("00", session.stringVariables["B$"]?.internalDisplay)
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test inspect evaluates current string expressions against debugger state`() {
        var session = startSession(
            """
            100 LET A$="HI"
            110 PRINT A$
            """.trimIndent(),
        )

        session = session.step()

        val inspectResult = inspectExpression(project, session, "A$&STR$(4711)")

        assertEquals("\"HI4711\" = 06 H I 4 7 1 1", inspectResult?.displayText)
    }

    fun `test inspect evaluates numeric expressions derived from debugger string state`() {
        var session = startSession(
            """
            100 LET A$="HELLO"
            110 PRINT A$
            """.trimIndent(),
        )

        session = session.step()

        val inspectResult = inspectExpression(project, session, "LEN(A$)+2")

        assertEquals("7", inspectResult?.displayText)
    }

    fun `test inspect evaluates numeric variables from debugger state`() {
        var session = startSession(
            """
            100 LET A=5
            110 PRINT A
            """.trimIndent(),
        )

        session = session.step()

        val inspectResult = inspectExpression(project, session, "A+1")

        assertEquals("6", inspectResult?.displayText)
    }

    fun `test CALL KEY shows bad value for rounded mode outside valid range`() {
        var session = startSession("100 CALL KEY(5.6,K,S)")

        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.PendingStop, session.status)
        assertEquals(TiBasicDebugMetadata.message(TiBasicDebugMetadata.badValueKey, "5.6"), session.statusMessage)
    }

    fun `test CALL KEY mode 4 accepts rounded scan codes up to 143`() {
        var session = startSession(
            """
            100 CALL KEY(4,K,S)
            110 PRINT K
            """.trimIndent(),
        ).copy(keyboardScanInput = "142.6")

        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.Paused, session.status)
        assertEquals("143", session.numericVariables["K"]?.usualDisplay)
        assertEquals("1", session.numericVariables["S"]?.usualDisplay)
        assertEquals(4, session.lastKeyboardMode)
    }

    fun `test CALL KEY mode zero defaults to mode five when no previous keyboard mode exists`() {
        var session = startSession(
            """
            100 CALL KEY(0,K,S)
            110 PRINT K
            """.trimIndent(),
        )

        assertEquals(5, session.keyboardRequest?.mode)
        assertEquals("-1", session.keyboardRequest?.scanInput)

        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.Paused, session.status)
        assertEquals("-1", session.numericVariables["K"]?.usualDisplay)
        assertEquals("0", session.numericVariables["S"]?.usualDisplay)
        assertEquals(5, session.lastKeyboardMode)
        assertEquals(110, session.currentProgramLine?.lineNumber)
        assertNull(session.keyboardRequest)
    }

    fun `test CALL KEY mode zero reuses last keyboard mode`() {
        var session = startSession(
            """
            100 CALL KEY(2,K,S)
            110 CALL KEY(0,K,S)
            120 PRINT K
            """.trimIndent(),
        ).copy(keyboardScanInput = "5")

        session = session.step()

        assertEquals(2, session.lastKeyboardMode)
        assertEquals(2, session.keyboardRequest?.mode)

        session = session.copy(keyboardScanInput = "19")
        session = session.step()

        assertEquals("19", session.numericVariables["K"]?.usualDisplay)
        assertEquals("1", session.numericVariables["S"]?.usualDisplay)
        assertEquals(2, session.lastKeyboardMode)
        assertEquals(120, session.currentProgramLine?.lineNumber)
    }

    fun `test CALL KEY rounds valid mode 2 scan input and sets key status to one`() {
        var session = startSession(
            """
            100 CALL KEY(2,K,S)
            110 PRINT K
            """.trimIndent(),
        ).copy(keyboardScanInput = "18.6")

        session = session.step()

        assertEquals("19", session.numericVariables["K"]?.usualDisplay)
        assertEquals("1", session.numericVariables["S"]?.usualDisplay)
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test CALL KEY mode 3 accepts rounded scan codes in its allowed range`() {
        var session = startSession(
            """
            100 CALL KEY(3,K,S)
            110 PRINT K
            """.trimIndent(),
        ).copy(keyboardScanInput = "94.6")

        session = session.step()

        assertEquals("95", session.numericVariables["K"]?.usualDisplay)
        assertEquals("1", session.numericVariables["S"]?.usualDisplay)
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test CALL KEY mode 5 accepts rounded scan code 187`() {
        var session = startSession(
            """
            100 CALL KEY(5,K,S)
            110 PRINT K
            """.trimIndent(),
        ).copy(keyboardScanInput = "186.6")

        session = session.step()

        assertEquals("187", session.numericVariables["K"]?.usualDisplay)
        assertEquals("1", session.numericVariables["S"]?.usualDisplay)
        assertEquals(5, session.lastKeyboardMode)
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test CALL KEY with invalid scan input stays on current line and shows bad value`() {
        var session = startSession("100 CALL KEY(1,K,S)").copy(keyboardScanInput = "20")

        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.Paused, session.status)
        assertEquals(TiBasicDebugMetadata.message(TiBasicDebugMetadata.badValueKey, 20), session.statusMessage)
        assertEquals(100, session.currentProgramLine?.lineNumber)
        assertNull(session.numericVariables["K"])
        assertNull(session.numericVariables["S"])
        assertEquals(1, session.keyboardRequest?.mode)
        assertEquals("20", session.keyboardRequest?.scanInput)
    }

    fun `test CALL KEY mode 3 rejects disallowed scan input 96`() {
        var session = startSession("100 CALL KEY(3,K,S)").copy(keyboardScanInput = "96")

        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.Paused, session.status)
        assertEquals(TiBasicDebugMetadata.message(TiBasicDebugMetadata.badValueKey, 96), session.statusMessage)
        assertEquals(100, session.currentProgramLine?.lineNumber)
        assertEquals(3, session.keyboardRequest?.mode)
    }

    fun `test CALL JOYST defaults to centered position`() {
        var session = startSession(
            """
            100 CALL JOYST(1,X,Y)
            110 PRINT X
            """.trimIndent(),
        )

        assertEquals(1, session.joystickRequest?.keyUnit)
        assertEquals(0, session.joystickRequest?.position?.x)
        assertEquals(0, session.joystickRequest?.position?.y)
        assertEquals("center", session.joystickRequest?.position?.compactDisplay)

        session = session.step()

        assertEquals("0", session.numericVariables["X"]?.usualDisplay)
        assertEquals("0", session.numericVariables["Y"]?.usualDisplay)
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test CALL JOYST applies selected joystick position`() {
        var session = startSession(
            """
            100 CALL JOYST(2,X,Y)
            110 PRINT X
            """.trimIndent(),
        ).copy(keyboardScanInput = "4,-4")

        session = session.step()

        assertEquals("4", session.numericVariables["X"]?.usualDisplay)
        assertEquals("-4", session.numericVariables["Y"]?.usualDisplay)
        assertEquals(110, session.currentProgramLine?.lineNumber)
        assertNull(session.joystickRequest)
    }

    private fun startSession(programText: String): TiBasicDebugSession {
        val file = configureFile(programText)
        val snapshot = TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document)
        return snapshot.initialSession()
    }

    private fun screenText(session: TiBasicDebugSession, row: Int, column: Int, length: Int): String =
        (column - 1 until column - 1 + length)
            .map { index -> session.screenContents.characterCodes[row - 1][index].toChar() }
            .joinToString(separator = "")
}
