package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.minecraft.world.level.levelgen.RandomSource;
import net.yazloysasha.tfcrealworld.world.region.RegionContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shows each region task its context: see {@link RegionContext}. */
@Mixin(
  targets = "net.dries007.tfc.world.region.RegionGenerator$Context",
  remap = false
)
public abstract class RegionGeneratorContextMixin {

  @Shadow
  @Final
  Region region;

  @Shadow
  @Final
  RandomSource random;

  @Invoker("generator")
  abstract RegionGenerator tfcrealworld$generator();

  @Inject(
    method = "run(Lnet/dries007/tfc/world/region/RegionGenerator$Task;)V",
    at = @At("HEAD")
  )
  private void tfcrealworld$enter(CallbackInfo ci) {
    RegionContext.enter(region, random, tfcrealworld$generator());
  }

  @Inject(
    method = "run(Lnet/dries007/tfc/world/region/RegionGenerator$Task;)V",
    at = @At("RETURN")
  )
  private void tfcrealworld$leave(CallbackInfo ci) {
    RegionContext.leave();
  }
}
