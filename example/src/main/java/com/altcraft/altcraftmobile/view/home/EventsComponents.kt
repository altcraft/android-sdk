package com.altcraft.altcraftmobile.view.home

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altcraft.altcraftmobile.functions.app.Formatter.formatDateToTimestampString
import com.altcraft.sdk.AltcraftSDK
import com.altcraft.sdk.push.push
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel.Event
import java.util.Date

object EventsComponents {

    private const val HTTP_CODE_KEY = "http_code"
    private const val TOKEN = "token"

    private val SuccessColor = Color(0xFF33FF5F)
    private val ErrorColor = Color(0xFFFF6E7F)
    private val NormalColor = Color.Black

    private val TextPrimary = Color(0xFF0F172A)
    private val TextSecondary = Color(0xFF475569)
    private val BorderColor = Color(0xFFE2E8F0)
    private val CardBackground = Color(0xFFFFFFFF)

    @Composable
    fun EventCard(
        functionName: String,
        eventDate: Date? = null,
        eventMessage: String,
        fontSizeForTitle: TextUnit = 10.sp,
        fontSizeForBody: TextUnit = 9.sp,
        event: Event? = null,
        isFirst: Boolean = false,
        isLast: Boolean = false,
        onClick: ((Event) -> Unit)? = null
    ) {
        val statusColor = getEventStatusColor(event)
        val statusBadgeText = getStatusBadgeText(event)

        var fontSizeForDate by remember { mutableStateOf(8.5.sp) }
        var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .clickable(enabled = event != null && onClick != null) {
                    if (event != null) {
                        onClick?.invoke(event)
                    }
                }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimelineIndicator(
                color = statusColor,
                isFirst = isFirst,
                isLast = isLast
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier
                    .width(82.dp)
                    .padding(start = 5.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = eventDate?.let { formatDateToTimestampString(it).substringBefore(" ") } ?: "--",
                    color = TextSecondary,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    softWrap = false
                )

                Text(
                    text = eventDate?.let { formatDateToTimestampString(it).substringAfter(" ", "") } ?: "--",
                    color = TextPrimary,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    softWrap = false
                )
            }

            LaunchedEffect(textLayoutResult) {
                textLayoutResult?.let { layoutResult ->
                    val doesTextFit = layoutResult.didOverflowWidth.not()
                    if (!doesTextFit && fontSizeForDate > 6.sp) {
                        fontSizeForDate *= 0.9f
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = functionName,
                    color = TextPrimary,
                    fontSize = fontSizeForTitle,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(1.dp))

                Text(
                    text = eventMessage.ifBlank { "SDK event" },
                    color = TextSecondary,
                    fontSize = fontSizeForBody,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (statusBadgeText != null) {
                StatusBadge(
                    text = statusBadgeText,
                    color = statusColor
                )
            } else {
                Text(
                    text = "›",
                    color = TextSecondary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        if (!isLast) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 92.dp)
                    .height(1.dp)
                    .background(BorderColor.copy(alpha = 0.7f))
            )
        }
    }

    @Composable
    fun GenerateEventCards(eventsList: List<Event>) {
        val listState = rememberLazyListState()

        val visibleEvents = eventsList
            .takeLast(5)
            .reversed()

        LaunchedEffect(eventsList.size) {
            if (visibleEvents.isNotEmpty()) {
                listState.animateScrollToItem(0)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 5.dp)
                .heightIn(max = 270.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(CardBackground)
                .padding(start = 5.dp, end = 5.dp, top = 5.dp),
            verticalArrangement = Arrangement.Top
        ) {
            itemsIndexed(
                items = visibleEvents,
                key = { _, event -> event.hashCode() }
            ) { index, event ->
                EventCard(
                    functionName = event.function,
                    eventMessage = event.eventMessage ?: "",
                    eventDate = event.date,
                    event = event,
                    isFirst = index == 0,
                    isLast = index == visibleEvents.lastIndex
                )
            }
        }
    }

    @Composable
    private fun TimelineIndicator(
        color: Color,
        isFirst: Boolean,
        isLast: Boolean
    ) {
        Box(
            modifier = Modifier
                .width(14.dp)
                .height(54.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(27.dp)
                        .align(Alignment.TopCenter)
                        .background(BorderColor)
                )
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(27.dp)
                        .align(Alignment.BottomCenter)
                        .background(BorderColor)
                )
            }

            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }

    @Composable
    private fun StatusBadge(
        text: String,
        color: Color
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(color.copy(alpha = 0.2f))
                .padding(horizontal = 6.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color.Black,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    private fun getEventStatusColor(event: Event?): Color {
        if (event == null) return NormalColor

        if (event.event == AltcraftSDK.push.moduleEvents.pushProviderSet) {
            return SuccessColor
        }

        if (event is com.altcraft.sdk.data.models.CoreModels.SDKEventModel.Error) {
            return ErrorColor
        }

        val httpCode = getHttpCode(event)

        return when (httpCode) {
            in 200..299, 304 -> SuccessColor
            null -> NormalColor
            else -> ErrorColor
        }
    }

    private fun getStatusBadgeText(event: Event?): String? {
        if (event == null) return null

        if (event.event == AltcraftSDK.push.moduleEvents.pushProviderSet)
            return getTokenLastThreeSymbols(event)

        return getHttpCode(event)?.toString()
    }


    private fun getTokenLastThreeSymbols(event: Event): String? {
        val value = event.eventValue ?: return null
        val token = value[TOKEN]?.toString()?.takeIf { it.isNotBlank() } ?: return null

        return token.takeLast(3)
    }

    private fun getHttpCode(event: Event?): Int? {
        val value = event?.eventValue ?: return null

        return when (val code = value[HTTP_CODE_KEY]) {
            is Int -> code
            is Long -> code.toInt()
            is Double -> code.toInt()
            is Float -> code.toInt()
            is String -> code.toIntOrNull()
            else -> null
        }
    }
}