# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Everlog is a native Android fitness/workout-tracking app (package `com.everlog`). Despite the repo name (`everlog-kotlin-multiplatform`), the codebase is currently a **single Android application module** (`:mobile`) — `settings.gradle.kts` only includes `:mobile`, there is no shared/iOS Kotlin Multiplatform module yet. Some leftover references to KMP exist (e.g. a link in `PULL_REQUEST_TEMPLATE.md`) but don't reflect the current module layout.

The app itself is a mixed Kotlin/Java codebase (`mobile/src/main/java`, ~284 `.kt` and ~115 `.java` files) built on an older stack: MVP (not MVVM/Compose), RxJava 1 (`rx.Observable`, not RxJava 2/3), and EventBus — see Architecture below before assuming modern Android patterns (coroutines, Flow, Compose) apply.

## Common commands

Run all commands from the repo root; the Gradle wrapper targets the `:mobile` module.

- Unit tests: `./gradlew testDebugUnitTest` (CI runs `./gradlew --no-daemon --continue testDebugUnitTest`)
- Single unit test class: `./gradlew testDebugUnitTest --tests "com.everlog.SomeTest"`
- Instrumented/E2E tests (needs a connected device or emulator): `./gradlew connectedAndroidTest`
- Build debug APK: `./gradlew :mobile:assembleDebug`
- Build release bundle + upload native symbols: `./gradlew :mobile:bundleRelease :mobile:uploadCrashlyticsSymbolFileRelease`
- Clean: `./gradlew clean`

There is no ktlint/detekt configured — the only lint is Android's built-in lint (bundled into the `bundleRelease`/`assembleRelease` tasks via `isMinifyEnabled`/proguard).

### Local secrets required to build

`mobile/build.gradle.kts` reads `mobile/secrets.properties` (gitignored) via `envSecret(key)`, and will throw a `GradleException` if a required key is missing. Locally you need at least:
- `E2E_TEST_USER_EMAIL`, `E2E_TEST_USER_PASSWORD` (debug build config fields, used by the `LoginActivityTest` instrumented E2E test)
- `KEYSTORE_RELEASE_PASSWORD`, `KEYSTORE_RELEASE_ALIAS`, `KEYSTORE_RELEASE_ALIAS_PASSWORD` (only needed for release signing)

You'll also need `mobile/src/debug/google-services.json` / `mobile/src/release/google-services.json` (Firebase config, gitignored) and debug/release keystores. On CI these are all materialized by `tools/secrets/secrets.sh` from GitHub Actions secrets — see that script for the exact list of files/env vars it produces.

### Versioning

`versionCode`/`versionName` are computed from `tools/versioning/version.txt` and `version_code.txt`, not hand-bumped per build. See `tools/versioning/README.md` for the full scheme (`MmmPTSS` version code format, per-environment suffixes). `tools/versioning/version_code.sh <staging|nightly|master> [--release]` is what CI (and `compute-app-version` action) invokes; it also shells out to `git describe`/`git rev-list` to derive a commit-based sequence number.

## CI/CD

Three workflows in `.github/workflows/`, all built from shared composite actions in `.github/actions/`:
- `staging-check.yml` — runs on push/PR to `master`: unit tests, debug build, instrumented tests, and deploys the debug APK to Firebase App Distribution (`everlog-staging-testers` group).
- `nightly-check.yml` — cron daily: same checks plus a **release** build uploaded to the Play Store internal track, with Slack status reporting to `MONITORING_SLACK_URL`.
- `release-check.yml` — manual (`workflow_dispatch`, requires a changelog input): full release build deployed to the Play Store production track via `fastlane android deploy_play_release`.

Fastlane lanes (`fastlane/Fastfile`): `deploy_firebase`, `deploy_play_internal`, `deploy_play_release`. Changelogs are auto-generated from `git log` since the last tag.

## Architecture

### Pattern: MVP + RxJava 1 + EventBus (not MVVM/coroutines)

- `ui/mvp/BasePresenter` / `BaseMvpView` — every screen has a Presenter attached/detached with the view lifecycle (`attachView`/`detachView`). Presenters hold a `CompositeSubscription` for RxJava subscriptions and register themselves on the global `EventBus` (greenrobot) for cross-screen notifications (e.g. data store updates).
- Async work uses RxJava 1 schedulers (`applySchedulers()`/`applyIOSchedulers()`/`applyUISchedulers()` helpers on `BasePresenter`), not coroutines/Flow.
- Screen-to-screen navigation is abstracted behind the `Navigator` interface (`ui/navigator/Navigator.java`), implemented by `ELNavigator`. Presenters navigate via `this.navigator`, never by constructing `Intent`s directly.
- `ui/activities`, `ui/fragments`, `ui/adapters`, `ui/bottomsheets`, `ui/dialog`, `ui/views` are organized by feature area under `home/` (e.g. `home/workout`, `home/plan`, `home/routine`, `home/exercise`, `home/statistics` via fragments, `home/pro` for paid features).

### Data layer: Firestore-backed stores, not a local DB

- `data/datastores/base/ELDocumentStore` and `ELCollectionStore` are generic base classes wrapping Firebase Firestore `Query`/`CollectionReference`. They handle cache-then-live snapshot listening (`Source.CACHE` first, then `addSnapshotListener`), mutex-guarded single in-flight loads, and CRUD (`create`/`delete`) that no-ops during Firebase Test Lab runs (`DeviceUtils.isFirebaseTestLabRun()`).
- Concrete stores under `data/datastores/{exercises,history,plans,routines,events}` subclass these and emit domain-specific EventBus events (`data/datastores/events/document`, `.../collection`) rather than using callbacks/RxJava for downstream consumers — presenters subscribe via `@Subscribe` methods.
- `data/model` holds Firestore-mapped models (`ELFirestoreModel` base, `ELUser`, `ELRoutine`, `ELIntegration`, etc.) plus feature-specific models under `model/{exercise,plan,pro,set,workout}`.
- `data/migration` implements a lightweight schema-migration system: `DataMigrationManager` tracks a `SCHEMA_VERSION` preference and runs numbered `Migration` implementations (`Migration1`, ...) sequentially against Firestore via `FirestorePathManager` — add new migrations here rather than mutating data ad hoc.

### Managers layer (`managers/`)

Singleton-style managers encapsulate cross-cutting concerns and are the integration point for most external services: `auth/AuthManager` + `auth/LocalUserManager` (Firebase Auth + local session), `billing/BillingManager` + `BillingBridge` (Play Billing / Pro subscriptions), `analytics/AnalyticsManager` (routes to `FirebaseAnalytic` implementations of the `Analytic` interface), `firebase/FirestorePathManager` + `FirebaseStorageManager`, `integrations/GoogleFitIntegrationManager`, `api/ApiManager` (Retrofit-based REST, e.g. cover images), `RemoteConfigManager` (Firebase Remote Config), `preferences/PreferencesManager` + `SettingsManager`. `AppConfig` (`config/AppConfig.kt`) is the single object wiring together tunable constants (rest timers, plan limits, rating-prompt thresholds) and app boot sequencing (`configureApp()` called from `ELApplication.onCreate()`).

### Background work

`services/workout/WorkoutService` + `WorkoutNotificationBuilder` run the active-workout foreground service/notification. `services/fcm/ELFirebaseMessagingService` handles push notifications. `receivers/` holds broadcast receivers.

## Pull requests

Use `PULL_REQUEST_TEMPLATE.md` for PR descriptions (`## What does this pull request change?`, `## Demo`, `## Screenshots`, `## How is this change tested?`). Irrelevant sections (e.g. Demo/Screenshots for a non-UI change) can be dropped.

## Testing reality check

Test coverage is currently minimal: `mobile/src/test/java` has only the default `ExampleUnitTest`, and `mobile/src/androidTest/java` has a single instrumented E2E test (`LoginActivityTest`, which drives a real login using the `E2E_TEST_USER_EMAIL`/`PASSWORD` secrets against a real Firebase project). Don't assume there's an existing unit test suite to model new tests after — check the actual class before assuming test conventions.
