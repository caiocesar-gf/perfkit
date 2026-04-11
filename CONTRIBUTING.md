# Contributing to PerfKit

Thank you for your interest in contributing. This document covers everything you need to get started.

---

## Development Setup

**Requirements:**
- Android Studio Hedgehog or newer
- JDK 17
- Android SDK with API 24 through 36 installed
- A physical device or emulator running API 28+ is strongly recommended for full violation capture

**Clone and build:**

```bash
git clone https://github.com/caiocesar-gf/perfkit.git
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
```

---

## Release Checklist

Releases are fully automated by the GitHub Actions `release` workflow. The only manual step is bumping the version.

### Steps to publish a new release

1. **Update the version** in `gradle.properties`:
   ```properties
   PERFKIT_VERSION=X.Y.Z
   ```

2. **Update `CHANGELOG.md`** — move items from `[Unreleased]` into a new `[X.Y.Z] — YYYY-MM-DD` section.

3. **Commit and push:**
   ```bash
   git add gradle.properties CHANGELOG.md
   git commit -m "chore(release): bump version to X.Y.Z"
   git push origin main
   ```

4. **The release workflow takes over** — it detects that tag `vX.Y.Z` does not yet exist, builds the project, creates the tag, and opens a GitHub Release with auto-generated notes.

5. **JitPack** picks up the new tag within a few minutes and builds the AAR artifacts on first consumer request.

### What NOT to do

- Do not create git tags manually — the workflow handles this.
- Do not push the same `PERFKIT_VERSION` twice without bumping it first; the workflow will skip the release without error.
- Do not modify `PERFKIT_GROUP` without bumping `PERFKIT_VERSION` — changing the groupId without a new release tag leaves existing consumers broken.

---

## Module Structure

| Module | Responsibility |
|---|---|
| `:sdk-api` | Public contracts only — no Android framework imports, no logic |
| `:sdk-core` | Business logic and infrastructure — event bus, buffer, classifier, deduplicator |
| `:sdk-strictmode` | Android StrictMode adapter — depends on `:sdk-core`, no UI |
| `:sdk-debug-ui` | Compose UI layer — depends on `:sdk-core`, no StrictMode knowledge |
| `:app` | Integration sample — depends on all SDK modules |

**Hard rules:**
- `:sdk-api` must have zero Android framework imports.
- `:sdk-core` must not depend on `:sdk-strictmode` or `:sdk-debug-ui`.
- App-specific demo concerns (debug FAB, trigger buttons) must stay in `:app`.
- No DI framework in any SDK module.

---

## Pull Request Guidelines

1. Open an issue before starting significant work.
2. Keep pull requests focused — one logical change per PR.
3. Ensure `./gradlew assemble` and `./gradlew :sdk-core:test` pass locally before opening the PR.
4. Document changes in the `[Unreleased]` section of `CHANGELOG.md`.
5. If your change affects the public API in `:sdk-api`, describe it clearly in the PR description.

---

## Code Style

- Follow the [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html).
- Prefer immutable data structures (`val`, `data class`, `copy()`).
- Keep public API surfaces in `:sdk-api` minimal and stable.
- Compose UI in `:sdk-debug-ui` follows unidirectional data flow: state down, events up.
- Do not suppress lint warnings without an inline comment.

---

## Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <short summary>
```

**Types:** `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `perf`

**Scopes:** `sdk-api`, `sdk-core`, `sdk-strictmode`, `sdk-debug-ui`, `app`

**Examples:**

```
feat(sdk-core): add per-category violation counter to circular buffer
fix(sdk-strictmode): handle penaltyListener registration failure on API 28
chore(release): bump version to 1.1.0
docs(readme): correct JitPack dependency coordinates
```
