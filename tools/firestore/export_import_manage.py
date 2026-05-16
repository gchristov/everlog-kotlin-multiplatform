import sys
import subprocess
import os

def main():
    if len(sys.argv) < 3:
        print("Usage:")
        print("  Export: docker run <image> export <collection_name> [--output filename.json]")
        print("  Import: docker run <image> import <collection_name> [--input filename.json]")
        sys.exit(1)

    command = sys.argv[1]
    collection = sys.argv[2]
    extra_args = sys.argv[3:]

    if command == "export":
        script = "export_collection.py"
    elif command == "import":
        script = "import_collection.py"
    else:
        print(f"Unknown command: {command}. Use 'export' or 'import'.")
        sys.exit(1)

    # Ensure the script exists
    if not os.path.exists(script):
        print(f"Error: {script} not found in the current directory.")
        sys.exit(1)

    full_command = ["python", script, collection] + extra_args
    subprocess.run(full_command)

if __name__ == "__main__":
    main()
