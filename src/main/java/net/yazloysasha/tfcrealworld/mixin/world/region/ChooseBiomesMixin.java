package net.yazloysasha.tfcrealworld.mixin.world.region;

import static net.dries007.tfc.world.layer.TFCLayers.*;

import com.llamalad7.mixinextras.sugar.Local;
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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChooseBiomes.class, remap = false)
public class ChooseBiomesMixin {

  @Unique
  private static final int TRENCH_SHELF_INFLUENCE_RADIUS = 2;

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
  private static final int[] MAP_SUBDUCTION_SHELF_BIOMES = {
    OCEANIC_VOLCANIC_ARC,
    OCEANIC_VOLCANIC_ARC,
    OCEANIC_VOLCANIC_ARC,
    VOLCANIC_ISLAND,
    OCEANIC_VOLCANIC_ARC,
  };

  @Unique
  private static final double ICE_SHEET_EDGE_MELTWATER_LAKE_CHANCE = 0.16;

  @Unique
  private static final double OCEANIC_MOUNTAIN_LAKE_CHANCE = 0.05;

  @Unique
  private static final float LAKE_RAINFALL_BOOST = 0.09f;

  @Unique
  private static void tfcrealworld$rollMeltwaterLakesOnIceSheetEdge(
    Region region,
    long worldSeed
  ) {
    if (!TFCRealWorldConfig.KOPPEN_FROM_MAP.get()) {
      return;
    }
    for (final Region.Point point : region.points()) {
      if (point == null || !point.land() || point.biome != ICE_SHEET_EDGE) {
        continue;
      }
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
      }
    }
  }

  @Unique
  private static void tfcrealworld$rollOceanicMountainLakes(
    Region region,
    long worldSeed
  ) {
    if (!TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()) {
      return;
    }
    for (final Region.Point point : region.points()) {
      if (point == null || !point.land() || point.lake()) {
        continue;
      }
      final int biome = point.biome;
      if (biome != OCEANIC_MOUNTAINS && biome != VOLCANIC_OCEANIC_MOUNTAINS) {
        continue;
      }
      if (
        tfcrealworld$seededChance(
          worldSeed,
          point.x,
          point.z,
          0x3c9e2b71a4d805f1L,
          OCEANIC_MOUNTAIN_LAKE_CHANCE
        )
      ) {
        point.setLake();
        point.rainfall += LAKE_RAINFALL_BOOST * (500f - point.rainfall);
        point.biome = lakeFor(biome);
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
        !MapTectonics.isLandRiftCore(divergenceNoise, point.x, point.z) &&
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

    if (divergenceNoise != null || altitudeFromMap) {
      final ChooseBiomesAccessor accessor = (ChooseBiomesAccessor) this;
      final Area blobArea = context.generator().biomeArea.get();
      final long rngSeed = context.random.nextLong();

      for (final Region.Point point : context.region.points()) {
        if (divergenceNoise != null) {
          tfcrealworld$applyLandRiftBiomes(
            point,
            divergenceNoise,
            accessor,
            blobArea,
            rngSeed
          );
        }

        if (altitudeFromMap && !tfcrealworld$skipOceanBiome(point)) {
          tfcrealworld$assignMapOceanBiome(
            point,
            divergenceNoise,
            accessor,
            blobArea,
            rngSeed
          );
        }
      }
    }

    tfcrealworld$rollMeltwaterLakesOnIceSheetEdge(
      context.region,
      generator.seed().seed()
    );

    tfcrealworld$rollOceanicMountainLakes(
      context.region,
      generator.seed().seed()
    );

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
    point.divergence = divergenceNoise.getDivergence(point.x, point.z);
    if (
      !point.land() ||
      point.island() ||
      point.hotSpotAge > 0 ||
      tfcrealworld$skipLandRiftBiomeReplace(point) ||
      !MapTectonics.isLandRiftCore(divergenceNoise, point.x, point.z)
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

  @Unique
  private static void tfcrealworld$assignMapOceanBiome(
    Region.Point point,
    PNGDivergenceNoise divergenceNoise,
    ChooseBiomesAccessor accessor,
    Area blobArea,
    long rngSeed
  ) {
    final int rawDepth = Byte.toUnsignedInt(point.oceanDepth);
    if (rawDepth >= PNGAltitudeNoise.MAP_OCEAN_TRENCH_RAW_DEPTH) {
      point.biome = DEEP_OCEAN_TRENCH;
      return;
    }

    point.biome = tfcrealworld$baseOceanBiomeForMapDepth(point, rawDepth);

    if (divergenceNoise == null) {
      return;
    }

    if (MapTectonics.isNearOceanRidge(divergenceNoise, point.x, point.z)) {
      point.biome = OCEAN_RIDGE;
      return;
    }

    if (
      rawDepth != PNGAltitudeNoise.REEF_OCEAN_DEPTH &&
      tfcrealworld$isSubductionShelf(point, divergenceNoise)
    ) {
      final int areaSeed = blobArea.get(point.x, point.z);
      point.biome = accessor.tfcrealworld$invokeRandomSeededFrom(
        rngSeed,
        areaSeed,
        MAP_SUBDUCTION_SHELF_BIOMES
      );
    }
  }

  @Unique
  private static int tfcrealworld$baseOceanBiomeForMapDepth(
    Region.Point point,
    int rawDepth
  ) {
    if (rawDepth == PNGAltitudeNoise.REEF_OCEAN_DEPTH) {
      return point.volcanic() ? OCEANIC_VOLCANIC_ARC : OCEAN_REEF;
    }

    final int depthBucket = Byte.toUnsignedInt(
      PNGAltitudeNoise.bucketFromRawOceanDepth(rawDepth)
    );
    if (depthBucket <= 2) {
      return point.temperature > 12 && point.distanceToLand > 4
        ? OCEAN_ATOLLS
        : OCEAN;
    }
    if (depthBucket == PNGAltitudeNoise.ABYSSAL_OCEAN_DEPTH) {
      return DEEP_OCEAN;
    }
    if (depthBucket >= 4) {
      return point.temperature > 12 && point.distanceToLand > 3
        ? DEEP_OCEAN_ATOLLS
        : DEEP_OCEAN;
    }
    return OCEAN;
  }

  @Unique
  private static boolean tfcrealworld$isSubductionShelf(
    Region.Point point,
    PNGDivergenceNoise divergenceNoise
  ) {
    final int rawDepth = Byte.toUnsignedInt(point.oceanDepth);
    if (
      rawDepth == PNGAltitudeNoise.REEF_OCEAN_DEPTH ||
      PNGAltitudeNoise.bucketFromRawOceanDepth(rawDepth) != 2
    ) {
      return false;
    }
    if (point.divergence >= 0f) {
      return false;
    }
    if (
      !MapTectonics.isNearTrenchInfluence(
        divergenceNoise,
        point.x,
        point.z,
        TRENCH_SHELF_INFLUENCE_RADIUS
      )
    ) {
      return false;
    }
    return point.distanceToLand > 1 && point.distanceToLand < 8;
  }

  @Unique
  private static boolean tfcrealworld$skipOceanBiome(Region.Point point) {
    return (
      point.land() ||
      point.island() ||
      point.mountain() ||
      point.hotSpotAge > 0 ||
      point.barrierIsland()
    );
  }
}
