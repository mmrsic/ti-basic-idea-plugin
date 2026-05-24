package com.github.mmrsic.idea.plugins.tibasic.language.analysis.calls

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase

class TiBasicCharacterDefinitionCollectorTest : TiBasicTestBase() {

    fun `test collector keeps duplicate character definitions as separate rows`() {
        val file = configureFile(
            """
            100 LET CODE=65
            110 LET PAT$="0F"
            120 CALL CHAR(CODE,PAT$)
            130 CALL CHAR(65,"AA")
            140 CALL CHAR(97,"FF")
            """.trimIndent(),
        )

        val definitions = collectCallCharDefinitions(file)

        assertEquals(listOf(65, 65, 97), definitions.map { it.code })
        assertEquals(listOf("A", "A", "a"), definitions.map { it.ascii })
        assertEquals(
            listOf("0F00000000000000", "AA00000000000000", "FF00000000000000"),
            definitions.map { it.pattern },
        )
        assertEquals(listOf(120, 130, 140), definitions.map { it.lineNumber })
        assertEquals(
            listOf(120, 130, 140).map { lineNumber -> file.lineByNumber(lineNumber)?.textOffset },
            definitions.map { it.offset },
        )
    }

    fun `test collector omits non constant CALL CHAR code or pattern`() {
        val file = configureFile(
            """
            100 INPUT CODE
            110 INPUT PAT$
            120 CALL CHAR(CODE,"AA")
            130 CALL CHAR(67,PAT$)
            """.trimIndent(),
        )

        val definitions = collectCallCharDefinitions(file)

        assertTrue(definitions.isEmpty())
    }

    fun `test collector includes CALL CHAR definitions resolved from READ DATA`() {
        val file = configureFile(
            """
            100 READ CODE,PAT$
            110 DATA 65,FF
            120 CALL CHAR(CODE,PAT$)
            """.trimIndent(),
        )

        val definitions = collectCallCharDefinitions(file)

        assertEquals(1, definitions.size)
        assertEquals(65, definitions.single().code)
        assertEquals("A", definitions.single().ascii)
        assertEquals("FF00000000000000", definitions.single().pattern)
        assertEquals(120, definitions.single().lineNumber)
    }

    fun `test collector includes repeated READ DATA definitions with RESTORE`() {
        val file = configureFile(
            """
            100 DATA 65,FF,66,0F
            110 READ CODE,PAT$
            120 CALL CHAR(CODE,PAT$)
            130 READ CODE,PAT$
            140 CALL CHAR(CODE,PAT$)
            150 RESTORE 100
            160 READ CODE,PAT$
            170 CALL CHAR(CODE,PAT$)
            """.trimIndent(),
        )

        val definitions = collectCallCharDefinitions(file)

        assertEquals(listOf(65, 65, 66), definitions.map { it.code })
        assertEquals(
            listOf("FF00000000000000", "FF00000000000000", "0F00000000000000"),
            definitions.map { it.pattern },
        )
        assertEquals(listOf(120, 170, 140), definitions.map { it.lineNumber })
    }

    fun `test collector resets READ DATA source to first DATA line for RESTORE without line number`() {
        val file = configureFile(
            """
            100 READ CODE,PAT$
            110 CALL CHAR(CODE,PAT$)
            120 DATA 65,FF,66,0F
            130 READ CODE,PAT$
            140 CALL CHAR(CODE,PAT$)
            150 RESTORE
            160 READ CODE,PAT$
            170 CALL CHAR(CODE,PAT$)
            """.trimIndent(),
        )

        val definitions = collectCallCharDefinitions(file)

        assertEquals(listOf(65, 65, 66), definitions.map { it.code })
        assertEquals(
            listOf("FF00000000000000", "FF00000000000000", "0F00000000000000"),
            definitions.map { it.pattern },
        )
        assertEquals(listOf(110, 170, 140), definitions.map { it.lineNumber })
    }

    fun `test collector resolves RESTORE line to next higher DATA line`() {
        val file = configureFile(
            """
            100 DATA 65,FF
            110 PRINT "SKIP"
            120 DATA 66,0F
            130 RESTORE 115
            140 READ CODE,PAT$
            150 CALL CHAR(CODE,PAT$)
            """.trimIndent(),
        )

        val definitions = collectCallCharDefinitions(file)

        assertEquals(listOf(66), definitions.map { it.code })
        assertEquals(listOf("0F00000000000000"), definitions.map { it.pattern })
        assertEquals(listOf(150), definitions.map { it.lineNumber })
    }

    fun `test collector aborts only at READ after RESTORE points past last DATA line but not past program`() {
        val file = configureFile(
            """
            100 CALL CHAR(64,"AA")
            110 DATA 65,FF
            120 RESTORE 200
            130 CALL CHAR(66,"0F")
            300 REM NO DATA HERE
            310 READ CODE,PAT$
            320 CALL CHAR(CODE,PAT$)
            """.trimIndent(),
        )

        val definitions = collectCallCharDefinitions(file)

        assertEquals(listOf(64, 66), definitions.map { it.code })
        assertEquals(listOf("AA00000000000000", "0F00000000000000"), definitions.map { it.pattern })
        assertEquals(listOf(100, 130), definitions.map { it.lineNumber })
    }

    fun `test collector aborts immediately when RESTORE line is greater than highest program line`() {
        val file = configureFile(
            """
            100 CALL CHAR(64,"AA")
            110 RESTORE 999
            120 CALL CHAR(65,"FF")
            """.trimIndent(),
        )

        val definitions = collectCallCharDefinitions(file)

        assertEquals(listOf(64), definitions.map { it.code })
        assertEquals(listOf("AA00000000000000"), definitions.map { it.pattern })
        assertEquals(listOf(100), definitions.map { it.lineNumber })
    }

    fun `test collector resets READ DATA source to targeted RESTORE line`() {
        val file = configureFile(
            """
            180 DATA AAAAAAAAAAAAAAAA,BBBBBBBBBBBBBBBB,CCCCCCCCCCCCCCCC
            190 DATA DDDDDDDDDDDDDDDD,EEEEEEEEEEEEEEEE,FFFFFFFFFFFFFFFF
            470 DATA 1111111111111111,2222222222222222,3333333333333333
            480 DATA 4444444444444444,5555555555555555,6666666666666666
            210 RESTORE 180
            220 FOR I=97 TO 99
            230 READ A$
            240 CALL CHAR(I,A$)
            250 NEXT I
            490 RESTORE 470
            500 FOR I=48 TO 50
            510 READ A$
            520 CALL CHAR(I,A$)
            530 NEXT I
            """.trimIndent(),
        )

        val definitions = collectCallCharDefinitions(file)

        assertEquals(listOf(48, 49, 50, 97, 98, 99), definitions.map { it.code })
        assertEquals(
            listOf(
                "1111111111111111",
                "2222222222222222",
                "3333333333333333",
                "AAAAAAAAAAAAAAAA",
                "BBBBBBBBBBBBBBBB",
                "CCCCCCCCCCCCCCCC",
            ),
            definitions.map { it.pattern },
        )
    }

    fun `test collector repeats READ DATA character definitions across FOR NEXT iterations`() {
        val file = configureFile(
            """
            100 DATA 65,FF,66,0F,67,F0,68,AA,69,55,70,33,71,CC
            1040 FOR I=1 TO 7
            1050 READ A,A$
            1060 CALL CHAR(A,A$)
            1070 NEXT I
            """.trimIndent(),
        )

        val definitions = collectCallCharDefinitions(file)

        assertEquals(listOf(65, 66, 67, 68, 69, 70, 71), definitions.map { it.code })
        assertEquals(
            listOf(
                "FF00000000000000",
                "0F00000000000000",
                "F000000000000000",
                "AA00000000000000",
                "5500000000000000",
                "3300000000000000",
                "CC00000000000000",
            ),
            definitions.map { it.pattern },
        )
        assertEquals(List(7) { 1060 }, definitions.map { it.lineNumber })
    }

    fun `test collector supports provided FOR NEXT READ CALL CHAR example`() {
        val file = configureFile(
            """
            100 DATA 65,FF,66,0F,67,F0,68,AA,69,55,70,33,71,CC
            1040 FOR I=1 TO 7
            1050 READ A,A$
            1060 CALL CHAR(A,A$)
            1070 NEXT I
            """.trimIndent(),
        )

        val definitions = collectCallCharDefinitions(file)

        assertEquals(7, definitions.size)
        assertEquals(listOf(65, 66, 67, 68, 69, 70, 71), definitions.map { it.code })
        assertEquals(List(7) { 1060 }, definitions.map { it.lineNumber })
    }

    fun `test collector follows IF THEN jumps inside FOR NEXT READ CALL CHAR flow`() {
        val file = configureFile(
            """
            170 FOR I=97 TO 102
            180 IF I<99 THEN 220
            190 IF I<101 THEN 240
            200 IF I<102 THEN 220
            210 IF I<103 THEN 240
            220 READ A$
            230 CALL CHAR(I,A$)
            240 NEXT I
            250 DATA "AAAAAAAAAAAAAAAA","BBBBBBBBBBBBBBBB","CCCCCCCCCCCCCCCC"
            """.trimIndent(),
        )

        val definitions = collectCallCharDefinitions(file)

        assertEquals(listOf(97, 98, 101), definitions.map { it.code })
        assertEquals(
            listOf(
                "AAAAAAAAAAAAAAAA",
                "BBBBBBBBBBBBBBBB",
                "CCCCCCCCCCCCCCCC",
            ),
            definitions.map { it.pattern },
        )
        assertEquals(List(3) { 230 }, definitions.map { it.lineNumber })
    }

    fun `test collector resolves arithmetic CALL CHAR codes inside FOR loops`() {
        val file = configureFile(
            """
            1320 VOLL$="FFFFFFFFFFFFFFFF"
            1330 LEER$="0"
            1340 HAVO$="3C3C3C3C3C3C3C3C"
            1350 HARE$="FF7F3F1F0F070301"
            1360 DIM ZL$(4)
            1370 ZL$(1)="180C06FFFF060C1"
            1380 ZL$(2)="183C7EDB99181818"
            1390 ZL$(3)="183060FFFF603018"
            1400 ZL$(4)="18181899DB7E3C18"
            1410 HALI$="FFFEFCF8F0E0C080"
            1420 FOR A=1 TO 7
            1430 CALL CHAR(A*8+25,VOLL$)
            1440 CALL CHAR(A*8+26,LEER$)
            1450 CALL CHAR(A*8+27,HALI$)
            1460 NEXT A
            1470 FOR A=8 TO 14
            1480 CALL CHAR(A*8+25,VOLL$)
            1490 CALL CHAR(A*8+26,LEER$)
            1500 CALL CHAR(A*8+27,HARE$)
            1510 NEXT A
            1520 CALL CHAR(144,VOLL$)
            1530 CALL CHAR(152,VOLL$)
            1540 CALL CHAR(153,LEER$)
            """.trimIndent(),
        )

        val definitions = collectCallCharDefinitions(file)
        val expectedDefinitions = buildList {
            for (a in 1..7) {
                add(ExpectedCharDefinition(a * 8 + 25, "FFFFFFFFFFFFFFFF", 1430))
                add(ExpectedCharDefinition(a * 8 + 26, "0000000000000000", 1440))
                add(ExpectedCharDefinition(a * 8 + 27, "FFFEFCF8F0E0C080", 1450))
            }
            for (a in 8..14) {
                add(ExpectedCharDefinition(a * 8 + 25, "FFFFFFFFFFFFFFFF", 1480))
                add(ExpectedCharDefinition(a * 8 + 26, "0000000000000000", 1490))
                add(ExpectedCharDefinition(a * 8 + 27, "FF7F3F1F0F070301", 1500))
            }
            add(ExpectedCharDefinition(144, "FFFFFFFFFFFFFFFF", 1520))
            add(ExpectedCharDefinition(152, "FFFFFFFFFFFFFFFF", 1530))
            add(ExpectedCharDefinition(153, "0000000000000000", 1540))
        }.sortedBy(ExpectedCharDefinition::code)

        assertEquals(expectedDefinitions.map(ExpectedCharDefinition::code), definitions.map { it.code })
        assertEquals(expectedDefinitions.map(ExpectedCharDefinition::pattern), definitions.map { it.pattern })
        assertEquals(expectedDefinitions.map(ExpectedCharDefinition::lineNumber), definitions.map { it.lineNumber })
    }

    fun `test collector resolves CALL CHAR pattern through constant string array elements inside FOR loop`() {
        val file = configureFile(
            """
            100 DATA FF,0F
            110 FOR I=65 TO 66
            120 READ S$
            130 F$(I)=S$
            140 CALL CHAR(I,F$(I))
            150 NEXT I
            """.trimIndent(),
        )

        val definitions = collectCallCharDefinitions(file)

        assertEquals(listOf(65, 66), definitions.map { it.code })
        assertEquals(listOf("FF00000000000000", "0F00000000000000"), definitions.map { it.pattern })
        assertEquals(List(2) { 140 }, definitions.map { it.lineNumber })
    }

    fun `test collector omits arithmetic CALL CHAR code with array access`() {
        val file = configureFile(
            """
            100 DIM Z(1)
            110 CALL CHAR(Z(1)+8,"FF")
            """.trimIndent(),
        )

        val definitions = collectCallCharDefinitions(file)

        assertTrue(definitions.isEmpty())
    }

    private data class ExpectedCharDefinition(
        val code: Int,
        val pattern: String,
        val lineNumber: Int,
    )
}
