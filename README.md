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

**The core TFC experience is unchanged.** We simply use map data to tell the game _where_ to place these incredible landscapes, making every world feel coherent, vast, and ripe for exploration.

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
- **Enhanced Canyon Biomes:** Optional config to make canyon biomes purely erosional, removing volcanic features.

</details>

<details>
<summary><b>Easy In-Game Configuration 🛠️</b></summary>

All settings are accessible directly from the **TFC world creation screen** for easy customization.

- **Spawn Location Modes:**
  - `DEFAULT` – Classic TFC coordinate-based spawn.
  - `GEOGRAPHIC` – Spawn using real-world longitude and latitude! Choose your starting city or region.
  - `RANDOM` – Spawn in a random location determined by the world seed.
- **World Generation Toggles:** Easily enable/disable the use of real-world maps for Continents, Altitude, Hotspots, and Climate.
- **Parameter Adjustment:** Fine-tune familiar TFC worldgen values like continentalness, temperature scale, and rainfall directly in the GUI.
- **Automatic Setup:** Default map files are provided and auto-copied to your `config` folder for easy modification.

</details>

<details>
<summary><b>Roadmap 🗺️</b></summary>

1.  **Add New Map Profiles:** Provide alternative world map projections (e.g., Equal Earth with the Americas centered, Mercator projection) for different visual styles and gameplay feels.
2.  **Improve Climate Transitions:** Smooth the blending between different climate zones for even more natural-looking biome borders.
3.  **Backport Support:** Bring mod to older, popular TFC versions, including 1.20.1, 1.18.2, and 1.12.2 (if possible).

</details>

---

**Dive into the ultimate survival exploration mod for TerraFirmaCraft. Start your journey on a world that feels like home, yet is filled with endless discovery.** 🚀
