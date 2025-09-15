# README ALTCRAFT ANDROID SDK

---

## Содержание README ALTCRAFT ANDROID SDK

* Виды авторизации API-запросов

  * JWT-авторизация (рекомендуемый способ)
  * Авторизация с использованием ролевого токена Altcraft

* Предварительные условия

* Установка зависимости Altcraft SDK

* Подготовка приложения

  * Реализация интерфейсов SDK

    * JWTInterface
    * FCMInterface
    * HMSInterface
    * RustoreInterface
  * Переопределение сервисов push-провайдеров

    * FirebaseMessagingService
    * HmsMessageService
    * RuStoreMessagingService

* Инициализация SDK

  * Конфигурация SDK
  * Выполнение инициализации SDK

* Получение событий SDK в приложении

  * Функции объекта Events

    * subscribe(newSubscriber: (DataClasses.Event) -> Unit)
    * unsubscribe()

* Работа со статусами подписки

  * Изменение статуса подписки

    * pushSubscribe(...)
    * pushSuspend(...)
    * pushUnSubscribe(...)
    * unSuspendPushSubscription(...)
  * Запрос статуса подписки

    * getStatusOfLatestSubscription(...)
    * getStatusForCurrentSubscription(...)
    * getStatusOfLatestSubscriptionForProvider(...)
  * Передача функциональных полей профиля

    * actionField(key: String)

* Работа с пуш-провайдерами

  * Функции объекта pushTokenFunctions

    * setPushToken(...)
    * getPushToken(...)
    * setFCMTokenProvider(...)
    * setHMSTokenProvider(...)
    * setRuStoreTokenProvider(...)
    * deleteDeviceToken(...)
    * forcedTokenUpdate(...)
    * changePushProviderPriorityList(...)

* Ручная регистрация push-событий

  * Функции объекта PublicPushEventFunctions

    * deliveryEvent(...)
    * openEvent(...)

* Передача push-уведомления в SDK

  * Класс PushReceiver

    * takePush(...)
    * pushHandler(...)
  * Переопределение класса PushReceiver

* Очистка данных SDK

  * clear(...)

* Дополнительные функции SDK

  * reinitializePushModuleInThisSession()
  * requestNotificationPermission(...)

* Публичные функции и классы SDK

  * object AltcraftSDK (обзор API)
  * object DataClasses
  * class AltcraftConfiguration

---
  
## Виды авторизации API-запросов

Взаимодействие между клиентом (приложением) и сервером Altcraft осуществляется с использованием одного из двух способов авторизации API-запросов.

### JWT-авторизация (рекомендуемый способ)

Данный тип авторизации использует JWT-токен, который приложение передаёт в SDK. Токен добавляется в заголовок каждого запроса.

**JWT (JSON Web Token)** — это строка в формате JSON, содержащая claims (набор данных), подписанных для проверки подлинности и целостности.

<br>

Токен формируется и подписывается ключом шифрования на стороне серверной части клиента (ключи шифрования не хранятся в приложении). По запросу SDK, приложение обязано передать полученный с сервера JWT токен. 

**Преимущества:**

* Повышенная безопасность API-запросов.
* Возможность поиска профилей по любым идентификаторам (email, телефон, custom ID).
* Поддержка нескольких пользователей на одном устройстве.
* Восстановление доступа к профилю после переустановки приложения.
* Идентификация конкретного профиля на разных устройствах.

### Авторизация с использованием ролевого токена Altcraft

Альтернативный способ авторизации — использование ролевого токена (*rToken*), переданного в параметры конфигурации SDK.
В этом случае запросы содержат заголовок с ролевым токеном.

**Особенности:**

* Поиск профилей возможен только по push-токену устройства (например, FCM).
* Если push-токен изменился и не был передан на сервер (например, после удаления и переустановки приложения), связь с профилем будет потеряна / создастся новый профиль.

**Ограничения:**

* Потеря связи с профилем при изменении push-токена, которое не было зафиксировано на сервере Altcraft.
* Отсутствие возможности использовать приложение для разных профилей на одном устройстве.
* Невозможность регистрации одного пользователя на другом устройстве.


## Предварительные условия

- SDK провайдеров push-уведомлений интегрированы в проект приложения (см. инструкции по интеграции push провайдеров).
- в приложении добавлен класс, расширяющий Application

## Установка зависимости Altcraft SDK
    
- Добавьте зависимость библиотеки в файл build.gradle.kts уровня приложения (app level)
    
```kotlin
    dependencies {
        implementation("com.altcraft:android-sdk:0.0.1")
    }
 ```
- выполните синхронизацию изменений Gradle


## Подготовка приложения

### Реализация интерфейсов SDK

   SDK содержит публичные интерфейсы которые могут быть реализованы на стороне приложения: 
   
> Регистрация провайдеров должна происходить в Application.onCreate() такая точка регистрации гарантирует раннюю, однократную и детерминированную регистрацию при старте процесса, в том числе в в фоновом режиме.
   
#### JWTInterface:

Интерфейс запроса JWT-токена. Предоставляет актуальный JWT-токен из приложения по запросу SDK. Реализация данного интерфейса требуется, если используется JWT-аутентификация API-запросов. JWT подтверждает, что пользовательские идентификаторы аутентифицированы приложением.
Реализация JWT-аутентификации обязательна, если используется тип [матчинга](https://guides.altcraft.com/user-guide/profiles-and-databases/matching/#peculiarities-of-matching) отличный от push-данных из подписки (например, идентификатор пользователя — email или телефон).

**Обратите внимание**
`getJWT()` — синхронная функция. Поток выполнения SDK будет приостановлен до получения JWT. Рекомендуется, чтобы `getJWT()` возвращал значение немедленно — из кэша (in-memory, `SharedPreferences` или `EncryptedSharedPreferences`) — это ускорит выполнение запросов. Желательно подготовить актуальный JWT как можно раньше (на старте приложения) и сохранить его в кэш, чтобы при обращении SDK токен был доступен без задержек. При отсутствии значения допустимо вернуть `null`.

---

Протокол SDK:

```kotlin
interface JWTInterface {
    fun getJWT(): String?
}
```

---

Реализация на стороне приложения:

```
import android.content.Context
import com.altcraft.sdk.interfaces.JWTInterface

class JWTProvider(
    private val context: Context // добавьте свойство context, если это необходимо
) : JWTInterface {
    override fun getJWT(): String? {
        // ваш код, возвращающий актуальный JWT токен
    }
}
```

---

Регистрация провайдера в Application.onCreate():

```
import android.app.Application
import com.altcraft.sdk.AltcraftSDK

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AltcraftSDK.setJWTProvider(JWTProvider(applicationContext))
    }
}
```

#### Интерфейсы запроса и удаления push токена

**FCMInterface** — Интерфейс запроса и удаления push-токена FCM.

---

**Интерфейс SDK:**

```kotlin
interface FCMInterface {
    suspend fun getToken(): String?
    suspend fun deleteToken(completion: (Boolean) -> Unit)
}
```

---

**Рекомендуемая реализация на стороне приложения:**

```
import com.altcraft.sdk.interfaces.FCMInterface
import com.google.firebase.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.tasks.await

class FCMProvider : FCMInterface {

    override suspend fun getToken(): String? = try {
        Firebase.messaging.token.await()
    } catch (e: Exception) {
        null
    }

    override suspend fun deleteToken(completion: (Boolean) -> Unit) {
        try {
            FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener {
                completion(it.isSuccessful)
            }
        } catch (e: Exception) {
            completion(false)
        }
    }
}
```

---

**Регистрация провайдера в `Application.onCreate()`:**

```
import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AltcraftSDK.pushTokenFunctions.setFCMTokenProvider(FCMProvider())
    }
}
```

---

**HMSInterface** — Интерфейс запроса и удаления push-токена HMS.

---

**Интерфейс SDK:**

```
import android.content.Context

interface HMSInterface {
    suspend fun getToken(context: Context): String?
    suspend fun deleteToken(context: Context, complete: (Boolean) -> Unit)
}
```

---

**Рекомендуемая реализация на стороне приложения:**

```
import android.content.Context
import com.altcraft.sdk.interfaces.HMSInterface
import com.huawei.agconnect.AGConnectOptionsBuilder
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.api.HuaweiApiAvailability

private const val APP_ID = "client/app_id"
private const val TOKEN_SCOPE = "HCM"

class HMSProvider : HMSInterface {

    override suspend fun getToken(context: Context): String? = try {
        val availability = HuaweiApiAvailability.getInstance()
            .isHuaweiMobileServicesAvailable(context)
        if (availability != com.huawei.hms.api.ConnectionResult.SUCCESS) return null

        val appId = AGConnectOptionsBuilder().build(context).getString(APP_ID)
        HmsInstanceId.getInstance(context).getToken(appId, TOKEN_SCOPE)
    } catch (e: Exception) {
        null
    }

    override suspend fun deleteToken(context: Context, complete: (Boolean) -> Unit) {
        try {
            val appId = AGConnectOptionsBuilder().build(context).getString(APP_ID)
            HmsInstanceId.getInstance(context).deleteToken(appId, TOKEN_SCOPE)
            complete(true)
        } catch (e: Exception) {
            complete(false)
        }
    }
}
```

---

**Регистрация провайдера в `Application.onCreate()`:**

```
import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AltcraftSDK.pushTokenFunctions.setHMSTokenProvider(HMSProvider())
    }
}
```

---

**RustoreInterface** — Интерфейс запроса и удаления push-токена RUSTORE.

---

**Интерфейс SDK:**

```kotlin
interface RustoreInterface {
    suspend fun getToken(): String?
    suspend fun deleteToken(complete: (Boolean) -> Unit)
}
```

---

**Рекомендуемая реализация на стороне приложения:**

```
import com.altcraft.sdk.interfaces.RustoreInterface
import kotlinx.coroutines.CompletableDeferred
import ru.rustore.sdk.core.feature.model.FeatureAvailabilityResult
import ru.rustore.sdk.pushclient.RuStorePushClient

class RuStoreProvider : RustoreInterface {

    override suspend fun getToken(): String? {
        val deferred = CompletableDeferred<String?>()
        try {
            val token = RuStorePushClient.getToken().await()
            RuStorePushClient.checkPushAvailability()
                .addOnSuccessListener { result ->
                    when (result) {
                        FeatureAvailabilityResult.Available -> deferred.complete(token)
                        is FeatureAvailabilityResult.Unavailable -> deferred.complete(null)
                    }
                }
                .addOnFailureListener { deferred.complete(null) }
        } catch (e: Exception) {
            return null
        }
        return deferred.await()
    }

    override suspend fun deleteToken(complete: (Boolean) -> Unit) {
        try {
            RuStorePushClient.deleteToken()
                .addOnSuccessListener { complete(true) }
                .addOnFailureListener { complete(false) }
        } catch (e: Exception) {
            complete(false)
        }
    }
}
```

---

**Регистрация провайдера в `Application.onCreate()`:**

```
import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AltcraftSDK.pushTokenFunctions.setRuStoreTokenProvider(RuStoreProvider())
    }
}
```

> Реализуйте интерфейсы только для тех push-провайдеров, которые используются в вашем проекте. Регистрацию (установку) классов, реализующих интерфейсы провайдеров (`FCMInterface` / `HMSInterface` / `RustoreInterface`), выполняйте в `Application.onCreate()`. Это гарантирует доступность провайдеров с момента старта процесса приложения (foreground/background) и корректную работу до вызова `AltcraftSDK.initialization(...)`.

### Переопределение сервисов push провайдеров

Входящие push-уведомления доставляются в сервис выбранного push-провайдера и обрабатываются в его колбэк функции onMessageReceived(...). Выполните передачу уведомления (его полезной нагрузки) в SDK c помощью функции AltcraftSDK.PushReceiver.takePush(context, message.data). 

---

#### FirebaseMessagingService 

- рекомендованная реализация FCMService с передачей уведомления в функцию takePush():

```
import com.altcraft.sdk.AltcraftSDK
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * FCM service for handling push tokens and messages.
 */
class FCMService : FirebaseMessagingService() {

    /**
     * Called when a new FCM token is generated.
     *
     * @param token The new FCM token.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    /**
     * Called when a push message is received.
     *
     * Forwards the message to all receivers with additional metadata.
     *
     * @param message The received [RemoteMessage].
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

       AltcraftSDK.PushReceiver.takePush(this@FCMService, message.data)
    }
}
```

---

#### HmsMessageService

 - рекомендованная реализация HMSService с передачей уведомления в функцию takePush():

```
import com.altcraft.sdk.AltcraftSDK
import com.huawei.hms.push.HmsMessageService
import com.huawei.hms.push.RemoteMessage

/**
 * HMS service for handling push tokens and incoming notifications.
 *
 * Extends [HmsMessageService] and overrides key HMS callback methods.
 */
class HMSService : HmsMessageService() {
    /**
     * Called when a new HMS token is generated.
     *
     * @param token The new HMS token.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    /**
     * Called when a push message is received from HMS.
     *
     * Forwards the message with additional metadata to all receivers.
     *
     * @param message The received [RemoteMessage].
     */
    override fun onMessageReceived(message: RemoteMessage) {
        AltcraftSDK.PushReceiver.takePush(this@HMSService, message.dataOfMap)
    }
}
```

---

#### RuStoreMessagingService

 - рекомендованная реализация RuStoreService с передачей уведомления в функцию takePush():

```
import com.altcraft.sdk.AltcraftSDK
import ru.rustore.sdk.pushclient.messaging.model.RemoteMessage
import ru.rustore.sdk.pushclient.messaging.service.RuStoreMessagingService

/**
 * RuStore service for handling push notifications.
 *
 * Extends [RuStoreMessagingService] and overrides key callbacks.
 */
class RuStoreService : RuStoreMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    /**
     * Called when a push message is received.
     *
     * Forwards the message to all receivers with added metadata.
     *
     * @param message The received RemoteMessage.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        AltcraftSDK.PushReceiver.takePush(this, message.data)
    }
}
```

#### Регистрация сервисов push провайдеров в AndroidManifest.xml приложения

Сервисы должны быть зарегистрированы в AndroidManifest.xml приложения (см. инструкцию подключения push провайдеров в проект):

```
<!-- FCM service -->
<service
    android:name="<your_package_name>.FCMService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>

<!-- HMS service -->
<service
    android:name="<your_package_name>.HMSService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.huawei.push.action.MESSAGING_EVENT" />
    </intent-filter>
</service>

<!-- RuStore service -->
<service
    android:name="<your_package_name>.RuStoreService"
    android:exported="true"
    tools:ignore="ExportedService">
    <intent-filter>
        <action android:name="ru.rustore.sdk.pushclient.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

Создайте и зарегистрируйте сервисы, которые требуются в вашем проекте. 

## Инициализация SDK

### Конфигурация SDK

Для передачи параметров конфигурации используется класс `AltcraftConfiguration`:

```kotlin
class AltcraftConfiguration private constructor(
    private val apiUrl: String,
    private val icon: Int? = null,                        // иконка уведомлений
    private val rToken: String? = null,                   // ролевой токен Altcraft
    private val usingService: Boolean = false,            // foreground-сервисы при подписке/обновлении токена
    private val serviceMessage: String? = null,           // текст уведомления foreground-сервисов/WM
    private val appInfo: DataClasses.AppInfo? = null,     // метаданные Firebase Analytics
    private val providerPriorityList: List<String>? = null,   // приоритет провайдеров push
    private val pushReceiverModules: List<String>? = null,    // модули с AltcraftPushReceiver
    private val pushChannelName: String? = null,              // имя канала уведомлений
    private val pushChannelDescription: String? = null        // описание канала уведомлений
)
```

**Описание параметров:**


* **apiUrl** — (обязательный параметр) URL конечной точки **Altcraft API**. (обязательный параметр)

* **icon** —  (опциональный параметр) идентификатор ресурса `drawable`, используемого как иконка уведомлений.

* **rToken** — (опциональный параметр) ролевой токен Altcraft (идентифицирует ресурс/БД/аккаунт). Используется, если единственный тип матчинга — push-токен устройства, выданный провайдером (например, FCM).

* **usingService** — (опциональный параметр) включает использование foreground-сервисов при оформлении подписки и обновлении push-токена. Дает до \~1 минуты гарантированного сетевого окна даже при сворачивании/закрытии приложения (Android-требование: показывать уведомление сервиса). В большинстве случаев не обязателен, но полезен при нестабильной сети.

* **serviceMessage** — (опциональный параметр) текст уведомления foreground-сервисов и задач WorkManager (если `null`, будет `"background process"`). Рекомендуется задать явно — уведомления могут отображаться в фоне при обработке контента push.

* **appInfo** — (опциональный параметр) базовые метаданные приложения для Firebase Analytics. 

  - (опциональный параметр)

  Для установки используйте публичный `data class` SDK `AppInfo` (пакет `com.altcraft.sdk.data.DataClasses`):

  ```kotlin
  data class AppInfo(
      /** Firebase app_id */
      val appID: String,
      /** Firebase app_instance_id */
      val appIID: String,
      /** Firebase app_version */
      val appVer: String
  )
  ```

* **providerPriorityList** — (опциональный параметр) список строковых имён провайдеров push-уведомлений Altcraft. SDK предоставляет публичные константы:

  ```kotlin
  const val FCM_PROVIDER: String = "android-firebase"
  const val HMS_PROVIDER: String = "android-huawei"
  const val RUS_PROVIDER: String = "android-rustore"
  ```

  Параметр **`providerPriorityList`** задаёт приоритет использования push-провайдеров.

  * Используется для **автоматического обновления push-токена подписки**, если токен более приоритетного провайдера недоступен.
  * Приоритет определяется **индексом в списке**: элемент с индексом **0** — самый приоритетный.

  **Пример:**

  ```kotlin
  providerPriorityList = listOf(
      FCM_PROVIDER,
      HMS_PROVIDER,
      RUS_PROVIDER
  )
  ```

  * SDK сначала запросит токен **FCM**; если FCM недоступен — **HMS**; если HMS недоступен — **RuStore**.
  * Работает при условии, что в приложении реализованы интерфейсы соответствующих провайдеров.

  **По умолчанию**, если параметр не указан:

  ```
  FCM_PROVIDER → HMS_PROVIDER → RUS_PROVIDER
  ```

  * Список может содержать **один элемент** — в этом случае будет использоваться только один провайдер, независимо от доступности токена.
  * Параметр можно не указывать, если:

    * в проекте используется только один провайдер,
    * или приоритет по умолчанию соответствует требованиям.
  * Полезен для **быстрого перехода** на нужного провайдера при инициализации SDK.

  **Кейсы:**

  1. Приоритет RUS_PROVIDER → FCM_PROVIDER. Пользователь удаляет RuStore — уведомления RuStore недоступны. При рекомендованной реализации RustoreInterface.getToken() вернёт `null`, SDK автоматически переключится на **FCM**, обновит токен подписки, коммуникации сохранятся.
  2. Параметр не задан, действуют дефолты `FCM → HMS → RuStore`. На устройстве Huawei без GMS SDK автоматически перейдёт на **HMS** без дополнительного кода.

* **pushReceiverModules** — (опциональный параметр) список имён пакетов, в которых находятся пользовательские реализации `AltcraftPushReceiver : AltcraftSDK.PushReceiver()`. Если указаны, SDK обнаружит эти классы и передаст им входящее уведомление (через `pushHandler(context: Context, message: Map<String, String>)`). Если классы не найдены ни в одном из пакетов, уведомление будет показано средствами SDK.

Пример создания класса AltcraftPushReceiver:

```
package com.altcraft.altcraftmobile.test

import android.content.Context
import androidx.annotation.Keep
import com.altcraft.sdk.AltcraftSDK

@Keep
class AltcraftPushReceiver: AltcraftSDK.PushReceiver() {
    override fun pushHandler(context: Context, message: Map<String, String>) {
        
        //выполните обработку уведомления с помощью функций SDK
        super.pushHandler(context, message)
        
        //ваша дополнительная логика обработки уведомления 
    }
}
```

передача параметра: pushReceiverModules = listOf("com.altcraft.altcraftmobile.test")
  
**Кейс:**  Приложение содержит несколько модулей в которых необходимы данные входящих push-уведомлений Altcraft. В каждом из этих модулей можно создать класс AltcraftPushReceiver: AltcraftSDK.PushReceiver() и получить входящее push уведомление как message: Map<String, String>.

* **pushChannelName** —  (опциональный параметр) базовое имя канала push-уведомлений (видно в настройках Android). В зависимости от настроек звука/вибрации на платформе Altcraft к имени добавляется суффикс:

  * `allSignal` — звук и вибрация включены;
  * `soundless` — бесшумный канал;
  * `onlySound` — только звук (без вибрации).
    Пример: при `pushChannelName = "Altcraft"` и режиме `allSignal` видимое имя канала — `"Altcraft_allSignal"`.
    Если параметр не указан — SDK использует имена по умолчанию: `"allSignal"`, `"soundless"`, `"onlySound"`.

* **pushChannelDescription** — (опциональный параметр) описание канала (видно в настройках Android). К описанию добавляется режим:

  * `Vibration and sound enabled`;
  * `Vibration and sound disabled`;
  * `Sound enabled, vibration disabled`.
    Пример: `"Altcraft notification channel. (vibration and sound enabled)"`.
    Если параметр не указан — SDK использует значения по умолчанию.

**Значения по умолчанию:**

* `icon` — altcraft-sdk/src/main/res/drawable/icon.xml (если icon = null);
* `serviceMessage` — "background process" (если serviceMessage = null);
* `providerPriorityList` — FCM_PROVIDER → HMS_PROVIDER → RUS_PROVIDER (если null);
* `pushChannelName` — "allSignal", "soundless", "onlySound" (если null);
* `pushChannelDescription` — "Vibration and sound enabled", "Vibration and sound disabled", "Sound enabled, vibration disabled" (если null).

---

### Выполнение инициализации SDK

• **fun initialization(context: Context, configuration: AltcraftConfiguration, complete: ((Result<Unit>) -> Unit)? = null)** — функция инициализации SDK.

```kotlin
// Инициализация SDK и установка конфигурации
AltcraftSDK.initialization(
    context = context,
    configuration = config,
    complete = null // опционально
)
```

**Обратите внимание**
Вызывайте `AltcraftSDK.initialization(...)` тогда, когда это необходимо, **но после** регистрации всех провайдеров (JWT-провайдера и провайдеров push-токенов). Запросы следует выполнять **после** установки конфигурации.

**Пример правильного порядка инициализации в `Application.onCreate()` (после регистрации провайдеров):**

```
import android.app.Application
import com.altcraft.sdk.AltcraftSDK
import com.altcraft.sdk.config.AltcraftConfiguration
import com.altcraft.sdk.data.DataClasses
import com.altcraft.fcm.FCMProvider
import com.altcraft.hms.HMSProvider
import com.altcraft.rustore.RuStoreProvider
import ru.rustore.sdk.pushclient.RuStorePushClient

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        // Инициализация SDK провайдера RuStore (если используется)
        RuStorePushClient.init(this, "rustore-project-id-1234")

        // Регистрация провайдеров до инициализации SDK
        AltcraftSDK.setJWTProvider(JWTProvider(applicationContext))
        AltcraftSDK.pushTokenFunctions.setFCMTokenProvider(FCMProvider())
        AltcraftSDK.pushTokenFunctions.setHMSTokenProvider(HMSProvider())
        AltcraftSDK.pushTokenFunctions.setRuStoreTokenProvider(RuStoreProvider())

        // Конфигурация SDK
        val config = AltcraftConfiguration.Builder(
            apiUrl = "https://pxl-example.altcraft.com",
            icon   = R.drawable.ic_notification
        ).build()

        // Инициализация
        AltcraftSDK.initialization(this, config)
    }
}
```

**Пример минимальной рабочей настройки:**

```kotlin
val config = AltcraftConfiguration.Builder(
    apiUrl = "https://pxl-example.altcraft.com"
).build()

AltcraftSDK.initialization(context, config)
```

**Пример настройки всех параметров:**

```kotlin
val config = AltcraftConfiguration.Builder(
    apiUrl = "https://pxl-example.altcraft.com",
    icon   = R.drawable.ic_notification,
    rToken = null,
    usingService   = true,
    serviceMessage = "Processing Altcraft operations…",
    appInfo = DataClasses.AppInfo(
        appID  = "com.example.app",
        appIID = "8b91f3a0-1111-2222-3333-c1a2c1a2c1a2",
        appVer = "1.0.0"
    ),
    providerPriorityList = listOf(
        FCM_PROVIDER, // "android-firebase"
        HMS_PROVIDER, // "android-huawei"
        RUS_PROVIDER  // "android-rustore"
    ),
    pushReceiverModules = listOf(
        context.packageName,
        "com.example.push_receiver",
        "com.example.feature.test"
    ),
    pushChannelName        = "Altcraft",
    pushChannelDescription = "Altcraft notifications channel"
).build()

AltcraftSDK.initialization(context, config)
```

**Пример инициализации с callback завершения:**

```kotlin
AltcraftSDK.initialization(context, config) { result ->
    when {
        result.isSuccess -> {
            // действия при успешной инициализации
        }
        result.isFailure -> {
            // обработка ошибки инициализации
        }
    }
}
```

## Получение событий SDK в приложении. Функции объекта Events

```
AltcraftSDK
└── val eventSDKFunctions: Events
    // Подписаться на события SDK
    ├── fun subscribe(
    │       newSubscriber: (DataClasses.Event) -> Unit
    │   ): Unit
    // Отписаться от событий SDK
    └── fun unsubscribe(): Unit
```

**Подписка на события**

• **fun subscribe(newSubscriber: (DataClasses.Event) -> Unit): Unit** — при возникновении события SDK вызывает колбэк и передаёт в него экземпляр `DataClasses.Event` (или его наследника).
**Важно:** в приложении может быть только **один** активный подписчик; новый вызов `subscribe(...)` заменяет предыдущего.

**Пример использования:**

```kotlin
AltcraftSDK.eventSDKFunctions.subscribe { event ->
    // обработка события
}
```

**Типы событий**

Все события, передаваемые SDK, являются экземплярами `DataClasses.Event` или его наследников:

```kotlin
open class Event(
    val function: String,
    val eventCode: Int? = null,
    val eventMessage: String? = null,
    val eventValue: Map<String, Any?>? = null,
    val date: Date = Date(),
)

open class Error(
    function: String,
    eventCode: Int? = 0,
    eventMessage: String? = null,
    eventValue: Map<String, Any?>? = null,
    date: Date = Date(),
) : Event(function, eventCode, eventMessage, eventValue, date)

class RetryError(
    function: String,
    eventCode: Int? = 0,
    eventMessage: String? = null,
    eventValue: Map<String, Any?>? = null,
    date: Date = Date(),
) : Error(function, eventCode, eventMessage, eventValue, date)
```

* **Event** — общее событие (информационные/успешные запросы).
* **Error** — событие об ошибке.
* **RetryError** — событие об ошибке для запроса, который SDK будет автоматически повторять.

**Содержимое события**

Каждое событие содержит поля:

* **function** — имя функции, вызвавшей событие;
* **eventCode** — внутренний код события SDK (см. раздел «События SDK»);
* **eventMessage** — текстовое сообщение;
* **eventValue** — произвольные данные `Map<String, Any?>`, добавляемые как полезная нагрузка;
* **date** — время события.

> Пример события успешной подписки на push-уведомления:

```
├─ function: processResponseprocessResponse
├─ eventCode: 230
├─ eventMessage: "successful request: push/subscribe"
├─ eventValue
│  ├─ http code: 200
│  └─ response
│     ├─ error: 0
│     ├─ errorText: ""
│     └─ profile
│        ├─ id: "your id"
│        ├─ status: "subscribed"
│        ├─ isTest: false
│        └─ subscription
│           ├─ subscriptionId: "your subscriptionId"
│           ├─ hashId: "c52b28d2"
│           ├─ provider: "android-firebase"
│           ├─ status: "subscribed"
│           ├─ fields
│           │  ├─ _device_name: "Pixel 7"
│           │  ├─ _device_model: "Google Pixel 7"
│           │  ├─ _os_tz: "+0300"
│           │  ├─ _os_language: "ru"
│           │  ├─ _os_ver: {"raw":"14","ver":[14]}
│           │  ├─ _ad_track: true
│           │  ├─ _os: "Android"
│           │  └─ _device_type: "Mobile"
│           └─ cats
│              └─ [ { name: "developer_news", title: "dev_news", steady: false, active: false } ]
└─ date: 2025-09-03 09:01:44 +0000
```

**Отписка от событий**

• **fun unsubscribe(): Unit** — прекращает доставку событий текущему подписчику (колбэк остаётся назначенным, но события не доставляются).


## Работа со статусами подписки

### Изменение статуса подписки

**Функции управления статусом подписки — `pushSubscribe()`, `pushSuspend()`, `pushUnSubscribe()`**

```
AltcraftSDK
└─ val pushSubscriptionFunctions: PublicPushSubscriptionFunctions
   // Подписка на push-уведомления (status = SUBSCRIBED)
   ├─ fun pushSubscribe(
   │     context: Context,
   │     sync: Boolean = true,
   │     profileFields: Map<String, Any?>? = null,
   │     customFields: Map<String, Any?>? = null,
   │     cats: List<DataClasses.CategoryData>? = null,
   │     replace: Boolean? = null,
   │     skipTriggers: Boolean? = null
   │   ): Unit
   // Приостановка подписки (status = SUSPENDED)
   ├─ fun pushSuspend(
   │     context: Context,
   │     sync: Boolean = true,
   │     profileFields: Map<String, Any?>? = null,
   │     customFields: Map<String, Any?>? = null,
   │     cats: List<DataClasses.CategoryData>? = null,
   │     replace: Boolean? = null,
   │     skipTriggers: Boolean? = null
   │   ): Unit
   // Отписка (status = UNSUBSCRIBED)
   └─ fun pushUnSubscribe(
         context: Context,
         sync: Boolean = true,
         profileFields: Map<String, Any?>? = null,
         customFields: Map<String, Any?>? = null,
         cats: List<DataClasses.CategoryData>? = null,
         replace: Boolean? = null,
         skipTriggers: Boolean? = null
       ): Unit
```

* **`pushSubscribe(...)`** — подписка на push-уведомления.
* **`pushSuspend(...)`** — приостановка подписки.
* **`pushUnSubscribe(...)`** — отмена подписки.

Данные функции имеют одинаковую сигнатуру, содержащую следующие параметры:
<br><br>

#### Параметр  `context`

<br>

• `context`: Context — Android Context.
<br>

#### Параметр `sync`


<br>

• `sync`: Boolean = true — флаг синхронного выполнения запроса (по умолчанию — синхронно).

Если запрос выполнен успешно, создаётся событие с кодом **230**. Содержимое `event.value` зависит от флага `sync`:

**если флаг sync == true:** 

```
ResponseWithHttpCode
├─ code: 230
├─ message: "successful request: push/subscribe"
├─ value
│  ├─ http code: 200
│  └─ response
│     ├─ error: 0
│     ├─ errorText: ""
│     └─ profile
│        ├─ id: "your id"
│        ├─ status: "subscribed"
│        ├─ isTest: false
│        └─ subscription
│           ├─ subscriptionId: "your subscriptionId"
│           ├─ hashId: "c52b28d2"
│           ├─ provider: "android-firebase"
│           ├─ status: "subscribed"
│           ├─ fields
│           │  ├─ _device_name: "Pixel 7"
│           │  ├─ _device_model: "Google Pixel 7"
│           │  ├─ _os_tz: "+0300"
│           │  ├─ _os_language: "ru"
│           │  ├─ _os_ver: {"raw":"14","ver":[14]}
│           │  ├─ _ad_track: true
│           │  ├─ _os: "Android"
│           │  └─ _device_type: "Mobile"
│           └─ cats
│              └─ [ { name: "developer_news", title: "dev_news", steady: false, active: false } ]
└─ date: 2025-09-03 09:01:44 +0000

```

В значении события (`event.value`) по ключу **`"response_with_http_code"`** доступны:

* **httpCode** – транспортный код ответа.
* **Response** *(public struct)*, содержащий:

  * `error: Int?` — внутренний код ошибки сервера (*0, если ошибок нет*).
  * `errorText: String?` — текст ошибки (*пустая строка, если ошибок нет*).
  * `profile: ProfileData?` — **всегда равно `null`** для асинхронного запроса.


**если флаг sync = false:**

```
ResponseWithHttpCode
├─ httpCode: Int?
└─ response: Response?
    ├─ error: Int?
    ├─ errorText: String?
    └─ profile: ProfileData? = null
```

В этом случае `profile` всегда **`null`**.

**Случаи ошибки:**

Если запрос данной группы функций завершился ошибкой, будет создано событие со следующими кодами:

* **430** – ошибка без автоматического повтора на стороне SDK.
* **530** – ошибка с автоматическим повтором на стороне SDK.

Содержимое события:

* только `httpCode`, если сервер Altcraft был **недоступен**;
* `error` и `errorText`, если сервер **вернул ошибку**.


Получить значения событий функций pushSubscribe, pushSuspend, pushUnSubscribe можно следующим образом: 

```kotlin
AltcraftSDK.eventSDKFunctions.subscribe { event ->
    if (event.eventCode in listOf(230, 430, 530)) {
        (event.eventValue?.get("response_with_http_code")
                as? DataClasses.ResponseWithHttpCode)?.let { responseWithHttp ->

            // HTTP code
            val httpCode = responseWithHttp.httpCode

            // Response
            val response = responseWithHttp.response
            val error = response?.error
            val errorText = response?.errorText

            // Profile
            val profile = response?.profile
            val profileId = profile?.id
            val profileStatus = profile?.status
            val profileIsTest = profile?.isTest

            // Subscription
            val subscription = profile?.subscription
            val subscriptionId = subscription?.subscriptionId
            val hashId = subscription?.hashId
            val provider = subscription?.provider
            val subscriptionStatus = subscription?.status

            // Fields (Map<String, JsonElement>)
            val fields = subscription?.fields

            // Cats (List<CategoryData>)
            val cats = subscription?.cats
            val firstCat = cats?.firstOrNull()
            val catName = firstCat?.name
            val catTitle = firstCat?.title
            val catSteady = firstCat?.steady
            val catActive = firstCat?.active
        }
    }
}
```

<br>


#### Параметр `profileFields` 
<br>


`Map<String, Any?>?` — карта, содержащая **поля профиля**:


Параметр может принимать как **системные поля** (например, `_fname` — имя или `_lname` — фамилия), так и **опциональные** (заранее создаются вручную в интерфейсе платформы). Если передано невалидное опциональное поле, запрос завершится с ошибкой: 

```
SDK error: 430
http code: 400
error: 400
errorTxt: Platform profile processing error: with field "<имя_поля>": Incorrect field
```

* **Допустимые структуры** (JSON-совместимые):

  * **Скалярные значения**:

    * String
    * Boolean
    * Int
    * Long
    * Float
    * Double
    * null
  * **Объекты**: `Map<String, *>`
  * **Списки**: `List<*>`
  * **Массивы карт**: `Array<Map<String, *>>`
<br>


#### Параметр `customFields`
<br>


Параметр может принимать как **системные поля** (например, `_device_model` — модель устройства или `_os` — операционная система), так и **опциональные** (заранее создаются вручную в интерфейсе платформы). Если передано невалидное опциональное поле, запрос завершится с ошибкой: 

```
SDK error: 430
http code: 400
error: 400
errorText: Platform profile processing error: field "<имя_поля>" is not valid: failed convert custom field
```

* **Допустимые типы значений** (JSON-совместимые, **только скаляры**):

  * String
  * Boolean
  * Int
  * Long
  * Float
  * Double
  * null


**Обратите внимание**  Большая часть системных полей подписки автоматически собирается SDK и добавляется к запросам pushSubscribe, pushSuspend, pushUnSubscribe. К ним относятся: "_os", "_os_tz", "_os_language", "_device_type", "_device_model", "_device_name", "_os_ver", "_ad_track", "_ad_id".
<br><br>


#### Параметр `cats`
<br>


`listOf(CategoryData)` - категории подписок.

```kotlin
data class CategoryData(
    val name: String? = null,
    val title: String? = null,
    val steady: Boolean? = null,
    val active: Boolean? = null
)
```

При отправке запроса pushSubscribe, pushSuspend, pushUnSubscribe с указанием категорий используйте только поля name - имя категории и active - статус активности категории(активна / неактивна), другие поля не используются в обработке запроса. Поля title и steady заполняются при получении информации о подписке. 

Пример запроса:

```kotlin
val cats = listOf(
    DataClasses.CategoryData(name = "football", active = true),
    DataClasses.CategoryData(name = "hockey",   active = true)
)
```

Категории используемые в запросе должны быть предварительно добавлены в ресурс Altcraft платформы. Если в запросе используется поля которые не добавлены в ресурс - запрос вернется с ошибкой:

```text
SDK error: 430
http code: 400
error: 400
errorText: Platform profile processing error: field "subscriptions.cats" is not valid: category not found in resource
```
<br>

   
#### Параметр `replace`
<br>

`replace`: Boolean? - флаг при активации которого, подписки других профилей с тем же push токеном в текущей базе данных будут переведены в статус unsubscribed после успешного выполнения запроса.

<br>


#### Параметр `skipTriggers`
<br>


`skipTriggers`: Boolean? - флаг при активации которого, профиль содержащий данную подписку будет игнорироваться в триггерах. 

<br>

Пример выполнения запроса подписки на push уведомления: 

минимальная рабочая настройка - 

```kotlin
AltcraftSDK.pushSubscriptionFunctions.pushSubscribe(context)
```

передача всех доступных параметров - 

```kotlin
AltcraftSDK.pushSubscriptionFunctions.pushSubscribe(
    context = this,
    sync = true,
    profileFields = mapOf("_fname" to "Andrey", "_lname" to "Pogodin"),
    customFields  = mapOf("developer" to true),
    cats = listOf(DataClasses.CategoryData(name = "developer_news", active = true)),
    replace = false,
    skipTriggers = false
)
```

**Для pushSubscribe, pushSuspend, pushUnSubscribe предусмотрен автоматический повтор запроса со стороны SDK если http код ответа находится в диапазоне 500..599. Запрос не повторяется если код ответа в этот диапазон не входит**
    
---

•  **suspend fun unSuspendPushSubscription(context: Context): DataClasses.ResponseWithHttpCode?**

> Функцию unSuspendPushSubscription() рекомендуется применять для создания logIn, LogOut переходов. 

unSuspendPushSubscription работает следующим образом: 

 - поиск подписок с тем же push токеном, что и текущий, не относящихся к профилю на который указывает текущий токен JWT.
 - смена статуса для найденных подписок с subscribed на suspended
 - смена статуса в подписках профиля на который указывает текущий JWT с suspended на subscribed если профиль на который указывает JWT существует и в нем содержатся подписки. 
 - возврат data class ResponseWithHttpCode? где response.profile - текущий профиль на который указывает JWT или null если профиль не существует.
 
**Рекомендация (LogIn / LogOut):** сочетайте `unSuspendPushSubscription()` и `pushSubscribe()`.

* **LogIn** - Анонимный пользователь входит в приложение. Данному пользователю присвоен JWT_1 - указывающий на базу данных #1Anonymous. Выполнена подписка на push уведомления, профиль создан в базе данных #1Anonymous. Пользователь регистрируется, ему присваивается JWT_2 - указывающий на базу данных #2Registered. Вызывается функция unSuspendPushSubscription() - Подписка анонимного пользователя в базе данных #1Anonymous приостанавливается. Выполняется поиск профиля в базе данных #2Registered для восстановления подписки, но так как подписки с таким push токеном в базе данных #2Registered не существует -  функция unSuspendPushSubscription() вернет null. После получения значения null можно выполнить запрос на подписку pushSubscribe() - который создаст новый профиль в базе #2Registered. 

* **LogOut** -  пользователь выполнил выход из профиля на стороне приложения(LogOut) - пользователю присваивается JWT_1 - указывающий на базу данных #1Anonymous. Вызывается функция unSuspendPushSubscription() которая приостановит подписку базе данных в #2Registered, сменит статус подписки в #1Anonymous на subscribed. Вернет профиль #1Anonymous != null - подписка существует, новая не требуется. 


```kotlin
private suspend fun unSuspend(context: Context, logIn: Boolean) {

    // Смена JWT перед запросом
    setAuth(context, logIn)

    AltcraftSDK.pushSubscriptionFunctions
        .unSuspendPushSubscription(context)
        ?.let { result ->
            if (result.httpCode == 200 && result.response?.profile?.subscription == null) {
                AltcraftSDK.pushSubscriptionFunctions.pushSubscribe(
                    context = context
                    // передайте необходимые параметры
                )
            }
        }
}

fun logIn(context: Context)  = CoroutineScope(Dispatchers.IO).launch { unSuspend(context, true) }
fun logOut(context: Context) = CoroutineScope(Dispatchers.IO).launch { unSuspend(context, false) }
```

---

### Запрос статуса подписки

Функциями запроса статуса подписки являются - `getStatusOfLatestSubscription()`, `getStatusOfLatestSubscriptionForProvider()`, `getStatusForCurrentSubscription()`

```
AltcraftSDK
└── val pushSubscriptionFunctions: PublicPushSubscriptionFunctions
    // Статус последней подписки профиля
    ├── suspend fun getStatusOfLatestSubscription(
    │       context: Context
    │   ): DataClasses.ResponseWithHttpCode?
    // Статус подписки по текущему токену/провайдеру
    ├── suspend fun getStatusForCurrentSubscription(
    │       context: Context
    │   ): DataClasses.ResponseWithHttpCode?
    // Статус последней подписки по указанному провайдеру (если null — используется текущий)
    └── suspend fun getStatusOfLatestSubscriptionForProvider(
            context: Context,
            provider: String? = null
        ): DataClasses.ResponseWithHttpCode?
```
<br><br>
• **suspend fun getStatusOfLatestSubscription(context: Context): DataClasses.ResponseWithHttpCode?** — возвращает объект `ResponseWithHttpCode?`, содержащий `response?.profile?.subscription` (последнюю созданную подписку в профиле), если такая подписка существует, иначе `null`.

```kotlin
// Статус последней подписки профиля
AltcraftSDK.pushSubscriptionFunctions.getStatusOfLatestSubscription(context)
```
<br><br>
• **suspend fun getStatusForCurrentSubscription(context: Context): DataClasses.ResponseWithHttpCode?** — возвращает объект `ResponseWithHttpCode?`, содержащий `response?.profile?.subscription` — подписку, найденную по текущему push-токену и провайдеру. Если такой подписки нет — `null`.

```kotlin
// Статус подписки для текущего токена/провайдера
AltcraftSDK.pushSubscriptionFunctions.getStatusForCurrentSubscription(context)
```
<br><br>
• **suspend fun getStatusOfLatestSubscriptionForProvider(context: Context, provider: String? = null): DataClasses.ResponseWithHttpCode?** — возвращает объект `ResponseWithHttpCode?`, содержащий `response?.profile?.subscription` — последнюю подписку с указанным провайдером.
Если `provider == null`, используется провайдер текущего токена. При отсутствии подписки — `null`.

```kotlin
// Статус последней подписки по провайдеру (если null — используется текущий)
AltcraftSDK.pushSubscriptionFunctions.getStatusOfLatestSubscriptionForProvider(context, provider = null)
```

<br>

> Пример извлечения данных из ответа:

```kotlin
CoroutineScope(Dispatchers.IO).launch {
    AltcraftSDK.pushSubscriptionFunctions
        .getStatusForCurrentSubscription(this@App)
        ?.let { it ->
            val httpCode = it.httpCode
            val response = it.response
            val error = response?.error
            val errorText = response?.errorText
            val profile = response?.profile
            val subscription = profile?.subscription
            val cats = subscription?.cats
        }
}
```

---

### Передача функциональных полей профиля

**fun actionField(key: String): ActionFieldBuilder** — вспомогательная функция для [функционального обновления полей профиля](https://guides.altcraft.com/developer-guide/profiles/3113720/).

```
AltcraftSDK
└─ val pushSubscriptionFunctions: PublicPushSubscriptionFunctions
   └─ fun actionField(key: String): ActionFieldBuilder
```

Пример использования:

```kotlin
AltcraftSDK.pushSubscriptionFunctions.pushSubscribe(
    context = context,
    profileFields = AltcraftSDK.pushSubscriptionFunctions
        .actionField("_fname").set("Andrey")
)
```

где "_fname" - поле к которому будет применяться изменение, .set("Andrey") - команда которая установит новое значение "Andrey" для этого поля. 

Поддерживаемые операции:

```
.set(value)
.unset(value)
.incr(value)
.add(value)
.delete(value)
.upsert(value)
```


## Работа с пуш провайдерами. Функции объекта pushTokenFunctions

```
AltcraftSDK
└── val pushTokenFunctions: PublicPushTokenFunctions
    
    // Сохранить токен провайдера вручную (onNewToken)
    ├── fun setPushToken(context: Context, provider: String, token: String): Unit
    
    // Получить данные о текущем токене устройства
    ├── suspend fun getPushToken(context: Context): DataClasses.TokenData?
    
    // Зарегистрировать провайдера Firebase Cloud Messaging (null — снять)
    ├── fun setFCMTokenProvider(provider: FCMInterface?): Unit
    
    // Зарегистрировать провайдера Huawei Mobile Services (null — снять)
    ├── fun setHMSTokenProvider(provider: HMSInterface?): Unit
    
    // Зарегистрировать провайдера RuStore (null — снять)
    ├── fun setRuStoreTokenProvider(provider: RustoreInterface?): Unit
    
    // Удалить токен у выбранного провайдера
    ├── suspend fun deleteDeviceToken(context: Context, provider: String, complete: () -> Unit): Unit
    
    // Форсировать обновление токена (удалить → обновить)
    ├── fun forcedTokenUpdate(context: Context, complete: () -> Unit): Unit
    
    // Изменить приоритет провайдеров и обновить токен
    └── suspend fun changePushProviderPriorityList(context: Context, priorityList: List<String>): Unit
```

<br>

• **fun setPushToken(context: Context, provider: String, token: String): Unit** — функция предназначена для ручной установки push-токена устройства и провайдера и должна выполняться в функции `onNewToken()` сервиса пуш-провайдера. Используется как упрощённый вариант передачи токена в SDK без реализации интерфейсов провайдеров.
**Этот подход не рекомендуется. Рекомендуемый способ — реализация FCMInterface, HMSInterface, RustoreInterface.**

```kotlin
// Сохранить токен вручную (onNewToken)
AltcraftSDK.pushTokenFunctions.setPushToken(context, provider, token)
```

> Пример передачи токена в `FCMService.onNewToken()`

```kotlin
class FCMService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        
        // ручная передача токена в SDK
        AltcraftSDK.pushTokenFunctions.setPushToken(this, FCM_PROVIDER, token)
    }

    override fun onDeletedMessages() {}
    
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        AltcraftSDK.PushReceiver.takePush(this@FCMService, message.data)
    }
}
```

<br>

• **suspend fun getPushToken(context: Context): DataClasses.TokenData?** — возвращает текущие данные push-токена устройства и провайдера в виде
`data class TokenData(val provider: String, val token: String)`. Если токен недоступен — `null`.

```kotlin
// Получить данные о текущем токене устройства
AltcraftSDK.pushTokenFunctions.getPushToken(context)
```

> Пример получения токена:

```kotlin
CoroutineScope(Dispatchers.IO).launch {
    AltcraftSDK.pushTokenFunctions.getPushToken(context).let {
        val provider = it?.provider
        val token = it?.token
    }
}
```

<br>

• **fun setFCMTokenProvider(provider: FCMInterface?): Unit** — устанавливает или снимает провайдера FCM-токена. Передайте реализацию `FCMInterface` (или `null`, чтобы отключить).
Важно: вызывайте `setFCMTokenProvider()` в `Application.onCreate()` до вызова `AltcraftSDK.initialization(...)`. Это гарантирует регистрацию при старте процесса приложения, независимо от жизненного цикла компонентов.

```kotlin
// Установить провайдера Firebase Cloud Messaging (null — снять)
AltcraftSDK.pushTokenFunctions.setFCMTokenProvider(FCMProvider())
```

<br>

• **fun setHMSTokenProvider(provider: HMSInterface?): Unit** — устанавливает или снимает провайдера HMS-токена. Передайте реализацию `HMSInterface` (или `null`, чтобы отключить).
Важно: вызывайте `setHMSTokenProvider()` в `Application.onCreate()` до вызова `AltcraftSDK.initialization(...)`.

```kotlin
// Установить провайдера Huawei Mobile Services (null — снять)
AltcraftSDK.pushTokenFunctions.setHMSTokenProvider(HMSProvider())
```

<br>

• **fun setRuStoreTokenProvider(provider: RustoreInterface?): Unit** — устанавливает или снимает провайдера RuStore-токена. Передайте реализацию `RustoreInterface` (или `null`, чтобы отключить).
Важно: вызывайте `setRuStoreTokenProvider()` в `Application.onCreate()`, предварительно инициализировав клиент RuStore Push, до вызова `AltcraftSDK.initialization(...)`.

```kotlin
// Установить провайдера RuStore (null — снять)
AltcraftSDK.pushTokenFunctions.setRuStoreTokenProvider(RuStoreProvider())
```

> Рекомендованный способ регистрации провайдеров в `Application.onCreate()`:

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // set RuStore client
        RuStorePushClient.init(this, "rustore project id")
        
        // set JWT Provider
        AltcraftSDK.setJWTProvider(JWTProvider(applicationContext))
        
        // set FCM Provider
        AltcraftSDK.pushTokenFunctions.setFCMTokenProvider(FCMProvider())
        
        // set HMS Provider
        AltcraftSDK.pushTokenFunctions.setHMSTokenProvider(HMSProvider())
        
        // set RuStore Provider
        AltcraftSDK.pushTokenFunctions.setRuStoreTokenProvider(RuStoreProvider())
        
        // create AltcraftConfiguration
        val config = AltcraftConfiguration.Builder(
            apiUrl = "your api url",
            R.drawable.ic_altcraft_label
        ).build()
        
        // SDK Initialization
        AltcraftSDK.initialization(context = this@App, configuration = config)
    }
}
```

<br>

• **suspend fun deleteDeviceToken(context: Context, provider: String, complete: () -> Unit): Unit** — функция удаления push-токена указанного провайдера. Токен инвалидируется и удаляется из локального кеша и на сервере провайдера. После удаления можно запросить новый.

```kotlin
// Удалить токен у выбранного провайдера
AltcraftSDK.pushTokenFunctions.deleteDeviceToken(context, provider) {
    // callback после удаления
}
```

<br>

• **fun forcedTokenUpdate(context: Context, complete: () -> Unit): Unit** — удаляет текущий push-токен с последующим обновлением.

```kotlin
// Форсировать обновление токена (удалить → обновить)
AltcraftSDK.pushTokenFunctions.forcedTokenUpdate(context) {
    // callback после обновления
}
```

<br>

• **suspend fun changePushProviderPriorityList(context: Context, priorityList: List<String>): Unit** — функция для динамической смены приоритета провайдеров push-уведомлений с обновлением токена подписки. Для этого необходимо передать новый список приоритетов (например: `listOf(HMS_PROVIDER, RUSTORE_PROVIDER, FCM_PROVIDER)`).

```kotlin
// Изменить приоритет провайдеров и обновить токен
AltcraftSDK.pushTokenFunctions.changePushProviderPriorityList(context, listOf(HMS_PROVIDER, RUSTORE_PROVIDER, FCM_PROVIDER))
```


## Ручная регистрация push-событий. Функции объекта PublicPushEventFunctions.

```
AltcraftSDK
└── val pushEventFunction: PublicPushEventFunctions
    
    // Зафиксировать доставку Altcraft-push (вызывает delivery-ивент)
    ├── fun deliveryEvent(
    │       context: Context, 
    │       message: Map<String, String>? = null, 
    │       uid: String? = null
    │   ): Unit
    
    // Зафиксировать открытие Altcraft-push (вызывает open-ивент)
    └── fun openEvent(
            context: Context, 
            message: Map<String, String>? = null, 
            uid: String? = null
        ): Unit
```

**Обратите внимание** Использование данных функций требуется если вы сомостоятельно реализуете логику обработки уведомлений без их передачи в функцию takePush() SDK.

<br>

• **fun deliveryEvent(context: Context, message: Map\<String, String>? = null, uid: String? = null): Unit** — функция ручной регистрации события доставки уведомления Altcraft. Передайте полезные данные push-уведомления в параметр `message` или `uid` уведомления Altcraft для регистрации события доставки на сервере.

```kotlin
// Зафиксировать доставку Altcraft-push (вызывает delivery-ивент)
AltcraftSDK.pushEventFunction.deliveryEvent(context, message, uid)
```

<br>

• **fun openEvent(context: Context, message: Map\<String, String>? = null, uid: String? = null): Unit** — функция ручной регистрации события открытия уведомления Altcraft. Передайте полезные данные push-уведомления в параметр `message` или `uid` уведомления Altcraft для регистрации события открытия на сервере.

```kotlin
// Зафиксировать открытие Altcraft-push (вызывает open-ивент)
AltcraftSDK.pushEventFunction.openEvent(context, message, uid)
```

### Передача push уведомления в SDK. Класс PushReceiver 

SDK содержит классы и функции, позволяющие принять, обработать, показать push уведомление. 

Публичным классом,содержащим функцию, позволяющую принять уведомления, является класс PushReceiver.

```
AltcraftSDK
└── open class PushReceiver
    
    // Обработка входящего push-сообщения
    ├── open fun pushHandler(
    │       context: Context, 
    │       message: Map<String, String>
    │   ): Unit
    
    // Точка входа доставки push в SDK
    └── companion object
        └── fun takePush(
                context: Context, 
                message: Map<String, String>
            ): Unit
```

• **fun takePush(context: Context, message: Map<String, String>): Unit** - функция SDK принимающая push уведомления в сервисе push провайдеров для их дальнейшей обработки на стороне SDK. 

```kotlin
// Точка входа доставки push в SDK
AltcraftSDK.PushReceiver.takePush(context, message)
```

Входящие push-уведомления доставляются в сервис выбранного push-провайдера и обрабатываются в его колбэк функции onMessageReceived(...). Выполните передачу уведомления (его полезной нагрузки) в SDK в функции onMessageReceived(...) с помощью функции SDK - `takePush(context: Context, message: Map<String, String>)`. 


#### Переопределение класса PushReceiver. Получение уведомления Altcraft в любом пакете приложения 

PushReceiver является open классом содержащим open функцию pushHandler().

• **open fun pushHandler(context: Context, message: Map<String, String>): Unit** - запускает стандартный механизм обработки push-уведомления Altcraft в SDK. Функция может быть переопределена; чтобы сохранить обработку на стороне SDK, рекомендуется вызывать super.pushHandler(context, message) и добавлять свою логику до или после него. 

> Класс PushReceiver и функцию pushHandler() можно использовать для получения уведомлений Altcraft переданных в функцию takePush() в любом пакете приложения.

Для этого выполните следующие шаги:

1) Создание класса **AltcraftPushReceiver**

Создайте класс `AltcraftPushReceiver` (**имя класса должно быть именно таким**) с переопределённой функцией `pushHandler()`.

* После создания экземпляра `AltcraftSDK.PushReceiver()` обработка и показ уведомления с помощью SDK контролируется вызовом
  `super.pushHandler(context, message)` — этот вызов запускает базовую обработку push-сообщения через SDK.
* Если у вас есть один пользовательский класс `AltcraftPushReceiver` и вы **не обрабатываете push-уведомление вручную**, обязательно вызывайте
  `super.pushHandler(context, message)` — иначе уведомление **не будет показано**.
* Если вы самостоятельно обрабатываете уведомление **без использования** `super.pushHandler(context, message)`, выполните функцию
  `openEvent()` для ручной отправки события открытия после клика по push-уведомлению, иначе событие клика **не зарегистрируется** на платформе.
* Событие доставки push (`deliveryEvent`) регистрируется автоматически после вызова `takePush()`. Создание `AltcraftPushReceiver` классов на регистрацию этого события **не влияет**.
* Если у вас несколько классов `AltcraftPushReceiver`, каждый вызов `super.pushHandler(context, message)` в них создаст **своё push-уведомление**.
  → Вызывайте `super.pushHandler(context, message)` **только в одном** классе, чтобы избежать дублирования.

<br>


```
import android.content.Context
import androidx.annotation.Keep
import com.altcraft.sdk.AltcraftSDK

@Keep
class AltcraftPushReceiver : AltcraftSDK.PushReceiver() {
    override fun pushHandler(context: Context, message: Map<String, String>) {
        // базовая обработка push-сообщения и показ уведомления
        super.pushHandler(context, message)
    }
}
```

2) Добавьте имена пакетов содержащего AltcraftPushReceiver классы в параметр pushReceiverModules конфигурации. SDK автоматически определит наличие классов AltcraftPushReceiver в указанных пакетах с помощью механизма рефлексии. Обратите внимание - класс должен быть помечен аннотаций @Keep или добавлен в правила R8/ProGuard если код приложения будет обфусцироваться, иначе SDK используя рефлексию для поиска - не сможет его обнаружить. 

> пример добавления пакета в параметр pushReceiverModules конфигурации: 

```kotlin
pushReceiverModules = listOf(
   context.packageName, //пакет приложения
   "com.altcraft.altcraftmobile.test" 
)
```

## Очистка данных SDK

SDK содержит функцию clear() позволяющую выполнить очистку данных SDK и отменить работу всех, ожидающих выполнения, фоновых задач.

```
AltcraftSDK
// Полная очистка данных SDK (БД, SharedPreferences, фоновые задачи)
└── fun clear(
        context: Context,
        onComplete: (() -> Unit)? = null
    ): Unit
```

• **fun clear(context: Context, onComplete: (() -> Unit)? = null)** - удаляет записи БД Room, очищает SharedPreferences, выполняет отмену задач WorkManager.

Функция содержит необязательный callback параметр выполняющийся после завершения очистки и отмены задач.


## Дополнительные функции SDK

### Сброс флага запрещающего повторное выполнение функции performPushModuleCheck()

SDK содержит внутреннюю функцию performPushModuleCheck() которая выполняется после установки конфигурации в процессе инициализации. Данная функция выполняет запуск  фоновых задач, выполняющих контроль и повторную отправку запросов SDK, связанных с push уведомлениями, а также проверку и выполнение запроса на обновление push токена устройства. Выполнение данной функции ограничено одним запуском в пределах одного жизненного цикла процесса приложения. Могут возникнуть ситуации для которых это ограничение должно быть сброшено. Для этого выполните функцию reinitializePushModuleInThisSession().

```
AltcraftSDK
// Разрешить переинициализацию push-модуля в текущей сессии
└── fun reinitializePushModuleInThisSession(): Unit
```
• **fun reinitializePushModuleInThisSession(): Unit** - сброс флага выполнения функции performPushModuleCheck().

### Запрос разрешения на отправку уведомлений

SDK содержит функцию requestNotificationPermission(), которая используется для вызова системного диалога с запросом разрешения на показ push-уведомлений у пользователя.

Начиная с Android 13 (API 33, Tiramisu), приложения должны явно запрашивать разрешение POST_NOTIFICATIONS, прежде чем отправлять уведомления. На более ранних версиях Android вызов функции не требуется — разрешение предоставляется автоматически, и функция не выполняет действий.

```
AltcraftSDK
// Запрос системного разрешения на показ push-уведомлений
└── fun requestNotificationPermission(
        context: Context,
        activity: ComponentActivity
    ): Unit
```

• **fun requestNotificationPermission(context: Context, activity: ComponentActivity)** — выполняет проверку текущего статуса разрешения и, при необходимости, отображает пользователю системный диалог запроса разрешения. 

**Обратите внимание** Если пользователь выбрал запрет на показ уведомлений - повторный вызов функции снова выведет диалог запроса. 


## Публичные функции и классы SDK

**object AltcraftSDK** 
  
```
AltcraftSDK
// Инициализация SDK и установка конфигурации
├─ fun initialization(context: Context, configuration: AltcraftConfiguration, complete: ((Result<Unit>) -> Unit)? = null): Unit
// Полная очистка данных SDK (БД, SP, фоновые задачи)
├─ fun clear(context: Context, onComplete: (() -> Unit)? = null): Unit
// Регистрация провайдера JWT
├─ fun setJWTProvider(provider: JWTInterface?): Unit
// Разрешить переинициализацию push-модуля в текущей сессии
├─ fun reinitializePushModuleInThisSession(): Unit
// Базовый получатель Altcraft push (можно переопределить)
├─ open class PushReceiver
│  // Обработка входящего push-сообщения
│  ├─ open fun pushHandler(context: Context, message: Map<String, String>): Unit
│  // Точка входа доставки push в SDK
│  └─ companion object
│     └─ fun takePush(context: Context, message: Map<String, String>): Unit
// Публичные функции подписки
├─ val pushSubscriptionFunctions: PublicPushSubscriptionFunctions
│  // Подписка на пуш уведомление(status = SUBSCRIBED)
│  ├─ fun pushSubscribe(
│  │     context: Context,
│  │     sync: Boolean = true,
│  │     profileFields: Map<String, Any?>? = null,
│  │     customFields: Map<String, Any?>? = null,
│  │     cats: List<DataClasses.CategoryData>? = null,
│  │     replace: Boolean? = null,
│  │     skipTriggers: Boolean? = null
│  │   ): Unit
│  // Приостановка подписки на push уведомления(status = SUSPENDED)
│  ├─ fun pushSuspend(
│  │     context: Context,
│  │     sync: Boolean = true,
│  │     profileFields: Map<String, Any?>? = null,
│  │     customFields: Map<String, Any?>? = null,
│  │     cats: List<DataClasses.CategoryData>? = null,
│  │     replace: Boolean? = null,
│  │     skipTriggers: Boolean? = null
│  │   ): Unit
│  // Отписка от push уведомлегний (status = UNSUBSCRIBED)
│  ├─ fun pushUnSubscribe(
│  │     context: Context,
│  │     sync: Boolean = true,
│  │     profileFields: Map<String, Any?>? = null,
│  │     customFields: Map<String, Any?>? = null,
│  │     cats: List<DataClasses.CategoryData>? = null,
│  │     replace: Boolean? = null,
│  │     skipTriggers: Boolean? = null
│  │   ): Unit
│  // смена статуса подписки указанной в JWT с suspended на subscribed, остальные подписки содержащие указанный push токен сменят статус с subscribed на suspended. 
│  ├─ suspend fun unSuspendPushSubscription(context: Context): DataClasses.ResponseWithHttpCode?
│  // Статус последней подписки профиля
│  ├─ suspend fun getStatusOfLatestSubscription(context: Context): DataClasses.ResponseWithHttpCode?
│  // Статус последней подписки профиля по указанному провайдеру push-уведомлений
│  ├─ suspend fun getStatusOfLatestSubscriptionForProvider(context: Context, provider: String? = null): DataClasses.ResponseWithHttpCode?
│  // Статус подписки с текущим токеном устройства. 
│  ├─ suspend fun getStatusForCurrentSubscription(context: Context): DataClasses.ResponseWithHttpCode?
│  // добавить функциональное поле профиля(set/incr/...)
│  └─ fun actionField(key: String): ActionFieldBuilder
// Публичные функции управления токенами
├─ val pushTokenFunctions: PublicPushTokenFunctions
│  // Сохранить токен провайдера вручную (onNewToken)
│  ├─ fun setPushToken(context: Context, provider: String, token: String): Unit
│  // Получить текущий токен устройства
│  ├─ suspend fun getPushToken(context: Context): DataClasses.TokenData?
│  // Зарегистрировать провайдера FCM
│  ├─ fun setFCMTokenProvider(provider: FCMInterface?): Unit
│  // Зарегистрировать провайдера HMS
│  ├─ fun setHMSTokenProvider(provider: HMSInterface?): Unit
│  // Зарегистрировать провайдера RuStore
│  ├─ fun setRuStoreTokenProvider(provider: RustoreInterface?): Unit
│  // Удалить токен у выбранного провайдера
│  ├─ suspend fun deleteDeviceToken(context: Context, provider: String, complete: () -> Unit): Unit
│  // Форс-обновление токена (удалить → обновить)
│  ├─ fun forcedTokenUpdate(context: Context, complete: () -> Unit): Unit
│  // Изменить приоритет провайдеров и обновить токен
│  └─ suspend fun changePushProviderPriorityList(context: Context, priorityList: List<String>): Unit
// Публичные функции отправки событий по пушам
├─ val pushEventFunction: PublicPushEventFunctions
│  // Зафиксировать доставку Altcraft-push (вызывает delivery-ивент)
│  ├─ fun deliveryEvent(context: Context, message: Map<String, String>? = null, uid: String? = null): Unit
│  // Зафиксировать открытие Altcraft-push (вызывает open-ивент)  
│  └─ fun openEvent(context: Context, message: Map<String, String>? = null, uid: String? = null): Unit
// События SDK (один подписчик)
└─ val eventSDKFunctions: Events
   // Подписаться на события SDK
   ├─ fun subscribe(newSubscriber: (DataClasses.Event) -> Unit): Unit
   // Отписаться от событий SDK
   └─ fun unsubscribe(): Unit
```

**object DataClasses**

```
DataClasses
// Базовое SDK-событие (универсальная телеметрия)
├─ open class Event(
│    function: String,
│    eventCode: Int? = null,
│    eventMessage: String? = null,
│    eventValue: Map<String, Any?>? = null,
│    date: Date = Date()
│  )
│
│  // Ошибка, наследует Event
├─ open class Error(
│    function: String,
│    eventCode: Int? = 0,
│    eventMessage: String? = null,
│    eventValue: Map<String, Any?>? = null,
│    date: Date = Date()
│  ) : Event(...)
│
│  // Ошибка запроса, для которого предусмотрен автоматический повтор попытки со стороны SDK, наследует Error
├─ class RetryError(
│    function: String,
│    eventCode: Int? = 0,
│    eventMessage: String? = null,
│    eventValue: Map<String, Any?>? = null,
│    date: Date = Date()
│  ) : Error(...)
│
│  // Информация о приложении (идентификаторы/версия)
├─ data class AppInfo(
│    appID: String,
│    appIID: String,
│    appVer: String
│  )
│
│  // Обёртка ответа API с HTTP-кодом
├─ data class ResponseWithHttpCode(
│    httpCode: Int?,
│    response: Response?
│  )
│
│  // Ответ API: код/текст ошибки и профиль
├─ data class Response(
│    error: Int? = null,
│    @SerialName("error_text") errorText: String? = null,
│    profile: ProfileData? = null
│  )
│
│  // Данные профиля пользователя
├─ data class ProfileData(
│    id: String? = null,
│    status: String? = null,
│    @SerialName("is_test") isTest: Boolean? = null,
│    subscription: SubscriptionData? = null
│  )
│
│  // Текущая подписка профиля
├─ data class SubscriptionData(
│    @SerialName("subscription_id") subscriptionId: String? = null,
│    @SerialName("hash_id") hashId: String? = null,
│    provider: String? = null,
│    status: String? = null,
│    fields: Map<String, JsonElement>? = null,
│    cats: List<CategoryData>? = null
│  )
│
│  // Категория подписки (имя/заголовок/флаги)
├─ data class CategoryData(
│    name: String? = null,
│    title: String? = null,
│    steady: Boolean? = null,
│    active: Boolean? = null
│  )
│
│  // Токен push-провайдера устройства
└─ data class TokenData(
     provider: String,
     token: String
   )

```

**class AltcraftConfiguration**

```
com.altcraft.sdk.config
└─ class AltcraftConfiguration private constructor(...)
   // Класс инициализации конфигурации Altcraft SDK:
   // URL API, ресурсный токен, сведения о приложении, флаг использования foreground service
   // и настройки push-уведомлений (канал, модули получателя и приоритет провайдеров).

   ├─ class Builder(
   │     apiUrl: String,                         // Базовый URL Altcraft API (обязательный)
   │     icon: Int? = null,                      // ID ресурса иконки уведомлений (опционально)
   │     rToken: String? = null,                 // Ролевой токен (опционально)
   │     usingService: Boolean = false,          // Использовать foreground service при подписке/обновлении токена
   │     serviceMessage: String? = null,         // Текст уведомления foreground service (опционально)
   │     appInfo: DataClasses.AppInfo? = null,   // Метаданные приложения (ID/IID/версия) (опционально)
   │     providerPriorityList: List<String>? = null, // Приоритет провайдеров push-уведомлений(опционально)
   │     pushReceiverModules: List<String>? = null,  // Пакеты модулей, где может быть переопределён PushReceiver (опционально)
   │     pushChannelName: String? = null,        // Имя канала push-уведомлений (опционально)
   │     pushChannelDescription: String? = null  // Описание канала push-уведомлений (опционально)
   │   )
   │   └─ fun build(): AltcraftConfiguration     // Построить валидную конфигурацию
   │
   ├─ fun getApiUrl(): String                    // Возвращает базовый URL Altcraft API
   ├─ fun getIcon(): Int?                        // Возвращает ID иконки для уведомлений (опционально)
   ├─ fun getRToken(): String?                   // Возвращает ресурсный токен (опционально)
   ├─ fun getUsingService(): Boolean             // Признак использования foreground service при подписке/обновлении токена
   ├─ fun getServiceMessage(): String?           // Сообщение уведомления foreground service (опционально)
   ├─ fun getAppInfo(): DataClasses.AppInfo?     // Сведения о приложении (ID/IID/версия) (опционально)
   ├─ fun getProviderPriorityList(): List<String>? // Приоритет провайдеров push-уведомлений (опционально)
   ├─ fun getPushReceiverModules(): List<String>? // Модули с переопределениями PushReceiver (опционально)
   ├─ fun getPushChannelName(): String?          // Имя канала уведомлений (опционально)
   └─ fun getPushChannelDescription(): String?   // Описание канала уведомлений (опционально)
```
