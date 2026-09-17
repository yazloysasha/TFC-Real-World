package net.yazloysasha.tfcrealworld.mixin.world.biome;

import net.dries007.tfc.world.biome.BiomeNoise;
import net.dries007.tfc.world.noise.Noise2D;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.registry.HotspotsNoiseRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BiomeNoise.class, remap = false)
public class BiomeNoiseMixin {

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

  private static void tfcrealworld$hotspotIntensityFromMap(
    byte age,
    long seed,
    CallbackInfoReturnable<Noise2D> cir
  ) {
    if (!TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get()) {
      return;
    }
    final var layout = HotspotsNoiseRegistry.biomeLayout();
    if (layout == null) {
      return;
    }
    cir.setReturnValue(layout.intensityNoise(age, seed));
  }
}
