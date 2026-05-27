import firebase_admin
from firebase_admin import credentials, storage
import os
import argparse
import glob

def upload_assets(input_dir, credentials_path, bucket_name_arg):
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

    # Handle bucket name and destination path if provided as "bucket/path"
    if "/" in bucket_name_arg:
        parts = bucket_name_arg.split("/", 1)
        actual_bucket_name = parts[0]
        destination_prefix = parts[1]
    else:
        actual_bucket_name = bucket_name_arg
        destination_prefix = ""

    # Initialize Firebase App
    cred = credentials.Certificate(credentials_path)
    if not firebase_admin._apps:
        firebase_admin.initialize_app(cred, {
            'storageBucket': actual_bucket_name
        })

    # Find all files in input directory
    files = [f for f in glob.glob(os.path.join(input_dir, "*")) if os.path.isfile(f)]

    if not files:
        print(f"⚠️ Warning: No files found in '{input_dir}'. Nothing to upload.", flush=True)
        return

    print(f"Target Bucket: {actual_bucket_name}", flush=True)
    print(f"Target Path Prefix: {destination_prefix if destination_prefix else '(root)'}", flush=True)
    print(f"Found {len(files)} files to upload.", flush=True)

    bucket = storage.bucket(actual_bucket_name)

    for i, file_to_upload in enumerate(files, 1):
        file_name = os.path.basename(file_to_upload)
        destination_blob_name = os.path.join(destination_prefix, file_name)

        print(f"\n[{i}/{len(files)}] Uploading '{file_name}'...", flush=True)

        try:
            blob = bucket.blob(destination_blob_name)

            # File-like wrapper to track progress
            class ProgressFile:
                def __init__(self, filename):
                    self.filename = filename
                    self.size = os.path.getsize(filename)
                    self.read_bytes = 0
                    self.file = open(filename, 'rb')

                def read(self, size):
                    data = self.file.read(size)
                    if data:
                        self.read_bytes += len(data)
                        percent = (self.read_bytes / self.size) * 100
                        print(f"\rProgress: {percent:.2f}% ({self.read_bytes}/{self.size} bytes)", end="", flush=True)
                    return data

                def __len__(self):
                    return self.size

                def close(self):
                    self.file.close()

            progress_file = ProgressFile(file_to_upload)
            try:
                blob.upload_from_file(progress_file, size=progress_file.size)
            finally:
                progress_file.close()

            print("\nUpload complete!", flush=True)

            # Make the blob public to get a public URL
            blob.make_public()

            print(f"Successfully uploaded: {file_name}", flush=True)
            print(f"Downloadable URL: {blob.public_url}", flush=True)

        except Exception as e:
            print(f"\n❌ Error during upload of {file_name}: {e}", flush=True)
            if "404" in str(e):
                print(f"Tip: Ensure '{actual_bucket_name}' is a valid bucket name (usually your-project.appspot.com)", flush=True)
                break # Stop if bucket is not found

    print("\n✅ All uploads finished.", flush=True)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Upload all assets from a directory to Firebase Storage.')
    parser.add_argument('--input', default='input', help='The input directory (default: input)')
    parser.add_argument('--credentials', default='admin-credentials-dev.json', help='Path to service account credentials (default: admin-credentials-dev.json)')
    parser.add_argument('--bucket', required=True, help='The Firebase Storage bucket name (e.g. project-id.appspot.com)')

    args = parser.parse_args()

    upload_assets(args.input, args.credentials, args.bucket)
