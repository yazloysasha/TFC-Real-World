package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.Region;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(
  targets = "net.dries007.tfc.world.region.AddRiversAndLakes$RegionRiverGenerator",
  remap = false
)
public interface RegionRiverGeneratorAccessor {
  @Accessor("region")
  Region tfcrealworld$getRegion();
}
