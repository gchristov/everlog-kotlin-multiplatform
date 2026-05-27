import os
from PIL import Image
import argparse
from collections import defaultdict

def resize_and_optimize(source_folder, output_folder, target_width=1024, quality=75):
    """
    Loops recursively through a folder of images, downscales them to mobile-friendly dimensions,
    and converts them into optimized WebP files. All output files are placed in the root
    of the output folder.
    """
    if not os.path.exists(source_folder):
        print(f"Error: Source folder '{source_folder}' does not exist.", flush=True)
        return

    # Create the output directory if it doesn't exist
    os.makedirs(output_folder, exist_ok=True)

    # Collect all image files recursively
    image_files = []
    for root, _, files in os.walk(source_folder):
        for file in files:
            if file.lower().endswith(('.jpg', '.jpeg', '.png')):
                image_files.append(os.path.join(root, file))

    if not image_files:
        print(f"No matching images found in '{source_folder}'", flush=True)
        return

    print(f"Found {len(image_files)} images to process...", flush=True)
    processed_count = 0
    successful_outputs = set()
    output_to_sources = defaultdict(list)

    for source_path in image_files:
        # Get only the filename to flatten the output structure
        filename = os.path.basename(source_path)
        print(f"Processing: {filename}...", flush=True)

        # Determine the output filename (swap extension to .webp)
        base_name = os.path.splitext(filename)[0]
        output_filename = f"{base_name}.webp"
        output_path = os.path.join(output_folder, output_filename)

        # Track which source paths map to this output path
        output_to_sources[output_path].append(source_path)

        try:
            with Image.open(source_path) as img:
                # Calculate new height maintaining aspect ratio
                original_width, original_height = img.size

                # Safeguard: Don't upscale if the image happens to be smaller than target_width
                if original_width > target_width:
                    w_percent = target_width / float(original_width)
                    target_height = int(float(original_height) * float(w_percent))

                    # LANCZOS is the gold standard for high-quality downsampling
                    img_resized = img.resize((target_width, target_height), Image.Resampling.LANCZOS)
                else:
                    img_resized = img

                # Save as WebP with quality compression
                img_resized.save(output_path, "WEBP", quality=quality)

                processed_count += 1
                successful_outputs.add(output_path)

                # Print progress every 10 files
                if processed_count % 10 == 0 or processed_count == len(image_files):
                    print(f"Progress: Processed {processed_count}/{len(image_files)} files.", flush=True)

        except Exception as e:
            print(f"Error processing file {source_path}: {e}", flush=True)

    print(f"\nSuccessfully optimized {processed_count} images into '{output_folder}'!", flush=True)

    # Verify files on disk
    output_files_on_disk = [f for f in os.listdir(output_folder) if os.path.isfile(os.path.join(output_folder, f))]
    disk_count = len(output_files_on_disk)
    print(f"Total files in output folder: {disk_count}", flush=True)

    # Check for missing files (files that were successfully processed but aren't found on disk)
    missing_files_info = []
    for path in successful_outputs:
        if not os.path.exists(path):
            missing_files_info.append((path, output_to_sources[path]))

    if missing_files_info:
        print(f"\n⚠️ WARNING: {len(missing_files_info)} output files were reported as optimized but are missing from the disk:", flush=True)
        for out_path, sources in missing_files_info:
            print(f"  - Output name: {os.path.basename(out_path)}", flush=True)
            for src in sources:
                print(f"    Failed from source: {src}", flush=True)

    # Check for collisions (if multiple source files mapped to the same output path)
    collisions = {out: sources for out, sources in output_to_sources.items() if len(sources) > 1}

    if collisions:
        print(f"\nℹ️ OVERWRITE REPORT: The following files were overwritten due to name collisions:", flush=True)
        for output_path, sources in collisions.items():
            output_name = os.path.basename(output_path)
            print(f"  - '{output_name}' was created from {len(sources)} sources (only the last one was kept):", flush=True)
            for src in sources:
                status = " (kept)" if src == sources[-1] else " (overwritten)"
                print(f"    source: {src}{status}", flush=True)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Resize images and convert to optimized WebP.')
    parser.add_argument('--input', default='./input', help='Input directory containing images')
    parser.add_argument('--output', default='./output', help='Output directory for optimized WebP images')
    parser.add_argument('--width', type=int, default=1024, help='Target width for resizing (default: 1024)')
    parser.add_argument('--quality', type=int, default=75, help='WebP quality (default: 75)')

    args = parser.parse_args()

    resize_and_optimize(args.input, args.output, target_width=args.width, quality=args.quality)
