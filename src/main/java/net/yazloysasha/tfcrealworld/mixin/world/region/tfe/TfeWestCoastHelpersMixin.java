package net.yazloysasha.tfcrealworld.mixin.world.region.tfe;

import com.newterraearth.tfe.world.NTE121ClimateHelpers;
import net.dries007.tfc.world.region.Region;
import net.yazloysasha.tfcrealworld.world.backport.WestCoastFromMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = NTE121ClimateHelpers.class, remap = false)
public class TfeWestCoastHelpersMixin {

  @Inject(method = "getDistanceToWestCoast", at = @At("RETURN"))
  private static void tfcrealworld$replaceWestCoastWithMap(
    Region region,
    int temperatureScale,
    CallbackInfoReturnable<byte[]> cir
  ) {
    WestCoastFromMap.fill(region, cir.getReturnValue());
  }
}
