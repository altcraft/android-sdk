package com.altcraft.altcraftmobile.view.config

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altcraft.altcraftmobile.view.config.Buttons.AddButton
import com.altcraft.altcraftmobile.view.config.Buttons.RemoveButton
import com.altcraft.altcraftmobile.view.config.EditTextRow.EditTextRow
import com.altcraft.altcraftmobile.view.config.ProviderSelectBox.ProviderSelectionBox
import com.altcraft.altcraftmobile.view.home.ActionsComponents.XShapedIcon
import com.altcraft.altcraftmobile.view.home.GradientShadowLine
import com.altcraft.altcraftmobile.view.home.TextComponents
import com.altcraft.altcraftmobile.data.AppPreferenses.getConfig
import com.altcraft.altcraftmobile.data.AppPreferenses.removeConfig
import com.altcraft.altcraftmobile.data.AppPreferenses.setConfig
import com.altcraft.altcraftmobile.data.AppDataClasses
import com.altcraft.altcraftmobile.functions.app.UI.SpacerHeightDp
import com.altcraft.altcraftmobile.icon
import com.altcraft.altcraftmobile.viewmodel.MainViewModel
import com.altcraft.altcraftmobile.functions.sdk.SDKFunctions.initAltcraft
import com.altcraft.sdk.AltcraftSDK.reinitializeRetryControlInThisSession
import com.altcraft.sdk.data.Constants.FCM_PROVIDER
import com.altcraft.sdk.data.Constants.HMS_PROVIDER
import com.altcraft.sdk.data.Constants.RUS_PROVIDER

object ConfigSettingComponents {
    @Composable
    fun ConfigSetting(viewModel: MainViewModel) {
        val context = LocalContext.current

        val apiUrlValue = remember { mutableStateOf("") }
        val rTokenValue = remember { mutableStateOf("") }
        val providerValue = remember { mutableStateOf("") }
        val serviceMsgValue = remember { mutableStateOf("") }
        val currentConfig = remember { mutableStateOf(getConfig(context)) }

        val parsedConfig = remember(currentConfig.value) {
            try {
                currentConfig.value
            } catch (_: Exception) {
                null
            }
        }

        fun resetAllConfig() {
            removeConfig(context)
            currentConfig.value = null
            viewModel.userName.value = null
            viewModel.configSetting.value = ""
            apiUrlValue.value = ""
            rTokenValue.value = ""
            providerValue.value = ""
            serviceMsgValue.value = ""
        }

        val apiUrlHint =
            if (currentConfig.value != null) parsedConfig?.apiUrl?.takeIf { it.isNotBlank() }
                ?: "" else ""
        val rTokenHint =
            if (currentConfig.value != null) parsedConfig?.rToken?.takeIf { it.isNotBlank() }
                ?: "" else ""
        val serviceMsgHint =
            if (currentConfig.value != null) parsedConfig?.serviceMessage?.takeIf { it.isNotBlank() }
                ?: "" else ""

        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 15.dp, top = 10.dp, bottom = 10.dp, end = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextComponents.Text(
                        text = "Config setting:",
                        fontSize = 18.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 0.dp)
                    )

                    Row {
                        RemoveButton(
                            onClick = { resetAllConfig() },
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        AddButton(
                            iconBackgroundColor =  Color(0xFFADF7BD).copy(alpha = 0.4f),
                            onClick = {
                                val apiUrl = apiUrlValue.value.ifBlank { apiUrlHint }
                                val rToken = rTokenValue.value.ifBlank { rTokenHint }
                                val serviceMsg = serviceMsgValue.value.ifBlank { serviceMsgHint }

                                if (apiUrl.isBlank()) {
                                    Toast.makeText(
                                        context,
                                        "API URL is not set",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@AddButton
                                }

                                val configToSave = AppDataClasses.ConfigData(
                                    apiUrl = apiUrl,
                                    rToken = rToken,
                                    priorityProviders = viewModel.providerList.value
                                        .takeIf { it.isNotEmpty() },
                                    icon = icon,
                                    usingService = serviceMsg.isNotEmpty(),
                                    serviceMessage = serviceMsg.ifEmpty { null }
                                )

                                setConfig(context, configToSave)

                                currentConfig.value = configToSave
                                viewModel.userName.value = configToSave.apiUrl
                                viewModel.configSetting.value = configToSave.toString()

                                reinitializeRetryControlInThisSession()

                                initAltcraft(context)

                                apiUrlValue.value = ""
                                rTokenValue.value = ""
                                providerValue.value = ""
                                serviceMsgValue.value = ""
                            },
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            }

            GradientShadowLine(
                horizontalPadding = 15.dp,
                colors = listOf(
                    Color(0xFF8DBFFC).copy(alpha = 0.4f),
                    Color.Black.copy(alpha = 0.1f),
                    Color(0xFFADF7BD).copy(alpha = 0.4f)
                )
            )

            SpacerHeightDp(10.dp)
            EditTextRow("API url", apiUrlValue, apiUrlHint)
            SpacerHeightDp(7.dp)
            EditTextRow("RToken", rTokenValue, rTokenHint)
            SpacerHeightDp(7.dp)
            ProviderSelectionBox(viewModel = viewModel)
            SpacerHeightDp(7.dp)
            EditTextRow("S.Msg", serviceMsgValue, serviceMsgHint)
        }
    }
}

object ProviderSelectBox {
    @Composable
    fun ProviderSelectionBox(
        viewModel: MainViewModel,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .height(25.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 5.dp)
                    ) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Providers",
                            color = Color.Black,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Normal
                        )

                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 1.dp,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(8.dp)
            ) {
                Column {
                    if (viewModel.providerList.value.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(viewModel.providerList.value) { provider ->
                                ProviderChip(
                                    provider = provider,
                                    onRemove = {
                                        viewModel.providerList.value -= provider
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProviderSelectionButton(
                            displayName = "FCM",
                            icon = painterResource(id = com.altcraft.altcraftmobile.R.drawable.ic_fcm),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (!viewModel.providerList.value.contains(FCM_PROVIDER)) {
                                    viewModel.providerList.value += FCM_PROVIDER
                                }
                            }
                        )

                        ProviderSelectionButton(
                            displayName = "HMS",
                            icon = painterResource(id = com.altcraft.altcraftmobile.R.drawable.ic_hms),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (!viewModel.providerList.value.contains(HMS_PROVIDER)) {
                                    viewModel.providerList.value += HMS_PROVIDER
                                }
                            }
                        )

                        ProviderSelectionButton(
                            displayName = "Rustore",
                            icon = painterResource(id = com.altcraft.altcraftmobile.R.drawable.ic_rus),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (!viewModel.providerList.value.contains(RUS_PROVIDER)) {
                                    viewModel.providerList.value += RUS_PROVIDER
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ProviderChip(
        provider: String,
        onRemove: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .height(28.dp)
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(14.dp)
                )
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .clickable { onRemove() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painter = when (provider) {
                        FCM_PROVIDER -> painterResource(id = com.altcraft.altcraftmobile.R.drawable.ic_fcm)
                        HMS_PROVIDER -> painterResource(id = com.altcraft.altcraftmobile.R.drawable.ic_hms)
                        RUS_PROVIDER -> painterResource(id = com.altcraft.altcraftmobile.R.drawable.ic_rus)
                        else -> painterResource(id = com.altcraft.altcraftmobile.R.drawable.ic_fcm)
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Black
                )

                Text(
                    text = when (provider) {
                        FCM_PROVIDER -> "FCM"
                        HMS_PROVIDER -> "HMS"
                        RUS_PROVIDER -> "Rustore"
                        else -> provider
                    },
                    color = Color.Black,
                    fontSize = 12.sp
                )

                XShapedIcon(
                    lineWidth = 10.dp,
                    lineHeight = 2.dp,
                    firstColor = Color.Black,
                    secondColor = Color.Black
                )
            }
        }
    }

    @Composable
    private fun ProviderSelectionButton(
        displayName: String,
        icon: Painter,
        modifier: Modifier = Modifier,
        onClick: () -> Unit
    ) {
        Box(
            modifier = modifier
                .height(40.dp)
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(8.dp)
                )
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = displayName,
                    fontSize = 12.sp,
                    color = Color.Black
                )
            }
        }
    }
}