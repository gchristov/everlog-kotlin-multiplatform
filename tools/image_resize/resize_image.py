import os
from PIL import Image, ImageChops
import argparse
from collections import defaultdict

def resize_and_optimize(source_folder, output_folder, target_size=1024, quality=75):
    """
    Extracts the main illustration from images, centers them on a square canvas
    with the original background color, and saves as optimized WebP.
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

        # Determine the output filename (swap extension to .webp)
        base_name = os.path.splitext(filename)[0]
        output_filename = f"{base_name}.webp"
        output_path = os.path.join(output_folder, output_filename)

        # Track which source paths map to this output path
        output_to_sources[output_path].append(source_path)

        try:
            with Image.open(source_path) as img:
                # Convert to RGB to handle various formats consistently
                img = img.convert("RGB")

                # 0. Pre-crop a small border (e.g., 5px) to remove potential edge noise/borders
                # that can interfere with background detection and illustration extraction.
                edge_margin = 25
                if img.width > edge_margin * 2 and img.height > edge_margin * 2:
                    img = img.crop((edge_margin, edge_margin, img.width - edge_margin, img.height - edge_margin))

                # 1. Detect background color (sampling corners for robustness)
                w, h = img.size
                corners = [img.getpixel((0, 0)), img.getpixel((w-1, 0)),
                           img.getpixel((0, h-1)), img.getpixel((w-1, h-1))]
                bg_color = max(set(corners), key=corners.count)

                # 2. Find the bounding box of the illustration
                bg = Image.new("RGB", img.size, bg_color)
                diff = ImageChops.difference(img, bg)

                # Convert to grayscale and use a gentle threshold to find content
                # Threshold of 10 avoids capturing minor JPEG noise but keeps illustration edges
                mask = diff.convert("L").point(lambda x: 255 if x > 10 else 0)
                bbox = mask.getbbox()

                if not bbox:
                    print(f"Skipping {filename}: No illustration detected (uniform background).", flush=True)
                    continue

                # 3. Crop tightly around the illustration
                cropped_img = img.crop(bbox)

                # 4. Calculate scaling factor (75% of the target square size)
                max_content_size = int(target_size * 0.75)

                # Resize the cropped illustration maintaining aspect ratio
                cropped_img.thumbnail((max_content_size, max_content_size), Image.Resampling.LANCZOS)

                # 5. Create a square canvas with the original background color
                new_canvas = Image.new("RGB", (target_size, target_size), bg_color)

                # 6. Center the resized illustration onto the square canvas
                offset_x = (target_size - cropped_img.width) // 2
                offset_y = (target_size - cropped_img.height) // 2
                new_canvas.paste(cropped_img, (offset_x, offset_y))

                # 7. Save as WebP with quality compression
                new_canvas.save(output_path, "WEBP", quality=quality)

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

    # Check for missing files
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

    # Check for collisions
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
    parser = argparse.ArgumentParser(description='Extract illustration, center on square canvas, and convert to WebP.')
    parser.add_argument('--input', default='./input', help='Input directory containing images')
    parser.add_argument('--output', default='./output', help='Output directory for optimized WebP images')
    parser.add_argument('--size', type=int, default=1024, help='Target square size (default: 1024)')
    parser.add_argument('--quality', type=int, default=75, help='WebP quality (default: 75)')

    args = parser.parse_args()

    # Note: Using args.size for target_size
    resize_and_optimize(args.input, args.output, target_size=args.size, quality=args.quality)
