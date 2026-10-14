# Monitoring

The KPI dashboard: the Roadmap's metrics for real new users, the onboarding v1 funnel, the paid test read and the loyal core, built from the Firebase Analytics export in BigQuery (`everlog-prod.analytics_173607556`). It's published as a private claude.ai artifact:

**https://claude.ai/artifact/TzSQGc6sBGScDHQToKyfdz**

The SQL here is the only copy of these queries. The Notion pages (Funnel baseline, Roadmap, the paid test ticket) link here rather than keeping their own.

## Refreshing

```sh
./tools/monitoring/refresh.sh
```

It runs every query in `sql/` in parallel through `tools/bigquery/bq_query.sh` (Docker and `tools/bigquery/bigquery-credentials-prod.json` are needed; see the repo's `CLAUDE.md`), then `build.py` turns the results and `config.json` into `out/data.json` and fills them into `dashboard.html` as `out/dashboard.html`. It takes a few minutes and reads about 3 GB in total, well inside BigQuery's free tier.

Then publish `out/dashboard.html` to the artifact above. In a Claude Code session, "refresh the KPI dashboard" does both steps (publish to the URL above, so the link stays the same).

`out/` is gitignored. Its CSVs have one row per new user (no ids, but install dates and behaviour), and only counts and rates go into `data.json` and the page.

## Before each read

- **Paid test:** when the campaigns start, set `paid_test.start_date` in `config.json`. At each read, copy each campaign's spend so far (in euros) from Google Ads into its `spend_eur`. BigQuery has no Google Ads data: the Ads ↔ Firebase link only puts the campaign name on each install (`traffic_source.name`).
- **Events:** add a line to `events` for anything that should be labelled on the charts. Releases come from the git tags.

## Definitions

These match the Funnel baseline in Notion, so the numbers can be compared against it.

- **Real users:** leave out every device that ever used the Test Lab account (`V23IEC4EeqadnnbeqUGWZEgFXmC2`, Play's pre-launch robots) or the owner's account (`YOjJX6BgCDVQFZGM9kPFHAW8JfV2`), sideloaded installs (`install_source = 'manual_install'`, which also covers scanners and local builds), nightly versions and devices with no country (robots that weren't recognised in time).
- **New users:** new accounts (`user_registered`) on real installs, plus real installs that never got an account. People reinstalling and logging in to an older account aren't new. Each user's events are followed across devices by account.
- **Headline rates** count new users who reached the home screen, as the baseline does. A rate only counts users who have had long enough to reach it: 1 day for day-one metrics, 7 for week one, 30 for days 8–30, and 3 days after the first finished workout for the second workout. Below 10 users a rate is shown as "too few users".
- **Setup on day one:** kept the starter templates or built their own in onboarding (`onboarding_finished` outcome), created a routine, exercise or plan, or started a plan, within 24 hours of install. Onboarding's own `settings_*_modified` events don't count, since every finished onboarding sends them.
- **Tourist:** no setup and no workout started on day one.
- **Finished workout:** `workout_completed` or `workout_service_workout_completed` (finished from the notification).
- **Week-one lifter:** a new user who finished a workout within 7 days of install. Cost per week-one lifter = a campaign's spend ÷ its week-one lifters.
- **Baselines and targets** (2021–2023 organic → target) come from the Roadmap: finished a workout in week one 21% → 30%, setup on day one 42% → 60%, tourists 34% → 20%, second workout within 3 days 41% → 50%, finished a workout in days 8–30 7.6% → 10%, 3+ workouts in week one 6.2%.

## Queries

| File | What it returns |
|---|---|
| `sql/new_users.sql` | One row per real new user since 2026-01-01, with the flags every new-user metric is built from |
| `sql/core.sql` | Workouts finished and people who lifted, per week since 2025 (all real users) |
| `sql/core_monthly.sql` | The same per month: the size of the loyal core |
| `sql/robots.sql` | Installs per month and what each robot filter removes, as a health check |

Each file runs on its own too, e.g. `./tools/bigquery/bq_query.sh bigquery-credentials-prod.json tools/monitoring/sql/robots.sql`.
