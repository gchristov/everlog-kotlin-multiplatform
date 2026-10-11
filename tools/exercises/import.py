"""Imports exercises from the Exercise Animatic pack into the library, in two steps with a review in between.

    ./tools/exercises/run.sh import propose --env dev "Cable lateral raises" "Leg press machine normal stance"
    # review (and edit) out/import-<time>.json; out/import-<time>.md summarises it
    ./tools/exercises/run.sh import apply --env dev import-<time>.json            # dry run
    ./tools/exercises/run.sh import apply --env dev import-<time>.json --write    # then prod with --confirm-prod

`propose` finds each pack row and proposes every field from its name and columns (family, equipment, set-up, grip,
laterality, muscles, kind, tracking, image), plus any registry entries they need. It flags what needs a look and
refuses duplicates. `apply` adds the registry entries (bumping registryVersion), uploads images that aren't in the
environment's bucket yet, and pushes through push.py, so the schema check, backup and diff all apply. The same
reviewed file is applied to dev and then prod; image URLs follow the environment.
"""
import argparse
import difflib
import json
import os
import re
import shutil
import sys
import tempfile
import time
from collections import defaultdict
from datetime import datetime, timezone
from urllib.parse import quote

import push
from pack import PACK_ID, Pack, exercise_uuid, muscles, normalise

# Words in pack names -> registry ids. Longer phrases are matched first.
EQUIPMENT = {
    'resistance band': 'band', 'loop band': 'band', 'band': 'band', 'ez bar': 'ez_bar', 'ez-bar': 'ez_bar', 'ez': 'ez_bar',
    'trap bar': 'trap_bar', 'smith machine': 'smith_machine', 'smith': 'smith_machine', 'suspension trainer': 'suspension',
    'trx': 'suspension', 'pull up bar': 'pull_up_bar', 'pull-up bar': 'pull_up_bar', 'barbell': 'barbell',
    'dumbbells': 'dumbbell', 'dumbbell': 'dumbbell', 'kettlebells': 'kettlebell', 'kettlebell': 'kettlebell',
    'cable': 'cable', 'machine': 'machine', 'lever': 'machine', 'plate': 'plate', 'bodyweight': 'bodyweight',
    'ab wheel': 'ab_wheel', 'jump rope': 'jump_rope',
}
LATERALITY = {'single arm': 'single', 'one arm': 'single', 'single leg': 'single', 'one leg': 'single',
              'single': 'single', 'alternating': 'alternating', 'alternate': 'alternating'}
GRIPS = {'close grip': 'close', 'close-grip': 'close', 'wide grip': 'wide', 'neutral grip': 'neutral', 'hammer grip': 'neutral',
         'reverse grip': 'underhand', 'underhand': 'underhand', 'supinated grip': 'underhand', 'overhand': 'overhand',
         'pronated grip': 'overhand', 'sumo': 'sumo', 'wide stance': 'wide', 'close stance': 'close', 'narrow stance': 'close'}
POSITIONS = {'incline': 'incline', 'decline': 'decline', 'flat bench': 'flat', 'seated': 'seated', 'sitting': 'seated',
             'standing': 'standing', 'lying': 'lying', 'bent over': 'bent_over', 'bent-over': 'bent_over',
             'kneeling': 'kneeling', 'hanging': 'hanging', 'chest supported': 'chest_supported', 'preacher': 'preacher',
             'overhead': 'overhead', 'goblet': 'goblet', 'front': 'front', 'deficit': 'deficit', 'floor': 'floor',
             'low to high': 'low_to_high', 'high to low': 'high_to_low'}
# Dropped from names: camera angles, takes, defaults
NOISE = r'\((version \d+)\)|\bversion \d+\b|\b(side|front|back|behind) (pov|view)\b|\bpov\b|\b360 degrees?\b|\bwith rack\b|\bnormal (grip|stance)\b|\bboth arms\b|\bsimultaneous( arms)?\b|\bslow\b|\s\d+$'
# Which delt a plain "Shoulders (Deltoids)" means in a family
DELTS = defaultdict(lambda: 'front_delts', {'lateral_raise': 'side_delts', 'upright_row': 'side_delts', 'rear_delt_fly': 'rear_delts',
                                            'face_pull': 'rear_delts', 'row': 'rear_delts', 'pull_up': 'rear_delts',
                                            'lat_pulldown': 'rear_delts', 'shrug': 'side_delts'})
SMALL_WORDS = {'to', 'with', 'and', 'on', 'of', 'the', 'a', 'in'}


def find(phrases, text):
    """Phrases (longest first) found in `text`, and the text without them."""
    found = []
    for phrase in sorted(phrases, key=len, reverse=True):
        pattern = rf'(?<![\w-]){re.escape(phrase)}(?![\w-])'
        if re.search(pattern, text):
            found.append(phrase)
            text = re.sub(pattern, ' ', text)
    return found, ' '.join(text.split())


def singular(phrase):
    words = phrase.split()
    if words:
        w = words[-1]
        if w.endswith('ies'):
            w = w[:-3] + 'y'
        elif re.search(r'(ss|sh|ch|x)es$', w):
            w = w[:-2]
        elif w.endswith('s') and not re.search(r'(ss|ceps|abs|us)$', w):
            w = w[:-1]
        words[-1] = w
    return ' '.join(words)


def title(phrase):
    out = []
    for i, w in enumerate(phrase.split()):
        out.append('EZ' if w == 'ez' else w if (i and w in SMALL_WORDS) else '-'.join(p[:1].upper() + p[1:] for p in w.split('-')))
    return ' '.join(out)


def movement(text):
    """The movement in a name, without equipment, laterality, grip and set-up words."""
    text = re.sub(NOISE, ' ', normalise(text).replace('_', ' ').replace('-', ' '))
    for words in (EQUIPMENT, LATERALITY, GRIPS, POSITIONS):
        text = find(words, text)[1]
    return singular(text)


LATERALITY_NAMES = {'single': lambda text: 'Single-Leg' if 'leg' in text else 'One-Arm', 'alternating': lambda text: 'Alternating'}
GRIP_NAMES = {'close': 'Close-Grip', 'wide': 'Wide-Grip', 'neutral': 'Neutral-Grip', 'underhand': 'Underhand', 'overhand': 'Overhand',
              'rotating': 'Rotating', 'sumo': 'Sumo'}
NO_EQUIPMENT_IN_NAME = {'bodyweight', 'pull_up_bar'}
# Set-ups that read after the equipment ("Barbell Bent-Over Row", "Barbell Front Squat"); angles go before it
AFTER_EQUIPMENT = {'bent_over', 'front', 'overhead', 'goblet', 'preacher', 'reverse', 'forward', 'deficit'}
POSITION_NAMES = {'bent_over': 'Bent-Over', 'chest_supported': 'Chest-Supported', 'straight_leg': 'Straight-Leg',
                  'rear_foot_elevated': 'Rear-Foot-Elevated', 'all_fours': 'All-Fours', 'low_to_high': 'Low-to-High',
                  'high_to_low': 'High-to-Low'}


def compose_name(text, laterality, position, grip, equipment, family_name, registry):
    """[One-Arm] [set-up] [grip] [equipment] [movement], e.g. "Incline Dumbbell Bench Press", "Wide-Grip Pull-Up"."""
    def setup(p):
        return POSITION_NAMES.get(p) or title(p.replace('_', ' '))
    parts = []
    if laterality in LATERALITY_NAMES:
        parts.append(LATERALITY_NAMES[laterality](text))
    parts += [setup(p) for p in position if p not in AFTER_EQUIPMENT]
    parts += [GRIP_NAMES.get(g, title(g)) for g in grip]
    if equipment not in NO_EQUIPMENT_IN_NAME:
        label = registry['equipment'][equipment]['name'] if equipment in registry['equipment'] else title(equipment.replace('_', ' '))
        if label.lower() not in family_name.lower():
            parts.append(title(label.lower()) if label.lower() != 'ez bar' else 'EZ Bar')
    parts += [setup(p) for p in position if p in AFTER_EQUIPMENT]
    return ' '.join(parts + [family_name])


def family_index(registry, live):
    """(family names and aliases -> id, library exercises' movements -> id). The first may match the end of a
    movement ("incline bench press" -> Bench Press); the second only a whole one, since an exercise's movement can be
    a single word ("press" from "EZ Bar Underhand Press")."""
    families, exercises = {}, {}
    for family_id, family in registry['families'].items():
        for label in [family['name']] + family.get('aliases', []):
            families[singular(normalise(label).replace('-', ' '))] = family_id
    for e in live.values():
        if e.get('family') and e.get('status', 'active') == 'active':
            exercises.setdefault(movement(e['name']), e['family'])
    families.pop('', None)
    exercises.pop('', None)
    return families, exercises


def propose_one(row, registry, live, proposed):
    notes = []
    text = re.sub(NOISE, ' ', normalise(row['name']).replace('_', ' '))
    equipment_words, rest = find(EQUIPMENT, text)
    laterality_words, rest = find(LATERALITY, rest)
    grip_words, rest = find(GRIPS, rest)
    position_words, base = find(POSITIONS, rest)
    base = singular(base)

    # Family: the movement left once the modifiers are taken out; the set-up words count too when they're part of
    # the family's name (Reverse Fly, Preacher Curl, Front Raise)
    families, exercises = family_index(registry, live)
    family, matched = None, base
    for candidate in [singular(rest), base]:
        if candidate in exercises:
            family, matched = exercises[candidate], candidate
        else:
            for start in range(len(candidate.split())):
                phrase = ' '.join(candidate.split()[start:])
                if phrase in families:
                    family, matched = families[phrase], phrase
                    break
        if family:
            position_words = [p for p in position_words if p not in matched]
            break
    new_family = None
    if not family:
        family = re.sub(r'[^a-z0-9]+', '_', base).strip('_')
        new_family = dict(name=title(base), aliases=[], combination=' to ' in base)
        notes.append(f'new family {family} ("{title(base)}"): check it isn\'t an existing one under another name')

    equipment_ids = sorted({EQUIPMENT[w] for w in equipment_words})
    column = (row['equipment'] or '').lower()
    column_ids = sorted({v for k, v in EQUIPMENT.items() if re.search(rf'\b{re.escape(k)}\b', column)})
    if 'none' in column and not column_ids:
        column_ids = ['bodyweight']
    equipment = next((e for e in equipment_ids if e not in ('machine',)), equipment_ids[0] if equipment_ids else None) or (column_ids[0] if column_ids else 'bodyweight')
    if equipment_ids and column_ids and equipment not in column_ids and column_ids != ['bodyweight']:
        notes.append(f'equipment: the name says {equipment}, the pack\'s column says "{row["equipment"]}"')
    if not equipment_ids and not column_ids:
        notes.append('equipment: neither the name nor the pack says; assumed bodyweight')

    position = sorted({POSITIONS[w] for w in position_words})
    grip = sorted({GRIPS[w] for w in grip_words})
    laterality = LATERALITY[laterality_words[0]] if laterality_words else 'both'
    accessories = sorted({'bench'} if re.search(r'\bbench\b', text) or set(position) & {'incline', 'decline', 'flat', 'preacher', 'chest_supported'} else set())
    if re.search(r'\b(box|step)\b', text):
        accessories.append('box')

    primary = muscles(row['primary'], DELTS[family])
    secondary = [m for m in muscles(row['secondary'], DELTS[family]) if m not in primary]
    if not primary:
        notes.append('muscles: the pack\'s muscles didn\'t map; add them by hand')
    kind = ('mobility' if re.search(r'stretch|yoga|pose', text) else 'cardio' if re.search(r'jump rope|running|cardio|jog', text)
            else 'plyometric' if re.search(r'\bjump|plyo|burpee', text) else 'strength')
    tracking = ('duration' if kind in ('mobility', 'cardio') or re.search(r'\bhold\b|plank|carry|walk', text)
                else 'reps' if equipment in ('bodyweight', 'pull_up_bar', 'band') else 'weight_reps')
    if 'assisted' in text:
        notes.append('assisted: the weight makes it easier, so it needs its own tracking value (e.g. assisted) in the registry first')
    groups = registry['muscleGroups']
    group = registry['muscles'].get(primary[0], {}).get('group') if primary else None
    category = groups.get(group, {}).get('category', 'LEGS')

    # The movement as the pack says it ("Preacher Curl"), or the family's own spelling when it's the same ("Pull-Up")
    known = registry['families'].get(family, {}).get('name')
    label = known if known and singular(normalise(known).replace('-', ' ')) == matched else title(matched)
    name = compose_name(text, laterality, position, grip, equipment, label, registry)

    signature = (family, equipment, tuple(position), tuple(grip), laterality)
    for other in list(live.values()) + proposed:
        if other.get('status', 'active') == 'active' and other.get('family') == family and \
                (other.get('equipment'), tuple(sorted(other.get('position', []))), tuple(sorted(other.get('grip', []))), other.get('laterality')) == signature[1:]:
            notes.append(f'can\'t be told apart from "{other["name"]}" (same family and modifiers): change a modifier, or skip it')

    return dict(name=name, family=family, equipment=equipment, accessories=accessories, position=position, grip=grip,
                laterality=laterality, kind=kind, tracking=tracking, primaryMuscles=primary, secondaryMuscles=secondary,
                category=category), new_family, notes


def propose(args):
    pack = Pack(args.pack)
    db = push.firestore_client(args.repo, args.env)
    registry, live = push.export(db)
    if registry is None:
        sys.exit(f'{args.env} has no registry yet')
    used_sources = {normalise(e['source']['id']): e['name'] for e in live.values() if e.get('source')}
    used_images = {e.get('imageUrl', '').rsplit('/', 1)[-1] for e in live.values()}

    exercises, additions, notes, problems = {}, defaultdict(dict), {}, []
    for wanted in args.names:
        row = pack.row(wanted)
        if not row:
            close = difflib.get_close_matches(normalise(wanted), list(pack.rows), n=5, cutoff=0.5)
            problems.append(f'"{wanted}" isn\'t a pack row with data. Close: {", ".join(pack.rows[c]["name"] for c in close) or "none"}')
            continue
        uuid = exercise_uuid(row['name'])
        if uuid in live or normalise(row['name']) in used_sources:
            problems.append(f'"{row["name"]}" is already in the library as "{(live.get(uuid) or {}).get("name") or used_sources[normalise(row["name"])]}"')
            continue
        pose = '1' if pack.image(row['name'], '1') else ''
        source_image = pack.image(row['name'], pose)
        if not source_image:
            problems.append(f'"{row["name"]}" has no illustration in the pack')
            continue
        fields, new_family, row_notes = propose_one(row, registry, live, list(exercises.values()))
        if new_family:
            additions['families'][fields['family']] = dict(new_family, primaryMuscles=fields['primaryMuscles'])
        for key, field in [('equipment', 'equipment'), ('positions', 'position'), ('grips', 'grip')]:
            values = fields[field] if isinstance(fields[field], list) else [fields[field]]
            for value in values:
                if value and value not in registry[key]:
                    entry = dict(name=title(value.replace('_', ' ')))
                    if key == 'equipment':
                        entry['kind'] = 'main'
                    additions[key][value] = entry
                    row_notes.append(f'new {key} entry {value}')
        for value in fields['accessories']:
            if value not in registry['equipment']:
                additions['equipment'][value] = dict(name=title(value), kind='accessory')
        if fields['kind'] not in registry['kinds']:
            additions['kinds'][fields['kind']] = dict(name=title(fields['kind']))
        if fields['tracking'] not in registry['tracking']:
            additions['tracking'][fields['tracking']] = dict(name=title(fields['tracking'].replace('_', ' ')))
        family_members = [e for e in live.values() if e.get('family') == fields['family']] + \
                         [e for e in exercises.values() if e['family'] == fields['family']]

        file_name = ' '.join(os.path.splitext(os.path.basename(source_image))[0].split()) + '.webp'
        if quote(file_name) in used_images:
            row_notes.append(f'image {file_name} is already used by another exercise: pick another pose')
        exercises[uuid] = dict(
            uuid=uuid, name=fields['name'], category=fields['category'], splitSides=False, schemaVersion=2, status='active',
            family=fields['family'], familyDefault=not family_members, equipment=fields['equipment'],
            accessories=fields['accessories'], position=fields['position'], grip=fields['grip'],
            laterality=fields['laterality'], kind=fields['kind'], tracking=fields['tracking'],
            primaryMuscles=fields['primaryMuscles'], secondaryMuscles=fields['secondaryMuscles'], aliases=[],
            instructions=row['instructions'], tips=row['tips'], source=dict(pack=PACK_ID, id=row['name']),
            _image=dict(file=file_name, source=os.path.relpath(source_image, pack.root)),
        )
        notes[uuid] = row_notes

    stamp = datetime.now(timezone.utc).strftime('%Y%m%d-%H%M%S')
    proposal = dict(exercises=exercises, registryAdditions=additions, notes=notes)
    with open(os.path.join(args.out, f'import-{stamp}.json'), 'w') as f:
        json.dump(proposal, f, indent=2, ensure_ascii=False)
    with open(os.path.join(args.out, f'import-{stamp}.md'), 'w') as f:
        f.write(summary(proposal, problems, registry))
    print(summary(proposal, problems, registry))
    print(f'Proposal: import-{stamp}.json (review and edit it), summary: import-{stamp}.md')


def summary(proposal, problems, registry):
    lines = ['# Import proposal', '']
    if problems:
        lines += ['## Not proposed', ''] + [f'- {p}' for p in problems] + ['']
    lines += ['## Exercises', '', '| Name | Family | Equipment | Accessories | Position | Grip | Laterality | Kind | Tracking | Primary | Secondary | Image |',
              '|---|---|---|---|---|---|---|---|---|---|---|---|']
    for e in proposal['exercises'].values():
        family = e['family'] + (' (new)' if e['family'] not in registry['families'] else '') + (' ★' if e['familyDefault'] else '')
        lines.append('| {} | {} | {} | {} | {} | {} | {} | {} | {} | {} | {} | {} |'.format(
            e['name'], family, e['equipment'], ', '.join(e['accessories']) or '–', ', '.join(e['position']) or '–',
            ', '.join(e['grip']) or '–', e['laterality'], e['kind'], e['tracking'], ', '.join(e['primaryMuscles']) or '?',
            ', '.join(e['secondaryMuscles']) or '–', e['_image']['file']))
    lines.append('')
    flagged = {u: n for u, n in proposal['notes'].items() if n}
    if flagged:
        lines += ['## To check', ''] + [f'- **{proposal["exercises"][u]["name"]}**: ' + '; '.join(n) for u, n in flagged.items()] + ['']
    if proposal['registryAdditions']:
        lines += ['## Registry additions', ''] + [f'- {k}.{i}: {json.dumps(v, ensure_ascii=False)}'
                                                  for k, entries in proposal['registryAdditions'].items() for i, v in entries.items()] + ['']
    lines += ['Always check the muscles and the name. Source: each row of the pack spreadsheet.', '']
    return '\n'.join(lines)


def apply(args):
    with open(os.path.join(args.out, args.proposal)) as f:
        proposal = json.load(f)
    if args.write and args.env == 'prod' and not args.confirm_prod:
        sys.exit('writing to prod needs --confirm-prod')
    app = push.firebase_app(args.repo, args.env)
    db = push.firestore_client(args.repo, args.env, app)
    from firebase_admin import storage
    bucket = storage.bucket(app=app)
    live_registry, live = push.export(db)

    # Only the registry entries the exercises still in the file use (some may have been removed during review)
    used = defaultdict(set)
    for e in proposal['exercises'].values():
        used['families'].add(e['family'])
        used['equipment'].update([e['equipment']] + e['accessories'])
        used['positions'].update(e['position'])
        used['grips'].update(e['grip'])
        used['kinds'].add(e['kind'])
        used['tracking'].add(e['tracking'])
    registry = None
    additions = {k: {i: v for i, v in entries.items() if i in used[k] and i not in live_registry.get(k, {})}
                 for k, entries in proposal['registryAdditions'].items()}
    if any(additions.values()):
        registry = json.loads(json.dumps(live_registry))
        for key, entries in additions.items():
            order = max([e.get('order', 0) for e in registry[key].values()] or [0])
            for entry_id, entry in entries.items():
                order += 1
                registry[key][entry_id] = dict(entry, order=order, status='active')
        registry['registryVersion'] = live_registry.get('registryVersion', 0) + 1

    base = f'https://storage.googleapis.com/{push.IMAGE_BUCKETS[args.env]}/exercises/illustrations/'
    exercises, pending = {}, {}
    for uuid, e in proposal['exercises'].items():
        e = dict(e)
        image = e.pop('_image')
        url = base + quote(image['file'])
        if not bucket.blob(f'exercises/illustrations/{image["file"]}').exists():
            pending[url] = image
        # Applying the same file again changes nothing: an exercise that's already there keeps its date
        created = (live.get(uuid) or {}).get('createdDate') or int(time.time() * 1000)
        exercises[uuid] = dict(e, createdDate=created, imageUrl=url, media=dict(image=url, imageEnd=None, video=None))

    print(f'{len(exercises)} exercises, {len(pending)} images to upload to {push.IMAGE_BUCKETS[args.env]}' +
          (f', registry version {registry["registryVersion"]}' if registry else ''))
    # Check everything before uploading anything
    push.run(db, args.env, registry, exercises, False, args.repo, args.out, pending_images=set(pending))
    if not args.write:
        return
    for url, image in pending.items():
        upload(bucket, os.path.join(args.pack, image['source']), image['file'], args.repo)
    push.run(db, args.env, registry, exercises, True, args.repo, args.out)


def upload(bucket, source, file_name, repo):
    """Makes the image the way tools/image_resize does (square, 1024 px, WebP) and uploads it, never overwriting."""
    sys.path.insert(0, os.path.join(repo, 'tools', 'image_resize'))
    from resize_image import resize_and_optimize
    blob = bucket.blob(f'exercises/illustrations/{file_name}')
    if blob.exists():
        print(f'{file_name} is already in the bucket; left as it is')
        return
    with tempfile.TemporaryDirectory() as work:
        os.makedirs(os.path.join(work, 'in'))
        shutil.copyfile(source, os.path.join(work, 'in', os.path.splitext(file_name)[0] + os.path.splitext(source)[1]))
        resize_and_optimize(os.path.join(work, 'in'), os.path.join(work, 'out'))
        blob.upload_from_filename(os.path.join(work, 'out', file_name))
        blob.make_public()
    print(f'Uploaded {file_name}')


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument('step', choices=['propose', 'apply'])
    parser.add_argument('items', nargs='+', help='propose: pack exercise names; apply: the proposal file in out/')
    parser.add_argument('--env', required=True, choices=['dev', 'prod'])
    parser.add_argument('--write', action='store_true')
    parser.add_argument('--confirm-prod', action='store_true')
    parser.add_argument('--pack', default='/pack')
    parser.add_argument('--repo', default='/repo')
    parser.add_argument('--out', default='/out')
    args = parser.parse_args()
    if args.step == 'propose':
        args.names = args.items
        propose(args)
    else:
        args.proposal = args.items[0]
        apply(args)


if __name__ == '__main__':
    main()
