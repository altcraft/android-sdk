package com.altcraft.altcraftmobile.view.config

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Divider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altcraft.altcraftmobile.view.config.Buttons.AddButton
import com.altcraft.altcraftmobile.view.config.Buttons.RemoveButton
import com.altcraft.altcraftmobile.view.config.EditTextRow.EditTextRow
import com.altcraft.altcraftmobile.view.config.SubscribeSettingComponents.AddFieldBox.AddKeyValueBox
import com.altcraft.altcraftmobile.view.config.SubscribeSettingComponents.AddFieldBox.RemoveFieldsBox
import com.altcraft.altcraftmobile.view.home.ActionsComponents.XShapedIcon
import com.altcraft.altcraftmobile.view.home.GradientShadowLine
import com.altcraft.altcraftmobile.view.home.TextComponents
import com.altcraft.altcraftmobile.data.AppPreferenses.getSubscribeSettings
import com.altcraft.altcraftmobile.data.AppPreferenses.removeSubscribeSettings
import com.altcraft.altcraftmobile.data.AppPreferenses.setSubscribeSettings
import com.altcraft.altcraftmobile.data.AppDataClasses
import com.altcraft.altcraftmobile.functions.app.Converter.Parser.parseJsonToMap
import com.altcraft.altcraftmobile.functions.app.Converter.mapToJsonString
import com.altcraft.altcraftmobile.functions.app.UI.SpacerHeightDp
import com.altcraft.altcraftmobile.viewmodel.MainViewModel
import com.altcraft.sdk.data.DataClasses
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object SubscribeSettingComponents {
    @Composable
    fun SubscribeSetting(viewModel: MainViewModel, modifier: Modifier = Modifier) {
        val context = LocalContext.current

        val currentSettings = remember {
            mutableStateOf(getSubscribeSettings(context))
        }

        val parsedSettings = remember(currentSettings.value) {
            try {
                currentSettings.value
            } catch (_: Exception) {
                null
            }
        }

        val syncChecked = remember {
            mutableStateOf(parsedSettings?.sync == true)
        }
        val replaceChecked = remember {
            mutableStateOf(parsedSettings?.replace == false)
        }
        val skipTriggersChecked = remember {
            mutableStateOf(parsedSettings?.skipTriggers == false)
        }

        val customFieldsHint by remember(viewModel.subscribeSettings.value) {
            mutableStateOf(
                viewModel.subscribeSettings.value.customFields.let { mapToJsonString(it) }
            )
        }

        val profileFieldsHint by remember(viewModel.subscribeSettings.value) {
            mutableStateOf(
                viewModel.subscribeSettings.value.profileFields.let { mapToJsonString(it) }
            )
        }

        val catsHint by remember(viewModel.subscribeSettings.value) {
            mutableStateOf(Gson().toJson(viewModel.subscribeSettings.value.cats))
        }

        val customFieldsValue = remember { mutableStateOf("") }
        val profileFieldsValue = remember { mutableStateOf("") }
        val catsValue = remember { mutableStateOf("") }

        // States for showing AddKeyValueBox
        val showCustomFieldsAddBox = remember { mutableStateOf(false) }
        val showProfileFieldsAddBox = remember { mutableStateOf(false) }
        val showCatsAddBox = remember { mutableStateOf(false) }

        fun saveSettings() {
            val gson = Gson()
            val catsJson = catsValue.value.ifBlank { catsHint }

            val settings = AppDataClasses.SubscribeSettings(
                sync = syncChecked.value,
                replace = replaceChecked.value,
                skipTriggers = skipTriggersChecked.value,
                customFields = parseJsonToMap(customFieldsValue.value.ifBlank {
                    customFieldsHint
                }),
                profileFields = parseJsonToMap(profileFieldsValue.value.ifBlank {
                    profileFieldsHint
                }),
                cats = try {
                    gson.fromJson(
                        catsJson,
                        object : TypeToken<List<DataClasses.CategoryData>>() {}.type
                    )
                } catch (_: Exception) {
                    emptyList()
                }
            )

            setSubscribeSettings(context, settings)
            currentSettings.value = settings
            viewModel.subscribeSettings.value = settings

            customFieldsValue.value = ""
            profileFieldsValue.value = ""
            catsValue.value = ""
        }

        fun clearSettings() {
            removeSubscribeSettings(context)
            currentSettings.value = null
            viewModel.subscribeSettings.value = AppDataClasses.SubscribeSettings.getDefault()

            syncChecked.value = true
            replaceChecked.value = false
            skipTriggersChecked.value = false
            customFieldsValue.value = ""
            profileFieldsValue.value = ""
            catsValue.value = ""
        }

        Column(modifier = modifier) {
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
                        text = "Subscribe setting:",
                        fontSize = 18.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 0.dp)
                    )

                    Row {
                        RemoveButton(
                            onClick = { clearSettings() },
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        AddButton(
                            onClick = { saveSettings() },
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

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(start = 10.dp)) {

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Switch(
                                text = "sync",
                                isChecked = syncChecked.value,
                                onCheckedChange = { syncChecked.value = it }
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 20.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                TextComponents.Text(
                                    text = "It is necessary to give a synchronous response",
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.W400,
                                    modifier = Modifier.offset(y = 5.dp)
                                )
                            }
                        }

                        Divider(
                            modifier = Modifier.padding(start = 55.dp, end = 15.dp, top = 4.dp),
                            thickness = 1.5.dp,
                            color = Color.LightGray.copy(alpha = 0.5f)
                        )
                    }

                    SpacerHeightDp(10.dp)

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Switch(
                                text = "replace",
                                isChecked = replaceChecked.value,
                                onCheckedChange = { replaceChecked.value = it }
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 20.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                TextComponents.Text(
                                    text = "Suspends other subscriptions in the database",
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.W400,
                                    modifier = Modifier.offset(y = 5.dp)
                                )
                            }
                        }

                        Divider(
                            modifier = Modifier.padding(start = 55.dp, end = 15.dp, top = 4.dp),
                            thickness = 1.5.dp,
                            color = Color.LightGray.copy(alpha = 0.5f)
                        )
                    }

                    SpacerHeightDp(10.dp)

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Switch(
                                text = "skipTriggers",
                                isChecked = skipTriggersChecked.value,
                                onCheckedChange = { skipTriggersChecked.value = it }
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 20.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                TextComponents.Text(
                                    text = "An instruction to ignore company triggers",
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.W400,
                                    modifier = Modifier.offset(y = 5.dp)
                                )
                            }
                        }

                        Divider(
                            modifier = Modifier.padding(start = 55.dp, end = 15.dp, top = 4.dp),
                            thickness = 1.5.dp,
                            color = Color.LightGray.copy(alpha = 0.5f)
                        )
                    }

                    SpacerHeightDp(15.dp)

                    TextComponents.Text(
                        text = "Fields & Cats values:",
                        fontSize = 15.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.W600,
                        modifier = Modifier
                            .offset(y = 5.dp)
                            .padding(start = 13.dp)
                    )

                    SpacerHeightDp(15.dp)

                    // Custom Fields Section
                    EditTextRow(
                        "C.Fields",
                        customFieldsValue,
                        customFieldsHint,
                        startPadding = 3.dp
                    )

                    if (showCustomFieldsAddBox.value) {
                        AddKeyValueBox(
                            viewModel = viewModel,
                            onAdd = { key, value ->
                                if (key.isNotBlank() && value.isNotBlank()) {
                                    val currentFields =
                                        viewModel.subscribeSettings.value.customFields.toMutableMap()
                                    currentFields[key] = value
                                    viewModel.subscribeSettings.value =
                                        viewModel.subscribeSettings.value.copy(
                                            customFields = currentFields
                                        )
                                    setSubscribeSettings(context, viewModel.subscribeSettings.value)
                                }
                                showCustomFieldsAddBox.value = false
                            }
                        )
                    } else {
                        RemoveFieldsBox(
                            fields = viewModel.subscribeSettings.value.customFields,
                            onFieldRemoved = { key ->
                                val currentFields =
                                    viewModel.subscribeSettings.value.customFields.toMutableMap()
                                currentFields.remove(key)
                                viewModel.subscribeSettings.value =
                                    viewModel.subscribeSettings.value.copy(
                                        customFields = currentFields
                                    )
                                setSubscribeSettings(context, viewModel.subscribeSettings.value)
                            },
                            onAddClick = { showCustomFieldsAddBox.value = true }
                        )
                    }

                    SpacerHeightDp(7.dp)

                    // Profile Fields Section
                    EditTextRow(
                        "P.Fields",
                        profileFieldsValue,
                        profileFieldsHint,
                        startPadding = 3.dp
                    )

                    if (showProfileFieldsAddBox.value) {
                        AddKeyValueBox(
                            viewModel = viewModel,
                            onAdd = { key, value ->
                                if (key.isNotBlank() && value.isNotBlank()) {
                                    val currentFields =
                                        viewModel.subscribeSettings.value.profileFields.toMutableMap()
                                    currentFields[key] = value
                                    viewModel.subscribeSettings.value =
                                        viewModel.subscribeSettings.value.copy(
                                            profileFields = currentFields
                                        )
                                    setSubscribeSettings(context, viewModel.subscribeSettings.value)
                                }
                                showProfileFieldsAddBox.value = false
                            }
                        )
                    } else {
                        RemoveFieldsBox(
                            fields = viewModel.subscribeSettings.value.profileFields,
                            onFieldRemoved = { key ->
                                val currentFields =
                                    viewModel.subscribeSettings.value.profileFields.toMutableMap()
                                currentFields.remove(key)
                                viewModel.subscribeSettings.value =
                                    viewModel.subscribeSettings.value.copy(
                                        profileFields = currentFields
                                    )
                                setSubscribeSettings(context, viewModel.subscribeSettings.value)
                            },
                            onAddClick = { showProfileFieldsAddBox.value = true }
                        )
                    }

                    SpacerHeightDp(7.dp)

                    // Cats Section
                    EditTextRow(
                        "Cats",
                        catsValue,
                        catsHint,
                        startPadding = 3.dp
                    )

                    if (showCatsAddBox.value) {
                        AddKeyValueBox(
                            viewModel = viewModel,
                            onAdd = { key, value ->
                                if (key.isNotBlank()) {
                                    val updatedCats =
                                        viewModel.subscribeSettings.value.cats.toMutableList()
                                    updatedCats.add(
                                        DataClasses.CategoryData(
                                            name = key,
                                            active = value.toBoolean()
                                        )
                                    )
                                    viewModel.subscribeSettings.value =
                                        viewModel.subscribeSettings.value.copy(
                                            cats = updatedCats
                                        )
                                    setSubscribeSettings(context, viewModel.subscribeSettings.value)
                                }
                                showCatsAddBox.value = false
                            }
                        )
                    } else {
                        RemoveFieldsBox(
                            fields = viewModel.subscribeSettings.value.cats
                                .mapNotNull { category ->
                                    category.name?.let { name ->
                                        name to (category.active?.toString() ?: "null")
                                    }
                                }
                                .toMap(),
                            onFieldRemoved = { nameToRemove ->
                                val updatedCats = viewModel.subscribeSettings.value.cats
                                    .filter { it.name != nameToRemove }
                                viewModel.subscribeSettings.value =
                                    viewModel.subscribeSettings.value.copy(
                                        cats = updatedCats
                                    )
                                setSubscribeSettings(context, viewModel.subscribeSettings.value)
                            },
                            onAddClick = { showCatsAddBox.value = true }
                        )
                    }
                }
            }
        }
    }


    object AddFieldBox {

        @Composable
        private fun FieldChip(
            key: String,
            onRemove: () -> Unit
        ) {
            Box(
                modifier = Modifier
                    .height(20.dp)
                    .shadow(
                        elevation = 3.dp,
                        shape = RoundedCornerShape(10.dp),
                        clip = false
                    )
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .clickable { onRemove() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = key,
                        color = Color.Black,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                    )

                    Spacer(modifier = Modifier.width(5.dp))

                    XShapedIcon(
                        lineHeight = 10.dp,
                        lineWidth = 2.dp,
                        firstColor = Color.Black,
                        secondColor = Color.Black,
                        modifier = Modifier
                    )

                    Spacer(modifier = Modifier.width(5.dp))
                }
            }
        }

        @Composable
        fun RemoveFieldsBox(
            fields: Map<String, Any?>,
            onFieldRemoved: (String) -> Unit,
            onAddClick: () -> Unit
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 10.dp)
                    .shadow(
                        elevation = 1.dp,
                        shape = RoundedCornerShape(12.dp),
                        clip = false
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(fields.keys.toList()) { key ->
                                FieldChip(
                                    key = key,
                                    onRemove = { onFieldRemoved(key) }
                                )
                            }
                        }
                    }

                    AddButton(
                        onClick = onAddClick,
                        modifier = Modifier.size(20.dp),
                        buttonSize = 20.dp,
                        iconSize = 15.dp,
                        iconBackgroundColor = Color(0xFFADF7BD).copy(alpha = 0.4f)
                    )
                }
            }
        }


        @Composable
        fun AddKeyValueBox(
            viewModel: MainViewModel,
            onAdd: (String, String) -> Unit
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 1.dp,
                        shape = RoundedCornerShape(12.dp),
                        clip = false
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .border(
                                width = 1.dp,
                                color = Color.LightGray,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = viewModel.newFieldKey.value,
                            onValueChange = { viewModel.newFieldKey.value = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 12.sp,
                                color = Color.Black
                            ),
                            decorationBox = { innerTextField ->
                                if (viewModel.newFieldKey.value.isEmpty()) {
                                    Text(
                                        "Key",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .border(
                                width = 1.dp,
                                color = Color.LightGray,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = viewModel.newFieldValue.value,
                            onValueChange = { viewModel.newFieldValue.value = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 12.sp,
                                color = Color.Black
                            ),
                            decorationBox = { innerTextField ->
                                if (viewModel.newFieldValue.value.isEmpty()) {
                                    Text(
                                        "Value",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }

                    Box(
                        modifier = Modifier.width(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AddButton(
                            onClick = {
                                onAdd(viewModel.newFieldKey.value, viewModel.newFieldValue.value)
                                viewModel.newFieldKey.value = ""
                                viewModel.newFieldValue.value = ""
                            },
                            modifier = Modifier.size(28.dp),
                            buttonSize = 20.dp,
                            iconSize = 19.dp,
                            iconBackgroundColor = Color(0xFFADF7BD).copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun Switch(
        modifier: Modifier = Modifier,
        text: String,
        isChecked: Boolean,
        onCheckedChange: (Boolean) -> Unit
    ) {
        val switchThumbColor = Color.Black
        val trackColor = Color.White

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
        ) {
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(32.dp)
                    .padding(
                        top = 7.5.dp,
                        start = 9.dp,
                        end = 7.5.dp
                    )
                    .shadow(
                        elevation = 2.7.dp,
                        shape = RoundedCornerShape(50.dp),
                        clip = false
                    )
                    .clip(RoundedCornerShape(50.dp))
                    .background(trackColor)
                    .clickable { onCheckedChange(!isChecked) }
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .padding(3.dp)
                        .border(
                            width = 1.5.dp,
                            color = if (isChecked)
                                Color(0xFFADF7BD).copy(alpha = 1f)
                            else
                                Color(0xFF8DBFFC).copy(alpha = 1f),
                            shape = CircleShape
                        )
                        .background(switchThumbColor, CircleShape)
                        .shadow(
                            elevation = 2.3.dp,
                            shape = CircleShape,
                            clip = true,
                            ambientColor = Color.Black.copy(alpha = 0.2f),
                            spotColor = Color.Black.copy(alpha = 0.1f)
                        )
                        .align(if (isChecked) Alignment.CenterEnd else Alignment.CenterStart)
                )
            }

            Text(
                text = text,
                modifier = Modifier.offset(y = 3.2.dp),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 10.5.sp
                ),
                color = Color.Black
            )
        }
    }
}





