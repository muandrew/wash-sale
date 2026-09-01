import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import java.util.Properties

plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
}

fun findDevServerPort(): Int? {
    val localPropertiesFile = rootProject.file("local.properties")
    val localPort = if (localPropertiesFile.exists()) {
        Properties().apply {
            localPropertiesFile.inputStream().use { load(it) }
        }.getProperty("wasm.port")
    } else null

    return (project.findProperty("wasmPort")
        ?: project.findProperty("wasm.port")
        ?: System.getenv("WASM_PORT")
        ?: localPort)
        ?.toString()?.toIntOrNull()
}

fun findDevServerPrefix(): String {
    val localPropertiesFile = rootProject.file("local.properties")
    val localPrefix = if (localPropertiesFile.exists()) {
        Properties().apply {
            localPropertiesFile.inputStream().use { load(it) }
        }.getProperty("wasm.prefix")
    } else null

    val rawPrefix = (project.findProperty("wasmPrefix")
        ?: project.findProperty("wasm.prefix")
        ?: System.getenv("WASM_PREFIX")
        ?: localPrefix)
        ?.toString()?.trim() ?: ""

    return if (rawPrefix.isEmpty()) {
        ""
    } else {
        val trimmed = rawPrefix.trim('/')
        if (trimmed.isEmpty()) "" else "/$trimmed/"
    }
}


kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "forecast"
        browser {
            commonWebpackConfig {
                outputFileName = "forecast.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    val customPort = findDevServerPort()
                    if (customPort != null) {
                        port = customPort
                    }
                }
            }
        }
        binaries.executable()
    }

    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            @OptIn(ExperimentalComposeLibrary::class)
            implementation(compose.components.resources)
            implementation(libs.kotlin.coroutines.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }


        wasmJsMain {
            dependencies {
                implementation(libs.kotlin.coroutines.core)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlin.coroutines.swing)
            }
        }
    }
}
