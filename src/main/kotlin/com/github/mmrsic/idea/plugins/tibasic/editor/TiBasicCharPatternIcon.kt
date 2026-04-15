package com.github.mmrsic.idea.plugins.tibasic.editor

import com.intellij.ui.JBColor
import java.awt.Component
import java.awt.Graphics
import javax.swing.Icon

private const val CELL_SIZE = 2
private const val GRID_SIZE = 8
private const val ICON_SIZE = CELL_SIZE * GRID_SIZE

class TiBasicCharPatternIcon(private val hexPattern: String) : Icon {

    override fun getIconWidth(): Int = ICON_SIZE
    override fun getIconHeight(): Int = ICON_SIZE

    override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
        val bytes = parseBytes(hexPattern)
        for (row in 0 until GRID_SIZE) {
            val byte = bytes.getOrElse(row) { 0 }
            for (col in 0 until GRID_SIZE) {
                val bitSet = (byte shr (GRID_SIZE - 1 - col)) and 1 == 1
                g.color = if (bitSet) JBColor.BLACK else JBColor.WHITE
                g.fillRect(x + col * CELL_SIZE, y + row * CELL_SIZE, CELL_SIZE, CELL_SIZE)
            }
        }
    }

    private fun parseBytes(pattern: String): List<Int> =
        (0 until GRID_SIZE).map { row ->
            pattern.substring(row * 2, row * 2 + 2).toInt(16)
        }
}
