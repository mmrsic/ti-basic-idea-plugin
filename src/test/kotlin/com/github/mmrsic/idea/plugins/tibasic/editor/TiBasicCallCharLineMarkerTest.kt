package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase
import java.awt.image.BufferedImage

class TiBasicCallCharLineMarkerTest : TiBasicTestBase() {

    fun `test gutter icon appears for valid CALL CHAR`() {
        configureFile("100 CALL CHAR(96,\"FFFFFFFFFFFFFFFF\")")
        val gutters = myFixture.findAllGutters()
        assertEquals("Exactly one gutter icon must appear for CALL CHAR", 1, gutters.size)
    }

    fun `test gutter icon appears for CALL CHAR with lowercase hex pattern`() {
        configureFile("100 CALL CHAR(96,\"ffffffffffffffff\")")
        val gutters = myFixture.findAllGutters()
        assertEquals("Gutter icon must appear for lowercase hex pattern", 1, gutters.size)
    }

    fun `test gutter icon appears for CALL CHAR with mixed case hex pattern`() {
        configureFile("170 CALL CHAR(96,\"00001C3E7F7F7F7F\")")
        val gutters = myFixture.findAllGutters()
        assertEquals("Gutter icon must appear for mixed-case hex pattern", 1, gutters.size)
    }

    fun `test gutter icon appears for CALL CHAR with empty pattern`() {
        configureFile("100 CALL CHAR(96,\"\")")
        val gutters = myFixture.findAllGutters()
        assertEquals("Gutter icon must appear for empty pattern (treated as all-zero like \"0\")", 1, gutters.size)
    }

    fun `test gutter icon does not appear for CALL CHAR with pattern longer than 16 chars`() {
        configureFile("100 CALL CHAR(96,\"FFFFFFFFFFFFFFFFFF\")")
        val gutters = myFixture.findAllGutters()
        assertTrue("No gutter icon must appear for pattern longer than 16 characters", gutters.isEmpty())
    }

    fun `test gutter icon appears for CALL CHAR with 2-char pattern`() {
        configureFile("100 CALL CHAR(96,\"FF\")")
        val gutters = myFixture.findAllGutters()
        assertEquals("Gutter icon must appear for 2-character hex pattern", 1, gutters.size)
    }

    fun `test gutter icon appears for CALL CHAR with 10-char pattern`() {
        configureFile("100 CALL CHAR(96,\"00001C3E7F\")")
        val gutters = myFixture.findAllGutters()
        assertEquals("Gutter icon must appear for 10-character hex pattern", 1, gutters.size)
    }

    fun `test gutter icon appears for CALL CHAR with 1-char pattern`() {
        configureFile("100 CALL CHAR(96,\"F\")")
        val gutters = myFixture.findAllGutters()
        assertEquals("Gutter icon must appear for 1-character hex pattern", 1, gutters.size)
    }

    fun `test gutter icon appears for CALL CHAR with 14-char pattern`() {
        configureFile("100 CALL CHAR(96,\"FFFFFFFFFFFFFF\")")
        val gutters = myFixture.findAllGutters()
        assertEquals("Gutter icon must appear for 14-character hex pattern (zero-padded to 16)", 1, gutters.size)
    }

    fun `test gutter icon does not appear for CALL CHAR with non-hex pattern`() {
        configureFile("100 CALL CHAR(96,\"ZZZZZZZZZZZZZZZZ\")")
        val gutters = myFixture.findAllGutters()
        assertTrue("No gutter icon must appear for a non-hex pattern", gutters.isEmpty())
    }

    fun `test gutter icon does not appear for CALL CHAR with variable instead of literal`() {
        configureFile("100 CALL CHAR(96,PAT\$)")
        val gutters = myFixture.findAllGutters()
        assertTrue("No gutter icon must appear when pattern is a variable", gutters.isEmpty())
    }

    fun `test gutter icon does not appear for CALL SCREEN`() {
        configureFile("100 CALL SCREEN(2)")
        val gutters = myFixture.findAllGutters()
        assertTrue("No gutter icon must appear for CALL SCREEN", gutters.isEmpty())
    }

    fun `test gutter icon does not appear for CALL CLEAR`() {
        configureFile("100 CALL CLEAR")
        val gutters = myFixture.findAllGutters()
        assertTrue("No gutter icon must appear for CALL CLEAR", gutters.isEmpty())
    }

    fun `test gutter icon tooltip contains pattern`() {
        configureFile("100 CALL CHAR(96,\"FFFFFFFFFFFFFFFF\")")
        val gutters = myFixture.findAllGutters()
        assertEquals(1, gutters.size)
        assertTrue(
            "Gutter tooltip must contain the hex pattern",
            gutters[0].tooltipText?.contains("FFFFFFFFFFFFFFFF") == true,
        )
    }
}

class TiBasicCharPatternIconTest : TiBasicTestBase() {

    fun `test icon has correct width`() {
        val icon = TiBasicCharPatternIcon("FFFFFFFFFFFFFFFF")
        assertEquals("Icon width must be 16", 16, icon.iconWidth)
    }

    fun `test icon has correct height`() {
        val icon = TiBasicCharPatternIcon("FFFFFFFFFFFFFFFF")
        assertEquals("Icon height must be 16", 16, icon.iconHeight)
    }

    fun `test icon paints all-black for all-ones pattern`() {
        val icon = TiBasicCharPatternIcon("FFFFFFFFFFFFFFFF")
        val image = BufferedImage(icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_RGB)
        icon.paintIcon(null, image.graphics, 0, 0)
        val blackPixel = java.awt.Color.BLACK.rgb
        // Skip the outermost border pixel row/column (drawn as dark gray by drawRect)
        for (y in 1 until icon.iconHeight - 1) {
            for (x in 1 until icon.iconWidth - 1) {
                assertEquals("Interior pixels must be black for pattern FFFFFFFFFFFFFFFF at ($x,$y)", blackPixel, image.getRGB(x, y))
            }
        }
    }

    fun `test icon paints all-white for all-zeros pattern`() {
        val icon = TiBasicCharPatternIcon("0000000000000000")
        val image = BufferedImage(icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_RGB)
        icon.paintIcon(null, image.graphics, 0, 0)
        val whitePixel = java.awt.Color.WHITE.rgb
        for (y in 1 until icon.iconHeight - 1) {
            for (x in 1 until icon.iconWidth - 1) {
                assertEquals("Interior pixels must be white for pattern 0000000000000000 at ($x,$y)", whitePixel, image.getRGB(x, y))
            }
        }
    }

    fun `test icon paints without exception for mixed pattern`() {
        val icon = TiBasicCharPatternIcon("00001C3E7F7F7F7F")
        val image = BufferedImage(icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_RGB)
        icon.paintIcon(null, image.graphics, 0, 0)
    }

    fun `test icon paints padded rows as white for 2-char pattern`() {
        val icon = TiBasicCharPatternIcon("FF00000000000000")
        val image = BufferedImage(icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_RGB)
        icon.paintIcon(null, image.graphics, 0, 0)
        val whitePixel = java.awt.Color.WHITE.rgb
        // Rows 1-7 (y pixels 2-15) must be white since the pattern is "FF" zero-padded
        for (y in 2 until icon.iconHeight - 1) {
            for (x in 1 until icon.iconWidth - 1) {
                assertEquals("Padded rows must be white at ($x,$y)", whitePixel, image.getRGB(x, y))
            }
        }
    }
}
