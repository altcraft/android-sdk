plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
//    alias(libs.plugins.google.gms.google.services)
//    id("com.huawei.agconnect")
}

android {
    namespace = "com.altcraft.altcraftmobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.altcraft.altcraftmobile"
        minSdk = 25
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
}

dependencies {

    // --- sdk modules ---
    implementation(project(":altcraft-core"))
    implementation(project(":altcraft-push"))
    implementation(project(":altcraft-inapp"))
    implementation(project(":altcraft-target"))
    implementation(project(":altcraft-profile"))

    // --- Kotlin / Serialization ---
    implementation(libs.kotlinx.serialization.json)

    // --- AndroidX / Jetpack / Compose ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.runtime)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compiler)
    debugImplementation(libs.androidx.ui.tooling)

    // --- Accompanist ---
    implementation(libs.accompanist.systemuicontroller)

    // --- Networking / Logging ---
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.logging.interceptor)

    // --- Push providers ---
    // FCM
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-messaging")

    // HMS
    implementation("com.huawei.hms:push:6.13.0.301")
    implementation("com.huawei.agconnect:agconnect-core:1.9.6.300")

    // RuStore
    implementation("ru.rustore.sdk:universalpush:7.5.0")
    implementation("ru.rustore.sdk:universalrustore:7.5.0")

    // --- Version alignment ---
    configurations.all {
        resolutionStrategy {
            force("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.3")
            force("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
        }
    }
}