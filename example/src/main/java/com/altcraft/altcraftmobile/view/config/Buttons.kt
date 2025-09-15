package com.altcraft.altcraftmobile.view.config

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.altcraft.altcraftmobile.view.home.ActionsComponents.PlusIcon
import com.altcraft.altcraftmobile.view.home.ActionsComponents.XShapedIcon

object Buttons {
    @Composable
    fun AddButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        buttonSize: Dp = 24.dp,
        iconSize: Dp = 18.dp,
        iconColor: Color = Color.Black,
        iconBackgroundColor: Color = Color(0xFFADF7BD).copy(alpha = 0.4f)
    ) {
        Box(
            modifier = modifier
                .size(buttonSize)
                .shadow(
                    elevation = 3.dp,
                    shape = CircleShape,
                    clip = false
                )
                .clip(CircleShape)
                .background(Color.White)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .clip(CircleShape)
                    .background(iconBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                PlusIcon(
                    lineWidth = 10.dp,
                    lineHeight = 2.5.dp,
                    color = iconColor
                )
            }
        }
    }

    @Composable
    fun RemoveButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        buttonSize: Dp = 24.dp,
        iconSize: Dp = 18.dp,
        iconBackgroundColor: Color = Color.Black.copy(alpha = 0.1f)
    ) {
        Box(
            modifier = modifier
                .size(buttonSize)
                .shadow(
                    elevation = 3.dp,
                    shape = CircleShape,
                    clip = false
                )
                .clip(CircleShape)
                .background(Color.White)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .clip(CircleShape)
                    .background(iconBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                XShapedIcon(
                    lineHeight = 10.dp,
                    lineWidth = 2.5.dp,
                    firstColor = Color.Black,
                    secondColor = Color.Black
                )
            }
        }
    }
}