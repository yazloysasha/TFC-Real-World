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

@Mixin(value = BiomeExtension.class, remap = false)
public class BiomeExtensionMixin {

  @Shadow
  @Final
  private ResourceKey<Biome> key;

  @Shadow
  private boolean hasCinderCones;

  private boolean isCanyonBiome() {
    String biomePath = key.location().getPath();
    return biomePath.equals("canyons") || biomePath.equals("doline_canyons");
  }

  private boolean shouldRemoveCinderCones() {
    return (
      TFCRealWorldConfig.CANYONS_NOT_VOLCANIC.get() &&
      hasCinderCones &&
      isCanyonBiome()
    );
  }

  @Inject(method = "hasCinderCones", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$overrideHasCinderCones(
    CallbackInfoReturnable<Boolean> cir
  ) {
    if (shouldRemoveCinderCones()) {
      cir.setReturnValue(false);
    }
  }

  @Inject(method = "createNoiseSampler", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$overrideCreateNoiseSampler(
    Seed seed,
    CallbackInfoReturnable<@Nullable BiomeNoiseSampler> cir
  ) {
    if (shouldRemoveCinderCones()) {
      String biomePath = key.location().getPath();
      if (biomePath.equals("canyons")) {
        cir.setReturnValue(
          BiomeNoiseSampler.fromHeightNoise(
            BiomeNoise.canyons(seed.seed(), -2, 40)
          )
        );
      } else if (biomePath.equals("doline_canyons")) {
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
    if (shouldRemoveCinderCones()) {
      cir.setReturnValue(NormalSurfaceBuilder.INSTANCE.apply(seed));
    }
  }
}
