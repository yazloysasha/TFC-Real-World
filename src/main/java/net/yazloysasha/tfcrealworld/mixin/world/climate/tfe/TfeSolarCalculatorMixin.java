package net.yazloysasha.tfcrealworld.mixin.world.climate.tfe;

import com.newterraearth.tfe.api.climate.NTE121SolarCalculator;
import net.yazloysasha.tfcrealworld.util.projection.ProjectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * TerraFirmaEarth's sun, and the daily temperature that follows it, read the
 * latitude here from TFC's classic Z. The world's latitude is the map's.
 */
@Mixin(value = NTE121SolarCalculator.class, remap = false)
public class TfeSolarCalculatorMixin {

  @Inject(method = "getLatitude", at = @At("HEAD"), cancellable = true)
  private static void tfcrealworld$latitudeOfTheMap(
    int z,
    float hemisphereScale,
    CallbackInfoReturnable<Float> cir
  ) {
    if (hemisphereScale == 0) {
      return;
    }
    cir.setReturnValue(
      (float) Math.toRadians(ProjectionManager.resolveLatitudeFromWorldZ(z))
    );
  }
}
