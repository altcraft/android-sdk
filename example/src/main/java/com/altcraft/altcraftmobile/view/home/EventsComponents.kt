package com.altcraft.altcraftmobile.view.home

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altcraft.altcraftmobile.extensions.Extensions.UI.fadeToWhiteEdges
import com.altcraft.altcraftmobile.functions.app.Formatter.formatDateToTimestampString
import com.altcraft.sdk.data.DataClasses
import java.util.Date

object EventsComponents {

    @Composable
    fun EventCard(
        eventCode: Int? = 0,
        eventDate: Date? = null,
        eventMessage: String,
        fontSizeForIcon: TextUnit = 16.sp,
        fontSizeForBody: TextUnit = 10.sp
    ) {
        val scrollState = rememberScrollState()
        var isExpanded by remember { mutableStateOf(false) }
        var fontSizeForDate by remember { mutableStateOf(8.5.sp) }
        var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

        Box(
            modifier = Modifier
                .width(140.dp)
                .height(140.dp)
                .padding(10.dp)
                .shadow(
                    elevation = 3.dp,
                    shape = RoundedCornerShape(15.dp),
                    clip = false
                )
                .clip(RoundedCornerShape(15.dp))
                .background(Color.White)
                .clickable { isExpanded = !isExpanded }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                if (eventCode != null) {
                    Text(
                        modifier = Modifier.offset(y = (-3).dp),
                        text = eventCode.toString(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = fontSizeForIcon
                        ),
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(5.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .clickable { isExpanded = true }
                ) {
                    Text(
                        text = eventMessage,
                        lineHeight = 10.sp,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = fontSizeForBody
                        ),
                        color = Color.Black,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 4,
                        overflow = if (isExpanded) TextOverflow.Visible else TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(5.dp))

                if (eventDate != null) {
                    Column {
                        Divider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            thickness = 1.dp,
                            color = Color(0xFFCCCCCC)
                        )

                        Text(
                            text = formatDateToTimestampString(eventDate),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = fontSizeForDate,
                                lineHeight = 12.sp
                            ),
                            color = Color.Black,
                            onTextLayout = { textLayoutResult = it },
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            softWrap = false
                        )

                        LaunchedEffect(textLayoutResult) {
                            textLayoutResult?.let { layoutResult ->
                                val doesTextFit = layoutResult.didOverflowWidth.not()
                                if (!doesTextFit && fontSizeForDate > 6.sp) {
                                    fontSizeForDate *= 0.9f
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun GenerateEventCards(eventsList: List<DataClasses.Event>) {
        val listState = rememberLazyListState()

        LaunchedEffect(eventsList.size) {
            if (eventsList.isNotEmpty()) {
                listState.animateScrollToItem(eventsList.lastIndex)
            }
        }

        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .fadeToWhiteEdges(isVertical = false),
            horizontalArrangement = Arrangement.spacedBy((-5).dp)
        ) {
            items(
                items = eventsList,
                key = { event -> event.hashCode() }
            ) { event ->
                EventCard(
                    eventMessage = "${event.function}(): ${event.eventMessage ?: ""}",
                    eventCode = event.eventCode ?: 0,
                    eventDate = event.date
                )
            }
        }
    }
}

