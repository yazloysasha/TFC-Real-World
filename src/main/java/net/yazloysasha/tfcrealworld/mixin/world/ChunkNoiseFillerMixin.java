package net.yazloysasha.tfcrealworld.mixin.world;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import net.dries007.tfc.world.ChunkNoiseFiller;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.river.RiverInfo;
import net.yazloysasha.tfcrealworld.world.SeaColumns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A column of the sea is no river: see {@link SeaColumns}. */
@Mixin(value = ChunkNoiseFiller.class, remap = false, priority = 1500)
public abstract class ChunkNoiseFillerMixin {

  @Unique
  private static final String HAS_RIVERS =
    "Lnet/dries007/tfc/world/biome/BiomeExtension;hasRivers()Z";

  /** Whether the column being filled is the sea. */
  @Unique
  private boolean tfcrealworld$sea;

  @Inject(method = "updateLocalCaches", at = @At("HEAD"))
  private void tfcrealworld$findSea(
    Object2DoubleMap<BiomeExtension> biomeWeights,
    BiomeExtension biomeAt,
    RiverInfo info,
    double height,
    CallbackInfo ci
  ) {
    tfcrealworld$sea = SeaColumns.isSea(biomeWeights);
  }

  @Redirect(
    method = "updateLocalCaches",
    at = @At(value = "INVOKE", target = HAS_RIVERS)
  )
  private boolean tfcrealworld$noRiverBiomeAtSea(BiomeExtension biome) {
    return !tfcrealworld$sea && biome.hasRivers();
  }

  @Redirect(
    method = "fillColumn",
    at = @At(value = "INVOKE", target = HAS_RIVERS)
  )
  private boolean tfcrealworld$noRiverFlowAtSea(BiomeExtension biome) {
    return !tfcrealworld$sea && biome.hasRivers();
  }
}
