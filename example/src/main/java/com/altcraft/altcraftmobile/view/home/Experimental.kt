package com.altcraft.altcraftmobile.view.home

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

object Experimental {
    /**
     * Draws a repeating polka-dots background.
     *
     * @param backgroundColor color of the background
     * @param dotColor color of the dots
     * @param dotRadius radius of each dot
     * @param spacing distance BETWEEN dot centers (grid step)
     * @param offsetX horizontal offset (can be animated)
     * @param offsetY vertical offset (can be animated)
     * @param staggerRows if true, odd rows are shifted by half spacing (staggered/hex-like layout)
     * @param dotAlpha dot opacity
     */
    fun Modifier.dotsBackground(
        backgroundColor: Color = Color.Black,
        dotColor: Color = Color.White,
        dotRadius: Dp = 10.dp,
        spacing: Dp = 32.dp,
        offsetX: Dp = 0.dp,
        offsetY: Dp = 0.dp,
        staggerRows: Boolean = false,
        dotAlpha: Float = 1f,
    ): Modifier = this.then(
        Modifier.drawWithCache {
            val r = dotRadius.toPx().coerceAtLeast(0f)
            val step = spacing.toPx().coerceAtLeast(1f)
            val ox = offsetX.toPx()
            val oy = offsetY.toPx()

            val extra = step

            onDrawBehind {
                drawRect(color = backgroundColor)

                val w = size.width
                val h = size.height

                val startX = -extra + (ox % step)
                val startY = -extra + (oy % step)

                val cols = ceil((w + 2 * extra) / step).toInt() + 1
                val rows = ceil((h + 2 * extra) / step).toInt() + 1

                for (row in 0 until rows) {
                    val y = startY + row * step

                    val rowShift = if (staggerRows && (row % 2 == 1)) step / 2f else 0f

                    for (col in 0 until cols) {
                        val x = startX + col * step + rowShift

                        if (x + r < 0f || x - r > w || y + r < 0f || y - r > h) continue

                        drawCircle(
                            color = dotColor.copy(alpha = dotAlpha),
                            radius = r,
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }
    )
}