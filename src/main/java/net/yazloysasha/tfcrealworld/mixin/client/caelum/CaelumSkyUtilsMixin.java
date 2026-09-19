package net.yazloysasha.tfcrealworld.mixin.client.caelum;

import net.minecraft.world.level.Level;
import net.yazloysasha.tfcrealworld.compat.CaelumCompat;
import nuparu.caelum.client.SkyUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SkyUtils.class, remap = false, priority = 1500)
public class CaelumSkyUtilsMixin {

  @Shadow
  private static Double starLatitudeRotation;

  @Inject(
    method = "calculateStarLatitudeRotation",
    at = @At("HEAD"),
    cancellable = true,
    remap = false
  )
  private static void tfcrealworld$calculateStarLatitudeRotation(
    Level level,
    double z,
    CallbackInfoReturnable<Double> cir
  ) {
    final double rotation = CaelumCompat.calculateStarLatitudeRotation(
      level,
      z
    );
    starLatitudeRotation = rotation;
    cir.setReturnValue(rotation);
  }
}
