# TFC: Real World 🌍

![Earth Maps](https://raw.githubusercontent.com/yazloysasha/TFC-Real-World/refs/heads/1.21.x/public/img/collage.png)

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

#### 🏞️ How It Works & Features

<div class="spoiler">
The mod works by replacing TFC's default noise generators with data sampled from customizable map images. This integrates seamlessly, letting TFC's rich procedural detail fill in the local terrain.<br><br>

<ul>
<li><b>Continent & Ocean Layout:</b> Shaped by a world map image, creating Earth-like landmasses.</li>
<li><b>Elevation & Depth:</b> Real altitude data creates realistic mountains, plains, and ocean floors.</li>
<li><b>Volcanic Activity:</b> Hotspot maps guide the placement of TFC's volcanoes to tectonically plausible areas.</li>
<li><b>Climate System:</b> A Köppen climate map defines temperature and rainfall belts (tropical, arid, temperate, continental, polar), which TFC's existing systems use to create biomes.</li>
<li><b>Non-Intrusive:</b> No new blocks, items, or mobs. Uses Mixins to only redirect worldgen rules.</li>
<li><b>Enhanced Canyon Biomes:</b> Optional config to make canyon biomes purely erosional, removing volcanic features.</li>
</ul>

</div>

#### 🛠️ Easy In-Game Configuration

<div class="spoiler">
All settings are accessible directly from the <b>TFC world creation screen</b> for easy customization.<br><br>

<ul>
<li><b>Spawn Location Modes:</b><ul>
  <li><code>DEFAULT</code> – Classic TFC coordinate-based spawn.</li>
  <li><code>GEOGRAPHIC</code> – Spawn using real-world longitude and latitude! Choose your starting city or region.</li>
  <li><code>RANDOM</code> – Spawn in a random location determined by the world seed.</li>
</ul></li>
<li><b>World Generation Toggles:</b> Easily enable/disable the use of real-world maps for Continents, Altitude, Hotspots, and Climate.</li>
<li><b>Parameter Adjustment:</b> Fine-tune familiar TFC worldgen values like continentalness, temperature scale, and rainfall directly in the GUI.</li>
<li><b>Automatic Setup:</b> Default map files are provided and auto-copied to your <code>config</code> folder for easy modification.</li>
</ul>

</div>

#### 🗺️ Roadmap

<div class="spoiler">
<ol>
<li><b>Backport Support:</b> Bring mod to older, popular TFC versions, including 1.20.1, 1.18.2, and 1.12.2 (if possible).</li>
<li><b>Improve Climate Transitions:</b> Smooth the blending between different climate zones for even more natural-looking biome borders.</li>
</ol>

</div>

---

**Dive into the ultimate survival exploration mod for TerraFirmaCraft. Start your journey on a world that feels like home, yet is filled with endless discovery.** 🚀
