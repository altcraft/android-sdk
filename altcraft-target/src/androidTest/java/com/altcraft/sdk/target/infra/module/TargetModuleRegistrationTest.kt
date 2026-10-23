@file:Suppress("SpellCheckingInspection")

package com.altcraft.sdk.target.infra.module

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.altcraft.sdk.core.modules.ModuleRegistry
import com.altcraft.sdk.target.data.Constants
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TargetModuleRegistrationTest
 *
 * Verifies that the target module registers itself with core and
 * delegates module lifecycle callbacks.
 */
@RunWith(AndroidJUnit4::class)
class TargetModuleRegistrationTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /** The startup initializer registers the target module. */
    @Test
    fun moduleInitializer_registersTargetModule() {
        mockkObject(ModuleRegistry)

        ModuleInitializer().create(context)

        verify(exactly = 1) { ModuleRegistry.register(TargetModule) }
        assertTrue(ModuleInitializer().dependencies().isEmpty())
    }

    /** The target module uses the expected identifier. */
    @Test
    fun targetModule_exposesExpectedModuleId() {
        assertEquals(Constants.MODULE_NAME, TargetModule.moduleId)
    }

    /** The target module delegates lifecycle callbacks to ModuleLifecycle. */
    @Test
    fun targetModule_delegatesLifecycleToModuleLifecycle() = runBlocking {
        mockkObject(ModuleLifecycle)
        coEvery { ModuleLifecycle.onSdkInitialized(context) } returns Unit
        coEvery { ModuleLifecycle.onSdkClear(context) } returns Unit

        TargetModule.onSdkInitialized(context)
        TargetModule.onSdkClear(context)

        coVerify(exactly = 1) { ModuleLifecycle.onSdkInitialized(context) }
        coVerify(exactly = 1) { ModuleLifecycle.onSdkClear(context) }
    }
}
