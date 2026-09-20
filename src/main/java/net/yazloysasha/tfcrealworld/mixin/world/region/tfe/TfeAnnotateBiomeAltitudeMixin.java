package net.yazloysasha.tfcrealworld.mixin.world.region.tfe;

import com.newterraearth.tfe.world.region.NTEPointAccess;
import net.dries007.tfc.world.region.AnnotateBiomeAltitude;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.backport.TfeOceanDepth;
import net.yazloysasha.tfcrealworld.world.region.MapTectonics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AnnotateBiomeAltitude.class, remap = false, priority = 1500)
public class TfeAnnotateBiomeAltitudeMixin {

  @Inject(method = "apply", at = @At("HEAD"))
  private void tfcrealworld$tfeVolcanicMountainsFromTrench(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    if (!TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()) {
      return;
    }
    TfeOceanDepth.applyFromAltitudeMap(context.region, context.generator());
    if (!MapTectonics.isActive(context.generator())) {
      return;
    }
    final Region.Point[] data = context.region.data();
    for (final Region.Point point : data) {
      if (point == null || !point.land() || !point.mountain()) {
        continue;
      }
      final NTEPointAccess access = (NTEPointAccess) point;
      if (access.nte$getDivergence() < MapTectonics.TRENCH_DIVERGENCE) {
        access.nte$setVolcanic(true);
      }
    }
  }
}
