package com.altcraft.altcraftmobile.view.home

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altcraft.altcraftmobile.view.home.ActionsComponents.ActionButtonRow
import com.altcraft.altcraftmobile.view.home.ActionsComponents.SubBox
import com.altcraft.altcraftmobile.view.home.EventsComponents.GenerateEventCards
import com.altcraft.altcraftmobile.view.home.ProfileComponents.ProfileBox
import com.altcraft.altcraftmobile.extensions.Extensions.UI.fadeToWhiteEdges
import com.altcraft.altcraftmobile.viewmodel.MainViewModel

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val status = viewModel.status.value
    val subscribeAction = viewModel.subscribeActions.value
    val profileAction = viewModel.profileActions.value
    val eventList by viewModel.eventList

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .fadeToWhiteEdges(isVertical = true)
            .background(Color.White)
    ) {
        item {
            InfoComponents.InfoCard(viewModel)
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))

            Row {
                TextComponents.Text(
                    text = "Subscribe status:",
                    fontSize = 14.sp,
                    paddingStart = 15.dp
                )

                Spacer(modifier = Modifier.width(5.dp))

                StatusComponents.StatusIndicator(15.dp, 3.dp, status)
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))

            Row {
                TextComponents.Text(
                    text = "Actions:",
                    fontSize = 24.sp,
                    paddingStart = 15.dp
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            ActionButtonRow(viewModel)
        }

        if (subscribeAction || profileAction) {
            item {
                Spacer(modifier = Modifier.height(20.dp))
            }

            if (subscribeAction) {
                item {
                    TextComponents.Text(
                        text = "Subscribe to:",
                        fontSize = 24.sp,
                        paddingStart = 15.dp
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    SubBox(
                        viewModel,
                        modifier = Modifier
                    )
                }
            }

            if (profileAction) {
                item {
                    Column {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextComponents.Text(
                                text = "Profile info:",
                                fontSize = 24.sp,
                                paddingStart = 15.dp
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            if (viewModel.profileDataIsLoading.value) {
                                Box(
                                    modifier = Modifier
                                        .padding(end = 25.dp)
                                        .size(20.dp)
                                ) {
                                    CircularProgressIndicator(strokeWidth = 2.dp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        ProfileBox(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        } else {
            item {
                Spacer(modifier = Modifier.height(20.dp))

                TextComponents.Text(
                    text = "Main events:",
                    fontSize = 24.sp,
                    paddingStart = 15.dp
                )
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))

                GenerateEventCards(eventList)

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}


