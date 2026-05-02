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

    fun `test quick documentation shows preview for CALL CHAR pattern literal`() {
        val doc = quickDocumentation("100 CALL CHAR(96,\"FFFF<caret>FFFFFFFFFFFF\")")
        assertNotNull(doc)
        val documentation = doc!!
        assertTrue(documentation.contains("CALL CHAR hex pattern"))
        assertTrue(documentation.contains("FFFFFFFFFFFFFFFF"))
    }

    fun `test quick documentation shows preview for CALL CHAR constant pattern variable`() {
        val doc = quickDocumentation("100 LET P$=\"0F\"\n110 CALL CHAR(96,P<caret>$)")
        assertNotNull(doc)
        val documentation = doc!!
        assertTrue(documentation.contains("CALL CHAR hex pattern"))
        assertTrue(documentation.contains("0F00000000000000"))
    }

    fun `test quick documentation shows preview for uninitialized CALL CHAR pattern variable`() {
        val doc = quickDocumentation("100 CALL CHAR(96,P<caret>$)")
        assertNotNull(doc)
        val documentation = doc!!
        assertTrue(documentation.contains("CALL CHAR hex pattern"))
        assertTrue(documentation.contains("0000000000000000"))
    }

    fun `test quick documentation shows preview for DATA hex token`() {
        val doc = quickDocumentation("10 DATA 30,FFFF<caret>FFFFFFFFFFFF")
        assertNotNull(doc)
        val documentation = doc!!
        assertTrue(documentation.contains("DATA hex pattern"))
        assertTrue(documentation.contains("Pattern"))
        assertTrue(documentation.contains("FFFFFFFFFFFFFFFF"))
        assertTrue(documentation.contains("Normalized pattern"))
        assertTrue(documentation.contains("<table"))
    }

    fun `test quick documentation shows preview for quoted DATA hex string`() {
        val doc = quickDocumentation("10 DATA \"0<caret>F\"")
        assertNotNull(doc)
        val documentation = doc!!
        assertTrue(documentation.contains("DATA hex pattern"))
        assertTrue(documentation.contains("<code>0F</code>"))
        assertTrue(documentation.contains("0F00000000000000"))
    }

    fun `test quick documentation is unavailable for short numeric DATA item`() {
        assertNull(quickDocumentation("10 DATA 3<caret>0,FFFFFFFFFFFFFFFF"))
    }

    fun `test quick documentation shows preview for short leading zero numeric DATA item`() {
        val doc = quickDocumentation("10 DATA 01<caret>23")
        assertNotNull(doc)
        val documentation = doc!!
        assertTrue(documentation.contains("DATA hex pattern"))
        assertTrue(documentation.contains("0123000000000000"))
    }

    fun `test quick documentation shows preview for 9 digit numeric DATA item`() {
        val doc = quickDocumentation("10 DATA 12345678<caret>9")
        assertNotNull(doc)
        val documentation = doc!!
        assertTrue(documentation.contains("DATA hex pattern"))
        assertTrue(documentation.contains("1234567890000000"))
    }

    fun `test quick documentation shows preview for 16 digit numeric DATA item`() {
        val doc = quickDocumentation("10 DATA 12345678901234<caret>56")
        assertNotNull(doc)
        val documentation = doc!!
        assertTrue(documentation.contains("DATA hex pattern"))
        assertTrue(documentation.contains("1234567890123456"))
    }

    fun `test quick documentation is unavailable for non hex DATA item`() {
        assertNull(quickDocumentation("10 DATA ZZ<caret>ZZ"))
    }

    fun `test quick documentation shows preview for generic string literal`() {
        val doc = quickDocumentation("100 PRINT \"01<caret>23\"")
        assertNotNull(doc)
        val documentation = doc!!
        assertTrue(documentation.contains("String hex pattern"))
        assertTrue(documentation.contains("0123000000000000"))
    }

    fun `test quick documentation shows preview for 9 digit generic string literal`() {
        val doc = quickDocumentation("100 PRINT \"12345678<caret>9\"")
        assertNotNull(doc)
        val documentation = doc!!
        assertTrue(documentation.contains("String hex pattern"))
        assertTrue(documentation.contains("1234567890000000"))
    }

    fun `test quick documentation is unavailable for short generic decimal string`() {
        assertNull(quickDocumentation("100 PRINT \"3<caret>0\""))
    }

    fun `test normalize DATA hex pattern rejects short digit only item`() {
        assertNull(normalizeDataHexPattern("30"))
    }

    fun `test normalize DATA hex pattern rejects 8 digit item without leading zero`() {
        assertNull(normalizeDataHexPattern("12345678"))
    }

    fun `test normalize DATA hex pattern accepts short leading zero digit only item`() {
        assertEquals("0123000000000000", normalizeDataHexPattern("0123"))
    }

    fun `test normalize DATA hex pattern accepts 9 digit item`() {
        assertEquals("1234567890000000", normalizeDataHexPattern("123456789"))
    }

    fun `test normalize DATA hex pattern accepts shorter alphanumeric item`() {
        assertEquals("0F00000000000000", normalizeDataHexPattern("0F"))
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
