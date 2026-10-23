import com.vanniktech.maven.publish.AndroidSingleVariantLibrary

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "com.altcraft.sdk.push"
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
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

group = "com.altcraft"

dependencies {

    //api(libs.altcraft.sdk.core)
    implementation(project(":altcraft-core"))

    implementation(libs.androidx.annotation)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.work.runtime.ktx)
    implementation(libs.startup.runtime)
    implementation(libs.datastore.preferences)
    implementation(libs.glide)
    implementation(libs.androidx.core.ktx)

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)

    // Instrumented tests
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.work.testing)
    androidTestImplementation(libs.kotlinx.coroutines.guava)
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

    coordinates("com.altcraft", "android-sdk-push", "1.0.0")

    pom {
        name = "Altcraft Android SDK Push"
        description = "Altcraft Android SDK Push Module"
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