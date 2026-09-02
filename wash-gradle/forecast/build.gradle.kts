import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import java.util.Properties

plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
}

fun findDevServerPort(): Int? {
    val localPropertiesFile = rootProject.file("local.properties")
    val localPort =
        if (localPropertiesFile.exists()) {
            Properties()
                .apply {
                    localPropertiesFile.inputStream().use { load(it) }
                }.getProperty("wasm.port")
        } else {
            null
        }

    return (
        project.findProperty("wasmPort")
            ?: project.findProperty("wasm.port")
            ?: System.getenv("WASM_PORT")
            ?: localPort
    )?.toString()
        ?.toIntOrNull()
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "forecast"
        browser {
            commonWebpackConfig {
                outputFileName = "forecast.js"
                devServer =
                    (devServer ?: KotlinWebpackConfig.DevServer()).apply {
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
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
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
