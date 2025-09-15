package com.altcraft.altcraftmobile.view.home

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altcraft.altcraftmobile.viewmodel.MainViewModel
import com.altcraft.altcraftmobile.functions.sdk.SDKFunctions.Login.logIn
import com.altcraft.altcraftmobile.functions.sdk.SDKFunctions.Login.logOut
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import com.altcraft.altcraftmobile.data.AppConstants.UNSUBSCRIBED
import com.altcraft.altcraftmobile.data.AppPreferenses.clearSubscriptionStatus
import com.altcraft.altcraftmobile.data.AppPreferenses.setAuthStatus
import com.altcraft.altcraftmobile.functions.sdk.SDKFunctions.Subscribe.subscribe
import com.altcraft.altcraftmobile.functions.sdk.SDKFunctions.Subscribe.unSubscribe
import com.altcraft.sdk.AltcraftSDK

object ActionsComponents {

    @Composable
    fun ActionButtonRow(viewModel: MainViewModel) {
        val context = LocalContext.current

        val size = 15.dp
        val height = 3.dp
        val strokeWidth = 2.5.dp

        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
        ) {
            ActionButtonItem(
                customIcon = {
                    PlusIcon(
                        modifier = Modifier.size(size),
                        lineWidth = size,
                        lineHeight = height
                    )
                },
                label = "Subscribe to push",
                onClick = {
                    viewModel.profileActions.value = false
                    viewModel.subscribeActions.value = true
                }
            )

            ActionButtonItem(
                customIcon = {
                    CommonIcon(rotationDegrees = 0f)
                },
                label = "Get profile status",
                onClick = {
                    viewModel.subscribeActions.value = false
                    viewModel.profileActions.value = true
                    viewModel.loadProfileStatus(context)
                }
            )

            ActionButtonItem(
                customIcon = {
                    UShapedIcon(
                        modifier = Modifier.size(size),
                        strokeWidth = strokeWidth
                    )
                },
                label = "Update device token",
                onClick = {
                    viewModel.subscribeActions.value = false
                    viewModel.profileActions.value = false
                    AltcraftSDK.pushTokenFunctions.forcedTokenUpdate(context) {
                        viewModel.updateTokenUI(context)
                    }
                }
            )

            ActionButtonItem(
                customIcon = {
                    XShapedIcon(
                        modifier = Modifier.size(size),
                        lineWidth = size,
                        lineHeight = height
                    )
                },
                label = "Clear SDK cache",
                onClick = {
                    viewModel.subscribeActions.value = false
                    viewModel.profileActions.value = false
                    viewModel.clearEventsList()
                    AltcraftSDK.clear(context) {
                        clearSubscriptionStatus(context)
                        setAuthStatus(context, status = false)
                        viewModel.status.value = UNSUBSCRIBED
                    }
                }
            )
        }
    }

    @Composable
    fun ActionButtonItem(
        customIcon: @Composable (() -> Unit)? = null,
        label: String,
        onClick: () -> Unit
    ) {
        ActionButton(
            customIcon = customIcon,
            label = label,
            onClick = onClick
        )
    }

    @Composable
    fun ActionButton(
        customIcon: @Composable (() -> Unit)? = null,
        label: String? = null,
        onClick: () -> Unit
    ) {
        val width90dp = 90.dp
        val height8dp = 8.dp
        val size60dp = 60.dp
        val size50dp = 50.dp
        val width70dp = 70.dp
        val paddingTop4dp = 4.dp
        val textSp = 10.sp

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(width90dp)
                .padding(vertical = height8dp)
        ) {
            Box(
                modifier = Modifier
                    .width(size60dp)
                    .height(size60dp)
                    .shadow(
                        elevation = 5.dp,
                        shape = CircleShape,
                        clip = false
                    )
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(size50dp)
                        .height(size50dp)
                        .shadow(
                            elevation = 3.dp,
                            shape = CircleShape,
                            clip = false
                        )
                        .clip(CircleShape)
                        .background(Color.Black)
                        .clickable(onClick = onClick)
                )

                if (customIcon != null) {
                    customIcon()
                }
            }
            if (label != null) {
                Text(
                    text = label,
                    fontSize = textSp,
                    textAlign = TextAlign.Center,
                    color = Color.Black,
                    lineHeight = textSp,
                    modifier = Modifier
                        .padding(top = paddingTop4dp)
                        .width(width70dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    @Composable
    fun PlusIcon(
        modifier: Modifier = Modifier,
        lineWidth: Dp,
        lineHeight: Dp,
        color: Color = Color(0xFF33FF5F)
    ) {
        Canvas(modifier = modifier.size(lineWidth)) {
            val width = size.width
            val height = size.height
            val lineThickness = lineHeight.toPx()

            drawRoundRect(
                color = color,
                size = Size(width, lineThickness),
                topLeft = Offset(0f, height / 2 - lineThickness / 2),
                cornerRadius = CornerRadius(5f, 5f)
            )

            rotate(90f) {
                drawRoundRect(
                    color = color,
                    size = Size(width, lineThickness),
                    topLeft = Offset(0f, height / 2 - lineThickness / 2),
                    cornerRadius = CornerRadius(5f, 5f)
                )
            }
        }
    }

    @Composable
    fun CommonIcon(
        modifier: Modifier = Modifier,
        rotationDegrees: Float = 0f,
        circleSize: Dp = 7.dp,
        circleColor: Color = Color.White,
        checkMarkColor: Color = Color(0xFF8DBFFC)
    ) {
        val iconSize = 17.dp

        Box(
            modifier = modifier
                .size(iconSize)
                .graphicsLayer(rotationZ = rotationDegrees)
                .wrapContentSize(Alignment.TopCenter)
        ) {
            CustomCircle(
                modifier = Modifier.align(Alignment.TopCenter),
                size = circleSize,
                color = circleColor
            )
            CheckMarkIcon(
                modifier = Modifier.fillMaxSize(),
                strokeColor = checkMarkColor
            )
        }
    }

    @Composable
    fun CustomCircle(
        modifier: Modifier = Modifier,
        color: Color = Color.White,
        size: Dp = 7.dp
    ) {
        Box(
            modifier = modifier
                .size(size)
                .background(
                    color = color,
                    shape = CircleShape
                )
        )
    }

    @Composable
    fun CheckMarkIcon(
        modifier: Modifier = Modifier,
        strokeWidth: Dp = 3.dp,
        strokeColor: Color = Color(0xFF8DBFFC)
    ) {
        Canvas(modifier = modifier) {
            val width = size.width
            val height = size.height
            val strokePx = strokeWidth.toPx()

            val bottom = Offset(width / 2f, height * 0.9f)
            val verticalOffset = height * 0.65f
            val left = Offset(width * 0.15f, verticalOffset)
            val right = Offset(width * 0.85f, verticalOffset)

            drawLine(
                color = strokeColor,
                start = left,
                end = bottom,
                strokeWidth = strokePx,
                cap = StrokeCap.Round
            )

            drawLine(
                color = strokeColor,
                start = right,
                end = bottom,
                strokeWidth = strokePx,
                cap = StrokeCap.Round
            )
        }
    }

    @Composable
    fun UShapedIcon(
        modifier: Modifier = Modifier,
        strokeWidth: Dp = 8.dp,
        bottomArcColor: Color = Color(0xFF8DBFFC),
        topArcColor: Color = Color.White
    ) {
        Canvas(modifier = modifier) {
            val size = size.minDimension
            val strokeWidthPx = strokeWidth.toPx()

            drawArc(
                color = bottomArcColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                style = Stroke(strokeWidthPx),
                size = Size(size, size),
                topLeft = Offset(0f, 0f)
            )

            drawArc(
                color = topArcColor,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                style = Stroke(strokeWidthPx),
                size = Size(size, size),
                topLeft = Offset(0f, 0f)
            )
        }
    }

    @Composable
    fun XShapedIcon(
        modifier: Modifier = Modifier,
        lineWidth: Dp,
        lineHeight: Dp,
        firstColor: Color = Color(0xFF8DBFFC),
        secondColor: Color = Color.White
    ) {
        Canvas(
            modifier = modifier.size(lineWidth)
        ) {
            val width = size.width
            val height = size.height
            val lineThickness = lineHeight.toPx()

            rotate(-45f) {
                drawRoundRect(
                    color = firstColor,
                    size = Size(width, lineThickness),
                    topLeft = Offset(0f, height / 2 - lineThickness / 2),
                    cornerRadius = CornerRadius(5f, 5f)
                )
            }

            rotate(45f) {
                drawRoundRect(
                    color = secondColor,
                    size = Size(width, lineThickness),
                    topLeft = Offset(0f, height / 2 - lineThickness / 2),
                    cornerRadius = CornerRadius(5f, 5f)
                )
            }
        }
    }

    @Composable
    fun SubBox(
        viewModel: MainViewModel,
        modifier: Modifier = Modifier
    ) {
        val context = LocalContext.current
        val corner = 10.dp
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .padding(end = 2.5.dp)
                            .shadow(
                                elevation = 3.dp,
                                shape = RoundedCornerShape(corner),
                                clip = false
                            )
                            .clip(RoundedCornerShape(corner))
                            .background(Color.White)
                            .clickable {
                                subscribe(context)
                                viewModel.subscribeActions.value = false
                                viewModel.profileActions.value = false
                            }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(35.dp)
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(Color(0xFF33FF5F).copy(alpha = 0.1f))
                            ) {
                                PlusIcon(
                                    modifier = Modifier
                                        .size(15.dp)
                                        .align(Alignment.Center),
                                    lineWidth = 15.dp,
                                    lineHeight = 3.dp,
                                    color = Color.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Subscribe",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Black,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .padding(horizontal = 2.5.dp)
                            .shadow(
                                elevation = 3.dp,
                                shape = RoundedCornerShape(corner),
                                clip = false
                            )
                            .clip(RoundedCornerShape(corner))
                            .background(Color.White)
                            .clickable {
                                AltcraftSDK.pushSubscriptionFunctions.pushSuspend(context)
                                viewModel.subscribeActions.value = false
                                viewModel.profileActions.value = false
                            }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(35.dp)
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(Color.Black.copy(alpha = 0.1f))
                            ) {
                                UShapedIcon(
                                    modifier = Modifier
                                        .size(15.dp)
                                        .align(Alignment.Center),
                                    strokeWidth = 3.dp,
                                    bottomArcColor = Color.Black,
                                    topArcColor = Color.Black,
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Suspend",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Black,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Unsubscribe Button (1/3 width)
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .padding(start = 2.5.dp)
                            .shadow(
                                elevation = 3.dp,
                                shape = RoundedCornerShape(corner),
                                clip = false
                            )
                            .clip(RoundedCornerShape(corner))
                            .background(Color.White)
                            .clickable {
                                unSubscribe(context)
                                viewModel.subscribeActions.value = false
                                viewModel.profileActions.value = false
                            }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(35.dp)
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(Color.Black.copy(alpha = 0.1f))
                            ) {
                                XShapedIcon(
                                    modifier = Modifier
                                        .size(15.dp)
                                        .align(Alignment.Center),
                                    lineWidth = 15.dp,
                                    lineHeight = 3.dp,
                                    firstColor = Color.Black,
                                    secondColor = Color.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Unsubscribe",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Black,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Log In/Out Buttons (fixed width)
                Column(
                    modifier = Modifier.width(88.5.dp)
                ) {
                    // Log In
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .padding(
                                start = 2.5.dp, end = 2.5.dp, bottom = 5.dp
                            )
                            .shadow(
                                elevation = 3.dp,
                                shape = RoundedCornerShape(corner),
                                clip = false
                            )
                            .clip(RoundedCornerShape(corner))
                            .background(Color.White)
                            .clickable {
                                logIn(context)
                                viewModel.subscribeActions.value = false
                                viewModel.profileActions.value = false
                            }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(23.dp)
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(Color.Black.copy(alpha = 0.7f))
                            ) {
                                CommonIcon(
                                    rotationDegrees = -90f,
                                    modifier = Modifier
                                        .zIndex(1f)
                                        .align(Alignment.Center),
                                    circleColor = Color(0xFF8DBFFC).copy(alpha = 0f),
                                    checkMarkColor = Color.White,
                                    circleSize = 5.dp
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Log In",
                                fontSize = 10.sp,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Black
                            )
                        }
                    }

                    // Log Out
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .padding(horizontal = 2.5.dp)
                            .shadow(
                                elevation = 3.dp,
                                shape = RoundedCornerShape(corner),
                                clip = false
                            )
                            .clip(RoundedCornerShape(corner))
                            .background(Color.White)
                            .clickable {
                                logOut(context = context)
                                viewModel.subscribeActions.value = false
                                viewModel.profileActions.value = false
                            }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(23.dp)
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(Color.Black.copy(alpha = 0.7f))
                            ) {
                                CommonIcon(
                                    rotationDegrees = 90f,
                                    modifier = Modifier
                                        .zIndex(1f)
                                        .align(Alignment.Center),
                                    circleColor = Color(0xFF8DBFFC).copy(alpha = 0f),
                                    checkMarkColor = Color.White,
                                    circleSize = 5.dp
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Log Out",
                                fontSize = 10.sp,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Black
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(15.dp))

            SectionInfo(
                action = "sub",
                onClick = {
                    viewModel.subscribeActions.value = false
                    viewModel.profileActions.value = false
                }
            )

            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    @Composable
    fun SectionInfo(
        action: String? = null,
        boxHeight: Dp = 25.dp,
        onClick: () -> Unit = {}
    ) {
        val backgroundColor = when (action) {
            "sub" -> Color(0xFFADF7BD).copy(alpha = 0.2f)
            "profile" -> Color(0xFF8DBFFC).copy(alpha = 0.2f)
            else -> Color(0xFF8DBFFC).copy(alpha = 0.2f)
        }

        val displayText = when (action) {
            "sub" -> "Managing a push subscription. Click to close"
            "profile" -> "Profile information. Click to close"
            else -> "Send push"
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(boxHeight)
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(backgroundColor)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayText,
                color = Color.Gray,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}