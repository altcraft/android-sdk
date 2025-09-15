package com.altcraft.altcraftmobile.view.config

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object EditTextRow {
    @Composable
    fun EditTextRow(
        name: String,
        state: MutableState<String>,
        hint: String?,
        startPadding: Dp = 15.dp,
        endPadding: Dp = 15.dp,
        ovalBoxHeight: Dp = 25.dp,
        boxRadius: Dp = 30.dp,
        whiteBoxSize: Dp = 20.dp,
        whiteBoxPadding: Dp = 5.dp,
        textStyle: TextStyle = TextStyle(
            color = Color.Black,
            fontSize = 12.sp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = startPadding, end = endPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .height(ovalBoxHeight)
                        .clip(RoundedCornerShape(boxRadius))
                        .background(Color.White.copy(alpha = 0.0f)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(whiteBoxSize)
                                .padding(whiteBoxPadding)
                                .border(
                                    width = 1.5.dp,
                                    Color.Gray,
                                    shape = CircleShape
                                )
                                .shadow(
                                    elevation = 5.dp,
                                    shape = RoundedCornerShape(boxRadius),
                                    clip = false
                                )
                                .clip(RoundedCornerShape(boxRadius))
                                .background(Color.Black)
                        )

                        Spacer(modifier = Modifier.width(3.dp))

                        Text(
                            text = name,
                            color = Color.Black,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier
                                .wrapContentWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.width(5.dp))

                ApiTextField(
                    state = state,
                    hint = hint,
                    textStyle = textStyle,
                    modifier = Modifier
                        .align(Alignment.Bottom)
                        .offset(y = 3.dp)
                        .fillMaxWidth()
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

    @Composable
    fun ApiTextField(
        modifier: Modifier = Modifier,
        state: MutableState<String>,
        hint: String? = null,
        startPadding: Dp = 0.dp,
        endPadding: Dp = 0.dp,
        textStyle: TextStyle = TextStyle(
            color = Color.Black,
            fontSize = 12.sp
        ),
    ) {
        Box(
            modifier = modifier
                .padding(start = startPadding, end = endPadding)
                .wrapContentHeight(),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = state.value,
                onValueChange = { state.value = it },
                singleLine = true,
                textStyle = textStyle.copy(
                    lineHeight = textStyle.fontSize
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (state.value.isEmpty() && !hint.isNullOrBlank()) {
                            Text(
                                text = hint,
                                color = Color.Gray,
                                style = textStyle.copy(
                                    lineHeight = textStyle.fontSize
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}