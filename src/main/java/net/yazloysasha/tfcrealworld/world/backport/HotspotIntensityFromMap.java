package net.yazloysasha.tfcrealworld.world.backport;

import net.dries007.tfc.world.noise.Noise2D;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.volcano.MapHotspotNoise;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public final class HotspotIntensityFromMap {

  private HotspotIntensityFromMap() {}

  public static void apply(
    byte age,
    long seed,
    CallbackInfoReturnable<Noise2D> cir
  ) {
    if (!TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get()) {
      return;
    }
    cir.setReturnValue(MapHotspotNoise.forAge(age, seed));
  }

  public static void applyCombined(
    long seed,
    CallbackInfoReturnable<Noise2D> cir
  ) {
    if (!TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get()) {
      return;
    }
    cir.setReturnValue(MapHotspotNoise.combined(seed));
  }
}
