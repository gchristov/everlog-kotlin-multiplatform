"""Reads the licensed Exercise Animatic pack (spreadsheet and illustrations) that the library is built from.

The pack lives outside the repo (EXERCISE_PACK_DIR, mounted at /pack by run.sh). Its names are inconsistent
(trailing spaces, "_female", numbered poses), so rows and images are looked up by a normalised name. Some rows have only
a name, with an illustration and videos but no instructions, tips or muscles (e.g. "Glute Bridge Bodyweight"); they're
kept, marked `data: False`, so an import can still use them with text written by hand.
"""
import os
import re
import uuid
from collections import defaultdict
from urllib.parse import unquote

SPREADSHEET = '1500+ exercise data.xlsx'
ILLUSTRATIONS = 'ILLUSTRATIONS'
PACK_ID = 'exercise_animatic'
# Fixed forever: an exercise imported from the pack gets the same uuid every time, so an import can't duplicate it
UUID_NAMESPACE = uuid.UUID('6f2b1c1e-6d1b-4c55-9a52-3e2f0e8d7a10')


def exercise_uuid(row_name):
    """The library uuid for a pack row, e.g. exercise_uuid('Bodyweight squat')."""
    return str(uuid.uuid5(UUID_NAMESPACE, f'{PACK_ID}:{normalise(row_name)}'))


def normalise(name):
    """'Barbell Hip Thrust  1.jpg' -> 'barbell hip thrust 1'; '_female' / 'Female' suffixes dropped."""
    name = re.sub(r'\.(jpe?g|png|webp)$', '', name.strip(), flags=re.I)
    name = re.sub(r'[\s_]*\(?(fe)?male\)?\s*(?=\d?$)', ' ', name, flags=re.I)
    return ' '.join(name.lower().split())


class Pack:
    def __init__(self, root):
        self.root = root
        self.rows = self._load_rows()
        self.images = self._index_images()
        self._videos = None

    def _load_rows(self):
        from openpyxl import load_workbook
        sheet = load_workbook(os.path.join(self.root, SPREADSHEET), read_only=True).worksheets[0]
        rows = {}
        for category, name, instructions, tips, primary, secondary, equipment in sheet.iter_rows(min_row=2, values_only=True):
            if not name:
                continue
            key = normalise(str(name))
            # A row with muscle data wins over one with only a name, then the male version over the female one
            if key in rows and (rows[key]['data'] or not primary):
                continue
            rows[key] = dict(name=str(name).strip(), instructions=steps(instructions), tips=steps(tips),
                             primary=primary or '', secondary=secondary or '', equipment=equipment or '', data=bool(primary))
        return rows

    def _index_images(self):
        """Normalised name -> {pose: path}. Pose '' is the start (X.jpg), '1' the other (X1.jpg); some have 2 and 3."""
        index, female = defaultdict(dict), defaultdict(dict)
        for folder, _, files in os.walk(os.path.join(self.root, ILLUSTRATIONS)):
            for file in files:
                if not re.search(r'\.(jpe?g|png)$', file, re.I):
                    continue
                match = re.match(r'(.*?)\s*(\d)?$', normalise(file))
                poses = female if re.search(r'female', file, re.I) else index
                poses[match.group(1)][match.group(2) or ''] = os.path.join(folder, file)
        # About 150 exercises are only drawn with a female model
        for key, poses in female.items():
            index.setdefault(key, poses)
        return index

    def row(self, name):
        return self.rows.get(normalise(name))

    def image(self, name, pose):
        return self.images.get(normalise(name), {}).get(pose)

    def videos(self):
        """Normalised names of the pack's videos, in any of its video folders."""
        if self._videos is None:
            self._videos = set()
            for top in os.listdir(self.root):
                if top == ILLUSTRATIONS or not os.path.isdir(os.path.join(self.root, top)):
                    continue
                for _, _, files in os.walk(os.path.join(self.root, top)):
                    self._videos.update(normalise(re.sub(r'\.(mp4|mov)$', '', f, flags=re.I)) for f in files
                                        if re.search(r'\.(mp4|mov)$', f, re.I))
        return self._videos

    def search(self, query):
        """Every spreadsheet row and illustration whose name has all the words of `query`, or of a common other way
        of saying it ("knee push-up" finds "Kneeling Push-up", "hip raise" finds glute bridges), closest first. A query
        of three words or more also finds names missing one of them, as close hits ("smith machine bench press" finds
        "Smith machine press flat"), since the pack often leaves out a word lifters use. Each hit says what the pack
        has for it: muscle data, an illustration, a video, and the word a close hit is missing."""
        queries = [query.lower()]
        for one, other in SYNONYMS:
            for q in list(queries):
                for a, b in [(one, other), (other, one)]:
                    variant = re.sub(rf'\b{re.escape(a)}\b', b, q)
                    if variant not in queries:
                        queries.append(variant)
        names = {key: row['name'] for key, row in self.rows.items()}
        names.update({key: key for key in self.images if key not in names})
        hits = []
        for key, name in names.items():
            have = words(key)
            # "pulldown" finds "pull down"
            have += [a + b for a, b in zip(have, have[1:])]
            best = None
            for q in queries:
                wanted = words(q)
                missing = [w for w in wanted if not any(h.startswith(w) for h in have)]
                if wanted and (not missing or (len(missing) == 1 and len(wanted) >= 3)) and \
                        (best is None or len(missing) < len(best[1])):
                    best = (wanted, missing)
            if best:
                row = self.rows.get(key)
                hits.append(dict(name=name, data=bool(row and row['data']), image=bool(self.images.get(key)),
                                 video=key in self.videos(), missing=best[1][0] if best[1] else None,
                                 extra=len(words(key)) - len(best[0])))
        return sorted(hits, key=lambda h: (h['missing'] is not None, not h['image'], h['extra'], not h['data'], h['name'].lower()))


# Other ways people write the same thing, for search. Each side finds the other.
SYNONYMS = [('knee', 'kneeling'), ('pushup', 'push up'), ('press up', 'push up'), ('situp', 'sit up'),
            ('pullup', 'pull up'), ('chinup', 'chin up'), ('hip raise', 'glute bridge'), ('hip lift', 'glute bridge'),
            ('one arm', 'single arm'), ('one leg', 'single leg'), ('stiff leg', 'straight leg'), ('rdl', 'romanian deadlift'),
            ('db', 'dumbbell'), ('bb', 'barbell'), ('kb', 'kettlebell'), ('ohp', 'overhead press'), ('skull crusher', 'lying triceps extension'),
            ('pulldown', 'pull down'), ('pushdown', 'push down'), ('pushdown', 'press down'), ('pushdown', 'extension'),
            ('stepup', 'step up'), ('hex bar', 'trap bar'), ('rear delt', 'reverse'), ('pec deck', 'machine fly'),
            ('hyperextension', 'back extension'), ('standing leg curl', 'kneeling leg curl'), ('hamstring curl', 'leg curl'),
            ('woodchop', 'chop'), ('wood chop', 'chop'), ('seal row', 'prone bench row'), ('assault bike', 'airbike'),
            ('ab machine', 'abdominal crunches machine'), ('hanging leg raise', 'captains chair leg raise'),
            ('bench press', 'chest press'), ('overhead press', 'shoulder press'), ('military press', 'shoulder press')]


def words(text):
    """'Push-Ups (on knees)' -> ['push', 'up', 'on', 'knee']: lower case, no punctuation, no plural s."""
    return [re.sub(r'(?<=[a-z]{3})s$', '', w) for w in re.sub(r'[^a-z0-9]+', ' ', text.lower()).split()]


def steps(text):
    """'1. Do this.\\n2. Then that.' -> ['Do this.', 'Then that.']"""
    if not text:
        return []
    return [re.sub(r'^\s*\d+[.)]\s*', '', line).strip() for line in str(text).splitlines() if line.strip()]


# Pack muscle text is "Group (Latin, Latin), Group (Latin)"; the Latin names map reliably onto the registry's muscles
LATIN = [
    (r'clavicular|upper chest|upper pec', 'upper_chest'),
    (r'pector|chest', 'chest'),
    (r'latissimus|\blats?\b', 'lats'),
    (r'trapez|\btraps\b', 'traps'),
    (r'rhomboid|teres|middle back|mid back', 'mid_back'),
    (r'erector|lower back', 'lower_back'),
    (r'anterior delt|front delt|front shoulder', 'front_delts'),
    (r'lateral delt|medial delt|side delt', 'side_delts'),
    (r'posterior delt|rear delt|rear shoulder', 'rear_delts'),
    (r'triceps', 'triceps'),
    (r'biceps brachii|\bbiceps\b(?! femoris)|brachialis', 'biceps'),
    (r'brachioradialis|forearm|wrist', 'forearms'),
    (r'oblique', 'obliques'),
    (r'rectus abdominis|abdominal|\babs\b|core', 'abs'),
    (r'iliopsoas|hip flexor', 'hip_flexors'),
    (r'gluteus medius|tensor|abductor|outer thigh', 'abductors'),
    (r'glute', 'glutes'),
    (r'adductor|inner thigh', 'adductors'),
    (r'quadricep|rectus femoris|vastus', 'quads'),
    (r'hamstring|biceps femoris|semitendinosus|semimembranosus', 'hamstrings'),
    (r'gastrocnemius|soleus|calf|calves', 'calves'),
]
# Which delt a plain "Shoulders (Deltoids)" means in a family
DELTS = defaultdict(lambda: 'front_delts', {'lateral_raise': 'side_delts', 'upright_row': 'side_delts', 'rear_delt_fly': 'rear_delts',
                                            'face_pull': 'rear_delts', 'row': 'rear_delts', 'pull_up': 'rear_delts',
                                            'lat_pulldown': 'rear_delts', 'shrug': 'side_delts'})
# Group words without Latin names
GROUP_WORDS = {'back': 'lats', 'upper back': 'traps', 'back muscles': 'lats', 'arms': 'biceps', 'legs': 'quads', 'thighs': 'quads'}


def muscles(text, delts='front_delts'):
    """Registry muscle ids for a pack muscle text. A plain 'Shoulders (Deltoids)' becomes `delts`, which depends
    on the movement (side delts for a lateral raise, rear delts for a row)."""
    found = []
    for part in re.split(r',\s*(?![^()]*\))', text or ''):
        part = part.strip().lower()
        if not part or part == 'none':
            continue
        ids = [muscle for pattern, muscle in LATIN if re.search(pattern, part)]
        if 'glutes' in ids and part.startswith('glute'):
            ids = ['glutes']  # "Glutes (gluteus maximus, gluteus medius)" is glutes, not abductors
        if not ids:
            if re.search(r'deltoid|shoulder', part):
                ids = [delts]
            elif re.sub(r'\s*\(.*', '', part).strip() in GROUP_WORDS:
                ids = [GROUP_WORDS[re.sub(r'\s*\(.*', '', part).strip()]]
        for muscle in ids:
            if muscle not in found:
                found.append(muscle)
    if 'upper_chest' in found and 'chest' in found:
        found.remove('chest')  # incline work: upper chest says it all
    return found


def differences(exercise, pack):
    """How a library exercise differs from its pack row: muscles, steps, tips, and an image from another row.
    Not errors (a review often corrects the pack), so pushes list them for a look rather than refusing them. The
    pack's equipment column is left out: it's too unreliable to compare."""
    source = (exercise.get('source') or {}).get('id')
    if not source:
        return []
    row = pack.row(source)
    if not row:
        return [f'its source "{source}" isn\'t a pack row']
    if not row['data']:
        return ['the pack has only a name for this row, so its text and muscles were written by hand: check them']
    delts = DELTS[exercise.get('family')]
    primary = muscles(row['primary'], delts)
    secondary = [m for m in muscles(row['secondary'], delts) if m not in primary]
    found = []
    for field, expected in [('primaryMuscles', primary), ('secondaryMuscles', secondary)]:
        if set(exercise.get(field) or []) != set(expected):
            found.append(f'{field} {exercise.get(field) or []}, pack {expected}')
    for field, expected in [('instructions', row['instructions']), ('tips', row['tips'])]:
        if (exercise.get(field) or []) != expected:
            found.append(f'{field} differ from the pack')
    image = ((exercise.get('media') or {}).get('image') or exercise.get('imageUrl') or '').rsplit('/', 1)[-1]
    image_row = re.sub(r'\s*\d$', '', normalise(unquote(image)))
    if image and image_row != normalise(source):
        found.append(f'image from the pack\'s "{image_row}", not its source row')
    return found
