package net.yazloysasha.tfcrealworld.mixin.world;

import net.dries007.tfc.world.TFCChunkGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = TFCChunkGenerator.class, remap = false)
public class TFCChunkGeneratorMixin {

  @ModifyVariable(
    method = "<init>",
    at = @At("HEAD"),
    argsOnly = true,
    ordinal = 4,
    remap = false
  )
  private static boolean tfcrealworld$overrideFlatBedrock(boolean flatBedrock) {
    return TFCRealWorldConfig.FLAT_BEDROCK.get();
  }
}
