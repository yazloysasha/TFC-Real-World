package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.AnnotateBaseLandHeight;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.world.region.MapRegionTasks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AnnotateBaseLandHeight.class, remap = false)
public class AnnotateBaseLandHeightMixin {

  @Inject(method = "apply", at = @At("TAIL"))
  private void tfcrealworld$baseLandHeightFromRelief(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    MapRegionTasks.baseLandHeight(context);
  }
}
