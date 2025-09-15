package com.altcraft.altcraftmobile.extensions

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.annotation.SuppressLint
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object Extensions {
    object  UI {
        @SuppressLint("UnnecessaryComposedModifier")
        fun Modifier.fadeToWhiteEdges(
            fadeLength: Dp = 10.dp,
            fadeColor: Color = Color.White,
            isVertical: Boolean = false
        ): Modifier = composed {
            val fadePx = with(LocalDensity.current) {
                fadeLength.toPx()
            }

            this.drawWithContent {
                drawContent()

                if (isVertical) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(fadeColor, Color.Transparent),
                            startY = 0f,
                            endY = fadePx,
                            tileMode = TileMode.Clamp
                        ),
                        topLeft = Offset(0f, 0f),
                        size = Size(size.width, fadePx)
                    )

                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, fadeColor),
                            startY = size.height - fadePx,
                            endY = size.height,
                            tileMode = TileMode.Clamp
                        ),
                        topLeft = Offset(0f, size.height - fadePx),
                        size = Size(size.width, fadePx)
                    )
                } else {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(fadeColor, Color.Transparent),
                            startX = 0f,
                            endX = fadePx,
                            tileMode = TileMode.Clamp
                        ),
                        topLeft = Offset(0f, 0f),
                        size = Size(fadePx, size.height)
                    )

                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, fadeColor),
                            startX = size.width - fadePx,
                            endX = size.width,
                            tileMode = TileMode.Clamp
                        ),
                        topLeft = Offset(size.width - fadePx, 0f),
                        size = Size(fadePx, size.height)
                    )
                }
            }
        }
    }
}