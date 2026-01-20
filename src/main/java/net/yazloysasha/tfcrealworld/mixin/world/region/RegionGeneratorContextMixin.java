package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.util.helpers.RegionContextHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
  targets = "net.dries007.tfc.world.region.RegionGenerator$Context",
  remap = false
)
public abstract class RegionGeneratorContextMixin {

  @Shadow
  @Final
  Region region;

  @Invoker("generator")
  public abstract RegionGenerator tfcrealworld$invokeGenerator();

  @Inject(
    method = "run(Lnet/dries007/tfc/world/region/RegionGenerator$Task;)V",
    at = @At("HEAD")
  )
  private void tfcrealworld$setCurrentContext(
    RegionGenerator.Task task,
    CallbackInfo ci
  ) {
    RegionContextHolder.set(region, tfcrealworld$invokeGenerator());
  }

  @Inject(
    method = "run(Lnet/dries007/tfc/world/region/RegionGenerator$Task;)V",
    at = @At("RETURN")
  )
  private void tfcrealworld$clearCurrentContext(
    RegionGenerator.Task task,
    CallbackInfo ci
  ) {
    RegionContextHolder.clear();
  }
}
