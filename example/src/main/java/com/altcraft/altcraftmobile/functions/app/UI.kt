package com.altcraft.altcraftmobile.functions.app

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.


import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

object UI {
    @Composable
    fun SpacerHeightDp(height: Dp) {
        val density = LocalDensity.current
        Spacer(modifier = Modifier.height(with(density) { height.toPx().toDp() }))
    }
}