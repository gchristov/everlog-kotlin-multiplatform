"""Turns the query results in out/*.csv and config.json into out/data.json, and fills it into
dashboard.html as out/dashboard.html. Standard library only. Run by refresh.sh.

Every number here follows the definitions in README.md. Rates only count users who have had
long enough to reach them (a week-one rate needs 7 days since install).
"""
import csv
import json
import os
import subprocess
from collections import OrderedDict
from datetime import date, datetime, timedelta, timezone

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, 'out')

ONBOARDING_STEPS = ['welcome', 'units', 'days', 'where', 'experience', 'goal', 'building', 'reveal', 'saving']

# 2021-2023 organic baseline and Roadmap targets (Funnel baseline and Roadmap pages in Notion)
HEADLINES = [
    {'id': 'finished_w1', 'label': 'Finished a workout in week one', 'baseline': 0.21, 'target': 0.30,
     'note': 'The verdict for onboarding v1', 'read_at': '7 days after install'},
    {'id': 'setup_d0', 'label': 'Did setup on day one', 'baseline': 0.42, 'target': 0.60,
     'note': 'Kept the starter templates, or created a routine, exercise or plan', 'read_at': '1 day after install'},
    {'id': 'tourists', 'label': 'Tourists', 'baseline': 0.34, 'target': 0.20, 'lower_is_better': True,
     'note': 'No setup and no workout on day one', 'read_at': '1 day after install'},
    {'id': 'second_3d', 'label': 'Second workout within 3 days', 'baseline': 0.41, 'target': 0.50,
     'note': 'Of users who finished a workout', 'read_at': '3 days after the first workout'},
    {'id': 'finished_d8_30', 'label': 'Finished a workout in days 8–30', 'baseline': 0.076, 'target': 0.10,
     'note': 'Still lifting after the first week', 'read_at': '30 days after install'},
    {'id': 'activated_w1', 'label': '3+ workouts in week one', 'baseline': 0.062, 'target': None,
     'note': 'Direction only', 'read_at': '7 days after install'},
]


def read_csv(name):
    with open(os.path.join(OUT, name + '.csv'), newline='') as f:
        return list(csv.DictReader(f))


def flag(v):
    return v == 'True'


def num(v):
    return float(v) if v not in ('', None) else None


def load_users():
    users = []
    for r in read_csv('new_users'):
        users.append({
            'install_date': r['install_date'],
            'days': float(r['days_observed']),
            'channel': r['channel'],
            'reached_home': flag(r['reached_home']),
            'ob_viewed': set(filter(None, (r['ob_viewed'] or '').split(','))),
            'ob_completed': set(filter(None, (r['ob_completed'] or '').split(','))),
            'ob_outcome': r['ob_outcome'] or None,
            'setup_d0': flag(r['setup_d0']),
            'started_d0': flag(r['started_d0']),
            'started_w1': flag(r['started_w1']),
            'completed_w1': int(r['completed_w1']),
            'completed_d7_30': int(r['completed_d7_30']),
            'first_c': num(r['first_completion_d']),
            'second_c': num(r['second_completion_d']),
        })
    return users


def rate(hit, n):
    return {'hit': hit, 'n': n, 'rate': (hit / n) if n else None}


def headline_values(users):
    home = [u for u in users if u['reached_home']]
    w1 = [u for u in home if u['days'] >= 7]
    d1 = [u for u in home if u['days'] >= 1]
    d30 = [u for u in home if u['days'] >= 30]
    first = [u for u in home if u['first_c'] is not None and u['days'] >= u['first_c'] + 3]
    return {
        'finished_w1': rate(sum(u['completed_w1'] >= 1 for u in w1), len(w1)),
        'setup_d0': rate(sum(u['setup_d0'] for u in d1), len(d1)),
        'tourists': rate(sum(not u['setup_d0'] and not u['started_d0'] for u in d1), len(d1)),
        'second_3d': rate(sum(u['second_c'] is not None and u['second_c'] - u['first_c'] <= 3 for u in first), len(first)),
        'finished_d8_30': rate(sum(u['completed_d7_30'] >= 1 for u in d30), len(d30)),
        'activated_w1': rate(sum(u['completed_w1'] >= 3 for u in w1), len(w1)),
    }


def funnel(users):
    w1 = [u for u in users if u['days'] >= 7]
    steps = []
    for s in ONBOARDING_STEPS:
        viewed = sum(s in u['ob_viewed'] for u in users)
        completed = sum(s in u['ob_completed'] for u in users)
        steps.append({'step': s, 'viewed': viewed, 'completed': completed})
    outcomes = OrderedDict((k, 0) for k in ['starter_templates', 'own_template', 'skipped'])
    for u in users:
        if u['ob_outcome']:
            outcomes[u['ob_outcome']] = outcomes.get(u['ob_outcome'], 0) + 1
    reveal = next(s for s in steps if s['step'] == 'reveal')['viewed']
    return {
        'new_users': len(users),
        'reached_home': sum(u['reached_home'] for u in users),
        'onboarding_started': sum(bool(u['ob_viewed']) for u in users),
        'steps': steps,
        'outcomes': outcomes,
        'kept_or_built': rate(outcomes['starter_templates'] + outcomes['own_template'], reveal),
        'setup_d0': sum(u['setup_d0'] for u in users if u['days'] >= 1),
        'mature_d1': sum(u['days'] >= 1 for u in users),
        'mature_w1': len(w1),
        'started_w1': sum(u['started_w1'] for u in w1),
        'finished_w1': sum(u['completed_w1'] >= 1 for u in w1),
        'two_plus_w1': sum(u['completed_w1'] >= 2 for u in w1),
        'three_plus_w1': sum(u['completed_w1'] >= 3 for u in w1),
    }


def paid(users, config):
    test = config['paid_test']
    start = test['start_date'] or config['onboarding_v1_date']
    cohort = [u for u in users if u['install_date'] >= start]
    rows = []
    channels = [(c['name'], c['label'], c['spend_eur']) for c in test['campaigns']]
    channels += [('Other paid', 'Other paid', None), ('Organic', 'Organic', None)]
    for name, label, spend in channels:
        us = [u for u in cohort if u['channel'] == name]
        if name == 'Other paid' and not us:
            continue
        w1 = [u for u in us if u['days'] >= 7]
        lifters = sum(u['completed_w1'] >= 1 for u in w1)
        rows.append({
            'label': label,
            'paid': name != 'Organic',
            'new_users': len(us),
            'finished_onboarding': sum(u['ob_outcome'] is not None for u in us),
            'kept_or_built': sum(u['ob_outcome'] in ('starter_templates', 'own_template') for u in us),
            'mature_w1': len(w1),
            'lifters': lifters,
            'spend_eur': spend,
            'cost_per_lifter': (spend / lifters) if spend and lifters else None,
        })
    return {'start_date': test['start_date'], 'cohort_from': start, 'bar_eur_per_lifter': test['bar_eur_per_lifter'], 'rows': rows}


def monday(d):
    d = date.fromisoformat(d)
    return (d - timedelta(days=d.weekday())).isoformat()


def weekly_cohorts(users):
    weeks = OrderedDict()
    if not users:
        return []
    d = date.fromisoformat(monday(min(u['install_date'] for u in users)))
    end = date.fromisoformat(monday(date.today().isoformat()))
    while d <= end:
        weeks[d.isoformat()] = {'week': d.isoformat(), 'organic': 0, 'paid': 0, 'mature_w1': 0, 'lifters': 0}
        d += timedelta(days=7)
    for u in users:
        w = weeks[monday(u['install_date'])]
        w['organic' if u['channel'] == 'Organic' else 'paid'] += 1
        if u['days'] >= 7:
            w['mature_w1'] += 1
            w['lifters'] += u['completed_w1'] >= 1
    return list(weeks.values())


def releases():
    try:
        out = subprocess.run(['git', 'for-each-ref', '--sort=creatordate', '--format=%(refname:short) %(creatordate:short)', 'refs/tags'],
                             cwd=HERE, capture_output=True, text=True, check=True).stdout
    except (OSError, subprocess.CalledProcessError):
        return []
    tags = [line.split(' ') for line in out.strip().splitlines() if line]
    return [{'version': t, 'date': d} for t, d in tags if d >= '2025-01-01']


def main():
    with open(os.path.join(HERE, 'config.json')) as f:
        config = json.load(f)
    users = load_users()
    v1 = config['onboarding_v1_date']
    before = [u for u in users if u['install_date'] < v1]
    since = [u for u in users if u['install_date'] >= v1]
    hb, hs = headline_values(before), headline_values(since)
    headlines = [dict(h, before=hb[h['id']], since=hs[h['id']]) for h in HEADLINES]

    data = {
        'generated_at': datetime.now(timezone.utc).strftime('%Y-%m-%dT%H:%MZ'),
        'onboarding_v1_date': v1,
        'counts': {'new_users_2026': len(users), 'before_v1': len(before), 'since_v1': len(since),
                   'reached_home_before': sum(u['reached_home'] for u in before),
                   'reached_home_since': sum(u['reached_home'] for u in since)},
        'headlines': headlines,
        'funnel': funnel(since),
        'paid': paid(users, config),
        'cohorts': weekly_cohorts(users),
        'core_weekly': [{'week': r['week'], 'workouts': int(r['workouts_completed']), 'lifters': int(r['lifters'])} for r in read_csv('core')],
        'core_monthly': [{'month': r['month'], 'lifters': int(r['lifters']), 'workouts': int(r['workouts_completed'])} for r in read_csv('core_monthly')],
        'robots': [{k: (v if k == 'month' else int(v)) for k, v in r.items()} for r in read_csv('robots')],
        'releases': releases(),
        'events': config['events'],
    }
    with open(os.path.join(OUT, 'data.json'), 'w') as f:
        json.dump(data, f, indent=1)
    with open(os.path.join(HERE, 'dashboard.html')) as f:
        page = f.read()
    marker = '/*__DATA__*/null'
    if marker not in page:
        raise SystemExit('dashboard.html has no /*__DATA__*/null placeholder')
    with open(os.path.join(OUT, 'dashboard.html'), 'w') as f:
        f.write(page.replace(marker, json.dumps(data, separators=(',', ':'))))
    print(f"Wrote out/data.json and out/dashboard.html ({len(users)} new users, {len(since)} since onboarding v1)")


if __name__ == '__main__':
    main()
