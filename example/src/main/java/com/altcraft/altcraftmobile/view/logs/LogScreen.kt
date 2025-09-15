package com.altcraft.altcraftmobile.view.logs

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altcraft.altcraftmobile.view.header.Header.HeaderSection
import com.altcraft.altcraftmobile.extensions.Extensions.UI.fadeToWhiteEdges
import com.altcraft.altcraftmobile.functions.app.Formatter.formatDateToTimestampString
import com.altcraft.altcraftmobile.viewmodel.MainViewModel
import java.util.Date

object LogScreen {

    @Composable
    fun LogScreen(viewModel: MainViewModel) {
        val events by viewModel.eventList
        val listState = rememberLazyListState()

        LaunchedEffect(events.size) {
            if (events.isNotEmpty()) {
                listState.animateScrollToItem(events.lastIndex)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            HeaderSection()

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f)
            ) {
                if (events.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No events yet",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .fadeToWhiteEdges(isVertical = true),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(events) { event ->
                            Column {
                                EventLogItem(
                                    date = event.date,
                                    message = event.eventMessage ?: "No message",
                                )
                                Divider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    thickness = 0.8.dp,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun EventLogItem(
        date: Date?,
        message: String
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(2.dp)
            ) {
                Row(
                    modifier = Modifier.wrapContentWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    date?.let {
                        Text(
                            text = formatDateToTimestampString(it),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.W700
                            ),
                            color = Color(0xFF3678E2)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = message,
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}