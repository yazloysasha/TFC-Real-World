package net.yazloysasha.tfcrealworld.mixin.world.biome;

import net.dries007.tfc.world.TFCChunkGenerator;
import net.dries007.tfc.world.biome.BiomeNoise;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BiomeNoise.class, remap = false)
public class BiomeNoiseMixin {

  /**
   * With coastlines from the map, TFC 3's shore: up to 5 blocks above sea
   * level. TFC 2's, up to 1.8, is TFC 3's tidal flats.
   */
  @Inject(method = "shore", at = @At("HEAD"), cancellable = true)
  private static void tfcrealworld$tfc3Shore(
    long seed,
    CallbackInfoReturnable<Noise2D> cir
  ) {
    if (TFCRealWorldConfig.CONTINENT_FROM_MAP.get()) {
      cir.setReturnValue(
        new OpenSimplex2D(seed)
          .octaves(4)
          .spread(0.17f)
          .scaled(
            TFCChunkGenerator.SEA_LEVEL_Y,
            TFCChunkGenerator.SEA_LEVEL_Y + 5f
          )
      );
    }
  }
}
