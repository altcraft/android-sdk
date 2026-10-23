package com.altcraft.altcraftmobile.view.home

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.inapp.inApp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altcraft.altcraftmobile.R
import com.altcraft.altcraftmobile.data.AppConstants.EMPTY_VALUE_UI
import com.altcraft.altcraftmobile.data.AppConstants.HMS_PROVIDER
import com.altcraft.altcraftmobile.data.AppConstants.RUSTORE_PROVIDER
import com.altcraft.altcraftmobile.data.AppConstants.SUBSCRIBED
import com.altcraft.altcraftmobile.data.AppConstants.UNSUBSCRIBED
import com.altcraft.altcraftmobile.extensions.Extensions.UI.fadeToWhiteEdges
import com.altcraft.altcraftmobile.functions.app.DataPreparation.getIconByProvider
import com.altcraft.altcraftmobile.functions.app.DataPreparation.getModuleStatusByProvider
import com.altcraft.altcraftmobile.functions.app.DataPreparation.getTokenCardTextByProvider
import com.altcraft.altcraftmobile.functions.app.Formatter.getLastTenCharacters
import com.altcraft.altcraftmobile.functions.app.Formatter.getUserNameForInfo
import com.altcraft.altcraftmobile.functions.sdk.SDKFunctions.updateProvider
import com.altcraft.altcraftmobile.viewmodel.MainViewModel
import com.altcraft.sdk.AltcraftSDK
import com.altcraft.sdk.push.data.Constants.FCM_PROVIDER
import com.altcraft.sdk.push.push

object InfoComponents {

    private const val HOME_INAPP_SUBSCRIPTION_BUTTON =
        "home_inapp_subscription_button"

    private const val HOME_AUTH_BUTTON =
        "home_auth_button"

    @Composable
    fun InfoCard(viewModel: MainViewModel) {
        val context = LocalContext.current

        val provider by remember { viewModel.provider }
        val token by remember { viewModel.token }
        val userName by remember { viewModel.userName }
        val updateTime by remember { viewModel.updateTokenTime }
        val subscriptionStatus by remember { viewModel.status }
        val tokenValue = getLastTenCharacters(token ?: EMPTY_VALUE_UI)
        val userValue = getUserNameForInfo(userName)

        val pushSubscribed = subscriptionStatus == SUBSCRIBED
        val inAppSubscribed = viewModel.inAppStatus.value == SUBSCRIBED
        val isAuthenticated = viewModel.authStatus.value

        val tokenCardTitle = getTokenCardTextByProvider(provider ?: "android-firebase")
        val tokenCardIcon = getIconByProvider(provider ?: "android-firebase")

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
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
                            status = getModuleStatusByProvider(FCM_PROVIDER, provider),
                            onClick = {
                                updateProvider(
                                    context,
                                    viewModel,
                                    listOf(FCM_PROVIDER, HMS_PROVIDER, RUSTORE_PROVIDER)
                                )
                            }
                        )

                        ModuleCard(
                            moduleName = "hms",
                            imgIcon = R.drawable.ic_hms,
                            imgSize = 25.dp,
                            status = getModuleStatusByProvider(HMS_PROVIDER, provider),
                            onClick = {
                                updateProvider(
                                    context,
                                    viewModel,
                                    listOf(HMS_PROVIDER, FCM_PROVIDER, RUSTORE_PROVIDER)
                                )
                            }
                        )

                        ModuleCard(
                            moduleName = "rustore",
                            imgIcon = R.drawable.ic_rus,
                            imgSize = 22.dp,
                            status = getModuleStatusByProvider(RUSTORE_PROVIDER, provider),
                            onClick = {
                                updateProvider(
                                    context,
                                    viewModel,
                                    listOf(RUSTORE_PROVIDER, FCM_PROVIDER, HMS_PROVIDER)
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fadeToWhiteEdges(isVertical = false)
                        ) {
                            items(
                                listOf(
                                    InfoCardData(tokenCardTitle, tokenValue, tokenCardIcon),
                                    InfoCardData("platform:", "Android", R.drawable.ic_os, 28.dp),
                                    InfoCardData("update time:", updateTime, R.drawable.ic_time_icon),
                                    InfoCardData("version:", "2.0.0_beta", R.drawable.ic_info_setting2)
                                ),
                                key = { it.hashCode() }
                            ) { card ->
                                InfoItemCard(
                                    cardTitle = card.title,
                                    cardBody = card.body,
                                    imgIcon = card.icon,
                                    iconSize = card.iconSize
                                )
                            }
                        }

                        Column {
                            StatusCardsLayout(
                                status = true,
                                paddingStart = 7.dp,
                                paddingTop = 3.dp,
                                paddingEnd = 5.dp,
                                paddingBottom = 0.dp,
                                first = StatusCardConfig(
                                    imgSize = 24.dp,
                                    alphaImage = 0.5f,
                                    status = inAppSubscribed,
                                    accessibilityId = HOME_INAPP_SUBSCRIPTION_BUTTON,
                                    onClick = {
                                        if (inAppSubscribed) {
                                            AltcraftSDK.inApp.subscription.unsubscribe(context)
                                        } else {
                                            AltcraftSDK.inApp.subscription.subscribe(context)
                                        }
                                    },
                                    content = { status ->
                                        InAppStatus(status)
                                    }
                                ),
                                second = StatusCardConfig(
                                    imgSize = 22.dp,
                                    alphaImage = 0.7f,
                                    status = pushSubscribed,
                                    onClick = {
                                        if (pushSubscribed) {
                                            AltcraftSDK.push.subscription.unsubscribe(context)
                                        } else {
                                            AltcraftSDK.push.subscription.subscribe(context)
                                        }
                                    },
                                    content = { status ->
                                        PushStatus(status)
                                    }
                                ),
                                third = StatusCardConfig(
                                    imgSize = 25.dp,
                                    alphaImage = 1f,
                                    status = isAuthenticated,
                                    accessibilityId = HOME_AUTH_BUTTON,
                                    onClick = {
                                        if (isAuthenticated) {
                                            AltcraftSDK.auth.logOut(context)
                                        } else {
                                            AltcraftSDK.auth.authenticate(context)
                                        }
                                    },
                                    content = { status ->
                                        AuthStatus(status)
                                    }
                                ),
                                fourth = StatusCardConfig(
                                    imgIcon = R.drawable.ic_github,
                                    imgSize = 20.dp,
                                    url = "https://github.com/altcraft/android-sdk"
                                )
                            )

                            StatusCardsLayout(
                                status = false,
                                paddingStart = 7.dp,
                                paddingTop = 3.dp,
                                paddingEnd = 5.dp,
                                paddingBottom = 0.dp,
                                first = StatusCardConfig(alpha = 0f),
                                second = StatusCardConfig(alpha = 0f),
                                third = StatusCardConfig(alpha = 0f),
                                fourth = StatusCardConfig(
                                    imgIcon = R.drawable.ic_altcraft_push_icon,
                                    imgSize = 27.dp,
                                    url = "https://guides.altcraft.com/developer-guide/sdk/mobile/android/"
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    data class StatusCardConfig(
        val imgIcon: Int? = null,
        val imgSize: Dp = 20.dp,
        val status: Boolean = false,
        val alpha: Float = 1f,
        val alphaImage: Float = 1f,
        val elevation: Dp = 2.dp,
        val url: String? = null,
        val accessibilityId: String? = null,
        val onClick: (() -> Unit)? = null,
        val content: (@Composable BoxScope.(status: Boolean) -> Unit)? = null
    )

    data class InfoCardData(
        val title: String?,
        val body: String?,
        val icon: Int,
        val iconSize: Dp = 20.dp
    )

    @Composable
    fun StatusCard(
        status: Boolean,
        config: StatusCardConfig,
        weight: Dp = 90.dp,
        height: Dp = 66.dp,
        modifier: Modifier = Modifier
    ) {
        val uriHandler = LocalUriHandler.current

        val clickModifier = when {
            config.onClick != null -> {
                Modifier.clickable {
                    config.onClick.invoke()
                }
            }

            config.url != null -> {
                Modifier.clickable {
                    uriHandler.openUri(config.url)
                }
            }

            else -> Modifier
        }

        Box(
            modifier = modifier
                .width(weight)
                .height(height)
                .alpha(config.alpha)
                .padding(3.dp)
                .shadow(
                    elevation = config.elevation,
                    shape = RoundedCornerShape(15.dp),
                    clip = false
                )
                .clip(RoundedCornerShape(15.dp))
                .background(Color.White)
                .then(
                    if (config.accessibilityId != null) {
                        Modifier.semantics {
                            contentDescription = config.accessibilityId
                        }
                    } else {
                        Modifier
                    }
                )
                .then(clickModifier),
            contentAlignment = Alignment.Center
        ) {
            if (config.content != null) {
                config.content.invoke(this, config.status)
            } else if (config.imgIcon != null) {
                Image(
                    painter = painterResource(id = config.imgIcon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(config.imgSize)
                        .alpha(config.alphaImage)
                )
            }
        }
    }

    @Composable
    fun AuthStatus(
        status: Boolean,
        size: Dp = 25.dp,
        backgroundColor: Color = Color(0xFFADF7BD).copy(alpha = 0.5f),
        iconAlpha: Float = 1f
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            if (status) {
                Box(
                    modifier = Modifier
                        .size(15.dp)
                        .background(backgroundColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                }
            }

            Image(
                painter = painterResource(id = R.drawable.ic_fingerprint),
                contentDescription = null,
                modifier = Modifier
                    .size(size)
                    .alpha(iconAlpha)
            )
        }
    }

    @Composable
    fun PushStatus(
        status: Boolean,
        size: Dp = 20.dp,
        iconAlpha: Float = 0.7f
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_bell_3),
                contentDescription = null,
                modifier = Modifier
                    .size(size)
                    .alpha(iconAlpha)
            )

            if (status) {
                OnBadge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 0.dp, y = (-2).dp),
                    size = 11.dp
                )
            }
        }
    }

    @Composable
    fun InAppStatus(
        status: Boolean,
        size: Dp = 22.dp,
        iconAlpha: Float = 0.5f
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_menu_example),
                contentDescription = null,
                modifier = Modifier
                    .size(size)
                    .alpha(iconAlpha)
            )

            if (status) {
                OnBadge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 0.dp, y = (-2).dp),
                    size = 11.dp
                )
            }
        }
    }

    @Composable
    fun OnBadge(
        modifier: Modifier = Modifier,
        size: Dp
    ) {
        Box(
            modifier = modifier
                .size(size + 2.dp)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(size)
                    .background(Color.Black, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_check),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier.size(size * 0.55f)
                )
            }
        }
    }

    @Composable
    fun StatusCardsLayout(
        status: Boolean,
        paddingStart: Dp = 0.dp,
        paddingTop: Dp = 0.dp,
        paddingEnd: Dp = 0.dp,
        paddingBottom: Dp = 0.dp,
        first: StatusCardConfig,
        second: StatusCardConfig,
        third: StatusCardConfig,
        fourth: StatusCardConfig
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(65.dp)
                .padding(
                    start = paddingStart,
                    top = paddingTop,
                    end = paddingEnd,
                    bottom = paddingBottom
                )
        ) {
            Row(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
            ) {
                StatusCard(
                    status,
                    first,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )

                StatusCard(
                    status,
                    second,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            Row(
                modifier = Modifier
                    .weight(1.85f)
                    .fillMaxHeight()
            ) {
                StatusCard(
                    status,
                    third,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )

                StatusCard(
                    status,
                    fourth,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
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
        iconSize: Dp = 20.dp
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
                    modifier = Modifier.padding(start = 5.dp, top = 5.dp),
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
                                modifier = Modifier.size(iconSize)
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
                    modifier = Modifier.fillMaxSize(),
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
}