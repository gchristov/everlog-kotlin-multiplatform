import firebase_admin
from firebase_admin import credentials, firestore
import json
import os
import argparse

def import_collection(collection_name, input_file, credentials_path):
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
        print(f"❌ Error: Credentials file not found at {credentials_path}", flush=True)
        return

    if not os.path.exists(input_file):
        print(f"⚠️ Warning: Input file '{input_file}' not found. Nothing to import.", flush=True)
        return

    cred = credentials.Certificate(credentials_path)
    if not firebase_admin._apps:
        firebase_admin.initialize_app(cred)
    db = firestore.client()

    with open(input_file, 'r') as f:
        data = json.load(f)

    print(f"Importing/Merging {len(data)} documents into collection: {collection_name}...", flush=True)

    batch = db.batch()
    count = 0
    for doc_id, doc_data in data.items():
        doc_ref = db.collection(collection_name).document(doc_id)
        # Use set with merge=True to merge changes
        batch.set(doc_ref, doc_data, merge=True)
        count += 1

        # Firestore batches are limited to 500 operations
        if count % 500 == 0:
            batch.commit()
            batch = db.batch()

    batch.commit()
    print(f"Successfully imported {count} documents.", flush=True)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Import a JSON file to a Firestore collection.')
    parser.add_argument('collection', help='The name of the Firestore collection to import into')
    parser.add_argument('--input', default='import.json', help='The input JSON file (default: import.json)')
    parser.add_argument('--credentials', default='admin-credentials-dev.json', help='Path to service account credentials (default: admin-credentials-dev.json)')

    args = parser.parse_args()

    import_collection(args.collection, args.input, args.credentials)
