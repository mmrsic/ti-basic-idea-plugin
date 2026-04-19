package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase

class TiBasicCharacterCodeDocumentationTest : TiBasicTestBase() {

    private val provider = TiBasicCharacterCodeDocumentationProvider()

    fun `test character group boundaries`() {
        assertEquals(1, tiBasicCharacterGroup(32))
        assertEquals(16, tiBasicCharacterGroup(159))
        assertNull(tiBasicCharacterGroup(31))
        assertNull(tiBasicCharacterGroup(160))
    }

    fun `test quick documentation shows ascii and group for CALL HCHAR literal`() {
        val doc = quickDocumentation("100 CALL HCHAR(1,1,6<caret>5)")
        assertNotNull(doc)
        val documentation = doc!!
        assertTrue(documentation.contains("Character code 65"))
        assertTrue(documentation.contains("CALL HCHAR character code"))
        assertTrue(documentation.contains("ASCII"))
        assertTrue(documentation.contains("A"))
        assertTrue(documentation.contains("TI-Basic character group"))
        assertTrue(documentation.contains("5"))
    }

    fun `test quick documentation shows overrides for CHR dollar constant variable`() {
        val doc = quickDocumentation(
            """
            100 CALL CHAR(96,"FFFFFFFFFFFFFFFF")
            110 CALL CHAR(96,"0F")
            120 LET CODE=96
            130 PRINT CHR$(CO<caret>DE)
            """.trimIndent(),
        )
        assertNotNull(doc)
        val documentation = doc!!
        assertTrue(documentation.contains("Character code 96"))
        assertTrue(documentation.contains("CHR$ character code"))
        assertTrue(documentation.contains("Line 100"))
        assertTrue(documentation.contains("FFFFFFFFFFFFFFFF"))
        assertTrue(documentation.contains("Line 110"))
        assertTrue(documentation.contains("0F00000000000000"))
    }

    fun `test quick documentation reports unresolved composite expression`() {
        val doc = quickDocumentation("100 LET I=1\n110 CALL VCHAR(1,1,3<caret>2+I)")
        assertNotNull(doc)
        val documentation = doc!!
        assertTrue(documentation.contains("CALL VCHAR character code"))
        assertTrue(documentation.contains("not statically determinable"))
    }

    fun `test quick documentation is unavailable for CALL CHAR pattern argument`() {
        assertNull(quickDocumentation("100 CALL CHAR(96,P<caret>$)"))
    }

    fun `test collect call char overrides ignores unresolvable entries`() {
        val file = configureFile(
            """
            100 CALL CHAR(96,"FF")
            110 LET C=96
            120 CALL CHAR(C,"0F")
            130 LET P$="AA"
            140 CALL CHAR(96,P$)
            150 CALL CHAR(95+1,"AA")
            """.trimIndent(),
        )
        val overrides = collectCallCharOverrides(file)
        assertEquals(
            listOf(
                TiBasicCharacterCodeOverride(100, "FF00000000000000"),
                TiBasicCharacterCodeOverride(120, "0F00000000000000"),
                TiBasicCharacterCodeOverride(140, "AA00000000000000"),
            ),
            overrides[96],
        )
        assertEquals(1, overrides.size)
    }

    private fun quickDocumentation(text: String): String? {
        configureFile(text)
        val file = myFixture.file
        val offset = myFixture.caretOffset
        val contextElement = file.findElementAt(offset) ?: file.findElementAt(offset - 1)
        val target = provider.getCustomDocumentationElement(myFixture.editor, file, contextElement, offset)
        return provider.generateDoc(target, contextElement)
    }
}
