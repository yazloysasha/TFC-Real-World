package net.yazloysasha.tfcrealworld.world.volcano;

import static com.newterraearth.tfe.world.NTELayerIds.ACTIVE_SHIELD_VOLCANO;
import static com.newterraearth.tfe.world.NTELayerIds.ANCIENT_SHIELD_VOLCANO;
import static com.newterraearth.tfe.world.NTELayerIds.DORMANT_SHIELD_VOLCANO;
import static com.newterraearth.tfe.world.NTELayerIds.EXTINCT_SHIELD_VOLCANO;
import static com.newterraearth.tfe.world.NTELayerIds.GLACIATED_SHIELD_VOLCANO;
import static com.newterraearth.tfe.world.NTELayerIds.ICE_SHEET_SHIELD_VOLCANO;
import static com.newterraearth.tfe.world.NTELayerIds.SUNKEN_SHIELD_VOLCANO;
import static net.dries007.tfc.world.layer.TFCLayers.DEEP_OCEAN;
import static net.dries007.tfc.world.layer.TFCLayers.DEEP_OCEAN_TRENCH;
import static net.dries007.tfc.world.layer.TFCLayers.OCEAN;
import static net.dries007.tfc.world.layer.TFCLayers.OCEAN_REEF;

import net.dries007.tfc.world.region.Region;
import net.yazloysasha.tfcrealworld.world.backport.TfeKarstBiomeInvoke;

public final class TfeShieldHotspotBiomes {

  private TfeShieldHotspotBiomes() {}

  public static int assign(
    Region.Point point,
    byte hotSpotAge,
    Object chooseBiomesReceiver
  ) {
    if (shouldUseSunkenShieldVolcano(hotSpotAge, point)) {
      return SUNKEN_SHIELD_VOLCANO;
    }
    int biome = TfeKarstBiomeInvoke.hotSpotBiome(
      chooseBiomesReceiver,
      hotSpotAge
    );
    if (biome == 0) {
      biome = fallbackShieldForAge(hotSpotAge);
    }
    return applyGlacialShieldVariant(point, biome);
  }

  private static int fallbackShieldForAge(byte age) {
    return switch (age) {
      case 4 -> ANCIENT_SHIELD_VOLCANO;
      case 3 -> EXTINCT_SHIELD_VOLCANO;
      case 2 -> DORMANT_SHIELD_VOLCANO;
      case 1 -> ACTIVE_SHIELD_VOLCANO;
      default -> ANCIENT_SHIELD_VOLCANO;
    };
  }

  private static int applyGlacialShieldVariant(Region.Point point, int biome) {
    if (!isGlaciatableShieldVolcano(biome)) {
      return biome;
    }
    final float maxIceSheetTemp = -14f + 0.006f * point.rainfall;
    final float temperature = point.temperature;
    if (point.land() && temperature < maxIceSheetTemp) {
      return ICE_SHEET_SHIELD_VOLCANO;
    }
    if (temperature < maxIceSheetTemp + 4f) {
      return GLACIATED_SHIELD_VOLCANO;
    }
    return biome;
  }

  private static boolean shouldUseSunkenShieldVolcano(
    int hotSpotAge,
    Region.Point point
  ) {
    if (point.land() || point.island()) {
      return false;
    }
    if (MapHotspotBiomes.keepOceanBiomeForAncient((byte) hotSpotAge, point)) {
      return true;
    }
    final int biome = point.biome;
    return (
      biome == DEEP_OCEAN ||
      biome == OCEAN_REEF ||
      biome == DEEP_OCEAN_TRENCH ||
      (hotSpotAge == MapHotspotBiomes.ANCIENT_AGE && biome == OCEAN)
    );
  }

  private static boolean isGlaciatableShieldVolcano(int biome) {
    return (
      biome == ACTIVE_SHIELD_VOLCANO ||
      biome == DORMANT_SHIELD_VOLCANO ||
      biome == EXTINCT_SHIELD_VOLCANO
    );
  }
}
