package net.yazloysasha.tfcrealworld.mixin.world.region;

import java.util.List;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RiverEdge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = Region.class, remap = false)
public interface RegionAccessor {
  @Invoker("setRivers")
  void tfcrealworld$setRivers(List<RiverEdge> rivers);
}
