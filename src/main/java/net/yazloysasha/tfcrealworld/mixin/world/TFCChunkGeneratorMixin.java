package net.yazloysasha.tfcrealworld.mixin.world;

import net.dries007.tfc.world.TFCChunkGenerator;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.yazloysasha.tfcrealworld.util.helpers.WorldSeedHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hands the level seed to the region generator made in this call. */
@Mixin(value = TFCChunkGenerator.class, remap = false)
public class TFCChunkGeneratorMixin {

  @Inject(method = "initRandomState", at = @At("HEAD"))
  private void tfcrealworld$holdLevelSeed(
    ChunkMap chunkMap,
    ServerLevel level,
    CallbackInfo ci
  ) {
    WorldSeedHolder.setSeed(level.getSeed());
  }
}
