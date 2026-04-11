# PerfKit

[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)](https://github.com/perfkit/perfkit/releases)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)
[![Min SDK](https://img.shields.io/badge/minSdk-24-orange.svg)](https://developer.android.com/about/versions/nougat)

PerfKit is a modular Android SDK for detecting, processing, and visualizing StrictMode violations in real time during development. It captures violations at the framework level, classifies them by category and severity, and surfaces them in a non-intrusive debug UI — all without touching your production builds.

> Screenshots will be added after first device test.

---

## Features

- Real-time StrictMode violation capture (ThreadPolicy and VmPolicy)
- Full programmatic capture via `penaltyListener` on API 28+; Logcat fallback on API 24–27
- Automatic classification by category (disk, network, leaks, cleartext, etc.) and severity (LOW → CRITICAL)
- Circular buffer with configurable capacity and deduplication window
- Sticky notification badge with violation count
- `ViolationPanelActivity` built in Jetpack Compose for in-app inspection
- Zero impact on release builds — production dependency is a no-op API contract
- No dependency injection framework required — works with any DI setup or none

---

## Module Structure

| Module | Artifact | Description |
|---|---|---|
| `:sdk-api` | `com.perfkit:sdk-api:1.0.0` | Public contracts: `ViolationEvent`, `PerfKitConfig`, use case interfaces, domain service interfaces |
| `:sdk-core` | `com.perfkit:sdk-core:1.0.0` | Infrastructure: event bus, circular buffer, classifier, deduplicator, logger, `PerfKit` singleton |
| `:sdk-strictmode` | `com.perfkit:sdk-strictmode:1.0.0` | Android adapter — installs StrictMode with `penaltyListener` (API 28+) and log fallback (API 24–27) |
| `:sdk-debug-ui` | `com.perfkit:sdk-debug-ui:1.0.0` | Debug overlay: sticky notification + `ViolationPanelActivity` in Jetpack Compose |
| `:app` | — | Sample app demonstrating SDK integration |

**Dependency graph:**

```
sdk-api ← sdk-core ← sdk-strictmode
                   ← sdk-debug-ui
```

---

## Quick Start

### 1. Add dependencies

```kotlin
// build.gradle.kts
dependencies {
    debugImplementation("com.perfkit:sdk-core:1.0.0")
    debugImplementation("com.perfkit:sdk-strictmode:1.0.0")
    debugImplementation("com.perfkit:sdk-debug-ui:1.0.0")

    // No-op contract for release builds — keeps API calls compilable in shared code
    releaseImplementation("com.perfkit:sdk-api:1.0.0")
}
```

### 2. Initialize in Application

```kotlin
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        PerfKit.initialize(
            context = this,
            config = PerfKitConfig(
                strictModeEnabled = true,
                debugUiEnabled = true,
                detectDiskReads = true,
                detectDiskWrites = true,
                detectNetwork = true,
                detectLeakedClosableObjects = true,
                maxBufferSize = 200,
                dedupWindowMs = 2_000L,
                minSeverityToDisplay = ViolationSeverity.MEDIUM
            )
        )

        StrictModePlugin.install(this)
        DebugUiPlugin.install(this)
    }
}
```

### 3. Trigger a violation manually (for testing)

```kotlin
// Force a disk read on the main thread to verify the SDK is working
Thread.currentThread().let {
    File(filesDir, "test.txt").readText() // triggers DISK_READ violation
}
```

### 4. Open the violation panel programmatically

```kotlin
// From any Activity or Fragment
PerfKit.openDebugPanel(context)

// Clear the current buffer
PerfKit.clearViolations()
```

---

## Configuration Reference

```kotlin
data class PerfKitConfig(
    val enabled: Boolean = true,                              // Master switch
    val debugOnly: Boolean = true,                            // Auto-disable on release builds
    val strictModeEnabled: Boolean = true,
    val debugUiEnabled: Boolean = true,
    val detectDiskReads: Boolean = true,
    val detectDiskWrites: Boolean = true,
    val detectNetwork: Boolean = true,
    val detectCustomSlowCalls: Boolean = true,
    val detectLeakedClosableObjects: Boolean = true,
    val detectActivityLeaks: Boolean = true,
    val detectCleartextNetwork: Boolean = true,
    val maxBufferSize: Int = 200,                             // Circular buffer capacity
    val dedupWindowMs: Long = 2_000L,                         // Dedup window in milliseconds
    val minSeverityToDisplay: ViolationSeverity = ViolationSeverity.MEDIUM,
    val logger: ViolationLogger? = null                       // Custom log sink
)
```

### ViolationEvent

```kotlin
data class ViolationEvent(
    val id: String,
    val timestamp: Long,
    val source: ViolationSource,       // THREAD_POLICY | VM_POLICY
    val category: ViolationCategory,   // See categories below
    val severity: ViolationSeverity,   // LOW | MEDIUM | HIGH | CRITICAL
    val threadName: String,
    val message: String,
    val stacktrace: String?,
    val className: String?,
    val policyLabel: String?,
)
```

**Violation categories:** `DISK_READ`, `DISK_WRITE`, `NETWORK`, `SLOW_CALL`, `LEAKED_RESOURCE`, `RESOURCE_MISMATCH`, `CLEARTEXT_NETWORK`, `UNTAGGED_SOCKET`, `CUSTOM`, `UNKNOWN`

---

## Android & API Level Support

| API Level | Capture Method | Behavior |
|---|---|---|
| API 28+ (Android 9+) | `penaltyListener` on ThreadPolicy and VmPolicy | Full programmatic capture — violations are intercepted, classified, and displayed in the panel |
| API 24–27 (Android 7–8.1) | `penaltyLog()` fallback | Violations appear in Logcat only; no programmatic capture or panel display |

The SDK installs the appropriate policy automatically based on the device API level. No configuration change is required.

---

## Demo Experience

The sample app ships with a persistent **debug FAB** (floating action button) that acts as the main entry point for the PerfKit debug experience. It is visible only in debug builds.

> Screenshot placeholder — run `./gradlew :app:installDebug` and take a screenshot of the FAB on the main screen.

### What the FAB does

| Gesture | Action |
|---|---|
| **Tap** | Opens the PerfKit violation panel (`ViolationPanelActivity`) |
| **Long press** | Opens a quick actions sheet |

### Quick actions sheet

The bottom sheet that appears on long press shows:
- **SDK status:** Monitoring Active badge, Debug Only badge
- **API info:** current API level + capture mode (`Full Capture` on API 28+ or `Logcat` on API 24–27)
- **Quick actions:** Open PerfKit Panel, Trigger Disk Read, Trigger Disk Write, Trigger Slow Call, Clear Captured Events

### Violation panel

The panel (`ViolationPanelActivity`) shows:
- Status banner: Monitoring Active · Debug Only · API level + capture mode
- Summary chips: count per category, color-coded by highest severity
- Category filter row
- Violation list: severity dot, category, message, timestamp, thread name
- Tap any violation for full detail with stacktrace

---

## How to Present It

1. Launch the app on a debug build (emulator API 28+ recommended for full capture).
2. Point to the dark FAB in the bottom-right corner — explain it is always visible and only in debug builds.
3. Tap a **Trigger** button (Disk Read, Disk Write, Slow Call).
4. Long press the FAB → show the quick actions sheet with the status info.
5. Tap **Open PerfKit Panel** → walk through the violation list, status banner, and tap a violation to see the full stacktrace.
6. Use **Clear Captured Events** to reset for the next demo segment.

**API level note:** on API 28+ emulators/devices, violations are captured programmatically and appear in the panel immediately. On API 24–27, violations are written to Logcat only and will not appear in the panel.

---

## Running the Sample App

```bash
# Build all modules
./gradlew clean assemble

# Install the sample app on a connected device or emulator
./gradlew :app:installDebug

# Run unit tests for the core module
./gradlew :sdk-core:test
```

---

## Publishing Locally

Use `publishToMavenLocal` to publish all SDK artifacts to your local Maven repository, then consume them from another project without needing a remote registry.

```bash
./gradlew publishToMavenLocal
```

In the consuming project:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenLocal()   // must come before google() and mavenCentral()
        google()
        mavenCentral()
    }
}
```

```kotlin
// build.gradle.kts
dependencies {
    debugImplementation("com.perfkit:sdk-core:1.0.0")
    debugImplementation("com.perfkit:sdk-strictmode:1.0.0")
    debugImplementation("com.perfkit:sdk-debug-ui:1.0.0")
    releaseImplementation("com.perfkit:sdk-api:1.0.0")
}
```

---

## Roadmap

| Version | Focus | Highlights |
|---|---|---|
| **v1.1** | Reporting & panel improvements | Export violations as JSON, share report via Android share sheet, advanced filters in the panel, per-category counters |
| **v1.2** | Startup & rendering metrics | App startup time tracking, frozen frame detection, jank metrics |
| **v1.3** | Stability heuristics | ANR heuristics, violation sampling for high-frequency apps |

**Out of scope for v1.x:** backend upload, web dashboard, remote export, database persistence, Perfetto tracing integration, system-level overlay (chat-head style).

---

## Tech Stack

| Component | Version |
|---|---|
| Kotlin | 2.0.21 |
| Android Gradle Plugin | 8.10.0 |
| Min SDK | 24 |
| Compile SDK | 35 |
| Compose BOM | 2025.05.00 |

---

## License

PerfKit is distributed under the [MIT License](LICENSE).
