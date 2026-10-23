package com.altcraft.sdk.inapp.util

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.

/**
 * Utility for building in-app scripts and messages.
 */
internal object StringBuilder {

    /**
     * Builds JavaScript for sending protected fullscreen insets to the in-app content.
     *
     * @param top Top protected inset.
     * @param bottom Bottom protected inset.
     * @return JavaScript code that dispatches the fringeSize native-event.
     */
    fun fringeSizeInAppScript(
        top: Int,
        bottom: Int
    ) = """
        window.dispatchEvent(new CustomEvent('native-event', {
            detail: {
                type: 'fringeSize',
                data: [$top, $bottom]
            }
        }));
    """.trimIndent()

    /**
     * Builds an in-app image load error message.
     *
     * @param failedUrls Image URLs that failed to preload.
     * @return Message with failed URLs joined by `;`.
     */
    fun inAppImagesErrorMessage(failedUrls: List<String>) =
        "image preload failed for: ${failedUrls.joinToString(";")}"
}
