package com.altcraft.altcraftmobile.view.config

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.altcraft.altcraftmobile.functions.app.UI.SpacerHeightDp
import com.altcraft.altcraftmobile.view.config.ConfigSettingComponents.ConfigSetting
import com.altcraft.altcraftmobile.view.config.JWTSettingComponents.JWTSetting
import com.altcraft.altcraftmobile.view.config.SubscribeSettingComponents.SubscribeSetting
import com.altcraft.altcraftmobile.view.header.Header.HeaderSection
import com.altcraft.altcraftmobile.extensions.Extensions.UI.fadeToWhiteEdges
import com.altcraft.altcraftmobile.viewmodel.MainViewModel


@Composable
fun ConfigScreen(viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        HeaderSection()
        SpacerHeightDp(10.dp)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .fadeToWhiteEdges(isVertical = true),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { ConfigSetting(viewModel = viewModel) }
            item { JWTSetting(viewModel = viewModel) }
            item {
                SubscribeSetting(
                    viewModel = viewModel,
                    modifier = Modifier.fillParentMaxHeight(0.9f)
                )
            }
        }
    }
}
