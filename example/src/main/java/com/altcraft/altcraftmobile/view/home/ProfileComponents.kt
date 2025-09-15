package com.altcraft.altcraftmobile.view.home

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altcraft.altcraftmobile.view.home.ActionsComponents.SectionInfo
import com.altcraft.altcraftmobile.viewmodel.MainViewModel
import com.altcraft.sdk.data.DataClasses

object ProfileComponents {
    @Composable
    fun ProfileBox(
        viewModel: MainViewModel,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .fillMaxWidth()
            ) {
                ProfileDataView(
                    profileData = viewModel.profileData.value,
                    isLoading = viewModel.profileDataIsLoading.value,
                    errorMessage = viewModel.errorMessage.value,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 2.5.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.White)
                )
            }
            Spacer(modifier = Modifier.height(15.dp))

            SectionInfo(
                action = "profile",
                onClick = {
                    viewModel.subscribeActions.value = false
                    viewModel.profileActions.value = false
                }
            )

            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    @Composable
    internal fun ProfileDataItem(label: String, value: String?) {
        val displayedValue = remember(label, value) {
            if (label == "Subscription ID" && value != null && value.length > 30) {
                val head = value.take(15)
                val tail = value.takeLast(15)
                "$head...$tail"
            } else {
                value ?: "null"
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                fontSize = 10.sp
            )
            Text(
                text = displayedValue,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Black,
                fontSize = 10.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    @Composable
    fun ProfileDataView(
        profileData: DataClasses.ProfileData?,
        errorMessage: String?,
        isLoading: Boolean,
        modifier: Modifier = Modifier
    ) {
        val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.7f

        Box(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .background(Color.White)
                .padding(8.dp),
            contentAlignment = Alignment.TopStart
        ) {
            when {
                !isLoading && (errorMessage != null || profileData == null) -> {
                    Text(
                        text = errorMessage ?: "Profile data is null",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                profileData != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ProfileDataItem("ID", profileData.id)
                        ProfileDataItem("Status", profileData.status)
                        ProfileDataItem("Is Test", profileData.isTest?.toString())

                        profileData.subscription?.let { subscription ->
                            Spacer(modifier = Modifier.height(5.dp))
                            GradientShadowLine(
                                colors = listOf(
                                    Color(0xFF8DBFFC).copy(alpha = 0.4f),
                                    Color.Black.copy(alpha = 0.1f),
                                    Color.Black.copy(alpha = 0.1f)
                                )
                            )
                            Text(
                                text = "Subscription:",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(5.dp))

                            ProfileDataItem("Subscription ID", subscription.subscriptionId)
                            ProfileDataItem("Hash ID", subscription.hashId)
                            ProfileDataItem("Provider", subscription.provider)
                            ProfileDataItem("Status", subscription.status)

                            subscription.cats?.let { cats ->
                                Spacer(modifier = Modifier.height(5.dp))
                                GradientShadowLine(
                                    colors = listOf(
                                        Color(0xFF8DBFFC).copy(alpha = 0.4f),
                                        Color.Black.copy(alpha = 0.1f),
                                        Color.Black.copy(alpha = 0.1f)
                                    )
                                )
                                Text(
                                    text = "Categories:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.height(5.dp))

                                cats.forEach { category ->
                                    ProfileDataItem("Name", category.name)
                                    ProfileDataItem("Title", category.title)
                                    ProfileDataItem("Steady", category.steady?.toString())
                                    ProfileDataItem("Active", category.active?.toString())
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }

                            subscription.fields?.let { fields ->
                                Spacer(modifier = Modifier.height(5.dp))
                                GradientShadowLine(
                                    colors = listOf(
                                        Color(0xFF8DBFFC).copy(alpha = 0.4f),
                                        Color.Black.copy(alpha = 0.1f),
                                        Color.Black.copy(alpha = 0.1f)
                                    )
                                )
                                Text(
                                    text = "Fields:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.height(5.dp))

                                fields.forEach { (key, value) ->
                                    ProfileDataItem(key, value.toString())
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GradientShadowLine(
    modifier: Modifier = Modifier,
    height: Dp = 3.dp,
    horizontalPadding: Dp = 0.dp,
    colors: List<Color> = listOf(
        Color.Black.copy(alpha = 0.1f),
        Color.Black.copy(alpha = 0.1f),
        Color(0xFFADF7BD).copy(alpha = 0.4f)
    )
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = horizontalPadding)
    ) {
        val gradient = Brush.horizontalGradient(colors)
        drawRoundRect(
            brush = gradient,
            topLeft = Offset(0f, 0f),
            size = size,
            cornerRadius = CornerRadius(size.height / 2, size.height / 2)
        )
    }
}

