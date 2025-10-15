package com.altcraft.altcraftmobile.view.home

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altcraft.altcraftmobile.viewmodel.MainViewModel
import com.altcraft.altcraftmobile.data.AppConstants.EMPTY_VALUE_UI
import com.altcraft.altcraftmobile.data.AppConstants.HMS_PROVIDER
import com.altcraft.altcraftmobile.data.AppConstants.RUSTORE_PROVIDER
import com.altcraft.altcraftmobile.functions.app.DataPreparation.getIconByProvider
import com.altcraft.altcraftmobile.functions.app.DataPreparation.getModuleStatusByProvider
import com.altcraft.altcraftmobile.functions.app.DataPreparation.getTokenCardTextByProvider
import com.altcraft.altcraftmobile.functions.app.Formatter.getLastTenCharacters
import com.altcraft.altcraftmobile.functions.app.Formatter.getUserNameForInfo
import com.altcraft.altcraftmobile.functions.sdk.SDKFunctions.updateProvider
import com.altcraft.sdk.data.Constants.FCM_PROVIDER
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.TextStyle
import com.altcraft.altcraftmobile.R
import com.altcraft.altcraftmobile.data.AppConstants.SUBSCRIBED
import com.altcraft.altcraftmobile.data.AppConstants.UNSUBSCRIBED
import com.altcraft.altcraftmobile.extensions.Extensions.UI.fadeToWhiteEdges

object InfoComponents {

    @Composable
    fun InfoCard(viewModel: MainViewModel) {

        val context = LocalContext.current

        val provider by remember { viewModel.provider }
        val token by remember { viewModel.token }
        val userName by remember { viewModel.userName }
        val updateTime by remember { viewModel.updateTokenTime }
        val tokenValue = getLastTenCharacters(token ?: EMPTY_VALUE_UI)
        val userValue = getUserNameForInfo(userName)

        val tokenCardTitle = getTokenCardTextByProvider(provider ?: "android-firebase")
        val tokenCardIcon = getIconByProvider(provider ?: "android-firebase")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(255.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(
                        topStart = 0.dp,
                        topEnd = 0.dp,
                        bottomStart = 30.dp,
                        bottomEnd = 30.dp
                    ),
                    clip = false
                )
                .clip(
                    RoundedCornerShape(
                        topStart = 0.dp,
                        topEnd = 0.dp,
                        bottomStart = 30.dp,
                        bottomEnd = 30.dp
                    )
                )
                .background(Color.White)
        ) {
            Column {

                Image(
                    painter = painterResource(id = R.drawable.ic_altcraft),
                    contentDescription = "",
                    modifier = Modifier
                        .size(80.dp, 20.dp)
                        .offset(x = 16.dp, y = 16.dp)
                )

                Row(
                    modifier = Modifier.padding(
                        top = 27.dp,
                        start = 10.dp
                    )
                ) {
                    Column(
                        verticalArrangement = Arrangement.Top
                    ) {
                        ModuleCard(
                            moduleName = "fcm",
                            imgIcon = R.drawable.ic_fcm,
                            imgSize = 19.dp,
                            status = getModuleStatusByProvider(
                                FCM_PROVIDER,
                                provider
                            ),
                            onClick = {
                                updateProvider(
                                    context, viewModel, listOf(
                                        FCM_PROVIDER,
                                        HMS_PROVIDER,
                                        RUSTORE_PROVIDER
                                    )
                                )
                            }
                        )

                        ModuleCard(
                            moduleName = "hms",
                            imgIcon = R.drawable.ic_hms,
                            imgSize = 25.dp,
                            status = getModuleStatusByProvider(
                                HMS_PROVIDER,
                                provider
                            ),
                            onClick = {
                                updateProvider(
                                    context, viewModel, listOf(
                                        HMS_PROVIDER,
                                        FCM_PROVIDER,
                                        RUSTORE_PROVIDER
                                    )
                                )
                            }
                        )

                        ModuleCard(
                            moduleName = "rustore",
                            imgIcon = R.drawable.ic_rus,
                            imgSize = 22.dp,
                            status = getModuleStatusByProvider(
                                RUSTORE_PROVIDER,
                                provider
                            ),
                            onClick = {
                                updateProvider(
                                    context, viewModel, listOf(
                                        RUSTORE_PROVIDER,
                                        FCM_PROVIDER,
                                        HMS_PROVIDER
                                    )
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fadeToWhiteEdges(isVertical = false)
                    ) {
                        items(
                            listOf(
                                Triple(tokenCardTitle, tokenValue, tokenCardIcon),
                                Triple("update time:", updateTime, R.drawable.ic_time_icon),
                                Triple("user:", userValue, R.drawable.ic_info_setting2)
                            ),
                            key = { it.hashCode() }
                        ) { triple ->
                            val (title, body, icon) = triple
                            InfoItemCard(
                                cardTitle = title,
                                cardBody = body,
                                imgIcon = icon
                            )
                        }
                    }
                }
            }
// qa test ui ------------------------------------------------------------------------
            Text(
                text = "QA",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .offset(x = (-50).dp, y = (-3).dp),
                style = TextStyle(
                    color = Color.Black,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            TestSwitch(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .offset(x = 25.dp, y = 20.dp),
                viewModel
            )
// qa test ui ------------------------------------------------------------------------
        }
    }

    @Composable
    fun ModuleCard(
        moduleName: String?,
        imgIcon: Int? = null,
        imgSize: Dp = 20.dp,
        status: String? = "off",
        onClick: () -> Unit,
        cardWidth: Dp = 90.dp,
        cardHeight: Dp = 66.dp,
        cardCornerRadius: Dp = 15.dp,
        cardElevation: Dp = 3.dp,
        paddingTop: Dp = 7.dp,
        paddingStart: Dp = 3.dp,
        iconCircleSize: Dp = 37.dp,
        iconOffsetX: Dp = 5.dp,
        iconOffsetY: Dp = 5.dp,
        spacingBetweenIconAndText: Dp = 10.dp,
        textOffsetY: Dp = 10.dp,
        textSpacingBelow: Dp = 10.dp,
        statusSpacing: Dp = 10.dp,
        statusIndicatorRadius: Dp = 7.dp,
        textFontSize: TextUnit = 9.sp
    ) {
        Box(
            modifier = Modifier
                .width(cardWidth)
                .height(cardHeight)
                .padding(top = paddingTop, start = paddingStart)
                .shadow(
                    elevation = cardElevation,
                    shape = RoundedCornerShape(cardCornerRadius),
                    clip = false
                )
                .clip(RoundedCornerShape(cardCornerRadius))
                .background(Color.White)
                .clickable(onClick = onClick)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.offset(x = iconOffsetX, y = iconOffsetY)) {
                    Box(
                        modifier = Modifier
                            .size(iconCircleSize)
                            .background(Color.Black, shape = CircleShape)
                    ) {
                        if (imgIcon != null) {
                            Image(
                                painter = painterResource(id = imgIcon),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(imgSize)
                                    .align(Alignment.Center)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(spacingBetweenIconAndText))

                    Column {
                        Text(
                            modifier = Modifier.offset(y = textOffsetY),
                            text = moduleName ?: "",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = textFontSize
                            ),
                            color = Color.Black
                        )

                        Spacer(modifier = Modifier.height(textSpacingBelow))

                        Row {
                            Text(
                                modifier = Modifier.offset(y = 7.dp),
                                text = status ?: "",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = textFontSize
                                ),
                                color = Color.Black
                            )

                            Spacer(modifier = Modifier.width(statusSpacing))

                            StatusComponents.StatusIndicator(
                                radius = statusIndicatorRadius,
                                status = if (status == "off") UNSUBSCRIBED else SUBSCRIBED,
                                modifier = Modifier.offset(y = 7.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun InfoItemCard(
        cardTitle: String?,
        cardBody: String? = "off",
        imgIcon: Int? = null,
    ) {
        Box(
            modifier = Modifier
                .width(137.dp)
                .height(135.dp)
                .padding(
                    top = 7.dp,
                    start = 8.dp,
                    end = 7.dp
                )
                .shadow(
                    elevation = 5.dp,
                    shape = RoundedCornerShape(15.dp),
                    clip = false
                )
                .clip(RoundedCornerShape(15.dp))
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier
                        .padding(start = 5.dp, top = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(35.dp)
                            .background(Color.Black, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        imgIcon?.let {
                            Image(
                                painter = painterResource(id = it),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = cardTitle ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 11.sp
                        ),
                        color = Color.Black
                    )
                }


                Spacer(modifier = Modifier.height(5.dp))

                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cardBody ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.W600,
                            fontSize = 12.sp
                        ),
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.offset(x = 2.dp, y = (-10).dp)
                    )
                }
            }
        }
    }

    @Suppress("unused")
    @Composable
    fun TestSwitch(modifier: Modifier = Modifier, viewModel: MainViewModel) {

        val context = LocalContext.current
        val isChecked = remember { mutableStateOf(false) }
        val switchThumbColor = if (isChecked.value) Color(0xFF33FF5F) else Color.Black
        val trackColor = Color.White

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(35.dp)
                    .padding(
                        top = 7.dp,
                        start = 8.dp,
                        end = 7.dp
                    )
                    .shadow(
                        elevation = 5.dp,
                        shape = RoundedCornerShape(50.dp),
                        clip = false
                    )
                    .clip(RoundedCornerShape(50.dp))
                    .background(trackColor)
                    .clickable { isChecked.value = !isChecked.value }
            ) {
                Box(
                    modifier = Modifier
                        .size(25.dp)
                        .padding(5.dp)
                        .background(switchThumbColor, CircleShape)
                        .align(if (isChecked.value) Alignment.CenterEnd else Alignment.CenterStart)
                )
            }
        }
// qa test ui --------------------------------------------------------------------------------------
//        LaunchedEffect(isChecked.value) {
//            if (isChecked.value) {
//                AltcraftTest.startTests(context, viewModel)
//            } else {
//                AltcraftTest.stopTests()
//            }
//        }
// qa test ui --------------------------------------------------------------------------------------
    }
}