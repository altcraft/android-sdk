package com.altcraft.altcraftmobile.view.menu

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.inapp.inApp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.altcraft.altcraftmobile.R
import com.altcraft.sdk.AltcraftSDK

object UIMenu {

    @Composable
    fun BottomNavigationBar(navController: NavHostController) {
        val context = LocalContext.current
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White,
                                Color.White,
                                Color.White
                            )
                        )
                    )
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                NavigationButton(
                    iconRes = R.drawable.ic_menu_home,
                    label = "Home",
                    onClick = {
                        navController.navigate("home")
                        AltcraftSDK.inApp.setScreen(context, "home")
                    },
                    textSize = 10.sp,
                    textColor = Color.Black
                )
                NavigationButton(
                    iconRes = R.drawable.ic_menu_example,
                    label = "Example",
                    onClick = {
                        navController.navigate("example")
                    },
                    textSize = 10.sp,
                    textColor = Color.Black
                )
                NavigationButton(
                    iconRes = R.drawable.ic_menu_log,
                    label = "Logs",
                    onClick = { navController.navigate("logs") },
                    textSize = 10.sp,
                    textColor = Color.Black
                )
                NavigationButton(
                    iconRes = R.drawable.ic_in_app,
                    label = "InApp",
                    onClick = {
                        navController.navigate("in-app")
                        AltcraftSDK.inApp.setScreen(context, "inapp")
                              },
                    textSize = 10.sp,
                    textColor = Color.Black
                )
                NavigationButton(
                    iconRes = R.drawable.ic_menu_config,
                    label = "Config",
                    onClick = {
                        navController.navigate("config")
                    },
                    textSize = 10.sp,
                    textColor = Color.Black
                )
            }
        }
    }

    @Composable
    fun NavigationButton(
        iconRes: Int,
        label: String,
        onClick: () -> Unit,
        textSize: TextUnit = 12.sp,
        textColor: Color = Color.Black
    ) {
        Column(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                modifier = Modifier.size(23.dp)
            )
            Text(
                text = label,
                fontSize = textSize,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}