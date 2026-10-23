package net.yazloysasha.tfcrealworld.mixin.world.climate.tfe;

import com.newterraearth.tfe.world.NTEClimateSeasonModel;
import net.yazloysasha.tfcrealworld.util.projection.ProjectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * TerraFirmaEarth's seasons swing, as TFC 1.21's, by a triangle of Z: none at
 * its equator, {@code temperatureScale / 2}, and opposite on either side; its
 * hemisphere turns there too. The world's equator lies by latitude, so the
 * seasons are read at the classic Z of the latitude, and the hemisphere is
 * the latitude's.
 */
@Mixin(value = NTEClimateSeasonModel.class, remap = false)
public class TfeClimateSeasonModelMixin {

  @Unique
  private static final String SIGNED_AMPLITUDE =
    "Lcom/newterraearth/tfe/world/NTEClimateSeasonModel;signedSeasonalTemperatureAmplitude(IF)F";

  @Redirect(
    method = "seasonalTemperatureAmplitude",
    at = @At(value = "INVOKE", target = SIGNED_AMPLITUDE)
  )
  private static float tfcrealworld$amplitudeOfLatitude(
    int z,
    float temperatureScale
  ) {
    return tfcrealworld$signedAmplitudeOfLatitude(z, temperatureScale);
  }

  @Redirect(
    method = "seasonalTemperature",
    at = @At(value = "INVOKE", target = SIGNED_AMPLITUDE)
  )
  private static float tfcrealworld$temperatureOfLatitude(
    int z,
    float temperatureScale
  ) {
    return tfcrealworld$signedAmplitudeOfLatitude(z, temperatureScale);
  }

  @Inject(method = "isNorthernHemisphere", at = @At("HEAD"), cancellable = true)
  private static void tfcrealworld$hemisphereOfLatitude(
    int z,
    float temperatureScale,
    CallbackInfoReturnable<Boolean> cir
  ) {
    if (temperatureScale == 0) {
      return;
    }
    cir.setReturnValue(ProjectionManager.resolveLatitudeFromWorldZ(z) >= 0);
  }

  @Unique
  private static float tfcrealworld$signedAmplitudeOfLatitude(
    int z,
    float temperatureScale
  ) {
    final int classicZ =
      temperatureScale == 0
        ? z
        : (int) Math.round(
            ProjectionManager.virtualClassicZFromLatitude(
              ProjectionManager.resolveLatitudeFromWorldZ(z),
              (int) temperatureScale
            )
          );
    return TfeClimateSeasonModelInvoker.tfcrealworld$invokeSignedSeasonalTemperatureAmplitude(
      classicZ,
      temperatureScale
    );
  }
}
