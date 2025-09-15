package com.altcraft.altcraftmobile.view.example

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Divider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.altcraft.altcraftmobile.functions.app.UI.SpacerHeightDp
import com.altcraft.altcraftmobile.view.config.Buttons.AddButton
import com.altcraft.altcraftmobile.view.config.Buttons.RemoveButton
import com.altcraft.altcraftmobile.view.config.EditTextRow.EditTextRow
import com.altcraft.altcraftmobile.view.home.ActionsComponents.XShapedIcon
import com.altcraft.altcraftmobile.view.home.TextComponents
import com.altcraft.altcraftmobile.data.AppPreferenses.getNotificationData
import com.altcraft.altcraftmobile.data.AppPreferenses.removeNotificationData
import com.altcraft.altcraftmobile.data.AppDataClasses
import com.altcraft.altcraftmobile.viewmodel.MainViewModel
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.altcraft.altcraftmobile.R
import java.util.Date

object ExampleComponents {
    @Composable
    fun NotificationCard(
        viewModel: MainViewModel,
        modifier: Modifier = Modifier,
        onButtonClick: (String) -> Unit
    ) {
        val title = viewModel.notificationTitle.value
        val body = viewModel.notificationBody.value
        val smallImg = viewModel.smallImageBitmap.value
        val largeImg = viewModel.largeImageBitmap.value
        val buttons = viewModel.notificationButtons.value

        Box(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 15.dp)
                .shadow(
                    elevation = 3.dp,
                    shape = RoundedCornerShape(25.dp)
                )
                .clip(RoundedCornerShape(25.dp))
                .background(Color.White)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, top = 15.dp, end = 15.dp, bottom = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .width(45.dp)
                            .wrapContentHeight()
                    ) {
                        IconBox()
                    }
                    Row(
                        modifier = Modifier
                            .weight(0.9f)
                            .wrapContentHeight()
                    ) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                ) {
                                    Row {
                                        Text(
                                            text = "AltcraftMSDKv2",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.W600
                                            ),
                                            color = Color.Gray
                                        )

                                        Spacer(modifier = Modifier.width(5.dp))

                                        Text(
                                            text = SimpleDateFormat("HH:mm", Locale.getDefault())
                                                .format(Date()),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.W500
                                            ),
                                            color = Color.Gray
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(3.dp))

                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.W600
                                        ),
                                        color = Color.Black
                                    )
                                }

                                smallImg?.let { bitmap ->
                                    Spacer(modifier = Modifier.width(10.dp))
                                    RemoteImage(
                                        bitmap = bitmap,
                                        largeImg = false,
                                        modifier = Modifier
                                            .padding(end = 15.dp)
                                            .align(Alignment.CenterVertically)
                                    )
                                }

                                SharpAngleShape(
                                    size = 5.dp,
                                    stroke = 2.dp,
                                    rotationDegrees = 45f,
                                    color = Color.Gray,
                                    modifier = Modifier.offset(x = (-5).dp, y = 10.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            Row {
                                Text(
                                    text = body,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.W400
                                    ),
                                    color = Color.Black
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            largeImg?.let {
                                RemoteImage(
                                    it,
                                    largeImg = true,
                                    size = 150.dp,
                                    cornerRadius = 20.dp
                                )
                            }
                        }
                    }
                }

                if (largeImg != null || buttons != null) {
                    Spacer(modifier = Modifier.height(15.dp))

                    NotificationButtonRow(
                        buttons = buttons,
                        modifier = Modifier.offset(y = (-5).dp),
                    )
                }
            }
        }
    }

    @Composable
    private fun IconBox() {
        Box(
            modifier = Modifier
                .size(35.dp)
                .shadow(
                    elevation = 2.dp,
                    shape = CircleShape,
                    clip = true
                )
                .background(Color.White, CircleShape)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_altcraft_label),
                contentDescription = "icon",
                modifier = Modifier.size(19.dp)
            )
        }
    }

    @Composable
    fun RemoteImage(
        bitmap: Bitmap,
        modifier: Modifier = Modifier,
        largeImg: Boolean,
        size: Dp = 30.dp,
        backgroundColor: Color = Color.Transparent,
        cornerRadius: Dp = 5.dp,
        contentScale: ContentScale = ContentScale.FillWidth
    ) {
        if (largeImg) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Large notification image",
                contentScale = contentScale,
                modifier = modifier
                    .clip(RoundedCornerShape(cornerRadius))
            )
        } else {
            val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()

            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "image",
                contentScale = ContentScale.Fit,
                modifier = modifier
                    .height(size)
                    .aspectRatio(aspectRatio)
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(backgroundColor, RoundedCornerShape(cornerRadius))
            )
        }
    }


    @Composable
    fun SharpAngleShape(
        modifier: Modifier = Modifier,
        size: Dp = 5.dp,
        stroke: Dp = 2.dp,
        rotationDegrees: Float = 0f,
        color: Color = Color.Black,
    ) {

        Canvas(
            modifier = modifier
                .size(size * 2)
                .graphicsLayer(rotationZ = rotationDegrees)
        ) {
            val path = Path().apply {
                moveTo(0f, size.toPx())
                lineTo(0f, 0f)
                lineTo(size.toPx(), 0f)
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = stroke.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }

    @Composable
    fun NotificationButtonRow(
        buttons: List<String>?,
        modifier: Modifier = Modifier
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(30.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            buttons?.forEachIndexed { index, label ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        )
                    )
                }

                if (index < buttons.lastIndex) {
                    Divider(
                        modifier = Modifier
                            .height(20.dp)
                            .width(1.dp)
                            .padding(vertical = 4.dp),
                        color = Color.LightGray
                    )
                }
            }
        }
    }


    @Composable
    fun TextSetting(viewModel: MainViewModel) {
        val context = LocalContext.current

        val title = remember { mutableStateOf("") }
        val body = remember { mutableStateOf("") }

        val currentSetting = remember { mutableStateOf(getNotificationData(context)) }

        fun resetTextSetting() {
            removeNotificationData(context)
            currentSetting.value = null
            title.value = ""
            body.value = ""
        }

        val defaultSetting = AppDataClasses.NotificationData.getDefault()
        val titleHint = currentSetting.value?.title ?: defaultSetting.title
        val bodyHint = currentSetting.value?.body ?: defaultSetting.body

        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val shadowHeight = 3.dp.toPx()
                        val horizontalPadding = 15.dp.toPx()

                        val gradient = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF8DBFFC).copy(alpha = 0.4f),
                                Color.Black.copy(alpha = 0.1f),
                                Color(0xFFADF7BD).copy(alpha = 0.4f)
                            )
                        )

                        drawRoundRect(
                            brush = gradient,
                            topLeft = Offset(horizontalPadding, size.height),
                            size = Size(size.width - 2 * horizontalPadding, shadowHeight),
                            cornerRadius = CornerRadius(shadowHeight / 2, shadowHeight / 2)
                        )
                    }
                    .background(Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 15.dp, top = 10.dp, bottom = 10.dp, end = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextComponents.Text(
                        text = "Text setting:",
                        fontSize = 18.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 0.dp)
                    )

                    Row {
                        RemoveButton(
                            onClick = { resetTextSetting() },
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        AddButton(
                            onClick = {
                                val titleValue = title.value.ifBlank { titleHint }
                                val bodyValue = body.value.ifBlank { bodyHint }

                                viewModel.notificationTitle.value = titleValue
                                viewModel.notificationBody.value = bodyValue

                                viewModel.saveNotificationData(context)
                            },
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            }

            SpacerHeightDp(10.dp)
            EditTextRow("Title", title, titleHint)
            SpacerHeightDp(7.dp)
            EditTextRow("Body", body, bodyHint)
        }
    }


    @Composable
    fun ImageSetting(viewModel: MainViewModel) {
        val context = LocalContext.current

        val smallImageUrl = remember { mutableStateOf("") }
        val largeImageUrl = remember { mutableStateOf("") }

        val currentSetting = remember { mutableStateOf(getNotificationData(context)) }

        fun resetImageSetting() {
            removeNotificationData(context)
            currentSetting.value = null
            smallImageUrl.value = ""
            largeImageUrl.value = ""
            viewModel.smallImageBitmap.value = null
            viewModel.largeImageBitmap.value = null
        }

        val smallImageHint = currentSetting.value?.smallImageUrl ?: ""
        val largeImageHint = currentSetting.value?.largeImageUrl ?: ""

        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val shadowHeight = 3.dp.toPx()
                        val horizontalPadding = 15.dp.toPx()

                        val gradient = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF8DBFFC).copy(alpha = 0.4f),
                                Color.Black.copy(alpha = 0.1f),
                                Color(0xFFADF7BD).copy(alpha = 0.4f)
                            )
                        )

                        drawRoundRect(
                            brush = gradient,
                            topLeft = Offset(horizontalPadding, size.height),
                            size = Size(size.width - 2 * horizontalPadding, shadowHeight),
                            cornerRadius = CornerRadius(shadowHeight / 2, shadowHeight / 2)
                        )
                    }
                    .background(Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 15.dp, top = 10.dp, bottom = 10.dp, end = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextComponents.Text(
                        text = "Image setting:",
                        fontSize = 18.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 0.dp)
                    )

                    Row {
                        RemoveButton(
                            onClick = { resetImageSetting() },
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        AddButton(
                            onClick = {
                                val smallUrl = smallImageUrl.value.ifBlank { smallImageHint }
                                val largeUrl = largeImageUrl.value.ifBlank { largeImageHint }

                                viewModel.smallImageUrl.value = smallUrl.ifEmpty { null }
                                viewModel.largeImageUrl.value = largeUrl.ifEmpty { null }

                                viewModel.loadSmallImage(context, smallUrl)
                                viewModel.loadLargeImage(context, largeUrl)

                                viewModel.saveNotificationData(context)

                                smallImageUrl.value = ""
                                largeImageUrl.value = ""

                                currentSetting.value = getNotificationData(context)
                            },
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            }

            SpacerHeightDp(10.dp)
            EditTextRow(
                name = "Small URL",
                state = smallImageUrl,
                hint = smallImageHint.takeIf { it.isNotBlank() } ?: "Enter small image URL"
            )
            SpacerHeightDp(7.dp)
            EditTextRow(
                name = "Large URL",
                state = largeImageUrl,
                hint = largeImageHint.takeIf { it.isNotBlank() } ?: "Enter large image URL"
            )
        }
    }

    @Composable
    fun ButtonsSetting(viewModel: MainViewModel) {
        val context = LocalContext.current

        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind { /* ... */ }
                    .background(Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 15.dp, top = 10.dp, bottom = 10.dp, end = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextComponents.Text(
                        text = "Buttons setting:",
                        fontSize = 18.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 0.dp)
                    )

                    Row {
                        RemoveButton(
                            onClick = {
                                viewModel.notificationButtons.value = emptyList()
                                viewModel.saveNotificationData(context)
                            },
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        AddButton(
                            onClick = {
                                if ((viewModel.notificationButtons.value?.size ?: 0) < 3) {
                                    val newButtons = (viewModel.notificationButtons.value ?: emptyList()) + ""
                                    viewModel.notificationButtons.value = newButtons
                                    viewModel.saveNotificationData(context)
                                }
                            },
                            modifier = Modifier.align(Alignment.CenterVertically),
                        )
                    }
                }
            }

            SpacerHeightDp(10.dp)

            viewModel.notificationButtons.value?.let { buttons ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    buttons.forEachIndexed { index, text ->
                        EditableButtonItem(
                            text = text,
                            onTextChanged = { newText ->
                                val updated = buttons.toMutableList().apply { set(index, newText) }
                                viewModel.notificationButtons.value = updated
                                viewModel.saveNotificationData(context)
                            },
                            onRemove = {
                                val updated = buttons.toMutableList().apply { removeAt(index) }
                                viewModel.notificationButtons.value = updated
                                viewModel.saveNotificationData(context)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun EditableButtonItem(
        text: String,
        onTextChanged: (String) -> Unit,
        onRemove: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        var editableText by remember(text) { mutableStateOf(text) }

        LaunchedEffect(editableText) {
            if (editableText != text) {
                onTextChanged(editableText)
            }
        }

        Box(
            modifier = modifier
                .shadow(3.dp, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = editableText,
                        onValueChange = { editableText = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 14.sp,
                            color = Color.Black
                        ),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (editableText.isEmpty()) {
                                    Text(
                                        text = "Button text",
                                        color = Color.Gray,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onRemove),
                    contentAlignment = Alignment.Center
                ) {
                    XShapedIcon(
                        lineHeight = 10.dp,
                        lineWidth = 2.5.dp,
                        firstColor = Color.Black,
                        secondColor = Color.Black
                    )
                }
            }
        }
    }
}