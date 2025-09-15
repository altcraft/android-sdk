package com.altcraft.altcraftmobile.view.example

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.altcraft.altcraftmobile.view.example.ExampleComponents.ButtonsSetting
import com.altcraft.altcraftmobile.functions.app.UI.SpacerHeightDp
import com.altcraft.altcraftmobile.view.example.ExampleComponents.ImageSetting
import com.altcraft.altcraftmobile.view.example.ExampleComponents.NotificationCard
import com.altcraft.altcraftmobile.view.example.ExampleComponents.TextSetting
import com.altcraft.altcraftmobile.view.header.Header.HeaderSection
import com.altcraft.altcraftmobile.view.home.ActionsComponents.SectionInfo
import com.altcraft.altcraftmobile.functions.app.Notification.getExamplePushData
import com.altcraft.altcraftmobile.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.altcraft.altcraftmobile.extensions.Extensions.UI.fadeToWhiteEdges
import com.altcraft.sdk.AltcraftSDK

@Composable
fun ExampleScreen(viewModel: MainViewModel) {

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HeaderSection()
            SpacerHeightDp(15.dp)
            NotificationCard(viewModel = viewModel, onButtonClick = {})
            SpacerHeightDp(5.dp)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .fadeToWhiteEdges(isVertical = true),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { TextSetting(viewModel = viewModel) }
                item { ImageSetting(viewModel = viewModel) }
                item { ButtonsSetting(viewModel = viewModel) }
                item {
                    SectionInfo(boxHeight = 30.dp, onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            AltcraftSDK.PushReceiver.takePush(
                                context,
                                getExamplePushData(context)
                            )
                        }
                    })
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}









