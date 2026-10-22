# README ALTCRAFT ANDROID EXAMPLE APP

## Application Preparation

* Clone the repository.
* Open the project in Android Studio.

The project was developed with:

* Kotlin 1.9.22
* AGP 8.10.1

The `example` module depends on the SDK as a local module:
`implementation(project(":altcraft-sdk"))`.

---

### Preparing the application for FCM notification testing

* Add the `google-services.json` file to the `example` module folder.
* In the `build.gradle.kts` file of the `example` module, uncomment the following plugin:

  ```kotlin
  alias(libs.plugins.google.gms.google.services)
  ```

### Preparing the application for HMS notification testing

* Add the `agconnect-services.json` file to the `example` module folder.
* In the `build.gradle.kts` file of the `example` module, uncomment the following plugin:

  ```kotlin
  id("com.huawei.agconnect")
  ```

### Preparing the application for RuStore notification testing

* In the `App` class (`App.onCreate()`), uncomment the client initialization line and specify your project ID:

  ```kotlin
  RuStorePushClient.init(this, "your_rustore_project_id")
  ```

* For notifications to work properly, the RuStore application must be installed on the device.

---

## SDK Initialization

The example app registers the JWT provider and all push providers in `App.onCreate()`,
then initializes the SDK with the configuration saved by the user:

* Open the app, go to the **Config** section, fill in **API url** (required) and
  optional parameters, and press **"+"** — the SDK is initialized immediately,
  no restart required.

You can also initialize the SDK programmatically without using the UI:

```kotlin
// SDK Configuration
val config = AltcraftConfiguration.Builder(
    apiUrl = "https://your-api-url",
    providerPriorityList = listOf(
      FCM_PROVIDER, 
      HMS_PROVIDER, 
      RUS_PROVIDER
    )
).build()

// Initialization
AltcraftSDK.initialization(this, config)
```

For the full list of configuration parameters, refer to the SDK README.

---

## Setting JWT Tokens Without Using the Application UI

* Open the `App` class of the example module.
* Set the JWT token value for the anonymous users database in the `anonJWT` variable.
* Set the JWT token value for the registered users database in the `regJWT` variable.

Values set in code take priority over the values saved in the Config screen.

---

## Notification Permission

The app requests the system notification permission on launch
(`AltcraftSDK.requestNotificationPermission`). On Android 13+ confirm the request,
otherwise push notifications will not be displayed.

---

## Install the Application

After completing the preparation steps, install the application on your device.

**Option 1 — Android Studio.** Select the `example` configuration, pick a
device/emulator, and press Run.

**Option 2 — Gradle.** Connect a device or start an emulator, then:

```
./gradlew :example:installDebug
```

This builds the APK and installs it on the connected device automatically.

**Option 3 — Build only.** To just build the APK without installing:

```
./gradlew :example:assembleDebug
```

The artifact is written to `example/build/outputs/apk/debug/`.

---

## Application Navigation

The application consists of four sections (**Home**, **Example**, **Logs**,
**Config**) available from the bottom navigation bar.

### Deep links

The app supports deep links:

* `altcraft.main://home`, `altcraft.main://example`, `altcraft.main://logs`,
  `altcraft.main://config` — open the app on the corresponding section;
* `altcraft.deep://` — opens a demonstration screen used as a push action target.

---

### Home

This section of the application:

* displays the current token, token update date, username, subscription status, and SDK events;
* allows executing main SDK requests using UI elements.

Elements:

* provider module cards (top left corner): **fcm**, **hms**, **rustore** — each card
  shows whether the provider is currently active; tapping a card makes this provider
  the highest-priority one;
* information cards (top of the screen): active push token (last 10 characters),
  token update time, and current username;
* subscription status indicator:
  * green — subscription active (subscribed);
  * yellow — subscription suspended (suspended);
  * red — unsubscribed or not yet created (unsubscribed).

**Actions section**:

* **Subscribe to push** — opens the subscription management screen:

  * Subscribe — subscribes to push notifications;
  * Suspend — suspends the subscription;
  * Unsubscribe — cancels the subscription;
  * Log In — switches the JWT token from anonymous to registered → calls `unSuspend()`
    → subscribes if the current profile has no subscription;
  * Log Out — switches the JWT token from registered to anonymous → calls `unSuspend()`
    → subscribes if the current profile has no subscription.

* **Get profile status** — displays profile information.
* **Update device token** — deletes the current push token → requests a new one
  → updates the profile.
* **Clear SDK cache** — clears SDK data, stops SDK tasks, and clears SDK events
  in the Main Events section.

**Main Events section** — displays SDK events (event code, message, date).

---

### Example

This section allows creating a test push notification with the SDK builder
and sending it to the system notification panel.

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

The created notification settings are saved and restored between app launches.

---

### Logs

This section contains the list of SDK messages extracted from events
(message and timestamp). If there are no events yet, the section shows
"No events yet".

---

### Config

This section allows configuring SDK parameters, JWT tokens, and push subscription
settings.

* **Config setting**:

  * "x" button — clear the saved configuration;
  * "+" button — save the entered information and initialize the SDK with it;
  * API url field — set the `apiUrl` configuration parameter (required);
  * RToken field — set the `rToken` configuration parameter (used for role token
    authorization);
  * Providers panel — add providers (**FCM**, **HMS**, **Rustore** buttons) and
    remove them (tap a chip); the order of the chips defines the provider priority.

* **JWT setting**:

  * "x" button — clear the saved JWT tokens;
  * "+" button — save the entered information;
  * Anon.JWT field — set the JWT token for anonymous users;
  * Reg.JWT field — set the JWT token for registered users.

* **Subscribe setting**:

  * sync switch — synchronous / asynchronous request;
  * replace switch — suspends other subscriptions in the database;
  * skipTriggers switch — if enabled, the created subscription will ignore
    campaign triggers;
  * **Fields & Cats values** — configure subscription fields
    (custom subscription fields and profile fields) and subscription categories:

    * **C.Fields** — subscription custom fields:

      * "+" button — add a new field (key/value).

    * **P.Fields** — profile fields:

      * "+" button — add a new field (key/value).

    * **Cats** — subscription categories:

      * "+" button — add a new category.

---

**Note**: When using JWT authorization, first specify and save JWT tokens,
then save the configuration parameters.
