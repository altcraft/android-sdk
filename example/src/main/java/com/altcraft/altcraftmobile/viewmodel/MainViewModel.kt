package com.altcraft.altcraftmobile.viewmodel

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.altcraft.altcraftmobile.data.AppConstants.EXAMPLE_BODY
import com.altcraft.altcraftmobile.data.AppConstants.EXAMPLE_TITLE
import com.altcraft.altcraftmobile.data.AppConstants.PROFILE_NOT_FOUND
import com.altcraft.altcraftmobile.data.AppConstants.RESPONSE_WITH_HTTP
import com.altcraft.altcraftmobile.data.AppConstants.SUBSCRIBED
import com.altcraft.altcraftmobile.data.AppConstants.UNSUBSCRIBED
import com.altcraft.altcraftmobile.data.AppDataClasses
import com.altcraft.altcraftmobile.data.AppPreferenses
import com.altcraft.altcraftmobile.data.AppPreferenses.getAnonJWT
import com.altcraft.altcraftmobile.data.AppPreferenses.getConfig
import com.altcraft.altcraftmobile.data.AppPreferenses.getInAppSubscriptionStatus
import com.altcraft.altcraftmobile.data.AppPreferenses.getInternalAuthStatus
import com.altcraft.altcraftmobile.data.AppPreferenses.getNotificationData
import com.altcraft.altcraftmobile.data.AppPreferenses.getRegJWT
import com.altcraft.altcraftmobile.data.AppPreferenses.getSubscribeSettings
import com.altcraft.altcraftmobile.data.AppPreferenses.getSubscriptionStatus
import com.altcraft.altcraftmobile.data.AppPreferenses.getTokenUpdateTime
import com.altcraft.altcraftmobile.data.AppPreferenses.setInAppSubscriptionStatus
import com.altcraft.altcraftmobile.data.AppPreferenses.setInternalAuthStatus
import com.altcraft.altcraftmobile.data.AppPreferenses.setNotificationData
import com.altcraft.altcraftmobile.data.AppPreferenses.setTokenUpdateTime
import com.altcraft.altcraftmobile.event.EventReceiver
import com.altcraft.altcraftmobile.functions.app.Formatter.formatDate
import com.altcraft.altcraftmobile.functions.app.SubFunction.loadImage
import com.altcraft.sdk.AltcraftSDK
import com.altcraft.sdk.data.models.CoreModels.ProfileModel.ProfileData
import com.altcraft.sdk.push.push
import com.altcraft.sdk.data.models.CoreModels.ResponseModel.BaseResponseData
import com.altcraft.sdk.data.models.CoreModels.ResponseModel.ResponseWithHttpCode
import com.altcraft.sdk.data.models.CoreModels.SDKEventModel.Event
import com.altcraft.sdk.inapp.inApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Date

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val status = mutableStateOf(UNSUBSCRIBED)
    val token = mutableStateOf<String?>(null)
    val provider = mutableStateOf<String?>(null)
    val providerList = mutableStateOf<List<String>>(listOf())
    val updateTokenTime = mutableStateOf<String?>(null)
    val userName = mutableStateOf<String?>(null)
    val anonJWT = mutableStateOf("")
    val regJWT = mutableStateOf("")

    val authStatus = mutableStateOf(false)
    val inAppStatus = mutableStateOf(UNSUBSCRIBED)
    val inAppPlacementCount = mutableIntStateOf(0)

    val profileActions = mutableStateOf(false)
    val profileData = mutableStateOf<ProfileData?>(null)
    val profileDataIsLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)

    val configSetting = mutableStateOf("")
    val subscribeActions = mutableStateOf(false)
    val targetActions = mutableStateOf(false)
    val subscribeSettings = mutableStateOf(AppDataClasses.SubscribeSettings.getDefault())

    val eventList = mutableStateOf<List<Event>>(emptyList())

    /** Maximum number of log records kept in the event list. */
    private val maxLogSize = 1000

    val newFieldKey = mutableStateOf("")
    val newFieldValue = mutableStateOf("")

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

        authStatus.value = getInternalAuthStatus(context)
        inAppStatus.value = getInAppSubscriptionStatus(context)
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

    private fun handleIncomingEvent(event: Event, context: Context) {
        eventList.value = eventList.value.toMutableList().apply {
            add(event)
            if (size > maxLogSize) subList(0, size - maxLogSize).clear()
        }

        when (event.event) {
            AltcraftSDK.push.moduleEvents.subscribeRequestSuccessful,
            AltcraftSDK.push.moduleEvents.suspendRequestSuccessful,
            AltcraftSDK.push.moduleEvents.unsubscribeRequestSuccessful,
            AltcraftSDK.push.moduleEvents.statusRequestSuccess -> {
                status.value = handleStatusUpdate(context, event)
            }

            AltcraftSDK.push.moduleEvents.tokenUpdateRequestSuccessful -> {
                handleTokenUpdate(context, event.date)
                updateConfigUI(context)
            }

            AltcraftSDK.sdkEvents.moduleEvents.pushStatusRequestFailed -> {
                val responseWithHttp = event.eventValue?.get(
                    RESPONSE_WITH_HTTP
                ) as? ResponseWithHttpCode<*>

                val baseResponse = responseWithHttp?.response as? BaseResponseData
                val errorText = baseResponse?.errorText

                if (errorText?.contains(PROFILE_NOT_FOUND) == true) {
                    status.value = UNSUBSCRIBED
                    AppPreferenses.setSubscriptionStatus(context, UNSUBSCRIBED)
                }

                errorMessage.value =
                    "Profile information is not available. Error: ${event.eventMessage.orEmpty()}"
            }

            AltcraftSDK.sdkEvents.moduleEvents.authenticateSuccessful -> {
                authStatus.value = true
                setInternalAuthStatus(context, true)
            }

            AltcraftSDK.sdkEvents.moduleEvents.userLogOut -> {
                authStatus.value = false
                setInternalAuthStatus(context, false)
            }

            AltcraftSDK.inApp.moduleEvents.subscribeRequestSuccessful -> {
                inAppStatus.value = SUBSCRIBED
                setInAppSubscriptionStatus(context, SUBSCRIBED)
            }

            AltcraftSDK.inApp.moduleEvents.unsubscribeRequestSuccessful -> {
                inAppStatus.value = UNSUBSCRIBED
                setInAppSubscriptionStatus(context, UNSUBSCRIBED)
            }

            AltcraftSDK.inApp.moduleEvents.statusRequestSuccessful -> {
                inAppStatus.value = handleInAppStatusUpdate(event).also {
                    setInAppSubscriptionStatus(context, it)
                }
            }

            AltcraftSDK.inApp.moduleEvents.placementsRequestSuccessful -> {
                val smidS = event.eventValue
                    ?.get("all_smid") as? List<*>

                inAppPlacementCount.intValue = smidS
                    ?.filterIsInstance<String>()
                    ?.size
                    ?: 0
            }

            AltcraftSDK.sdkEvents.moduleEvents.configIsSet -> {
                updateUI(context)
            }

            AltcraftSDK.sdkEvents.moduleEvents.sdkCleared -> {
                token.value = null
                provider.value = null
                status.value = UNSUBSCRIBED
                inAppStatus.value = UNSUBSCRIBED
                authStatus.value = false

                AppPreferenses.clearSdkIndicatorStatuses(context)
            }


            else -> return
        }
    }

    private fun handleInAppStatusUpdate(event: Event): String {
        return try {
            val profile = event.eventValue?.get("profile") as? ProfileData
            profile?.subscription?.status ?: UNSUBSCRIBED
        } catch (_: Exception) {
            UNSUBSCRIBED
        }
    }

    private fun handleStatusUpdate(context: Context, event: Event): String {
        return try {
            val profile = event.eventValue?.get("profile") as? ProfileData

            (profile?.subscription?.status ?: UNSUBSCRIBED).also {
                AppPreferenses.setSubscriptionStatus(context, it)
            }
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

    internal fun updateTokenUI(context: Context) {
        CoroutineScope(Dispatchers.Main).launch {
            val current = AltcraftSDK.push.token.getToken(
                context
            )

            provider.value = current?.provider
            token.value = current?.token
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

    fun loadProfileStatus(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            profileDataIsLoading.value = true
            errorMessage.value = null

            profileData.value = null

            CoroutineScope(Dispatchers.Main).launch {
                val status = AltcraftSDK.push.subscription.status.current(
                    context
                )
                profileData.value = status?.response?.profile
                profileDataIsLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        AltcraftSDK.sdkEvents.subscription.unsubscribe()
    }
}