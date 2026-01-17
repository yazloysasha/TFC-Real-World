package net.yazloysasha.tfcrealworld.mixin.world.biome;

import net.dries007.tfc.world.BiomeNoiseSampler;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.biome.BiomeNoise;
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
  private boolean volcanic;

  private boolean isCanyonBiome() {
    String biomePath = key.location().getPath();
    return biomePath.equals("canyons");
  }

  private boolean shouldRemoveCinderCones() {
    return (
      TFCRealWorldConfig.CANYONS_NOT_VOLCANIC.get() &&
      volcanic &&
      isCanyonBiome()
    );
  }

  @Inject(method = "isVolcanic", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$overrideIsVolcanic(
    CallbackInfoReturnable<Boolean> cir
  ) {
    if (shouldRemoveCinderCones()) {
      cir.setReturnValue(false);
    }
  }

  @Inject(method = "createNoiseSampler", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$overrideCreateNoiseSampler(
    long seed,
    CallbackInfoReturnable<@Nullable BiomeNoiseSampler> cir
  ) {
    if (shouldRemoveCinderCones()) {
      cir.setReturnValue(
        BiomeNoiseSampler.fromHeightNoise(BiomeNoise.canyons(seed, -2, 40))
      );
    }
  }
}
