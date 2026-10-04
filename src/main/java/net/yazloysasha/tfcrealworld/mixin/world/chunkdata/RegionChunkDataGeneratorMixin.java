package net.yazloysasha.tfcrealworld.mixin.world.chunkdata;

import net.dries007.tfc.world.chunkdata.RegionChunkDataGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * TFC scales a river's wet ground by its width up to its own widest river.
 * Map rivers can be wider, and count as the widest.
 */
@Mixin(value = RegionChunkDataGenerator.class, remap = false, priority = 500)
public class RegionChunkDataGeneratorMixin {

  @ModifyVariable(
    method = "adjustRiverRainfall",
    at = @At("HEAD"),
    ordinal = 2,
    argsOnly = true
  )
  private float tfcrealworld$capWidthInfluence(float widthInfluence) {
    return Math.min(widthInfluence, 1f);
  }
}
