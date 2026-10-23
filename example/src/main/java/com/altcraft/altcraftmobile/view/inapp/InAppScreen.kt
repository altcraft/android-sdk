package com.altcraft.altcraftmobile.view.inapp

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import com.altcraft.sdk.inapp.inApp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altcraft.altcraftmobile.R
import com.altcraft.altcraftmobile.view.header.Header.HeaderSection
import com.altcraft.altcraftmobile.viewmodel.MainViewModel
import com.altcraft.sdk.AltcraftSDK

@Composable
fun InAppScreen(viewModel: MainViewModel) {
    val dividerColor = Color(0xFFE0E0E0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        HeaderSection()

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(15.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                PlacementCell(
                    count = viewModel.inAppPlacementCount.intValue
                )

                VerticalDivider(dividerColor)

                InAppIconCell(
                    iconRes = R.drawable.ic_in_app_center,
                    title = "Modal",
                    trigger = "tr_modal"
                )
            }

            HorizontalDivider(dividerColor)

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                InAppIconCell(
                    iconRes = R.drawable.ic_in_app_fullscreen,
                    title = "Fullscreen",
                    trigger = "tr_fullscreen"
                )

                VerticalDivider(dividerColor)

                InAppIconCell(
                    iconRes = R.drawable.ic_in_app_down,
                    title = "Floating Bar",
                    trigger = "tr_floating_bar"
                )
            }

            HorizontalDivider(dividerColor)

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                InAppIconCell(
                    iconRes = R.drawable.ic_in_app_top,
                    title = "SlideUp",
                    trigger = "tr_slide_up"
                )

                VerticalDivider(dividerColor)

                InAppIconCell(
                    iconRes = R.drawable.ic_in_app_custom,
                    title = "Custom",
                    trigger = "tr_custom"
                )
            }
        }
    }
}

@Composable
private fun RowScope.PlacementCell(
    count: Int
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Placements",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = Color.Black,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clickable {
                        AltcraftSDK.inApp.getPlacements(context)
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_in_app_download),
                    contentDescription = "Placements",
                    modifier = Modifier
                        .size(120.dp)
                        .padding(8.dp)
                )

                if (count > 0) {
                    CountBadge(
                        count = count,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-2).dp, y = 0.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Load placements",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 11.sp
                ),
                color = Color.Gray,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun VerticalDivider(color: Color) {
    Box(
        modifier = Modifier
            .padding(vertical = 15.dp)
            .width(2.dp)
            .fillMaxSize()
            .background(color)
    )
}

@Composable
private fun HorizontalDivider(color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .padding(horizontal = 15.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .background(color)
        )

        Box(
            modifier = Modifier
                .width(60.dp)
                .height(2.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .background(color)
        )
    }
}

@Composable
private fun CountBadge(
    count: Int,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp
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
            Text(
                text = count.toString(),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 10.sp
            )
        }
    }
}

@Composable
private fun RowScope.InAppIconCell(
    iconRes: Int,
    title: String,
    trigger: String
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = Color.Black,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(12.dp))

            Image(
                painter = painterResource(iconRes),
                contentDescription = title,
                modifier = Modifier
                    .size(120.dp)
                    .padding(8.dp)
                    .clickable {
                        AltcraftSDK.inApp.trigger(
                            context = context,
                            name = trigger
                        )
                    }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = trigger,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 11.sp
                ),
                color = Color.Gray,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}