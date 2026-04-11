# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Common Commands

```bash
# Build all modules
./gradlew clean assemble

# Install the sample app on a connected device or emulator
./gradlew :app:installDebug

# Run unit tests (sdk-core has the full test suite; no instrumented SDK tests)
./gradlew :sdk-core:test

# Run a single test class
./gradlew :sdk-core:test --tests "com.perfkit.core.domain.DefaultViolationClassifierTest"

# Publish all SDK modules to Maven Local
./gradlew publishToMavenLocal
```

## Module Architecture

Strict dependency hierarchy — do not violate it:

```
:sdk-api  ←  :sdk-core  ←  :sdk-strictmode
                         ←  :sdk-debug-ui
:app  →  :sdk-core + :sdk-strictmode + :sdk-debug-ui
```

### Module responsibilities

- **`:sdk-api`** — Pure Kotlin contracts only: `ViolationEvent`, `ViolationCategory`, `ViolationSeverity`, `PerfKitConfig`, use case `fun interface`s, domain service interfaces. Zero Android imports.
- **`:sdk-core`** — Infrastructure wiring and the `PerfKit` singleton. Owns `ViolationEventBus` (SharedFlow), `CircularViolationBuffer`, `DefaultViolationClassifier`, `ThrottledViolationDeduplicator`, `AndroidViolationLogger`, and three use case implementations. Manual DI throughout — no Hilt/Koin.
- **`:sdk-strictmode`** — Installs Android `StrictMode` policies. API 28+: `penaltyListener` routes violations to `PerfKit.violationSink`. API 24–27: `penaltyLog()` fallback (Logcat only, no programmatic capture).
- **`:sdk-debug-ui`** — Persistent `NotificationBubbleNotifier` (sticky low-priority notification, no overlay permission) + `ViolationPanelActivity` (Compose, launched via `PerfKit.openDebugPanel(context)` using reflection).
- **`:app`** — Demo showcase. Initializes PerfKit in `PerfKitApp.onCreate()`, demonstrates violation triggers in `MainActivity`, owns all demo-specific UI including the debug FAB.

## Key Design Constraints

- **No DI framework.** Manual factory/singleton wiring throughout. Do not introduce Hilt, Koin, or similar.
- **`PerfKit` singleton** is the only cross-module coupling point. External modules access `PerfKit.violationSink`, `PerfKit.observeViolations`, `PerfKit.observeSummaries`.
- **`openDebugPanel` uses class-name reflection** to avoid a compile-time `sdk-core → sdk-debug-ui` circular dependency.
- **`debugOnly = true` by default.** `PerfKit.initialize()` is a no-op on release builds unless overridden. All debug UI must respect `BuildConfig.DEBUG`.
- **Violation pipeline:** `RawViolation` → `ProcessViolationUseCase` → classify → deduplicate → buffer → event bus → `Flow<List<ViolationEvent>>`.

## API Level Behaviour

- API 28+ (`StrictModeAdapter`): `penaltyListener` gives programmatic capture of typed `android.os.strictmode.*` Throwable subclasses.
- API 24–27: Detection flags + `penaltyLog()` only. Violations appear in Logcat but are not captured in the buffer or shown in the panel.

## Module Boundary Rules

- `sdk-api` must have no Android imports.
- `sdk-core` must not depend on `sdk-strictmode` or `sdk-debug-ui`.
- `sdk-strictmode` and `sdk-debug-ui` must not depend on each other.
- Demo-specific concerns (trigger buttons, demo FAB) must stay in `:app`.
- Keep the public `PerfKit` API minimal — additions need justification.

## Commit Convention

`<type>(<scope>): <summary>` where scope is the module name (e.g. `feat(sdk-core): …`, `fix(app): …`, `docs(readme): …`).
