package net.yazloysasha.tfcrealworld.mixin.world.region;

import static net.dries007.tfc.world.layer.TFCLayers.*;

import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.dries007.tfc.world.layer.framework.Area;
import net.dries007.tfc.world.region.ChooseBiomes;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.registry.DivergenceNoiseRegistry;
import net.yazloysasha.tfcrealworld.util.registry.HotspotsNoiseRegistry;
import net.yazloysasha.tfcrealworld.world.biome.CoverageRareBiomes;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGAltitudeNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGDivergenceNoise;
import net.yazloysasha.tfcrealworld.world.region.MapTectonics;
import net.yazloysasha.tfcrealworld.world.volcano.CenteredFeatureAligner;
import net.yazloysasha.tfcrealworld.world.volcano.MapHotspotLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChooseBiomes.class, remap = false)
public class ChooseBiomesMixin {

  @Unique
  private static final int[] RIFT_VALLEY_BIOMES = {
    RIFT_VALLEY,
    RIFT_VALLEY,
    RIFT_VALLEY,
    RIFT_LAKE,
    RIFT_LAKE,
  };

  @Unique
  private static final int[] LAND_RIFT_SPAWN_ROLL = { 1, 0 };

  @Unique
  private static final double ICE_SHEET_EDGE_MELTWATER_LAKE_CHANCE = 0.16;

  @Unique
  private static final double OCEANIC_MOUNTAIN_LAKE_CHANCE = 0.1;

  @Unique
  private static final float LAKE_RAINFALL_BOOST = 0.09f;

  /**
   * Vanilla coastal volcanic ice-sheet cutoff. Kept identical; only the
   * comparison temperature is jittered for map Köppen discreteness.
   */
  @Unique
  private static final float COASTAL_VOLCANIC_ICE_SHEET_BASE_TEMP = -16f;

  @Unique
  private static final float COASTAL_VOLCANIC_ICE_SHEET_RAIN_SCALE = 0.006f;

  @Unique
  private static final float COASTAL_VOLCANIC_ICE_SHEET_TEMP_OFFSET = 2f;

  @Unique
  private static final float COASTAL_VOLCANIC_GLACIATED_TEMP_OFFSET = 6f;

  @Unique
  private static final float COASTAL_VOLCANIC_CARVED_TEMP_OFFSET = 10f;

  @Unique
  private static final float VOLCANIC_OCEANIC_GLACIAL_TEMP_CHAOS = 1f;

  @Unique
  private static final double VOLCANIC_OCEANIC_GLACIAL_ECOTONE_CHANCE = 0.5;

  @Unique
  private static void tfcrealworld$rollMapLakes(Region region, long worldSeed) {
    final boolean koppenFromMap = TFCRealWorldConfig.KOPPEN_FROM_MAP.get();
    final boolean altitudeFromMap = TFCRealWorldConfig.ALTITUDE_FROM_MAP.get();
    if (!koppenFromMap && !altitudeFromMap) {
      return;
    }

    for (final Region.Point point : region.points()) {
      if (point == null || !point.land()) {
        continue;
      }
      if (koppenFromMap && point.biome == ICE_SHEET_EDGE && !point.lake()) {
        if (
          tfcrealworld$seededChance(
            worldSeed,
            point.x,
            point.z,
            0x7a4f2c91e83b05d6L,
            ICE_SHEET_EDGE_MELTWATER_LAKE_CHANCE
          )
        ) {
          point.setLake();
          point.rainfall += LAKE_RAINFALL_BOOST * (500f - point.rainfall);
          point.biome = lakeFor(ICE_SHEET_EDGE);
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
            point.x,
            point.z,
            0x3c9e2b71a4d805f1L,
            OCEANIC_MOUNTAIN_LAKE_CHANCE
          )
        ) {
          final int biome = point.biome;
          point.setLake();
          point.rainfall += LAKE_RAINFALL_BOOST * (500f - point.rainfall);
          point.biome = lakeFor(biome);
        }
      }
    }
  }

  @Unique
  private static void tfcrealworld$applyVolcanicOceanicGlacialBands(
    Region region,
    long worldSeed
  ) {
    if (!TFCRealWorldConfig.KOPPEN_FROM_MAP.get()) {
      return;
    }

    for (final Region.Point point : region.points()) {
      if (
        point == null ||
        !point.land() ||
        point.lake() ||
        isLake(point.biome) ||
        !tfcrealworld$isVolcanicOceanicMountainFamily(point.biome)
      ) {
        continue;
      }

      final float maxIceSheetTemp =
        COASTAL_VOLCANIC_ICE_SHEET_BASE_TEMP +
        COASTAL_VOLCANIC_ICE_SHEET_RAIN_SCALE * point.rainfall;
      final float biomeTemp =
        point.temperature +
        VOLCANIC_OCEANIC_GLACIAL_TEMP_CHAOS *
        tfcrealworld$seededSignedUnit(
          worldSeed,
          point.x,
          point.z,
          0x51c3e90a7b6d24f8L
        );
      point.biome = tfcrealworld$coastalVolcanicOceanicBiomeForTemp(
        biomeTemp,
        maxIceSheetTemp
      );
    }

    tfcrealworld$paintVolcanicOceanicGlacialEcotone(region, worldSeed);
  }

  /**
   * Köppen EF/ET often abuts DFC with no DFD strip. Paint a one-cell glaciated
   * / glacially-carved pair on that volcanic oceanic contact so both biomes
   * can exist without changing stored climate.
   */
  @Unique
  private static void tfcrealworld$paintVolcanicOceanicGlacialEcotone(
    Region region,
    long worldSeed
  ) {
    final IntArrayList toGlaciated = new IntArrayList();
    final IntArrayList toCarved = new IntArrayList();

    for (final Region.Point point : region.points()) {
      if (
        point == null || !point.land() || point.lake() || isLake(point.biome)
      ) {
        continue;
      }
      if (
        !tfcrealworld$seededChance(
          worldSeed,
          point.x,
          point.z,
          0x2e9b14c86a70d5f3L,
          VOLCANIC_OCEANIC_GLACIAL_ECOTONE_CHANCE
        )
      ) {
        continue;
      }

      if (
        point.biome == ICE_SHEET_VOLCANIC_OCEANIC_MOUNTAINS &&
        tfcrealworld$touchesWarmerVolcanicOceanic(region, point)
      ) {
        toGlaciated.add(point.index);
      } else if (
        point.biome == VOLCANIC_OCEANIC_MOUNTAINS &&
        tfcrealworld$touchesIceOrGlaciatedVolcanicOceanic(region, point)
      ) {
        toCarved.add(point.index);
      }
    }

    for (int i = 0; i < toGlaciated.size(); i++) {
      final Region.Point point = region.atIndex(toGlaciated.getInt(i));
      if (point != null) {
        point.biome = GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS;
      }
    }
    for (int i = 0; i < toCarved.size(); i++) {
      final Region.Point point = region.atIndex(toCarved.getInt(i));
      if (point != null) {
        point.biome = GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS;
      }
    }
  }

  @Unique
  private static int tfcrealworld$coastalVolcanicOceanicBiomeForTemp(
    float temp,
    float maxIceSheetTemp
  ) {
    if (temp < maxIceSheetTemp + COASTAL_VOLCANIC_ICE_SHEET_TEMP_OFFSET) {
      return ICE_SHEET_VOLCANIC_OCEANIC_MOUNTAINS;
    }
    if (temp < maxIceSheetTemp + COASTAL_VOLCANIC_GLACIATED_TEMP_OFFSET) {
      return GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS;
    }
    if (temp < maxIceSheetTemp + COASTAL_VOLCANIC_CARVED_TEMP_OFFSET) {
      return GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS;
    }
    return VOLCANIC_OCEANIC_MOUNTAINS;
  }

  @Unique
  private static boolean tfcrealworld$isVolcanicOceanicMountainFamily(
    int biome
  ) {
    return (
      biome == VOLCANIC_OCEANIC_MOUNTAINS ||
      biome == ICE_SHEET_VOLCANIC_OCEANIC_MOUNTAINS ||
      biome == GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS ||
      biome == GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS
    );
  }

  @Unique
  private static boolean tfcrealworld$isVolcanicMountainFamily(int biome) {
    return (
      biome == VOLCANIC_MOUNTAINS ||
      biome == VOLCANIC_OCEANIC_MOUNTAINS ||
      biome == VOLCANIC_MOUNTAIN_LAKE ||
      biome == VOLCANIC_OCEANIC_MOUNTAIN_LAKE ||
      biome == ICE_SHEET_VOLCANIC_MOUNTAINS ||
      biome == ICE_SHEET_VOLCANIC_OCEANIC_MOUNTAINS ||
      biome == GLACIATED_VOLCANIC_MOUNTAINS ||
      biome == GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS ||
      biome == GLACIALLY_CARVED_VOLCANIC_MOUNTAINS ||
      biome == GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS
    );
  }

  @Unique
  private static boolean tfcrealworld$touchesWarmerVolcanicOceanic(
    Region region,
    Region.Point point
  ) {
    final int index = point.index;
    Region.Point neighbor = region.atOffset(index, 1, 0);
    if (neighbor != null && neighbor.land()) {
      final int biome = neighbor.biome;
      if (
        biome == VOLCANIC_OCEANIC_MOUNTAINS ||
        biome == GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS ||
        biome == GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS
      ) {
        return true;
      }
    }
    neighbor = region.atOffset(index, -1, 0);
    if (neighbor != null && neighbor.land()) {
      final int biome = neighbor.biome;
      if (
        biome == VOLCANIC_OCEANIC_MOUNTAINS ||
        biome == GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS ||
        biome == GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS
      ) {
        return true;
      }
    }
    neighbor = region.atOffset(index, 0, 1);
    if (neighbor != null && neighbor.land()) {
      final int biome = neighbor.biome;
      if (
        biome == VOLCANIC_OCEANIC_MOUNTAINS ||
        biome == GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS ||
        biome == GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS
      ) {
        return true;
      }
    }
    neighbor = region.atOffset(index, 0, -1);
    if (neighbor != null && neighbor.land()) {
      final int biome = neighbor.biome;
      if (
        biome == VOLCANIC_OCEANIC_MOUNTAINS ||
        biome == GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS ||
        biome == GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS
      ) {
        return true;
      }
    }
    return false;
  }

  @Unique
  private static boolean tfcrealworld$touchesIceOrGlaciatedVolcanicOceanic(
    Region region,
    Region.Point point
  ) {
    final int index = point.index;
    Region.Point neighbor = region.atOffset(index, 1, 0);
    if (neighbor != null && neighbor.land()) {
      final int biome = neighbor.biome;
      if (
        biome == ICE_SHEET_VOLCANIC_OCEANIC_MOUNTAINS ||
        biome == GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS
      ) {
        return true;
      }
    }
    neighbor = region.atOffset(index, -1, 0);
    if (neighbor != null && neighbor.land()) {
      final int biome = neighbor.biome;
      if (
        biome == ICE_SHEET_VOLCANIC_OCEANIC_MOUNTAINS ||
        biome == GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS
      ) {
        return true;
      }
    }
    neighbor = region.atOffset(index, 0, 1);
    if (neighbor != null && neighbor.land()) {
      final int biome = neighbor.biome;
      if (
        biome == ICE_SHEET_VOLCANIC_OCEANIC_MOUNTAINS ||
        biome == GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS
      ) {
        return true;
      }
    }
    neighbor = region.atOffset(index, 0, -1);
    if (neighbor != null && neighbor.land()) {
      final int biome = neighbor.biome;
      if (
        biome == ICE_SHEET_VOLCANIC_OCEANIC_MOUNTAINS ||
        biome == GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS
      ) {
        return true;
      }
    }
    return false;
  }

  @Unique
  private static boolean tfcrealworld$seededChance(
    long worldSeed,
    int gridX,
    int gridZ,
    long salt,
    double chance
  ) {
    return tfcrealworld$seededUnit(worldSeed, gridX, gridZ, salt) < chance;
  }

  @Unique
  private static float tfcrealworld$seededSignedUnit(
    long worldSeed,
    int gridX,
    int gridZ,
    long salt
  ) {
    return (float) (tfcrealworld$seededUnit(worldSeed, gridX, gridZ, salt) *
        2.0 -
      1.0);
  }

  @Unique
  private static double tfcrealworld$seededUnit(
    long worldSeed,
    int gridX,
    int gridZ,
    long salt
  ) {
    long hash = worldSeed ^ salt;
    hash ^= (long) gridX * 0x9E3779B97F4A7C15L;
    hash ^= (long) gridZ * 0x6C078965L;
    hash = tfcrealworld$mix64(hash);
    return (hash >>> 11) * (1.0 / (1L << 53));
  }

  @Unique
  private static long tfcrealworld$mix64(long z) {
    z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
    z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
    return z ^ (z >>> 33);
  }

  /**
   * Vanilla already maps {@code SALT_MARSH → TOWER_KARST_BAY} inside
   * {@code getTowerKarstBiome}, but paints mangrove marshes after karst.
   * Feed drowned coastal karst into that vanilla call so climate stays in TFC.
   */
  @ModifyArg(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/region/ChooseBiomes;getTowerKarstBiome(I)I"
    )
  )
  private int tfcrealworld$coastalLowlandsAsMarshForTowerKarst(
    int biome,
    @Local Region.Point point
  ) {
    if (
      point.distanceToOcean <= 2 &&
      (biome == LOWLANDS || biome == PLAINS || biome == LOW_CANYONS)
    ) {
      return SALT_MARSH;
    }
    return biome;
  }

  /**
   * Vanilla Burren only remaps a few bases. Paleo ice-margin biomes,
   * ice-sheet rim, humid-high {@code OLD_MOUNTAINS}, and mesa/canyon leftovers
   * fall through. Climate is already decided by TFC before this call.
   */
  @ModifyArg(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/region/ChooseBiomes;getBurrenBiome(I)I"
    )
  )
  private int tfcrealworld$burrenBasesForVanilla(int biome) {
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

  @Redirect(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/region/ChooseBiomes;getHotSpotBiome(I)I"
    )
  )
  private int tfcrealworld$mapHotspotBiomeOrKeepVanillaMountain(
    ChooseBiomes instance,
    int age,
    @Local Region.Point point
  ) {
    // Ocean cells must not become land shield-volcano islands (AddHotspots no
    // longer calls setLand()). Age 4 ocean → SUNKEN is handled by vanilla
    // before this call when the biome is already an ocean family.
    // Inland ICE_SHEET_VOLCANIC_MOUNTAINS / strato path is untouched: land +
    // keepMountainBiome still returns the mountain biome chosen above.
    if (!point.land()) {
      return point.biome;
    }
    // Never replace an already-chosen volcanic-mountain (incl. ice-sheet)
    // biome with a shield volcano — that was wiping ICE_SHEET_VOLCANIC_*.
    if (tfcrealworld$isVolcanicMountainFamily(point.biome)) {
      return point.biome;
    }
    final MapHotspotLayout layout = HotspotsNoiseRegistry.biomeLayout();
    if (layout != null && layout.keepMountainBiome(point)) {
      return point.biome;
    }
    return (
      (ChooseBiomesAccessor) (Object) instance
    ).tfcrealworld$invokeGetHotSpotBiome(age);
  }

  @Inject(method = "apply", at = @At("HEAD"))
  private void tfcrealworld$prepareMapDivergenceForChooseBiomes(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    if (TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get()) {
      final MapHotspotLayout layout = HotspotsNoiseRegistry.biomeLayout();
      if (layout != null) {
        layout.prepareChooseBiomes(
          context.region,
          context.generator().seed().seed()
        );
      }
    }

    if (!MapTectonics.isActive(context.generator())) {
      return;
    }
    final PNGDivergenceNoise divergenceNoise = DivergenceNoiseRegistry.get(
      context.generator()
    );
    if (divergenceNoise == null) {
      return;
    }
    for (final Region.Point point : context.region.points()) {
      float divergence = divergenceNoise.getDivergence(point.x, point.z);
      if (
        point.land() &&
        point.distanceToEdge < 3 &&
        !MapTectonics.isLandRiftCore(
          divergenceNoise,
          point.x,
          point.z,
          divergence
        ) &&
        divergence > 0
      ) {
        divergence = 0f;
      }
      point.divergence = divergence;
    }
  }

  @Inject(method = "apply", at = @At("TAIL"))
  private void tfcrealworld$applyMapBiomes(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    final RegionGenerator generator = context.generator();
    final boolean altitudeFromMap = TFCRealWorldConfig.ALTITUDE_FROM_MAP.get();
    final PNGDivergenceNoise divergenceNoise = MapTectonics.isActive(generator)
      ? DivergenceNoiseRegistry.get(generator)
      : null;

    // Ocean biomes: leave vanilla ChooseBiomes (driven by oceanDepth only).
    // Depth/trench/ridge come from altitude + tectonics, not biome hardcode.
    if (divergenceNoise != null) {
      final ChooseBiomesAccessor accessor = (ChooseBiomesAccessor) this;
      final Area blobArea = context.generator().biomeArea.get();
      final long rngSeed = context.random.nextLong();

      for (final Region.Point point : context.region.points()) {
        tfcrealworld$applyLandRiftBiomes(
          point,
          divergenceNoise,
          accessor,
          blobArea,
          rngSeed
        );
      }
    }

    // Abyssal (7) falls through vanilla's "else" ocean branch and becomes
    // DEEP_OCEAN_ATOLLS across Earth-scale warm basins. Remap to plain deep
    // without assigning other ocean biomes — reefs/arcs/ridges/trenches stay
    // whatever vanilla already chose from oceanDepth / flags.
    if (altitudeFromMap) {
      tfcrealworld$remapAbyssalDeepAtolls(context.region);
    }

    tfcrealworld$applyVolcanicOceanicGlacialBands(
      context.region,
      generator.seed().seed()
    );

    tfcrealworld$rollMapLakes(context.region, generator.seed().seed());

    if (
      tfcrealworld$shouldAlignCenteredVolcanoes(
        altitudeFromMap,
        divergenceNoise != null
      )
    ) {
      CenteredFeatureAligner.align(context.region, generator.seed().seed());
    }
  }

  @Unique
  private static boolean tfcrealworld$shouldAlignCenteredVolcanoes(
    boolean altitudeFromMap,
    boolean mapTectonics
  ) {
    return (
      TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get() ||
      altitudeFromMap ||
      mapTectonics
    );
  }

  @Unique
  private static boolean tfcrealworld$skipLandRiftBiomeReplace(
    Region.Point point
  ) {
    if (point.lake() || isLake(point.biome)) {
      return true;
    }
    if (CoverageRareBiomes.preserve(point.biome)) {
      return true;
    }
    if (point.isSurfaceRockKarst) {
      return true;
    }
    return (
      isFlatIceSheet(point.biome) ||
      point.biome == ICE_SHEET_EDGE ||
      point.biome == ICE_SHEET_SHORE
    );
  }

  @Unique
  private static void tfcrealworld$applyLandRiftBiomes(
    Region.Point point,
    PNGDivergenceNoise divergenceNoise,
    ChooseBiomesAccessor accessor,
    Area blobArea,
    long rngSeed
  ) {
    if (
      !point.land() ||
      point.island() ||
      point.hotSpotAge > 0 ||
      tfcrealworld$skipLandRiftBiomeReplace(point) ||
      !MapTectonics.isLandRiftCore(
        divergenceNoise,
        point.x,
        point.z,
        point.divergence
      )
    ) {
      return;
    }

    final int areaSeed = blobArea.get(point.x, point.z);
    if (
      accessor.tfcrealworld$invokeRandomSeededFrom(
        rngSeed,
        areaSeed,
        LAND_RIFT_SPAWN_ROLL
      ) !=
      1
    ) {
      return;
    }

    if (point.distanceToOcean > 2) {
      point.biome = accessor.tfcrealworld$invokeRandomSeededFrom(
        rngSeed,
        areaSeed ^ 0x5f3759df,
        RIFT_VALLEY_BIOMES
      );
    } else {
      point.biome = RIFT_VALLEY;
    }
  }

  /**
   * Map abyssal plain depth (7) would otherwise become {@code DEEP_OCEAN_ATOLLS}
   * under vanilla warm+far rules. Force plain {@code DEEP_OCEAN} for that
   * sentinel only; leave depth-4 atoll habitat and all other ocean biomes.
   */
  @Unique
  private static void tfcrealworld$remapAbyssalDeepAtolls(Region region) {
    for (final Region.Point point : region.points()) {
      if (point == null || point.land()) {
        continue;
      }
      if (
        point.biome == DEEP_OCEAN_ATOLLS &&
        Byte.toUnsignedInt(point.oceanDepth) ==
        PNGAltitudeNoise.ABYSSAL_OCEAN_DEPTH
      ) {
        point.biome = DEEP_OCEAN;
      }
    }
  }
}
