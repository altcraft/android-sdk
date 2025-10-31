# README ALTCRAFT ANDROID EXAMPLE APP

## Application Preparation

* Clone the repository.
* Open the project in Android Studio.

The project was developed with:

* Android Studio Mertkan Feature Drop | 2024.3.2 Patch 1
* Kotlin 1.9.22
* AGP 8.10.1

---

### Preparing the application for FCM notification testing

* Add the `google-services.json` file to the `example` module folder.
* In the `build.gradle.kts` file of the application module, uncomment the following plugin:

  ```kotlin
  alias(libs.plugins.google.gms.google.services)
  ```

### Preparing the application for HMS notification testing

* Add the `agconnect-services.json` file to the `example` module folder.
* In the `build.gradle.kts` file of the application module, uncomment the following plugin:

  ```kotlin
  id("com.huawei.agconnect")
  ```

### Preparing the application for RuStore notification testing

* Initialize the RuStore client in the `Application.onCreate()` function of the app module by specifying the project ID.
* For notifications to work properly, the RuStore application must be installed on the device.

---

## SDK Initialization Without Using the Application UI

You can create a configuration variable and initialize the SDK without using the UI:

```kotlin
// SDK Configuration
val config = AltcraftConfiguration.Builder(
    apiUrl = "your-api-url"
).build()

// Initialization
AltcraftSDK.initialization(this, config)
```

For more details about the SDK configuration and parameter descriptions, refer to the SDK README.

---

## Setting JWT Tokens Without Using the Application UI

* Open the `App` class of the application module.
* Set the JWT token value for the anonymous users database in the `anonJWT` variable.
* Set the JWT token value for the registered users database in the `regJWT` variable.

---

## Install the Application

After completing the preparation steps, install the application on your device.

---

## Application Navigation

### Home

This section of the application:

* displays the current token, token update date, username, subscription status, and SDK events;
* allows executing main SDK requests using UI elements.

Elements:

* buttons for dynamic switching of the priority push provider (FCM, HMS, RuStore) — top left corner;
* information about the active push token, last update date, and current username — top of the screen;
* subscription status indicator:

  * green — subscription active (subscribed);
  * yellow — subscription suspended (suspended);
  * red — unsubscribed or not yet created (unsubscribed).

**Actions section**:

* **Subscribe to push** — opens the subscription management screen:

  * Subscribe — subscribes to push notifications;
  * Suspend — suspends the subscription;
  * Unsubscribe — cancels the subscription;
  * Log In — switches the JWT token from anonymous to registered → calls `unSuspend()` → subscribes if `unSuspend()` returned `null`;
  * Log Out — switches the JWT token from registered to anonymous → calls `unSuspend()` → subscribes if `unSuspend()` returned `null`.
* **Get profile status** — displays profile information.
* **Update device token** — deletes the current push token → requests a new one → updates the profile.
* **Clear SDK cache** — clears SDK data, stops SDK tasks, and clears SDK events in the Main Events section.

**Main Events section** — displays SDK events (event code, message, date).

---

### Example

This section allows creating a test push notification with the SDK builder and sending it to the system notification panel.

Elements:

* preview of the notification rendered by the builder (top of the screen);
* **Text setting**:

  * "x" button — clear the entered information;
  * "+" button — save the entered information;
  * set title;
  * set body.
* **Image setting**:

  * "x" button — clear the entered information;
  * "+" button — load and save an image by the provided link;
  * set small image — enter a link to load the image;
  * set large image (banner) — enter a link to load the image.
* **Buttons setting**:

  * "x" button — remove created notification buttons;
  * "+" button — create a new notification button.
* **Send push** button — sends the created notification to the system notification panel.

---

### Logs

This section contains the list of SDK messages extracted from events.

---

### Config

This section allows configuring SDK parameters, JWT tokens, and push subscription settings.

* **Config setting**:

  * "x" button — clear the entered information;
  * "+" button — save the entered information;
  * API url field — set the `apiUrl` configuration parameter (required);
  * RToken field — set the `rToken` configuration parameter (used for role token authorization);
  * Providers panel — set the priority of push providers;
  * S.Msg field — if set, the SDK will use foreground services for subscription and token update requests, displaying a system notification with the specified text while running.
* **JWT setting**:

  * "x" button — clear the entered information;
  * "+" button — save the entered information;
  * Anon.JWT field — set the JWT token for anonymous users;
  * Reg.JWT field — set the JWT token for registered users.
* **Subscribe setting**:

  * sync switch — synchronous / asynchronous request;
  * replace switch — subscription inclusivity flag;
  * skipTriggers switch — if enabled, the created subscription will ignore campaign triggers;
  * **Fields & Cats values** — configure subscription fields (custom subscription fields and profile fields) and subscription categories:

    * **C.Fields** — subscription custom fields:

      * "+" button — add a new field.
    * **P.Fields** — profile fields:

      * "+" button — add a new field.
    * **Cats** — subscription categories:

      * "+" button — add a new category.

---

**Note**: When using JWT authorization, first specify and save JWT tokens, then save the configuration parameters.

---
