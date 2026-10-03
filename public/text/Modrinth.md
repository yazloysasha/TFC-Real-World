# TFC: Real World 🌍

**🎉 The Ultra-Realistic Update is here!** Real rivers, lakes and islands, a detailed coastline, and a world map with 400+ waypoints and advancements since v4.2.0 on Minecraft 1.21.1 (older Minecraft versions will get it later).

![The world from above](https://raw.githubusercontent.com/yazloysasha/TFC-Real-World/refs/heads/1.21.x/public/img/satellite.png)

### Survive on the Real Earth

**The whole planet, block by block, with TerraFirmaCraft's survival on top.** 🌄

Sail down the Nile to its delta. Paddle up the Amazon through the rainforest. Cross the Sahara, climb into the Himalayas, winter on the shore of Baikal, or hop from atoll to atoll across the Pacific until Hawaii's volcanoes rise from the sea. Every coast, river and mountain range is where you know it should be - and everything you love about TFC is still there, untouched. 🗺️

#### 🧭 What Awaits You:

- **Real Rivers & Lakes:** The Mississippi, the Volga, the Yangtze and thousands of kilometres of other great rivers run where they really do, wide where they are mighty, and fan out into deltas at the sea. Victoria and the Great Lakes hold fresh water; the Dead Sea and the Great Salt Lake are salt, as in reality. 🏞️
- **A Detailed Coastline & Real Islands:** The fjords of Norway, the boot of Italy, the thousand islands of Indonesia - bays, peninsulas and inland seas drawn as on a real map, islands from Britain and Japan down to small ocean islets, and atolls where coral reefs actually grow. 🏝️
- **Real Mountains & Volcanoes:** The Andes and the Alps rise where plates collide, rift valleys open where they pull apart, and Iceland and the Ring of Fire get their volcanoes. 🏔️🌋
- **Real Climates:** From the jungles of the Congo through the savannas and the Gobi to the Siberian taiga and the ice of Antarctica - each with TFC's authentic seasons. ☀️❄️
- **Start Wherever You Like:** Type in any latitude and longitude and begin your survival exactly there - in your home town, for example, at the foot of Kilimanjaro or on a Caribbean island. 📍
- **True to Size:** An equal-area map keeps every land its real size - Greenland is not bigger than Africa - on a world 80,000 by 40,000 blocks across, and you can make it larger or smaller. 📐
- **Conquer the Whole Planet:** 400+ real cities and settlements, from national capitals to remote island villages and polar stations, are waiting on the geography map in your inventory, each with its own advancement. Reach Paris, Cairo, Tokyo, Canberra, Cape Town... Can you set foot on every one of them? 🧭🏆
- **Or Bring Your Own World:** Play the whole Earth, the Old World or the New World alone - or drop in your own maps and play on any planet you can draw. 🪐

**It is still TFC.** The mod only tells TerraFirmaCraft what kind of place every spot on Earth is; TFC builds the biomes, the rocks and the seasons itself. Prefer TFC's random generation for some part of the world? Continents, lakes, tectonics, volcanoes, rivers and climate each have their own switch on the world creation screen and in the config.

---

### ⚙️ Technical Details & Configuration

<details>
<summary><b>How It Works & Features 🏞️</b></summary>

The mod works by replacing TFC's default noise generators with data sampled from customizable map images. This integrates seamlessly, letting TFC's rich procedural detail fill in the local terrain.

![Altitude, biomes and climate of the generated world](https://raw.githubusercontent.com/yazloysasha/TFC-Real-World/refs/heads/1.21.x/public/img/collage.png)

- **Continents, Islands & Lakes:** A world map shapes landmasses, oceans, islands down to single islets, and fresh and salt lakes.
- **Tectonics & Relief:** A tectonics map tells TFC what kind of place every region is: lowland or mountain, rift or collision belt, volcanic arc, coral reef sea. TFC then picks the biome itself, so the Himalayas become collisional mountains, East Africa gets rift valleys and fjord coasts get oceanic mountains.
- **Volcanoes:** Hotspot shield volcanoes stand at their real locations, from active to ancient.
- **Rivers:** The real river network of the profile, with widths from real discharge and deltas from real delta data. TFC carves, bends and floods them as its own rivers.
- **Climate System:** Temperature, rainfall and rainfall-seasonality maps give every place its real climate zone (tropical, arid, temperate, continental, polar), which TFC's existing systems use to create biomes.
- **Geography Map & Advancements:** A new inventory tab shows the world map with your position and the profile's waypoints, filtered by continent, region and subregion. Coming close to a waypoint discovers it and grants its advancement.
- **Non-Intrusive:** No new blocks, items, or mobs. Uses Mixins to feed TFC's own world generation with map data.
- **Enhanced Canyon Biomes:** Optional config to make canyon biomes purely erosional, removing volcanic features (1.21.1 only).

</details>

<details>
<summary><b>Configuration Guide 🛠️</b></summary>

**Important Version Notice:** This configuration guide applies to **TFC: Real World v4.0.3+ (1.21.1), v3.0.4+ (1.20.1), and v2.0.2+ (1.18.2)**. If you are using an older version, I strongly recommend updating to the latest version for access to these improved configuration options. Legacy versions use a different configuration system and are no longer supported with guides.

All configuration is accessible directly from the **TFC world creation screen** for easy adjustment. Advanced users can also modify the config files manually.

#### 📋 Map Profiles

- **Map Profile**: Select which set of map images to use for world generation (e.g., Full World, Old World). The default profile contains all necessary Earth map data.

#### 📍 Spawn Settings

Choose where you start your adventure:

- **Spawn Mode**:
  - `GEOGRAPHIC`: Spawn using real-world coordinates! Set a latitude and longitude.
  - `RANDOM`: A random location determined by the world seed.
  - `CLASSIC`: Use TFC's original coordinate-based spawning system.
- **Geographic Spawn Center (Longitude/Latitude)**: When using `GEOGRAPHIC` mode, set the exact center of the area where you can spawn. The game will pick a suitable nearby location.

#### 🎯 TFC Spawn Settings

TFC's original coordinate-based spawning options (used in `CLASSIC` mode):

- **Classic Spawn Center (X/Z)**: When using `CLASSIC` mode, these TFC options define the center point for spawning.
- **Spawn Distance**: Maximum spawn radius from the spawn center. Applies to both `GEOGRAPHIC` and `CLASSIC` modes.

#### 🌿 Biome Modifications

- **Canyons Not Volcanic**: When enabled (default), removes volcanic rock and features from Canyon and Doline Canyon biomes, making them purely erosional landscapes.

#### ⛰️ TFC World Parameters

Fine-tune familiar TFC world generation values.

- **Flat Bedrock**: If enabled, the bottom of the world is a single, flat bedrock layer.
- **Finite Continents**: If enabled, the world has a limited number of continents surrounded by a vast, deep ocean (1.21.1 only).
- **Continentalness**: Controls landmass size. Lower values = more fragmented land and islands. Higher values = larger, solid continents (if continents from map are disabled).
- **Grass Density**: Affects the amount of grass coverage globally (1.20.1+).
- **Temperature Constant**: A number representing the temperature for an entire world, where -1.0 is polar and 1.0 is tropical (if climate from map is disabled, 1.20.1+).
- **Rainfall Constant**: A number representing the rainfall for an entire world, where -1.0 is arid and 1.0 is tropical (if climate from map is disabled, 1.20.1+).
- **Temperature Scale**: The distance (in blocks) between the hottest and coldest climate zones (if climate from map is disabled).
- **Rainfall Scale**: The distance (in blocks) between the wettest and driest climate zones (if climate from map is disabled).

#### ⚠️ Critical Scaling Settings

These two values are **crucial** for maintaining correct map proportions. They control how many Minecraft blocks represent the real-world data.

- **Horizontal Scale**: The radius of the world map in blocks.
- **Vertical Scale**: The height limit for terrain in blocks.

**Important:** The **ratio between Horizontal Scale and Vertical Scale must match the original map data's aspect ratio**. If these values are set to disproportionate sizes, the world will appear **stretched or squashed**. The default values are correctly calibrated.

#### 🌐 World Generation Modes

Toggle which aspects of the world are shaped by real data. Disabling a mode will revert that feature to TFC's standard procedural generation.

- **Generate Continents from Map**: Shapes landmasses, islands and oceans using the world map.
- **Generate Lakes from Map**: Places the real lakes of the world map instead of TFC's procedural lakes (needs continents from map).
- **Generate Tectonics from Map**: Relief, plate boundaries and volcanism from the tectonics map (needs continents from map).
- **Generate Volcanoes from Map**: Places hotspot volcanoes where the tectonics map has them instead of at random (needs tectonics from map).
- **Generate Rivers from Map**: Places the real rivers of the profile instead of TFC's procedural rivers (needs continents from map).
- **Generate Climate from Map**: Reads temperature, rainfall and rainfall variance from the climate maps instead of generating them procedurally.

#### 💡 Quick Tips

1. **For an authentic Earth experience**, keep all six `Generate ... from Map` options enabled.
2. Use **Geographic Spawn** to start in a specific country or near famous landmarks.
3. **Do not change `Horizontal Scale` or `Vertical Scale`** unless you understand the map's proportions and want a deliberately distorted world.

</details>

<details>
<summary><b>Advanced: Custom Map Profiles 🎨</b></summary>

This guide explains how to create custom map profiles for advanced users who want to generate worlds using their own geographic data.

#### 🏗️ Map Profile Structure

```
{namespace}/{profile_name}/
├── maps/
│   ├── continent.png
│   ├── tectonics.png
│   ├── temperature.png
│   ├── rainfall.png
│   ├── rain_variance.png
│   └── rivers.bin
├── settings.json
└── tectonics.json
```

Profiles can be placed in two locations:

- **Mod JAR resources:** `data/tfc_real_world/profiles/{namespace}/{profile_name}/`
- **External config directory:** `config/tfc_real_world/profiles/{namespace}/{profile_name}/` (or as ZIP files in this directory)

External profiles take priority over JAR profiles with the same namespace and name.

#### 🔧 Profile Settings (`settings.json`)

All fields are optional and will use default values if omitted.

- `index` (Integer, default: `2147483647`): Display order in the profile selection list. Lower values appear first.
- `lang` (Object, default: `{}`): Localized display names for the profile. Keys are language codes (e.g., `"en_us"`, `"ru_ru"`), values are display strings.
- `spawn_center_longitude` / `spawn_center_latitude` (Double, default: `12.51133` / `41.89193`): Default geographic spawn center (Rome, Italy).
- `horizontal_scale` / `vertical_scale` (Integer, default: `40000` / `20000`): The radius of the world map in blocks along X and Z. Their ratio should match your map's aspect ratio.
- `west_edge_longitude` / `east_edge_longitude` (Double, default: `-170.0` / `190.0`): Western and eastern edges of the map.
- `south_edge_latitude` / `north_edge_latitude` (Double, default: `-90.0` / `90.0`): Southern and northern edges of the map.
- `map_projection` (String, default: `"EQUAL_EARTH"`): Map projection. Currently only `"EQUAL_EARTH"` is supported.
- `waypoints` (String array, default: `[]`): Waypoints (`namespace:slug`) shown on the in-game geography map for this profile.

#### 📍 Custom Geography & Waypoints

The in-game geography map (inventory globe tab) shows the waypoints listed in the active profile's `settings.json`. Waypoint and hierarchy JSON live separately from map images:

- **JAR:** `data/tfc_real_world/geography/{namespace}/{continents|regions|subregions|waypoints}/`
- **External (folders or ZIP):** `config/tfc_real_world/geography/` — either `{namespace}/waypoints/my_place.json` or a `.zip` whose root contains the same `{namespace}/...` tree

Waypoints use `namespace:slug` IDs; continents, regions and subregions use `namespace:continent|region|subregion/slug`. External geography overrides JAR entries with the same ID. A waypoint appears on the map only if it is listed in the active profile and its JSON has `latitude` / `longitude`.

#### 🖼️ Map Images

All maps cover the same area in an equal-area projection (e.g., Equal Earth). The climate and tectonics maps share one size (e.g. 1248×624); `continent.png` may be larger for detailed coastlines (e.g. 9984×4992).

**Continent Map (`continent.png`):** Grayscale PNG with five bands:

- `0` (black) = Ocean
- `64` = Island (small landmasses: TFC island biomes)
- `128` = Fresh lake
- `192` = Salt lake
- `255` (white) = Land

**Tectonics Map (`tectonics.png` + `tectonics.json`):** Indexed (palette) or 8-bit grayscale PNG; each pixel value is a class index into `tectonics.json`, a list of classes in index order. A class never names a biome: it only describes the place, and TFC chooses the biome on its own. `continent.png` decides land versus water, so each class describes both: `land` is used on land, `water` at sea.

```json
[
  { "water": "shelf" },
  { "boundary": "convergent", "land": "mountain", "water": "trench" },
  { "land": "upland", "water": "shelf", "hotspot": 2 }
]
```

- `boundary` — active plate boundary zone: `none`, `convergent` (collision belts on land, trenches at sea), `divergent` (rift valleys on land, spreading ridges at sea) or `transform`.
- `land` — relief: `lowland`, `upland`, `highland` or `mountain`.
- `water` — seafloor: `reef`, `shelf`, `ridge`, `deep` or `trench`.
- `volcanism` — `none`, `arc` (subduction volcanoes), `rift` or `intraplate`.
- `coast` — `1` for a mountain range the sea reaches into (fjords, steep island coasts): TFC's oceanic mountains.
- `atolls` — `1` for sea where coral reefs stand: TFC builds its atolls there if the water is warm enough, and nowhere else.
- `hotspot` — `0`, or hotspot age `1` (active) … `4` (ancient) at a shield volcano centre. As in TFC, every hotspot but an ancient one raises land around it, also in the sea, so keep it away from straits you want open; an ancient one is a sunken shield in the open ocean and an ancient shield everywhere else.

Omitted fields default to `none` / `lowland` / `deep` / `none` / `0` / `0` / `0`. Every palette index used in the PNG must have a class.

**Painting the tectonics map:** The mod reads only the palette index of each pixel, never its color, so pick any colors you like:

1. Write the classes you need in `tectonics.json`; the first entry is class `0`, the next class `1`, and so on.
2. Create the image in indexed color mode (GIMP: _Image → Mode → Indexed_, Aseprite: _Indexed_ color mode, Photoshop: _Image → Mode → Indexed Color_, then _Color Table_) and give it one palette entry per class, in the same order. Color number `N` is class `N`.
3. Paint with those palette colors only, with antialiasing and color smoothing off, and save as an indexed PNG. Resizing must use nearest neighbour, or new colors appear between the classes.

A grayscale PNG works too: gray level `N` is class `N` (black is class `0`).

**Climate Maps:** 8-bit grayscale PNGs with the TFC climate of each pixel, read as is:

- `temperature.png` — annual mean temperature: `0` = −50 °C … `255` = 50 °C.
- `rainfall.png` — annual rainfall: `0` = 0 mm … `255` = 500 mm.
- `rain_variance.png` — rainfall seasonality by the local season, the same in both hemispheres: `0` = −1 (wet winter, dry summer) … `128` ≈ even rain … `255` = 1 (wet summer, dry winter).

TFC derives the climate zone from these values (its Köppen classification), so the maps alone decide deserts, tundra, monsoon forests and the rest.

**Rivers (`rivers.bin`, optional):** The river network as straight edges; TFC bends and carves them itself. A profile without the file gets TFC's procedural rivers. Several edges may leave one vertex (a delta). Big-endian binary:

- `"TFRW"`, then `int` version (`1`).
- `int` vertex count, then for each vertex `short x`, `short z`: `-32767` … `32767` from one edge of the map to the other.
- `int` edge count, then for each edge `int` source vertex, `int` drain vertex, `byte` width in blocks: the half-width of the channel (TFC's own rivers use `8` … `24`, the default profiles `6` … `40`).

A river is a chain of edges that share vertices, running from source to drain. Keep edges about 2.7 region cells long (345 blocks): TFC checks every edge near a column, so many short edges slow generation down. Rivers flow at sea level, and where an edge runs into the sea or a lake nothing is carved.

**Ridges (`ridges.bin`, optional):** The axes of the mid-ocean ridges as straight segments. TFC's ocean ridge biome raises its crest along them; a profile without the file keeps TFC's own crest, which does not follow the map. Keep the segments of one ridge in one direction, so its two sides stay the same sides along it. Big-endian binary:

- `"TFRG"`, then `int` version (`1`).
- `int` segment count, then for each segment `short x0`, `short z0`, `short x1`, `short z1`: `-32767` … `32767` from one edge of the map to the other.

#### 🏝️ Example: Creating a Simple Island Map

1. **Continent Map:** Create a 9984×4992 grayscale image at `0` (ocean) with a circular island in the center at `255` (land).
2. **Tectonics Map:** Create a 1248×624 grayscale image where `0` is the sea and coast around the island, `1` is the island interior and `2` is a single pixel at the island centre. In `tectonics.json` write `[{"water": "shelf"}, {"land": "upland", "water": "shelf"}, {"land": "upland", "water": "shelf", "hotspot": 2}]` for a dormant shield volcano.
3. **Temperature Map:** Create a 1248×624 grayscale image with a gradient from `158` (≈12 °C) at the edges to `168` (≈16 °C) at the center.
4. **Rainfall Map:** Create a 1248×624 grayscale image with a gradient from `102` (≈200 mm) at the edges to `153` (≈300 mm) at the center — together an oceanic (Cfb) climate.
5. **Rain Variance Map:** Create a 1248×624 image filled with `128` (rain all year round).
6. **Settings:** Create `settings.json` with `horizontal_scale` = `40000` and `vertical_scale` = `20000` to match the 2:1 aspect ratio of the maps. Note that due to the 2:1, a circular island in your map will appear as an oval in the generated world.

</details>

<details>
<summary><b>Roadmap 🗓️</b></summary>

1. A similar mod for vanilla Minecraft.
2. A version with a larger and more detailed world map.
3. Port the mod to TFC 1.12.2 (though this will be challenging).

</details>

---

### 🤝 Compatibility

- [TerraFirmaGreg Modern](https://www.curseforge.com/minecraft/modpacks/terrafirmagreg-modern) on Minecraft 1.20.1, since v3.1.1.
- [Auroras](https://modrinth.com/mod/auroras), [TFC Caelum](https://modrinth.com/mod/tfc-caelum) and [Firma: Civilization](https://modrinth.com/mod/firmaciv), since v2.1.2 / v3.1.2 / v4.1.2.
- [TerraFirmaEarth](https://www.curseforge.com/minecraft/mc-mods/terrafirmaearth) on Minecraft 1.20.1, since v3.1.3.

---

### 📎 Links

- [Explore GitHub](https://github.com/yazloysasha/TFC-Real-World)
- [Create issue](https://github.com/yazloysasha/TFC-Real-World/issues/new)

If you have suggestions or want to report a bug, please create an issue and I will definitely respond.

---

**Dive into the ultimate survival exploration mod for TerraFirmaCraft. Start your journey on a world that feels like home, yet is filled with endless discovery.** 🚀
