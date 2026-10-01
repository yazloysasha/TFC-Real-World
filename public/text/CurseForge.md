# TFC: Real World 🌍

<p align="center"><b>🎉 The Ultra-Realistic Update is here! Real rivers, lakes and islands, a detailed coastline, and a world map with 400+ waypoints and advancements since v4.2.0 on Minecraft 1.21.1</b> (older Minecraft versions will get it later).</p>

![Earth Maps](https://raw.githubusercontent.com/yazloysasha/TFC-Real-World/refs/heads/1.21.x/public/img/collage.png)

### Survive on the Real Earth

**The whole planet, block by block, with TerraFirmaCraft's survival on top.** 🌄

Sail down the Nile to its delta. Winter on the shore of Baikal. Climb from the Ganges plain into the Himalayas, or cross the Pacific from atoll to atoll until Hawaii's volcanoes rise from the sea. Every coast, river and mountain range is where you know it should be - and everything you love about TFC is still there, untouched. 🗺️

#### 🧭 What Awaits You:

- **Real Rivers & Lakes:** The Nile, the Amazon, the Volga and thousands of kilometres of other great rivers run where they really do, wide where they are mighty, and fan out into deltas at the sea. Baikal, Victoria and the Great Lakes are there too, fresh or salt as in reality. 🏞️
- **A Detailed Coastline & Real Islands:** Bays, peninsulas, fjords and inland seas drawn as on a real map, islands from Britain and Japan down to small ocean islets, and atolls where coral reefs actually grow. 🏝️
- **Real Mountains & Volcanoes:** The Himalayas, the Andes and the Alps rise where plates collide, rift valleys cut East Africa, and Hawaii, Iceland and the Ring of Fire get their volcanoes. 🏔️🌋
- **Real Climates:** From equatorial rainforests through savannas and deserts to taiga and the ice sheets of Greenland and Antarctica - each with TFC's authentic seasons. ☀️❄️
- **Start Wherever You Like:** Type in a latitude and longitude and begin your survival in your home town, at the foot of Everest or on a Pacific island. 📍
- **True to Size:** An equal-area map keeps every land its real size - Greenland is not bigger than Africa - on a world 80,000 by 40,000 blocks across, and you can make it larger or smaller. 📐
- **Conquer the Whole Planet:** 400+ real cities and settlements, from national capitals to remote island villages and polar stations, are waiting on the geography map in your inventory, each with its own advancement. Reach Paris, Cairo, Tokyo, Honolulu, Cape Town... Can you set foot on every one of them? 🧭🏆
- **Or Bring Your Own World:** Play the whole Earth, the Old World or the New World alone - or drop in your own maps and play on any planet you can draw. 🪐

**It is still TFC.** The mod only tells TerraFirmaCraft what kind of place every spot on Earth is; TFC builds the biomes, the rocks and the seasons itself. Prefer TFC's random generation for some part of the world? Continents, lakes, tectonics, volcanoes, rivers and climate each have their own switch on the world creation screen and in the config.

---

### ⚙️ Technical Details & Configuration

#### 🏞️ How It Works & Features

<div class="spoiler">
The mod works by replacing TFC's default noise generators with data sampled from customizable map images. This integrates seamlessly, letting TFC's rich procedural detail fill in the local terrain.<br>

<ul>
<li><b>Continents, Islands &amp; Lakes:</b> A world map shapes landmasses, oceans, islands down to single islets, and fresh and salt lakes.</li>
<li><b>Tectonics &amp; Relief:</b> A tectonics map tells TFC what kind of place every region is: lowland or mountain, rift or collision belt, volcanic arc, coral reef sea. TFC then picks the biome itself, so the Himalayas become collisional mountains, East Africa gets rift valleys and fjord coasts get oceanic mountains.</li>
<li><b>Volcanoes:</b> Hotspot shield volcanoes stand at their real locations, from active to ancient.</li>
<li><b>Rivers:</b> The real river network of the profile, with widths from real discharge and deltas from real delta data. TFC carves, bends and floods them as its own rivers.</li>
<li><b>Climate System:</b> Temperature, rainfall and rainfall-seasonality maps give every place its real climate zone (tropical, arid, temperate, continental, polar), which TFC's existing systems use to create biomes.</li>
<li><b>Geography Map &amp; Advancements:</b> A new inventory tab shows the world map with your position and the profile's waypoints, filtered by continent, region and subregion. Coming close to a waypoint discovers it and grants its advancement.</li>
<li><b>Non-Intrusive:</b> No new blocks, items, or mobs. Uses Mixins to feed TFC's own world generation with map data.</li>
<li><b>Enhanced Canyon Biomes:</b> Optional config to make canyon biomes purely erosional, removing volcanic features (1.21.1 only).</li>
</ul>

</div>

#### 🛠️ Configuration Guide

<div class="spoiler">
<b>Important Version Notice:</b> This configuration guide applies to <b>TFC: Real World v4.0.3+ (1.21.1), v3.0.4+ (1.20.1), and v2.0.2+ (1.18.2)</b>. If you are using an older version, I strongly recommend updating to the latest version for access to these improved configuration options. Legacy versions use a different configuration system and are no longer supported with guides.<br><br>

All configuration is accessible directly from the <b>TFC world creation screen</b> for easy adjustment. Advanced users can also modify the config files manually.<br><br>

<b>📋 Map Profiles</b><br>

<ul>
<li><b>Map Profile</b>: Select which set of map images to use for world generation (e.g., Full World, Old World). The default profile contains all necessary Earth map data.</li>
</ul>

<b>📍 Spawn Settings</b><br>
Choose where you start your adventure:<br>

<ul>
<li><b>Spawn Mode</b>:<ul>
  <li><code>GEOGRAPHIC</code>: Spawn using real-world coordinates! Set a latitude and longitude.</li>
  <li><code>RANDOM</code>: A random location determined by the world seed.</li>
  <li><code>CLASSIC</code>: Use TFC's original coordinate-based spawning system.</li>
</ul></li>
<li><b>Geographic Spawn Center (Longitude/Latitude)</b>: When using <code>GEOGRAPHIC</code> mode, set the exact center of the area where you can spawn. The game will pick a suitable nearby location.</li>
</ul>

<b>🎯 TFC Spawn Settings</b><br>
TFC's original coordinate-based spawning options (used in <code>CLASSIC</code> mode):<br>

<ul>
<li><b>Classic Spawn Center (X/Z)</b>: When using <code>CLASSIC</code> mode, these TFC options define the center point for spawning.</li>
<li><b>Spawn Distance</b>: Maximum spawn radius from the spawn center. Applies to both <code>GEOGRAPHIC</code> and <code>CLASSIC</code> modes.</li>
</ul>

<b>🌿 Biome Modifications</b><br>

<ul>
<li><b>Canyons Not Volcanic</b>: When enabled (default), removes volcanic rock and features from Canyon and Doline Canyon biomes, making them purely erosional landscapes.</li>
</ul>

<b>⛰️ TFC World Parameters</b><br>
Fine-tune familiar TFC world generation values.<br>

<ul>
<li><b>Flat Bedrock</b>: If enabled, the bottom of the world is a single, flat bedrock layer.</li>
<li><b>Finite Continents</b>: If enabled, the world has a limited number of continents surrounded by a vast, deep ocean (1.21.1 only).</li>
<li><b>Continentalness</b>: Controls landmass size. Lower values = more fragmented land and islands. Higher values = larger, solid continents (if continents from map are disabled).</li>
<li><b>Grass Density</b>: Affects the amount of grass coverage globally (1.20.1+).</li>
<li><b>Temperature Constant</b>: A number representing the temperature for an entire world, where -1.0 is polar and 1.0 is tropical (if climate from map is disabled, 1.20.1+).</li>
<li><b>Rainfall Constant</b>: A number representing the rainfall for an entire world, where -1.0 is arid and 1.0 is tropical (if climate from map is disabled, 1.20.1+).</li>
<li><b>Temperature Scale</b>: The distance (in blocks) between the hottest and coldest climate zones (if climate from map is disabled).</li>
<li><b>Rainfall Scale</b>: The distance (in blocks) between the wettest and driest climate zones (if climate from map is disabled).</li>
</ul>

<b>⚠️ Critical Scaling Settings</b><br>
These two values are <b>crucial</b> for maintaining correct map proportions. They control how many Minecraft blocks represent the real-world data.<br>

<ul>
<li><b>Horizontal Scale</b>: The radius of the world map in blocks.</li>
<li><b>Vertical Scale</b>: The height limit for terrain in blocks.</li>
</ul>

<b>Important:</b> The <b>ratio between Horizontal Scale and Vertical Scale must match the original map data's aspect ratio</b>. If these values are set to disproportionate sizes, the world will appear <b>stretched or squashed</b>. The default values are correctly calibrated.<br><br>

<b>🌐 World Generation Modes</b><br>
Toggle which aspects of the world are shaped by real data. Disabling a mode will revert that feature to TFC's standard procedural generation.<br>

<ul>
<li><b>Generate Continents from Map</b>: Shapes landmasses, islands and oceans using the world map.</li>
<li><b>Generate Lakes from Map</b>: Places the real lakes of the world map instead of TFC's procedural lakes (needs continents from map).</li>
<li><b>Generate Tectonics from Map</b>: Relief, plate boundaries and volcanism from the tectonics map (needs continents from map).</li>
<li><b>Generate Volcanoes from Map</b>: Places hotspot volcanoes where the tectonics map has them instead of at random (needs tectonics from map).</li>
<li><b>Generate Rivers from Map</b>: Places the real rivers of the profile instead of TFC's procedural rivers (needs continents from map).</li>
<li><b>Generate Climate from Map</b>: Reads temperature, rainfall and rainfall variance from the climate maps instead of generating them procedurally.</li>
</ul>

<b>💡 Quick Tips</b><br>

<ol>
<li><b>For an authentic Earth experience</b>, keep all six <code>Generate ... from Map</code> options enabled.</li>
<li>Use <b>Geographic Spawn</b> to start in a specific country or near famous landmarks.</li>
<li><b>Do not change <code>Horizontal Scale</code> or <code>Vertical Scale</code></b> unless you understand the map's proportions and want a deliberately distorted world.</li>
</ol>

</div>

#### 🎨 Advanced: Custom Map Profiles

<div class="spoiler">
This guide explains how to create custom map profiles for advanced users who want to generate worlds using their own geographic data.<br><br>

<b>🏗️ Map Profile Structure</b><br>
<code>{namespace}/{profile_name}/<br>
├── maps/<br>
│ ├── continent.png<br>
│ ├── tectonics.png<br>
│ ├── temperature.png<br>
│ ├── rainfall.png<br>
│ ├── rain_variance.png<br>
│ └── rivers.bin<br>
├── settings.json<br>
└── tectonics.json</code><br><br>

Profiles can be placed in two locations:<br>

<ul>
<li><b>Mod JAR resources:</b> <code>data/tfc_real_world/profiles/{namespace}/{profile_name}/</code></li>
<li><b>External config directory:</b> <code>config/tfc_real_world/profiles/{namespace}/{profile_name}/</code> (or as ZIP files in this directory)</li>
</ul>

External profiles take priority over JAR profiles with the same namespace and name.<br><br>

<b>🔧 Profile Settings (<code>settings.json</code>)</b><br>
All fields are optional and will use default values if omitted.<br>

<ul>
<li><code>index</code> (Integer, default: <code>2147483647</code>): Display order in the profile selection list. Lower values appear first.</li>
<li><code>lang</code> (Object, default: <code>{}</code>): Localized display names for the profile. Keys are language codes (e.g., <code>"en_us"</code>, <code>"ru_ru"</code>), values are display strings.</li>
<li><code>spawn_center_longitude</code> / <code>spawn_center_latitude</code> (Double, default: <code>12.51133</code> / <code>41.89193</code>): Default geographic spawn center (Rome, Italy).</li>
<li><code>horizontal_scale</code> / <code>vertical_scale</code> (Integer, default: <code>40000</code> / <code>20000</code>): The radius of the world map in blocks along X and Z. Their ratio should match your map's aspect ratio.</li>
<li><code>west_edge_longitude</code> / <code>east_edge_longitude</code> (Double, default: <code>-170.0</code> / <code>190.0</code>): Western and eastern edges of the map.</li>
<li><code>south_edge_latitude</code> / <code>north_edge_latitude</code> (Double, default: <code>-90.0</code> / <code>90.0</code>): Southern and northern edges of the map.</li>
<li><code>map_projection</code> (String, default: <code>"EQUAL_EARTH"</code>): Map projection. Currently only <code>"EQUAL_EARTH"</code> is supported.</li>
<li><code>waypoints</code> (String array, default: <code>[]</code>): Waypoints (<code>namespace:slug</code>) shown on the in-game geography map for this profile.</li>
</ul>

<b>📍 Custom Geography &amp; Waypoints</b><br>
The in-game geography map (inventory globe tab) shows the waypoints listed in the active profile's <code>settings.json</code>. Waypoint and hierarchy JSON live separately from map images:<br>

<ul>
<li><b>JAR:</b> <code>data/tfc_real_world/geography/{namespace}/{continents|regions|subregions|waypoints}/</code></li>
<li><b>External (folders or ZIP):</b> <code>config/tfc_real_world/geography/</code> — either <code>{namespace}/waypoints/my_place.json</code> or a <code>.zip</code> whose root contains the same <code>{namespace}/...</code> tree</li>
</ul>

Waypoints use <code>namespace:slug</code> IDs; continents, regions and subregions use <code>namespace:continent|region|subregion/slug</code>. External geography overrides JAR entries with the same ID. A waypoint appears on the map only if it is listed in the active profile and its JSON has <code>latitude</code> / <code>longitude</code>.<br><br>

<b>🖼️ Map Images</b><br>
All maps cover the same area in an equal-area projection (e.g., Equal Earth). The climate and tectonics maps share one size (e.g. 1248×624); <code>continent.png</code> may be larger for detailed coastlines (e.g. 9984×4992).<br><br>

<b>Continent Map (<code>continent.png</code>):</b> Grayscale PNG with five bands:<br>

<ul>
<li><code>0</code> (black) = Ocean</li>
<li><code>64</code> = Island (small landmasses: TFC island biomes)</li>
<li><code>128</code> = Fresh lake</li>
<li><code>192</code> = Salt lake</li>
<li><code>255</code> (white) = Land</li>
</ul>

<b>Tectonics Map (<code>tectonics.png</code> + <code>tectonics.json</code>):</b> Indexed (palette) or 8-bit grayscale PNG; each pixel value is a class index into <code>tectonics.json</code>, a list of classes in index order. A class never names a biome: it only describes the place, and TFC chooses the biome on its own. <code>continent.png</code> decides land versus water, so each class describes both: <code>land</code> is used on land, <code>water</code> at sea.<br><br>

<code>[<br>
{ "water": "shelf" },<br>
{ "boundary": "convergent", "land": "mountain", "water": "trench" },<br>
{ "land": "upland", "water": "shelf", "hotspot": 2 }<br>
]</code><br>

<ul>
<li><code>boundary</code> — active plate boundary zone: <code>none</code>, <code>convergent</code> (collision belts on land, trenches at sea), <code>divergent</code> (rift valleys on land, spreading ridges at sea) or <code>transform</code>.</li>
<li><code>land</code> — relief: <code>lowland</code>, <code>upland</code>, <code>highland</code> or <code>mountain</code>.</li>
<li><code>water</code> — seafloor: <code>reef</code>, <code>shelf</code>, <code>ridge</code>, <code>deep</code> or <code>trench</code>.</li>
<li><code>volcanism</code> — <code>none</code>, <code>arc</code> (subduction volcanoes), <code>rift</code> or <code>intraplate</code>.</li>
<li><code>coast</code> — <code>1</code> for a mountain range the sea reaches into (fjords, steep island coasts): TFC's oceanic mountains.</li>
<li><code>atolls</code> — <code>1</code> for sea where coral reefs stand: TFC builds its atolls there if the water is warm enough, and nowhere else.</li>
<li><code>hotspot</code> — <code>0</code>, or hotspot age <code>1</code> (active) … <code>4</code> (ancient) at a shield volcano centre. As in TFC, every hotspot but an ancient one raises land around it, also in the sea, so keep it away from straits you want open; an ancient one is a sunken shield in the open ocean and an ancient shield everywhere else.</li>
</ul>

Omitted fields default to <code>none</code> / <code>lowland</code> / <code>deep</code> / <code>none</code> / <code>0</code> / <code>0</code> / <code>0</code>. Every palette index used in the PNG must have a class.<br><br>

<b>Painting the tectonics map:</b> The mod reads only the palette index of each pixel, never its color, so pick any colors you like:<br>

<ol>
<li>Write the classes you need in <code>tectonics.json</code>; the first entry is class <code>0</code>, the next class <code>1</code>, and so on.</li>
<li>Create the image in indexed color mode (GIMP: <i>Image → Mode → Indexed</i>, Aseprite: <i>Indexed</i> color mode, Photoshop: <i>Image → Mode → Indexed Color</i>, then <i>Color Table</i>) and give it one palette entry per class, in the same order. Color number <code>N</code> is class <code>N</code>.</li>
<li>Paint with those palette colors only, with antialiasing and color smoothing off, and save as an indexed PNG. Resizing must use nearest neighbour, or new colors appear between the classes.</li>
</ol>

A grayscale PNG works too: gray level <code>N</code> is class <code>N</code> (black is class <code>0</code>).<br><br>

<b>Climate Maps:</b> 8-bit grayscale PNGs with the TFC climate of each pixel, read as is:<br>

<ul>
<li><code>temperature.png</code> — annual mean temperature: <code>0</code> = −25 °C … <code>255</code> = 30 °C.</li>
<li><code>rainfall.png</code> — annual rainfall: <code>0</code> = 0 mm … <code>255</code> = 500 mm.</li>
<li><code>rain_variance.png</code> — rainfall seasonality: <code>0</code> = −1 (wet January, dry July) … <code>128</code> ≈ even rain … <code>255</code> = 1 (dry January, wet July).</li>
</ul>

TFC derives the climate zone from these values (its Köppen classification), so the maps alone decide deserts, tundra, monsoon forests and the rest.<br><br>

<b>Rivers (<code>rivers.bin</code>, optional):</b> The river network as straight edges; TFC bends and carves them itself. A profile without the file gets TFC's procedural rivers. Several edges may leave one vertex (a delta). Big-endian binary:<br>

<ul>
<li><code>"TFRW"</code>, then <code>int</code> version (<code>1</code>).</li>
<li><code>int</code> vertex count, then for each vertex <code>short x</code>, <code>short z</code>: <code>-32767</code> … <code>32767</code> from one edge of the map to the other.</li>
<li><code>int</code> edge count, then for each edge <code>int</code> source vertex, <code>int</code> drain vertex, <code>byte</code> width in blocks: the half-width of the channel (TFC's own rivers use <code>8</code> … <code>24</code>, the default profiles <code>6</code> … <code>40</code>).</li>
</ul>

A river is a chain of edges that share vertices, running from source to drain. Keep edges about 2.7 region cells long (345 blocks): TFC checks every edge near a column, so many short edges slow generation down. Rivers flow at sea level, and where an edge runs into the sea or a lake nothing is carved.<br><br>

<b>🏝️ Example: Creating a Simple Island Map</b><br>

<ol>
<li><b>Continent Map:</b> Create a 9984×4992 grayscale image at <code>0</code> (ocean) with a circular island in the center at <code>255</code> (land).</li>
<li><b>Tectonics Map:</b> Create a 1248×624 grayscale image where <code>0</code> is the sea and coast around the island, <code>1</code> is the island interior and <code>2</code> is a single pixel at the island centre. In <code>tectonics.json</code> write <code>[{"water": "shelf"}, {"land": "upland", "water": "shelf"}, {"land": "upland", "water": "shelf", "hotspot": 2}]</code> for a dormant shield volcano.</li>
<li><b>Temperature Map:</b> Create a 1248×624 grayscale image with a gradient from <code>171</code> (≈12 °C) at the edges to <code>190</code> (≈16 °C) at the center.</li>
<li><b>Rainfall Map:</b> Create a 1248×624 grayscale image with a gradient from <code>102</code> (≈200 mm) at the edges to <code>153</code> (≈300 mm) at the center — together an oceanic (Cfb) climate.</li>
<li><b>Rain Variance Map:</b> Create a 1248×624 image filled with <code>128</code> (rain all year round).</li>
<li><b>Settings:</b> Create <code>settings.json</code> with <code>horizontal_scale</code> = <code>40000</code> and <code>vertical_scale</code> = <code>20000</code> to match the 2:1 aspect ratio of the maps. Note that due to the 2:1, a circular island in your map will appear as an oval in the generated world.</li>
</ol>

</div>

#### 🗓️ Roadmap

<div class="spoiler">

<ol>
<li>A similar mod for vanilla Minecraft.</li>
<li>A version with a larger and more detailed world map.</li>
<li>Port the mod to TFC 1.12.2 (though this will be challenging).</li>
</ol>

</div>

---

### 🤝 Compatibility

- [TerraFirmaGreg Modern](https://www.curseforge.com/minecraft/modpacks/terrafirmagreg-modern) on Minecraft 1.20.1, since v3.1.1.
- [Auroras](https://www.curseforge.com/minecraft/mc-mods/auroras), [TFC Caelum](https://www.curseforge.com/minecraft/mc-mods/tfc-caelum) and [Firma: Civilization](https://www.curseforge.com/minecraft/mc-mods/firmaciv), since v2.1.2 / v3.1.2 / v4.1.2.
- [TerraFirmaEarth](https://www.curseforge.com/minecraft/mc-mods/terrafirmaearth) on Minecraft 1.20.1, since v3.1.3.

---

### 📎 Links

- [Explore GitHub](https://github.com/yazloysasha/TFC-Real-World)
- [Create issue](https://github.com/yazloysasha/TFC-Real-World/issues/new)

If you have suggestions or want to report a bug, please create an issue and I will definitely respond.

---

**Dive into the ultimate survival exploration mod for TerraFirmaCraft. Start your journey on a world that feels like home, yet is filled with endless discovery.** 🚀
