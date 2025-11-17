# Altcraft Android SDK

![Altcraft SDK Logo](https://guides.altcraft.com/img/logo.svg)

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9%2B-blue?style=flat-square)](https://kotlinlang.org/)
[![Platforms](https://img.shields.io/badge/Platforms-Android-green?style=flat-square)](https://developer.android.com/)
[![Push Providers](https://img.shields.io/badge/Push-Firebase_Huawei_RuStore-orange?style=flat-square)](#)
[![Maven Central](https://img.shields.io/badge/dynamic/xml?url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Fcom%2Faltcraft%2Fandroid-sdk%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Frelease&label=Maven%20Central)](https://central.sonatype.com/artifact/com.altcraft/android-sdk)


Altcraft Android SDK is a library for managing push notifications, user profiles, and interacting with the **Altcraft Marketing platform**. The SDK automates push notification delivery, event submission, request retries, and supports flexible workflows with JWT or role-based tokens.

---

## Features

* [x] Compatible with Kotlin and Java.
* [x] Works with anonymous and registered users; supports multiple profiles on one device (JWT).
* [x] Push subscription management: push subscribe, push suspend, push unsubscribe.
* [x] Automatic display of push notifications configured in the platform.
* [x] Automatic push token update when it changes.
* [x] Automatic transmission of notification delivery and open events.
* [x] Mobile events registration.
* [x] Automatic retry of failed requests.
* [x] Support for push providers: APNS, Firebase, Huawei.
* [x] Secure requests using JWT and flexible identifier matching.
* [x] Support for rToken for simple subscription scenarios.
* [x] SDK data cleanup and background tasks termination.

---

## Dependency

`implementation("com.altcraft:sdk:1.0.1")` 

---

## Authorization Types

### JWT-Authorization (recommended approach)

JWT is added to the header of every request. The SDK requests the current token from the app via the `JWTInterface`.

**Advantages:**

* Enhanced security of API requests.
* Profile lookup by any identifier (email, phone, custom ID).
* Support for multiple users on a single device.
* Profile persists after app reinstallation.
* Unified user identity across devices.

### Authorization with a role token (*rToken*)

The role token is specified in the SDK configuration.
Profile lookup is limited to the push token identifier.

**Limitations:**

* The link to a profile may be lost if the push token changes and the change isn’t reflected on the platform.
* No multi-profile support.
* It’s not possible to register the same user on another device.

---

## Requirements

* Android 7.1+ (API 25).
* Push providers integrated SDK (Firebase, Huawei, RuStore).
* `Application` class in the project.

---

## Documentations

Detailed information on SDK setup, functionality, and usage is available on the Altcraft documentation portal. You can navigate to the required section using the links below:

- [**Quick Start**](https://guides.altcraft.com/en/developer-guide/sdk/v1/android/quick-start)
- [**SDK Functionality**](https://guides.altcraft.com/en/developer-guide/sdk/v1/android/functionality)
- [**SDK Configuration**](https://guides.altcraft.com/en/developer-guide/sdk/v1/android/setup)
- [**Classes and Structures**](https://guides.altcraft.com/en/developer-guide/sdk/v1/android/api)

**Provider Setup**

- [FCM](https://guides.altcraft.com/en/developer-guide/sdk/v1/android/providers/fcm/)
- [HMS](https://guides.altcraft.com/en/developer-guide/sdk/v1/android/providers/hms/)
- [RuStore](https://guides.altcraft.com/en/developer-guide/sdk/v1/android/providers/rustore/)

---

## License

### EULA

END USER LICENSE AGREEMENT

Copyright © 2024 Altcraft LLC. All rights reserved.

1. LICENSE GRANT
   This agreement grants you certain rights to use the Altcraft Mobile SDK (hereinafter referred to as the “Software”).
   All rights not expressly granted by this agreement remain with the copyright holder.

2. USE
   You are permitted to use and distribute the Software for both commercial and non-commercial purposes.

3. MODIFICATION WITHOUT PUBLICATION
   You may modify the Software for your own internal purposes without any obligation to publish such modifications.

4. MODIFICATION WITH PUBLICATION
   Publication of modified Software requires prior written permission from the copyright holder.

5. DISCLAIMER OF WARRANTIES
   The Software is provided “as is,” without any warranties, express or implied, including but not limited to
   warranties of merchantability, fitness for a particular purpose, and non-infringement of third-party rights.

6. LIMITATION OF LIABILITY
   Under no circumstances shall the copyright holder be liable for any direct, indirect, incidental, special,
   punitive, or consequential damages (including but not limited to: procurement of substitute goods or services;
   loss of data, profits, or business interruption) arising in any way from the use of this Software,
   even if the copyright holder has been advised of the possibility of such damages.

7. DISTRIBUTION
   When distributing the Software, you must provide all recipients with a copy of this license agreement.

8. COPYRIGHT AND THIRD-PARTY COMPONENTS
   This Software may include components distributed under other licenses. The full list of such components
   and their respective licenses is provided below:

### Third-party libraries and licenses

  **Apache License 2.0**  
Licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).  
You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

* [Kotlin](https://github.com/JetBrains/kotlin)
* [KotlinX Serialization](https://github.com/Kotlin/kotlinx.serialization)
* [KotlinX Coroutines](https://github.com/Kotlin/kotlinx.coroutines)
* [Retrofit](https://github.com/square/retrofit)
* [OkHttp](https://github.com/square/okhttp)
* [AndroidX Room](https://developer.android.com/jetpack/androidx/releases/room)
* [WorkManager](https://developer.android.com/jetpack/androidx/releases/work)
* [AndroidX Lifecycle](https://developer.android.com/jetpack/androidx/releases/lifecycle)
* [AndroidX Test](https://developer.android.com/testing)
* [Material Components for Android](https://github.com/material-components/material-components-android)
* [MockK](https://mockk.io/)

**Other licenses:**
* [Glide](https://github.com/bumptech/glide) — **BSD-2-Clause** 
* [JUnit 4](https://junit.org/junit4/) — **EPL-1.0 (Eclipse Public License 1.0)**
* [Google Play Services Ads Identifier](https://developers.google.com/android/reference/com/google/android/gms/ads/identifier/AdvertisingIdClient) — subject to [Google Play Services SDK License Agreement](https://developers.google.com/terms)
