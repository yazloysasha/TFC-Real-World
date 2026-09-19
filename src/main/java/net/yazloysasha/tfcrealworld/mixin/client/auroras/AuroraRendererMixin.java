package net.yazloysasha.tfcrealworld.mixin.client.auroras;

import auroras.util.AuroraRenderer;
import net.minecraft.client.player.LocalPlayer;
import net.yazloysasha.tfcrealworld.compat.AurorasCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = AuroraRenderer.class, remap = false, priority = 1500)
public class AuroraRendererMixin {

  @Redirect(
    method = "getNorthernness(Lauroras/util/AuroraData;Lnet/minecraft/client/Minecraft;Lnet/minecraft/world/level/Level;ID)D",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/client/player/LocalPlayer;getZ()D"
    ),
    remap = true
  )
  private double tfcrealworld$remapPlayerZForNorthernness(LocalPlayer player) {
    return AurorasCompat.toVirtualClassicZ(player.getZ());
  }
}
