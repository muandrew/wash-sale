rootProject.name = "WashSale"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        // other problems and performance suffer from using mavenLocal
        // https://docs.gradle.org/current/userguide/declaring_repositories.html#sec:repository-content-filtering
        mavenLocal {
            content {
                includeGroup("com.muandrew.stock")
            }
        }
    }
}

include(":composeApp")