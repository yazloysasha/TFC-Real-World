package net.yazloysasha.tfcrealworld.mixin.world.biome;

import net.dries007.tfc.world.BiomeNoiseSampler;
import net.dries007.tfc.world.Seed;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.biome.BiomeNoise;
import net.dries007.tfc.world.biome.TFCBiomes;
import net.dries007.tfc.world.surface.builder.NormalSurfaceBuilder;
import net.dries007.tfc.world.surface.builder.SurfaceBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.MapLakeWater;
import net.yazloysasha.tfcrealworld.world.surface.FrozenWaterSurfaceBuilder;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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

  @Unique
  private boolean tfcrealworld$isCanyonBiome() {
    String biomePath = key.location().getPath();
    return biomePath.equals("canyons") || biomePath.equals("doline_canyons");
  }

  @Unique
  private boolean tfcrealworld$shouldRemoveCinderCones() {
    return (
      TFCRealWorldConfig.CANYONS_NOT_VOLCANIC.get() &&
      hasCinderCones &&
      tfcrealworld$isCanyonBiome()
    );
  }

  /**
   * A lake biome takes its salinity from the map lake cells around the column
   * being sampled. Other biomes (oceans, land) stay vanilla, so TFC's own
   * coastal rules still apply around a lake.
   */
  @Inject(method = "isSalty", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$saltLakeIsSalty(
    CallbackInfoReturnable<Boolean> cir
  ) {
    if (
      !MapLakeWater.isColumnOpen() ||
      !MapLakeWater.isLakeBiome((BiomeExtension) (Object) this)
    ) {
      return;
    }
    final Boolean salty = MapLakeWater.saltyFlagOverride();
    if (salty != null) {
      cir.setReturnValue(salty);
    }
  }

  @Inject(method = "hasCinderCones", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$overrideHasCinderCones(
    CallbackInfoReturnable<Boolean> cir
  ) {
    if (tfcrealworld$shouldRemoveCinderCones()) {
      cir.setReturnValue(false);
    }
  }

  @Inject(method = "createNoiseSampler", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$overrideCreateNoiseSampler(
    Seed seed,
    CallbackInfoReturnable<@Nullable BiomeNoiseSampler> cir
  ) {
    if (tfcrealworld$shouldRemoveCinderCones()) {
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
    if (tfcrealworld$shouldRemoveCinderCones()) {
      cir.setReturnValue(NormalSurfaceBuilder.INSTANCE.apply(seed));
    }
  }

  /**
   * Water freezes over, with icebergs, in every biome where vanilla's sea
   * does. A meltwater lake is open water by its nature.
   */
  @Inject(
    method = "createSurfaceBuilder",
    at = @At("RETURN"),
    cancellable = true
  )
  private void tfcrealworld$frozenWater(
    Seed seed,
    CallbackInfoReturnable<SurfaceBuilder> cir
  ) {
    final BiomeExtension biome = (BiomeExtension) (Object) this;
    if (biome != TFCBiomes.MELTWATER_LAKE) {
      cir.setReturnValue(
        new FrozenWaterSurfaceBuilder(cir.getReturnValue(), seed)
      );
    }
  }
}
