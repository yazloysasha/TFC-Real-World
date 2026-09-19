package net.yazloysasha.tfcrealworld.mixin.firmaciv;

import com.alekiponi.firmaciv.common.item.AbstractNavItem;
import net.minecraft.world.phys.Vec3;
import net.yazloysasha.tfcrealworld.compat.FirmaCivCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractNavItem.class, remap = false)
public class AbstractNavItemMixin {

  @Inject(method = "getNavLocation", at = @At("HEAD"), cancellable = true)
  private static void tfcrealworld$getNavLocation(
    Vec3 position,
    CallbackInfoReturnable<double[]> cir
  ) {
    cir.setReturnValue(FirmaCivCompat.getNavLocation(position));
  }
}
