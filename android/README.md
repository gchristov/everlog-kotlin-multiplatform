# Android modules

The app is split into Gradle modules, with [Metro](https://zacsweers.github.io/metro/) for dependency injection (see [Dependency injection](#dependency-injection)). Every feature's screens and data live in its modules. What's left in `:android:app` is the app shell and the old screens still waiting to be removed, see [What stays in the app](#what-stays-in-the-app).

## Layout

```
android/
  app/                       the app shell and the old screens, see "What stays in the app"
  datasync/                  DataSync, how data modules keep Firestore's cache in step with the server
  common/<name>/             setup and code shared by other modules
  <feature>/data/            a feature's repositories, use cases and stores
  <feature>/feature/         a feature's screens and view-models
  <feature>/ui/              UI a feature offers for other features' screens to show
  <feature>/test-fixtures/   a feature's fakes and test robots, for other modules' tests
```

A feature has only the modules it has code for: a `data` module once it has data of its own, `test-fixtures` once other modules' tests need its fakes, and a `ui` module once another feature shows its UI (see [Feature ui modules](#feature-ui-modules)). No empty modules wait for code.

| Feature | `feature` holds | `ui` holds | `data` holds |
| --- | --- | --- | --- |
| `auth` | Login, reset password |  | The signed-in user (`UserSession`, `RealUserSession`), the user, device and consent stores and models |
| `onboarding` | The onboarding: welcome, questions, building and the reveal |  | The starter routines |
| `exercise` | The exercise picker, detail and create screens |  | `ExerciseRepository`, the exercise library and guide use cases, the exercise stores, `ExerciseImageStorage` |
| `workout` | The workout screen, Set suggestions, workout details, `WorkoutService` and its notification |  | `WorkoutManager`, the ongoing workout, prefill, the workout stores, `WorkoutBroadcasts` |
| `workout-builder` | No `feature` module | The exercise table and edit list the workout screen and the template editor show (`exercisetable`) | The workout being built (blocks, rounds, sets), its mapping to and from `ELWorkout` and the rules that edit it |
| `home` | Home's Week tab with its banner (in Compose in `weektab` while Remote Config's `week_tab_v2` is on, the XML one in `week` otherwise), and the Activity tab, which shows the statistics and history pages | | The banner's Remote Config model and whether the user dismissed it (`HomeNotifications`), the Week tab's numbers (`weekStats`) |
| `template` | The template editor, template details, the start-a-workout screen, the template and set type pickers |  | `RoutineRepository`, the template load and save use cases, the routine stores, `OngoingPlanRepository` |
| `plan` | Plan create, details and week days, the cover picker, the congratulations screen | The plan day row (`PlanDayAdapter`) that plan details and the Week tab show, the Pro gate and lock it uses, `PlanAnalytics` | `PlanProgress` (the ongoing plan) and its rules (`OngoingPlanRules`: the current week, the next step, skipping and completing days), the plan stores and models, the cover images API, `PlanBroadcasts` |
| `history` | No `feature` module | The Activity tab's history page: the workouts calendar and list | No `data` module: the history reads `workout:data` |
| `statistics` | No `feature` module | The Activity tab's statistics page and the stats the Week tab shows (`UserStatsController`) | A period's numbers and chart points (`userStats`) for the Compose statistics page |
| `pro` | The Pro screen, `BillingManager` |  | The Pro models |
| `settings` | Settings, the integration screen |  | The user's settings (`UserSettings`, `RealUserSettings`), the integration store and model |
| `showcase` | The design system showcase (debug builds only) |  | No `data` module |

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
| `common:navigation` | The `Navigator` every feature opens other features' and the app's screens through, and the destinations: one per screen opened from outside its feature, with what it's opened with and, for screens that hand something back, how to read their result. The app's `AppNavigator` implements it |
| `common:persistence` | `PreferencesManager`, the app's key-value storage. The user's settings (`UserSettings`) go to `settings:data` |
| `common:data` | The models every feature shares (sets, exercises, routines, workouts), with their R8 keep rule, and the store base classes with their EventBus events, until they're split into the features' `data` modules. Feature-only models live in their feature's `data` module, each with its own keep rule |
| `common:data-test` | Fixtures for building the shared models in tests |

`datasync` is a module of its own: `DataSync`, the cache-first sync layer, and how the data modules that read Firestore through it (`exercise:data`, `template:data`, `workout:data`) keep the cache in step with the server. The app wires its instance in `AppDataSync`. Its `README.md` explains how it works.

## Dependency rules

The convention plugins enforce these: a module that declares a dependency breaking one fails the build, with the rule it breaks (`gradle-plugins/conventions/…/modules`, unit tested in `ModuleRulesTest`).

- **app → feature → data → common.** The app depends on every `feature` module. A feature depends on its own `data` module and on any other feature's `data` module it uses, e.g. `auth:data` for the user or `settings:data` for the settings.
- **Only data modules and the app depend on `datasync`**, each declaring it where it reads through it. Screens read through their feature's data module, and `datasync` itself uses only common modules without UI.
- **A feature never depends on another feature's screens.** Screens open each other through the app's `Navigator`, so a `feature` module never depends on another feature's `feature` module. What one feature shows inside another's screen comes from a `ui` module, which any `feature` module may depend on.
- **A `ui` module depends on other features' `ui` and `data` modules and on common modules, UI ones too**, never on a `feature` module (its own included) or on `datasync`.
- **Data may depend on other features' data**, and on their `test-fixtures` in tests.
- **Each module declares the modules it uses**, as `implementation`. A module exposes another as `api` only when that module's types are part of its own public API (e.g. `auth:data` exposes `pro:data`, as `ELUser` holds the Pro subscription), never to hand another feature's data on to the modules using it. The plugins can't check this one, so check it in review.
- **Data never depends on UI**: no `feature` or `ui` modules, `common:compose`, `common:compose-test`, `common:mvp` or `common:design`.
- **Nothing depends on the app**, and **common modules never depend on a feature's modules.**
- **Code moves with its tests**, never ahead of them. A feature's tests go to its `feature`, `ui` or `data` module when the code they test does, and common modules hold only common code and its tests.
- **Test support goes through `test-fixtures` modules and the `common:*-test` modules** (`common:test`, `common:compose-test`, `common:data-test`), which only test configurations (`testImplementation`, `androidTestImplementation`) and other test support may depend on. Their code lives in `main`, with their dependencies as `api`, because a module's `test` sources can't be shared. A feature's `feature` and `data` modules use its `test-fixtures` for their tests, and other features' `test-fixtures` for the data they use.

## Feature ui modules

A feature's `ui` module holds the UI it offers for other features' screens to show inside them: a tab, a section, a row, with their UI models, view-models and the rules behind them. The feature's own `feature` module uses it too. Two features showing the same domain UI, e.g. the exercise table that the workout screen and the template editor both edit (`workout-builder:ui`), share it through one `ui` module rather than through the app or a common module.

- **A whole screen stays in `feature`** and opens through the `Navigator`. A `ui` module holds what's embedded in someone else's screen.
- **Stateless, domain-free pieces go to the design system** (`common:compose`): a button, a list row, a progress ring. Pieces that know the domain or hold state (sets, workouts, a history list with its view-model) go to the `ui` module of the feature they belong to.
- **XML/MVP code may live in a `ui` module** while it moves, so code can be untangled before it's rebuilt in Compose.

## What a feature gets from the app

A feature module can't see the app, but some of what it needs lives there: services built on the app's managers (analytics, remote config, auth, billing) and other features' screens. Screens open through the shared `Navigator` (`common:navigation`). For the rest each feature declares small interfaces by role, e.g. `ExerciseConfig` and `ExerciseAnalytics`, which app classes implement and bind into the dependency graph (see below). The feature's activities read them from the graph and hand them to their presenters and view-models, so tests still pass fakes.

Services the app shares, like the user's settings, are an interface with a `Real…` implementation and a `Fake…` in the feature's `test-fixtures` (`UserSettings`, `RealUserSettings`, `FakeUserSettings`; `UserSession` for the signed-in user). The graph creates one instance of each. Code that only needs a value, like an adapter showing weights, takes the value; one that lives on while the setting changes, like a Home tab, takes the interface and reads it when it draws.

`common:mvp` and `common:data` still get what their bases need (screen views, the signed-in user check, Test Lab runs) through holders the app registers in `ELApplication.onCreate`, `CommonMvpDependencies` and `CommonDataDependencies`, and the `…Stores` objects get the session the same way, until the graph builds them.

## Dependency injection

[Metro](https://zacsweers.github.io/metro/) builds the app's dependencies at compile time, so a missing one fails the build rather than the app. The base convention plugin applies it to every module.

- **The graph** is `AppGraph` in the app (`AppScope`), created first thing in `ELApplication.onCreate`. Code that can't have its dependencies passed in yet reads it through `ELApplication.graph`.
- **Modules contribute their implementations**: a data module marks its `Real…` class `@Inject @SingleIn(AppScope::class) @ContributesBinding(AppScope::class, binding = binding<…>())`, and the graph binds the interface to it without any wiring in the app. `binding` names the interface when the class has other supertypes, like `PreferencesManager`.
- **Metro only reads Kotlin**: a Java class gets into the graph through a Kotlin `@Provides` function, or by moving it to Kotlin.
- **Activities and presenters** can't take constructor parameters (constructor injection for activities needs API 28, and the app supports 24), so they get what they need from the graph.

- **Every feature** works like this:
  - Its data classes are `@Inject` in their `data` module and contribute their bindings, e.g. `RealExerciseRepository` and the library and guide use cases. They take dispatchers by qualifier (`@IoDispatcher`, `@DefaultDispatcher`, from `common:kotlin`'s `di` package).
  - What only the app can do is a small interface in the feature, bound by an app class: `ExerciseConfig` (Remote Config), the analytics interfaces. Other features' screens open through the `Navigator`.
  - The feature declares what its screens use in one `@ContributesTo(AppScope::class)` interface, `ExerciseGraph`, which the app's graph takes on. Activities and fragments read it with `appGraph<ExerciseGraph>()` (from Java, `appGraph(context, ExerciseGraph.class)`), through the application (`GraphOwner`), and hand presenters their parts in the constructor.
  - `AppBindings` gives the graph what comes from outside it: `DataSync`, the app's settings like how many months of workouts to sync (`@WorkoutSyncMonths`), and the old workouts store. A data module provides its own store the same way, e.g. `TemplateStoreBindings`. The graph takes the application `Context` in its factory.
- **Next**: the `common:mvp` and `common:data` holders, then the stores, which still come from the `…Stores` objects.

## What stays in the app

`:android:app` keeps what ties the features together or belongs to no single one, and what's due to be deleted rather than moved:

- **The shell**: `ELApplication` with each feature's dependencies (`application/features`), `SplashActivity` (the launcher, which keeps its class name), Home (`HomeActivity`, which its tabs reach through `HomeHost`) with its Workouts tab, the `Navigator`, `AppConfig` and Remote Config, the data migrations, `AppDataSync`, FCM, the alarm receivers and their notifications, and the web view.
- **Managers on the app's services**: `AuthManager`, `AnalyticsManager`, the rating prompt, in-app updates, `BillingBridge`, `FirestorePathManager`, Google Fit, `ELUserStore` and `ELDatastore`.
- **Shared XML pieces only the app uses**: `AppDialogs` and the other pickers and inputs in `ui/dialog`, and the views and adapters the screens above use.
- **The old screens behind v2 flags** (`CreateRoutineActivity`, `CreateExerciseGroupsActivity`, `MuscleGoalActivity` and their adapters), which go when their flags are removed rather than moving into modules.

## Convention plugins

Each module applies one plugin for its type, from `gradle-plugins/conventions`, through the version catalog:

| Module type | Plugin | Adds |
| --- | --- | --- |
| `common` | `everlog.android.module` (plus `everlog.android.compose` for Compose code) | An Android library on the shared SDK, Java and desugaring setup, with `common:test` for unit tests |
| `<feature>/data` | `everlog.android.data` | The module setup, with `common:kotlin`, `common:firebase` and `common:data` as `api` |
| `datasync` | `everlog.android.module` | The module setup |
| `<feature>/feature`, `<feature>/ui` | `everlog.android.feature` | The module setup and Compose, with `common:compose`, `common:design`, `common:mvvm` and `common:navigation`, and `common:compose-test` for instrumented tests |
| `<feature>/test-fixtures` | `everlog.android.module` | The module setup |
| `common:test` | `everlog.android.base` | The shared setup only, as `everlog.android.module` adds `common:test` itself |
| `app` | `everlog.android.applicationBinary` and `everlog.android.firebase` | The application, version code and name, and google-services, Crashlytics and Performance |

## Adding a module

1. Create `android/<path>/build.gradle.kts` with the plugin for its type and a `namespace` matching its path: `com.everlog.android.<feature>` for a feature's `feature` module, `com.everlog.android.<feature>.<type>` for its `data`, `ui` and `test-fixtures` modules, or `com.everlog.android.common.<name>`, without dashes (`test-fixtures` becomes `testfixtures`).
2. Include it in `settings.gradle.kts`, and depend on it through the type-safe accessors (`projects.android.exercise.data`).
3. Resources move with the code that uses them, and resources several modules use go to `common:design`. Each module has its own `R` class, so code reads another module's resources through that module's `R`: in Kotlin `import com.everlog.android.common.design.R as DesignR` and then `DesignR.color.main_accent`, in Java the full name. XML refers to them as before (`@color/main_accent`). A library module can't see the app's resources, so anything a moved resource refers to moves with it.
4. Code moved into the module takes the namespace as its package, except what the system keeps by its full class name outside a running process: the launcher activity (home screen shortcuts), receivers behind scheduled alarms, and anything a notification posted from an alarm points at. These keep their package and name, so an app update can't break them. A foreground service, its notification and the screens they open can be renamed: they all go when the process dies, and an update always kills it. Move the R8 keep rules for anything Firestore or Gson maps by reflection into the module's `consumerProguardFiles`, and check them with `./gradlew :android:app:minifyReleaseWithR8` and the mapping.
