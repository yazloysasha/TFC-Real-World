package net.yazloysasha.tfcrealworld.mixin.world.region.tfe;

import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.backport.HotspotGeneratorNoises;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RegionGenerator.class, remap = false, priority = 1100)
public class TfeRegionGeneratorReapplyHotspotsMixin {

  @Inject(method = "<init>", at = @At("TAIL"))
  private void tfcrealworld$reapplyMapHotspotNoisesAfterTfeInit(
    CallbackInfo ci
  ) {
    if (!TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get()) {
      return;
    }
    HotspotGeneratorNoises.reapplyMapNoises((RegionGenerator) (Object) this);
  }
}
