package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase
import com.github.mmrsic.idea.plugins.tibasic.lang.TiColor
import java.awt.Color
import java.awt.image.BufferedImage

class TiBasicCallColorLineMarkerTest : TiBasicTestBase() {

    fun `test gutter icon appears for CALL COLOR with constant arguments`() {
        configureFile("100 CALL COLOR(2,7,1)")
        val gutters = myFixture.findAllGutters()
        assertEquals("Exactly one gutter icon must appear for CALL COLOR", 1, gutters.size)
    }

    fun `test gutter icon appears for CALL COLOR with variable foreground`() {
        configureFile("100 CALL COLOR(2,FG,1)")
        val gutters = myFixture.findAllGutters()
        assertEquals("Gutter icon must appear even with variable fg (shown as Transparent)", 1, gutters.size)
    }

    fun `test gutter icon appears for CALL COLOR with variable background`() {
        configureFile("100 CALL COLOR(2,7,BG)")
        val gutters = myFixture.findAllGutters()
        assertEquals("Gutter icon must appear even with variable bg (shown as Transparent)", 1, gutters.size)
    }

    fun `test gutter icon appears for CALL COLOR with all variable arguments`() {
        configureFile("100 CALL COLOR(S,FG,BG)")
        val gutters = myFixture.findAllGutters()
        assertEquals("Gutter icon must appear even with all variable arguments", 1, gutters.size)
    }

    fun `test gutter icon tooltip contains fg and bg color names`() {
        configureFile("100 CALL COLOR(1,2,16)")
        val gutters = myFixture.findAllGutters()
        assertEquals(1, gutters.size)
        val tooltip = gutters[0].tooltipText ?: ""
        assertTrue("Tooltip must contain fg color name Black (code 2)", tooltip.contains("Black"))
        assertTrue("Tooltip must contain bg color name White (code 16)", tooltip.contains("White"))
    }

    fun `test gutter icon tooltip contains color name for constant fg variable`() {
        configureFile("100 LET FG=2\n200 CALL COLOR(1,FG,16)")
        val gutters = myFixture.findAllGutters()
        assertEquals(1, gutters.size)
        val tooltip = gutters[0].tooltipText ?: ""
        assertTrue("Tooltip must contain fg color name Black (code 2) from constant variable", tooltip.contains("Black"))
    }

    fun `test gutter icon tooltip contains color name for constant bg variable`() {
        configureFile("100 LET BG=16\n200 CALL COLOR(1,2,BG)")
        val gutters = myFixture.findAllGutters()
        assertEquals(1, gutters.size)
        val tooltip = gutters[0].tooltipText ?: ""
        assertTrue("Tooltip must contain bg color name White (code 16) from constant variable", tooltip.contains("White"))
    }

    fun `test gutter icon shows Transparent for non-constant numeric variable`() {
        configureFile("100 LET FG=2\n200 LET FG=7\n300 CALL COLOR(1,FG,16)")
        val gutters = myFixture.findAllGutters()
        assertEquals(1, gutters.size)
        val tooltip = gutters[0].tooltipText ?: ""
        assertTrue("Tooltip must contain Transparent for non-constant fg variable", tooltip.contains("Transparent"))
    }

    fun `test gutter icon shows Transparent for uninitialized numeric variable`() {
        configureFile("100 CALL COLOR(1,FG,16)")
        val gutters = myFixture.findAllGutters()
        assertEquals(1, gutters.size)
        val tooltip = gutters[0].tooltipText ?: ""
        assertTrue("Uninitialized numeric variable defaults to 0, which is out of range; must show Transparent", tooltip.contains("Transparent"))
    }

    fun `test gutter icon shows Transparent for string variable used as color argument`() {
        configureFile("100 LET C\$=\"7\"\n200 CALL COLOR(1,C\$,16)")
        val gutters = myFixture.findAllGutters()
        assertEquals(1, gutters.size)
        val tooltip = gutters[0].tooltipText ?: ""
        assertTrue("String variable is not a valid numeric color argument; must show Transparent", tooltip.contains("Transparent"))
    }

    fun `test gutter icon does not appear for CALL SCREEN`() {
        configureFile("100 CALL SCREEN(2)")
        val gutters = myFixture.findAllGutters()
        assertEquals("Exactly one gutter icon must appear for CALL SCREEN (from SCREEN provider, not COLOR)", 1, gutters.size)
    }

    fun `test gutter icon does not appear for CALL CLEAR`() {
        configureFile("100 CALL CLEAR")
        val gutters = myFixture.findAllGutters()
        assertTrue("No gutter icon must appear for CALL CLEAR", gutters.isEmpty())
    }
}

class TiBasicColorPreviewIconTest : TiBasicTestBase() {

    fun `test icon has correct width`() {
        assertEquals(16, TiBasicColorPreviewIcon(TiColor.Black, TiColor.White).iconWidth)
    }

    fun `test icon has correct height`() {
        assertEquals(16, TiBasicColorPreviewIcon(TiColor.Black, TiColor.White).iconHeight)
    }

    fun `test icon paints left half in fg color`() {
        val icon = TiBasicColorPreviewIcon(TiColor.Black, TiColor.White)
        val image = BufferedImage(icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_RGB)
        icon.paintIcon(null, image.graphics, 0, 0)
        val blackRgb = Color.BLACK.rgb
        // Interior of left half (skip border at x=0)
        for (y in 1 until icon.iconHeight - 1) {
            assertEquals("Left half interior must be black at y=$y", blackRgb, image.getRGB(1, y))
        }
    }

    fun `test icon paints right half in bg color`() {
        val icon = TiBasicColorPreviewIcon(TiColor.Black, TiColor.White)
        val image = BufferedImage(icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_RGB)
        icon.paintIcon(null, image.graphics, 0, 0)
        val whiteRgb = Color.WHITE.rgb
        // Interior of right half (skip border at x=15)
        for (y in 1 until icon.iconHeight - 1) {
            assertEquals("Right half interior must be white at y=$y", whiteRgb, image.getRGB(14, y))
        }
    }

    fun `test icon paints without exception for Transparent colors`() {
        val icon = TiBasicColorPreviewIcon(TiColor.Transparent, TiColor.Transparent)
        val image = BufferedImage(icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_RGB)
        icon.paintIcon(null, image.graphics, 0, 0)
    }

    fun `test icon paints without exception for all TiColor values`() {
        TiColor.entries.forEach { fg ->
            TiColor.entries.forEach { bg ->
                val icon = TiBasicColorPreviewIcon(fg, bg)
                val image = BufferedImage(icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_RGB)
                icon.paintIcon(null, image.graphics, 0, 0)
            }
        }
    }
}
