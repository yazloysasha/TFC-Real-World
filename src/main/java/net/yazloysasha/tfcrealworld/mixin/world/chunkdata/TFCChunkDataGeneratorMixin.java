package net.yazloysasha.tfcrealworld.mixin.world.chunkdata;

import net.dries007.tfc.world.chunkdata.TFCChunkDataGenerator;
import net.dries007.tfc.world.noise.Noise2D;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.noise.koppen.KoppenBasedRainfallNoise;
import net.yazloysasha.tfcrealworld.world.noise.koppen.KoppenBasedTemperatureNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGKoppenNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGRainfallNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGTemperatureNoise;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
  private void tfcrealworld$replaceChunkClimateNoises(CallbackInfo ci) {
    if (!TFCRealWorldConfig.KOPPEN_FROM_MAP.get()) {
      return;
    }

    final int horizontalTileSize =
      TFCRealWorldConfig.HORIZONTAL_TILE_SIZE.get();
    final int verticalTileSize = TFCRealWorldConfig.VERTICAL_TILE_SIZE.get();

    final PNGKoppenNoise koppenNoise = new PNGKoppenNoise(
      horizontalTileSize,
      verticalTileSize
    );
    final PNGTemperatureNoise temperatureMapNoise = new PNGTemperatureNoise(
      horizontalTileSize,
      verticalTileSize
    );
    final PNGRainfallNoise rainfallMapNoise = new PNGRainfallNoise(
      horizontalTileSize,
      verticalTileSize
    );

    final float toGrid = 1f / (float) TFCRealWorld.GRID_WIDTH_IN_BLOCK;

    final Noise2D mapTemperature = new KoppenBasedTemperatureNoise(
      koppenNoise,
      temperatureMapNoise,
      rainfallMapNoise
    );
    final Noise2D mapRainfall = new KoppenBasedRainfallNoise(
      koppenNoise,
      temperatureMapNoise,
      rainfallMapNoise
    );

    this.temperatureNoise = (x, z) ->
      mapTemperature.noise(x * toGrid, z * toGrid);
    this.rainfallNoise = (x, z) -> mapRainfall.noise(x * toGrid, z * toGrid);
  }
}
