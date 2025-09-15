// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        maven("https://developer.huawei.com/repo/")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.10.1")
        classpath("com.huawei.agconnect:agcp:1.9.1.301")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.google.gms.google.services) apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
    alias(libs.plugins.android.library) apply false
}