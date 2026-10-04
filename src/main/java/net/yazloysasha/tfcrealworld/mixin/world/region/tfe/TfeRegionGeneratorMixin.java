package net.yazloysasha.tfcrealworld.mixin.world.region.tfe;

import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.Units;
import net.minecraft.util.Mth;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.registry.LevelSeedRegistry;
import net.yazloysasha.tfcrealworld.util.registry.TectonicsRegistry;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicsMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * TerraFirmaEarth asks the generator for the two things TFC 4 keeps on it:
 * how much of the continents is left near the world's edge, and the
 * divergence of the plates at a point.
 */
@Mixin(value = RegionGenerator.class, remap = false, priority = 1500)
public class TfeRegionGeneratorMixin {

  /** Map edges: how far past the scale the continents fade out. */
  @Unique
  private static final float FINITE_CONTINENTS_FADE_FROM_MAP = 1.01f;

  @Unique
  private static final float FINITE_CONTINENTS_FADE = 1.2f;

  @Inject(method = "nte$continentFactor", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$continentFactorFromMap(
    int gridX,
    int gridZ,
    CallbackInfoReturnable<Float> cir
  ) {
    if (!TFCRealWorldConfig.FINITE_CONTINENTS.get()) {
      return;
    }
    final float fade = TFCRealWorldConfig.CONTINENT_FROM_MAP.get()
      ? FINITE_CONTINENTS_FADE_FROM_MAP
      : FINITE_CONTINENTS_FADE;
    cir.setReturnValue(
      Math.min(
        tfcrealworld$edgeFactor(
          gridX * Units.GRID_WIDTH_IN_BLOCK,
          TFCRealWorldConfig.HORIZONTAL_SCALE.get(),
          fade
        ),
        tfcrealworld$edgeFactor(
          gridZ * Units.GRID_WIDTH_IN_BLOCK,
          TFCRealWorldConfig.VERTICAL_SCALE.get(),
          fade
        )
      )
    );
  }

  @Unique
  private static float tfcrealworld$edgeFactor(
    float block,
    int scale,
    float fade
  ) {
    return scale == 0
      ? 1f
      : Mth.clampedMap(Math.abs(block), scale, fade * scale, 1, 0);
  }

  /**
   * Divergence is a property of the boundary zone a point lies in: negative
   * in convergent zones, positive in divergent zones, zero elsewhere.
   */
  @Inject(
    method = "nte$getDivergence(II)D",
    at = @At("HEAD"),
    cancellable = true
  )
  private void tfcrealworld$divergenceFromTectonics(
    int gridX,
    int gridZ,
    CallbackInfoReturnable<Double> cir
  ) {
    final TectonicsMap tectonics = TectonicsRegistry.get(
      (RegionGenerator) (Object) this
    );
    if (tectonics != null) {
      cir.setReturnValue(
        (double) tectonics.classAtGrid(gridX, gridZ).boundary().divergence()
      );
    }
  }

  @Inject(method = "nte$setRootLevelSeed", at = @At("RETURN"))
  private void tfcrealworld$keepLevelSeed(long rootLevelSeed, CallbackInfo ci) {
    LevelSeedRegistry.register((RegionGenerator) (Object) this, rootLevelSeed);
  }
}
