package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.region.RegionGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = RegionGenerator.class, remap = false)
public interface RegionGeneratorAccessor {
  @Accessor("seed")
  long tfcrealworld$seed();

  @Accessor("temperatureNoise")
  Noise2D tfcrealworld$temperatureNoise();

  @Accessor("rainfallNoise")
  Noise2D tfcrealworld$rainfallNoise();
}
