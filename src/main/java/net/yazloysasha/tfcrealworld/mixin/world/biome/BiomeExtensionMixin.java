package net.yazloysasha.tfcrealworld.mixin.world.biome;

import net.dries007.tfc.world.BiomeNoiseSampler;
import net.dries007.tfc.world.Seed;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.biome.BiomeNoise;
import net.dries007.tfc.world.surface.builder.NormalSurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Removes volcanic features from canyons and doline_canyons biomes when configured.
 */
@Mixin(BiomeExtension.class)
public class BiomeExtensionMixin {

  @Shadow
  @Final
  private ResourceKey<Biome> key;

  @Shadow
  private boolean volcanic;

  @Inject(method = "isVolcanic", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$overrideIsVolcanic(
    CallbackInfoReturnable<Boolean> cir
  ) {
    if (
      TFCRealWorldConfig.getCanyonsNotVolcanic() &&
      volcanic &&
      (key.location().getPath().equals("canyons") ||
        key.location().getPath().equals("doline_canyons"))
    ) {
      cir.setReturnValue(false);
    }
  }

  @Inject(method = "createNoiseSampler", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$overrideCreateNoiseSampler(
    Seed seed,
    CallbackInfoReturnable<@Nullable BiomeNoiseSampler> cir
  ) {
    if (
      TFCRealWorldConfig.getCanyonsNotVolcanic() &&
      volcanic &&
      (key.location().getPath().equals("canyons") ||
        key.location().getPath().equals("doline_canyons"))
    ) {
      // Return original noise without volcanoes
      if (key.location().getPath().equals("canyons")) {
        cir.setReturnValue(
          BiomeNoiseSampler.fromHeightNoise(
            BiomeNoise.canyons(seed.seed(), -2, 40)
          )
        );
      } else if (key.location().getPath().equals("doline_canyons")) {
        cir.setReturnValue(
          BiomeNoiseSampler.fromHeightNoise(
            BiomeNoise.bowlDolines(
              seed.seed(),
              BiomeNoise.canyons(seed.seed(), -2, 34),
              15
            )
          )
        );
      }
    }
  }

  @Inject(method = "createSurfaceBuilder", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$overrideCreateSurfaceBuilder(
    Seed seed,
    CallbackInfoReturnable<SurfaceBuilder> cir
  ) {
    if (
      TFCRealWorldConfig.getCanyonsNotVolcanic() &&
      volcanic &&
      (key.location().getPath().equals("canyons") ||
        key.location().getPath().equals("doline_canyons"))
    ) {
      cir.setReturnValue(NormalSurfaceBuilder.INSTANCE.apply(seed));
    }
  }
}
