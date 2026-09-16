package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.AddContinentsAndSetOceanDepths;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.region.MapTectonics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
    final boolean mapTectonics = MapTectonics.isActive(generator);

    for (final Region.Point point : context.region.points()) {
      final double tectonicFeatures = mapTectonics
        ? MapTectonics.continentRiftAdjustment(point.divergence)
        : 0;

      final double continent =
        (generator.continentNoise.noise(point.x, point.z) + tectonicFeatures) *
        generator.continentFactor(point);

      if (continent > LAND_THRESHOLD) {
        point.setLand();
      } else if (!altitudeFromMap) {
        if (
          mapTectonics && point.divergence > MapTectonics.OCEAN_RIDGE_DIVERGENCE
        ) {
          point.oceanDepth = 3;
        } else if (
          mapTectonics && point.divergence < MapTectonics.TRENCH_DIVERGENCE
        ) {
          point.oceanDepth = 5;
        } else if (continent > CONTINENTAL_SHELF_THRESHOLD) {
          point.oceanDepth = 2;
        } else {
          point.oceanDepth = 4;
        }
      }
    }

    ci.cancel();
  }
}
