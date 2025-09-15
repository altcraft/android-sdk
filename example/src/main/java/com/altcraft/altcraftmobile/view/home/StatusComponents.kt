package com.altcraft.altcraftmobile.view.home

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.altcraft.altcraftmobile.data.AppConstants.SUBSCRIBED
import com.altcraft.altcraftmobile.data.AppConstants.SUSPENDED
import com.altcraft.altcraftmobile.data.AppConstants.UNSUBSCRIBED

object StatusComponents {

    @Composable
    fun StatusIndicator(
        radius: Dp,
        marginTop: Dp = 0.dp,
        status: String = "unsubscribe",
        @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
    ) {
        val startColor: Color = when (status) {
            UNSUBSCRIBED -> Color(0xFFFF6E7F)
            SUSPENDED -> Color(0xFFFFD700)
            SUBSCRIBED -> Color(0xFFADF7BD)
            else -> Color(0xFFFF6E7F)
        }

        val endColor: Color = when (status) {
            UNSUBSCRIBED -> Color(0xFFFF6E7F)
            SUSPENDED -> Color(0xFFFFD700)
            SUBSCRIBED -> Color(0xFF33FF5F)
            else -> Color(0xFFFF6E7F)
        }

        val size = minOf(radius, radius)

        val widthPx = with(LocalDensity.current) { size.toPx() }
        val heightPx = with(LocalDensity.current) { size.toPx() }

        Canvas(
            modifier = modifier
                .padding(top = marginTop)
                .size(size, size)
        ) {
            val brush = Brush.horizontalGradient(
                colors = listOf(startColor, endColor)
            )

            drawCircle(
                brush = brush,
                radius = size.toPx() / 2,
                center = Offset(widthPx / 2, heightPx / 2)
            )
        }
    }
}