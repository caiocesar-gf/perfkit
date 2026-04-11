# PerfKit

[![CI](https://github.com/caiocesar-gf/perfkit/actions/workflows/ci.yml/badge.svg)](https://github.com/caiocesar-gf/perfkit/actions/workflows/ci.yml)
[![JitPack](https://jitpack.io/v/caiocesar-gf/perfkit.svg)](https://jitpack.io/#caiocesar-gf/perfkit)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)
[![Min SDK](https://img.shields.io/badge/minSdk-24-orange.svg)](https://developer.android.com/about/versions/nougat)

PerfKit is a modular Android SDK for detecting, processing, and visualizing StrictMode violations in real time during development. It captures violations at the framework level, classifies them by category and severity, and surfaces them in a non-intrusive debug UI — all without touching your production builds.

---

## Features

- Real-time StrictMode violation capture (ThreadPolicy and VmPolicy)
- Full programmatic capture via `penaltyListener` on API 28+; Logcat fallback on API 24–27
- Automatic classification by category (disk, network, leaks, cleartext…) and severity (LOW → CRITICAL)
- Circular buffer with configurable capacity and deduplication window
- Persistent notification badge with live violation count
- Jetpack Compose violation panel for in-app inspection
- Persistent debug FAB — tap to open panel, long press for quick actions
- Zero impact on release builds — production dependency is a compile-only no-op surface
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
        maven { url = uri("https://jitpack.io") }   // ← add this line
    }
}
```

### 2. Add dependencies

Replace `TAG` with the latest release tag shown in the JitPack badge above (e.g. `v1.0.1`).

```kotlin
// app/build.gradle.kts
dependencies {
    debugImplementation("com.github.caiocesar-gf.perfkit:sdk-core:TAG")
    debugImplementation("com.github.caiocesar-gf.perfkit:sdk-strictmode:TAG")
    debugImplementation("com.github.caiocesar-gf.perfkit:sdk-debug-ui:TAG")

    // Public API contracts — safe for release, keeps calls compilable in shared code
    releaseImplementation("com.github.caiocesar-gf.perfkit:sdk-api:TAG")
}
```

> **JitPack note:** on first use of a new tag, JitPack builds the artifacts on-demand.
> The initial request may take 1–3 minutes; subsequent resolves are instant.

<details>
<summary>Using Version Catalog (libs.versions.toml)</summary>

```toml
# gradle/libs.versions.toml
[versions]
perfkit = "v1.0.1"   # use the latest tag from the JitPack badge

[libraries]
perfkit-api        = { group = "com.github.caiocesar-gf.perfkit", name = "sdk-api",        version.ref = "perfkit" }
perfkit-core       = { group = "com.github.caiocesar-gf.perfkit", name = "sdk-core",       version.ref = "perfkit" }
perfkit-strictmode = { group = "com.github.caiocesar-gf.perfkit", name = "sdk-strictmode", version.ref = "perfkit" }
perfkit-debugui    = { group = "com.github.caiocesar-gf.perfkit", name = "sdk-debug-ui",   version.ref = "perfkit" }
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

```kotlin
dependencies {
    debugImplementation("com.github.caiocesar-gf.perfkit:sdk-core:TAG")
    debugImplementation("com.github.caiocesar-gf.perfkit:sdk-strictmode:TAG")
    releaseImplementation("com.github.caiocesar-gf.perfkit:sdk-api:TAG")
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

        StrictModePlugin.install(this)   // installs penaltyListener (API 28+) or fallback
        DebugUiPlugin.install(this)      // activates notification badge
    }
}
```

> **Order matters:** `PerfKit.initialize()` must be called before `StrictModePlugin` and `DebugUiPlugin`.

### 4. Use the API

```kotlin
// Open the violation panel from any Activity
PerfKit.openDebugPanel(context)

// Clear the in-memory buffer
PerfKit.clearViolations()

// Observe violations as a Flow
lifecycleScope.launch {
    PerfKit.observeViolations().collect { events: List<ViolationEvent> ->
        // react to captured violations
    }
}
```

---

## Module Structure

| Module | Artifact | Description |
|---|---|---|
| `:sdk-api` | `…:sdk-api:TAG` | Public contracts: `ViolationEvent`, `PerfKitConfig`, use case interfaces |
| `:sdk-core` | `…:sdk-core:TAG` | Infrastructure: event bus, buffer, classifier, deduplicator, `PerfKit` singleton |
| `:sdk-strictmode` | `…:sdk-strictmode:TAG` | StrictMode adapter — `penaltyListener` (API 28+), Logcat fallback (API 24–27) |
| `:sdk-debug-ui` | `…:sdk-debug-ui:TAG` | Compose UI: sticky notification + `ViolationPanelActivity` |
| `:app` | — | Sample app |

**Dependency graph:**

```
sdk-api ← sdk-core ← sdk-strictmode
                   ← sdk-debug-ui
```

---

## Configuration Reference

```kotlin
PerfKitConfig(
    enabled: Boolean = true,                          // Master switch
    debugOnly: Boolean = true,                        // Auto-disable on release builds
    strictModeEnabled: Boolean = true,
    debugUiEnabled: Boolean = true,

    // Thread policy
    detectDiskReads: Boolean = true,
    detectDiskWrites: Boolean = true,
    detectNetwork: Boolean = true,
    detectCustomSlowCalls: Boolean = true,

    // VM policy
    detectLeakedClosableObjects: Boolean = true,
    detectActivityLeaks: Boolean = true,
    detectCleartextNetwork: Boolean = true,

    // Processing
    maxBufferSize: Int = 200,
    dedupWindowMs: Long = 2_000L,
    minSeverityToDisplay: ViolationSeverity = ViolationSeverity.MEDIUM,
    logger: ViolationLogger? = null
)
```

### ViolationEvent

```kotlin
data class ViolationEvent(
    val id: String,
    val timestamp: Long,
    val source: ViolationSource,       // THREAD_POLICY | VM_POLICY
    val category: ViolationCategory,   // DISK_READ | DISK_WRITE | NETWORK | SLOW_CALL | …
    val severity: ViolationSeverity,   // LOW | MEDIUM | HIGH | CRITICAL
    val threadName: String,
    val message: String,
    val stacktrace: String?,
    val className: String?,
    val policyLabel: String?,
)
```

---

## API Level Support

| API Level | Capture Method | Result |
|---|---|---|
| **API 28+** (Android 9+) | `penaltyListener` on ThreadPolicy + VmPolicy | Full capture — classified, buffered, shown in panel |
| **API 24–27** (Android 7–8.1) | `penaltyLog()` fallback | Violations appear in Logcat only; panel remains empty |

---

## Demo Experience

The sample app ships with a persistent **debug FAB** visible only in debug builds.

### What the FAB does

| Gesture | Action |
|---|---|
| **Tap** | Opens the violation panel (`ViolationPanelActivity`) |
| **Long press** | Opens quick actions sheet |

### Quick actions sheet

Shows SDK status (Monitoring Active · Debug Only · API level + capture mode) and one-tap triggers: Disk Read, Disk Write, Slow Call, Clear Events.

### Violation panel

Status banner · Summary chips by category · Filterable list · Tap for full stacktrace detail.

---

## How to Present the Demo

1. Launch on API 28+ emulator for full capture.
2. Point to the dark FAB (bottom-right corner) — debug builds only.
3. Tap a trigger button (Disk Read, Disk Write, Slow Call).
4. Long press the FAB → show quick actions and SDK status.
5. Open the panel → walk through violations, tap one for stacktrace.
6. **Clear Captured Events** to reset between demo segments.

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

## CI/CD & Releases

Every push to `main` and every pull request runs the **CI** workflow (build + unit tests).

When `PERFKIT_VERSION` in `gradle.properties` changes, the **Release** workflow automatically:

1. Detects that the new tag (e.g. `v1.0.1`) does not yet exist
2. Builds and verifies the project
3. Creates and pushes the git tag
4. Creates a GitHub Release with auto-generated notes

If the tag already exists, the workflow exits cleanly with no action.

JitPack then builds the SDK modules from the new tag. Artifacts are available within a few minutes at `https://jitpack.io/#caiocesar-gf/perfkit`.

### Bumping a version

```
1. Edit gradle.properties  →  PERFKIT_VERSION=X.Y.Z
2. git commit -m "chore(release): bump version to X.Y.Z"
3. git push origin main
```

The release workflow does the rest.

---

## Local Development & Publishing

Clone and build:

```bash
git clone https://github.com/caiocesar-gf/perfkit.git
cd perfkit
./gradlew clean assemble
```

Publish all SDK modules to Maven Local (useful for testing in another local project):

```bash
./gradlew :sdk-api:publishToMavenLocal \
          :sdk-core:publishToMavenLocal \
          :sdk-strictmode:publishToMavenLocal \
          :sdk-debug-ui:publishToMavenLocal
```

Add to the consuming project's `settings.gradle.kts`:

```kotlin
repositories {
    mavenLocal()    // must come before google() and mavenCentral()
    google()
    mavenCentral()
}
```

Then use:

```kotlin
debugImplementation("com.github.caiocesar-gf.perfkit:sdk-core:1.0.0")
```

---

## Roadmap

| Version | Focus |
|---|---|
| **v1.1** | JSON export, share sheet, advanced panel filters |
| **v1.2** | Startup time, frozen frame detection, jank metrics |
| **v1.3** | ANR heuristics, violation sampling |

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

[MIT License](LICENSE)
