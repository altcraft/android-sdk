plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
    id("com.google.devtools.ksp")
    id("com.vanniktech.maven.publish") version "0.34.0"
}

android {
    namespace = "com.altcraft.altcraftsdk"
    compileSdk = 35

    defaultConfig {
        minSdk = 23
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

dependencies {
    //Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.22")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.7.3")
    //Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.github.bumptech.glide:annotations:4.16.0")
    implementation("com.github.bumptech.glide:okhttp3-integration:4.11.0")
    //Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.1.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
    //OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    //JWT
    implementation("com.auth0:java-jwt:4.4.0")
    //WorkManager
    implementation("androidx.work:work-runtime-ktx:2.10.2")
    //Material
    implementation("com.google.android.material:material:1.12.0")
    //Livecycle
    implementation("androidx.lifecycle:lifecycle-process:2.6.1")
    //Play-services
    implementation("com.google.android.gms:play-services-ads-identifier:18.2.0")

    // Unit tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.23")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    // Instrumented tests
    androidTestImplementation("junit:junit:4.13.2")
    androidTestImplementation("io.mockk:mockk-android:1.13.12")
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
    androidTestImplementation("androidx.arch.core:core-testing:2.2.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    // WorkManager testing
    androidTestImplementation("androidx.work:work-testing:2.9.0")
}

configurations.matching {
    it.name.contains("androidTest", ignoreCase = true)
}.all {
    exclude(group = "org.junit.jupiter")
    exclude(group = "org.junit.platform")
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates("com.altcraft", "android-sdk", "0.0.3")

    pom {
        name = "Altcraft Android SDK"
        description = "Altcraft Android SDK"
        inceptionYear = "2025"
        url = "https://github.com/altcraft/android-sdk"
        licenses {
            license {
                name = "The Altcraft License"
                url = "https://github.com/altcraft/android-sdk/blob/main/LICENSE.md"
            }
        }
        developers {
            developer {
                id = "Altcraft"
                name = "Altcraft"
                url = "contact@altcraft.com"
            }
        }
        scm {
            url = "https://github.com/altcraft/android-sdk"
            connection = "scm:git:git://github.com/altcraft/android-sdk.git"
            developerConnection = "scm:git:ssh://git@github.com/altcraft/android-sdk.git"
        }
    }
}