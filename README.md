# TFC: Real World 🌍

![Earth Maps](public/img/collage.png)

### Explore a World You Know

**TerraFirmaCraft** meets **real-world geography**! 🌄

Ever dreamed of surviving and thriving in a world that feels truly familiar? A world where vast oceans separate iconic continents, majestic mountain ranges scrape the sky, and sweeping deserts give way to frozen tundras—all governed by TFC's deep and authentic survival systems?

**TFC: Real World** makes that dream a reality. ✨

This mod reshapes your TFC world using the very layout of our own planet. Experience the ultimate exploration adventure on a grand, believable scale, where every journey feels like charting undiscovered territory on Earth itself—complete with TerraFirmaCraft's signature realism and progression. ⛏️🌱

---

### What This Mod Does

**TFC: Real World** transforms the foundation of your world—not the gameplay. All the beloved TFC mechanics, resources, and challenges remain perfectly intact. What changes is the _stage_ on which you play. 🗺️

Instead of random generation, the world's continents, mountains, oceans, and climate zones are guided by real-world data. This creates a uniquely immersive and logical geography for your survival saga.

#### 🧭 Key Experiences & New Rules:

- **Sail Across Familiar Waters:** Navigate vast oceans and coastlines that mirror Earth's great seas. ⛵
- **Conquer Legendary Peaks:** Trek through towering mountain ranges and descend into deep oceanic trenches. 🏔️
- **Traverse Global Climates:** Journey from lush equatorial rainforests, through arid deserts and vast grasslands, into temperate woodlands, all the way to the frozen poles—each with TFC's authentic seasonal effects. ☀️❄️
- **Discover Logical Landscapes:** Find volcanoes where tectonic forces would place them, and experience climate transitions that make geographical sense. 🌋

**The core TFC experience is unchanged.** I simply use map data to tell the game _where_ to place these incredible landscapes, making every world feel coherent, vast, and ripe for exploration.

---

### ⚙️ Technical Details & Configuration

<details>
<summary><b>How It Works & Features 🏞️</b></summary>

The mod works by replacing TFC's default noise generators with data sampled from customizable map images. This integrates seamlessly, letting TFC's rich procedural detail fill in the local terrain.

- **Continent & Ocean Layout:** Shaped by a world map image, creating Earth-like landmasses.
- **Elevation & Depth:** Real altitude data creates realistic mountains, plains, and ocean floors.
- **Volcanic Activity:** Hotspot maps guide the placement of TFC's volcanoes to tectonically plausible areas.
- **Climate System:** A Köppen climate map defines temperature and rainfall belts (tropical, arid, temperate, continental, polar), which TFC's existing systems use to create biomes.
- **Non-Intrusive:** No new blocks, items, or mobs. Uses Mixins to only redirect worldgen rules.
- **Enhanced Canyon Biomes:** Optional config to make canyon biomes purely erosional, removing volcanic features (1.21.1 only).

</details>

<details>
<summary><b>Configuration Guide 🛠️</b></summary>

**Important Version Notice:** This configuration guide applies to **TFC: Real World v4.0.2+ (1.21.1), v3.0.3+ (1.20.1), and v2.0.1+ (1.18.2)**. If you are using an older version, I strongly recommend updating to the latest version for access to these improved configuration options. Legacy versions use a different configuration system and are no longer supported with guides.

All configuration is accessible directly from the **TFC world creation screen** for easy adjustment. Advanced users can also modify the config files manually.

#### 📁 Map Profiles

- **Map Profile**: Select which set of map images to use for world generation (e.g., Full World, Old World). The default profile contains all necessary Earth map data.

#### 📍 Spawn Settings

Choose where you start your adventure:

- **Spawn Mode**:
  - `GEOGRAPHIC`: Spawn using real-world coordinates! Set a latitude and longitude.
  - `RANDOM`: A random location determined by the world seed.
  - `CLASSIC`: Use TFC's original coordinate-based spawning system.
- **Geographic Spawn Center (Longitude/Latitude)**: When using `GEOGRAPHIC` mode, set the exact center of the area where you can spawn. The game will pick a suitable nearby location.
- **Classic Spawn Center (X/Z)**: When using `CLASSIC` mode, these TFC options define the center point for spawning.
- **Spawn Distance**: Maximum spawn radius from the spawn center. Applies to both `GEOGRAPHIC` and `CLASSIC` modes.

#### 🌍 World Generation Modes

Toggle which aspects of the world are shaped by real data. Disabling a mode will revert that feature to TFC's standard procedural generation.

- **Generate Continents from Map**: Shapes landmasses and oceans using the world map.
- **Generate Altitude from Map**: Creates realistic mountains, hills, plains, and ocean depth.
- **Generate Hotspots from Map**: Places TFC's volcanoes in tectonically plausible areas.
- **Generate Climate from Map**: Uses the Köppen climate map to create logical temperature and rainfall belts (tropical, arid, temperate, etc.).

#### ⚠️ Critical Scaling Settings

These two values are **crucial** for maintaining correct map proportions. They control how many Minecraft blocks represent the real-world data.

- **Horizontal Scale**: The radius of the world map in blocks.
- **Vertical Scale**: The height limit for terrain in blocks.

**Important:** The **ratio between Horizontal Scale and Vertical Scale must match the original map data's aspect ratio**. If these values are set to disproportionate sizes, the world will appear **stretched or squashed**. The default values are correctly calibrated.

#### 🏔️ TFC World Parameters

Fine-tune familiar TFC world generation values.

- **Flat Bedrock**: If enabled, the bottom of the world is a single, flat bedrock layer.
- **Finite Continents**: If enabled, the world has a limited number of continents surrounded by a vast, deep ocean (1.21.1 only).
- **Continentalness**: Controls landmass size. Lower values = more fragmented land and islands. Higher values = larger, solid continents (if continents and altitude from map is disabled).
- **Grass Density**: Affects the amount of grass coverage globally (1.20.1+).
- **Temperature Constant**: A number representing the temperature for an entire world, where -1.0 is polar and 1.0 is tropical (if climate from map is disabled, 1.20.1+).
- **Rainfall Constant**: A number representing the rainfall for an entire world, where -1.0 is arid and 1.0 is tropical (if climate from map is disabled, 1.20.1+).
- **Temperature Scale**: The distance (in blocks) between the hottest and coldest climate zones (if climate from map is disabled).
- **Rainfall Scale**: The distance (in blocks) between the wettest and driest climate zones (if climate from map is disabled).

#### 🏞️ Biome Modifications

- **Canyons Not Volcanic**: When enabled (default), removes volcanic rock and features from Canyon and Doline Canyon biomes, making them purely erosional landscapes.

#### 💡 Quick Tips

1. **For an authentic Earth experience**, keep all four `Generate ... from Map` options enabled.
2. Use **Geographic Spawn** to start in a specific country or near famous landmarks.
3. **Do not change `Horizontal Scale` or `Vertical Scale`** unless you understand the map's proportions and want a deliberately distorted world.

</details>

<details>
<summary><b>Roadmap 🗺️</b></summary>

1. **Vanilla Minecraft Version:** A similar mod for vanilla Minecraft.
2. **High-Resolution World Map:** A version with a larger and more detailed world map.
3. **Backport to 1.12.2:** Port the mod to TFC 1.12.2 (though this will be challenging).

</details>

---

**Dive into the ultimate survival exploration mod for TerraFirmaCraft. Start your journey on a world that feels like home, yet is filled with endless discovery.** 🚀
