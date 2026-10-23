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
        maven {
            url = uri("https://developer.huawei.com/repo/")
        }

        maven {
            url = uri("https://nexus-external.vkteam.ru/repository/maven/")
        }
    }
}

rootProject.name = "AltcraftMobile"
include(":example")
include(":altcraft-core")

include(":altcraft-target")
include(":altcraft-push")
include(":altcraft-profile")
include(":altcraft-inapp")
