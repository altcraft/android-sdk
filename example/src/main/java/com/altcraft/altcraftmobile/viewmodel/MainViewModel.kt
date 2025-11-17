package com.altcraft.altcraftmobile.viewmodel

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.altcraft.altcraftmobile.data.AppPreferenses
import com.altcraft.altcraftmobile.data.AppPreferenses.getAnonJWT
import com.altcraft.altcraftmobile.data.AppPreferenses.getNotificationData
import com.altcraft.altcraftmobile.data.AppPreferenses.getRegJWT
import com.altcraft.altcraftmobile.data.AppPreferenses.getConfig
import com.altcraft.altcraftmobile.data.AppPreferenses.getSubscribeSettings
import com.altcraft.altcraftmobile.data.AppPreferenses.getSubscriptionStatus
import com.altcraft.altcraftmobile.data.AppPreferenses.getTokenUpdateTime
import com.altcraft.altcraftmobile.data.AppPreferenses.setNotificationData
import com.altcraft.altcraftmobile.data.AppPreferenses.setTokenUpdateTime
import com.altcraft.altcraftmobile.data.AppConstants.EXAMPLE_BODY
import com.altcraft.altcraftmobile.data.AppConstants.EXAMPLE_TITLE
import com.altcraft.altcraftmobile.data.AppConstants.STATUS_EVENT
import com.altcraft.altcraftmobile.data.AppConstants.STATUS_EVENT_ERRORS
import com.altcraft.altcraftmobile.data.AppConstants.SUBSCRIBE_EVENT
import com.altcraft.altcraftmobile.data.AppConstants.TOKEN_UPDATE_EVENT
import com.altcraft.altcraftmobile.data.AppConstants.UNSUBSCRIBED
import com.altcraft.altcraftmobile.data.AppDataClasses
import com.altcraft.altcraftmobile.event.EventReceiver
import com.altcraft.altcraftmobile.functions.app.Formatter.formatDate
import com.altcraft.altcraftmobile.functions.app.SubFunction
import com.altcraft.altcraftmobile.functions.app.SubFunction.loadImage
import com.altcraft.sdk.AltcraftSDK
import com.altcraft.sdk.data.DataClasses
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Date

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // Authentication and token states
    val status = mutableStateOf(UNSUBSCRIBED)
    val token = mutableStateOf<String?>(null)
    val provider = mutableStateOf<String?>(null)
    val providerList = mutableStateOf<List<String>>(listOf())
    val updateTokenTime = mutableStateOf<String?>(null)
    val userName = mutableStateOf<String?>(null)
    val anonJWT = mutableStateOf("")
    val regJWT = mutableStateOf("")

    // User interface and profile states
    val profileActions = mutableStateOf(false)
    val profileData = mutableStateOf<DataClasses.ProfileData?>(null)
    val profileDataIsLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)

    // Application configuration and subscriptions
    val configSetting = mutableStateOf("")
    val subscribeActions = mutableStateOf(false)
    val subscribeSettings = mutableStateOf(AppDataClasses.SubscribeSettings.getDefault())

    // Events
    val eventList = mutableStateOf<List<DataClasses.Event>>(emptyList())

    val newFieldKey = mutableStateOf("")
    val newFieldValue = mutableStateOf("")

    // Notification states
    val notificationTitle = mutableStateOf(EXAMPLE_TITLE)
    val notificationBody = mutableStateOf(EXAMPLE_BODY)
    val notificationButtons = mutableStateOf<List<String>?>(null)

    val smallImageUrl = mutableStateOf<String?>(null)
    val largeImageUrl = mutableStateOf<String?>(null)

    val smallImageBitmap = mutableStateOf<Bitmap?>(null)
    val largeImageBitmap = mutableStateOf<Bitmap?>(null)

    init {
        val context = getApplication<Application>().applicationContext
        updateUI(context)
        collectEvents(context)
    }

    private fun collectEvents(context: Context) {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            EventReceiver.get().events.collectLatest { event ->
                handleIncomingEvent(event, context)
            }
        }
    }

    private fun updateUI(context: Context) {
        updateTokenUI(context)
        updateConfigUI(context)
        updateStatusUI(context)
        updateTokenUpdateUI(context)
        loadNotificationSettings(context)
    }

    private fun loadNotificationSettings(context: Context) {
        val savedData = getNotificationData(context) ?: AppDataClasses.NotificationData.getDefault()

        notificationTitle.value = savedData.title
        notificationBody.value = savedData.body
        notificationButtons.value = savedData.buttons
        smallImageUrl.value = savedData.smallImageUrl
        largeImageUrl.value = savedData.largeImageUrl

        savedData.smallImageUrl?.let { loadSmallImage(context, it) }
        savedData.largeImageUrl?.let { loadLargeImage(context, it) }
    }

    internal fun saveNotificationData(context: Context) {
        smallImageUrl.value?.let {
            if (it.isEmpty()) {
                smallImageUrl.value = null
            }
        }

        largeImageUrl.value?.let {
            if (it.isEmpty()) {
                largeImageUrl.value = null
            }
        }

        val newData = AppDataClasses.NotificationData(
            title = notificationTitle.value,
            body = notificationBody.value,
            buttons = notificationButtons.value,
            smallImageUrl = smallImageUrl.value,
            largeImageUrl = largeImageUrl.value
        )

        setNotificationData(context, newData)

        loadSmallImage(context, smallImageUrl.value ?: "")
        loadLargeImage(context, largeImageUrl.value ?: "")
    }

    /**
     * Processes incoming SDK events
     */
    private fun handleIncomingEvent(event: DataClasses.Event, context: Context) {
        eventList.value = eventList.value.toMutableList().apply {
            add(event)
        }

        when (event.eventCode) {
            SUBSCRIBE_EVENT, STATUS_EVENT -> status.value = handleStatusUpdate(context, event)

            TOKEN_UPDATE_EVENT -> {
                handleTokenUpdate(context, event.date)
                updateConfigUI(context)
            }

            STATUS_EVENT_ERRORS[0], STATUS_EVENT_ERRORS[1] -> {
                errorMessage.value =
                    "Profile information is not available. Error: ${event.eventMessage}"
            }
        }
    }

    private fun handleStatusUpdate(context: Context, event: DataClasses.Event): String {
        return try {
            val responseWithHttp =
                event.eventValue?.get("response_with_http_code") as? DataClasses.ResponseWithHttpCode

            val status = responseWithHttp?.response?.profile?.subscription?.status ?: UNSUBSCRIBED

            AppPreferenses.setSubscriptionStatus(context, status)

            status
        } catch (_: Exception) {
            AppPreferenses.setSubscriptionStatus(context, UNSUBSCRIBED)
            UNSUBSCRIBED
        }
    }

    internal fun clearEventsList() {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            eventList.value = emptyList()
        }
    }

    /**
     * Check subscribe status in preferenses
     */
    private fun updateStatusUI(context: Context) {
        status.value = getSubscriptionStatus(context) ?: UNSUBSCRIBED
    }

    private fun handleTokenUpdate(context: Context, date: Date) {
        updateTokenUI(context)
        setTokenUpdateTime(context, date)
        updateTokenTime.value = formatDate(date)
    }

    private fun updateTokenUpdateUI(context: Context) {
        if (getTokenUpdateTime(context) == null) {
            setTokenUpdateTime(context, Date())
        }
        updateTokenTime.value = getTokenUpdateTime(context)
    }

    private fun updateConfigUI(context: Context) {
        val config = getConfig(context)
        val saveSubSetting = getSubscribeSettings(context)
        val defaultSubSetting = AppDataClasses.SubscribeSettings.getDefault()


        val subscribeSetting = saveSubSetting ?: defaultSubSetting
        providerList.value = config?.priorityProviders ?: emptyList()

        userName.value = config?.apiUrl
        configSetting.value = config.toString()
        subscribeSettings.value = subscribeSetting

        anonJWT.value = getAnonJWT(context) ?: ""
        regJWT.value = getRegJWT(context) ?: ""
    }

    /**
     * Updates the current push token data from AltcraftSDK.
     *
     * Fetches the latest token and updates the [provider] and [token] values.
     * Runs in the background using [Dispatchers.IO].
     *
     * @param context The application context required for token retrieval
     */
    internal fun updateTokenUI(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {

            val currentToken = AltcraftSDK.pushTokenFunctions.getPushToken(context)

            provider.value = currentToken?.provider
            token.value = currentToken?.token
        }
    }

    internal fun loadSmallImage(context: Context, url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (url.isNotEmpty()) {
                val bitmap = loadImage(context, url)
                smallImageBitmap.value = bitmap
            }
        }
    }

    internal fun loadLargeImage(context: Context, url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (url.isNotEmpty()) {
                val bitmap = loadImage(context, url)
                largeImageBitmap.value = bitmap
            }
        }
    }

    /**
     * Loads profile status and updates UI state:
     * - Sets loading state (profileDataIsLoading)
     * - Clears any errors (errorMessage)
     * - Updates profile data (profileData)
     * - Resets loading when complete
     *
     * @param context The application context required for token retrieval
     */
    fun loadProfileStatus(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            profileDataIsLoading.value = true
            errorMessage.value = null

            profileData.value = AltcraftSDK
                .pushSubscriptionFunctions
                .getStatusForCurrentSubscription(context)
                ?.response?.profile

            profileDataIsLoading.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        AltcraftSDK.eventSDKFunctions.unsubscribe()
    }
}


