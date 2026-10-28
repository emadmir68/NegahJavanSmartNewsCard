"""Restore hash-pinned NASA photographs used only by native design tests.

The successful review workflow commits these unchanged inputs alongside its
renders. Subsequent builds use the committed files without a network request.
"""
import hashlib
from pathlib import Path
from urllib.request import urlopen

ROOT = Path(__file__).resolve().parents[1]
PHOTOS = [
    ("webb-portrait.jpg", "https://science.nasa.gov/wp-content/uploads/2023/04/webb_4.jpg",
     "313a63f70e89009580640c0cf249b73eec7ef42a62859a6d7c72744d065c0f0d"),
    ("webb-landscape.png", "https://science.nasa.gov/wp-content/uploads/2024/09/wallpaper-cleanroomground-bkgr-orig.png",
     "734f529d342ab81f4fb00521a72aec1c2ac89c8b5386f2b4485182c3a4b69230"),
]
folder = ROOT / "app/src/test/resources/photos"
folder.mkdir(parents=True, exist_ok=True)
for name, url, expected in PHOTOS:
    path = folder / name
    data = path.read_bytes() if path.exists() else urlopen(url, timeout=30).read()
    if hashlib.sha256(data).hexdigest() != expected:
        raise RuntimeError(f"Photo source changed: {name}")
    if not path.exists():
        path.write_bytes(data)
    print(f"Verified {name}: {len(data)} bytes")
