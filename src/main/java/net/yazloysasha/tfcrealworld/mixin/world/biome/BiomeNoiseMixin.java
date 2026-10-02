package net.yazloysasha.tfcrealworld.mixin.world.biome;

import net.dries007.tfc.world.TFCChunkGenerator;
import net.dries007.tfc.world.biome.BiomeNoise;
import net.dries007.tfc.world.noise.FastNoiseLite;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.registry.TectonicsRegistry;
import net.yazloysasha.tfcrealworld.world.tectonics.MapRidges;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BiomeNoise.class, remap = false)
public class BiomeNoiseMixin {

  /** Vanilla rift lake floor: from the valley floor down by this much. */
  @Unique
  private static final double RIFT_LAKE_FLOOR_DEPTH = 15;

  @Inject(method = "activeHotSpots", at = @At("HEAD"), cancellable = true)
  private static void tfcrealworld$activeHotSpotsFromMap(
    long seed,
    CallbackInfoReturnable<Noise2D> cir
  ) {
    tfcrealworld$hotspotIntensityFromMap((byte) 1, seed, cir);
  }

  @Inject(method = "dormantHotSpots", at = @At("HEAD"), cancellable = true)
  private static void tfcrealworld$dormantHotSpotsFromMap(
    long seed,
    CallbackInfoReturnable<Noise2D> cir
  ) {
    tfcrealworld$hotspotIntensityFromMap((byte) 2, seed, cir);
  }

  @Inject(method = "extinctHotSpots", at = @At("HEAD"), cancellable = true)
  private static void tfcrealworld$extinctHotSpotsFromMap(
    long seed,
    CallbackInfoReturnable<Noise2D> cir
  ) {
    tfcrealworld$hotspotIntensityFromMap((byte) 3, seed, cir);
  }

  @Inject(method = "ancientHotSpots", at = @At("HEAD"), cancellable = true)
  private static void tfcrealworld$ancientHotSpotsFromMap(
    long seed,
    CallbackInfoReturnable<Noise2D> cir
  ) {
    tfcrealworld$hotspotIntensityFromMap((byte) 4, seed, cir);
  }

  /**
   * Vanilla digs the rift lake along the cell edges of its own plate noise,
   * where vanilla also places rift biomes. With tectonics, rift lakes are the
   * map's lakes inside rift zones (continent.png lakes, e.g. Tanganyika,
   * Malawi, Baikal), so the lake floor covers the whole biome: vanilla's
   * valley floor depth and roughness, without the plate-edge profile.
   */
  @Inject(method = "riftValley", at = @At("HEAD"), cancellable = true)
  private static void tfcrealworld$riftLakeFromMap(
    long seed,
    int minHeightIn,
    int edgeHeightIn,
    boolean isLake,
    CallbackInfoReturnable<Noise2D> cir
  ) {
    if (
      !isLake ||
      !TFCRealWorldConfig.CONTINENT_FROM_MAP.get() ||
      !TFCRealWorldConfig.TECTONICS_FROM_MAP.get()
    ) {
      return;
    }
    final double floor = TFCChunkGenerator.SEA_LEVEL_Y + minHeightIn;
    cir.setReturnValue(
      new OpenSimplex2D(seed)
        .octaves(3)
        .spread(0.04f)
        .scaled(-8, 8)
        .addConstant(floor - RIFT_LAKE_FLOOR_DEPTH / 2)
    );
  }

  /**
   * Vanilla measures a ridge from the edges of its own plate noise, which
   * the map's ridges do not follow. With tectonics the crest, its seafloor
   * and its vents are measured from the real axis ({@link MapRidges}).
   */
  @Inject(
    method = "getOceanRidgeWarpedEdgeDistanceAndScale",
    at = @At("HEAD"),
    cancellable = true
  )
  private static void tfcrealworld$ridgeAlongMapAxis(
    double x,
    double y,
    long seed,
    boolean getDistanceToGaps,
    CallbackInfoReturnable<FastNoiseLite.Vector2> cir
  ) {
    if (
      !TFCRealWorldConfig.CONTINENT_FROM_MAP.get() ||
      !TFCRealWorldConfig.TECTONICS_FROM_MAP.get()
    ) {
      return;
    }
    final MapRidges ridges = TectonicsRegistry.ridges();
    if (ridges != null) {
      cir.setReturnValue(
        ridges.warpedAxisDistanceAndScale(x, y, seed, getDistanceToGaps)
      );
    }
  }

  @Unique
  private static void tfcrealworld$hotspotIntensityFromMap(
    byte age,
    long seed,
    CallbackInfoReturnable<Noise2D> cir
  ) {
    if (
      !TFCRealWorldConfig.CONTINENT_FROM_MAP.get() ||
      !TFCRealWorldConfig.TECTONICS_FROM_MAP.get() ||
      !TFCRealWorldConfig.VOLCANOES_FROM_MAP.get()
    ) {
      return;
    }
    final var layout = TectonicsRegistry.hotspotLayout();
    if (layout == null) {
      return;
    }
    cir.setReturnValue(layout.intensityNoise(age, seed));
  }
}
