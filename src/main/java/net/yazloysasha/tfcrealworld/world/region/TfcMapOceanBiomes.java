package net.yazloysasha.tfcrealworld.world.region;

import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.layer.framework.Area;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.registry.HotspotsNoiseRegistry;
import net.yazloysasha.tfcrealworld.world.backport.ChooseBiomesSupport;
import net.yazloysasha.tfcrealworld.world.volcano.MapHotspotBiomes;
import net.yazloysasha.tfcrealworld.world.volcano.MapHotspotLayout;
import org.jetbrains.annotations.Nullable;

/**
 * TFC 3 ocean biomes from {@code baseOceanDepth} when altitude comes from the
 * map on pure TFC 3 ({@code ChooseBiomes}). TFG uses {@code TFGChooseBiomesTask};
 * TFE uses {@code MapOceanBiomeFromAltitude}.
 */
public final class TfcMapOceanBiomes {

  private static final int[] MID_DEPTH_OCEAN_BIOMES = {
    TFCLayers.OCEAN,
    TFCLayers.OCEAN_REEF,
  };

  private TfcMapOceanBiomes() {}

  public static void apply(RegionGenerator.Context context) {
    if (!TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()) {
      return;
    }

    final Region region = context.region;
    final Area blobArea = context.generator().biomeArea.get();
    final long rngSeed = context.random.nextLong();
    @Nullable
    final MapHotspotLayout hotspotLayout =
      TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get()
        ? HotspotsNoiseRegistry.biomeLayout()
        : null;

    final Region.Point[] data = region.data();
    for (int index = 0; index < data.length; index++) {
      final Region.Point point = data[index];
      if (point == null || point.land() || point.island() || point.mountain()) {
        continue;
      }
      if (skipForHotspot(region, index, point, hotspotLayout)) {
        continue;
      }

      final int depth = Byte.toUnsignedInt(point.baseOceanDepth);
      final int areaSeed = blobArea.get(
        RegionCoords.gridX(region, index),
        RegionCoords.gridZ(region, index)
      );

      if (depth < 4) {
        point.biome = TFCLayers.OCEAN;
      } else if (depth > 9) {
        point.biome = TFCLayers.DEEP_OCEAN_TRENCH;
      } else if (depth > 6) {
        point.biome = TFCLayers.DEEP_OCEAN;
      } else {
        point.biome = ChooseBiomesSupport.randomSeededFrom(
          rngSeed,
          areaSeed,
          MID_DEPTH_OCEAN_BIOMES
        );
      }
    }
  }

  private static boolean skipForHotspot(
    Region region,
    int index,
    Region.Point point,
    @Nullable MapHotspotLayout layout
  ) {
    if (layout == null) {
      return false;
    }
    final byte age = layout.ageAtGrid(
      RegionCoords.gridX(region, index),
      RegionCoords.gridZ(region, index)
    );
    if (age == 0 || MapHotspotBiomes.keepOceanBiomeForAncient(age, point)) {
      return false;
    }
    return true;
  }
}
