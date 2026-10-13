package net.yazloysasha.tfcrealworld.mixin.world.chunkdata;

import net.dries007.tfc.world.chunkdata.TFCChunkDataGenerator;
import net.dries007.tfc.world.noise.Noise2D;
import net.minecraft.util.Mth;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGRainfallNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGTemperatureNoise;
import net.yazloysasha.tfcrealworld.world.region.ClimateScale;
import net.yazloysasha.tfcrealworld.world.region.RegionCoords;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * TFC 2 takes the climate of a chunk from noises of its own, not from the
 * region points: with climate from the map they read the maps, at the
 * grid cell centred on the block.
 */
@Mixin(value = TFCChunkDataGenerator.class, remap = false)
public class TFCChunkDataGeneratorMixin {

  @Shadow
  @Final
  @Mutable
  private Noise2D temperatureNoise;

  @Shadow
  @Final
  @Mutable
  private Noise2D rainfallNoise;

  @Inject(
    method = "<init>(Lnet/dries007/tfc/world/biome/BiomeSourceExtension$Settings;)V",
    at = @At("TAIL")
  )
  private void tfcrealworld$climateFromMap(CallbackInfo ci) {
    if (!TFCRealWorldConfig.CLIMATE_FROM_MAP.get()) {
      return;
    }
    final int horizontalScale = TFCRealWorldConfig.HORIZONTAL_SCALE.get();
    final int verticalScale = TFCRealWorldConfig.VERTICAL_SCALE.get();
    final PNGTemperatureNoise temperature = new PNGTemperatureNoise(
      horizontalScale,
      verticalScale
    );
    final PNGRainfallNoise rainfall = new PNGRainfallNoise(
      horizontalScale,
      verticalScale
    );
    temperatureNoise = (x, z) ->
      ClimateScale.temperature(
        (float) temperature.noise(
          tfcrealworld$toGrid(x),
          tfcrealworld$toGrid(z)
        )
      );
    rainfallNoise = (x, z) ->
      Mth.clamp(
        (float) rainfall.noise(tfcrealworld$toGrid(x), tfcrealworld$toGrid(z)),
        0,
        500
      );
  }

  /** The corner of the grid cell centred on a block. */
  @Unique
  private static double tfcrealworld$toGrid(float block) {
    return (double) block / RegionCoords.GRID_WIDTH_IN_BLOCK - 0.5;
  }
}
