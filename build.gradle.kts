import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.conveyor)
}

group = "com.yokai"
version = "1.0.0"

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.swing)
}

kotlin {
    jvmToolchain(21)
}

compose.desktop {
    application {
        mainClass = "com.yokai.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.AppImage)
            packageName = "Yokai"
            packageVersion = "1.0.0"
            description = "A local manga reader with AniList tracking"
            vendor = "Yokai"

            linux {
                appCategory = "Graphics"
            }
        }
    }
}
