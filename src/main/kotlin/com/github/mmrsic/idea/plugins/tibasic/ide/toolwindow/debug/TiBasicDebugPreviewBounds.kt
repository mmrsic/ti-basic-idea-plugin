package com.github.mmrsic.idea.plugins.tibasic.ide.toolwindow.debug

import com.intellij.ui.JBColor
import java.awt.Graphics2D
import java.awt.GradientPaint
import java.awt.Rectangle
import kotlin.math.roundToInt

internal fun scaledDebugPreviewBounds(
    availableWidth: Int,
    availableHeight: Int,
    previewWidth: Int,
    previewHeight: Int,
    keepAspectRatio: Boolean,
): Rectangle {
    if (!keepAspectRatio) {
        return Rectangle(0, 0, availableWidth, availableHeight)
    }
    val scale = minOf(
        availableWidth.toDouble() / previewWidth,
        availableHeight.toDouble() / previewHeight,
    )
    val scaledWidth = (previewWidth * scale).roundToInt()
    val scaledHeight = (previewHeight * scale).roundToInt()
    return Rectangle(
        (availableWidth - scaledWidth) / 2,
        (availableHeight - scaledHeight) / 2,
        scaledWidth,
        scaledHeight,
    )
}

internal fun paintDebugPreviewBackdrop(
    graphics: Graphics2D,
    width: Int,
    height: Int,
) {
    graphics.paint = GradientPaint(
        0f,
        0f,
        JBColor(DEBUG_PREVIEW_SHADE_LIGHT, DEBUG_PREVIEW_SHADE_DARK),
        width.toFloat(),
        height.toFloat(),
        JBColor(DEBUG_PREVIEW_SHADE_DARK, DEBUG_PREVIEW_SHADE_LIGHT),
    )
    graphics.fillRect(0, 0, width, height)
}

private const val DEBUG_PREVIEW_SHADE_LIGHT = 0xD3D3D3
private const val DEBUG_PREVIEW_SHADE_DARK = 0x8C8C8C
