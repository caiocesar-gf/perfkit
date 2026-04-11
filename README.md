# PerfKit

[![JitPack](https://jitpack.io/v/caiocesar-gf/perfkit.svg)](https://jitpack.io/#caiocesar-gf/perfkit)
[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)](https://github.com/caiocesar-gf/perfkit/releases)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)
[![Min SDK](https://img.shields.io/badge/minSdk-24-orange.svg)](https://developer.android.com/about/versions/nougat)

PerfKit is a modular Android SDK for detecting, processing, and visualizing StrictMode violations in real time during development. It captures violations at the framework level, classifies them by category and severity, and surfaces them in a non-intrusive debug UI — all without touching your production builds.

---

## Features

- Real-time StrictMode violation capture (ThreadPolicy and VmPolicy)
- Full programmatic capture via `penaltyListener` on API 28+; Logcat fallback on API 24–27
- Automatic classification by category (disk, network, leaks, cleartext, etc.) and severity (LOW → CRITICAL)
- Circular buffer with configurable capacity and deduplication window
- Persistent notification badge with violation count
- Jetpack Compose violation panel for in-app inspection
- Persistent debug FAB with quick actions (tap to open panel, long press for triggers)
- Zero impact on release builds — production dependency is a no-op API contract
- No dependency injection framework required

---

## Quick Start

### 1. Add JitPack to your project

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }  // ← add this
    }
}
```

### 2. Add dependencies

```kotlin
// app/build.gradle.kts
dependencies {
    // Debug-only — automatically excluded from release builds
    debugImplementation("com.perfkit:sdk-core:1.0.0")
    debugImplementation("com.perfkit:sdk-strictmode:1.0.0")
    debugImplementation("com.perfkit:sdk-debug-ui:1.0.0")

    // Public API contracts — safe for release, keeps calls compilable in shared code
    releaseImplementation("com.perfkit:sdk-api:1.0.0")
}
```

<details>
<summary>Using Version Catalog (libs.versions.toml)</summary>

```toml
# gradle/libs.versions.toml
[versions]
perfkit = "1.0.0"

[libraries]
perfkit-api        = { group = "com.perfkit", name = "sdk-api",        version.ref = "perfkit" }
perfkit-core       = { group = "com.perfkit", name = "sdk-core",       version.ref = "perfkit" }
perfkit-strictmode = { group = "com.perfkit", name = "sdk-strictmode", version.ref = "perfkit" }
perfkit-debugui    = { group = "com.perfkit", name = "sdk-debug-ui",   version.ref = "perfkit" }
```

```kotlin
// app/build.gradle.kts
dependencies {
    debugImplementation(libs.perfkit.core)
    debugImplementation(libs.perfkit.strictmode)
    debugImplementation(libs.perfkit.debugui)
    releaseImplementation(libs.perfkit.api)
}
```

</details>

<details>
<summary>Minimal setup — Logcat only, no debug UI</summary>

If you only need violation detection in Logcat without the in-app panel:

```kotlin
// app/build.gradle.kts
dependencies {
    debugImplementation("com.perfkit:sdk-core:1.0.0")
    debugImplementation("com.perfkit:sdk-strictmode:1.0.0")
    releaseImplementation("com.perfkit:sdk-api:1.0.0")
}
```

</details>

### 3. Initialize in Application

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
                minSeverityToDisplay = ViolationSeverity.MEDIUM,
            )
        )

        StrictModePlugin.install(this)   // installs penaltyListener
        DebugUiPlugin.install(this)      // starts notification badge
    }
}
```

> **Order matters:** `PerfKit.initialize()` must be called before the other two.

### 4. Use the API

```kotlin
// Open the violation panel from any Activity
PerfKit.openDebugPanel(context)

// Clear the in-memory buffer
PerfKit.clearViolations()

// Observe violations as a Flow
lifecycleScope.launch {
    PerfKit.observeViolations().collect { events ->
        // events: List<ViolationEvent>
    }
}
```

---

## Module Structure

| Module | Artifact | Description |
|---|---|---|
| `:sdk-api` | `com.perfkit:sdk-api:1.0.0` | Public contracts: `ViolationEvent`, `PerfKitConfig`, use case interfaces |
| `:sdk-core` | `com.perfkit:sdk-core:1.0.0` | Infrastructure: event bus, circular buffer, classifier, deduplicator, `PerfKit` singleton |
| `:sdk-strictmode` | `com.perfkit:sdk-strictmode:1.0.0` | Android adapter — StrictMode with `penaltyListener` (API 28+) and Logcat fallback (API 24–27) |
| `:sdk-debug-ui` | `com.perfkit:sdk-debug-ui:1.0.0` | Debug overlay: sticky notification + `ViolationPanelActivity` in Jetpack Compose |
| `:app` | — | Sample app demonstrating SDK integration |

**Dependency graph:**

```
sdk-api ← sdk-core ← sdk-strictmode
                   ← sdk-debug-ui
```

---

## Configuration Reference

```kotlin
PerfKitConfig(
    enabled: Boolean = true,                              // Master switch
    debugOnly: Boolean = true,                            // Auto-disable on release builds
    strictModeEnabled: Boolean = true,
    debugUiEnabled: Boolean = true,

    // Thread policy
    detectDiskReads: Boolean = true,
    detectDiskWrites: Boolean = true,
    detectNetwork: Boolean = true,
    detectCustomSlowCalls: Boolean = true,
    detectResourceMismatches: Boolean = false,

    // VM policy
    detectLeakedClosableObjects: Boolean = true,
    detectActivityLeaks: Boolean = true,
    detectCleartextNetwork: Boolean = true,
    detectLeakedRegistrationObjects: Boolean = false,
    detectFileUriExposure: Boolean = false,

    // Processing
    maxBufferSize: Int = 200,                             // Circular buffer capacity
    dedupWindowMs: Long = 2_000L,                         // Dedup window in milliseconds
    minSeverityToDisplay: ViolationSeverity = ViolationSeverity.MEDIUM,

    logger: ViolationLogger? = null                       // Custom log sink; null = Logcat
)
```

### ViolationEvent

```kotlin
data class ViolationEvent(
    val id: String,
    val timestamp: Long,
    val source: ViolationSource,       // THREAD_POLICY | VM_POLICY
    val category: ViolationCategory,   // DISK_READ | DISK_WRITE | NETWORK | SLOW_CALL | LEAKED_RESOURCE | ...
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
| API 28+ (Android 9+) | `penaltyListener` on ThreadPolicy and VmPolicy | Full programmatic capture — violations intercepted, classified, displayed in the panel |
| API 24–27 (Android 7–8.1) | `penaltyLog()` fallback | Violations in Logcat only; panel shows empty state |

The SDK installs the appropriate policy automatically. No configuration change needed.

---

## Demo Experience

The sample app ships with a persistent **debug FAB** visible only in debug builds.

> Screenshot placeholder — run `./gradlew :app:installDebug` and take screenshots.

### What the FAB does

| Gesture | Action |
|---|---|
| **Tap** | Opens the PerfKit violation panel (`ViolationPanelActivity`) |
| **Long press** | Opens the quick actions sheet |

### Quick actions sheet

- **SDK status:** Monitoring Active · Debug Only · API level + capture mode
- **Quick triggers:** Disk Read, Disk Write, Slow Call
- **Clear Captured Events**

### Violation panel

- Status banner: Monitoring Active · Debug Only · API level + capture mode
- Summary chips: count per category, color-coded by highest severity
- Category filter row
- Violation list with severity dot, category, message, timestamp, thread name
- Tap any item for full detail with expandable stacktrace

---

## How to Present It

1. Launch the app on API 28+ emulator/device for full capture.
2. Point to the dark FAB (bottom-right) — debug builds only.
3. Tap a **Trigger** button (Disk Read, Disk Write, Slow Call).
4. Long press the FAB → show quick actions and SDK status info.
5. Open panel → walk through the list, tap a violation to see stacktrace.
6. **Clear Captured Events** to reset for the next demo segment.

**Note:** on API 24–27, violations appear in Logcat only and the panel stays empty.

---

## Running the Sample App

```bash
# Build all modules
./gradlew clean assemble

# Install on a connected device or emulator
./gradlew :app:installDebug

# Run unit tests
./gradlew :sdk-core:test
```

---

## Roadmap

| Version | Focus | Highlights |
|---|---|---|
| **v1.1** | Reporting | Export violations as JSON, share via Android share sheet, advanced filters |
| **v1.2** | Startup & rendering | App startup time, frozen frame detection, jank metrics |
| **v1.3** | Stability heuristics | ANR heuristics, violation sampling for high-frequency apps |

---

## Contributing

### Build locally

```bash
# Clone and build
git clone https://github.com/caiocesar-gf/perfkit.git
cd perfkit
./gradlew clean assemble

# Publish all SDK modules to Maven Local for local testing
./gradlew publishToMavenLocal
```

In the consuming project:

```kotlin
// settings.gradle.kts — add mavenLocal() before google() and mavenCentral()
repositories {
    mavenLocal()
    google()
    mavenCentral()
}
```

### Releasing a new version

1. Update `PERFKIT_VERSION` in `gradle.properties`
2. Update `CHANGELOG.md`
3. Commit: `git commit -m "chore(release): bump version to X.Y.Z"`
4. Tag: `git tag vX.Y.Z && git push origin main && git push origin vX.Y.Z`
5. JitPack builds automatically on tag push — artifacts available within minutes at `https://jitpack.io/#caiocesar-gf/perfkit`

### Module boundary rules

- `sdk-api` must have no Android imports.
- `sdk-core` must not depend on `sdk-strictmode` or `sdk-debug-ui`.
- App-specific demo concerns must stay in `:app`.

Commit messages follow Conventional Commits: `<type>(<scope>): <summary>`

---

## Tech Stack

| Component | Version |
|---|---|
| Kotlin | 2.0.21 |
| Android Gradle Plugin | 8.10.0 |
| Min SDK | 24 |
| Compile SDK | 36 |
| Compose BOM | 2025.05.00 |

---

## License

PerfKit is distributed under the [MIT License](LICENSE).
