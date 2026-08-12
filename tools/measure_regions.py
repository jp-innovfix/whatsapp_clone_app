from __future__ import annotations

import sys
from pathlib import Path

import numpy as np
from PIL import Image


REGIONS = {
    "title": (20, 80, 300, 170),
    "camera": (530, 80, 650, 165),
    "search": (20, 175, 720, 290),
    "archive": (20, 330, 720, 410),
    "row1": (20, 420, 720, 550),
    "navigation": (0, 1340, 738, 1510),
}


def measure(path: Path) -> None:
    pixels = np.asarray(Image.open(path).convert("RGB"))
    print(path)
    for name, (x1, y1, x2, y2) in REGIONS.items():
        crop = pixels[y1:y2, x1:x2]
        maximum = crop.max(axis=2)
        minimum = crop.min(axis=2)
        mask = (maximum > 180) & ((maximum - minimum) < 45)
        ys, xs = np.where(mask)
        bounds = None if not len(xs) else (
            x1 + xs.min(),
            y1 + ys.min(),
            x1 + xs.max() + 1,
            y1 + ys.max() + 1,
        )
        print(f"  {name}: {bounds}")


if __name__ == "__main__":
    for argument in sys.argv[1:]:
        measure(Path(argument))
