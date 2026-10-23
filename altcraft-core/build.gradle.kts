import com.vanniktech.maven.publish.AndroidSingleVariantLibrary

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.maven.publish)
}

group = "com.altcraft"

android {
    namespace = "com.altcraft.sdk.core"
    compileSdk = 35

    defaultConfig {
        minSdk = 25
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    implementation(libs.androidx.annotation)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.scalars)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.work.runtime.ktx)
    implementation(libs.lifecycle.process)

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)

    // Instrumented tests
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.work.testing)
}

configurations.matching {
    it.name.contains("androidTest", ignoreCase = true)
}.all {
    exclude(group = "org.junit.jupiter")
    exclude(group = "org.junit.platform")
}


mavenPublishing {
    configure(
        AndroidSingleVariantLibrary(
            variant = "release",
            sourcesJar = true,
            publishJavadocJar = false
        )
    )

    publishToMavenCentral()
    signAllPublications()

    coordinates("com.altcraft", "android-sdk-core", "1.0.0")

    pom {
        name = "Altcraft Android SDK"
        description = "Altcraft Android SDK"
        inceptionYear = "2026"
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