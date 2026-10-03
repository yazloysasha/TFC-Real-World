# Migrating map profiles from 4.1.5 to 4.2.0

TFC: Real World 4.2.0 reads different maps than 4.1.5. A custom profile made
for 4.1.5 (or any earlier 4.x) will not load until it is converted. The
scripts here do that.

| Script             | What it does                                                            |
| ------------------ | ----------------------------------------------------------------------- |
| `migrate_4_1_5.py` | Converts a 4.1.5 profile folder into a 4.2.0 one.                       |
| `check_profile.py` | Checks that a folder is a valid 4.2.0 profile and prints what it holds. |

## Quick start

You need Python 3.9 or newer with `numpy` and `Pillow`:

```sh
pip install numpy pillow
```

A profile folder is the one that holds `settings.json` and `maps/`:

```sh
python migrate_4_1_5.py path/to/old_profile path/to/new_profile
python check_profile.py path/to/new_profile
```

The old folder is only read. Install the new one where the old one was:
`config/tfc_real_world/profiles/{namespace}/{profile_name}/`.

Worlds made with 4.1.5 do not carry over: terrain generates differently, so
start a new world.

## What changes

| 4.1.5                                            | 4.2.0                                                  | How it is converted                |
| ------------------------------------------------ | ------------------------------------------------------ | ---------------------------------- |
| `continent.png`                                  | `continent.png`                                        | Kept: black is sea, white is land. |
| `altitude.png`, `divergence.png`, `hotspots.png` | `tectonics.png` + `tectonics.json`                     | See below.                         |
| `koppen.png`, `temperature.png`, `rainfall.png`  | `temperature.png`, `rainfall.png`, `rain_variance.png` | See below.                         |
| `settings.json` and anything else in the folder  | the same                                               | Copied as is.                      |

All converted maps keep the size of the old ones.

### Tectonics

4.2.0 has one indexed map, `tectonics.png`, whose palette entries are classes
described in `tectonics.json`: what kind of place each pixel is. The script
derives the classes with the same rules 4.1.5 applied in the game:

- **Land relief** from the altitude map: lowland, upland (height 5 of 24 and
  up), highland (11 and up), mountain (18 and up, or 15 to 17 standing in a
  group).
- **Seafloor** from the altitude map: shelf (shallow sea and a strip along
  every coast), deep, trench (the deepest sea).
- **Plate boundaries** from the divergence map: divergent above the rift
  threshold, which makes rift valleys on land and ridges at sea; convergent
  below the trench threshold, which makes mountains there volcanic and a
  shelf beside it a volcanic arc.
- **Coastal mountains**: mountains within three region cells of the sea.
- **Atolls**: sea far enough from land, as vanilla TFC places them (it still
  asks for warm water itself).
- **Hotspots** from the hotspot map, with the same four ages.

The palette colours of `tectonics.png` are only there to tell the classes
apart in an image editor; the game reads the index.

### Climate

In 4.1.5 the Köppen map chose a climate zone and the temperature and rainfall
maps were shades _within_ that zone. In 4.2.0 the three maps hold real values
and TFC derives the zone from them:

- `temperature.png`: `0` = −50 °C … `255` = 50 °C
- `rainfall.png`: `0` = 0 mm … `255` = 500 mm
- `rain_variance.png`: `0` = −1 (wet winter) … `255` = 1 (wet summer), by the
  local season in both hemispheres

The script turns every pixel into the temperature and rainfall 4.1.5 would
have given it, and picks a rainfall seasonality that keeps the pixel in its
zone. Every pixel of the converted maps classifies as the zone the Köppen
map gave it.

## What the script cannot give you

A converted profile is the old world in the new format, not a 4.2.0-quality
world. These are new in 4.2.0 and have no source in 4.1.5 maps:

- **Lakes and islands**: paint them into `continent.png` with the gray bands
  `64` (island), `128` (fresh lake) and `192` (salt lake).
- **Rivers** (`rivers.bin`) and **ridge axes** (`ridges.bin`): without them
  TFC makes its own procedural rivers and ridge crests.
- **Reefs, transform boundaries, rift and intraplate volcanism**: classes
  4.1.5 had no data for.
- **Detail**: the default 4.2.0 maps are eight times finer than the 4.1.5
  ones. A converted map keeps its old resolution.

The formats are described in the main README.

## Differences you may notice

- **Seasonality.** 4.1.5 used the weakest rainfall seasonality a zone allows,
  right on the edge of the next zone. The script uses the middle of what the
  zone allows, so that zones survive the blending 4.2.0 does between pixels.
  Wet and dry seasons are a little more pronounced than before.
- **Zone borders.** 4.2.0 averages the climate over a region cell and adds
  moisture near rivers and lakes, so the zone the game shows can differ from
  the Köppen map along borders and rivers.
- **No smoothing pass.** 4.1.5 smoothed the climate between neighbouring
  pixels at load time. The converted maps hold the unsmoothed values; 4.2.0
  blends them when it samples the maps.

## How this was tested

The three default profiles of 4.1.5 (taken from the `v4.1.5` tag) were
converted and checked:

- every pixel of the converted climate maps classifies as its Köppen zone
  (100% in all three profiles);
- the converted Full World profile was loaded into 4.2.0 and the whole world
  generated: at region cell level 91% of land has exactly the old zone and
  97% the same climate group, 95% of the land the old altitude map marked as
  mountain height is mountain, and the world has rifts, ridges, trenches,
  volcanic arcs, atolls and all four hotspot ages.
