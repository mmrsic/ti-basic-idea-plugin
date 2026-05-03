package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase
import com.github.mmrsic.idea.plugins.tibasic.lang.TiColor
import java.awt.Color
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

    fun `test gutter icon appears for CALL CHAR with constant string variable`() {
        configureFile("100 LET P$=\"FFFFFFFFFFFFFFFF\"\n200 CALL CHAR(96,P$)")
        val gutters = myFixture.findAllGutters()
        assertEquals("Gutter icon must appear when pattern variable has a single constant string value", 1, gutters.size)
    }

    fun `test gutter icon does not appear for CALL CHAR with non-constant string variable`() {
        configureFile("100 LET P$=\"FF\"\n200 LET P$=\"0F\"\n300 CALL CHAR(96,P$)")
        val gutters = myFixture.findAllGutters()
        assertTrue("No gutter icon must appear when pattern variable has multiple different values", gutters.isEmpty())
    }

    fun `test gutter icon appears for CALL CHAR with uninitialized string variable`() {
        configureFile("100 CALL CHAR(96,PAT$)")
        val gutters = myFixture.findAllGutters()
        assertEquals("Gutter icon must appear for uninitialized string variable (defaults to empty pattern)", 1, gutters.size)
    }

    fun `test gutter icon does not appear for CALL CHAR with numeric variable instead of string`() {
        configureFile("100 LET N=255\n200 CALL CHAR(96,N)")
        val gutters = myFixture.findAllGutters()
        assertTrue("No gutter icon must appear when pattern argument is a numeric variable", gutters.isEmpty())
    }

    fun `test gutter icon does not appear for CALL SCREEN`() {
        configureFile("100 CALL SCREEN(2)")
        val gutters = myFixture.findAllGutters()
        assertEquals("Exactly one gutter icon must appear for CALL SCREEN (from SCREEN provider, not CHAR)", 1, gutters.size)
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
        val blackPixel = Color.BLACK.rgb
        for (y in 0 until icon.iconHeight) {
            for (x in 0 until icon.iconWidth) {
                assertEquals("All pixels must be black for pattern FFFFFFFFFFFFFFFF at ($x,$y)", blackPixel, image.getRGB(x, y))
            }
        }
    }

    fun `test icon paints checkerboard for all-zeros pattern`() {
        val icon = TiBasicCharPatternIcon("0000000000000000")
        val image = BufferedImage(icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_RGB)
        icon.paintIcon(null, image.graphics, 0, 0)
        val whitePixel = Color.WHITE.rgb
        // CELL_SIZE = 2: each character cell occupies a 2×2 pixel block.
        for (row in 0 until 8) {
            for (col in 0 until 8) {
                // Verify top-left pixel of the 2×2 block
                assertEquals("0-bit cell ($row,$col) must have white color", whitePixel, image.getRGB(col * 2, row * 2))
            }
        }
    }

    fun `test icon paints without exception for mixed pattern`() {
        val icon = TiBasicCharPatternIcon("00001C3E7F7F7F7F")
        val image = BufferedImage(icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_RGB)
        icon.paintIcon(null, image.graphics, 0, 0)
    }

    fun `test icon paints 1-bit cells as black in mixed pattern`() {
        // "FF00000000000000" → first character row is all 1-bits (black), rest are 0-bits (checkerboard)
        val icon = TiBasicCharPatternIcon("FF00000000000000")
        val image = BufferedImage(icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_RGB)
        icon.paintIcon(null, image.graphics, 0, 0)
        val blackPixel = Color.BLACK.rgb
        for (col in 0 until 8) {
            assertEquals("First row must be black for FF pattern at col $col", blackPixel, image.getRGB(col * 2, 0))
        }
    }

    fun `test icon bottom-row pattern differs from empty pattern`() {
        // Regression: "00000000000000FF" must look different from "" (all-zero)
        val iconBottomRow = TiBasicCharPatternIcon("00000000000000FF")
        val iconEmpty = TiBasicCharPatternIcon("0000000000000000")
        val imageBottomRow = BufferedImage(iconBottomRow.iconWidth, iconBottomRow.iconHeight, BufferedImage.TYPE_INT_RGB)
        val imageEmpty = BufferedImage(iconEmpty.iconWidth, iconEmpty.iconHeight, BufferedImage.TYPE_INT_RGB)
        iconBottomRow.paintIcon(null, imageBottomRow.graphics, 0, 0)
        iconEmpty.paintIcon(null, imageEmpty.graphics, 0, 0)
        val blackPixel = Color.BLACK.rgb
        // Last character row (row 7) of "00000000000000FF" must be black
        for (col in 0 until 8) {
            assertEquals("Bottom row must be black for 00000000000000FF at col $col", blackPixel, imageBottomRow.getRGB(col * 2, 7 * 2))
        }
        // Same row of all-zero pattern must NOT be black (it's checkerboard)
        val lastRowAllBlack = (0 until 8).all { col -> imageEmpty.getRGB(col * 2, 7 * 2) == blackPixel }
        assertFalse("All-zero pattern must not have an all-black bottom row", lastRowAllBlack)
    }

    fun `test icon paints padded rows as checkerboard for 2-char pattern`() {
        val icon = TiBasicCharPatternIcon("FF00000000000000")
        val image = BufferedImage(icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_RGB)
        icon.paintIcon(null, image.graphics, 0, 0)
        val whitePixel = Color.WHITE.rgb
        // Rows 1-7 (character rows) must be checkerboard since the pattern is "FF" zero-padded
        for (row in 1 until 8) {
            for (col in 0 until 8) {
                assertEquals("Padded row $row, col $col must be WHITE", whitePixel, image.getRGB(col * 2, row * 2))
            }
        }
    }

    fun `test colored icon paints set bits in foreground color`() {
        val icon = TiBasicColoredCharPatternIcon("FF00000000000000", TiColor.DarkRed, TiColor.White)
        val image = BufferedImage(icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_RGB)

        icon.paintIcon(null, image.graphics, 0, 0)

        assertEquals(Color(TiColor.DarkRed.rgbValue).rgb, image.getRGB(1, 1))
    }

    fun `test colored icon paints unset bits in background color`() {
        val icon = TiBasicColoredCharPatternIcon("FF00000000000000", TiColor.DarkRed, TiColor.LightBlue)
        val image = BufferedImage(icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_RGB)

        icon.paintIcon(null, image.graphics, 0, 0)

        assertEquals(Color(TiColor.LightBlue.rgbValue).rgb, image.getRGB(1, 3))
    }
}
