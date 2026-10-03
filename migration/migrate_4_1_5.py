#!/usr/bin/env python3
"""
Migrates a TFC: Real World map profile from 4.1.5 (and earlier 4.x) to 4.2.0.

    python migrate_4_1_5.py <old profile folder> <new profile folder>

A profile folder is the one that holds settings.json and maps/. The old folder
is only read; the new one is created. See README.md next to this script.

4.1.5 maps                      4.2.0 maps
  continent.png      -> continent.png      (unchanged: black sea, white land)
  altitude.png    \\
  divergence.png   } -> tectonics.png + tectonics.json
  hotspots.png    /
  koppen.png      \\     temperature.png   (real degrees)
  temperature.png  } -> rainfall.png      (real millimetres)
  rainfall.png    /     rain_variance.png (seasonality)

Requires Python 3.9+, numpy and Pillow.
"""

import argparse
import json
import shutil
import sys
from pathlib import Path

import numpy as np
from PIL import Image

Image.MAX_IMAGE_PIXELS = None

BLOCKS_PER_CELL = 128.0

# ---------------------------------------------------------------- 4.1.5 values

# koppen.png colours of 4.1.5; a pixel takes the nearest of them.
KOPPEN_COLORS = {
    "AF": (0, 0, 220), "AS": (0, 100, 240), "AW": (0, 150, 220), "AM": (40, 80, 200),
    "BWH": (210, 0, 0), "BSH": (210, 120, 0), "BWK": (200, 80, 80), "BSK": (200, 120, 60),
    "CSA": (250, 250, 0), "CSB": (180, 180, 0), "CSC": (120, 120, 0),
    "CWA": (100, 240, 130), "CWB": (80, 210, 120), "CWC": (70, 160, 110),
    "CFA": (170, 240, 90), "CFB": (140, 200, 80), "CFC": (110, 170, 70),
    "DSA": (190, 20, 190), "DSB": (160, 20, 180), "DSC": (130, 20, 170), "DSD": (100, 20, 160),
    "DFA": (40, 190, 190), "DFB": (30, 170, 170), "DFC": (20, 150, 140), "DFD": (10, 130, 110),
    "DWA": (80, 80, 220), "DWB": (70, 70, 190), "DWC": (60, 60, 160), "DWD": (60, 60, 130),
    "ET": (190, 190, 190), "EF": (80, 80, 80),
}  # fmt: skip
ZONES = list(KOPPEN_COLORS)

# The climates 4.1.5 chose from: every combination on this lattice.
OLD_TEMPERATURES = np.arange(-25, 31, dtype=np.float32)
OLD_RAINFALLS = np.arange(0, 501, 10, dtype=np.float32)
OLD_VARIANCES = (np.float32(-1) + np.arange(21, dtype=np.float32) * np.float32(0.1)).clip(-1, 1)
OLD_GRID = 64  # steps a gray level was rounded to within a zone

# 4.1.5 altitude.png: 0..127 sea (darker is deeper), 128..255 land height 0..24.
SEA_LEVEL_GRAY = 128
MAX_LAND_HEIGHT = 24
UPLAND_HEIGHT = 5
HIGHLAND_HEIGHT = 11
MOUNTAIN_HEIGHT = 18
MOUNTAIN_EDGE_HEIGHT = 15
SHELF_MAX_RAW_DEPTH = 5  # the sea is a shelf down to this depth (of 2..15)
ATOLL_MAX_RAW_DEPTH = 7  # deeper sea than this had no atolls
TRENCH_MIN_RAW_DEPTH = 10  # and from this depth it was a trench

# 4.1.5 divergence.png: 128 neutral with a dead zone, then up to +-2.
DIVERGENCE_NEUTRAL = 128
DIVERGENCE_DEAD_ZONE = 24
DIVERGENCE_MAX = 2.0
RIFT_DIVERGENCE = 1.0  # rift valleys and ocean ridges above this
TRENCH_DIVERGENCE = -0.6  # trenches and volcanic mountains below this

# Region cells, as vanilla TFC counts distances.
COASTAL_MOUNTAIN_CELLS = 3  # a mountain this close to the sea is a coastal one
ATOLL_SHELF_CELLS = 4  # vanilla: atolls further than this from land
ATOLL_DEEP_CELLS = 3
ARC_TRENCH_CELLS = 2  # a shelf this close to a trench boundary is a volcanic arc
ARC_LAND_CELLS = (1, 8)  # if it lies between these distances from land

# ---------------------------------------------------------------- 4.2.0 values

NEW_TEMPERATURE = (-50.0, 50.0)  # temperature.png: gray 0..255
NEW_RAINFALL = (0.0, 500.0)  # rainfall.png
NEW_VARIANCE = (-1.0, 1.0)  # rain_variance.png, wet summer is positive

BOUNDARIES = ("none", "convergent", "divergent")
LANDS = ("lowland", "upland", "highland", "mountain")
WATERS = ("shelf", "ridge", "deep", "trench")
VOLCANISMS = ("none", "arc")
LEGEND_DEFAULTS = {
    "boundary": "none", "land": "lowland", "water": "deep",
    "volcanism": "none", "coast": 0, "atolls": 0, "hotspot": 0,
}  # fmt: skip


# -------------------------------------------------------------------- climate


def classify(temperature, rainfall, variance):
    """
    TFC's Köppen classification of a climate (northern hemisphere reading of
    the rainfall variance), as 4.1.5 restricted it: a temperate or continental
    climate is only valid with the rainfall its letter stands for. Returns
    the zone name, or None for a combination 4.1.5 never used.
    """
    if temperature < -17 + 0.006 * rainfall:
        return "EF"
    if temperature <= -12:
        return "ET"
    if rainfall < 75:
        return "BWH" if temperature > 18 else "BWK"
    if rainfall < 150:
        return "BSH" if temperature > 18 else "BSK"
    if temperature > 21:
        if rainfall * (1 + variance) > 600:
            return "AM"
        if variance > 0.5:
            return "AW"
        if variance < -0.5:
            return "AS"
        return "AF"
    if temperature > 17:
        group = "C", "A"
    elif temperature > 12:
        group = "C", "B"
    elif temperature > 8:
        group = "C", "C"
    elif temperature > 3:
        group = "D", "A"
    elif temperature > -2:
        group = "D", "B"
    elif temperature > -8:
        group = "D", "C"
    else:
        group = "D", "D"
    if variance > 0.5 and rainfall > 315:
        return group[0] + "W" + group[1]
    if variance < -0.5 and rainfall < 175:
        return group[0] + "S" + group[1]
    if -0.5 <= variance <= 0.5 and 175 <= rainfall <= 315:
        return group[0] + "F" + group[1]
    return None


def classify_tfc(temperature, rainfall, variance):
    """TFC's own classification, which the game shows for a place."""
    t, r, v = temperature, rainfall, variance
    zone = np.full(t.shape, "", dtype="<U3")

    def put(mask, name):
        zone[(zone == "") & mask] = name

    put(t < -17 + 0.006 * r, "EF")
    put(t <= -12, "ET")
    put((r < 75) & (t > 18), "BWH")
    put(r < 75, "BWK")
    put((r < 150) & (t > 18), "BSH")
    put(r < 150, "BSK")
    hot = t > 21
    put(hot & (r * (1 + v) > 600), "AM")
    put(hot & (v > 0.5), "AW")
    put(hot & (v < -0.5), "AS")
    put(hot, "AF")
    season = np.where(v > 0.5, "W", np.where(v < -0.5, "S", "F"))
    for low, group, band in (
        (17, "C", "A"), (12, "C", "B"), (8, "C", "C"),
        (3, "D", "A"), (-2, "D", "B"), (-8, "D", "C"), (-1e9, "D", "D"),
    ):  # fmt: skip
        mask = (zone == "") & (t > low)
        zone[mask] = np.char.add(np.char.add(group, season[mask]), band)
    return zone


def old_zone_grids():
    """
    For every zone, what 4.1.5 made of a temperature gray level and a
    rainfall gray level inside it: three OLD_GRID x OLD_GRID tables of
    temperature, rainfall and rainfall variance.

    4.1.5 stretched the gray levels over the range of the zone and took the
    nearest climate of its lattice that classifies as the zone.
    """
    by_zone = {zone: {} for zone in ZONES}
    for t in OLD_TEMPERATURES:
        for r in OLD_RAINFALLS:
            for v in OLD_VARIANCES:
                zone = classify(float(t), float(r), float(v))
                if zone is not None:
                    pair = (float(t), float(r))
                    by_zone[zone].setdefault(pair, []).append(float(v))
    grids = {}
    steps = np.arange(OLD_GRID) / (OLD_GRID - 1)
    for zone, pairs in by_zone.items():
        if not pairs:
            raise SystemExit(f"internal error: no climate for zone {zone}")
        t = np.array([p[0] for p in pairs])
        r = np.array([p[1] for p in pairs])
        # The middle of the variances that keep the zone. 4.1.5 took the
        # lowest, which sits on the threshold to the next zone and does not
        # survive the blending of neighbouring pixels in 4.2.0.
        v = np.array([sorted(pairs[p])[(len(pairs[p]) - 1) // 2] for p in pairs])
        t_span = max(t.max() - t.min(), 1e-3)
        r_span = max(r.max() - r.min(), 1e-3)
        v_span = max(v.max() - v.min(), 1e-3)
        # 4.1.5 looked through the lattice in this order and kept the first
        # of equally near climates.
        order = np.argsort(
            (t - t.min()) / t_span + (r - r.min()) / r_span + (v - v.min()) / v_span, kind="stable"
        )
        t, r, v = t[order], r[order], v[order]
        target_t = t.min() + t_span * steps
        target_r = r.min() + r_span * steps
        distance = ((t[None, None, :] - target_t[:, None, None]) / t_span) ** 2 + (
            (r[None, None, :] - target_r[None, :, None]) / r_span
        ) ** 2
        nearest = np.argmin(distance, axis=2)
        grids[zone] = (t[nearest], r[nearest], v[nearest])
    return grids


def nearest_zone(koppen_rgb):
    """Index into ZONES of the nearest 4.1.5 colour, for every pixel."""
    colors = np.array([KOPPEN_COLORS[z] for z in ZONES], dtype=np.int32)
    flat = koppen_rgb.reshape(-1, 3).astype(np.int32)
    unique, inverse = np.unique(flat, axis=0, return_inverse=True)
    distance = ((unique[:, None, :] - colors[None, :, :]) ** 2).sum(axis=2)
    return np.argmin(distance, axis=1)[inverse.reshape(-1)].reshape(koppen_rgb.shape[:2])


def encode(values, bounds, keeps_zone=None):
    """
    Gray levels of real values on a 4.2.0 climate map. Rounding moves a value
    by up to half a gray level, which can carry it over a threshold of the
    classification; keeps_zone(decoded) tells which pixels are still in their
    zone, and the others take the neighbouring gray level that is.
    """
    low, high = bounds
    exact = (values - low) / (high - low) * 255.0
    gray = np.clip(np.rint(exact), 0, 255)
    if keeps_zone is not None:
        for other in (np.floor(exact), np.ceil(exact), gray - 1, gray + 1):
            other = np.clip(other, 0, 255)
            wrong = ~keeps_zone(decode(gray, bounds))
            better = wrong & keeps_zone(decode(other, bounds))
            gray = np.where(better, other, gray)
    return gray.astype(np.uint8)


def decode(gray, bounds):
    low, high = bounds
    return low + gray.astype(np.float64) / 255.0 * (high - low)


def migrate_climate(maps, report):
    """temperature.png, rainfall.png and rain_variance.png of 4.2.0."""
    zone_index = nearest_zone(np.array(Image.open(maps / "koppen.png").convert("RGB")))
    temperature_gray = load_gray(maps / "temperature.png", zone_index.shape)
    rainfall_gray = load_gray(maps / "rainfall.png", zone_index.shape)
    t_step = np.rint(temperature_gray / 255.0 * (OLD_GRID - 1)).astype(int)
    r_step = np.rint(rainfall_gray / 255.0 * (OLD_GRID - 1)).astype(int)

    temperature = np.zeros(zone_index.shape)
    rainfall = np.zeros(zone_index.shape)
    variance = np.zeros(zone_index.shape)
    grids = old_zone_grids()
    for index, zone in enumerate(ZONES):
        mask = zone_index == index
        if mask.any():
            t, r, v = grids[zone]
            temperature[mask] = t[t_step[mask], r_step[mask]]
            rainfall[mask] = r[t_step[mask], r_step[mask]]
            variance[mask] = v[t_step[mask], r_step[mask]]

    wanted = np.array(ZONES)[zone_index]

    def zone_of(t, r, v):
        return classify_tfc(t, r, v)

    # Encode one map at a time, each keeping the zone with the others exact.
    t_gray = encode(temperature, NEW_TEMPERATURE, lambda t: zone_of(t, rainfall, variance) == wanted)
    t_new = decode(t_gray, NEW_TEMPERATURE)
    r_gray = encode(rainfall, NEW_RAINFALL, lambda r: zone_of(t_new, r, variance) == wanted)
    r_new = decode(r_gray, NEW_RAINFALL)
    v_gray = encode(variance, NEW_VARIANCE, lambda v: zone_of(t_new, r_new, v) == wanted)
    v_new = decode(v_gray, NEW_VARIANCE)

    kept = zone_of(t_new, r_new, v_new) == wanted
    report["climate zones kept"] = f"{kept.mean() * 100:.2f}% of {kept.size} pixels"
    report["temperature"] = f"{t_new.min():.1f} to {t_new.max():.1f} °C"
    report["rainfall"] = f"{r_new.min():.0f} to {r_new.max():.0f} mm"
    return t_gray, r_gray, v_gray, kept


# ------------------------------------------------------------------ tectonics


def chessboard_distance(mask, limit):
    """
    Pixels from every pixel to the nearest one of the mask, counted as
    vanilla counts cells (a diagonal step is one), up to limit.
    """
    distance = np.where(mask, 0, limit + 1).astype(np.int32)
    reached = mask.copy()
    for step in range(1, limit + 1):
        grown = reached.copy()
        for dz in (-1, 0, 1):
            for dx in (-1, 0, 1):
                grown |= shifted(reached, dx, dz, False)
        distance[grown & ~reached] = step
        reached = grown
    return distance


def shifted(array, dx, dz, fill):
    """The array moved by (dx, dz), with fill where nothing moves in."""
    out = np.full_like(array, fill)
    height, width = array.shape
    src_z = slice(max(0, -dz), height - max(0, dz))
    dst_z = slice(max(0, dz), height - max(0, -dz))
    src_x = slice(max(0, -dx), width - max(0, dx))
    dst_x = slice(max(0, dx), width - max(0, -dx))
    out[dst_z, dst_x] = array[src_z, src_x]
    return out


def mountain_cores(height, land):
    """
    4.1.5 mountains: land of height 18 and more, and the height 15 to 17
    that stands in a group (a neighbour of 16, two of 15, or for 16 and 17
    one of 14).
    """
    neighbours_15 = np.zeros(height.shape, dtype=np.int32)
    any_16 = np.zeros(height.shape, dtype=bool)
    any_14 = np.zeros(height.shape, dtype=bool)
    for dz in (-1, 0, 1):
        for dx in (-1, 0, 1):
            if dx == 0 and dz == 0:
                continue
            near = shifted(np.where(land, height, -1), dx, dz, -1)
            neighbours_15 += near >= 15
            any_16 |= near >= 16
            any_14 |= near >= 14
    grouped = any_16 | (neighbours_15 >= 2) | ((height >= 16) & any_14)
    return land & ((height >= MOUNTAIN_HEIGHT) | ((height >= MOUNTAIN_EDGE_HEIGHT) & grouped))


def migrate_tectonics(maps, land, cells_per_pixel, report):
    """tectonics.png (palette indices) and the classes of tectonics.json."""
    shape = land.shape
    altitude = load_gray(maps / "altitude.png", shape)
    divergence_gray = load_gray(maps / "divergence.png", shape, missing=DIVERGENCE_NEUTRAL)
    hotspots = load_gray(maps / "hotspots.png", shape, missing=0)

    # Land relief.
    height = np.clip(
        np.rint((altitude - SEA_LEVEL_GRAY) / (255.0 - SEA_LEVEL_GRAY) * MAX_LAND_HEIGHT), 0, MAX_LAND_HEIGHT
    )
    mountain = mountain_cores(height, land)
    relief = np.zeros(shape, dtype=np.int32)
    relief[height >= UPLAND_HEIGHT] = 1
    relief[height >= HIGHLAND_HEIGHT] = 2
    relief[mountain] = 3

    # Seafloor. Sea on the continent map with land on the altitude map (the
    # two maps disagree along a coast) is the shallowest sea.
    raw_depth = np.clip(np.rint(1 + (1 - (np.minimum(altitude, 127) + 1) / SEA_LEVEL_GRAY) * 14), 2, 15)
    delta = divergence_gray - DIVERGENCE_NEUTRAL
    divergence = np.where(
        np.abs(delta) <= DIVERGENCE_DEAD_ZONE,
        0.0,
        np.where(
            delta > 0,
            np.clip((delta - DIVERGENCE_DEAD_ZONE) / (255.0 - DIVERGENCE_NEUTRAL - DIVERGENCE_DEAD_ZONE), 0, 1),
            -np.clip((-delta - DIVERGENCE_DEAD_ZONE) / (DIVERGENCE_NEUTRAL - DIVERGENCE_DEAD_ZONE), 0, 1),
        )
        * DIVERGENCE_MAX,
    )
    reach = max(1, int(np.ceil(ARC_LAND_CELLS[1] / cells_per_pixel)) + 1)
    to_land = chessboard_distance(land, reach) * cells_per_pixel
    to_sea = chessboard_distance(~land, reach) * cells_per_pixel
    to_trench = chessboard_distance(divergence < TRENCH_DIVERGENCE, reach) * cells_per_pixel

    # 4.1.5: the deepest sea is a trench, a ridge runs where the plates part,
    # and a shelf about a cell wide lies along every coast.
    shelf, ridge, deep, trench = (WATERS.index(name) for name in ("shelf", "ridge", "deep", "trench"))
    water = np.full(shape, deep, dtype=np.int32)
    water[(raw_depth <= SHELF_MAX_RAW_DEPTH) | (to_land <= 1.0)] = shelf
    water[divergence > RIFT_DIVERGENCE] = ridge
    water[raw_depth >= TRENCH_MIN_RAW_DEPTH] = trench

    boundary = np.zeros(shape, dtype=np.int32)
    boundary[divergence < TRENCH_DIVERGENCE] = BOUNDARIES.index("convergent")
    boundary[divergence > RIFT_DIVERGENCE] = BOUNDARIES.index("divergent")

    # 4.1.5: mountains at a convergent boundary are volcanic and a shelf by a
    # trench boundary is a volcanic arc; mountains by the sea are coastal;
    # vanilla atolls stand in warm sea far from land, but not in the abyss.
    volcanic_land = land & mountain & (divergence < TRENCH_DIVERGENCE)
    arc_sea = (
        ~land
        & (water == shelf)
        & (divergence < 0)
        & (to_trench <= ARC_TRENCH_CELLS)
        & (to_land > ARC_LAND_CELLS[0])
        & (to_land < ARC_LAND_CELLS[1])
    )
    volcanism = (volcanic_land | arc_sea).astype(np.int32)
    coast = (mountain & (to_sea < COASTAL_MOUNTAIN_CELLS)).astype(np.int32)
    atolls = (
        ~land
        & (
            ((water == shelf) & (to_land > ATOLL_SHELF_CELLS))
            | ((water == deep) & (raw_depth <= ATOLL_MAX_RAW_DEPTH) & (to_land > ATOLL_DEEP_CELLS))
        )
    ).astype(np.int32)

    hotspot = np.zeros(shape, dtype=np.int32)
    hotspot[hotspots > 32] = 4
    hotspot[hotspots > 95.5] = 3
    hotspot[hotspots > 159.5] = 2
    hotspot[hotspots > 223.5] = 1

    # The game reads one half of a class, by the continent map: the other
    # half is left at its default, so the palette stays small.
    relief = np.where(land, relief, 0)
    water = np.where(land, WATERS.index("deep"), water)
    coast = np.where(land, coast, 0)

    code = (((((boundary * 4 + relief) * 4 + water) * 2 + volcanism) * 2 + coast) * 2 + atolls) * 5 + hotspot
    codes, indices = np.unique(code, return_inverse=True)
    if len(codes) > 256:
        raise SystemExit(f"{len(codes)} tectonic classes do not fit a 256-colour palette")
    classes, palette = [], []
    for value in codes:
        value, age = divmod(int(value), 5)
        value, has_atolls = divmod(value, 2)
        value, is_coast = divmod(value, 2)
        value, volcanic = divmod(value, 2)
        value, water_index = divmod(value, 4)
        boundary_index, land_index = divmod(value, 4)
        fields = {
            "boundary": BOUNDARIES[boundary_index],
            "land": LANDS[land_index],
            "water": WATERS[water_index],
            "volcanism": VOLCANISMS[volcanic],
            "coast": is_coast,
            "atolls": has_atolls,
            "hotspot": age,
        }
        classes.append({k: v for k, v in fields.items() if v != LEGEND_DEFAULTS[k]})
        palette.extend(class_color(fields))
    report["tectonic classes"] = len(classes)
    report["mountains"] = f"{mountain.sum() / max(land.sum(), 1) * 100:.1f}% of land"
    report["hotspot pixels"] = int((hotspot > 0).sum())
    return indices.reshape(shape).astype(np.uint8), palette, classes


def class_color(fields):
    """A colour to tell the classes apart in an image editor; the game ignores it."""
    if fields["hotspot"]:
        return (255, 60 * fields["hotspot"] - 40, 0)
    if fields["boundary"] == "divergent":
        return (60, 110, 230) if fields["water"] == "ridge" else (230, 90, 200)
    if fields["boundary"] == "convergent":
        return (150, 40, 40) if fields["land"] == "mountain" else (90, 30, 90)
    land = {"lowland": (120, 170, 100), "upland": (170, 180, 110), "highland": (190, 160, 110), "mountain": (140, 110, 90)}
    water = {"shelf": (150, 190, 230), "ridge": (60, 110, 230), "deep": (40, 60, 120), "trench": (15, 20, 60)}
    red, green, blue = land[fields["land"]]
    sea = water[fields["water"]]
    # Both halves show: the land colour tinted by the seafloor.
    if fields["land"] == "lowland":
        red, green, blue = sea
    return (red - 20 * fields["coast"], green, min(255, blue + 20 * fields["atolls"]))


# ---------------------------------------------------------------------- files


def load_gray(path, shape, missing=None):
    """A map as float gray levels, checked against the size of the others."""
    if not path.exists():
        if missing is None:
            raise SystemExit(f"{path} is missing")
        print(f"  {path.name} is missing: treated as empty")
        return np.full(shape, float(missing))
    array = np.array(Image.open(path).convert("L"), dtype=np.float64)
    if array.shape != shape:
        raise SystemExit(f"{path.name} is {array.shape[1]}x{array.shape[0]}, the other maps are {shape[1]}x{shape[0]}")
    return array


def migrate(old, new):
    old, new = Path(old), Path(new)
    old_maps, new_maps = old / "maps", new / "maps"
    if not (old_maps / "continent.png").exists():
        raise SystemExit(f"{old_maps / 'continent.png'} not found: pass the folder that holds settings.json and maps/")
    if new.resolve() == old.resolve():
        raise SystemExit("the new profile folder must differ from the old one")
    settings_path = old / "settings.json"
    settings = json.loads(settings_path.read_text(encoding="utf-8")) if settings_path.exists() else {}
    horizontal_scale = float(settings.get("horizontal_scale", 40000))

    continent = np.array(Image.open(old_maps / "continent.png").convert("L"))
    land = continent > 127
    cells_per_pixel = 2.0 * horizontal_scale / continent.shape[1] / BLOCKS_PER_CELL
    report = {"map size": f"{continent.shape[1]}x{continent.shape[0]}", "land": f"{land.mean() * 100:.1f}%"}

    new_maps.mkdir(parents=True, exist_ok=True)
    for name in old.iterdir():
        if name.is_file():
            shutil.copy2(name, new / name.name)
    Image.fromarray(np.where(land, 255, 0).astype(np.uint8), mode="L").save(new_maps / "continent.png", optimize=True)

    indices, palette, classes = migrate_tectonics(old_maps, land, cells_per_pixel, report)
    image = Image.fromarray(indices, mode="P")
    image.putpalette(list(np.array(palette, dtype=np.uint8).clip(0, 255).flatten()) + [0] * (768 - len(palette)))
    image.save(new_maps / "tectonics.png", optimize=True)
    legend = "[\n" + ",\n".join("  " + json.dumps(c) for c in classes) + "\n]\n"
    (new / "tectonics.json").write_text(legend, encoding="utf-8")

    if (old_maps / "koppen.png").exists():
        t_gray, r_gray, v_gray, _ = migrate_climate(old_maps, report)
        Image.fromarray(t_gray, mode="L").save(new_maps / "temperature.png", optimize=True)
        Image.fromarray(r_gray, mode="L").save(new_maps / "rainfall.png", optimize=True)
        Image.fromarray(v_gray, mode="L").save(new_maps / "rain_variance.png", optimize=True)
    else:
        print("  koppen.png is missing: no climate maps written (turn Climate from map off, or paint them)")

    print(f"Migrated {old} -> {new}")
    for key, value in report.items():
        print(f"  {key}: {value}")
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("old", help="4.1.5 profile folder (with settings.json and maps/)")
    parser.add_argument("new", help="folder to create the 4.2.0 profile in")
    args = parser.parse_args()
    migrate(args.old, args.new)
    return 0


if __name__ == "__main__":
    sys.exit(main())
