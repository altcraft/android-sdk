package com.altcraft.altcraftmobile.view.config

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altcraft.altcraftmobile.view.config.Buttons.AddButton
import com.altcraft.altcraftmobile.view.config.Buttons.RemoveButton
import com.altcraft.altcraftmobile.view.config.EditTextRow.EditTextRow
import com.altcraft.altcraftmobile.view.home.GradientShadowLine
import com.altcraft.altcraftmobile.view.home.TextComponents
import com.altcraft.altcraftmobile.data.AppPreferenses.removeJWT
import com.altcraft.altcraftmobile.data.AppPreferenses.setAnonJWT
import com.altcraft.altcraftmobile.data.AppPreferenses.setAuthStatus
import com.altcraft.altcraftmobile.data.AppPreferenses.setRegJWT
import com.altcraft.altcraftmobile.functions.app.UI.SpacerHeightDp
import com.altcraft.altcraftmobile.viewmodel.MainViewModel

object JWTSettingComponents {
    @Composable
    fun JWTSetting(viewModel: MainViewModel) {
        val context = LocalContext.current

        val anonJWTValue = remember { mutableStateOf("") }
        val regJWTValue = remember { mutableStateOf("") }

        val anonJWTHint = viewModel.anonJWT.value
        val regJWTHint = viewModel.regJWT.value

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
                        text = "JWT setting:",
                        fontSize = 18.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 0.dp)
                    )

                    Row {
                        RemoveButton(
                            onClick = {
                                removeJWT(context)
                                setAuthStatus(context, false)
                                viewModel.anonJWT.value = ""
                                viewModel.regJWT.value = ""
                                anonJWTValue.value = ""
                                regJWTValue.value = ""
                                Toast.makeText(context, "JWT cleared", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        AddButton(
                            onClick = {
                                val anonJWT = anonJWTValue.value.ifBlank { anonJWTHint }
                                val regJWT = regJWTValue.value.ifBlank { regJWTHint }

                                if (anonJWT.isBlank() && regJWT.isBlank()) {
                                    Toast.makeText(
                                        context,
                                        "At least one JWT must be set",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@AddButton
                                }

                                setAnonJWT(context, anonJWT)
                                setRegJWT(context, regJWT)
                                viewModel.anonJWT.value = anonJWT
                                viewModel.regJWT.value = regJWT

                                anonJWTValue.value = ""
                                regJWTValue.value = ""
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
            EditTextRow(
                "Anon.JWT",
                anonJWTValue,
                anonJWTHint.takeIf { it.isNotBlank() }?.truncatedJWT()
            )
            SpacerHeightDp(7.dp)
            EditTextRow(
                "Reg.JWT",
                regJWTValue,
                regJWTHint.takeIf { it.isNotBlank() }?.truncatedJWT()
            )
        }
    }
}

private fun String.truncatedJWT(): String =
    takeIf { length > 40 }?.let {
        "${it.take(17)}...${it.takeLast(17)}"
    } ?: this