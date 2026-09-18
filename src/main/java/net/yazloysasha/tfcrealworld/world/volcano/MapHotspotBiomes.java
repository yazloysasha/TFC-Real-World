package net.yazloysasha.tfcrealworld.world.volcano;

import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.region.Region;
import net.yazloysasha.tfcrealworld.world.volcano.MapHotspotLayout.MountainStyle;
import org.jetbrains.annotations.Nullable;

/**
 * Shared hotspot rules aligned with TFC 4.2.10 {@code AddHotspots} and
 * {@code ChooseBiomes}. Ancient (age 4) cells never become land, so ocean
 * straits stay open; TFC 3 has no sunken shield layer and keeps the ocean biome.
 */
public final class MapHotspotBiomes {

  /** Oldest hotspot age on the map PNG (TFC {@code hotSpotAge == 4}). */
  public static final byte ANCIENT_AGE = 4;

  private MapHotspotBiomes() {}

  /** {@code AddHotspots}: {@code if (age != 4) point.setLand()}. */
  public static boolean shouldSetLandForMapAge(byte age) {
    return age > 0 && age != ANCIENT_AGE;
  }

  /**
   * Ancient hotspots must not paint land volcanics on ocean — TFG uses
   * {@code SUNKEN_SHIELD_VOLCANO} instead; TFC 3 keeps the ocean biome.
   */
  public static boolean keepOceanBiomeForAncient(byte age, Region.Point point) {
    return age == ANCIENT_AGE && !point.land() && !point.island();
  }

  public static int volcanicMountainFor(
    Region.Point point,
    int volcanicMountains,
    int volcanicOceanicMountains
  ) {
    return point.coastalMountain()
      ? volcanicOceanicMountains
      : volcanicMountains;
  }

  public static int assignTfc(
    Region.Point point,
    int proposedBiome,
    boolean proposedIsVolcanic,
    int gridX,
    int gridZ,
    @Nullable MapHotspotLayout layout
  ) {
    if (point.lake()) {
      return proposedBiome;
    }

    final byte age = layout == null ? 0 : layout.ageAtGrid(gridX, gridZ);
    if (keepOceanBiomeForAncient(age, point)) {
      return proposedBiome;
    }

    if (point.mountain()) {
      if (
        layout != null &&
        layout.styleAtGrid(gridX, gridZ) == MountainStyle.NATURAL_MOUNTAIN
      ) {
        return proposedBiome;
      }
      return volcanicMountainFor(
        point,
        TFCLayers.VOLCANIC_MOUNTAINS,
        TFCLayers.VOLCANIC_OCEANIC_MOUNTAINS
      );
    }

    if (!point.land()) {
      return TFCLayers.VOLCANIC_OCEANIC_MOUNTAINS;
    }

    if (proposedIsVolcanic && !isTfcVolcanicMountain(proposedBiome)) {
      return proposedBiome;
    }

    return TFCLayers.CANYONS;
  }

  public static boolean isTfcVolcanicMountain(int biome) {
    return (
      biome == TFCLayers.VOLCANIC_MOUNTAINS ||
      biome == TFCLayers.VOLCANIC_OCEANIC_MOUNTAINS ||
      biome == TFCLayers.VOLCANIC_MOUNTAIN_LAKE ||
      biome == TFCLayers.VOLCANIC_OCEANIC_MOUNTAIN_LAKE
    );
  }

  public static boolean isTfcVolcanoBiome(int biome) {
    return (biome == TFCLayers.CANYONS || isTfcVolcanicMountain(biome));
  }

  public static boolean isTfcLakeBiome(int biome) {
    return (
      biome == TFCLayers.LAKE ||
      biome == TFCLayers.MOUNTAIN_LAKE ||
      biome == TFCLayers.OLD_MOUNTAIN_LAKE ||
      biome == TFCLayers.OCEANIC_MOUNTAIN_LAKE ||
      biome == TFCLayers.VOLCANIC_MOUNTAIN_LAKE ||
      biome == TFCLayers.VOLCANIC_OCEANIC_MOUNTAIN_LAKE ||
      biome == TFCLayers.PLATEAU_LAKE
    );
  }
}
