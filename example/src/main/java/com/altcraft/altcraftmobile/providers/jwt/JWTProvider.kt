package com.altcraft.altcraftmobile.providers.jwt

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.altcraftmobile.anonJWT
import com.altcraft.altcraftmobile.data.AppPreferenses.getAnonJWT
import com.altcraft.altcraftmobile.data.AppPreferenses.getExternalAuthStatus
import com.altcraft.altcraftmobile.data.AppPreferenses.getRegJWT
import com.altcraft.altcraftmobile.regJWT
import com.altcraft.sdk.platform.authenticate.external.jwt.JWTInterface
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

class JWTProvider(private val context: Context) : JWTInterface {
    private suspend fun fetchJwt(context: Context): String? {

        //jwt receipt delay test
         //delay(2.seconds)

        return when (getExternalAuthStatus(context)) {
            true -> regJWT ?: getRegJWT(context)
            false -> anonJWT ?: getAnonJWT(context)
        }
    }

    override fun getJWT(): String? = runBlocking {
        fetchJwt(context)
    }
}