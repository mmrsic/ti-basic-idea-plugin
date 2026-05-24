package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase
import com.github.mmrsic.idea.plugins.tibasic.language.analysis.references.TiBasicInboundLineReference
import com.github.mmrsic.idea.plugins.tibasic.language.analysis.references.TiBasicInboundLineReferenceCollector
import com.github.mmrsic.idea.plugins.tibasic.language.analysis.references.referencedByTooltip

class TiBasicLineReferenceLineMarkerTest : TiBasicTestBase() {

    fun `test gutter icon appears on referenced line`() {
        configureFile("100 GOTO 200\n200 PRINT \"OK\"")
        val gutters = myFixture.findAllGutters()
        assertEquals("Exactly one gutter icon must appear for a referenced line", 1, gutters.size)
    }

    fun `test gutter icon does not appear on unreferenced lines`() {
        configureFile("100 PRINT \"A\"\n200 PRINT \"B\"")
        val gutters = myFixture.findAllGutters()
        assertTrue("No gutter icon must appear when no line is referenced", gutters.isEmpty())
    }

    fun `test gutter icon tooltip lists referring line numbers`() {
        configureFile("100 GOTO 300\n200 GOSUB 300\n300 PRINT \"OK\"\n400 RETURN")
        val gutters = myFixture.findAllGutters()
        assertEquals(1, gutters.size)
        assertEquals("Referenced by lines 100, 200", gutters.single().tooltipText)
    }

    fun `test self reference alone does not produce gutter icon`() {
        configureFile("100 GOTO 100")
        val gutters = myFixture.findAllGutters()
        assertTrue("A line must not show the gutter icon when only it references itself", gutters.isEmpty())
    }
}

class TiBasicInboundLineReferenceCollectorTest : TiBasicTestBase() {

    fun `test collector maps inbound references by target line number`() {
        val file = configureFile("100 GOTO 300\n200 GOSUB 300\n300 PRINT \"OK\"\n400 RETURN")
        val inboundReferences = TiBasicInboundLineReferenceCollector.collect(file)
        assertEquals(listOf(100, 200), inboundReferences[300]?.map { it.sourceLineNumber })
    }

    fun `test collector ignores invalid target line numbers`() {
        val file = configureFile("100 GOTO 0\n200 GOTO 32768\n300 PRINT \"OK\"")
        val inboundReferences = TiBasicInboundLineReferenceCollector.collect(file)
        assertTrue(inboundReferences.isEmpty())
    }

    fun `test tooltip summary shortens long referring line lists`() {
        val file = configureFile(
            "100 GOTO 700\n" +
                    "110 GOTO 700\n" +
                    "120 GOTO 700\n" +
                    "130 GOTO 700\n" +
                    "140 GOTO 700\n" +
                    "150 GOTO 700\n" +
                    "700 PRINT \"OK\"",
        )
        val tooltip = referencedByTooltip(
            file.lines()
                .filter { it.lineNumber() != 700 }
                .map { TiBasicInboundLineReference(it, it.textOffset) },
        )
        assertEquals("Referenced by 6 lines: 100, 110, 120, 130, 140, ...", tooltip)
    }
}
