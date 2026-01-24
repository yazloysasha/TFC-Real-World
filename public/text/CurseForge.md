# TFC: Real World 🌍

![Earth Maps](https://raw.githubusercontent.com/yazloysasha/TFC-Real-World/refs/heads/1.21.x/public/img/collage.png)

### Explore a World You Know

**TerraFirmaCraft** meets **real-world geography**! 🌄

Ever dreamed of surviving and thriving in a world that feels truly familiar? A world where vast oceans separate iconic continents, majestic mountain ranges scrape the sky, and sweeping deserts give way to frozen tundras - all governed by TFC's deep and authentic survival systems?

**TFC: Real World** makes that dream a reality. ✨

This mod reshapes your TFC world using the very layout of our own planet. Experience the ultimate exploration adventure on a grand, believable scale, where every journey feels like charting undiscovered territory on Earth itself - complete with TerraFirmaCraft's signature realism and progression. ⛏️🌱

---

### What This Mod Does

**TFC: Real World** transforms the foundation of your world - not the gameplay. All the beloved TFC mechanics, resources, and challenges remain perfectly intact. What changes is the _stage_ on which you play. 🗺️

Instead of random generation, the world's continents, mountains, oceans, and climate zones are guided by real-world data. This creates a uniquely immersive and logical geography for your survival saga.

#### 🧭 Key Experiences & New Rules:

- **Sail Across Familiar Waters:** Navigate vast oceans and coastlines that mirror Earth's great seas. ⛵
- **Conquer Legendary Peaks:** Trek through towering mountain ranges and descend into deep oceanic trenches. 🏔️
- **Traverse Global Climates:** Journey from lush equatorial rainforests, through arid deserts and vast grasslands, into temperate woodlands, all the way to the frozen poles - each with TFC's authentic seasonal effects. ☀️❄️
- **Discover Logical Landscapes:** Find volcanoes where tectonic forces would place them, and experience climate transitions that make geographical sense. 🌋

**The core TFC experience is unchanged.** I simply use map data to tell the game _where_ to place these incredible landscapes, making every world feel coherent, vast, and ripe for exploration.

---

### ⚙️ Technical Details & Configuration

#### 🏞️ How It Works & Features

<div class="spoiler">
The mod works by replacing TFC's default noise generators with data sampled from customizable map images. This integrates seamlessly, letting TFC's rich procedural detail fill in the local terrain.<br><br>

<ul>
<li><b>Continent & Ocean Layout:</b> Shaped by a world map image, creating Earth-like landmasses.</li>
<li><b>Elevation & Depth:</b> Real altitude data creates realistic mountains, plains, and ocean floors.</li>
<li><b>Volcanic Activity:</b> Hotspot maps guide the placement of TFC's volcanoes to tectonically plausible areas.</li>
<li><b>Climate System:</b> A Köppen climate map defines temperature and rainfall belts (tropical, arid, temperate, continental, polar), which TFC's existing systems use to create biomes.</li>
<li><b>Non-Intrusive:</b> No new blocks, items, or mobs. Uses Mixins to only redirect worldgen rules.</li>
<li><b>Enhanced Canyon Biomes:</b> Optional config to make canyon biomes purely erosional, removing volcanic features (1.21.1 only).</li>
</ul>

</div>

#### 🛠️ Configuration Guide

<div class="spoiler">
<b>Important Version Notice:</b> This configuration guide applies to <b>TFC: Real World v4.0.2+ (1.21.1), v3.0.3+ (1.20.1), and v2.0.1+ (1.18.2)</b>. If you are using an older version, I strongly recommend updating to the latest version for access to these improved configuration options. Legacy versions use a different configuration system and are no longer supported with guides.<br><br>

All configuration is accessible directly from the <b>TFC world creation screen</b> for easy adjustment. Advanced users can also modify the config files manually.<br><br>

<b>📋 Map Profiles</b><br>

<ul>
<li><b>Map Profile:</b> Select which set of map images to use for world generation (e.g., Full World, Old World). The default profile contains all necessary Earth map data.</li>
</ul>

<b>📍 Spawn Settings</b><br>
Choose where you start your adventure:<br>

<ul>
<li><b>Spawn Mode:</b><ul>
  <li><code>GEOGRAPHIC</code> – Spawn using real-world coordinates! Set a latitude and longitude.</li>
  <li><code>RANDOM</code> – A random location determined by the world seed.</li>
  <li><code>CLASSIC</code> – Use TFC's original coordinate-based spawning system.</li>
</ul></li>
<li><b>Geographic Spawn Center (Longitude/Latitude):</b> When using <code>GEOGRAPHIC</code> mode, set the exact center of the area where you can spawn. The game will pick a suitable nearby location.</li>
</ul>

<b>🎯 TFC Spawn Settings</b><br>
TFC's original coordinate-based spawning options (used in <code>CLASSIC</code> mode):<br>

<ul>
<li><b>Classic Spawn Center (X/Z):</b> When using <code>CLASSIC</code> mode, these TFC options define the center point for spawning.</li>
<li><b>Spawn Distance:</b> Maximum spawn radius from the spawn center. Applies to both <code>GEOGRAPHIC</code> and <code>CLASSIC</code> modes.</li>
</ul>

<b>🌿 Biome Modifications</b><br>

<ul>
<li><b>Canyons Not Volcanic:</b> When enabled (default), removes volcanic rock and features from Canyon and Doline Canyon biomes, making them purely erosional landscapes.</li>
</ul>

<b>⛰️ TFC World Parameters</b><br>
Fine-tune familiar TFC world generation values.<br>

<ul>
<li><b>Flat Bedrock:</b> If enabled, the bottom of the world is a single, flat bedrock layer.</li>
<li><b>Finite Continents:</b> If enabled, the world has a limited number of continents surrounded by a vast, deep ocean (1.21.1 only).</li>
<li><b>Continentalness:</b> Controls landmass size. Lower values = more fragmented land and islands. Higher values = larger, solid continents (if continents and altitude from map is disabled).</li>
<li><b>Grass Density:</b> Affects the amount of grass coverage globally (1.20.1+).</li>
<li><b>Temperature Constant:</b> A number representing the temperature for an entire world, where -1.0 is polar and 1.0 is tropical (if climate from map is disabled, 1.20.1+).</li>
<li><b>Rainfall Constant:</b> A number representing the rainfall for an entire world, where -1.0 is arid and 1.0 is tropical (if climate from map is disabled, 1.20.1+).</li>
<li><b>Temperature Scale:</b> The distance (in blocks) between the hottest and coldest climate zones (if climate from map is disabled).</li>
<li><b>Rainfall Scale:</b> The distance (in blocks) between the wettest and driest climate zones (if climate from map is disabled).</li>
</ul>

<b>⚠️ Critical Scaling Settings</b><br>
These two values are <b>crucial</b> for maintaining correct map proportions. They control how many Minecraft blocks represent the real-world data.<br>

<ul>
<li><b>Horizontal Scale:</b> The radius of the world map in blocks.</li>
<li><b>Vertical Scale:</b> The height limit for terrain in blocks.</li>
</ul>

<b>Important:</b> The <b>ratio between Horizontal Scale and Vertical Scale must match the original map data's aspect ratio</b>. If these values are set to disproportionate sizes, the world will appear <b>stretched or squashed</b>. The default values are correctly calibrated.<br><br>

<b>🌐 World Generation Modes</b><br>
Toggle which aspects of the world are shaped by real data. Disabling a mode will revert that feature to TFC's standard procedural generation.<br>

<ul>
<li><b>Generate Continents from Map:</b> Shapes landmasses and oceans using the world map.</li>
<li><b>Generate Altitude from Map:</b> Creates realistic mountains, hills, plains, and ocean depth.</li>
<li><b>Generate Hotspots from Map:</b> Places TFC's volcanoes in tectonically plausible areas.</li>
<li><b>Generate Climate from Map:</b> Uses the Köppen climate map to create logical temperature and rainfall belts (tropical, arid, temperate, etc.).</li>
</ul>

<b>💡 Quick Tips</b><br>

<ol>
<li>For an authentic Earth experience, keep all four <code>Generate ... from Map</code> options enabled.</li>
<li>Use <b>Geographic Spawn</b> to start in a specific country or near famous landmarks.</li>
<li><b>Do not change <code>Horizontal Scale</code> or <code>Vertical Scale</code></b> unless you understand the map's proportions and want a deliberately distorted world.</li>
</ol>

</div>

#### 🎨 Advanced: Custom Map Profiles

<div class="spoiler">
This guide explains how to create custom map profiles for advanced users who want to generate worlds using their own geographic data.<br><br>

<b>🏗️ Map Profile Structure</b><br>
Map profiles organize all the map images needed for world generation. Each profile must have a <code>maps/</code> directory containing the required map files:<br><br>

<code>{namespace}/{profile_name}/<br>
├─ maps/<br>
│ ├── continent.png<br>
│ ├── altitude.png<br>
│ ├── hotspots.png<br>
│ ├── koppen.png<br>
│ ├── temperature.png<br>
│ └── rainfall.png<br>
└── settings.json</code><br><br>

Profiles can be placed in two locations:<br>

<ul>
<li><b>Mod JAR resources:</b> <code>data/tfc_real_world/profiles/{namespace}/{profile_name}/</code></li>
<li><b>External config directory:</b> <code>config/tfc_real_world/profiles/{namespace}/{profile_name}/</code> (or as ZIP files in this directory)</li>
</ul>

External profiles take priority over JAR profiles with the same namespace and name.<br><br>

<b>🔧 Profile Settings (<code>settings.json</code>)</b><br>
Each map profile requires a <code>settings.json</code> file that defines the profile's configuration. All fields are optional and will use default values if omitted.<br><br>

<b>Display Settings:</b><br>

<ul>
<li><b>Index</b> (Integer, default: <code>2147483647</code>): Display order in the profile selection list. Lower values appear first.</li>
<li><b>Lang</b> (Object, default: <code>{}</code>): Localized display names for the profile. Keys are language codes (e.g., <code>"en_us"</code>, <code>"ru_ru"</code>), values are display strings.</li>
</ul>

<b>Spawn Settings:</b><br>

<ul>
<li><b>Spawn Center Longitude</b> (Double, default: <code>12.4964</code>): Geographic longitude for the default spawn center (Rome, Italy).</li>
<li><b>Spawn Center Latitude</b> (Double, default: <code>41.9028</code>): Geographic latitude for the default spawn center (Rome, Italy).</li>
</ul>

<b>Scaling Settings:</b><br>

<ul>
<li><b>Horizontal Scale</b> (Integer, default: <code>40000</code>): The radius of the world map in blocks.</li>
<li><b>Vertical Scale</b> (Integer, default: <code>20000</code>): The height limit for terrain in blocks.</li>
</ul>

<b>Important:</b> The ratio between <code>horizontal_scale</code> and <code>vertical_scale</code> should match your map's aspect ratio to avoid stretching or squashing.<br><br>

<b>Map Boundaries:</b><br>

<ul>
<li><b>West Edge Longitude</b> (Double, default: <code>-170.0</code>): Western edge of the map in degrees longitude.</li>
<li><b>East Edge Longitude</b> (Double, default: <code>190.0</code>): Eastern edge of the map in degrees longitude.</li>
<li><b>South Edge Latitude</b> (Double, default: <code>-90.0</code>): Southern edge of the map in degrees latitude.</li>
<li><b>North Edge Latitude</b> (Double, default: <code>90.0</code>): Northern edge of the map in degrees latitude.</li>
</ul>

<b>Projection:</b><br>

<ul>
<li><b>Map Projection</b> (String, default: <code>"EQUAL_EARTH"</code>): Map projection type. Currently only <code>"EQUAL_EARTH"</code> is supported.</li>
</ul>

<b>🖼️ Required Map Images</b><br>
All maps must be PNG format and have identical dimensions. Maps should use an equal-area projection (e.g., Equal Earth) to maintain proper proportions.<br><br>

<b>Continent Map (<code>continent.png</code>):</b> Defines landmass distribution and continental boundaries. Format: Grayscale PNG. Legend: <code>0</code> (black) = Ocean, <code>255</code> (white) = Land. This map shapes the basic layout of continents and oceans in your world.<br><br>

<b>Altitude Map (<code>altitude.png</code>):</b> Defines terrain elevation and ocean depth. Format: Grayscale PNG. Legend: <code>0-127</code> = Ocean depth (darker = deeper), <code>128-255</code> = Land elevation (brighter = higher). Creates realistic mountains, hills, plains, and ocean floors. Example values: <code>0</code> = Deepest ocean, <code>64</code> = Shallow ocean, <code>128</code> = Sea level (coastline), <code>192</code> = Hills, <code>255</code> = Highest mountains.<br><br>

<b>Hotspots Map (<code>hotspots.png</code>):</b> Defines volcanic hotspot locations and ages. Format: Grayscale PNG. Legend: <code>0</code> = No hotspot (age 0), <code>64</code> = Age 4 (oldest), <code>128</code> = Age 3, <code>192</code> = Age 2, <code>255</code> = Age 1 (youngest). Places TFC volcanoes in tectonically plausible areas.<br><br>

<b>Köppen Climate Map (<code>koppen.png</code>):</b> Defines climate zones using the Köppen climate classification system. Format: RGB Color PNG. Each climate type has a specific RGB color that must match exactly.<br><br>

Climate types and their RGB colors:<br>

<ul>
<li><b>AF</b> (Humid Tropical): <code>(0, 0, 220)</code></li>
<li><b>AS</b> (Tropical Dry/Wet): <code>(0, 100, 240)</code></li>
<li><b>AW</b> (Tropical Wet/Dry): <code>(0, 150, 220)</code></li>
<li><b>AM</b> (Tropical Monsoon): <code>(40, 80, 200)</code></li>
<li><b>BWH</b> (Hot Desert): <code>(210, 0, 0)</code></li>
<li><b>BSH</b> (Hot Semi-Arid): <code>(210, 120, 0)</code></li>
<li><b>BWK</b> (Cold Desert): <code>(200, 80, 80)</code></li>
<li><b>BSK</b> (Cold Semi-Arid): <code>(200, 120, 60)</code></li>
<li><b>CSA</b> (Coastal Subtropical): <code>(250, 250, 0)</code></li>
<li><b>CSB</b> (Coastal): <code>(180, 180, 0)</code></li>
<li><b>CSC</b> (Cold Coastal): <code>(120, 120, 0)</code></li>
<li><b>CWA</b> (Monsoonal Subtropical): <code>(100, 240, 130)</code></li>
<li><b>CWB</b> (Monsoonal Temperate): <code>(80, 210, 120)</code></li>
<li><b>CWC</b> (Cold Monsoonal Temperate): <code>(70, 160, 110)</code></li>
<li><b>CFA</b> (Oceanic Subtropical): <code>(170, 240, 90)</code></li>
<li><b>CFB</b> (Oceanic): <code>(140, 200, 80)</code></li>
<li><b>CFC</b> (Cold Oceanic): <code>(110, 170, 70)</code></li>
<li><b>DSA</b> (Coastal Continental): <code>(190, 20, 190)</code></li>
<li><b>DSB</b> (Cold Coastal Continental): <code>(160, 20, 180)</code></li>
<li><b>DSC</b> (Coastal Subarctic): <code>(130, 20, 170)</code></li>
<li><b>DSD</b> (Coastal Cold Subarctic): <code>(100, 20, 160)</code></li>
<li><b>DFA</b> (Continental): <code>(40, 190, 190)</code></li>
<li><b>DFB</b> (Cold Continental): <code>(30, 170, 170)</code></li>
<li><b>DFC</b> (Subarctic): <code>(20, 150, 140)</code></li>
<li><b>DFD</b> (Cold Subarctic): <code>(10, 130, 110)</code></li>
<li><b>DWA</b> (Monsoonal Continental): <code>(80, 80, 220)</code></li>
<li><b>DWB</b> (Cold Monsoonal Continental): <code>(70, 70, 190)</code></li>
<li><b>DWC</b> (Monsoonal Subarctic): <code>(60, 60, 160)</code></li>
<li><b>DWD</b> (Cold Monsoonal Subarctic): <code>(60, 60, 130)</code></li>
<li><b>ET</b> (Tundra): <code>(190, 190, 190)</code></li>
<li><b>EF</b> (Polar): <code>(80, 80, 80)</code></li>
</ul>

<b>Temperature Map (<code>temperature.png</code>):</b> Provides temperature data used in conjunction with the Köppen map. Format: Grayscale PNG. Legend: <code>0</code> (black) = Coldest, <code>255</code> (white) = Hottest. Each Köppen climate zone interprets grayscale values within its own temperature range, so higher brightness indicates warmer temperatures for that specific climate type.<br><br>

<b>Rainfall Map (<code>rainfall.png</code>):</b> Provides rainfall data used in conjunction with the Köppen map. Format: Grayscale PNG. Legend: <code>0</code> (black) = Driest, <code>255</code> (white) = Wettest. Each Köppen climate zone interprets grayscale values within its own rainfall range, so higher brightness indicates more rainfall for that specific climate type.<br><br>

<b>✅ Best Practices</b><br>

<ol>
<li><b>Consistency:</b> Ensure all maps align properly - continents should match altitude, climate should match temperature/rainfall patterns.</li>
<li><b>Smooth Transitions:</b> Use gradual gradients rather than sharp boundaries to avoid visual artifacts in the generated world.</li>
<li><b>Sea Level:</b> In the altitude map, keep the sea level boundary (128) consistent with your continent map - ocean areas should have values below 128.</li>
<li><b>Color Accuracy:</b> For the Köppen map, use the exact RGB values provided. Even small deviations will cause the mod to use the nearest matching climate type.</li>
<li><b>Testing:</b> Test your maps with a small world first to verify proportions and alignment before creating large-scale maps.</li>
<li><b>Map Dimensions:</b> All maps in a profile must have identical width and height. Use an equal-area projection to maintain proper proportions across the entire map.</li>
</ol>

<b>🏝️ Example: Creating a Simple Island Map</b><br>
Here's a minimal example for creating a basic island map profile:<br><br>

<ol>
<li><b>Continent Map:</b> Create a 1280x640 grayscale image with most of the map at <code>0</code> (ocean) and a circular island in the center at <code>255</code> (land).</li>
<li><b>Altitude Map:</b> Create a matching 1280x640 grayscale image with ocean areas at <code>64</code> (shallow ocean), island edges at <code>128</code> (sea level), and island center at <code>200</code> (hills).</li>
<li><b>Hotspots Map:</b> Create a 1280x640 grayscale image with most areas at <code>0</code> (no volcanoes) and a small hotspot on the island at <code>192</code> (age 2).</li>
<li><b>Köppen Map:</b> Create a 1280x640 RGB image using <code>(140, 200, 80)</code> for CFB (Oceanic climate).</li>
<li><b>Temperature Map:</b> Create a 1280x640 grayscale image with a gradient from <code>120</code> (cooler) at the edges to <code>180</code> (warmer) at the center, representing temperature variation across the island.</li>
<li><b>Rainfall Map:</b> Create a 1280x640 grayscale image with a gradient from <code>140</code> (drier) at the edges to <code>200</code> (wetter) at the center, representing rainfall variation across the island.</li>
<li><b>Settings:</b> Create <code>settings.json</code> with <code>horizontal_scale</code> = <code>40000</code> and <code>vertical_scale</code> = <code>20000</code> to match the 2:1 aspect ratio of the maps. Note that due to the 2:1, a circular island in your map will appear as an oval in the generated world.</li>
</ol>

All six maps must be exactly 1280x640 pixels and saved as PNG files in the profile's <code>maps/</code> directory.

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

**Dive into the ultimate survival exploration mod for TerraFirmaCraft. Start your journey on a world that feels like home, yet is filled with endless discovery.** 🚀
