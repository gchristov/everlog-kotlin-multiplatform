"""Reads the licensed Exercise Animatic pack (spreadsheet and illustrations) that the library is built from.

The pack lives outside the repo (EXERCISE_PACK_DIR, mounted at /pack by run.sh). Its names are inconsistent
(trailing spaces, "_female", numbered poses), so rows and images are looked up by a normalised name.
"""
import os
import re
from collections import defaultdict

SPREADSHEET = '1500+ exercise data.xlsx'
ILLUSTRATIONS = 'ILLUSTRATIONS'


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

    def _load_rows(self):
        from openpyxl import load_workbook
        sheet = load_workbook(os.path.join(self.root, SPREADSHEET), read_only=True).worksheets[0]
        rows = {}
        for category, name, instructions, tips, primary, secondary, equipment in sheet.iter_rows(min_row=2, values_only=True):
            # Only rows with muscle data; the male version wins over the female one
            if name and primary and normalise(name) not in rows:
                rows[normalise(name)] = dict(name=name.strip(), instructions=steps(instructions), tips=steps(tips),
                                             primary=primary, secondary=secondary or '', equipment=equipment or '')
        return rows

    def _index_images(self):
        """Normalised name -> {pose: path}. Pose '' is the start (X.jpg), '1' the other (X1.jpg); some have 2 and 3."""
        index = defaultdict(dict)
        for folder, _, files in os.walk(os.path.join(self.root, ILLUSTRATIONS)):
            for file in files:
                if not re.search(r'\.(jpe?g|png)$', file, re.I) or re.search(r'female', file, re.I):
                    continue
                key = normalise(file)
                match = re.match(r'(.*?)\s*(\d)?$', key)
                index[match.group(1)][match.group(2) or ''] = os.path.join(folder, file)
        return index

    def row(self, name):
        return self.rows.get(normalise(name))

    def image(self, name, pose):
        return self.images.get(normalise(name), {}).get(pose)


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
