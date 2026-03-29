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

    fun `test NEXT variable is ignored for access counting`() {
        val file = configureFile("100 FOR I=1 TO 10\n200 NEXT I")
        val entries = TiBasicVariableCollector.collect(file)
        val entry = entries.single { it.name == "I" && it.type == TiBasicVariableType.NUMERIC }
        assertEquals(1, entry.writes + entry.reads)
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

    fun `test DIM creates separate DIM_DECLARATION entry`() {
        val file = configureFile("100 DIM A(10)")
        val entries = TiBasicVariableCollector.collect(file)
        val dimEntry = entries.single { it.name == "A" && it.type == TiBasicVariableType.DIM_DECLARATION }
        assertEquals(100, dimEntry.occurrences.single().lineNumber)
        assertEquals(0, dimEntry.reads)
        assertEquals(0, dimEntry.writes)
    }

    fun `test DIM and usage create two separate entries`() {
        val file = configureFile("100 DIM A(10)\n200 LET A(1)=5")
        val entries = TiBasicVariableCollector.collect(file)
        val dimEntry = entries.single { it.type == TiBasicVariableType.DIM_DECLARATION }
        val usageEntry = entries.single { it.type == TiBasicVariableType.NUMERIC_ARRAY }
        assertEquals("A", dimEntry.name)
        assertEquals("A", usageEntry.name)
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
        val entry = entries.single { it.name == "A\$" && it.type == TiBasicVariableType.STRING_ARRAY }
        assertEquals(1, entry.writes)
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
}
