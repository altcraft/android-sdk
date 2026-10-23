@file:Suppress("SpellCheckingInspection")

package test.profile

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

import android.content.Context
import com.altcraft.sdk.AltcraftSDK
import com.altcraft.sdk.profile.Profile
import com.altcraft.sdk.profile.platform.updateFields.UpdateFieldsProcess
import com.altcraft.sdk.profile.profile
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

/**
 * ProfileRoutingTest
 *
 * Verifies that the public profile API delegates to the update-fields process.
 *
 * Positive scenarios:
 *  - `updateProfileFields` forwards profile fields and the skip-triggers flag.
 *  - the SDK extension exposes the [Profile] facade.
 */
class ProfileRoutingTest {

    private val context: Context = mockk(relaxed = true)

    @Before
    fun setUp() {
        mockkObject(UpdateFieldsProcess)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /** The public API forwards fields and skip-triggers to the process. */
    @Test
    fun updateProfileFields_delegatesToUpdateFieldsProcess() {
        val fields = mapOf("email" to "a@b.co", "age" to 30)

        Profile.updateProfileFields(
            context = context,
            profileFields = fields,
            skipTriggers = true
        )

        verify(exactly = 1) {
            UpdateFieldsProcess.updateFields(
                context = context,
                profileFields = fields,
                skipTriggers = true
            )
        }
    }

    /** The public API forwards nulls unchanged. */
    @Test
    fun updateProfileFields_delegatesWithDefaults() {
        Profile.updateProfileFields(context = context)

        verify(exactly = 1) {
            UpdateFieldsProcess.updateFields(
                context = context,
                profileFields = null,
                skipTriggers = null
            )
        }
    }

    /** The SDK extension resolves to the profile facade. */
    @Test
    fun altcraftSdkProfile_extensionResolvesToProfile() {
        assertSame(Profile, AltcraftSDK.profile)
    }
}
