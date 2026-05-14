package com.github.mmrsic.idea.plugins.tibasic.toolwindow

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase

class TiBasicVariableCollectorTest : TiBasicTestBase() {

    fun `test numeric variable in LET is write`() {
        val file = configureFile("100 LET A=5")
        val entries = TiBasicVariableCollector.collect(file)
        val entry = entries.single { it.name == "A" && it.type == TiBasicVariableType.NUMERIC }
        assertEquals(1, entry.writes)
        assertEquals(0, entry.reads)
    }

    fun `test numeric variable on right side of LET is read`() {
        val file = configureFile("100 LET A=5\n200 LET B=A")
        val entries = TiBasicVariableCollector.collect(file)
        val entryA = entries.single { it.name == "A" && it.type == TiBasicVariableType.NUMERIC }
        assertEquals(1, entryA.writes)
        assertEquals(1, entryA.reads)
    }

    fun `test string variable in LET is write`() {
        val file = configureFile("100 LET A$=\"HELLO\"")
        val entries = TiBasicVariableCollector.collect(file)
        val entry = entries.single { it.name == "A$" && it.type == TiBasicVariableType.STRING }
        assertEquals(1, entry.writes)
        assertEquals(0, entry.reads)
    }

    fun `test FOR loop variable is write`() {
        val file = configureFile("100 FOR I=1 TO 10\n200 NEXT I")
        val entries = TiBasicVariableCollector.collect(file)
        val entry = entries.single { it.name == "I" && it.type == TiBasicVariableType.NUMERIC }
        assertEquals(1, entry.writes)
    }

    fun `test NEXT variable without FOR is read`() {
        val file = configureFile("100 NEXT I")
        val entries = TiBasicVariableCollector.collect(file)
        val entry = entries.single { it.name == "I" && it.type == TiBasicVariableType.NUMERIC }
        assertEquals(0, entry.writes)
        assertEquals(1, entry.reads)
    }

    fun `test NEXT variable is read`() {
        val file = configureFile("100 FOR I=1 TO 10\n200 NEXT I")
        val entries = TiBasicVariableCollector.collect(file)
        val entry = entries.single { it.name == "I" && it.type == TiBasicVariableType.NUMERIC }
        assertEquals(1, entry.writes)
        assertEquals(1, entry.reads)
    }

    fun `test INPUT variable is write`() {
        val file = configureFile("100 INPUT A")
        val entries = TiBasicVariableCollector.collect(file)
        val entry = entries.single { it.name == "A" && it.type == TiBasicVariableType.NUMERIC }
        assertEquals(1, entry.writes)
        assertEquals(0, entry.reads)
    }

    fun `test INPUT multiple variables are writes`() {
        val file = configureFile("100 INPUT A,B,C")
        val entries = TiBasicVariableCollector.collect(file)
        listOf("A", "B", "C").forEach { name ->
            val entry = entries.single { it.name == name && it.type == TiBasicVariableType.NUMERIC }
            assertEquals("$name should be WRITE", 1, entry.writes)
        }
    }

    fun `test READ variable is write`() {
        val file = configureFile("100 READ A")
        val entries = TiBasicVariableCollector.collect(file)
        val entry = entries.single { it.name == "A" && it.type == TiBasicVariableType.NUMERIC }
        assertEquals(1, entry.writes)
        assertEquals(0, entry.reads)
    }

    fun `test CALL GCHAR third argument is write`() {
        val file = configureFile("100 CALL GCHAR(1,1,C)")
        val entries = TiBasicVariableCollector.collect(file)
        val entry = entries.single { it.name == "C" && it.type == TiBasicVariableType.NUMERIC }
        assertEquals(1, entry.writes)
        assertEquals(0, entry.reads)
    }

    fun `test CALL GCHAR first and second arguments are reads`() {
        val file = configureFile("100 CALL GCHAR(R,C,V)")
        val entries = TiBasicVariableCollector.collect(file)
        val entryR = entries.single { it.name == "R" && it.type == TiBasicVariableType.NUMERIC }
        val entryC = entries.single { it.name == "C" && it.type == TiBasicVariableType.NUMERIC }
        assertEquals(1, entryR.reads)
        assertEquals(0, entryR.writes)
        assertEquals(1, entryC.reads)
        assertEquals(0, entryC.writes)
    }

    fun `test CALL KEY second and third arguments are writes`() {
        val file = configureFile("100 CALL KEY(0,K,S)")
        val entries = TiBasicVariableCollector.collect(file)
        val entryK = entries.single { it.name == "K" && it.type == TiBasicVariableType.NUMERIC }
        val entryS = entries.single { it.name == "S" && it.type == TiBasicVariableType.NUMERIC }
        assertEquals(1, entryK.writes)
        assertEquals(1, entryS.writes)
    }

    fun `test CALL JOYST second and third arguments are writes`() {
        val file = configureFile("100 CALL JOYST(1,X,Y)")
        val entries = TiBasicVariableCollector.collect(file)
        val entryX = entries.single { it.name == "X" && it.type == TiBasicVariableType.NUMERIC }
        val entryY = entries.single { it.name == "Y" && it.type == TiBasicVariableType.NUMERIC }
        assertEquals(1, entryX.writes)
        assertEquals(1, entryY.writes)
    }

    fun `test DIM creates numeric array entry with metadata and DIM line`() {
        val file = configureFile("100 DIM A(10)")
        val entries = TiBasicVariableCollector.collect(file)
        val entry = entries.single { it.name == "A" && it.type == TiBasicVariableType.NUMERIC_ARRAY }
        assertEquals(0, entry.reads)
        assertEquals(0, entry.writes)
        assertEquals("10", entry.dimensions)
        assertEquals("0", entry.optionBase)
        assertEquals("100", entry.dimLine)
        assertEquals(listOf(100), entry.dimOccurrences.map { it.lineNumber })
    }

    fun `test DIM and usage share one array entry`() {
        val file = configureFile("100 DIM A(10)\n200 LET A(1)=5")
        val entries = TiBasicVariableCollector.collect(file)
        val entry = entries.single { it.name == "A" && it.type == TiBasicVariableType.NUMERIC_ARRAY }
        assertEquals("10", entry.dimensions)
        assertEquals("0", entry.optionBase)
        assertEquals("100", entry.dimLine)
        assertEquals(listOf(100), entry.dimOccurrences.map { it.lineNumber })
        assertEquals(1, entry.writes)
    }

    fun `test DEF creates USER_FUNCTION entry without access counts`() {
        val file = configureFile("100 DEF F(X)=X*2")
        val entries = TiBasicVariableCollector.collect(file)
        val defEntry = entries.single { it.name == "F" && it.type == TiBasicVariableType.USER_FUNCTION }
        assertEquals(100, defEntry.occurrences.single().lineNumber)
        assertEquals(0, defEntry.reads)
        assertEquals(0, defEntry.writes)
    }

    fun `test numeric array variable has NUMERIC_ARRAY type`() {
        val file = configureFile("100 LET A(1)=5")
        val entries = TiBasicVariableCollector.collect(file)
        val entry = entries.single { it.name == "A" && it.type == TiBasicVariableType.NUMERIC_ARRAY }
        assertEquals(1, entry.writes)
    }

    fun `test string array variable has STRING_ARRAY type`() {
        val file = configureFile("100 LET A$(1)=\"HI\"")
        val entries = TiBasicVariableCollector.collect(file)
        val entry = entries.single { it.name == "A$" && it.type == TiBasicVariableType.STRING_ARRAY }
        assertEquals(1, entry.writes)
        assertEquals("10", entry.dimensions)
        assertEquals("0", entry.optionBase)
    }

    fun `test explicit array dimension and OPTION BASE are shown on array entries`() {
        val file = configureFile("100 OPTION BASE 1\n200 DIM A(10,10,10)\n300 LET A(1,1,1)=5")
        val entries = TiBasicVariableCollector.collect(file)
        val entry = entries.single { it.name == "A" && it.type == TiBasicVariableType.NUMERIC_ARRAY }
        assertEquals("10,10,10", entry.dimensions)
        assertEquals("1", entry.optionBase)
        assertEquals("200", entry.dimLine)
        assertEquals(listOf(200), entry.dimOccurrences.map { it.lineNumber })
    }

    fun `test DIM creates string array entry without usage`() {
        val file = configureFile("100 DIM A$(10)")
        val entries = TiBasicVariableCollector.collect(file)
        val entry = entries.single { it.name == "A$" && it.type == TiBasicVariableType.STRING_ARRAY }
        assertEquals("10", entry.dimensions)
        assertEquals("0", entry.optionBase)
        assertEquals("100", entry.dimLine)
        assertTrue(entry.occurrences.isEmpty())
    }

    fun `test implicit multidimensional array uses default dimensions and base`() {
        val file = configureFile("100 LET A(1,2,3)=5")
        val entries = TiBasicVariableCollector.collect(file)
        val entry = entries.single { it.name == "A" && it.type == TiBasicVariableType.NUMERIC_ARRAY }
        assertEquals("10,10,10", entry.dimensions)
        assertEquals("0", entry.optionBase)
    }

    fun `test multiple occurrences on different lines are all recorded`() {
        val file = configureFile("100 LET A=1\n200 LET A=2\n300 PRINT A")
        val entries = TiBasicVariableCollector.collect(file)
        val entry = entries.single { it.name == "A" && it.type == TiBasicVariableType.NUMERIC }
        assertEquals(listOf(100, 200, 300), entry.lineNumbers)
        assertEquals(2, entry.writes)
        assertEquals(1, entry.reads)
    }

    fun `test PRINT variable is read`() {
        val file = configureFile("100 PRINT A")
        val entries = TiBasicVariableCollector.collect(file)
        val entry = entries.single { it.name == "A" && it.type == TiBasicVariableType.NUMERIC }
        assertEquals(0, entry.writes)
        assertEquals(1, entry.reads)
    }

    fun `test numeric variable never written has constValue 0`() {
        val file = configureFile("100 PRINT A")
        val entries = TiBasicVariableCollector.collect(file)
        val entry = entries.single { it.name == "A" && it.type == TiBasicVariableType.NUMERIC }
        assertEquals("0", entry.constValue)
    }

    fun `test string variable never written has constValue empty string literal`() {
        val file = configureFile("100 PRINT A$")
        val entries = TiBasicVariableCollector.collect(file)
        val entry = entries.single { it.name == "A$" && it.type == TiBasicVariableType.STRING }
        assertEquals("\"\"", entry.constValue)
    }

    fun `test numeric variable written with single literal has that constValue`() {
        val file = configureFile("100 LET A=42")
        val entries = TiBasicVariableCollector.collect(file)
        val entry = entries.single { it.name == "A" && it.type == TiBasicVariableType.NUMERIC }
        assertEquals("42", entry.constValue)
    }

    fun `test string variable written with single literal has that constValue`() {
        val file = configureFile("100 LET A$=\"HELLO\"")
        val entries = TiBasicVariableCollector.collect(file)
        val entry = entries.single { it.name == "A$" && it.type == TiBasicVariableType.STRING }
        assertEquals("\"HELLO\"", entry.constValue)
    }

    fun `test numeric variable written with same literal twice has that constValue`() {
        val file = configureFile("100 LET A=5\n200 LET A=5")
        val entries = TiBasicVariableCollector.collect(file)
        val entry = entries.single { it.name == "A" && it.type == TiBasicVariableType.NUMERIC }
        assertEquals("5", entry.constValue)
    }

    fun `test numeric variable written with different literals has null constValue`() {
        val file = configureFile("100 LET A=5\n200 LET A=10")
        val entries = TiBasicVariableCollector.collect(file)
        val entry = entries.single { it.name == "A" && it.type == TiBasicVariableType.NUMERIC }
        assertNull(entry.constValue)
    }

    fun `test numeric variable written via expression has null constValue`() {
        val file = configureFile("100 LET A=5*2")
        val entries = TiBasicVariableCollector.collect(file)
        val entry = entries.single { it.name == "A" && it.type == TiBasicVariableType.NUMERIC }
        assertNull(entry.constValue)
    }

    fun `test numeric variable written via INPUT has null constValue`() {
        val file = configureFile("100 INPUT A")
        val entries = TiBasicVariableCollector.collect(file)
        val entry = entries.single { it.name == "A" && it.type == TiBasicVariableType.NUMERIC }
        assertNull(entry.constValue)
    }

    fun `test FOR loop variable has null constValue`() {
        val file = configureFile("100 FOR I=1 TO 10\n200 NEXT I")
        val entries = TiBasicVariableCollector.collect(file)
        val entry = entries.single { it.name == "I" && it.type == TiBasicVariableType.NUMERIC }
        assertNull(entry.constValue)
    }

    fun `test numeric array variable has null constValue`() {
        val file = configureFile("100 LET A(1)=5")
        val entries = TiBasicVariableCollector.collect(file)
        val entry = entries.single { it.name == "A" && it.type == TiBasicVariableType.NUMERIC_ARRAY }
        assertNull(entry.constValue)
    }

    fun `test DIM-only array has null constValue`() {
        val file = configureFile("100 DIM A(10)")
        val entries = TiBasicVariableCollector.collect(file)
        val entry = entries.single { it.name == "A" && it.type == TiBasicVariableType.NUMERIC_ARRAY }
        assertNull(entry.constValue)
    }

    fun `test DEF user function has null constValue`() {
        val file = configureFile("100 DEF F(X)=X*2")
        val entries = TiBasicVariableCollector.collect(file)
        val entry = entries.single { it.name == "F" && it.type == TiBasicVariableType.USER_FUNCTION }
        assertNull(entry.constValue)
    }
}
