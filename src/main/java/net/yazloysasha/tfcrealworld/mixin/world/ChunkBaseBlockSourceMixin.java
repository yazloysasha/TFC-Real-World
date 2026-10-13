package net.yazloysasha.tfcrealworld.mixin.world;

import net.dries007.tfc.world.ChunkBaseBlockSource;
import net.minecraft.world.level.block.state.BlockState;
import net.yazloysasha.tfcrealworld.world.MapLakeWater;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Aquifer cache misses call {@code isSalty()} outside column sampling.
 * The column position lets a lake biome answer from the map lake cells.
 */
@Mixin(value = ChunkBaseBlockSource.class, remap = false)
public class ChunkBaseBlockSourceMixin {

  @Inject(method = "modifyFluid", at = @At("HEAD"))
  private void tfcrealworld$enterLakeColumn(
    BlockState fluidOrAir,
    int x,
    int z,
    CallbackInfoReturnable<BlockState> cir
  ) {
    MapLakeWater.enterColumn(x, z);
  }

  @Inject(method = "modifyFluid", at = @At("RETURN"))
  private void tfcrealworld$leaveLakeColumn(
    BlockState fluidOrAir,
    int x,
    int z,
    CallbackInfoReturnable<BlockState> cir
  ) {
    MapLakeWater.leaveColumn();
  }
}
