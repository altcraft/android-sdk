pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven("https://developer.huawei.com/repo/")
        maven("https://artifactory-external.vkpartner.ru/artifactory/maven")
    }
}

rootProject.name = "AltcraftMobile"
include(":example")
include(":altcraft-sdk")
