package net.yazloysasha.tfcrealworld.mixin.world.biome.tfg;

import net.dries007.tfc.world.BiomeNoiseSampler;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.biome.BiomeNoise;
import net.dries007.tfc.world.surface.builder.NormalSurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.yazloysasha.tfcrealworld.compat.TfgCompat;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import su.terrafirmagreg.core.world.new_ow_wg.noise.TFGBiomeNoise;

/**
 * TFG canyons still carry cinder cones. Same option as 1.21.1
 * {@code BiomeExtensionMixin} / {@code canyons_not_volcanic}. Gated to TFG:
 * TFC 3 {@code tfc:canyons} stay volcanic when Core-Modern is absent.
 */
@Mixin(value = BiomeExtension.class, remap = false)
public abstract class TfgBiomeExtensionMixin {

  @Shadow
  @Final
  private ResourceKey<Biome> key;

  @Unique
  private boolean tfcrealworld$isCanyonBiome() {
    final String path = key.location().getPath();
    return path.equals("earth/canyons") || path.equals("earth/doline_canyons");
  }

  @Unique
  private boolean tfcrealworld$shouldStripCanyonVolcanoes() {
    return (
      TfgCompat.isModPresent() &&
      TFCRealWorldConfig.CANYONS_NOT_VOLCANIC.get() &&
      tfcrealworld$isCanyonBiome()
    );
  }

  @Inject(method = "tfg$hasCinderCones", at = @At("HEAD"), cancellable = true)
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
    final String path = key.location().getPath();
    if (path.equals("earth/canyons")) {
      cir.setReturnValue(
        BiomeNoiseSampler.fromHeightNoise(BiomeNoise.canyons(seed, -2, 40))
      );
    } else if (path.equals("earth/doline_canyons")) {
      cir.setReturnValue(
        BiomeNoiseSampler.fromHeightNoise(
          TFGBiomeNoise.bowlDolines(seed, BiomeNoise.canyons(seed, -2, 34), 15)
        )
      );
    }
  }

  @Inject(method = "createSurfaceBuilder", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$overrideCreateSurfaceBuilder(
    long seed,
    CallbackInfoReturnable<SurfaceBuilder> cir
  ) {
    if (tfcrealworld$shouldStripCanyonVolcanoes()) {
      cir.setReturnValue(NormalSurfaceBuilder.INSTANCE.apply(seed));
    }
  }
}
