# Exercise library

The app's global exercise library is data in Firestore, not code. This folder has the tools to change it safely and
is the reference for how it's shaped. Keep it in step with `schema.py`.

**Firestore is the source of truth**, in dev and prod separately. Nothing here holds a copy of the data; every
change starts from what's live and goes through `push.py`. Never edit these documents in the Firebase console: that
skips the checks and the backup.

## The model

### Families and variations

- A **family** is the movement lifters name: Bench Press, Squat, Row. It groups exercises for browsing and search.
- A **variation** is one exercise in a family, told apart by four modifiers: `equipment` (one main piece, plus
  `accessories`), `position` (the set-up: angle, body position, direction or where the load sits), `grip` (or
  stance) and `laterality` (both sides, alternating, one side). Every variation is its own exercise with its own
  uuid, history, 1RM and prefill; history is never merged.
- A **substitute** (what a swap would offer) is in the same family with the same position and grip, and differs
  only in equipment, or both ↔ alternating. One-arm or one-leg versions aren't substitutes; neither is flat ↔
  incline, where the emphasis moves (the muscles say so).
- A **generic** exercise has no `equipment`: not specified, whatever the person used (not the same as
  `bodyweight`). Some older exercises are generic because people logged them with different equipment. They have no
  substitutes.
- Set techniques (pause, tempo, 21s) aren't variations.

### An exercise: `global/exercises/all/{uuid}`

| Field | Type | Meaning |
|---|---|---|
| `uuid` | string | Never changes: routines, workouts and the onboarding's starter routines point at it |
| `name` | string | Shown everywhere, past workouts included (the app shows the library's version of each exercise) |
| `schemaVersion` | int | 2. Goes up only when what a field means changes |
| `status` | `active` · `hidden` | Hidden exercises stay readable by history but leave the library. Nothing is deleted |
| `family` | family id · null | Null only for users' own exercises |
| `familyDefault` | boolean | The family's main exercise, shown first. Exactly one per family |
| `equipment` | equipment id · null | The main equipment (`kind: main` in the registry). Null means generic |
| `accessories` | equipment ids | What else it needs (`kind: accessory`): bench, box, rope |
| `position` | position ids | The set-up, e.g. `["overhead", "seated"]`. Empty is the standard set-up |
| `grip` | grip ids | Grip or stance, e.g. `["close"]`. Empty is the standard grip |
| `laterality` | `both` · `alternating` · `single` | |
| `primaryMuscles` | muscle ids | The muscles it targets, most first: worked for a lift, stretched for a stretch |
| `secondaryMuscles` | muscle ids | Never also primary |
| `kind` | kind id | strength, plyometric, mobility, cardio. Lets stats tell lifts from stretches |
| `tracking` | tracking id | The default inputs of a set (weight and reps, reps, time), not a restriction |
| `aliases` | strings | Other names to search by, e.g. a renamed exercise's old name |
| `instructions`, `tips` | strings | Steps and form tips, from the pack |
| `media` | `{ image, imageEnd, video }` | Start pose, end pose (a combination move shows both halves), demo video |
| `source` | `{ pack, id }` · null | The pack row it came from. Makes imports repeatable and catches duplicates |
| `category` | legacy | One of 6 (CHEST, BACK, LEGS, SHOULDERS, ARMS, ABS). Kept for older apps; existing values never change, new exercises take their first primary muscle's group's category |
| `imageUrl` | legacy | Always equal to `media.image` |
| `splitSides` | legacy | Kept as it is (four one-sided exercises are true in prod); new exercises leave it false. `laterality` replaces it |
| `createdDate` | long | |

Users' own exercises (`users/{uid}/exercises`) have the same shape with every newer field optional.

### The registry: `global/exercises/registry/current`

One document with the ids exercises use, each with a `name` (one language for now; translations later, for the
registry and exercise names together), an `order` and a `status`:

- `muscles` (each with its `group` and the body `side` it's on, front or back), `muscleGroups` (each with its
  `region` and legacy `category`), `regions` (upper body, lower body, core)
- `equipment` (each `main` or `accessory`), `positions`, `grips`, `laterality`, `tracking`, `kinds`
- `families` (each with `aliases` for search, its main `primaryMuscles`, and `combination` for moves like "curl to
  press")
- `schemaVersion`, and `registryVersion`, which goes up on every change

It holds only what the library uses, and grows as exercises need more. That's how filters by muscle, group, area,
body side, equipment, set-up, grip and kind, a body map and families can be built without app releases for data.

### Rules for apps reading this

Apps must handle data newer than they are, so the registry and library can change without a release:
unknown fields are ignored; an unknown family means none; an unknown muscle falls back to its group, or its id as a
name; unknown equipment, positions or grips show their id and are left out of filters; an unknown tracking means
weight and reps; hidden entries stay in history and routines but leave the library and filters; a newer
`schemaVersion` or `registryVersion` still loads.

Until the registry is loaded, show what the exercise itself has. Older app versions read exercises with
`toObject(ELExercise)`, which skips the newer fields.

## Rules for the data

- **uuids and registry ids never change or get reused.** Retire things with `status: hidden`; never delete.
- **Never delete or overwrite exercise images in Storage.** Apps before 2.10.0 (still in use in October 2026) show
  the copies of exercises stored in routines and workouts, which point at the image they had when saved, and apps
  cache images by URL. A changed image is always a new file.
- **Each environment's exercises use its own bucket** under `exercises/illustrations/`: `everlog-prod.appspot.com`
  for prod, `everlog-9bddf.appspot.com` for dev. Images are square, 1024 px WebP, made with `tools/image_resize`.
- **No two active exercises share an image**, and **variations in a family must differ** in at least one modifier.
- **The starter routines' exercises** (`StarterExercises.kt`) must exist and be active in every environment the
  app runs against. Add new ones to the library before an app release points at them.

`schema.py` checks all of this on every push.

## The tools

They run in Docker from the repo root with `./tools/exercises/run.sh <script> [args]`, use the admin credentials in
`tools/firebase/admin-credentials-<env>.json` (gitignored), and read the licensed Exercise Animatic pack from
`EXERCISE_PACK_DIR` (default `~/Documents/Archive/Exercises`) when they need it. Everything they write goes to
`tools/exercises/out/` (gitignored), including a backup of what was live before every push.

| File | What |
|---|---|
| `import.py` | Adds exercises from the pack: proposes, you review, it applies |
| `push.py` | The only way changes reach Firestore |
| `schema.py` | The checks every push runs |
| `pack.py` | Reads the pack's spreadsheet and illustrations; gives an imported exercise a stable uuid |

### Adding exercises from the pack

```
./tools/exercises/run.sh import propose --env dev "Cable lateral raises" "Leg press machine normal stance"
```

Finds each pack row (names as in the pack's spreadsheet; it suggests close ones if a name doesn't match) and proposes
every field: a name in the library's style, the family, modifiers, muscles, kind, tracking and image, plus any
registry entries they need. It refuses exercises already in the library. Review `out/import-<time>.md`, especially
the names, families and muscles, and edit `out/import-<time>.json` where needed. Then:

```
./tools/exercises/run.sh import apply --env dev import-<time>.json             # dry run
./tools/exercises/run.sh import apply --env dev import-<time>.json --write
```

`apply` adds the registry entries (bumping `registryVersion`), makes and uploads the images the environment's bucket
doesn't have yet (never overwriting), and pushes through `push.py`. Check the app on dev, then apply the same file
with `--env prod --write --confirm-prod`.

### Any other change

1. **Export** what's live: `./tools/exercises/run.sh push --env dev` saves the registry and every exercise to
   `out/backups/<env>-<time>/`.
2. **Edit** a copy in `out/`: the whole registry document, and/or full exercise documents keyed by uuid (only the
   ones that change or are new).
3. **Bump** `registryVersion` when the registry changes.
4. **Dry run**: `./tools/exercises/run.sh push --env dev --registry registry.json --exercises exercises.json`. It
   backs up what's live, runs the checks, checks new images exist on the environment's bucket, and writes the diff
   to `out/push-<env>-<time>.md`.
5. **Push** with `--write`: dev first, check the app, then `--env prod --write --confirm-prod`.
