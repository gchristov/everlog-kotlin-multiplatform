import argparse
import csv
import os
import time
from collections import defaultdict
from datetime import datetime, timezone

import firebase_admin
from firebase_admin import auth, credentials, firestore

DAY_MILLIS = 24 * 60 * 60 * 1000


def format_date(millis):
    return datetime.fromtimestamp(millis / 1000, tz=timezone.utc).strftime('%Y-%m-%d')


def collect_workout_stats(db):
    """Aggregates completed workouts per user from the users/{uid}/history subcollections."""
    stats = defaultdict(lambda: {'total': 0, 'first': None, 'last': None})
    # Only fetch the completion date to keep the payload small
    query = db.collection_group('history').select(['completedDate'])
    scanned = 0
    for doc in query.stream():
        scanned += 1
        parent = doc.reference.parent.parent
        # Ignore any 'history' collections that don't live under users/{uid}
        if parent is None or parent.parent.id != 'users':
            continue
        completed = (doc.to_dict() or {}).get('completedDate') or 0
        if completed <= 0:
            continue
        entry = stats[parent.id]
        entry['total'] += 1
        entry['first'] = completed if entry['first'] is None else min(entry['first'], completed)
        entry['last'] = completed if entry['last'] is None else max(entry['last'], completed)
    print(f"Scanned {scanned} history documents across {len(stats)} users.", flush=True)
    return stats


def resolve_contact(db, user_id):
    """Returns (email, name, is_anonymous). Prefers the Firestore user document, falls back to Firebase Auth."""
    snapshot = db.collection('users').document(user_id).get()
    data = (snapshot.to_dict() or {}) if snapshot.exists else {}
    email = data.get('email')
    name = data.get('displayName')
    if email and name:
        return email, name, False
    try:
        record = auth.get_user(user_id)
    except auth.UserNotFoundError:
        return email, name, not email
    email = email or record.email
    name = name or record.display_name
    return email, name, not email and not record.provider_data


def find_active_users(credentials_path, min_workouts, max_inactive_days, top):
    if not os.path.exists(credentials_path):
        raise SystemExit(f"Error: Credentials file not found at {credentials_path}")

    firebase_admin.initialize_app(credentials.Certificate(credentials_path))
    db = firestore.client()

    stats = collect_workout_stats(db)
    cutoff = int(time.time() * 1000) - max_inactive_days * DAY_MILLIS

    candidates = [
        (user_id, s) for user_id, s in stats.items()
        if s['total'] >= min_workouts and s['last'] >= cutoff
    ]
    # Ranking: total workouts, then most recent workout, then earliest first workout (tenure)
    candidates.sort(key=lambda c: (-c[1]['total'], -c[1]['last'], c[1]['first']))

    rows = []
    for user_id, s in candidates[:top]:
        email, name, anonymous = resolve_contact(db, user_id)
        rows.append({
            'userId': user_id,
            'name': name or '',
            'email': email or ('(anonymous)' if anonymous else '(no email)'),
            'total_workouts': s['total'],
            'last_workout_date': format_date(s['last']),
            'first_workout_date': format_date(s['first']),
        })
    return rows


def main():
    parser = argparse.ArgumentParser(description='Find the most active users by completed workout count.')
    parser.add_argument('--credentials', default='admin-credentials-prod.json', help='Path to service account credentials (default: admin-credentials-prod.json)')
    parser.add_argument('--min-workouts', type=int, default=3, help='Exclude users with fewer completed workouts (default: 3)')
    parser.add_argument('--max-inactive-days', type=int, default=90, help='Exclude users whose last workout is older than this (default: 90)')
    parser.add_argument('--top', type=int, default=15, help='Number of users to return (default: 15)')
    parser.add_argument('--output', help='Optional CSV output file')
    args = parser.parse_args()

    rows = find_active_users(args.credentials, args.min_workouts, args.max_inactive_days, args.top)
    fields = ['userId', 'name', 'email', 'total_workouts', 'last_workout_date', 'first_workout_date']

    print('\t'.join(fields))
    for row in rows:
        print('\t'.join(str(row[f]) for f in fields))

    if args.output:
        with open(args.output, 'w', newline='') as f:
            writer = csv.DictWriter(f, fieldnames=fields)
            writer.writeheader()
            writer.writerows(rows)
        print(f"Wrote {len(rows)} rows to {args.output}", flush=True)


if __name__ == '__main__':
    main()
