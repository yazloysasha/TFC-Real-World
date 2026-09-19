package net.yazloysasha.tfcrealworld.mixin.client.auroras;

import auroras.util.AHelpers;
import net.minecraft.client.player.LocalPlayer;
import net.yazloysasha.tfcrealworld.compat.AurorasCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = AHelpers.class, remap = false, priority = 1500)
public class AHelpersMixin {

  @Redirect(
    method = "angleFromPole(Lauroras/util/AuroraData;Lnet/minecraft/world/level/Level;Lnet/minecraft/client/Minecraft;D)D",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/client/player/LocalPlayer;getZ()D"
    ),
    remap = true
  )
  private static double tfcrealworld$remapPlayerZForAngleFromPole(
    LocalPlayer player
  ) {
    return AurorasCompat.toVirtualClassicZ(player.getZ());
  }
}
