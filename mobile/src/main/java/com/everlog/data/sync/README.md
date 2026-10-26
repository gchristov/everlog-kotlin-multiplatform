# Data sync

How screens get Firestore data: always from the cache, which syncs with the server when a read needs it.
This replaces the old stores (`ELCollectionStore`, `ELDocumentStore`) and their EventBus events, screen by
screen. Background and plan: TAS-462 in Notion.

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
  reads. Repositories `combine` streams for joined data (e.g. exercise names in workouts).
- **Writes go straight to Firestore** through repositories, and never wait for the server without a
  timeout: an offline write only finishes once back online.
- **Due** means the last sync is older than the max age: Remote Config `data_sync_max_age_minutes`
  (60 by default) in release builds, 0 in debug builds.

## Sources

| Plan | Syncs | For |
|---|---|---|
| `SyncPlan.Whole` | the whole collection when due | small collections: routines, plans, exercises, the library |
| `SyncPlan.Recent(dateField, months)` | the last months when due; the full history once, in chunks, the first time a read needs it | big collections: workouts |
| `DocumentSource` | the document when due | one document: the registry, the user |

Each read picks its `Range`: `All`, or `Last(months)` for a `Recent` collection. A read further back than
the plan's window waits for the full history (the shimmer, once per install), rather than showing part
of it. Others show what's cached straight away.

After the full history has synced, each due sync of a `Recent` collection also compares the cached count
with the server's (one read per 1,000 documents). A difference means something older changed elsewhere
or went missing from the cache, so the full history syncs again.

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

## Tests

`DataSyncTest` drives every rule against fake backends (`FakeSyncBackends.kt`), and `SyncRulesTest`
covers the pure rules. `FirestoreBackendsTest` (instrumented) checks the Firestore backends against dev
Firestore, with a second Firebase app as another phone. Repositories built on this test against fakes of
their own `Flow<Load<T>>`s.
