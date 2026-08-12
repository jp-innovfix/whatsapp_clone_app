from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image


NAMES = [
    "avatar_own",
    "avatar_mia",
    "avatar_dev",
    "avatar_sara",
    "avatar_rohan",
    "avatar_mona",
    "avatar_reena",
    "avatar_aisha",
    "avatar_spare",
]


source = Image.open(sys.argv[1]).convert("RGB")
destination = Path(sys.argv[2])
destination.mkdir(parents=True, exist_ok=True)
cell_width = source.width // 3
cell_height = source.height // 3
inset = max(3, source.width // 300)

for index, name in enumerate(NAMES):
    row, column = divmod(index, 3)
    left = column * cell_width + inset
    top = row * cell_height + inset
    right = (column + 1) * cell_width - inset
    bottom = (row + 1) * cell_height - inset
    portrait = source.crop((left, top, right, bottom)).resize((256, 256), Image.Resampling.LANCZOS)
    portrait.save(destination / f"{name}.webp", "WEBP", quality=88, method=6)
