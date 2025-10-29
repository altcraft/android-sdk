package com.altcraft.altcraftmobile

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.app.Application
import com.altcraft.altcraftmobile.event.EventReceiver
import com.altcraft.altcraftmobile.functions.sdk.SDKFunctions.initAltcraft
import com.altcraft.altcraftmobile.providers.jwt.JWTProvider
import com.altcraft.altcraftmobile.providers.fcm.FCMProvider
import com.altcraft.altcraftmobile.providers.hms.HMSProvider
import com.altcraft.altcraftmobile.providers.rustore.RuStoreProvider
import com.altcraft.sdk.AltcraftSDK
import ru.rustore.sdk.pushclient.RuStorePushClient

val icon = R.drawable.ic_altcraft_label

//set the JWT value for the anonymous user as anonJWT or in the application interface (config)
internal val anonJWT: String? = null

//set the JWT value for the registered user as regJWT or in the application interface (config)
internal val regJWT: String? = null

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        /**
         * Altcraft push setup
         *
         * 1. Place provider config files in the app module root:
         * * app/google-services.json  (Firebase Cloud Messaging)
         * * app/agconnect-services.json  (Huawei Push Kit)
         *
         * 2. In app/build.gradle.kts, uncomment the plugins and sync Gradle:
         * plugins {
         * ```
        alias(libs.plugins.android.application)
        ```
         * ```
        alias(libs.plugins.kotlin.android)
        ```
         * ```
        id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
        ```
         * ```
        alias(libs.plugins.google.gms.google.services) // uncomment
        ```
         * ```
        id("com.huawei.agconnect") // uncomment
        ```
         * }
         *
         * 3. Sync Gradle to apply changes.
         */

        //Initialize the Rustore client by specifying your ProjectID
        // RuStorePushClient.init(this, "your_rustore_project_id")

        EventReceiver.init(this)
        AltcraftSDK.setJWTProvider(JWTProvider(this))
        AltcraftSDK.pushTokenFunctions.setFCMTokenProvider(FCMProvider())
        AltcraftSDK.pushTokenFunctions.setHMSTokenProvider(HMSProvider())
        AltcraftSDK.pushTokenFunctions.setRuStoreTokenProvider(RuStoreProvider())

        //Initialization of the SDK that accepts parameters from the application interface
        initAltcraft(this)

        /**
         *
         * You can configure Altcraft SDK in the app UI (Config) or initialize it programmatically.
         *
         * Example (programmatic init):
         *
         * val config = AltcraftConfiguration.Builder(
         *     apiUrl = "https://pxl-example.altcraft.com",
         *     icon   = R.drawable.ic_notification,
         *     rToken = null,
         *     usingService   = true,
         *     serviceMessage = "Processing Altcraft operations…",
         *     appInfo = DataClasses.AppInfo(
         *         appID  = "com.example.app",
         *         appIID = "8b91f3a0-1111-2222-3333-c1a2c1a2c1a2",
         *         appVer = "1.0.0"
         *     ),
         *     providerPriorityList = listOf(
         *         FCM_PROVIDER, // "android-firebase"
         *         HMS_PROVIDER, // "android-huawei"
         *         RUS_PROVIDER  // "android-rustore"
         *     ),
         *     pushReceiverModules = listOf(
         *         context.packageName,
         *         "com.example.push_receiver",
         *         "com.example.feature.test"
         *     ),
         *     pushChannelName        = "Altcraft",
         *     pushChannelDescription = "Altcraft notifications channel"
         *  ).build()
         *
         *  AltcraftSDK.initialization(context, config)
         *
         *
         *
         * Example of implementing sdk functions in an app:
         * package com.altcraft.altcraftmobile.functions.sdk
         */
    }
}