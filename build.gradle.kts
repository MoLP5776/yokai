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

val appPackageName = "Yokai"
val appMainClass  = "com.yokai.MainKt"

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)
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

// "auto" detection relies on Kotlin Multiplatform source sets, which this
// JVM-only project doesn't have, so the Res class never gets generated unless
// this is forced.
compose.resources {
    generateResClass = always
}

// Determine which formats to declare based on the OS running the build.
// AppImage is Linux-only — declaring it on macOS causes a configuration error,
// so we must gate it here rather than listing all three unconditionally.
val currentOs = org.gradle.internal.os.OperatingSystem.current()
val desiredTargetFormats = when {
    currentOs.isLinux   -> arrayOf(TargetFormat.AppImage)
    currentOs.isWindows -> arrayOf(TargetFormat.Msi)
    currentOs.isMacOsX  -> arrayOf(TargetFormat.Dmg)
    else                -> emptyArray()
}

compose.desktop {
    application {
        mainClass = appMainClass

        nativeDistributions {
            targetFormats(*desiredTargetFormats)

            packageName    = appPackageName
            packageVersion = "1.0.0"
            description    = "A local manga reader with AniList tracking"
            vendor         = "Yokai"

            linux {
                appCategory = "Graphics"
                iconFile.set(project.file("src/main/composeResources/drawable/icon.png"))
            }

            windows {
                iconFile.set(project.file("src/main/composeResources/drawable/yokai-logo.ico"))
                menuGroup     = appPackageName
                upgradeUuid   = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
                dirChooser    = true
                perUserInstall = true
            }

            macOS {
                iconFile.set(project.file("src/main/composeResources/drawable/yokai-logo.icns"))
                bundleID = "com.yokai.app"
            }
        }
    }
}

// ── AppImage packaging (Linux only) ──────────────────────────────────────────

val appImageDir = layout.buildDirectory.dir("compose/tmp/AppDir")

val prepareAppImageDir by tasks.registering(Sync::class) {
    description = "Assembles the AppDir used to build the AppImage"
    dependsOn("packageAppImage")

    from(layout.buildDirectory.dir("compose/binaries/main/app/$appPackageName"))
    into(appImageDir)

    doFirst { delete(appImageDir) }

    doLast {
        val appDir  = appImageDir.get().asFile
        val icon    = file("src/main/composeResources/drawable/icon.png")
        val wmClass = appMainClass.replace(".", "-")

        icon.copyTo(appDir.resolve("$appPackageName.png"), overwrite = true)

        val hicolorIcon = appDir.resolve("usr/share/icons/hicolor/256x256/apps/$appPackageName.png")
        hicolorIcon.parentFile.mkdirs()
        icon.copyTo(hicolorIcon, overwrite = true)

        appDir.resolve("$appPackageName.desktop").writeText(
            """
            [Desktop Entry]
            Type=Application
            Name=$appPackageName
            Comment=A local manga reader with AniList tracking
            Exec=AppRun
            Icon=$appPackageName
            Categories=Graphics;
            Terminal=false
            StartupWMClass=$wmClass
            """.trimIndent() + "\n"
        )

        val appRun = appDir.resolve("AppRun")
        appRun.writeText(
            """
            #!/bin/sh
            HERE="$(dirname "$(readlink -f "${'$'}{0}")")"
            exec "${'$'}HERE/bin/$appPackageName" "${'$'}@"
            """.trimIndent() + "\n"
        )
        appRun.setExecutable(true)
    }
}

tasks.register<Exec>("packageAppImageToDir") {
    description = "Builds the .AppImage with appimagetool and places it in build/appimage"
    dependsOn(prepareAppImageDir)

    val outputDir  = layout.buildDirectory.dir("appimage")
    val outputFile = outputDir.map { it.file("$appPackageName-${project.version}-x86_64.AppImage") }

    inputs.dir(appImageDir)
    outputs.file(outputFile)

    doFirst { outputDir.get().asFile.mkdirs() }

    environment("ARCH", "x86_64")
    commandLine(
        "appimagetool",
        appImageDir.get().asFile.absolutePath,
        outputFile.get().asFile.absolutePath,
    )
}