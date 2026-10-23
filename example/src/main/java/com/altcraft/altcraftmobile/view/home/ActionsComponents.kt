package com.altcraft.altcraftmobile.view.home

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.inapp.inApp

import com.altcraft.altcraftmobile.R
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.altcraft.altcraftmobile.data.AppConstants.UNSUBSCRIBED
import com.altcraft.altcraftmobile.data.AppPreferenses.clearSubscriptionStatus
import com.altcraft.altcraftmobile.data.AppPreferenses.setExternalAuthStatus
import com.altcraft.altcraftmobile.functions.sdk.SDKFunctions.Login.logIn
import com.altcraft.altcraftmobile.functions.sdk.SDKFunctions.Login.logOut
import com.altcraft.altcraftmobile.functions.sdk.SDKFunctions.Subscribe.subscribe
import com.altcraft.altcraftmobile.functions.sdk.SDKFunctions.Subscribe.unSubscribe
import com.altcraft.altcraftmobile.viewmodel.MainViewModel
import com.altcraft.sdk.AltcraftSDK
import com.altcraft.sdk.target.target

object ActionsComponents {

    private const val HOME_CLEAR_CONFIG_BUTTON = "home_clear_config_button"

    @Composable
    fun ActionButtonRow(viewModel: MainViewModel) {
        val context = LocalContext.current

        val size = 15.dp
        val height = 3.dp
        val strokeWidth = 2.5.dp

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(0f)
            ) {
            }

            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(1f)
            ) {
                ActionButtonItem(
                    customIcon = {
                        PlusIcon(
                            modifier = Modifier.size(size),
                            lineWidth = size,
                            lineHeight = height
                        )
                    },
                    label = "Subscribe",
                    onClick = {
                        val wasOpened = viewModel.subscribeActions.value

                        viewModel.profileActions.value = false
                        viewModel.targetActions.value = false
                        viewModel.subscribeActions.value = !wasOpened
                    }
                )

                ActionButtonItem(
                    customIcon = {
                        CommonIcon(rotationDegrees = 0f)
                    },
                    label = "Profile",
                    onClick = {
                        val wasOpened = viewModel.profileActions.value

                        viewModel.subscribeActions.value = false
                        viewModel.targetActions.value = false
                        viewModel.profileActions.value = !wasOpened

                        if (!wasOpened) {
                            viewModel.loadProfileStatus(context)
                        }
                    }
                )

                ActionButtonItem(
                    customIcon = {
                        UShapedIcon(
                            modifier = Modifier.size(size),
                            strokeWidth = strokeWidth
                        )
                    },
                    label = "Target",
                    onClick = {
                        val wasOpened = viewModel.targetActions.value

                        viewModel.subscribeActions.value = false
                        viewModel.profileActions.value = false
                        viewModel.targetActions.value = !wasOpened
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
                    label = "Clear",
                    accessibilityId = HOME_CLEAR_CONFIG_BUTTON,
                    onClick = {
                        viewModel.subscribeActions.value = false
                        viewModel.profileActions.value = false
                        viewModel.targetActions.value = false
                        viewModel.clearEventsList()

                        AltcraftSDK.clear(context) {
                            clearSubscriptionStatus(context)
                            setExternalAuthStatus(context, status = false)
                            viewModel.status.value = UNSUBSCRIBED
                        }
                    }
                )
            }
        }
    }

    @Composable
    fun ActionButtonItem(
        customIcon: @Composable (() -> Unit)? = null,
        label: String,
        accessibilityId: String? = null,
        onClick: () -> Unit
    ) {
        ActionButton(
            customIcon = customIcon,
            label = label,
            accessibilityId = accessibilityId,
            onClick = onClick
        )
    }

    @Composable
    fun ActionButton(
        customIcon: @Composable (() -> Unit)? = null,
        label: String? = null,
        accessibilityId: String? = null,
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
                    .then(
                        if (accessibilityId != null) {
                            Modifier.semantics {
                                contentDescription = accessibilityId
                            }
                        } else {
                            Modifier
                        }
                    )
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
                    fontWeight = FontWeight.Normal,
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
            val iconSize = size.minDimension
            val strokeWidthPx = strokeWidth.toPx()

            drawArc(
                color = bottomArcColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                style = Stroke(strokeWidthPx),
                size = Size(iconSize, iconSize),
                topLeft = Offset(0f, 0f)
            )

            drawArc(
                color = topArcColor,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                style = Stroke(strokeWidthPx),
                size = Size(iconSize, iconSize),
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

        fun closeActions() {
            viewModel.subscribeActions.value = false
            viewModel.profileActions.value = false
            viewModel.targetActions.value = false
        }

        @Composable
        fun VerticalLabel(
            text: String,
            color: Color,
            modifier: Modifier = Modifier
        ) {
            Column(
                modifier = modifier
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(corner))
                    .background(color.copy(alpha = 0.1f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                text.forEach { char ->
                    Text(
                        text = char.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Black,
                        fontSize = 10.sp,
                        lineHeight = 10.sp
                    )
                }
            }
        }

        @Composable
        fun SubActionButton(
            text: String,
            modifier: Modifier = Modifier,
            iconSize: Dp = 20.dp,
            textSize: TextUnit = 8.sp,
            iconBackground: Color = Color.Black.copy(alpha = 0.1f),
            onClick: () -> Unit,
            icon: @Composable () -> Unit
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.5.dp, vertical = 2.dp)
                    .shadow(
                        elevation = 3.dp,
                        shape = RoundedCornerShape(corner),
                        clip = false
                    )
                    .clip(RoundedCornerShape(corner))
                    .background(Color.White)
                    .clickable {
                        onClick()
                    }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(iconSize)
                            .clip(RoundedCornerShape(30.dp))
                            .background(iconBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        icon()
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Black,
                        fontSize = textSize,
                        maxLines = 1
                    )
                }
            }
        }

        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .padding(horizontal = 10.dp)
            ) {
                VerticalLabel(
                    text = "push",
                    color = Color(0xFF33FF5F),
                    modifier = Modifier
                        .weight(0.1f)
                        .padding(end = 2.5.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(0.2f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    SubActionButton(
                        modifier = Modifier.weight(1f),
                        text = "Subscribe",
                        iconBackground = Color(0xFF33FF5F).copy(alpha = 0.1f),
                        onClick = {
                            subscribe(context)
                            closeActions()
                        },
                        icon = {
                            PlusIcon(
                                modifier = Modifier.size(10.dp),
                                lineWidth = 10.dp,
                                lineHeight = 2.dp,
                                color = Color.Black
                            )
                        }
                    )

                    SubActionButton(
                        modifier = Modifier.weight(1f),
                        text = "Unsubscribe",
                        onClick = {
                            unSubscribe(context)
                            closeActions()
                        },
                        icon = {
                            XShapedIcon(
                                modifier = Modifier.size(10.dp),
                                lineWidth = 10.dp,
                                lineHeight = 2.dp,
                                firstColor = Color.Black,
                                secondColor = Color.Black
                            )
                        }
                    )
                }

                VerticalLabel(
                    text = "inapp",
                    color = Color(0xFF8DBFFC),
                    modifier = Modifier
                        .weight(0.1f)
                        .padding(horizontal = 2.5.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(0.2f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    SubActionButton(
                        modifier = Modifier.weight(1f),
                        text = "Subscribe",
                        iconBackground = Color(0xFF8DBFFC).copy(alpha = 0.1f),
                        onClick = {
                            AltcraftSDK.inApp.subscription.subscribe(context)
                            closeActions()
                        },
                        icon = {
                            PlusIcon(
                                modifier = Modifier.size(10.dp),
                                lineWidth = 10.dp,
                                lineHeight = 2.dp,
                                color = Color.Black
                            )
                        }
                    )

                    SubActionButton(
                        modifier = Modifier.weight(1f),
                        text = "Unsubscribe",
                        onClick = {
                            AltcraftSDK.inApp.subscription.unsubscribe(context)
                            closeActions()
                        },
                        icon = {
                            XShapedIcon(
                                modifier = Modifier.size(10.dp),
                                lineWidth = 10.dp,
                                lineHeight = 2.dp,
                                firstColor = Color.Black,
                                secondColor = Color.Black
                            )
                        }
                    )
                }

                Column(
                    modifier = Modifier
                        .width(88.5.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    SubActionButton(
                        modifier = Modifier.weight(1f),
                        text = "Log In",
                        iconSize = 23.dp,
                        textSize = 10.sp,
                        iconBackground = Color.Black.copy(alpha = 0.7f),
                        onClick = {
                            logIn(context)
                            closeActions()
                        },
                        icon = {
                            CommonIcon(
                                rotationDegrees = -90f,
                                modifier = Modifier.zIndex(1f),
                                circleColor = Color(0xFF8DBFFC).copy(alpha = 0f),
                                checkMarkColor = Color.White,
                                circleSize = 5.dp
                            )
                        }
                    )

                    SubActionButton(
                        modifier = Modifier.weight(1f),
                        text = "Log Out",
                        iconSize = 23.dp,
                        textSize = 10.sp,
                        iconBackground = Color.Black.copy(alpha = 0.7f),
                        onClick = {
                            logOut(context = context)
                            closeActions()
                        },
                        icon = {
                            CommonIcon(
                                rotationDegrees = 90f,
                                modifier = Modifier.zIndex(1f),
                                circleColor = Color(0xFF8DBFFC).copy(alpha = 0f),
                                checkMarkColor = Color.White,
                                circleSize = 5.dp
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(15.dp))

            SectionInfo(
                action = "sub",
                startPadding = 12.dp,
                endPadding = 12.dp,
                onClick = {
                    closeActions()
                }
            )

            Spacer(modifier = Modifier.height(10.dp))
        }
    }


    @Composable
    fun MobileEventBox(
        viewModel: MainViewModel,
        modifier: Modifier = Modifier
    ) {
        val context = LocalContext.current
        val corner = 10.dp

        val sid = remember { mutableStateOf("") }
        val eventName = remember { mutableStateOf("") }

        @Composable
        fun MobileEventTextRow(
            name: String,
            state: MutableState<String>
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 15.dp, end = 15.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = name,
                        color = Color.Black,
                        fontSize = 10.sp,
                        modifier = Modifier.width(38.dp)
                    )

                    BasicTextField(
                        value = state.value,
                        onValueChange = { state.value = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color.Black,
                            fontSize = 12.sp,
                            lineHeight = 12.sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, bottom = 1.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Divider(
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                    thickness = 1.dp,
                    color = Color.LightGray.copy(alpha = 0.5f)
                )
            }
        }

        Column(modifier = modifier) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp)
                    .clip(RoundedCornerShape(corner))
                    .background(Color.White)
                    .padding(vertical = 10.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MobileEventTextRow("sid", sid)
                    Spacer(modifier = Modifier.height(7.dp))
                    MobileEventTextRow("name", eventName)
                }
            }

            Spacer(modifier = Modifier.height(15.dp))

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SectionInfo(
                        action = "mobile_event_send",
                        startPadding = 20.dp,
                        endPadding = 2.dp,
                        endImage = R.drawable.ic_mark_2,
                        endImageRotation = 45f,
                        onClick = {
                            AltcraftSDK.target.sendTarget(
                                context = context,
                                sid = sid.value,
                                eventName = eventName.value
                            )

                            viewModel.subscribeActions.value = false
                            viewModel.profileActions.value = false
                            viewModel.targetActions.value = false
                        }
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    SectionInfo(
                        action = "mobile_event_close",
                        endPadding = 20.dp,
                        startPadding = 2.dp,
                        endImage = R.drawable.ic_mark_2,
                        onClick = {
                            viewModel.subscribeActions.value = false
                            viewModel.profileActions.value = false
                            viewModel.targetActions.value = false
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    @Composable
    fun SectionInfo(
        action: String? = null,
        boxHeight: Dp = 25.dp,
        onClick: () -> Unit = {},
        startPadding: Dp = 4.dp,
        endPadding: Dp = 4.dp,
        startImage: Int? = null,
        endImage: Int? = null,
        startImageRotation: Float = 0f,
        endImageRotation: Float = 0f
    ) {
        val backgroundColor = when (action) {
            "sub" -> Color(0xFFADF7BD).copy(alpha = 0.2f)
            "profile" -> Color(0xFF8DBFFC).copy(alpha = 0.2f)
            "mobile_event_send" -> Color(0xFFADF7BD).copy(alpha = 0.2f)
            else -> Color(0xFF8DBFFC).copy(alpha = 0.2f)
        }

        val displayText = when (action) {
            "sub" -> "Managing a push subscription. Click to close"
            "profile" -> "Profile information. Click to close"
            "mobile_event_send" -> "Click to send"
            "mobile_event_close" -> "Click to close"
            else -> "Send push"
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(boxHeight)
                .padding(start = startPadding, end = endPadding)
                .clip(RoundedCornerShape(5.dp))
                .background(backgroundColor)
                .clickable { onClick() }
        ) {
            startImage?.let { image ->
                Image(
                    painter = painterResource(id = image),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.Black.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 25.dp)
                        .size(7.dp)
                        .graphicsLayer {
                            rotationZ = startImageRotation
                        }
                )
            }

            Text(
                modifier = Modifier.align(Alignment.Center),
                text = displayText,
                color = Color.Gray,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            endImage?.let { image ->
                Image(
                    painter = painterResource(id = image),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.Black.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 25.dp)
                        .size(8.dp)
                        .graphicsLayer {
                            rotationZ = endImageRotation
                        }
                )
            }
        }
    }
}