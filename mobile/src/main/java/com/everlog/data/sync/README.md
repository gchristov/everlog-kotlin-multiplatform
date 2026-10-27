# Data sync

How screens get Firestore data: always from the cache, which syncs with the server when a read needs it.
This replaces the old stores (`ELCollectionStore`, `ELDocumentStore`) and their EventBus events, screen by
screen. Background and plan: TAS-462 in Notion.

In use so far: the exercise library, the user's own exercises and the registry (`ExerciseRepository`: the
exercise picker, the exercise detail's Info tab, the names of the user's own exercises on the workout screen), and workouts
(`WorkoutRepository`: the workout screen's set suggestions, the picker's Recently done), and routines
(`RoutineRepository.routine`, through `LoadTemplateUseCase` for the template editor). The other screens
still read through the old stores, which share the same Firestore cache.

## Rules

- **Screens read the cache, always.** `DataSync.observe` gives a stream of `Load` states:
  - `Loading`: nothing cached and never synced, so the full-screen shimmer.
  - `Ready(value)`: what's cached. It's empty only once the server has said so.
  - `Unavailable(error)`: nothing cached, never synced, and the server can't be reached. Collecting again tries again.

  There's no "syncing" state on screens: new data simply arrives as another `Ready`. `DataSync.status`
  has each source's sync for a Settings sync status.
- **Reading is what syncs.** Callers never start a sync. When a read starts, `DataSync` checks the
  source's sync records and syncs if it's due or doesn't reach back far enough. It does this in its own
  scope, shared with every other read of the source. The result lands in Firestore's cache, and the cache
  listener passes it on.
- **Own changes are live for free.** Readers follow a cache-only listener. Firestore puts local writes in
  the cache straight away, so every open screen shows an edit made on any other, without EventBus or
  reads. Repositories `combine` streams for joined data (e.g. `LoadExerciseLibraryUseCase` joins the
  library, the user's own exercises and the registry).
- **Writes go straight to Firestore** through repositories, and never wait for the server without a
  timeout: an offline write only finishes once back online.
- **Due** means the last sync is older than the max age: Remote Config `data_sync_max_age_minutes`
  (60 by default) in release builds, 0 in debug builds.

## Sources

| Plan | Syncs | For |
|---|---|---|
| `SyncPlan.Whole` | the whole collection when due | small collections: routines, plans, exercises, the library |
| `SyncPlan.Recent(dateField, months)` | the last months when due; the full history, in chunks, the first time a read needs it, then weekly and when the count check finds a difference | big collections: workouts (3 months, `AppConfig.workoutSyncMonths`) |
| `DocumentSource` | the document when due | one document: the registry, the user |

Each read picks its `Range`: `All`, or `Last(months)` for a `Recent` collection. A read further back than
the plan's window waits for the full history (the shimmer, the first time after an install), rather than showing part
of it. Others show what's cached straight away.

After the full history has synced, each due sync of a `Recent` collection also compares the cached count
with the server's (one read per 1,000 documents). A difference means something older was added or deleted
elsewhere, or went missing from the cache, so the full history syncs again. An older document edited on
another phone keeps the count, so an all-time read also syncs the full history again once it's a week old
(Remote Config `data_sync_full_history_max_age_days`, 7 by default, debug builds too), showing the cache
meanwhile.

Long lists (the history) read in pages with `DataSync.pages(source, pageSize)`, newest first, for a
`Recent` collection. Its cache listener grows a page at a time (`loadMore()`), so workouts added or
deleted anywhere don't make rows skip or repeat. Pages come from the cache, which the recent sync fills.
Past what's cached, before the full history has synced, the next page comes from the server (`Page.loadingMore`
for a footer spinner) until it has nothing older (`Page.endReached`). A page that fails is retried by the
next `loadMore()`.

A source's backend is a `FirestoreCollection` or `FirestoreDocument`. Give it every document of the
source (e.g. a user's workouts), the order screens show them in, and how to parse one. A document that
doesn't parse is left out and logged.

## Edge cases it handles

- **Weak signal or offline.** Each server read times out (`AppConfig.dataSyncTimeoutSeconds`). A failed sync isn't recorded, so the next read tries again.
- **Full history interrupted.** It's only recorded once the last chunk has landed. Documents without the date field still sync, since chunks go by document id.
- **Deletions on another phone.** A server read drops the documents it no longer returns (checked by `FirestoreBackendsTest`). Older ones go through the count check.
- **Backups.** The sync records (`DataSync.xml`) and Firestore's cache are left out of backups, so a restored phone syncs instead of trusting an old cache.
- **Signing out.** `AuthManager.logout` calls `signOut()`, which stops syncs, forgets the records and ends every read.
- **Clock moved back.** A sync time in the future counts as due.
- **A sync finishing before the cache shows it.** Firestore can finish a server read before its open listeners
  show what it brought. Each sync records how many documents it brought, and a read whose cache hasn't shown
  anything since then stays where it was rather than showing empty (`withSyncStatus` in `CacheView.kt`).
- **A server read answered from the cache.** Once a cache-only listener has seen a server read of its query,
  Firestore answers the next server read of that exact query from the listener's view, marked as the
  server's. Listeners never use the same query as a server read (see `cacheOnly` in `FirestoreBackends.kt`).
- **Remote Config without a value.** Its `getLong` gives 0 for a key the console doesn't have until the
  defaults load, which would mean syncing on every read, so a value with no source falls back to the default.

## Extending it

A rough guide for what's coming, so each addition fits the split above: `DataSync` keeps the cache in
step and says what a screen can show; repositories join sources into what screens need. Build each
piece when the feature that needs it arrives.

- **Joining (hydration).** Data that points at other data by uuid joins in the repository, with a pure
  function over the sources' flows, not in `DataSync` or a decorator. For example, plans hold routine
  uuids: a plans repository combines the user's plans, their routines (both `SyncPlan.Whole`) and the
  exercise lookup, the way `exerciseLibraryOf` does. That costs no reads beyond the syncs, and a rename
  anywhere reaches every open screen. This replaces `ELPlanDecorator` and `ELRoutineDecorator`, which read
  each reference one at a time. Combine `Load`s with `zip` (wait for both, pass on `Unavailable`) and
  `optional()` for a part the join can do without, as `LoadTemplateUseCase` does.
- **Hydrating on the backend.** Keeping joins as pure functions in repositories means they can move to
  the server: a Cloud Function writes the joined or summarised fields onto the document. The repository
  then reads one source instead of combining several, and screens don't change. A backend that isn't
  Firestore (e.g. a REST API) is a bigger step: `CollectionBackend` is an interface, but reading offline
  and showing edits live rely on Firestore's cache, so such a backend needs a local store of its own.
- **Shared collections** (workouts and plans for everyone, like the exercise library). A shared source
  has `userId = null`, so its records are kept once per device. Plans already live in a top-level `plans`
  collection with `createdByUserId` and `privateToUser`, so public plans are a query on it. Joins work
  across shared and the user's own sources the same way. What a big shared catalogue would need:
  - **Refreshing only what changed.** A `Whole` sync costs every document, for every active user, each
    max age. A `SyncPlan.Since(updatedAt)` reads only what changed since the last sync. It needs every
    write to set `updatedAt` (we write shared collections through our own tools, so that can be
    enforced), and deletions as hidden documents rather than removed ones, as the exercise library does.
    The library itself would get cheaper too.
  - **Paging by something other than date.** `Recent` and `PagedRead` page by a date field. A catalogue
    sorted by name or popularity needs paging by any order.
  - **A source per query.** A source's key is its name and user. Browsing a category or a search needs the
    query's parameters in the key, so each one has its own sync records and max age.
  - **References outside what's synced.** A shared plan pointing at routines no synced collection holds
    needs a "these ids" source. Firestore's `in` queries take at most 30 ids, so it reads in batches.
- **Queries and server reads.** A new kind of server read must never be the same query as a cache
  listener. Firestore answers it from the listener's view (see `cacheOnly` in `FirestoreBackends.kt`).
  Add a case to `FirestoreBackendsTest` that follows the data while syncing it.

## Tests

`DataSyncTest` drives every rule against fake backends (`FakeSyncBackends.kt`), and `SyncRulesTest`
covers the pure rules. `FirestoreBackendsTest` (instrumented) checks the Firestore backends against dev
Firestore, with a second Firebase app as another phone. Repositories built on this test against fakes of
their own `Flow<Load<T>>`s.
