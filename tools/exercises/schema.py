"""Checks the exercise registry and library exercises against the data model.

Every write (migration, import, registry change) runs it first. Errors stop the write; warnings are for the owner.
"""
import os
import re
from collections import defaultdict

STARTER_EXERCISES = 'mobile/src/main/java/com/everlog/data/controllers/starterroutines/StarterExercises.kt'
LEGACY_CATEGORIES = {'CHEST', 'BACK', 'LEGS', 'SHOULDERS', 'ARMS', 'ABS'}
STATUSES = {'active', 'hidden'}
ID = re.compile(r'^[a-z][a-z0-9_]*$')
REGISTRY_LISTS = ['muscles', 'muscleGroups', 'regions', 'equipment', 'positions', 'grips', 'laterality', 'tracking', 'kinds', 'families']


def starter_uuids(repo):
    """The library exercises the onboarding's starter routines use; they must stay in the library and active."""
    with open(os.path.join(repo, STARTER_EXERCISES)) as f:
        return re.findall(r'Exercise\("([0-9a-f-]{36})"', f.read())


def validate(registry, exercises, starter_uuids=(), previous_registry=None, previous_exercises=None):
    """`exercises` maps uuid -> document. Returns (errors, warnings), each a list of strings."""
    errors, warnings = [], []
    _registry(registry, previous_registry, errors)
    _exercises(registry, exercises, previous_exercises or {}, errors)
    _families(exercises, errors)
    _images(exercises, errors)
    for uuid in starter_uuids:
        exercise = exercises.get(uuid)
        if not exercise:
            errors.append(f'starter routines: {uuid} is not in the library')
        elif exercise.get('status') != 'active':
            errors.append(f'starter routines: {exercise["name"]} is hidden')
    return errors, warnings


def _registry(registry, previous, errors):
    for key in REGISTRY_LISTS:
        entries = registry.get(key)
        if not isinstance(entries, dict) or not entries:
            errors.append(f'registry: {key} is missing or empty')
            continue
        for entry_id, entry in entries.items():
            if not ID.match(entry_id):
                errors.append(f'registry: {key}.{entry_id} is not a snake_case id')
            if not entry.get('name') or not isinstance(entry.get('order'), int) or entry.get('status') not in STATUSES:
                errors.append(f'registry: {key}.{entry_id} needs a name, an order and a status')
    for muscle_id, muscle in registry.get('muscles', {}).items():
        if muscle.get('group') not in registry.get('muscleGroups', {}):
            errors.append(f'registry: muscle {muscle_id} has an unknown group')
        if muscle.get('side') not in ('front', 'back'):
            errors.append(f'registry: muscle {muscle_id} needs a side (front or back)')
    for group_id, group in registry.get('muscleGroups', {}).items():
        if group.get('region') not in registry.get('regions', {}):
            errors.append(f'registry: muscle group {group_id} has an unknown region')
        if group.get('category') not in LEGACY_CATEGORIES:
            errors.append(f'registry: muscle group {group_id} has an unknown legacy category')
    for family_id, family in registry.get('families', {}).items():
        for muscle in family.get('primaryMuscles', []):
            if muscle not in registry.get('muscles', {}):
                errors.append(f'registry: family {family_id} uses unknown muscle {muscle}')
    if previous:
        # Ids are never removed or reused; retire them with status hidden
        for key in REGISTRY_LISTS:
            for entry_id in previous.get(key, {}):
                if entry_id not in registry.get(key, {}):
                    errors.append(f'registry: {key}.{entry_id} was removed (hide it instead)')
        if registry.get('registryVersion', 0) <= previous.get('registryVersion', 0):
            errors.append('registry: registryVersion must go up on every push')


def _exercises(registry, exercises, previous, errors):
    def known(key, value):
        return value in registry.get(key, {})

    for uuid, e in exercises.items():
        label = f'{e.get("name")} ({uuid})'
        if e.get('uuid') != uuid:
            errors.append(f'{label}: uuid field differs from the document id')
        if not e.get('name'):
            errors.append(f'{label}: no name')
        if e.get('status') not in STATUSES:
            errors.append(f'{label}: status must be active or hidden')
        if e.get('category') not in LEGACY_CATEGORIES:
            errors.append(f'{label}: legacy category {e.get("category")} is not one of the 6')
        if e.get('splitSides'):
            errors.append(f'{label}: splitSides is legacy and must stay false')
        media = e.get('media') or {}
        if not media.get('image') or e.get('imageUrl') != media.get('image'):
            errors.append(f'{label}: imageUrl must equal media.image')
        old = previous.get(uuid)
        if old and old.get('imageUrl') != e.get('imageUrl') and old.get('imageUrl', '').rsplit('/', 1)[-1] == e.get('imageUrl', '').rsplit('/', 1)[-1]:
            errors.append(f'{label}: a changed image needs a new file name (apps cache by URL)')
        if e.get('family') is not None and not known('families', e['family']):
            errors.append(f'{label}: unknown family {e["family"]}')
        equipment = e.get('equipment')
        if equipment is not None and registry.get('equipment', {}).get(equipment, {}).get('kind') != 'main':
            errors.append(f'{label}: equipment {equipment} is not main equipment in the registry')
        for accessory in e.get('accessories', []):
            if registry.get('equipment', {}).get(accessory, {}).get('kind') != 'accessory':
                errors.append(f'{label}: accessory {accessory} is not an accessory in the registry')
        for key, field in [('positions', 'position'), ('grips', 'grip')]:
            if not isinstance(e.get(field), list):
                errors.append(f'{label}: {field} must be a list')
                continue
            for value in e[field]:
                if not known(key, value):
                    errors.append(f'{label}: unknown {field} {value}')
        for key, field in [('laterality', 'laterality'), ('tracking', 'tracking'), ('kinds', 'kind')]:
            if not known(key, e.get(field)):
                errors.append(f'{label}: unknown {field} {e.get(field)}')
        if not e.get('primaryMuscles'):
            errors.append(f'{label}: a library exercise needs at least one primary muscle')
        for muscle in e.get('primaryMuscles', []) + e.get('secondaryMuscles', []):
            if not known('muscles', muscle):
                errors.append(f'{label}: unknown muscle {muscle}')
        if set(e.get('primaryMuscles', [])) & set(e.get('secondaryMuscles', [])):
            errors.append(f'{label}: a muscle is both primary and secondary')


def _families(exercises, errors):
    members = defaultdict(list)
    for e in exercises.values():
        if e.get('family') and e.get('status') == 'active':
            members[e['family']].append(e)
    for family, active in members.items():
        defaults = [e['name'] for e in active if e.get('familyDefault')]
        if len(defaults) != 1:
            errors.append(f'family {family}: needs exactly one active familyDefault, has {defaults or "none"}')
        seen = {}
        for e in active:
            signature = (e.get('equipment'), tuple(sorted(e.get('position', []))), tuple(sorted(e.get('grip', []))), e.get('laterality'))
            if signature in seen:
                errors.append(f'family {family}: {seen[signature]} and {e["name"]} have the same modifiers')
            seen[signature] = e['name']


def _images(exercises, errors):
    owners = defaultdict(list)
    for e in exercises.values():
        if e.get('status') != 'active':
            continue
        media = e.get('media') or {}
        for url in (media.get('image'), media.get('imageEnd')):
            if url:
                owners[url].append(e['name'])
    for url, names in owners.items():
        if len(set(names)) > 1:
            errors.append(f'shared image {url.rsplit("/", 1)[-1]}: {", ".join(names)}')
