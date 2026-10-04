package net.yazloysasha.tfcrealworld.mixin.world.biome.tfe;

import com.newterraearth.tfe.world.NTEBiomeNoise;
import com.newterraearth.tfe.world.noise.NTECellular2D;
import net.dries007.tfc.world.TFCChunkGenerator;
import net.dries007.tfc.world.noise.FastNoiseLite;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.registry.LevelSeedRegistry;
import net.yazloysasha.tfcrealworld.util.registry.TectonicsRegistry;
import net.yazloysasha.tfcrealworld.world.tectonics.MapRidges;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = NTEBiomeNoise.class, remap = false)
public class TfeBiomeNoiseMixin {

  /** The generator's rift lake floor: from the valley floor down by this. */
  @Unique
  private static final double RIFT_LAKE_FLOOR_DEPTH = 15;

  /**
   * The generator digs the rift lake along the cell edges of its own plate
   * noise, where it also places rift biomes. With tectonics, rift lakes are
   * the map's lakes inside rift zones (Tanganyika, Malawi, Baikal), so the
   * lake floor covers the whole biome: the valley floor depth and
   * roughness, without the plate-edge profile.
   */
  @Inject(method = "riftValley", at = @At("HEAD"), cancellable = true)
  private static void tfcrealworld$riftLakeFromMap(
    long seed,
    int minHeightIn,
    int edgeHeightIn,
    boolean isLake,
    CallbackInfoReturnable<Noise2D> cir
  ) {
    if (!isLake || !tfcrealworld$tectonicsFromMap()) {
      return;
    }
    final double floor = TFCChunkGenerator.SEA_LEVEL_Y + minHeightIn;
    cir.setReturnValue(
      new OpenSimplex2D(seed)
        .octaves(3)
        .spread(0.04f)
        .scaled(-8, 8)
        .add((x, z) -> floor - RIFT_LAKE_FLOOR_DEPTH / 2)
    );
  }

  /**
   * The generator measures a ridge from the edges of its own plate noise,
   * which the map's ridges do not follow. With tectonics the crest, its
   * seafloor and its vents are measured from the real axis.
   */
  @Inject(
    method = "oceanRidgeDistanceAndScale",
    at = @At("HEAD"),
    cancellable = true
  )
  private static void tfcrealworld$ridgeAlongMapAxis(
    double x,
    double z,
    NTECellular2D cellNoise,
    Noise2D baseFaultingNoise,
    Noise2D warpNoise,
    Noise2D bigWarpNoise,
    CallbackInfoReturnable<double[]> cir
  ) {
    if (!tfcrealworld$tectonicsFromMap()) {
      return;
    }
    final MapRidges ridges = TectonicsRegistry.ridges();
    if (ridges != null) {
      final FastNoiseLite.Vector2 axis = ridges.warpedAxisDistanceAndScale(
        x,
        z,
        LevelSeedRegistry.latest(),
        true
      );
      cir.setReturnValue(new double[] { axis.x, axis.y });
    }
  }

  @Unique
  private static boolean tfcrealworld$tectonicsFromMap() {
    return (
      TFCRealWorldConfig.CONTINENT_FROM_MAP.get() &&
      TFCRealWorldConfig.TECTONICS_FROM_MAP.get()
    );
  }
}
