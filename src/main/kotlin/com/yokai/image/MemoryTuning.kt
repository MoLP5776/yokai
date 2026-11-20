package com.yokai.image

import com.sun.management.HotSpotDiagnosticMXBean
import java.lang.management.ManagementFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.management.ObjectName

/**
 * Makes the app give memory back to the OS once it goes idle.
 *
 * Applied from code rather than as JVM flags so it works no matter how the app is launched
 * (packaged app, `./gradlew run`, or an IDE run configuration, which ignores Gradle's jvmArgs).
 *
 * Without this the JVM keeps a heap it once grew while rendering, never notices the native
 * pixels of bitmaps nothing references anymore (they barely use any heap), and memory freed by
 * Skia stays reserved by malloc, so RAM usage only ever goes up.
 */
object MemoryTuning {
    private const val PERIODIC_GC_INTERVAL_MS = 15_000L
    private const val NATIVE_TRIM_INTERVAL_SECONDS = 20L

    fun install() {
        val diagnostics = runCatching {
            ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean::class.java)
        }.getOrNull()

        diagnostics?.let { bean ->
            // When no GC happened for a while, run one. This also frees the native pixels of
            // bitmaps that were dropped without an explicit release (e.g. evicted covers).
            // Only G1 has this option; with other collectors this is a no-op.
            bean.trySet("G1PeriodicGCInterval", PERIODIC_GC_INTERVAL_MS.toString())
            // Shrink the committed heap after a GC when most of it is unused.
            // Min must be lowered first: Max may never drop below Min.
            bean.trySet("MinHeapFreeRatio", "10")
            bean.trySet("MaxHeapFreeRatio", "30")
        }

        // Equivalent of -XX:TrimNativeHeapInterval, which can't be changed at runtime: return
        // small native allocations freed by Skia to the OS. This is a bonus on top of releasing
        // bitmaps explicitly (large pixel buffers go back to the OS on their own), and not every
        // JDK build has the command (e.g. JetBrains Runtime 21.0.10 doesn't), so skip it there.
        val trimCommand = ObjectName("com.sun.management:type=DiagnosticCommand")
        val server = ManagementFactory.getPlatformMBeanServer()
        val canTrim = runCatching {
            server.getMBeanInfo(trimCommand).operations.any { it.name == "systemTrimNativeHeap" }
        }.getOrDefault(false)
        if (!canTrim) {
            println("Native heap trimming is not supported by this JVM; skipping it.")
        } else {
            Executors.newSingleThreadScheduledExecutor { task ->
                Thread(task, "yokai-native-trim").apply { isDaemon = true }
            }.scheduleWithFixedDelay(
                {
                    runCatching {
                        server.invoke(
                            trimCommand,
                            "systemTrimNativeHeap",
                            arrayOf<Any>(emptyArray<String>()),
                            arrayOf(Array<String>::class.java.name),
                        )
                    }
                },
                NATIVE_TRIM_INTERVAL_SECONDS,
                NATIVE_TRIM_INTERVAL_SECONDS,
                TimeUnit.SECONDS,
            )
        }
    }

    private fun HotSpotDiagnosticMXBean.trySet(name: String, value: String) {
        runCatching { setVMOption(name, value) }
            .onFailure { println("Could not set VM option $name=$value: ${it.message}") }
    }
}
