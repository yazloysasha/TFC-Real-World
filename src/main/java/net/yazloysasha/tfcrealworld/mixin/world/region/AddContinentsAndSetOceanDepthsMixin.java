package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.AddContinentsAndSetOceanDepths;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces tectonic continent logic when generating from a map: land mask from the
 * continent map only, without rift seas or divergence-based ocean depth classes.
 * Ocean depth from ETOPO is applied later when {@code ALTITUDE_FROM_MAP} is enabled.
 */
@Mixin(value = AddContinentsAndSetOceanDepths.class, remap = false)
public class AddContinentsAndSetOceanDepthsMixin {

  private static final double LAND_THRESHOLD = 4.4;
  private static final double CONTINENTAL_SHELF_THRESHOLD = 3.3;

  @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$applyContinentsFromMapOnly(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    if (!TFCRealWorldConfig.CONTINENT_FROM_MAP.get()) {
      return;
    }

    final RegionGenerator generator = context.generator();
    final boolean altitudeFromMap = TFCRealWorldConfig.ALTITUDE_FROM_MAP.get();

    for (final Region.Point point : context.region.points()) {
      final double continent =
        generator.continentNoise.noise(point.x, point.z) *
        generator.continentFactor(point);

      if (continent > LAND_THRESHOLD) {
        point.setLand();
      } else if (!altitudeFromMap) {
        if (continent > CONTINENTAL_SHELF_THRESHOLD) {
          point.oceanDepth = 2;
        } else {
          point.oceanDepth = 4;
        }
      }
    }

    ci.cancel();
  }
}
