package com.altcraft.altcraftmobile.functions.app

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import com.altcraft.altcraftmobile.R
import com.altcraft.altcraftmobile.data.AppConstants.FCM_PROVIDER
import com.altcraft.altcraftmobile.data.AppConstants.HMS_PROVIDER
import com.altcraft.altcraftmobile.data.AppConstants.RUSTORE_PROVIDER
import com.altcraft.altcraftmobile.data.AppConstants.TOKEN_CARD_TEXT_EMPTY
import com.altcraft.altcraftmobile.data.AppConstants.TOKEN_CARD_TEXT_FCM
import com.altcraft.altcraftmobile.data.AppConstants.TOKEN_CARD_TEXT_HMS
import com.altcraft.altcraftmobile.data.AppConstants.TOKEN_CARD_TEXT_RUSTORE

object DataPreparation {
    fun getIconByProvider(provider: String): Int {
        return when (provider) {
            FCM_PROVIDER -> R.drawable.ic_fcm
            HMS_PROVIDER -> R.drawable.ic_hms_logo_2
            RUSTORE_PROVIDER -> R.drawable.ic_rus
            else -> R.drawable.ic_fcm
        }
    }

    fun getTokenCardTextByProvider(provider: String): String {
        return when (provider) {
            FCM_PROVIDER -> TOKEN_CARD_TEXT_FCM
            HMS_PROVIDER -> TOKEN_CARD_TEXT_HMS
            RUSTORE_PROVIDER -> TOKEN_CARD_TEXT_RUSTORE
            else -> TOKEN_CARD_TEXT_EMPTY
        }
    }

    fun getModuleStatusByProvider(module: String, provider: String?): String {
        return  if (module == provider) {return "on"} else "off"
    }
}