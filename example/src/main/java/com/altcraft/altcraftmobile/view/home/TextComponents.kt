package com.altcraft.altcraftmobile.view.home

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

object TextComponents {

    @Composable
    fun Text(
        text: String,
        fontSize: TextUnit,
        paddingStart: Dp = 0.dp,
        fontWeight: FontWeight = FontWeight.Bold,
        color: Color = Color.Black,
        @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = fontWeight,
                fontSize = fontSize
            ),
            color = color,
            modifier = modifier
                .padding(start = paddingStart)
        )
    }
}