package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.lang.TiColor
import java.awt.Component
import java.awt.Graphics
import javax.swing.Icon

internal const val TI_BASIC_CHAR_PATTERN_GRID_SIZE = 8

internal class TiBasicCharPatternBitmap private constructor(private val rowBytes: List<Int>) {

    fun bitAt(row: Int, col: Int): Boolean =
        (rowBytes[row] shr (TI_BASIC_CHAR_PATTERN_GRID_SIZE - 1 - col)) and 1 == 1

    companion object {
        fun fromHexPattern(hexPattern: String): TiBasicCharPatternBitmap =
            TiBasicCharPatternBitmap(
                (0 until TI_BASIC_CHAR_PATTERN_GRID_SIZE).map { row ->
                    hexPattern.substring(row * 2, row * 2 + 2).toInt(16)
                },
            )
    }
}

private const val CHARACTER_CELL_SIZE = 2
private const val CHARACTER_ICON_SIZE = CHARACTER_CELL_SIZE * TI_BASIC_CHAR_PATTERN_GRID_SIZE

internal fun paintTiBasicCharPattern(
    g: Graphics,
    x: Int,
    y: Int,
    pixelSize: Int,
    hexPattern: String,
    fg: TiColor,
    bg: TiColor,
) {
    val bitmap = TiBasicCharPatternBitmap.fromHexPattern(hexPattern)
    for (row in 0 until TI_BASIC_CHAR_PATTERN_GRID_SIZE) {
        for (col in 0 until TI_BASIC_CHAR_PATTERN_GRID_SIZE) {
            val bitSet = bitmap.bitAt(row, col)
            paintTiColorRect(
                g = g,
                x = x + col * pixelSize,
                y = y + row * pixelSize,
                width = pixelSize,
                height = pixelSize,
                color = if (bitSet) fg else bg,
            )
        }
    }
}

class TiBasicCharPatternIcon(hexPattern: String) : Icon {

    private val delegate = TiBasicColoredCharPatternIcon(hexPattern, TiColor.Black, TiColor.White)

    override fun getIconWidth(): Int = delegate.iconWidth
    override fun getIconHeight(): Int = delegate.iconHeight

    override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
        delegate.paintIcon(c, g, x, y)
    }
}

class TiBasicColoredCharPatternIcon(
    private val hexPattern: String,
    private val fg: TiColor,
    private val bg: TiColor,
) : Icon {

    override fun getIconWidth(): Int = CHARACTER_ICON_SIZE
    override fun getIconHeight(): Int = CHARACTER_ICON_SIZE

    override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
        paintTiBasicCharPattern(g, x, y, CHARACTER_CELL_SIZE, hexPattern, fg, bg)
    }
}
