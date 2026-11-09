# DataSync

How the app reads Firestore data: always from Firestore's local cache, which `DataSync` keeps in step with the
server when a read needs it. It replaces the old stores (`ELCollectionStore`, `ELDocumentStore`) and their
EventBus events, screen by screen.

Two things shape everything here:

- **Memory.** Firestore keeps a decoded copy of every document a listener's query matches, for as long as it
  listens. One copy of 2,000 workouts is about 70 MB, and phones give an app 256 MB or less. So nothing follows a
  long history whole: each read says how much it needs, and only that is held.
- **Cost.** Every document read from the server is a billed read. So syncs bring only what's due or missing,
  and screens read the cache.

## How a read works

1. A screen asks its feature's repository, e.g. `WorkoutRepository`, which asks `DataSync`. Screens never use
   `DataSync` directly, and only data modules and the app depend on this module (the module rules enforce it).
2. `DataSync` answers from the cache straight away.
3. Reading is what syncs. `DataSync` checks the source's sync records: if the cache is due a refresh, or doesn't
   reach as far back as this read goes, it starts a sync in its own scope, shared with every other read of the
   same source. Callers never start a sync themselves.
4. The sync writes into Firestore's cache, and the read's cache listener passes the new data on as another
   `Ready`.

## Which read to use

Pick the one that holds the least for what the screen does. A `Recent` collection (workouts) has no "all of it":
every read says how far back it goes.

| The screen needs | Use | Holds in memory | Syncs |
|---|---|---|---|
| A small collection, live: exercises, routines, plans | `observe(source)` on a `SyncPlan.Whole` source | The collection | The whole collection when due |
| The last months of a big collection, live, e.g. this week or month | `observe(source, Range.Last(months))` | Those months | The recent window when due, and further back the first time if `months` is longer than it |
| A calculation over a period, once, e.g. suggestions from the last year | `read(source, from, until)` | Nothing once read | As for `observe` over the same period, and it waits for that |
| A long list the user scrolls, e.g. the history | `pages(source, pageSize)` | The pages shown so far | The recent window; older pages from the server as the user reaches them |
| A few documents a read found missing | `fetchIds(source, ids)` | Nothing | Just those documents, 30 per server read |
| Where a long history starts, to go back through it a year at a time, e.g. an exercise's history | `oldest(source)` | Nothing | One server read; offline, the date it last gave |
| One document, live: the registry, the user | `observe(source)` on a `DocumentSource` | The document | The document when due |

Rules of thumb:

- **Don't follow more than the screen shows.** A list of this week's workouts observes `Range.Last(1)`, not the
  year. A calculation reads once rather than observing.
- **Read the cache in a few large parts, not many small ones.** Without an index, Firestore's cache goes through
  every document of the collection for each query, and keeps only the ones a filter matches. The app turns on the
  SDK's automatic cache indexes (`AppConfig.configureFirestore()`): for a query that scans far more than it keeps,
  like the workouts by date on a long history, the SDK builds a local index on that field in the background, about
  50 documents a minute while the app runs, carrying on from where it stopped next launch. Until it's full, and on
  a small collection, which gets none, queries still scan. `limit` without an indexed order doesn't help, as it's
  applied after everything is decoded. `read` goes by date for this reason.
- **For "all time" totals, don't read everything.** Store a running total instead (see Extending it).

## What a screen sees

`observe` gives a stream of `Load` states (`Load` lives in `common:kotlin`, as every module returns it):

- `Loading`: nothing cached and never synced, so the full-screen shimmer.
- `Ready(value)`: what's cached. It's empty only once the server has said so.
- `Unavailable(error)`: nothing cached, never synced, and the server can't be reached. Collecting again tries
  again.

There's no "syncing" state on screens: new data simply arrives as another `Ready`. `DataSync.status` has each
source's sync, for a Settings sync status.

Some reads wait for their sync rather than showing part of the data:

- A read further back than the recent window waits, the first time, for that part to sync.
- A source made with `completeOnly` waits for its first sync: the exercise library and the user's own exercises,
  whose cache can hold a few documents read one at a time, and which the picker searches as a whole.
- `observe(source, range, complete = true)` asks the same of one read: for a calculation over a range, which a part
  of it would get wrong, while lists of the same source show what's cached.

`read` always waits: it returns once the cache reaches back to `from`, or `Unavailable` if it can't sync.

## Live updates

- **Own changes are live for free.** Readers follow a cache-only listener, and Firestore puts local writes in the
  cache straight away, so every open screen shows an edit made on any other, without EventBus or reads.
- **Other phones' changes arrive with the next sync**, not in real time: hourly for the recent window, and for the
  older part when the count check finds a difference or once a week.
- **Writes go straight to Firestore** through repositories, and never wait for the server without a timeout: an
  offline write only finishes once back online.
- Repositories `combine` streams for joined data, e.g. `LoadExerciseLibraryUseCase` joins the library, the
  user's own exercises and the registry.

## Syncing

| Plan | Syncs | For |
|---|---|---|
| `SyncPlan.Whole` | the whole collection, in one read, when due | small collections: routines, plans, exercises, the library |
| `SyncPlan.Recent(dateField, months)` | the last months when due; further back only as far as a read goes | big collections: workouts (3 months, `AppConfig.workoutSyncMonths`) |
| `DocumentSource` | the document when due | one document: the registry, the user |

**Due** means the last sync is older than the max age: Remote Config `data_sync_max_age_minutes` (60 by default)
in release builds, 0 in debug builds, so debug builds sync on every read.

A `Recent` collection keeps two records per user, in `DataSync.xml`:

- **`lastSynced`**: when the recent window last synced. A due read syncs the window again, in one server read of
  everything dated within it.
- **`syncedBackTo` and `syncedBackAt`**: how far back beyond the window the cache is kept in step, and when that
  part last synced. It only exists once a read has reached back.

Reaching back:

- **The first time** a read goes further back than `syncedBackTo` (or the window, before there's one), it syncs
  just the missing stretch, from the read's date up to `syncedBackTo` (or the window), keeping `syncedBackAt`, so
  going back a year at a time fetches each year once. When the stretch synced back before is due its weekly sync,
  it syncs everything from the read's date up to the window instead. Either way by date, oldest first, in pages of 200
  (`AppConfig.dataSyncChunkSize`), ordered by date and id so documents of the same date aren't skipped. It's
  recorded once the last page has landed, so an interrupted sync starts over.
- **Every hour**, with the recent window, it compares the server's count of the synced-back stretch (one read per
  1,000 documents) with the cache's. The cache is counted a year at a time, so it never holds more than that. A
  difference means something older was added or deleted elsewhere, or went missing from the cache, so the stretch
  syncs again.
- **Once a week**, a read reaching back syncs the stretch again, for older documents edited on another phone,
  which keep the count (Remote Config `data_sync_full_history_max_age_days`, named after what it used to be for,
  7 days by default, debug builds too). It only goes as far back as that read, so what's kept fresh doesn't grow
  over time, and shows the cache meanwhile.

Documents older than `syncedBackTo` stay in the cache, they're just not kept fresh. A `Recent` collection's
documents without the date field never sync or show, as every read is by date.

## Pages

`pages(source, pageSize)` reads newest first, for a `Recent` collection. Its cache listener grows a page at a time
(`loadMore()`), so documents added or deleted anywhere don't make rows skip or repeat. Pages come from the cache,
which the recent sync fills. Past what's cached, the next page comes from the server (`Page.loadingMore` for a
footer spinner) until a page comes back short (`Page.endReached`). A page that fails is tried again by the next
`loadMore()`.

## Documents missing from the cache

`fetchIds(source, ids)` brings in documents a read found missing between syncs, e.g. a library exercise added
since the library last synced, or one the user created on another phone: a server read of just those ids (30 per
read, Firestore's limit for `in`), into the cache, where every reader sees them. It says which exist, so callers
can stop asking for the rest. `ExerciseLookup` uses it for the old stores' routine decorator.

## Edge cases it handles

- **Weak signal or offline.** Each server read times out (`AppConfig.dataSyncTimeoutSeconds`). A failed sync isn't
  recorded, so the next read tries again.
- **Deletions on another phone.** A server read drops the documents it no longer returns (checked by
  `FirestoreBackendsTest`). Older ones go through the count check.
- **Updating from an older version.** Its full history records (`fullHistorySynced:`) aren't read, so the phone
  syncs back once what its reads need.
- **Backups.** The sync records (`DataSync.xml`) and Firestore's cache are left out of backups, so a restored phone
  syncs instead of trusting an old cache.
- **Signing out.** `AuthManager.logout` calls `signOut()`, which stops syncs, forgets the records and ends every
  read.
- **Clock moved back.** A sync time in the future counts as due.
- **A sync finishing before the cache shows it.** Firestore can finish a server read before its open listeners show
  what it brought. Each sync records how many documents it brought, and a read whose cache hasn't shown anything
  since then stays where it was rather than showing empty (`withSyncStatus` in `CacheView.kt`).
- **A server read answered from the cache.** Once a cache-only listener has seen a server read of its query,
  Firestore answers the next server read of that exact query from the listener's view, marked as the server's.
  Listeners never use the same query as a server read (see `cacheOnly` in `FirestoreBackends.kt`).
- **Remote Config without a value.** Its `getLong` gives 0 for a key the console doesn't have until the defaults
  load, which would mean syncing on every read, so a value with no source falls back to the default.

## Who uses it

- `ExerciseRepository` (`exercise:data`): the exercise library, the user's own exercises and the registry, for the
  exercise picker, the exercise detail's Info tab, the workout screen, and `ExerciseLookup`, which the old stores'
  routine decorator resolves exercises through.
- `WorkoutRepository` (`workout:data`): the workout screen's set suggestions, read back a year at most, a window at
  a time (`prefillHistory`), and the picker's Recently done.
- `RoutineRepository` (`template:data`): one routine for the template editor, and the routines for the picker's
  Most used.
- The app wires the one `DataSync` in `AppDataSync`.

The other screens still read through the old stores, which share the same Firestore cache. Home's old workouts
store still follows the whole history, which is most of the memory a long history costs today, until Home moves
onto `DataSync`.

## Extending it

A rough guide for what's coming, so each addition fits the split above: `DataSync` keeps the cache in step and
says what a screen can show; repositories join sources into what screens need. Build each piece when the feature
that needs it arrives.

- **Running totals for "all time".** Statistics over everything (total volume, workouts ever) shouldn't read the
  whole history. Keep a summary document per user, updated when a workout is saved or by a Cloud Function, and
  read it as a `DocumentSource`.
- **Joining (hydration).** Data that points at other data by uuid joins in the repository, with a pure function
  over the sources' flows, not in `DataSync` or a decorator. For example, plans hold routine uuids: a plans
  repository combines the user's plans, their routines (both `SyncPlan.Whole`) and the exercises, the way
  `exerciseLibraryOf` does. That costs no reads beyond the syncs, and a rename anywhere reaches every open screen.
  This replaces `ELPlanDecorator` and `ELRoutineDecorator`. Combine `Load`s with `zip` (wait for both, pass on
  `Unavailable`) and `optional()` for a part the join can do without, as `LoadTemplateUseCase` does.
- **Hydrating on the backend.** Keeping joins as pure functions in repositories means they can move to the server:
  a Cloud Function writes the joined or summarised fields onto the document. The repository then reads one source
  instead of combining several, and screens don't change. A backend that isn't Firestore (e.g. a REST API) is a
  bigger step: `CollectionBackend` is an interface, but reading offline and showing edits live rely on Firestore's
  cache, so such a backend needs a local store of its own.
- **Shared collections** (workouts and plans for everyone, like the exercise library). A shared source has
  `userId = null`, so its records are kept once per device. Plans already live in a top-level `plans` collection
  with `createdByUserId` and `privateToUser`, so public plans are a query on it. What a big shared catalogue would
  need:
  - **Refreshing only what changed.** A `Whole` sync costs every document, for every active user, each max age.
    A `SyncPlan.Since(updatedAt)` reads only what changed since the last sync. It needs every write to set
    `updatedAt`, and deletions as hidden documents rather than removed ones, as the exercise library does.
  - **Paging by something other than date.** `Recent` and `pages` go by a date field. A catalogue sorted by name
    or popularity needs paging by any order.
  - **A source per query.** A source's key is its name and user. Browsing a category or a search needs the query's
    parameters in the key, so each one has its own sync records and max age.
  - **References outside what's synced.** A shared plan pointing at routines no synced collection holds can bring
    them in with `fetchIds`. Showing them as a list of their own would need a source whose cache listener follows
    just those ids.
  - **Copying instead of referencing.** Copying a shared plan and its routines into the user's own collections
    when they add or start it keeps their plans local (offline, editable, unchanged by later edits to the shared
    one), at the cost of library fixes not reaching copies. Either way the repository joins them, so screens don't
    change.
- **Queries and server reads.** A new kind of server read must never be the same query as a cache listener.
  Firestore answers it from the listener's view (see `cacheOnly` in `FirestoreBackends.kt`). Add a case to
  `FirestoreBackendsTest` that follows the data while syncing it.

## Tests

`DataSyncTest` drives every rule against fake backends (`FakeSyncBackends.kt`), and `SyncRulesTest` covers the
pure rules. `FirestoreBackendsTest` (instrumented, in the app) checks the Firestore backends against dev
Firestore, with a second Firebase app as another phone. Repositories built on this test against fakes of their own
`Flow<Load<T>>`s.
