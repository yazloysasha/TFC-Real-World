package net.yazloysasha.tfcrealworld.mixin.client.auroras;

import auroras.util.AHelpers;
import net.yazloysasha.tfcrealworld.compat.AurorasCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = AHelpers.class, remap = false, priority = 1500)
public class AHelpersMixin {

  @ModifyVariable(
    method = "angleFromPole(Lauroras/util/AuroraData;Lnet/minecraft/world/level/Level;DD)D",
    at = @At("HEAD"),
    ordinal = 0,
    argsOnly = true
  )
  private static double tfcrealworld$remapWorldZForAngleFromPole(
    double worldZ
  ) {
    return AurorasCompat.toVirtualClassicZ(worldZ);
  }
}
