package com.github.mmrsic.idea.plugins.tibasic.editor

import com.intellij.ui.JBColor
import java.awt.Component
import java.awt.Graphics
import javax.swing.Icon

internal const val TI_BASIC_CHAR_PATTERN_GRID_SIZE = 8

private const val CELL_SIZE = 2
private const val ICON_SIZE = CELL_SIZE * TI_BASIC_CHAR_PATTERN_GRID_SIZE

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

class TiBasicCharPatternIcon(private val hexPattern: String) : Icon {

    override fun getIconWidth(): Int = ICON_SIZE
    override fun getIconHeight(): Int = ICON_SIZE

    override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
        val bitmap = TiBasicCharPatternBitmap.fromHexPattern(hexPattern)
        for (row in 0 until TI_BASIC_CHAR_PATTERN_GRID_SIZE) {
            for (col in 0 until TI_BASIC_CHAR_PATTERN_GRID_SIZE) {
                val bitSet = bitmap.bitAt(row, col)
                g.color = if (bitSet) JBColor.BLACK else JBColor.WHITE
                g.fillRect(x + col * CELL_SIZE, y + row * CELL_SIZE, CELL_SIZE, CELL_SIZE)
            }
        }
    }
}
