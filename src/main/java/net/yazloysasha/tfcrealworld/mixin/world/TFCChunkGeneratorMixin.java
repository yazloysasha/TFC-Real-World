package net.yazloysasha.tfcrealworld.mixin.world;

import net.dries007.tfc.world.TFCChunkGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TFCChunkGenerator.class, remap = false)
public class TFCChunkGeneratorMixin {

  @Redirect(
    method = "makeBedrock",
    at = @At(
      value = "FIELD",
      target = "Lnet/dries007/tfc/world/TFCChunkGenerator;flatBedrock:Z",
      opcode = org.objectweb.asm.Opcodes.GETFIELD
    )
  )
  private boolean tfcrealworld$overrideFlatBedrock(TFCChunkGenerator instance) {
    return TFCRealWorldConfig.FLAT_BEDROCK.get();
  }
}
