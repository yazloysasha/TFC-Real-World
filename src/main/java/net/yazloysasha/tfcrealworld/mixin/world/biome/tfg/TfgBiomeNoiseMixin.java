package net.yazloysasha.tfcrealworld.mixin.world.biome.tfg;

import net.dries007.tfc.world.noise.Noise2D;
import net.yazloysasha.tfcrealworld.world.volcano.MapHotspotIntensity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import su.terrafirmagreg.core.world.new_ow_wg.noise.TFGBiomeNoise;

/** Shield volcanoes rise where the tectonics map has hotspots. */
@Mixin(value = TFGBiomeNoise.class, remap = false)
public class TfgBiomeNoiseMixin {

  @Inject(method = "activeHotSpots", at = @At("HEAD"), cancellable = true)
  private static void tfcrealworld$activeHotSpotsFromMap(
    long seed,
    CallbackInfoReturnable<Noise2D> cir
  ) {
    tfcrealworld$fromMap(MapHotspotIntensity.ofAge((byte) 1, seed), cir);
  }

  @Inject(method = "dormantHotSpots", at = @At("HEAD"), cancellable = true)
  private static void tfcrealworld$dormantHotSpotsFromMap(
    long seed,
    CallbackInfoReturnable<Noise2D> cir
  ) {
    tfcrealworld$fromMap(MapHotspotIntensity.ofAge((byte) 2, seed), cir);
  }

  @Inject(method = "extinctHotSpots", at = @At("HEAD"), cancellable = true)
  private static void tfcrealworld$extinctHotSpotsFromMap(
    long seed,
    CallbackInfoReturnable<Noise2D> cir
  ) {
    tfcrealworld$fromMap(MapHotspotIntensity.ofAge((byte) 3, seed), cir);
  }

  @Inject(method = "ancientHotSpots", at = @At("HEAD"), cancellable = true)
  private static void tfcrealworld$ancientHotSpotsFromMap(
    long seed,
    CallbackInfoReturnable<Noise2D> cir
  ) {
    tfcrealworld$fromMap(MapHotspotIntensity.ofAge((byte) 4, seed), cir);
  }

  @Inject(method = "hotSpotIntensity", at = @At("HEAD"), cancellable = true)
  private static void tfcrealworld$hotSpotIntensityFromMap(
    long seed,
    CallbackInfoReturnable<Noise2D> cir
  ) {
    tfcrealworld$fromMap(MapHotspotIntensity.ofAnyAge(seed), cir);
  }

  @Unique
  private static void tfcrealworld$fromMap(
    Noise2D noise,
    CallbackInfoReturnable<Noise2D> cir
  ) {
    if (noise != null) {
      cir.setReturnValue(noise);
    }
  }
}
