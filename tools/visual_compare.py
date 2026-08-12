from __future__ import annotations

import collections
import sys
from pathlib import Path

import numpy as np
from PIL import Image


def describe(path: Path) -> None:
    image = Image.open(path).convert("RGB")
    pixels = np.asarray(image)
    content = pixels[60:1510]
    colors = collections.Counter(map(tuple, content.reshape(-1, 3)))

    print(f"\n{path.name} {image.size[0]}x{image.size[1]}")
    print("dominant:", " ".join(f"#{r:02X}{g:02X}{b:02X}:{count}" for (r, g, b), count in colors.most_common(14)))

    background = np.array(colors.most_common(1)[0][0])
    row_background_share = (pixels == background).all(axis=2).mean(axis=1)
    bands: list[tuple[int, int, float]] = []
    start = 0
    bucket = round(float(row_background_share[0]), 1)
    for index, value in enumerate(row_background_share[1:], start=1):
        next_bucket = round(float(value), 1)
        if next_bucket != bucket:
            if index - start >= 10:
                bands.append((start, index - 1, bucket))
            start = index
            bucket = next_bucket
    if len(row_background_share) - start >= 10:
        bands.append((start, len(row_background_share) - 1, bucket))
    print("background row bands:", bands[:24])


if __name__ == "__main__":
    for argument in sys.argv[1:]:
        describe(Path(argument))
