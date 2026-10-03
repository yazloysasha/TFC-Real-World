#!/usr/bin/env python3
"""
Checks that a map profile folder is a valid TFC: Real World 4.2.0 profile.

    python check_profile.py <profile folder>

Prints what the profile holds and every problem found; exits with 1 if the
game would fail to load it or would generate a broken world.

Requires Python 3.9+, numpy and Pillow.
"""

import json
import struct
import sys
from pathlib import Path

import numpy as np
from PIL import Image

from migrate_4_1_5 import NEW_RAINFALL, NEW_TEMPERATURE, NEW_VARIANCE, classify_tfc, decode

Image.MAX_IMAGE_PIXELS = None

BANDS = {0: "ocean", 64: "island", 128: "fresh lake", 192: "salt lake", 255: "land"}
OLD_MAPS = ("altitude.png", "divergence.png", "hotspots.png", "koppen.png")
LEGEND_VALUES = {
    "boundary": {"none", "convergent", "divergent", "transform"},
    "land": {"lowland", "upland", "highland", "mountain"},
    "water": {"reef", "shelf", "ridge", "deep", "trench"},
    "volcanism": {"none", "arc", "rift", "intraplate"},
}
BINARY_FILES = {"rivers.bin": b"TFRW", "ridges.bin": b"TFRG"}


def check(profile):
    profile = Path(profile)
    maps = profile / "maps"
    errors, warnings = [], []

    def size(image):
        return f"{image.size[0]}x{image.size[1]}"

    if not (profile / "settings.json").exists():
        errors.append("settings.json is missing: the game does not see the folder as a profile")
    else:
        try:
            json.loads((profile / "settings.json").read_text(encoding="utf-8"))
        except ValueError as error:
            errors.append(f"settings.json is not valid JSON: {error}")

    leftovers = [name for name in OLD_MAPS if (maps / name).exists()]
    if leftovers and not (maps / "tectonics.png").exists():
        errors.append(
            "this is a 4.1.5 profile (" + ", ".join(leftovers) + " and no tectonics.png): run migrate_4_1_5.py on it first"
        )
    elif leftovers:
        warnings.append("4.1.5 maps that 4.2.0 does not read: " + ", ".join(leftovers))

    # Continent.
    land = None
    if not (maps / "continent.png").exists():
        errors.append("maps/continent.png is missing")
    else:
        image = Image.open(maps / "continent.png")
        gray = np.array(image.convert("L"))
        nearest = np.array(list(BANDS))[np.abs(gray[..., None].astype(int) - np.array(list(BANDS))).argmin(axis=2)]
        land = nearest != 0
        shares = ", ".join(f"{BANDS[v]} {(nearest == v).mean() * 100:.1f}%" for v in BANDS if (nearest == v).any())
        print(f"continent.png {size(image)}: {shares}")
        if (np.abs(gray.astype(int) - nearest) > 16).mean() > 0.01:
            warnings.append("continent.png has grays between the five bands (antialiased edges?): each takes the nearest band")

    # Tectonics.
    has_image, has_legend = (maps / "tectonics.png").exists(), (profile / "tectonics.json").exists()
    if not has_image:
        warnings.append("maps/tectonics.png is missing: Tectonics from map will not work")
    elif not has_legend:
        errors.append("tectonics.json is missing next to settings.json")
    else:
        image = Image.open(maps / "tectonics.png")
        if image.mode not in ("P", "L"):
            errors.append(f"tectonics.png is {image.mode}: it must be an indexed (palette) or 8-bit grayscale PNG")
        else:
            indices = np.array(image)
            try:
                legend = json.loads((profile / "tectonics.json").read_text(encoding="utf-8"))
            except ValueError as error:
                legend = None
                errors.append(f"tectonics.json is not valid JSON: {error}")
            if legend is not None:
                if not isinstance(legend, list) or len(legend) > 256:
                    errors.append("tectonics.json must be a list of at most 256 classes")
                else:
                    if indices.max() >= len(legend):
                        errors.append(
                            f"tectonics.png uses palette index {int(indices.max())}, tectonics.json has {len(legend)} classes"
                        )
                    for number, entry in enumerate(legend):
                        for key, value in entry.items():
                            if key in LEGEND_VALUES and value not in LEGEND_VALUES[key]:
                                errors.append(f"tectonics.json class {number}: unknown {key} '{value}'")
                            elif key == "hotspot" and value not in (0, 1, 2, 3, 4):
                                errors.append(f"tectonics.json class {number}: hotspot age {value}")
                            elif key not in LEGEND_VALUES and key not in ("coast", "atolls", "hotspot"):
                                warnings.append(f"tectonics.json class {number}: unknown field '{key}'")
                    used = np.unique(indices)
                    print(f"tectonics.png {size(image)}: {len(used)} of {len(legend)} classes used")

    # Climate.
    climate = {}
    for name, bounds in (("temperature", NEW_TEMPERATURE), ("rainfall", NEW_RAINFALL), ("rain_variance", NEW_VARIANCE)):
        path = maps / f"{name}.png"
        if not path.exists():
            warnings.append(f"maps/{name}.png is missing: Climate from map will not work")
            continue
        image = Image.open(path)
        values = decode(np.array(image.convert("L")), bounds)
        climate[name] = values
        print(f"{name}.png {size(image)}: {values.min():.1f} to {values.max():.1f}")
    if len(climate) == 3 and len({v.shape for v in climate.values()}) == 1:
        zones = classify_tfc(climate["temperature"], climate["rainfall"], climate["rain_variance"])
        if land is not None and land.shape != zones.shape:
            on_land = np.array(Image.fromarray(land).resize(zones.shape[::-1], Image.NEAREST))
        else:
            on_land = land if land is not None else np.ones(zones.shape, dtype=bool)
        names, counts = np.unique(zones[on_land], return_counts=True)
        top = sorted(zip(counts, names), reverse=True)
        print("climate zones on land: " + ", ".join(f"{n} {c / on_land.sum() * 100:.1f}%" for c, n in top[:12]))
    elif len(climate) == 3:
        warnings.append("the three climate maps differ in size (allowed, but usually a mistake)")

    for name, magic in BINARY_FILES.items():
        path = maps / name
        if path.exists():
            header = path.read_bytes()[:12]
            if len(header) < 12 or header[:4] != magic or struct.unpack(">i", header[4:8])[0] != 1:
                errors.append(f"maps/{name} is not a version 1 {magic.decode()} file")
            else:
                print(f"{name}: {struct.unpack('>i', header[8:12])[0]} entries")

    for warning in warnings:
        print(f"warning: {warning}")
    for error in errors:
        print(f"ERROR: {error}")
    print("OK" if not errors else f"{len(errors)} error(s)")
    return not errors


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit(__doc__)
    sys.exit(0 if check(sys.argv[1]) else 1)
