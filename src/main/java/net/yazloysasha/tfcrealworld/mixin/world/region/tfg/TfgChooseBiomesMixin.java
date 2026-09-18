package net.yazloysasha.tfcrealworld.mixin.world.region.tfg;

import static su.terrafirmagreg.core.world.new_ow_wg.TFGLayers.*;

import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.helpers.WorldSeedHolder;
import net.yazloysasha.tfcrealworld.util.registry.HotspotsNoiseRegistry;
import net.yazloysasha.tfcrealworld.world.region.RegionCoords;
import net.yazloysasha.tfcrealworld.world.volcano.CenteredFeatureAligner;
import net.yazloysasha.tfcrealworld.world.volcano.MapHotspotLayout;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import su.terrafirmagreg.core.world.new_ow_wg.TFGLayers;
import su.terrafirmagreg.core.world.new_ow_wg.region.IRegionPoint;
import su.terrafirmagreg.core.world.new_ow_wg.region.TFGChooseBiomesTask;

/**
 * TFG still chooses biomes. Map mountain cores follow TFC 4
 * {@code ChooseBiomes} ({@code mountain()} → ice / {@code MOUNTAINS} /
 * oceanic, never the high-land table). Burren/tower-karst get the same extra
 * bases as 1.21.1, and centered volcano cells are aligned to TFG's own noise.
 */
@Mixin(value = TFGChooseBiomesTask.class, remap = false)
public class TfgChooseBiomesMixin {

  @Unique
  private static final double ICE_SHEET_EDGE_MELTWATER_LAKE_CHANCE = 0.16;

  @Unique
  private static final double OCEANIC_MOUNTAIN_LAKE_CHANCE = 0.1;

  @Unique
  private static final float LAKE_RAINFALL_BOOST = 0.09f;

  @Unique
  private static final ThreadLocal<Boolean> ASSIGNING_HOTSPOT_BIOME =
    ThreadLocal.withInitial(() -> Boolean.FALSE);

  @Unique
  private static final ThreadLocal<Region> CURRENT_REGION = new ThreadLocal<>();

  @Unique
  private static final ThreadLocal<Region.Point> CURRENT_POINT =
    new ThreadLocal<>();

  @Inject(method = "apply", at = @At("HEAD"))
  private void tfcrealworld$prepareMapHotspotLayout(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    CURRENT_REGION.set(context.region);
    if (!TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get()) {
      return;
    }
    final MapHotspotLayout layout = HotspotsNoiseRegistry.biomeLayout();
    if (layout != null) {
      layout.prepareChooseBiomes(context.region, WorldSeedHolder.getSeed());
    }
  }

  @Inject(method = "apply", at = @At("TAIL"))
  private void tfcrealworld$cleanupChooseBiomes(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    tfcrealworld$applyMapKarstRims(context.region);
    tfcrealworld$paintTuyasEdge(context.region);
    tfcrealworld$rollMapLakes(context.region, WorldSeedHolder.getSeed());
    if (
      TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get() ||
      TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()
    ) {
      CenteredFeatureAligner.align(context.region, WorldSeedHolder.getSeed());
    }
    CURRENT_REGION.remove();
    CURRENT_POINT.remove();
    ASSIGNING_HOTSPOT_BIOME.remove();
  }

  @Redirect(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lsu/terrafirmagreg/core/world/new_ow_wg/region/TFGChooseBiomesTask;getHotSpotBiome(I)I"
    )
  )
  private int tfcrealworld$markHotspotBiomeAssignment(
    TFGChooseBiomesTask instance,
    int age
  ) {
    ASSIGNING_HOTSPOT_BIOME.set(Boolean.TRUE);
    return (
      (TfgChooseBiomesAccessor) (Object) instance
    ).tfcrealworld$invokeGetHotSpotBiome(age);
  }

  @Redirect(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lsu/terrafirmagreg/core/world/new_ow_wg/region/TFGChooseBiomesTask;getBurrenBiome(I)I"
    )
  )
  private int tfcrealworld$burrenBasesForVanilla(
    TFGChooseBiomesTask instance,
    int biome
  ) {
    return (
      (TfgChooseBiomesAccessor) (Object) instance
    ).tfcrealworld$invokeGetBurrenBiome(tfcrealworld$burrenBase(biome));
  }

  @Redirect(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lsu/terrafirmagreg/core/world/new_ow_wg/region/TFGChooseBiomesTask;getTowerKarstBiome(I)I"
    )
  )
  private int tfcrealworld$coastalLowlandsAsMarshForTowerKarst(
    TFGChooseBiomesTask instance,
    int biome
  ) {
    final Region.Point point = CURRENT_POINT.get();
    if (
      point != null &&
      point.distanceToOcean <= 2 &&
      (biome == LOWLANDS || biome == PLAINS || biome == LOW_CANYONS)
    ) {
      biome = SALT_MARSH;
    }
    return (
      (TfgChooseBiomesAccessor) (Object) instance
    ).tfcrealworld$invokeGetTowerKarstBiome(biome);
  }

  @Redirect(
    method = "apply",
    at = @At(
      value = "FIELD",
      target = "Lnet/dries007/tfc/world/region/Region$Point;biome:I",
      opcode = Opcodes.PUTFIELD
    )
  )
  private void tfcrealworld$keepMapMountainBiomeUnderHotspot(
    Region.Point point,
    int proposedBiome
  ) {
    CURRENT_POINT.set(point);
    try {
      if (
        TFCRealWorldConfig.ALTITUDE_FROM_MAP.get() &&
        point.mountain() &&
        tfcrealworld$isSoftMountainFill(proposedBiome)
      ) {
        proposedBiome = point.coastalMountain() ? OCEANIC_MOUNTAINS : MOUNTAINS;
      }
      if (
        Boolean.TRUE.equals(ASSIGNING_HOTSPOT_BIOME.get()) &&
        tfcrealworld$shouldKeepMountainBiome(point)
      ) {
        return;
      }
      point.biome = proposedBiome;
    } finally {
      ASSIGNING_HOTSPOT_BIOME.set(Boolean.FALSE);
    }
  }

  @Unique
  private static int tfcrealworld$burrenBase(int biome) {
    if (
      biome == KNOB_AND_KETTLE ||
      biome == PATTERNED_GROUND ||
      biome == INVERTED_PATTERNED_GROUND ||
      biome == ICE_SHEET_EDGE ||
      biome == TUYAS
    ) {
      return DRUMLINS;
    }
    if (biome == LOW_CANYONS || biome == LOWLANDS) {
      return PLAINS;
    }
    if (biome == OLD_MOUNTAINS) {
      return HIGHLANDS;
    }
    if (biome == STAIR_STEP_CANYONS || biome == MESAS || biome == BUTTES) {
      return PLATEAU;
    }
    return biome;
  }

  /**
   * Vanilla paints SALT_MARSH after karst, and Burren only remaps DRUMLINS.
   * Köppen cells skip those exact bases, so re-apply the 1.21.1 extra bases
   * on the finished region instead of trusting the in-loop ThreadLocal point.
   */
  @Unique
  private static void tfcrealworld$applyMapKarstRims(Region region) {
    final Region.Point[] data = region.data();
    for (final Region.Point point : data) {
      if (point == null || !point.land()) {
        continue;
      }
      final IRegionPoint extra = (IRegionPoint) point;
      if (!extra.tfg$getIsSurfaceRockKarst()) {
        continue;
      }
      final float rainfall = point.rainfall;
      final float temperature = point.temperature;
      final boolean towerKarst =
        rainfall > 425f && rainfall + 10f * temperature > 500f;
      if (towerKarst && point.distanceToOcean <= 2) {
        if (tfcrealworld$isTowerKarstBayBase(point.biome)) {
          point.biome = TOWER_KARST_BAY;
        }
        continue;
      }
      if (rainfall > 375f && temperature < 0f && !towerKarst) {
        if (tfcrealworld$isBurrenRocheBase(point.biome)) {
          point.biome = BURREN_ROCHE_MOUTONEE;
        }
      }
    }
  }

  @Unique
  private static boolean tfcrealworld$isTowerKarstBayBase(int biome) {
    return (
      biome == SALT_MARSH ||
      biome == LOWLANDS ||
      biome == PLAINS ||
      biome == LOW_CANYONS ||
      biome == TOWER_KARST_PLAINS ||
      biome == TOWER_KARST_LAKE
    );
  }

  @Unique
  private static boolean tfcrealworld$isBurrenRocheBase(int biome) {
    return (
      biome == DRUMLINS ||
      biome == TUYAS ||
      biome == KNOB_AND_KETTLE ||
      biome == PATTERNED_GROUND ||
      biome == INVERTED_PATTERNED_GROUND ||
      biome == ICE_SHEET_EDGE
    );
  }

  @Unique
  private static void tfcrealworld$paintTuyasEdge(Region region) {
    final Region.Point[] data = region.data();
    final boolean[] paint = new boolean[data.length];
    for (int index = 0; index < data.length; index++) {
      final Region.Point point = data[index];
      if (
        point == null ||
        !point.land() ||
        point.biome == ICE_SHEET_TUYAS ||
        !tfcrealworld$isTuyasEdgeRim(point.biome)
      ) {
        continue;
      }
      if (tfcrealworld$touchesIceSheetTuyas(region, index)) {
        paint[index] = true;
      }
    }
    for (int index = 0; index < data.length; index++) {
      if (paint[index]) {
        data[index].biome = ICE_SHEET_TUYAS_EDGE;
      }
    }
  }

  @Unique
  private static boolean tfcrealworld$touchesIceSheetTuyas(
    Region region,
    int index
  ) {
    return (
      tfcrealworld$neighborIsIceSheetTuyas(region, index, 0, -1) ||
      tfcrealworld$neighborIsIceSheetTuyas(region, index, 1, 0) ||
      tfcrealworld$neighborIsIceSheetTuyas(region, index, 0, 1) ||
      tfcrealworld$neighborIsIceSheetTuyas(region, index, -1, 0)
    );
  }

  @Unique
  private static boolean tfcrealworld$neighborIsIceSheetTuyas(
    Region region,
    int index,
    int dx,
    int dz
  ) {
    final Region.Point neighbor = RegionCoords.atOffset(region, index, dx, dz);
    return neighbor != null && neighbor.biome == ICE_SHEET_TUYAS;
  }

  @Unique
  private static boolean tfcrealworld$isTuyasEdgeRim(int biome) {
    return (
      biome == ICE_SHEET_EDGE ||
      biome == ICE_SHEET_SHORE ||
      biome == ICE_SHEET_OCEANIC ||
      biome == MELTWATER_LAKE ||
      biome == KNOB_AND_KETTLE ||
      biome == PATTERNED_GROUND ||
      biome == INVERTED_PATTERNED_GROUND ||
      biome == STONE_CIRCLES ||
      biome == DRUMLINS ||
      biome == TUYAS ||
      biome == SHORE ||
      biome == TIDAL_FLATS ||
      biome == PLAINS ||
      biome == HILLS ||
      biome == LOWLANDS ||
      biome == ROLLING_HILLS
    );
  }

  /**
   * TFC 4 never picks these for {@code point.mountain()}. TFG still samples
   * {@code MOUNTAIN_ALTITUDE_BIOMES} / {@code OCEANIC_MOUNTAIN_ALTITUDE_BIOMES},
   * which include them. Ice and volcanic mountain biomes are left alone — TFC 4
   * assigns those from climate (and {@code volcanic()}) in the same branch.
   */
  @Unique
  private static boolean tfcrealworld$isSoftMountainFill(int biome) {
    return (
      biome == OLD_MOUNTAINS ||
      biome == PLATEAU ||
      biome == PLATEAU_WIDE ||
      biome == HIGHLANDS ||
      biome == ROLLING_HILLS ||
      biome == ROCKY_PLATEAU
    );
  }

  @Unique
  private static boolean tfcrealworld$shouldKeepMountainBiome(
    Region.Point point
  ) {
    final MapHotspotLayout layout = HotspotsNoiseRegistry.biomeLayout();
    final Region region = CURRENT_REGION.get();
    if (layout == null || region == null) {
      return false;
    }
    final int index = RegionCoords.indexOf(region, point);
    if (index < 0) {
      return false;
    }
    final IRegionPoint extra = (IRegionPoint) point;
    return layout.keepMountainBiome(
      RegionCoords.gridX(region, index),
      RegionCoords.gridZ(region, index),
      point.mountain(),
      extra.tfg$getHotSpotAge()
    );
  }

  @Unique
  private static void tfcrealworld$rollMapLakes(Region region, long worldSeed) {
    final boolean koppenFromMap = TFCRealWorldConfig.KOPPEN_FROM_MAP.get();
    final boolean altitudeFromMap = TFCRealWorldConfig.ALTITUDE_FROM_MAP.get();
    if (!koppenFromMap && !altitudeFromMap) {
      return;
    }

    final Region.Point[] data = region.data();
    for (int index = 0; index < data.length; index++) {
      final Region.Point point = data[index];
      if (point == null || !point.land()) {
        continue;
      }
      final int gridX = RegionCoords.gridX(region, index);
      final int gridZ = RegionCoords.gridZ(region, index);
      if (koppenFromMap && point.biome == ICE_SHEET_EDGE && !point.lake()) {
        if (
          tfcrealworld$seededChance(
            worldSeed,
            gridX,
            gridZ,
            0x7a4f2c91e83b05d6L,
            ICE_SHEET_EDGE_MELTWATER_LAKE_CHANCE
          )
        ) {
          point.setLake();
          point.rainfall += LAKE_RAINFALL_BOOST * (500f - point.rainfall);
          point.biome = TFGLayers.lakeFor(ICE_SHEET_EDGE);
          continue;
        }
      }
      if (
        altitudeFromMap &&
        !point.lake() &&
        (point.biome == OCEANIC_MOUNTAINS ||
          point.biome == VOLCANIC_OCEANIC_MOUNTAINS)
      ) {
        if (
          tfcrealworld$seededChance(
            worldSeed,
            gridX,
            gridZ,
            0x3c9e2b71a4d805f1L,
            OCEANIC_MOUNTAIN_LAKE_CHANCE
          )
        ) {
          final int biome = point.biome;
          point.setLake();
          point.rainfall += LAKE_RAINFALL_BOOST * (500f - point.rainfall);
          point.biome = TFGLayers.lakeFor(biome);
        }
      }
    }
  }

  @Unique
  private static boolean tfcrealworld$seededChance(
    long worldSeed,
    int gridX,
    int gridZ,
    long salt,
    double chance
  ) {
    long hash = worldSeed ^ salt;
    hash ^= (long) gridX * 0x9E3779B97F4A7C15L;
    hash ^= (long) gridZ * 0x6C078965L;
    hash = tfcrealworld$mix64(hash);
    return (hash >>> 11) * (1.0 / (1L << 53)) < chance;
  }

  @Unique
  private static long tfcrealworld$mix64(long z) {
    z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
    z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
    return z ^ (z >>> 33);
  }
}
