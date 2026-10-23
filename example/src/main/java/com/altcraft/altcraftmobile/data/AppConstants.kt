package com.altcraft.altcraftmobile.data

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

object AppConstants {
    const val EMPTY_VALUE_UI = "........"

    //altcraft providers names
    const val FCM_PROVIDER: String = "android-firebase"
    const val HMS_PROVIDER: String = "android-huawei"
    const val RUSTORE_PROVIDER: String = "android-rustore"

    //status
    internal const val SUBSCRIBED: String = "subscribed"
    internal const val UNSUBSCRIBED: String = "unsubscribed"
    internal const val SUSPENDED: String = "suspended"

    //token card text
    const val TOKEN_CARD_TEXT_FCM = "fcm token:"
    const val TOKEN_CARD_TEXT_HMS = "hms token:"
    const val TOKEN_CARD_TEXT_RUSTORE = "rustore token:"
    const val TOKEN_CARD_TEXT_EMPTY = "no providers:"

    //status msg
    const val PROFILE_NOT_FOUND = "profile not found"
    const val RESPONSE_WITH_HTTP = "response_with_http_code"

    //config setting default - App info defaults
    const val APP_ID = "AltcraftMobile"
    const val APP_IID = "1.0.0"
    const val APP_VER = "1.0.0"

    //notification setting default  -
    const val EXAMPLE_TITLE = "Altcraft"
    const val EXAMPLE_BODY = "Welcome to the Altcraft Notification Builder!"
}