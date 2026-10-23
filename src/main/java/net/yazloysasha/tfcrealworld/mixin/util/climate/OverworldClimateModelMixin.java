package net.yazloysasha.tfcrealworld.mixin.util.climate;

import net.dries007.tfc.util.climate.OverworldClimateModel;
import net.yazloysasha.tfcrealworld.util.projection.ProjectionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Vanilla's seasons swing by a triangle of Z: none at its equator,
 * {@code temperatureScale / 2}, and opposite on either side. The world's
 * equator lies by latitude, as the sun's does, so the seasons are read at the
 * classic Z of the latitude: none at the world's equator, and the right way
 * round in each hemisphere.
 */
@Mixin(value = OverworldClimateModel.class, remap = false)
public abstract class OverworldClimateModelMixin {

  @Shadow
  @Final
  protected float temperatureScale;

  @ModifyVariable(
    method = "calculateMonthlyTemperature",
    at = @At("HEAD"),
    argsOnly = true
  )
  private int tfcrealworld$classicZOfLatitude(int z) {
    if (temperatureScale == 0) {
      return z;
    }
    return (int) Math.round(
      ProjectionManager.virtualClassicZFromLatitude(
        ProjectionManager.resolveLatitudeFromWorldZ(z),
        (int) temperatureScale
      )
    );
  }
}
