package net.yazloysasha.tfcrealworld.mixin.world.biome.tfe;

import com.newterraearth.tfe.world.NTEBiomeExtensionAccess;
import com.newterraearth.tfe.world.NTEBiomeNoise;
import com.newterraearth.tfe.world.volcano.NTECenteredFeatureBlendType;
import net.dries007.tfc.world.BiomeNoiseSampler;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BiomeExtension.class, remap = false)
public abstract class TfeBiomeExtensionMixin {

  @Shadow
  @Final
  private ResourceKey<Biome> key;

  @Unique
  private boolean tfcrealworld$isCanyonBiome() {
    final String path = key.location().getPath();
    return path.equals("canyons") || path.equals("doline_canyons");
  }

  @Unique
  private boolean tfcrealworld$shouldStripCanyonVolcanoes() {
    return (
      TFCRealWorldConfig.CANYONS_NOT_VOLCANIC.get() &&
      tfcrealworld$isCanyonBiome()
    );
  }

  @Inject(method = "hasCinderCones", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$overrideHasCinderCones(
    CallbackInfoReturnable<Boolean> cir
  ) {
    if (tfcrealworld$shouldStripCanyonVolcanoes()) {
      cir.setReturnValue(false);
    }
  }

  @Inject(method = "isVolcanic", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$overrideIsVolcanic(
    CallbackInfoReturnable<Boolean> cir
  ) {
    if (tfcrealworld$shouldStripCanyonVolcanoes()) {
      cir.setReturnValue(false);
    }
  }

  @Inject(method = "createNoiseSampler", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$overrideCreateNoiseSampler(
    long seed,
    CallbackInfoReturnable<@Nullable BiomeNoiseSampler> cir
  ) {
    if (!tfcrealworld$shouldStripCanyonVolcanoes()) {
      return;
    }
    tfcrealworld$clearCenteredCinder();
    final String path = key.location().getPath();
    if (path.equals("canyons")) {
      cir.setReturnValue(
        BiomeNoiseSampler.fromHeightNoise(BiomeNoise.canyons(seed, -2, 40))
      );
    } else if (path.equals("doline_canyons")) {
      cir.setReturnValue(
        BiomeNoiseSampler.fromHeightNoise(NTEBiomeNoise.dolineCanyons(seed))
      );
    }
  }

  @Inject(method = "createSurfaceBuilder", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$overrideCreateSurfaceBuilder(
    long seed,
    CallbackInfoReturnable<SurfaceBuilder> cir
  ) {
    if (tfcrealworld$shouldStripCanyonVolcanoes()) {
      tfcrealworld$clearCenteredCinder();
      cir.setReturnValue(NormalSurfaceBuilder.INSTANCE.apply(seed));
    }
  }

  @Unique
  private void tfcrealworld$clearCenteredCinder() {
    if (this instanceof NTEBiomeExtensionAccess access) {
      access.tfe$setCenteredFeatureBlendType(NTECenteredFeatureBlendType.NONE);
    }
  }
}
