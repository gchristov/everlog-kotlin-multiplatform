# Android modules

The app is split into Gradle modules, modelled on [newsfeed-kotlin-multiplatform](https://github.com/gchristov/newsfeed-kotlin-multiplatform) but without dependency injection: modules are wired by hand through view-model factories. Most code still lives in `:android:app` and moves into the modules below bit by bit (TAS-468), leaf modules first.

## Layout

```
android/
  app/                       the application: ELApplication, splash, home, Navigator, managers, FCM, receivers
  common/<name>/             setup and code shared by other modules
  <feature>/data/            a feature's repositories, use cases and stores
  <feature>/feature/         a feature's screens and view-models
  <feature>/test-fixtures/   a feature's fakes and test robots, for other modules' tests
```

Features: `auth`, `onboarding`, `exercise`, `workout`, `template`, `plan`, `history`, `pro`, `settings` and `showcase` (the design system showcase). Every feature has all three modules, even while one is empty.

Common modules:

| Module | Holds |
| --- | --- |
| `common:kotlin` | Coroutines, Arrow, `Load`, logging, the app context holder |
| `common:test` | JUnit, Truth, kotlin-test, threetenbp, shared test helpers |
| `common:design` | Shared resources: colours, fonts, dimens, drawables, strings |
| `common:compose` | The design system (theme and elements), `CommonComposeActivity`, shared components |
| `common:compose-test` | Compose UI test dependencies, test manifest and finders |
| `common:mvvm` | `CommonViewModel`, `ViewModelFactory` |
| `common:mvp` | The XML/MVP bases: presenters, base activities, fragments, bottom sheets, dialogs, shared views and adapters |
| `common:firebase` | Firebase, `FirestorePathManager`, the analytics interface |
| `common:persistence` | `PreferencesManager`, `SettingsManager` |
| `common:datasync` | `DataSync` (`data/sync`) |
| `common:data` | The models and stores every feature shares, until they're split into the features' `data` modules |

## Dependency rules

- **app → feature → data → common.** The app depends on every `feature` module. A feature depends on its own `data` module.
- **A feature never depends on another feature.** Screens open each other through the app's `Navigator`.
- **Data never depends on UI**: no `feature` modules, `common:compose`, `common:mvp` or `common:design`.
- **Code moves with its tests**, never ahead of them. A feature's tests go to its `feature` or `data` module when the code they test does, and common modules hold only common code and its tests.
- **Test support goes through `test-fixtures` modules and `common:test`/`common:compose-test`.** Their code lives in `main`, with their dependencies as `api`, because a module's `test` sources can't be shared. A feature's `feature` and `data` modules use its `test-fixtures` for their tests.

## Convention plugins

Each module applies one plugin for its type, from `gradle-plugins/conventions`, through the version catalog:

| Module type | Plugin | Adds |
| --- | --- | --- |
| `common` | `everlog.android.module` (plus `everlog.android.compose` for Compose code) | An Android library on the shared SDK, Java and desugaring setup, with `common:test` for unit tests |
| `<feature>/data` | `everlog.android.data` | The module setup, with `common:kotlin`, `common:firebase`, `common:datasync` and `common:data` as `api` |
| `<feature>/feature` | `everlog.android.feature` | The module setup and Compose, with `common:compose`, `common:design` and `common:mvvm`, and `common:compose-test` for instrumented tests |
| `<feature>/test-fixtures` | `everlog.android.module` | The module setup |
| `common:test` | `everlog.android.base` | The shared setup only, as `everlog.android.module` adds `common:test` itself |
| `app` | `everlog.android.applicationBinary` and `everlog.android.firebase` | The application, version code and name, and google-services, Crashlytics and Performance |

## Adding a module

1. Create `android/<path>/build.gradle.kts` with the plugin for its type and a `namespace` matching its path: `com.everlog.android.<feature>.<type>` or `com.everlog.android.common.<name>`, without dashes (`test-fixtures` becomes `testfixtures`).
2. Include it in `settings.gradle.kts`, and depend on it through the type-safe accessors (`projects.android.exercise.data`).
3. Code moved into the module takes the namespace as its package, except anything the system knows by its full class name: the launcher activity, services, and receivers behind pending alarms and notification intents. These keep their package and name, so an app update can't break them. Move the R8 keep rules for anything Firestore or Gson maps by reflection into the module's `consumerProguardFiles`, and check them with `./gradlew :android:app:minifyReleaseWithR8` and the mapping.
