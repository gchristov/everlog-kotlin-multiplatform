import firebase_admin
from firebase_admin import credentials, firestore
import json
import os
import argparse

def export_collection(collection_name, output_file, credentials_path):
    # Try to find credentials if the provided path doesn't exist
    if not os.path.exists(credentials_path):
        alternate_paths = [
            os.path.join("..", credentials_path),
            os.path.join("tools", "firebase", credentials_path)
        ]
        for alt in alternate_paths:
            if os.path.exists(alt):
                credentials_path = alt
                break

    if not os.path.exists(credentials_path):
        print(f"Error: Credentials file not found at {credentials_path}", flush=True)
        return

    cred = credentials.Certificate(credentials_path)
    if not firebase_admin._apps:
        firebase_admin.initialize_app(cred)
    db = firestore.client()

    print(f"Exporting collection: {collection_name}...", flush=True)
    docs = db.collection(collection_name).stream()

    data = {}
    for doc in docs:
        data[doc.id] = doc.to_dict()

    if not data:
        print(f"No documents found in collection '{collection_name}' or collection does not exist. Creating an empty file.", flush=True)

    with open(output_file, 'w') as f:
        json.dump(data, f, indent=4, default=str)

    print(f"Exported {len(data)} documents to {output_file}", flush=True)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Export a Firestore collection to a JSON file.')
    parser.add_argument('collection', help='The name of the Firestore collection to export')
    parser.add_argument('--output', default='export.json', help='The output JSON file (default: export.json)')
    parser.add_argument('--credentials', default='admin-credentials-dev.json', help='Path to service account credentials (default: admin-credentials-dev.json)')

    args = parser.parse_args()

    export_collection(args.collection, args.output, args.credentials)
