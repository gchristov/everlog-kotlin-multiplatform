# Monitoring

The KPI dashboard shows the Roadmap's metrics for real new users, the onboarding v1 funnel, the paid test read and the loyal core. It's built from the Firebase Analytics export in BigQuery (`everlog-prod.analytics_173607556`) and published as a private claude.ai artifact:

**https://claude.ai/artifact/TzSQGc6sBGScDHQToKyfdz**

GA4's own reports count Play's test robots, sideloaded builds, reinstalls and the owner's account, so they don't show what real people do. This tool counts real users only, with the same definitions as the Funnel baseline in Notion, so its numbers can be compared against the 2021–2023 baseline. The SQL here is the only copy of these queries. The Notion pages (Funnel baseline, Roadmap, the paid test ticket) link here rather than keeping their own.

## How it works

```
sql/*.sql ──▶ BigQuery ──▶ out/*.csv ──┐
                                       ├──▶ build.py ──▶ out/data.json ──▶ out/dashboard.html ──▶ artifact
config.json + git tags ────────────────┘                  (filled into dashboard.html)
```

1. **`refresh.sh`** runs every file in `sql/` in parallel through `tools/bigquery/bq_query.sh`. That needs Docker and `tools/bigquery/bigquery-credentials-prod.json`; see the repo's `CLAUDE.md`. Each result lands in `out/<name>.csv`.
2. **`build.py`** (Python 3 standard library only) reads the CSVs, `config.json` and the release tags from git, and works out every number on the page into `out/data.json`. It then writes `out/dashboard.html`: `dashboard.html` with the data put in place of its `/*__DATA__*/null` placeholder.
3. **Publishing:** `out/dashboard.html` is published to the artifact above, always to the same URL so the link never changes. The page is self-contained, with no network calls apart from Google Fonts. It can't query BigQuery itself, since a service-account key must never go into a web page. That's why refreshing is a manual step.

A refresh takes a few minutes and reads about 2.5 GB, well inside BigQuery's free tier of 1 TB a month.

**What leaves BigQuery:** `out/new_users.csv` has one row per new user. It has no ids, but it does have install dates and behaviour, so `out/` is gitignored. `data.json` and the page hold only counts and rates.

## Refreshing

```sh
./tools/monitoring/refresh.sh
```

Then publish `tools/monitoring/out/dashboard.html` to the URL above. In a Claude Code session, "refresh the KPI dashboard" does both steps.

### Before each read

- **Paid test:** when the campaigns start, set `paid_test.start_date` in `config.json`. At each read, copy each campaign's spend so far (in euros) from Google Ads into its `spend_eur`. BigQuery has no Google Ads data. The Ads ↔ Firebase link only puts the campaign name on each install (`traffic_source.name`), so spend has to be entered by hand. Campaign names must match those in Google Ads exactly.
- **Chart labels:** release markers come from the git tags (minor releases, `x.y.0`). To label a marker, add its date to `events`.

## Real users only

Nothing on the dashboard includes non-real users. Every query leaves out:

| Who | How they're recognised |
|---|---|
| Play pre-launch and Test Lab robots that log in | any device that ever used the Test Lab account, `user_id = 'V23IEC4EeqadnnbeqUGWZEgFXmC2'` |
| Play pre-launch robots that aren't recognised in time (e.g. OnePlus `IN2025`, Android 11) | no country (`geo.country` empty) |
| Test Lab installs, scanners and local builds | `app_info.install_source = 'manual_install'` |
| Pre-launch runs on internal-track nightlies | `-nightly-` in `app_info.version` |
| The owner | any device that ever used `user_id = 'YOjJX6BgCDVQFZGM9kPFHAW8JfV2'` |

For new users, the install (`first_open`) must pass the filters. For the loyal core, each finished workout must, since most of the core installed before the export began in May 2020. Don't rely on a blank country alone: one robot run reported Thailand, which is why the account and install-source filters exist too.

**Checking the filters:** `checks/robots.sql` lists installs per month and how many each filter removes. It isn't part of the dashboard. Run it by hand when the numbers look odd, e.g. when real installs jump with no campaign running:

```sh
./tools/bigquery/bq_query.sh bigquery-credentials-prod.json tools/monitoring/checks/robots.sql
```

## Definitions

- **New users:** new accounts (`user_registered`) on real installs, plus real installs that never got an account. People reinstalling and logging in to an older account aren't new. Each user's events are followed across devices by their account, plus the anonymous events on the device they installed on.
- **Channel:** the install's `traffic_source.name` when it's one of the paid test campaigns (`UAC - Installs - Onboarding v1%`), "Other paid" for any other `cpc` install, otherwise "Organic".
- **Maturity:** a rate only counts users who have had long enough to reach it. That's 1 day for day-one metrics, 7 for week one, 30 for days 8–30, and 3 days after the first finished workout for the second workout. A recent install that hasn't finished a workout yet isn't a failure.
- **Small numbers:** below 10 users, a rate is shown with its count and marked "too few users" instead of being judged against the target.
- **Reached home:** a `screen_home` screen view. Headline rates count only users who reached home, as the baseline does.
- **Finished workout:** `workout_completed` or `workout_service_workout_completed` (finished from the notification).
- **Setup on day one:** within 24 hours of install, the user kept the starter templates or built their own in onboarding (`onboarding_finished` outcome `starter_templates` or `own_template`), created a routine, exercise or plan, or started a plan. Onboarding's own `settings_*_modified` events don't count, since every finished onboarding sends them.
- **Tourist:** no setup and no workout started on day one.
- **Week-one lifter:** a new user who finished a workout within 7 days of install.

## The page, section by section

**Headline metrics:** the Roadmap's metrics, comparing new users who installed in 2026 before onboarding v1 (`onboarding_v1_date` in `config.json`) with those who installed since. The status pill only judges the "since" column: on target, above baseline (between baseline and target) or below baseline.

| Metric | Counted over | Baseline → target |
|---|---|---|
| Finished a workout in week one (the verdict) | reached home, 7+ days since install | 21% → 30% |
| Did setup on day one | reached home, 1+ day | 42% → 60% |
| Tourists (lower is better) | reached home, 1+ day | 34% → 20% |
| Second workout within 3 days of the first | finished a workout, 3+ days since it | 41% → 50% |
| Finished a workout in days 8–30 | reached home, 30+ days | 7.6% → 10% |
| 3+ workouts in week one | reached home, 7+ days | 6.2%, direction only |

**Onboarding v1 funnel:** new users since onboarding v1. It shows each onboarding step viewed (`onboarding_step_viewed`), how onboarding ended (`onboarding_finished` outcome), reached home, then setup on day one and week-one workouts for users with enough days. It also has the paid test's health checks: 70%+ of people completing each step they view, and 30%+ keeping or building templates at the reveal.

**Paid test:** one row per campaign in `config.json`, plus organic, for users who installed since `paid_test.start_date` (or since onboarding v1 before the campaigns start). Columns: new users, finished onboarding, kept or built, users with 7 days, week-one lifters and their rate, spend, and cost per week-one lifter (spend ÷ week-one lifters). A ✓ marks a cost at or under `bar_eur_per_lifter`. The decision rules are on the paid test ticket (TAS-448).

**Trends:**
- New users per install week, organic vs paid. The tooltip shows how many finished a workout in week one.
- Workouts finished per week by all real users.
- People who finished at least one workout per month: the size of the loyal core.

## Files

| File | What it does |
|---|---|
| `sql/new_users.sql` | One row per real new user since 2026-01-01, with the flags every new-user metric is built from |
| `sql/core.sql` | Workouts finished and people who lifted, per week and per month since 2025 (all real users) |
| `checks/robots.sql` | Not on the dashboard: installs per month and what each filter removes |
| `config.json` | Onboarding v1 date, paid test start date, campaigns and spend, labelled chart markers |
| `build.py` | Works out every number into `out/data.json` and fills it into the page |
| `dashboard.html` | The page template. Light and dark themes, works at phone width |
| `refresh.sh` | Runs the queries and the build |

Each query runs on its own too, e.g. `./tools/bigquery/bq_query.sh bigquery-credentials-prod.json tools/monitoring/sql/core.sql`.

### Changing it

- **A new metric:** add the flag it needs to `sql/new_users.sql`, work it out in `build.py`, and show it in `dashboard.html`. Keep this README's definitions in step.
- **A new robot filter:** add it to both queries and to `checks/robots.sql`, and to the table above.
- **Moving the cohort start** (e.g. into 2027): change the `_TABLE_SUFFIX` start in `sql/new_users.sql`. Reading more months costs more data.
