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
