"""Pushes exercise library changes to Firestore: the registry and/or library exercises.

The working routine for the registry and the library: export -> edit -> check -> bump -> push. Firestore is
the source of truth, so this starts from what's live: it exports it (that's the backup), applies the local files,
runs the schema check, checks every new image exists, and shows the diff. It writes only with --write, and prod also
needs --confirm-prod. Never edit these documents in the Firebase console: that skips all of this.

    ./tools/exercises/run.sh push --env dev --exercises exercises.json --registry registry.json [--write]
    ./tools/exercises/run.sh push --env dev        # no files: only export what's live to out/backups/

Local files are read from tools/exercises/out/. `--exercises` holds full documents keyed by uuid (only the ones
that change or are new); `--registry` holds the whole registry document with registryVersion bumped.
"""
import argparse
import json
import os
import sys
import urllib.request
from datetime import datetime, timezone

import schema

REGISTRY_DOC = 'global/exercises/registry/current'
LIBRARY = 'global/exercises/all'
# Each environment's own Storage bucket; its exercises' images must be there
IMAGE_BUCKETS = {'dev': 'everlog-9bddf.appspot.com', 'prod': 'everlog-prod.appspot.com'}
BATCH = 400


def firebase_app(repo, env):
    import firebase_admin
    from firebase_admin import credentials
    cred = credentials.Certificate(os.path.join(repo, 'tools', 'firebase', f'admin-credentials-{env}.json'))
    return firebase_admin.initialize_app(cred, {'storageBucket': IMAGE_BUCKETS[env]})


def firestore_client(repo, env, app=None):
    from firebase_admin import firestore
    return firestore.client(app or firebase_app(repo, env))


def export(db):
    doc = db.document(REGISTRY_DOC).get()
    registry = doc.to_dict() if doc.exists else None
    library = {d.id: d.to_dict() for d in db.collection(LIBRARY).stream()}
    return registry, library


def image_exists(url):
    try:
        with urllib.request.urlopen(urllib.request.Request(url, method='HEAD'), timeout=20) as response:
            return response.status == 200
    except Exception:
        return False


def short(value):
    text = json.dumps(value, ensure_ascii=False, default=str)
    return text if len(text) <= 80 else text[:77] + '...'


def diff(live_registry, registry, live, local):
    lines = []
    if registry is not None:
        if live_registry is None:
            lines += ['## Registry: created', ''] + [f'- {k}: {len(v)}' for k, v in registry.items() if isinstance(v, dict)]
        else:
            lines += [f'## Registry: version {live_registry.get("registryVersion")} → {registry.get("registryVersion")}', '']
            for key in schema.REGISTRY_LISTS:
                before, after = live_registry.get(key, {}), registry.get(key, {})
                for entry in sorted(set(before) | set(after)):
                    if before.get(entry) != after.get(entry):
                        state = 'added' if entry not in before else 'changed'
                        lines.append(f'- {key}.{entry} {state}: {short(after.get(entry))}')
        lines.append('')
    new = [u for u in local if u not in live]
    changed = [u for u in local if u in live and local[u] != live[u]]
    lines += [f'## Exercises: {len(new)} new, {len(changed)} changed, {len(live) - len(changed)} unchanged', '']
    for uuid in sorted(new, key=lambda u: local[u]['name']):
        lines.append(f'- **new** {local[uuid]["name"]} ({uuid})')
    for uuid in sorted(changed, key=lambda u: local[u]['name']):
        before, after = live[uuid], local[uuid]
        lines.append(f'- **{before.get("name")}** ({uuid})')
        for field in sorted(set(before) | set(after)):
            if before.get(field) != after.get(field):
                if field not in before:
                    lines.append(f'  - + {field}: {short(after[field])}')
                elif field not in after:
                    lines.append(f'  - − {field}')
                else:
                    lines.append(f'  - {field}: {short(before[field])} → {short(after[field])}')
    return '\n'.join(lines) + '\n'


def write(db, registry, local):
    if registry is not None:
        db.document(REGISTRY_DOC).set(registry)
    uuids = sorted(local)
    for start in range(0, len(uuids), BATCH):
        batch = db.batch()
        for uuid in uuids[start:start + BATCH]:
            batch.set(db.collection(LIBRARY).document(uuid), local[uuid])
        batch.commit()


def run(db, env, registry, local, write_changes, repo, out, pending_images=()):
    """Backs up what's live, checks `registry` (or None) and `local` exercises against it, writes the diff, and
    writes them when `write_changes`. `pending_images` are URLs about to be uploaded, so a dry run doesn't fail on
    them. Returns the live library as it was before the push (empty dict when nothing was pushed)."""
    live_registry, live = export(db)
    stamp = datetime.now(timezone.utc).strftime('%Y%m%d-%H%M%S')
    backup = os.path.join(out, 'backups', f'{env}-{stamp}')
    os.makedirs(backup)
    with open(os.path.join(backup, 'registry.json'), 'w') as f:
        json.dump(live_registry, f, indent=2, ensure_ascii=False, default=str)
    with open(os.path.join(backup, 'exercises.json'), 'w') as f:
        json.dump(live, f, indent=2, ensure_ascii=False, default=str)
    if registry is None and not local:
        print(f'Exported {env} to {os.path.relpath(backup, out)}: registry and {len(live)} exercises. Nothing to push.')
        return {}

    errors = []
    effective_registry = registry if registry is not None else live_registry
    if effective_registry is None:
        errors.append(f'{env} has no registry yet: push one with --registry')
    else:
        merged = {**live, **local}
        errors, warnings = schema.validate(effective_registry, merged, schema.starter_uuids(repo),
                                           previous_registry=live_registry if registry is not None else None,
                                           previous_exercises=live, image_bucket=IMAGE_BUCKETS[env])
        for w in warnings:
            print(f'warning: {w}')
    for uuid, doc in local.items():
        old_media = (live.get(uuid) or {}).get('media') or {}
        for url in [(doc.get('media') or {}).get('image'), (doc.get('media') or {}).get('imageEnd')]:
            known = (old_media.get('image'), old_media.get('imageEnd'), (live.get(uuid) or {}).get('imageUrl'))
            if url and url not in known and url not in pending_images and not image_exists(url):
                errors.append(f'{doc.get("name")}: image not found at {url}')

    report = diff(live_registry, registry, live, local)
    report_path = os.path.join(out, f'push-{env}-{stamp}.md')
    with open(report_path, 'w') as f:
        f.write(report)
    print(report.split('\n## Exercises')[0] if len(report) > 4000 else report)
    print(f'Backup: {os.path.relpath(backup, out)} · full diff: {os.path.basename(report_path)}')

    for e in errors:
        print(f'error: {e}')
    if errors:
        print('Nothing written.')
        sys.exit(1)
    if not write_changes:
        print(f'Dry run against {env}: checks passed, nothing written. Add --write to push.')
        return live
    write(db, registry, local)
    print(f'Written to {env}: {"registry, " if registry is not None else ""}{len(local)} exercises.')
    return live


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument('--env', required=True, choices=['dev', 'prod'])
    parser.add_argument('--registry', help='Registry document to push (file in tools/exercises/out)')
    parser.add_argument('--exercises', help='Exercise documents to push, keyed by uuid (file in tools/exercises/out)')
    parser.add_argument('--write', action='store_true', help='Write to Firestore; without it this is a dry run')
    parser.add_argument('--confirm-prod', action='store_true', help='Required with --env prod --write')
    parser.add_argument('--repo', default='/repo')
    parser.add_argument('--out', default='/out')
    args = parser.parse_args()
    if args.write and args.env == 'prod' and not args.confirm_prod:
        parser.error('writing to prod needs --confirm-prod')

    def load(name):
        with open(os.path.join(args.out, name)) as f:
            return json.load(f)

    registry = load(args.registry) if args.registry else None
    local = load(args.exercises) if args.exercises else {}
    run(firestore_client(args.repo, args.env), args.env, registry, local, args.write, args.repo, args.out)


if __name__ == '__main__':
    main()
