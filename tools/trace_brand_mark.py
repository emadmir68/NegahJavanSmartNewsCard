"""Trace the supplied wordmark into two code-native vector layers.

No raster output is produced. The archived source PNG remains unchanged.
Run from the repository root with Python, Pillow and NumPy installed.
"""
from collections import defaultdict
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app/src/main/res/drawable-nodpi/negahjavan_logo.png"
LEFT, TOP, RIGHT, BOTTOM = 140, 606, 1404, 1000
WIDTH, HEIGHT = RIGHT - LEFT, BOTTOM - TOP


def simplify(points, tolerance=0.9):
    if len(points) < 3:
        return points
    a, b = np.array(points[0]), np.array(points[-1])
    v = b - a
    offsets = np.array(points) - a
    distances = (abs(offsets[:, 0] * v[1] - offsets[:, 1] * v[0]) /
                 np.linalg.norm(v)) if v.any() else np.linalg.norm(offsets, axis=1)
    i = int(distances.argmax())
    if distances[i] <= tolerance:
        return [points[0], points[-1]]
    return simplify(points[:i + 1], tolerance)[:-1] + simplify(points[i:], tolerance)


def trace(mask):
    edges = defaultdict(list)
    pad = np.pad(mask, 1)
    sides = [((-1, 0), (0, 0), (1, 0)), ((0, 1), (1, 0), (1, 1)),
             ((1, 0), (1, 1), (0, 1)), ((0, -1), (0, 1), (0, 0))]
    for (dy, dx), a, b in sides:
        neighbor = pad[1 + dy:1 + dy + HEIGHT, 1 + dx:1 + dx + WIDTH]
        for y, x in zip(*np.where(mask & ~neighbor)):
            edges[(int(x) + a[0], int(y) + a[1])].append((int(x) + b[0], int(y) + b[1]))
    rings = []
    while edges:
        start = next(iter(edges))
        points, point = [start], start
        while True:
            choices = edges[point]
            if len(choices) > 1 and len(points) > 1:
                vx, vy = point[0] - points[-2][0], point[1] - points[-2][1]
                # Keep the filled side on the right at diagonally touching pixels.
                choices.sort(key=lambda q: vx * (q[1] - point[1]) - vy * (q[0] - point[0]))
            following = choices.pop()
            if not choices:
                del edges[point]
            point = following
            points.append(point)
            if point == start:
                break
        area = abs(sum(a[0] * b[1] - b[0] * a[1] for a, b in zip(points, points[1:]))) / 2
        # Exclude source noise and the tiny lower tip of the old tagline.
        if area > 500:
            pivot = max(range(len(points) - 1), key=lambda i: (points[i][0] - start[0]) ** 2 + (points[i][1] - start[1]) ** 2)
            ring = simplify(points[:pivot + 1])[:-1] + simplify(points[pivot:])[:-1]
            rings.append(ring)
    return " ".join("M" + " L".join(f"{x},{y}" for x, y in ring) + " Z" for ring in rings), len(rings)


pixels = np.array(Image.open(SOURCE).convert("RGB"))[TOP:BOTTOM, LEFT:RIGHT].astype(int)
r, g, b = pixels[:, :, 0], pixels[:, :, 1], pixels[:, :, 2]
masks = {"ink": (r < 105) & (g < 105) & (b < 105),
         "accent": (r > 130) & (g < 105) & (b < 130) & (r > g * 1.8)}
paths = {}
for name, mask in masks.items():
    path, rings = trace(mask)
    paths[name] = path
    xml = f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Vector outline derived from the user-supplied wordmark. See docs/design/BRAND-ASSETS.md. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="132dp" android:height="41.146dp"
    android:viewportWidth="{WIDTH}" android:viewportHeight="{HEIGHT}">
    <path android:fillColor="#FFFFFFFF" android:fillType="evenOdd"
        android:pathData="{path}" />
</vector>
'''
    (ROOT / f"app/src/main/res/drawable/negahjavan_mark_{name}.xml").write_text(xml)
    print(f"{name}: {rings} contours, {len(path)} path characters")
svg = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {WIDTH} {HEIGHT}">
  <title>نگاه جوان — transparent vector wordmark</title>
  <path fill="#171B21" fill-rule="evenodd" d="{paths['ink']}"/>
  <path fill="#D9082D" fill-rule="evenodd" d="{paths['accent']}"/>
</svg>
'''
(ROOT / "docs/design/assets/negahjavan-wordmark.svg").write_text(svg)
