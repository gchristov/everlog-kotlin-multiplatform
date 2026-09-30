import argparse
import csv
import os
import sys

from google.cloud import bigquery
from google.oauth2 import service_account

GB = 1024 ** 3


def read_sql(args):
    if args.query:
        return args.query
    if args.file and args.file != '-':
        with open(args.file) as f:
            return f.read()
    return sys.stdin.read()


def format_value(value):
    return '' if value is None else str(value)


def print_table(fields, rows, max_width):
    cells = [[format_value(row[name])[:max_width] for name in fields] for row in rows]
    widths = [max([len(name)] + [len(r[i]) for r in cells]) for i, name in enumerate(fields)]
    print('  '.join(name.ljust(widths[i]) for i, name in enumerate(fields)))
    print('  '.join('-' * w for w in widths))
    for r in cells:
        print('  '.join(value.ljust(widths[i]) for i, value in enumerate(r)))


def run(args):
    if not os.path.exists(args.credentials):
        raise SystemExit(f"Error: Credentials file not found at {args.credentials}")
    sql = read_sql(args).strip()
    if not sql:
        raise SystemExit("Error: No SQL given. Pass a file, --query, or pipe it on stdin.")

    creds = service_account.Credentials.from_service_account_file(args.credentials)
    client = bigquery.Client(project=args.project, credentials=creds)

    # A dry run costs nothing and reports how much the query would scan
    dry = client.query(sql, job_config=bigquery.QueryJobConfig(dry_run=True, use_query_cache=False))
    scanned = dry.total_bytes_processed or 0
    print(f"Query will scan {scanned / GB:.3f} GB (limit {args.max_gb} GB).", file=sys.stderr)
    if args.dry_run:
        return
    if scanned > args.max_gb * GB:
        raise SystemExit(f"Error: Query would scan more than {args.max_gb} GB. Narrow it down, or raise the limit with --max-gb.")

    job_config = bigquery.QueryJobConfig(maximum_bytes_billed=int(args.max_gb * GB))
    rows = list(client.query(sql, job_config=job_config).result())
    if not rows:
        print("No rows.", file=sys.stderr)
        return
    fields = list(rows[0].keys())

    if args.output:
        with open(args.output, 'w', newline='') as f:
            writer = csv.writer(f)
            writer.writerow(fields)
            for row in rows:
                writer.writerow([format_value(row[name]) for name in fields])
        print(f"Wrote {len(rows)} rows to {args.output}", file=sys.stderr)
    if args.csv:
        writer = csv.writer(sys.stdout)
        writer.writerow(fields)
        for row in rows:
            writer.writerow([format_value(row[name]) for name in fields])
    elif not args.output:
        print_table(fields, rows[:args.limit], args.max_width)
        if len(rows) > args.limit:
            print(f"... {len(rows) - args.limit} more rows (use --output or --csv for all)", file=sys.stderr)


def main():
    parser = argparse.ArgumentParser(description='Run a read-only SQL query against BigQuery.')
    parser.add_argument('file', nargs='?', help='SQL file to run, or - for stdin (default: stdin)')
    parser.add_argument('--query', '-q', help='SQL to run, instead of a file')
    parser.add_argument('--credentials', default='bigquery-credentials-prod.json', help='Path to service account credentials')
    parser.add_argument('--project', default='everlog-prod', help='Project to bill the query to (default: everlog-prod)')
    parser.add_argument('--max-gb', type=float, default=5, help='Refuse to run queries that scan more than this (default: 5)')
    parser.add_argument('--dry-run', action='store_true', help='Only report how much data the query would scan')
    parser.add_argument('--csv', action='store_true', help='Print all rows as CSV instead of a table')
    parser.add_argument('--output', help='Also write all rows to this CSV file')
    parser.add_argument('--limit', type=int, default=200, help='Max rows to print as a table (default: 200)')
    parser.add_argument('--max-width', type=int, default=60, help='Truncate table cells to this width (default: 60)')
    run(parser.parse_args())


if __name__ == '__main__':
    main()
