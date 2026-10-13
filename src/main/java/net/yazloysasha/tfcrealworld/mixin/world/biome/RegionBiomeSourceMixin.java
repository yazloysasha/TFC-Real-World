package net.yazloysasha.tfcrealworld.mixin.world.biome;

import net.dries007.tfc.world.biome.BiomeSourceExtension;
import net.dries007.tfc.world.biome.RegionBiomeSource;
import net.minecraft.core.Registry;
import net.minecraft.world.level.biome.Biome;
import net.yazloysasha.tfcrealworld.util.helpers.WorldSeedHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hands the level seed to the region generator made in this call. */
@Mixin(value = RegionBiomeSource.class, remap = false)
public class RegionBiomeSourceMixin {

  @Inject(
    method = "<init>",
    at = @At(
      value = "NEW",
      target = "net/dries007/tfc/world/region/RegionGenerator"
    )
  )
  private void tfcrealworld$holdLevelSeed(
    BiomeSourceExtension.Settings settings,
    Registry<Biome> biomeRegistry,
    CallbackInfo ci
  ) {
    WorldSeedHolder.setSeed(settings.seed());
  }
}
