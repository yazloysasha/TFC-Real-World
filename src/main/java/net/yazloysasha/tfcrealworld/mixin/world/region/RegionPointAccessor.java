package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.Region;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Region.Point.class, remap = false)
public interface RegionPointAccessor {
  @Accessor("flags")
  short tfcrealworld$getFlags();

  @Accessor("flags")
  void tfcrealworld$setFlags(short flags);
}
