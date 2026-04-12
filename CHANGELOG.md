# Changelog

All notable changes to PerfKit will be documented in this file.
Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) · Versioning: [Semantic Versioning](https://semver.org/).

---

## [Unreleased]

<!-- Move items here as you work, then promote them to a versioned section on release. -->

---

## [1.0.2] — 2026-04-12

### Added
- Aggregator POM at root level (`com.github.caiocesar-gf:perfkit:TAG`) — consumers now install with a single `debugImplementation` line instead of four separate module dependencies
- `jitpack.yml` updated to publish the root aggregator alongside the four SDK modules

### Changed
- README Quick Start simplified to single-dependency install story; individual module coordinates moved to "Advanced" collapsible section
- Local development `publishToMavenLocal` now publishes all artifacts (aggregator + modules) in one command

---

## [1.0.1] — 2026-04-11

### Changed
- Publishing groupId corrected to `com.github.caiocesar-gf.perfkit` for native JitPack compatibility
- Version centralised in `gradle.properties` (`PERFKIT_VERSION`); all modules read from it automatically
- Full POM metadata added to all SDK modules: name, description, url, license, developers, scm

### Added
- GitHub Actions CI workflow — runs on pull requests and pushes to `main`
- GitHub Actions Release workflow — creates git tag + GitHub Release when `PERFKIT_VERSION` changes; skips if tag already exists
- `jitpack.yml` — configures JDK 17 and publishes all SDK modules in a single Gradle invocation
- Debug FAB in sample app — persistent bottom-end button (debug builds only); tap opens violation panel, long press opens quick actions sheet
- `SdkStatusBanner` in `ViolationPanelActivity` — shows Monitoring Active, Debug Only, API level, and capture mode
- `CLAUDE.md` — codebase guidance for Claude Code sessions

---

## [1.0.0] — 2026-04-10

### Added

#### Core SDK (`:sdk-core`)
- `PerfKit` singleton with `initialize()`, `openDebugPanel()`, and `clearViolations()` entry points
- `PerfKitConfig` data class with full configuration surface
- In-memory circular buffer, event bus, violation deduplicator, and classifier

#### Public API (`:sdk-api`)
- `ViolationEvent`, `ViolationSource`, `ViolationCategory`, `ViolationSeverity`
- `PerfKitConfig` contract
- Use case and domain service interfaces

#### StrictMode adapter (`:sdk-strictmode`)
- `StrictModePlugin.install()` — `penaltyListener` on API 28+, `penaltyLog()` fallback on API 24–27

#### Debug UI (`:sdk-debug-ui`)
- `DebugUiPlugin.install()` — persistent notification badge with live count
- `ViolationPanelActivity` in Jetpack Compose

#### Sample app (`:app`)
- Demo initialization, violation triggers, panel launch

### Notes
- `debugOnly = true` by default — all plugins are no-ops in release builds
- DI-agnostic — no Hilt, Koin, or similar required
- Min SDK 24; full programmatic capture from API 28
