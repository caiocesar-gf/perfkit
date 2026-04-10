# Changelog

All notable changes to PerfKit will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [1.0.0] — 2026-04-10

### Added

#### Core SDK (`:sdk-core`)
- `PerfKit` singleton with `initialize()`, `openDebugPanel()`, and `clearViolations()` entry points
- `PerfKitConfig` data class with full configuration surface: per-violation-type toggles, buffer size, dedup window, minimum severity threshold, and custom logger injection
- In-memory circular buffer for `ViolationEvent` storage, configurable via `maxBufferSize`
- Event bus for distributing captured violations to registered consumers
- Violation deduplicator with configurable time window (`dedupWindowMs`) to suppress repeated identical violations
- Violation classifier that maps raw StrictMode policy labels to typed `ViolationCategory` and `ViolationSeverity` values
- `ViolationLogger` interface for plugging in custom log sinks

#### Public API contracts (`:sdk-api`)
- `ViolationEvent` — immutable data class carrying id, timestamp, source, category, severity, thread name, message, stacktrace, class name, and policy label
- `ViolationSource` enum: `THREAD_POLICY`, `VM_POLICY`
- `ViolationCategory` enum: `DISK_READ`, `DISK_WRITE`, `NETWORK`, `SLOW_CALL`, `LEAKED_RESOURCE`, `RESOURCE_MISMATCH`, `CLEARTEXT_NETWORK`, `UNTAGGED_SOCKET`, `CUSTOM`, `UNKNOWN`
- `ViolationSeverity` enum: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`
- `PerfKitConfig` contract with default values that are safe for production references
- Use case and domain service interfaces decoupling `:sdk-core` from upper layers

#### StrictMode adapter (`:sdk-strictmode`)
- `StrictModePlugin.install()` — installs ThreadPolicy and VmPolicy based on `PerfKitConfig` flags
- Full programmatic violation capture via `penaltyListener` on API 28+ (Android 9 and above)
- Automatic fallback to `penaltyLog()` on API 24–27 (Android 7–8.1), surfacing violations in Logcat
- Per-policy detection toggles: disk reads, disk writes, network on main thread, slow calls, leaked closable objects, activity leaks, cleartext network traffic, untagged sockets

#### Debug UI (`:sdk-debug-ui`)
- `DebugUiPlugin.install()` — registers a lifecycle observer that activates the debug overlay
- Persistent notification badge displaying the current violation count, updated in real time
- `ViolationPanelActivity` built entirely in Jetpack Compose — scrollable list of `ViolationEvent` entries with category, severity, thread name, timestamp, and expandable stacktrace
- `PerfKit.openDebugPanel(context)` convenience method to launch the panel from anywhere

#### Sample app (`:app`)
- Demonstrative application showing correct SDK initialization in `Application.onCreate()`
- Manual violation triggers for each major category (disk, network, slow call) to validate capture and display
- Example of opening the debug panel from a button press

### Notes

- The SDK enforces `debugOnly = true` by default. When `BuildConfig.DEBUG` is `false`, all plugins are no-ops and no StrictMode policy is applied.
- No dependency injection framework is required or assumed. The SDK is DI-agnostic.
- Minimum supported API level is 24. Full capture is available from API 28.
