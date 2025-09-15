plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    //alias(libs.plugins.google.gms.google.services)
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
    //id("com.huawei.agconnect")
}

android {
    namespace = "com.altcraft.altcraftmobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.altcraft.altcraftmobile"
        minSdk = 24
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
    sourceSets {
        getByName("main") {
            assets {
                srcDirs("src/main/assets")
            }
        }
    }
}

dependencies {
    // --- sdk modules ---
    implementation(project(":altcraft-sdk"))

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
    implementation("com.google.firebase:firebase-messaging:24.0.2")
    implementation("com.google.firebase:firebase-messaging-directboot:24.0.2")

    // HMS
    implementation("com.huawei.hms:push:6.11.0.300")
    implementation("com.huawei.agconnect:agconnect-core:1.7.3.302")

    // RuStore
    implementation("ru.rustore.sdk:pushclient:6.10.0")

    // --- Version alignment ---
    configurations.all {
        resolutionStrategy {
            force("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.3")
            force("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
        }
    }
}
