package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.noise.Cellular2D;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.minecraft.world.level.levelgen.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(
  targets = "net.dries007.tfc.world.region.RegionGenerator$Context",
  remap = false
)
public interface RegionGeneratorContextAccessor {
  @Accessor
  Region region();

  @Accessor
  RandomSource random();

  @Accessor
  Cellular2D.Cell regionCell();

  @Invoker("generator")
  RegionGenerator invokeGenerator();
}
