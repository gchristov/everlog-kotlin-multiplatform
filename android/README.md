# Android modules

The app is split into Gradle modules, modelled on [newsfeed-kotlin-multiplatform](https://github.com/gchristov/newsfeed-kotlin-multiplatform) but without dependency injection: modules are wired by hand through view-model factories. Every feature's screens and data live in its modules (TAS-468). What's left in `:android:app` is the app shell and the old screens still waiting to be removed, see [What stays in the app](#what-stays-in-the-app).

## Layout

```
android/
  app/                       the app shell and the old screens, see "What stays in the app"
  common/<name>/             setup and code shared by other modules
  <feature>/data/            a feature's repositories, use cases and stores
  <feature>/feature/         a feature's screens and view-models
  <feature>/test-fixtures/   a feature's fakes and test robots, for other modules' tests
```

Every feature has all three modules, even while one is empty:

| Feature | `feature` holds | `data` holds |
| --- | --- | --- |
| `auth` | Login, reset password | `LocalUserManager`, the user, device and consent stores and models |
| `onboarding` | The onboarding: welcome, questions, building and the reveal | The starter routines |
| `exercise` | The exercise picker, detail and create screens | `ExerciseRepository`, the exercise library and guide use cases, the exercise stores, `ExerciseImageStorage` |
| `workout` | The workout screen, Set suggestions, workout details, `WorkoutService` and its notification | `WorkoutManager`, the ongoing workout, prefill, the workout stores, `WorkoutBroadcasts` |
| `template` | The template editor, template details, the start-a-workout screen, the template and set type pickers | `RoutineRepository`, the template load and save use cases, the routine stores, `OngoingPlanRepository` |
| `plan` | Plan create, details and week days, the cover picker, the congratulations screen | `PlanManager`, the plan stores and models, the cover images API, `PlanBroadcasts` |
| `history` | The Activity tab: statistics and the workouts calendar | Nothing yet: the tab reads `workout:data` |
| `pro` | The Pro screen, `BillingManager` | The Pro models |
| `settings` | Settings, the integration screen | `SettingsManager`, the integration store and model |
| `showcase` | The design system showcase (debug builds only) | |

Each `data` module gives its stores one entry point, e.g. `WorkoutStores` or `PlanStores`, which the app's `ELDatastore` hands out to the code still using it.

Common modules:

| Module | Holds |
| --- | --- |
| `common:kotlin` | Coroutines, Arrow, `Load`, the date, string, number and unit helpers, logging, `AppContext` (the application context, for code without one of its own), and in `format` the shared display formatters with their strings. Formatters take plain values (counts, weights, durations, a `WeightUnit`), never models, so any module can use them |
| `common:test` | JUnit, Truth, kotlin-test, threetenbp, shared test helpers |
| `common:design` | Shared resources only: colours, fonts, dimens, drawables, strings. No code unless something strictly needs it |
| `common:compose` | The design system (theme and elements), `CommonComposeActivity` and shared components |
| `common:compose-test` | Compose UI test dependencies, test manifest and finders |
| `common:mvvm` | `CommonViewModel`, `ViewModelFactory` |
| `common:mvp` | The XML/MVP bases: presenters, base activities, fragments, bottom sheets and views, the generic prompts, EventBus and the broadcasts every presenter listens to, the vendored adapter |
| `common:firebase` | Firebase, the field names and `FirestorePaths` (each user's collections take the user id) |
| `common:persistence` | `PreferencesManager`, the app's key-value storage. The user's settings (`SettingsManager`) go to `settings:data` |
| `common:session` | The workout session model the workout screen and the template editor edit (blocks, rounds, sets) and its rules. A stopgap until each feature owns its part |
| `common:session-ui` | The exercise table and edit list both screens show. A stopgap until the table has its own UI model and joins the design system |
| `common:datasync` | `DataSync`, the cache-first sync layer (the app wires its instance in `AppDataSync`) |
| `common:data` | The models every feature shares (sets, exercises, routines, workouts), with their R8 keep rule, and the store base classes with their EventBus events, until they're split into the features' `data` modules. Feature-only models live in their feature's `data` module, each with its own keep rule |
| `common:data-test` | Fixtures for building the shared models in tests |

## Dependency rules

The convention plugins enforce these: a module that declares a dependency breaking one fails the build, with the rule it breaks (`gradle-plugins/conventions/…/modules`, unit tested in `ModuleRulesTest`).

- **app → feature → data → common.** The app depends on every `feature` module. A feature depends on its own `data` module.
- **A feature never depends on another feature.** Screens open each other through the app's `Navigator`. A `feature` module uses only its own feature's modules; when it needs another feature's data, its `data` module depends on that feature's `data` module.
- **Data may depend on other features' data**, and on their `test-fixtures` in tests.
- **Data never depends on UI**: no `feature` modules, `common:compose`, `common:compose-test`, `common:mvp` or `common:design`.
- **Nothing depends on the app**, and **common modules never depend on a feature's modules.**
- **Code moves with its tests**, never ahead of them. A feature's tests go to its `feature` or `data` module when the code they test does, and common modules hold only common code and its tests.
- **Test support goes through `test-fixtures` modules and the `common:*-test` modules** (`common:test`, `common:compose-test`, `common:data-test`), which only test configurations (`testImplementation`, `androidTestImplementation`) and other test support may depend on. Their code lives in `main`, with their dependencies as `api`, because a module's `test` sources can't be shared. A feature's `feature` and `data` modules use its `test-fixtures` for their tests.

## What a feature gets from the app

A feature module can't see the app, but some of what it needs lives there for now: services built on the app's managers (analytics, the user, remote config) and other features' screens, which open through the app's `Navigator`. So each feature declares one interface for them, e.g. `ExerciseFeatureDependencies` in `exercise:feature`, and a holder for it (`ExerciseFeature.dependencies`). The app implements it (`application/features`) and registers it first thing in `ELApplication.onCreate`. The feature's activities read it and hand its parts to their view-models' own interfaces, so view-model tests still pass fakes.

`common:mvp` does the same for what its bases need (screen views and the signed-in user check), with `CommonMvpDependencies`.

This stands in for dependency injection while the app is split into modules. Each interface is shaped like constructor injection, a list of what the feature needs, so introducing a DI framework after the modularisation (TAS-468) only replaces how the app registers them; the interfaces and the features stay as they are.

## What stays in the app

`:android:app` keeps what ties the features together or belongs to no single one, and what's due to be deleted rather than moved:

- **The shell**: `ELApplication` with each feature's dependencies (`application/features`), `SplashActivity` (the launcher, which keeps its class name), Home with its Week and Workouts tabs and home notifications, the `Navigator`, `AppConfig` and Remote Config, the data migrations, `AppDataSync`, FCM, the alarm receivers and their notifications, and the web view.
- **Managers on the app's services**: `AuthManager`, `AnalyticsManager`, the rating prompt, in-app updates, `BillingBridge`, `FirestorePathManager`, Google Fit, `ELUserStore` and `ELDatastore`.
- **Shared XML pieces only the app uses**: `AppDialogs` and the other pickers and inputs in `ui/dialog`, and the views and adapters the screens above use.
- **The old screens behind v2 flags** (`CreateRoutineActivity`, `CreateExerciseGroupsActivity`, `MuscleGoalActivity` and their adapters), which go when their flags are removed (TAS-465) rather than moving into modules.

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

1. Create `android/<path>/build.gradle.kts` with the plugin for its type and a `namespace` matching its path: `com.everlog.android.<feature>` for a feature's `feature` module, `com.everlog.android.<feature>.<type>` for its `data` and `test-fixtures` modules, or `com.everlog.android.common.<name>`, without dashes (`test-fixtures` becomes `testfixtures`).
2. Include it in `settings.gradle.kts`, and depend on it through the type-safe accessors (`projects.android.exercise.data`).
3. Resources move with the code that uses them, and resources several modules use go to `common:design`. Each module has its own `R` class, so code reads another module's resources through that module's `R`: in Kotlin `import com.everlog.android.common.design.R as DesignR` and then `DesignR.color.main_accent`, in Java the full name. XML refers to them as before (`@color/main_accent`). A library module can't see the app's resources, so anything a moved resource refers to moves with it.
4. Code moved into the module takes the namespace as its package, except what the system keeps by its full class name outside a running process: the launcher activity (home screen shortcuts), receivers behind scheduled alarms, and anything a notification posted from an alarm points at. These keep their package and name, so an app update can't break them. A foreground service, its notification and the screens they open can be renamed: they all go when the process dies, and an update always kills it. Move the R8 keep rules for anything Firestore or Gson maps by reflection into the module's `consumerProguardFiles`, and check them with `./gradlew :android:app:minifyReleaseWithR8` and the mapping.
