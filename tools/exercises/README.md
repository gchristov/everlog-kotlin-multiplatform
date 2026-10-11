# Exercise library tools

Tools for the exercise library's data in Firestore:

- **The library**, `global/exercises/all`: one document per exercise. Besides its name and images, each exercise
  has a `family` (the movement, e.g. Bench Press), the modifiers that make it a variation of that family
  (`equipment`, `position`, `grip`, `laterality`), its `primaryMuscles` and `secondaryMuscles`, its `kind` and
  `tracking`, and `source`, the pack row it came from. `category`, `imageUrl` and `splitSides` are legacy fields,
  still written for older app versions.
- **The registry**, `global/exercises/registry/current`: the ids those fields use (muscles with their group and side,
  muscle groups with their region, equipment, positions, grips, laterality, tracking, kinds) and the families, each
  with a display name.

**Firestore is the source of truth.** Nothing here holds a copy of the data; every change starts from what's live.
`schema.py` has the rules the data must follow.

```
./tools/exercises/run.sh <script> [args]      # from the repo root; runs in Docker
```

Files to push and everything the tools produce live in `tools/exercises/out/` (gitignored). Scripts that read the
licensed Exercise Animatic pack find it at `EXERCISE_PACK_DIR` (default `~/Documents/Archive/Exercises`).

| File | What |
|---|---|
| `push.py` | The only way changes reach Firestore (below) |
| `schema.py` | The checks every push runs |
| `pack.py` | Reads the pack's spreadsheet and illustrations, for imports |

## Changing the registry or the library

1. **Export** what's live: `./tools/firebase/firestore/export_collection.sh global/exercises/all <file>.json admin-credentials-dev.json`
   (the registry is the `current` document in `global/exercises/registry`).
2. **Edit** a copy in `tools/exercises/out/`: the whole registry document, and/or full exercise documents keyed by
   uuid (only the ones that change or are new).
3. **Bump** `registryVersion` when the registry changes (and `schemaVersion` only if what a field means changes).
4. **Dry run**: `./tools/exercises/run.sh push --env dev --registry registry.json --exercises exercises.json`.
   It backs up what's live to `out/backups/`, runs the schema check, checks new images exist, and writes the diff.
5. **Push** with `--write`: dev first, check the app, then `--env prod --write --confirm-prod`.

Never edit these documents in the Firebase console: it skips the checks and the backup.

## Rules

- **uuids and registry ids never change or get reused.** Retire things with `status: hidden`; never delete.
- **Never delete exercise images from Storage.** Apps before 2.10.0 (still in use in October 2026) show the copies
  of exercises stored in routines and workouts, which point at the image they had when saved. A changed image is
  always a new file (apps cache images by URL).
- **Images** for both dev and prod live in the dev project's bucket (`everlog-9bddf`, `exercises/illustrations/`).
  Make them with `tools/image_resize` from the pack's illustrations and upload them with `tools/firebase/storage`
  before pushing the documents that use them.
