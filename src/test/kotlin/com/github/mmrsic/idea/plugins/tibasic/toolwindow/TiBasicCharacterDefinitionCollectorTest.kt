package com.github.mmrsic.idea.plugins.tibasic.toolwindow

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase
import com.github.mmrsic.idea.plugins.tibasic.editor.collectCallCharDefinitions

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
            100 LET CODE=65
            110 LET CODE=66
            120 LET PAT$="FF"
            130 LET PAT$="0F"
            140 CALL CHAR(CODE,"AA")
            150 CALL CHAR(67,PAT$)
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
