# README Altcraft Android SDK

![Altcraft SDK Logo](https://guides.altcraft.com/img/logo.svg)

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9%2B-blue?style=flat-square)](https://kotlinlang.org/)
[![Platforms](https://img.shields.io/badge/Platforms-Android-green?style=flat-square)](https://developer.android.com/)
[![Push Providers](https://img.shields.io/badge/Push-Firebase_Huawei_RuStore-orange?style=flat-square)](#)
[![Maven Central](https://img.shields.io/badge/dynamic/xml?url=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Fcom%2Faltcraft%2Fandroid-sdk%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Frelease&label=Maven%20Central)](https://central.sonatype.com/artifact/com.altcraft/android-sdk)


Altcraft Android SDK is a library for managing push notifications, user profiles, and interacting with the **Altcraft Marketing** platform.
The SDK automates push notification delivery, event submission, request retries, and supports flexible workflows with JWT or role-based tokens.

---

## Features

* [x] Works with anonymous and registered users; supports multiple profiles on one device (JWT).
* [x] Push subscription management: `pushSubscribe()`, `pushSuspend()`, `pushUnSubscribe()`.
* [x] Automatic display of platform-configured push notifications with access to all `data` keys.
* [x] Automatic push token update when it changes.
* [x] Automatic transmission of notification delivery and open events.
* [x] Mobile events registration.
* [x] Automatic retry of failed requests.
* [x] Support for push providers: Firebase, Huawei, RuStore.
* [x] Request security via JWT and flexible ID matching.
* [x] `rToken` support for simple subscription scenarios.
* [x] Clearing SDK data (`clear()`) and stopping background tasks.

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

## Dependency

`implementation("com.altcraft:sdk:1.0.0")` 

---

## Installation Guides

* [Altcraft SDK](https://guides.altcraft.com/en/developer-guide/sdk/)
* [Firebase Cloud Messaging (FCM) integration](https://guides.altcraft.com/en/developer-guide/sdk/android/)

---

## License

Altcraft Android SDK is distributed under the **MIT** license.

### Third-party libraries and licenses

**Apache License 2.0**

* [Kotlin](https://github.com/JetBrains/kotlin)
* [KotlinX Serialization](https://github.com/Kotlin/kotlinx.serialization)
* [KotlinX Coroutines](https://github.com/Kotlin/kotlinx.coroutines)
* [Glide](https://github.com/bumptech/glide)
* [Retrofit](https://github.com/square/retrofit)
* [OkHttp](https://github.com/square/okhttp)
* [AndroidX Room](https://developer.android.com/jetpack/androidx/releases/room)
* [WorkManager](https://developer.android.com/jetpack/androidx/releases/work)
* [AndroidX Lifecycle](https://developer.android.com/jetpack/androidx/releases/lifecycle)
* [AndroidX Test](https://developer.android.com/testing)
* [Material Components for Android](https://github.com/material-components/material-components-android)
* [JUnit](https://junit.org/junit4/)
* [MockK](https://mockk.io/)

**Other licenses**

* [Java JWT (Auth0)](https://github.com/auth0/java-jwt) — MIT

---

