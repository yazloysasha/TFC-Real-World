package net.yazloysasha.tfcrealworld.mixin.utils.tfg;

import net.dries007.tfc.util.climate.Climate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import su.terrafirmagreg.core.utils.SnowCorrection;

/**
 * TerraFirmaGreg decides snow for a whole chunk from one random column, so a
 * mountainside turns white or bare chunk by chunk. A column follows that
 * decision only if its own temperature agrees.
 */
@Mixin(value = SnowCorrection.class, remap = false)
public abstract class TfgSnowCorrectionMixin {

  /** The correction's own limit for new snow. */
  @Unique
  private static final float SNOW_BELOW = -2f;

  /** The correction's own limit for melting. */
  @Unique
  private static final float MELT_ABOVE = 0.5f;

  @Inject(
    method = "handleSnowAccumulation",
    at = @At("HEAD"),
    cancellable = true
  )
  private static void tfcrealworld$snowOnlyWhereCold(
    ServerLevel level,
    BlockPos surfacePos,
    CallbackInfo ci
  ) {
    final BlockPos ground = level.getHeightmapPos(
      Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
      surfacePos
    );
    if (Climate.getTemperature(level, ground) >= SNOW_BELOW) {
      ci.cancel();
    }
  }

  @Inject(method = "removeSnowAt", at = @At("HEAD"), cancellable = true)
  private static void tfcrealworld$meltOnlyWhereWarm(
    ServerLevel level,
    BlockPos pos,
    CallbackInfo ci
  ) {
    if (Climate.getTemperature(level, pos) <= MELT_ABOVE) {
      ci.cancel();
    }
  }
}
