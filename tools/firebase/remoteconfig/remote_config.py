"""Reads and changes Firebase Remote Config parameters through its REST API.

Only a parameter's default value and description change, or a whole parameter goes: everything else in the
template is kept. Each change publishes a new template version, so the console's history can roll it back.
"""
import argparse
import json
import sys

import requests
from google.auth.transport.requests import Request
from google.oauth2 import service_account

SCOPE = 'https://www.googleapis.com/auth/firebase.remoteconfig'


def session(credentials_file):
    credentials = service_account.Credentials.from_service_account_file(credentials_file, scopes=[SCOPE])
    credentials.refresh(Request())
    with open(credentials_file) as f:
        project = json.load(f)['project_id']
    return project, {'Authorization': f'Bearer {credentials.token}'}


def fetch(project, headers):
    # Gzip, as the API only sends the ETag a change needs then
    response = requests.get(
        f'https://firebaseremoteconfig.googleapis.com/v1/projects/{project}/remoteConfig',
        headers={**headers, 'Accept-Encoding': 'gzip'},
    )
    response.raise_for_status()
    return response.json(), response.headers['ETag']


def publish(project, headers, template, etag):
    response = requests.put(
        f'https://firebaseremoteconfig.googleapis.com/v1/projects/{project}/remoteConfig',
        headers={**headers, 'Content-Type': 'application/json; UTF-8', 'If-Match': etag},
        data=json.dumps(template),
    )
    response.raise_for_status()
    return response.json().get('version', {}).get('versionNumber')


def describe(name, parameter):
    value = parameter.get('defaultValue', {}).get('value', '(in-app default)')
    kind = parameter.get('valueType', 'STRING')
    conditions = ', '.join(parameter.get('conditionalValues', {}))
    line = f'{name} = {value} ({kind})'
    if conditions:
        line += f' [conditions: {conditions}]'
    if parameter.get('description'):
        line += f'\n    {parameter["description"]}'
    return line


def main():
    parser = argparse.ArgumentParser(description='Read and change Firebase Remote Config parameters.')
    parser.add_argument('--credentials', required=True, help='Service account key of the project to use')
    commands = parser.add_subparsers(dest='command', required=True)
    listing = commands.add_parser('list', help='Every parameter, or those whose name contains a text')
    listing.add_argument('filter', nargs='?', default='')
    get = commands.add_parser('get', help='One parameter, with its conditional values')
    get.add_argument('name')
    change = commands.add_parser('set', help="Set a parameter's default value and/or description, adding it if new")
    change.add_argument('name')
    change.add_argument('--value', help='The new default value, e.g. true')
    change.add_argument('--type', choices=['BOOLEAN', 'STRING', 'NUMBER', 'JSON'],
                        help="The value's type for a new parameter (default STRING); kept for an existing one")
    change.add_argument('--description', help='The description shown in the console')
    remove = commands.add_parser('delete', help='Remove a parameter, conditional values and all')
    remove.add_argument('name')
    args = parser.parse_args()

    project, headers = session(args.credentials)
    template, etag = fetch(project, headers)
    parameters = template.setdefault('parameters', {})

    if args.command == 'list':
        for name in sorted(parameters):
            if args.filter in name:
                print(describe(name, parameters[name]))
        return

    if args.command == 'get':
        if args.name not in parameters:
            sys.exit(f'{project}: no parameter {args.name}')
        print(f'{project}: ' + describe(args.name, parameters[args.name]))
        print(json.dumps(parameters[args.name], indent=2))
        return

    if args.command == 'delete':
        if args.name not in parameters:
            sys.exit(f'{project}: no parameter {args.name}')
        before = describe(args.name, parameters.pop(args.name))
        version = publish(project, headers, template, etag)
        print(f'{project}, version {version}:\n  deleted: {before}')
        return

    if args.value is None and args.description is None:
        sys.exit('Nothing to set: pass --value and/or --description')
    parameter = parameters.setdefault(args.name, {'valueType': args.type or 'STRING'})
    before = describe(args.name, parameter) if 'defaultValue' in parameter else '(new)'
    if args.value is not None:
        parameter['defaultValue'] = {'value': args.value}
    if args.description is not None:
        parameter['description'] = args.description
    version = publish(project, headers, template, etag)
    print(f'{project}, version {version}:\n  before: {before}\n  after:  {describe(args.name, parameter)}')


if __name__ == '__main__':
    main()
