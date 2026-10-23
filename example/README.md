# README ALTCRAFT ANDROID EXAMPLE APP

Demo application for the modular **Altcraft Android SDK**. It demonstrates SDK initialization, push notifications (FCM, HMS, RuStore), in-app messages, target (mobile) events, profile requests, and JWT authorization.

## SDK Modules Used

The example depends on the following SDK modules (see `example/build.gradle.kts`):

* `altcraft-core` — initialization, configuration, authentication, networking;
* `altcraft-push` — push subscriptions, tokens, events, notification rendering;
* `altcraft-inapp` — in-app messages (WebView-based placements);
* `altcraft-target` — target (mobile) events;
* `altcraft-profile` — profile field updates.

Alternatively, the modules can be consumed from Maven as `com.altcraft:android-sdk-core|push|inapp|target|profile`.

---

## Application Preparation

* Clone the repository.
* Open the project in Android Studio.

The project was developed with:

* Android Studio Narwhal Feature Drop | 2025.1.2
* Kotlin 1.9.22
* AGP 8.10.1
* minSdk 25, targetSdk 35

### Push providers

Provider configuration files are already included in the repository, and the corresponding Gradle plugins are already enabled in `example/build.gradle.kts` — you can build and test FCM and HMS right away.

To connect the example app to your own push projects:

**FCM**

* Replace `example/google-services.json` with the file of your Firebase project.
* The `com.google.gms.google-services` plugin is already enabled in `build.gradle.kts`.

**HMS**

* Replace `example/agconnect-services.json` with the file of your Huawei project.
* The `com.huawei.agconnect` plugin is already enabled in `build.gradle.kts`.

**RuStore**

* The RuStore client is initialized in `App.onCreate()` (`app/src/main/java/com/altcraft/altcraftmobile/App.kt`) via `RuStoreUniversalPushClient.init(...)` — replace the demo `projectId` with your own.
* For notifications to work properly, the RuStore application must be installed on the device.

---

## SDK Initialization

### Programmatic initialization (App class)

The SDK is initialized in `App.onCreate()` (`App.kt`). Registration of providers is performed first, then the configuration:

```kotlin
// 1. JWT provider (see "JWT Tokens" below)
AltcraftSDK.auth.setJWTProvider(JWTProvider(this))

// 2. Push token providers
AltcraftSDK.push.token.setFCMTokenProvider(FCMProvider())
AltcraftSDK.push.token.setHMSTokenProvider(HMSProvider())
AltcraftSDK.push.token.setRuStoreTokenProvider(RuStoreProvider())

// 3. Configuration and initialization
val configuration = AltcraftConfiguration.Builder(
    apiUrl = "https://your-altcraft-server.com",
    enableLogging = true,
    appInfo = AppInfo(appVer = "v1.0.0"),
)
    .pushConfig(
        icon = R.drawable.ic_altcraft_push_icon,
        providerPriority = listOf(FCM_PROVIDER)
    )
    .inAppConfig(
        inAppAutoRequest = true,
        placementsRequestInterval = 10.minutes,
        maxPlacements = 10
    )
    .build()

AltcraftSDK.initialization(this, configuration)
```

### Initialization from the application UI

Alternatively, the SDK can be initialized through the **Config** screen of the application: the entered parameters are stored in SharedPreferences and applied on the next launch by `SDKFunctions.initAltcraft()`.

---

## Setting JWT Tokens Without Using the Application UI

* Open the `App` class of the application module.
* Set the JWT token value for the anonymous users database in the `anonJWT` variable.
* Set the JWT token value for the registered users database in the `regJWT` variable.

The same tokens can be set later through the Config screen.

---

## Build and Install

**Option 1 — Android Studio.** Select the `example` configuration, pick a device/emulator, and press **Run**.

**Option 2 — Gradle.** Connect a device or start an emulator, then:

```bash
./gradlew :example:installDebug
```

This builds the APK and installs it on the connected device automatically.

**Option 3 — build only.** To just build the APK without installing:

```bash
./gradlew :example:assembleDebug
```

The artifact is written to `example/build/outputs/apk/debug/`.

---

## Application Navigation

The bottom menu contains five sections: **Home**, **Example**, **Logs**, **InApp**, **Config**.

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

**Actions** (top row):

* **Subscribe** — subscription management panel:

  * **push** — Subscribe / Unsubscribe for push notifications;
  * **inApp** — Subscribe / Unsubscribe for in-app messages (`AltcraftSDK.inApp.subscription`);
  * **Log In** — switches the JWT token from anonymous to registered and transfers/reactivates the subscription under the registered profile;
  * **Log Out** — switches the JWT token from registered to anonymous and transfers/reactivates the subscription under the anonymous profile.
* **Profile** — requests and displays profile information (loading and error states are handled).
* **Target** — sends a target (mobile) event: enter `sid` and event `name`, then press **mobile_event_send** (`AltcraftSDK.target.sendTarget`); **mobile_event_close** closes the panel.
* **Clear** — clears SDK data, stops SDK tasks, and clears SDK events in the Main Events section.

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

### InApp

This section demonstrates in-app message triggers and placement management.

Elements:

* **Placements** — requests current in-app placements from the backend (`AltcraftSDK.inApp.getPlacements`); the badge shows the number of loaded placements;
* trigger cells — each fires the corresponding in-app trigger:

  * **Modal** (`tr_modal`);
  * **Fullscreen** (`tr_fullscreen`);
  * **Floating Bar** (`tr_floating_bar`);
  * **SlideUp** (`tr_slide_up`);
  * **Custom** (`tr_custom`).

Switching application screens is reported to the SDK (`AltcraftSDK.inApp.setScreen`) — placement display conditions can depend on the current screen.

---

### Config

This section allows configuring SDK parameters, JWT tokens, and push subscription settings.

* **Config setting**:

  * "x" button — clear the entered information;
  * "+" button — save the entered information;
  * API url field — set the `apiUrl` configuration parameter (required);
  * RToken field — set the `rToken` configuration parameter (used for role token authorization);
  * Providers panel — set the priority of push providers (FCM, HMS, RuStore);
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
  * **Fields & Cats values** — configure subscription fields and categories:

    * **C.Fields** — subscription custom fields ("+" button — add a new field);
    * **P.Fields** — profile fields ("+" button — add a new field);
    * **Cats** — subscription categories ("+" button — add a new category).

---

## Deep Links

The application registers two deep link schemes:

| Link                      | Behavior                               |
|---------------------------|----------------------------------------|
| `altcraft.main://home`    | Opens the Home screen                  |
| `altcraft.main://example` | Opens the Example screen               |
| `altcraft.main://logs`    | Opens the Logs screen                  |
| `altcraft.main://config`  | Opens the Config screen                |
| `altcraft.deep://...`     | Opens `DeepLinkActivity` (demo screen) |

Example:

```bash
adb shell am start -a android.intent.action.VIEW -d "altcraft.main://config"
```

---

**Note**: When using JWT authorization, first specify and save JWT tokens, then save the configuration parameters.

---
