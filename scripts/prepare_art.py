#!/usr/bin/env python3
"""Convert a Midjourney ink-sketch render into a shippable Hǎo Shield drawable.

The app's illustrations (see ill_open_window / ill_mountains) are alpha-only ink:
the PNG carries black strokes whose opacity IS the drawing, and the app tints them
at render time (ColorFilter.tint), so one asset follows both Calm and Dusk. This
script turns a render — dark ink on white/cream paper, possibly with photographed
paper texture — into that form:

  1. flatten to luminance,
  2. auto-level between the paper white and the ink black (percentile-based, so
     cream grounds and photo grain drop out),
  3. alpha = darkness, colour = pure black,
  4. resize to the placement's pixel width (3x the dp size, for drawable-xxhdpi),
  5. write app/src/main/res/drawable-xxhdpi/<name>.png

Usage:
  python3 scripts/prepare_art.py SRC.png ill_name WIDTH_PX [--trim] [--keep-color]

  --trim        crop away empty margins before resizing (keeps 2% breathing room)
  --keep-color  preserve hue (e.g. the cranes' red crowns): output keeps its own
                colours over transparency instead of alpha-only ink. Such assets
                must NOT be tinted in compose, and need a hand-made drawable-night
                variant if they should read differently at dusk.
"""

import sys
from pathlib import Path

from PIL import Image

RES_DIR = Path(__file__).resolve().parent.parent / "app/src/main/res/drawable-xxhdpi"

# Auto-level percentiles: what counts as "paper" and as "ink" in this render.
PAPER_PCT = 75  # this bright or brighter -> fully transparent
INK_PCT = 1     # this dark or darker    -> fully opaque


def levels(luma: Image.Image) -> tuple[int, int]:
    hist = luma.histogram()
    total = sum(hist)
    acc = 0
    ink = 0
    paper = 255
    for value, count in enumerate(hist):
        acc += count
        if ink == 0 and acc >= total * INK_PCT / 100:
            ink = value
        if acc >= total * PAPER_PCT / 100:
            paper = value
            break
    return max(ink, 0), max(paper, ink + 1)


def main() -> None:
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    flags = {a for a in sys.argv[1:] if a.startswith("--")}
    if len(args) != 3:
        sys.exit(__doc__)
    src, name, width = Path(args[0]), args[1], int(args[2])

    image = Image.open(src).convert("RGB")
    luma = image.convert("L")
    ink, paper = levels(luma)
    span = paper - ink

    # Darkness -> alpha, levelled so paper vanishes and ink is solid.
    alpha = luma.point(lambda l: 255 - max(0, min(255, round((l - ink) * 255 / span))))

    if "--keep-color" in flags:
        out = image.convert("RGBA")
        out.putalpha(alpha)
    else:
        out = Image.new("RGBA", image.size, (0, 0, 0, 0))
        black = Image.new("RGBA", image.size, (0, 0, 0, 255))
        out = Image.composite(black, out, alpha)
        out.putalpha(alpha)

    if "--trim" in flags:
        box = out.getbbox()
        if box:
            pad_x = round(out.width * 0.02)
            pad_y = round(out.height * 0.02)
            out = out.crop((
                max(0, box[0] - pad_x), max(0, box[1] - pad_y),
                min(out.width, box[2] + pad_x), min(out.height, box[3] + pad_y),
            ))

    height = round(out.height * width / out.width)
    out = out.resize((width, height), Image.LANCZOS)

    RES_DIR.mkdir(parents=True, exist_ok=True)
    dest = RES_DIR / f"{name}.png"
    out.save(dest, optimize=True)
    print(f"{src.name} -> {dest.relative_to(RES_DIR.parent.parent.parent.parent)}"
          f"  {width}x{height}  (ink<= {ink}, paper>= {paper})")


if __name__ == "__main__":
    main()
