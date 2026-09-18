package net.yazloysasha.tfcrealworld.mixin.world.region.tfg;

import static su.terrafirmagreg.core.world.new_ow_wg.TFGLayers.*;

import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.helpers.WorldSeedHolder;
import net.yazloysasha.tfcrealworld.util.registry.HotspotsNoiseRegistry;
import net.yazloysasha.tfcrealworld.world.region.MapBiomeLakeRolls;
import net.yazloysasha.tfcrealworld.world.region.RegionCoords;
import net.yazloysasha.tfcrealworld.world.volcano.MapHotspotBiomes;
import net.yazloysasha.tfcrealworld.world.volcano.MapHotspotLayout;
import net.yazloysasha.tfcrealworld.world.volcano.MapHotspotLayout.MountainStyle;
import net.yazloysasha.tfcrealworld.world.volcano.TfgCenteredFeatureAligner;
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
 * oceanic, never the high-land table). Stratovolcano mountain hotspots paint
 * {@code VOLCANIC_MOUNTAINS} instead of shield or canyons. Burren/tower-karst
 * extra bases match 1.21.1 {@code ChooseBiomesMixin}. Centered volcano cells
 * align to TFG noise.
 */
@Mixin(value = TFGChooseBiomesTask.class, remap = false)
public class TfgChooseBiomesMixin {

  @Unique
  private static final double ICE_SHEET_EDGE_MELTWATER_LAKE_CHANCE = 0.16;

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
    tfcrealworld$rollIceSheetEdgeLakes(
      context.region,
      WorldSeedHolder.getSeed()
    );
    MapBiomeLakeRolls.rollOceanicMountainLakes(
      context.region,
      WorldSeedHolder.getSeed(),
      OCEANIC_MOUNTAINS,
      VOLCANIC_OCEANIC_MOUNTAINS,
      TFGLayers::lakeFor
    );
    if (
      TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get() ||
      TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()
    ) {
      TfgCenteredFeatureAligner.align(
        context.region,
        WorldSeedHolder.getSeed()
      );
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

  /**
   * Vanilla already maps {@code SALT_MARSH → TOWER_KARST_BAY} inside
   * {@code getTowerKarstBiome}, but paints mangrove marshes after karst.
   * Feed drowned coastal karst into that vanilla call so climate stays in TFC.
   */
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
        if (tfcrealworld$shouldPaintStratovolcano(point)) {
          proposedBiome = MapHotspotBiomes.volcanicMountainFor(
            point,
            VOLCANIC_MOUNTAINS,
            VOLCANIC_OCEANIC_MOUNTAINS
          );
        } else {
          return;
        }
      }
      point.biome = proposedBiome;
    } finally {
      ASSIGNING_HOTSPOT_BIOME.set(Boolean.FALSE);
    }
  }

  /**
   * Vanilla Burren only remaps a few bases. Paleo ice-margin biomes,
   * ice-sheet rim, humid-high {@code OLD_MOUNTAINS}, and mesa/canyon leftovers
   * fall through. Climate is already decided by TFC before this call.
   */
  @Unique
  private static int tfcrealworld$burrenBase(int biome) {
    if (
      biome == KNOB_AND_KETTLE ||
      biome == PATTERNED_GROUND ||
      biome == INVERTED_PATTERNED_GROUND ||
      biome == ICE_SHEET_EDGE
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
    return layout.keepMountainBiome(
      region,
      point,
      ((IRegionPoint) point).tfg$getHotSpotAge()
    );
  }

  @Unique
  private static boolean tfcrealworld$shouldPaintStratovolcano(
    Region.Point point
  ) {
    if (tfcrealworld$isIceMountain(point.biome)) {
      return false;
    }
    final MapHotspotLayout layout = HotspotsNoiseRegistry.biomeLayout();
    final Region region = CURRENT_REGION.get();
    if (layout == null || region == null) {
      return false;
    }
    return layout.styleAt(region, point) == MountainStyle.STRATOVOLCANO;
  }

  @Unique
  private static boolean tfcrealworld$isIceMountain(int biome) {
    return (
      biome == ICE_SHEET_MOUNTAINS ||
      biome == ICE_SHEET_OCEANIC_MOUNTAINS ||
      biome == GLACIATED_MOUNTAINS ||
      biome == GLACIATED_OCEANIC_MOUNTAINS ||
      biome == GLACIALLY_CARVED_MOUNTAINS ||
      biome == GLACIALLY_CARVED_OCEANIC_MOUNTAINS
    );
  }

  @Unique
  private static void tfcrealworld$rollIceSheetEdgeLakes(
    Region region,
    long worldSeed
  ) {
    if (!TFCRealWorldConfig.KOPPEN_FROM_MAP.get()) {
      return;
    }

    final Region.Point[] data = region.data();
    for (int index = 0; index < data.length; index++) {
      final Region.Point point = data[index];
      if (point == null || !point.land() || point.lake()) {
        continue;
      }
      if (point.biome != ICE_SHEET_EDGE) {
        continue;
      }
      final int gridX = RegionCoords.gridX(region, index);
      final int gridZ = RegionCoords.gridZ(region, index);
      if (
        !tfcrealworld$seededChance(
          worldSeed,
          gridX,
          gridZ,
          0x7a4f2c91e83b05d6L,
          ICE_SHEET_EDGE_MELTWATER_LAKE_CHANCE
        )
      ) {
        continue;
      }
      point.setLake();
      point.rainfall += LAKE_RAINFALL_BOOST * (500f - point.rainfall);
      point.biome = TFGLayers.lakeFor(ICE_SHEET_EDGE);
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
