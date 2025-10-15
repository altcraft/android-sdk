package com.altcraft.altcraftmobile.view.header

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.altcraft.altcraftmobile.R


object Header {

    @Composable
    fun HeaderSection() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .shadow(
                    elevation = 3.dp,
                    shape = RoundedCornerShape(
                        topStart = 0.dp,
                        topEnd = 0.dp,
                        bottomStart = 5.dp,
                        bottomEnd = 5.dp
                    ),
                    clip = false
                )
                .clip(
                    RoundedCornerShape(
                        topStart = 0.dp,
                        topEnd = 0.dp,
                        bottomStart = 5.dp,
                        bottomEnd = 5.dp
                    )
                )
                .background(Color.White)
        ) {
            Column {
                Image(
                    painter = painterResource(id = R.drawable.ic_altcraft),
                    contentDescription = "",
                    modifier = Modifier
                        .size(
                            width = 80.dp,
                            height = 20.dp
                        )
                        .offset(
                            x = 16.dp,
                            y = 16.dp
                        )
                )
            }
        }
    }
}
