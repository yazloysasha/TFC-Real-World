package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.AnnotateBoundaryTypes;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.registry.DivergenceNoiseRegistry;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGDivergenceNoise;
import net.yazloysasha.tfcrealworld.world.region.MapTectonics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AnnotateBoundaryTypes.class, remap = false)
public class AnnotateBoundaryTypesMixin {

  @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$applyDivergenceFromMap(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    if (!TFCRealWorldConfig.CONTINENT_FROM_MAP.get()) {
      return;
    }

    if (!TFCRealWorldConfig.TECTONICS_FROM_MAP.get()) {
      return;
    }

    final RegionGenerator generator = context.generator();

    if (MapTectonics.isActive(generator)) {
      final PNGDivergenceNoise divergenceNoise = DivergenceNoiseRegistry.get(
        generator
      );
      for (final Region.Point point : context.region.points()) {
        point.divergence = divergenceNoise.getDivergence(point.x, point.z);
      }
      ci.cancel();
    }
  }
}
