package com.altcraft.altcraftmobile.providers.jwt

//  Created by Andrey Pogodin.
//
//  Copyright © 2025 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.altcraftmobile.anonJWT
import com.altcraft.altcraftmobile.data.AppPreferenses.getAnonJWT
import com.altcraft.altcraftmobile.data.AppPreferenses.getAuthStatus
import com.altcraft.altcraftmobile.data.AppPreferenses.getRegJWT
import com.altcraft.altcraftmobile.regJWT
import com.altcraft.sdk.interfaces.JWTInterface
import kotlinx.coroutines.runBlocking

class JWTProvider(private val context: Context) : JWTInterface {
    private suspend fun fetchJwt(context: Context): String? {

        //jwt receipt delay test
        // delay(2000)

        return when (getAuthStatus(context)) {
            true -> regJWT ?: getRegJWT(context)
            false -> anonJWT ?: getAnonJWT(context)
        }
    }

    override fun getJWT(): String? = runBlocking {
        fetchJwt(context)
    }
}