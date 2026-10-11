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

The tools use the admin credentials in `tools/firebase/admin-credentials-<env>.json` (gitignored).

1. **Export** what's live: `./tools/exercises/run.sh push --env dev` saves the registry and every exercise to
   `out/backups/<env>-<time>/`.
2. **Edit** a copy in `tools/exercises/out/`: the whole registry document, and/or full exercise documents keyed by
   uuid (only the ones that change or are new). A new exercise from the pack gets its uuid from
   `pack.exercise_uuid(<pack row name>)`.
3. **Bump** `registryVersion` when the registry changes (and `schemaVersion` only if what a field means changes).
4. **Dry run**: `./tools/exercises/run.sh push --env dev --registry registry.json --exercises exercises.json`.
   It backs up what's live again, runs the schema check, checks new images exist and are on the environment's own
   bucket, and writes the diff to `out/push-<env>-<time>.md`.
5. **Push** with `--write`: dev first, check the app, then `--env prod --write --confirm-prod`.

Never edit these documents in the Firebase console: it skips the checks and the backup.

## Rules

- **uuids and registry ids never change or get reused.** Retire things with `status: hidden`; never delete.
- **Never delete or overwrite exercise images in Storage.** Apps before 2.10.0 (still in use in October 2026) show the
  copies of exercises stored in routines and workouts, which point at the image they had when saved, and apps cache
  images by URL. A changed image is always a new file; check the name isn't taken before uploading (the upload
  script overwrites).
- **Images** live in each project's own bucket under `exercises/illustrations/`: `everlog-prod.appspot.com` for prod,
  `everlog-9bddf.appspot.com` for dev. Make them with `tools/image_resize` from the pack's illustrations (square,
  1024 px, WebP) and upload them to both buckets with `tools/firebase/storage` before pushing the documents that
  use them.
- **Legacy fields** (`category`, `imageUrl`, `splitSides`) keep their existing values; `imageUrl` always equals
  `media.image`, and new exercises leave `splitSides` false.
