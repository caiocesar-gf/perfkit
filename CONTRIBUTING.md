# Contributing to PerfKit

Thank you for your interest in contributing. This document covers everything you need to get started.

---

## Development Setup

**Requirements:**
- Android Studio Hedgehog or newer
- JDK 17
- Android SDK with API 24 through 35 installed
- A physical device or emulator running API 28+ is strongly recommended for full violation capture

**Clone and build:**

```bash
git clone https://github.com/perfkit/perfkit.git
cd perfkit
./gradlew clean assemble
```

**Run the sample app:**

```bash
./gradlew :app:installDebug
```

**Run unit tests:**

```bash
./gradlew :sdk-core:test
./gradlew :sdk-api:test
```

---

## Module Structure

| Module | Responsibility |
|---|---|
| `:sdk-api` | Public contracts only — no Android framework imports, no logic |
| `:sdk-core` | Business logic and infrastructure — event bus, buffer, classifier, deduplicator |
| `:sdk-strictmode` | Android StrictMode adapter — depends on `:sdk-core`, no UI |
| `:sdk-debug-ui` | Compose UI layer — depends on `:sdk-core`, no StrictMode knowledge |
| `:app` | Integration sample — depends on all SDK modules |

Keep cross-module dependencies aligned with the established graph. `:sdk-api` must not depend on any other SDK module. UI concerns belong exclusively in `:sdk-debug-ui`.

---

## Pull Request Guidelines

1. Open an issue before starting significant work so the approach can be discussed.
2. Keep pull requests focused — one logical change per PR.
3. Ensure `./gradlew assemble` and `./gradlew :sdk-core:test` pass locally before opening the PR.
4. Update `CHANGELOG.md` under the `[Unreleased]` section describing what was added, changed, or fixed.
5. If your change affects the public API surface in `:sdk-api`, document the change in the PR description.
6. PRs that introduce new `debugImplementation` dependencies in `:sdk-api` will not be accepted — the API module must remain a pure Kotlin module with no Android framework dependency.

---

## Code Style

- Follow the [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html).
- Prefer immutable data structures (`val`, `data class`, `copy()`).
- Keep public API surfaces in `:sdk-api` minimal and stable.
- Do not introduce a dependency injection framework into any SDK module — the SDK must remain DI-agnostic.
- Compose UI in `:sdk-debug-ui` should follow unidirectional data flow: state flows down, events flow up.
- Avoid suppressing lint warnings without an inline comment explaining why.

---

## Commit Messages

Follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```
<type>(<scope>): <short summary>
```

**Types:** `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `perf`

**Scopes:** `sdk-api`, `sdk-core`, `sdk-strictmode`, `sdk-debug-ui`, `app`

**Examples:**

```
feat(sdk-core): add per-category violation counter to circular buffer
fix(sdk-strictmode): handle penaltyListener registration failure on API 28
refactor(sdk-debug-ui): extract ViolationRow into a standalone composable
docs(sdk-api): clarify ViolationSeverity mapping in KDoc
```

Keep the summary line under 72 characters. Use the commit body for context when the change is non-obvious.
