package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.lang.TiColor
import com.intellij.ui.JBColor
import java.awt.Component
import java.awt.Graphics
import javax.swing.Icon

private const val ICON_SIZE = 16
private const val HALF_WIDTH = ICON_SIZE / 2
private const val CHECKER_CELL = 2

class TiBasicColorPreviewIcon(private val fg: TiColor, private val bg: TiColor) : Icon {

    override fun getIconWidth(): Int = ICON_SIZE
    override fun getIconHeight(): Int = ICON_SIZE

    override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
        paintColorRect(g, x, y, HALF_WIDTH, fg)
        paintColorRect(g, x + HALF_WIDTH, y, HALF_WIDTH, bg)
        g.color = JBColor.DARK_GRAY
        g.drawRect(x, y, ICON_SIZE - 1, ICON_SIZE - 1)
        g.drawLine(x + HALF_WIDTH, y, x + HALF_WIDTH, y + ICON_SIZE - 1)
    }
}

class TiBasicScreenColorIcon(private val color: TiColor) : Icon {

    override fun getIconWidth(): Int = ICON_SIZE
    override fun getIconHeight(): Int = ICON_SIZE

    override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
        paintColorRect(g, x, y, ICON_SIZE, color)
        g.color = JBColor.DARK_GRAY
        g.drawRect(x, y, ICON_SIZE - 1, ICON_SIZE - 1)
    }
}

private fun paintColorRect(g: Graphics, x: Int, y: Int, width: Int, color: TiColor) {
    if (color == TiColor.Transparent) {
        for (row in 0 until ICON_SIZE) {
            for (col in 0 until width) {
                g.color = if ((row / CHECKER_CELL + col / CHECKER_CELL) % 2 == 0) JBColor.LIGHT_GRAY else JBColor.WHITE
                g.fillRect(x + col, y + row, 1, 1)
            }
        }
    } else {
        g.color = JBColor(color.rgbValue, color.rgbValue)
        g.fillRect(x, y, width, ICON_SIZE)
    }
}
