package net.yazloysasha.tfcrealworld.mixin.client.overworld;

import net.dries007.tfc.client.overworld.SolarCalculator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.types.CachedSolarCalculation;
import net.yazloysasha.tfcrealworld.util.projection.ProjectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SolarCalculator.class)
public class SolarCalculatorMixin {

  private static final ThreadLocal<CachedSolarCalculation> CALCULATION_CACHE =
    new ThreadLocal<>();

  private static int transformCoordForLatitude(int value, int tileSize) {
    int tileRadius = tileSize / 2;
    int tileValue = (int) Math.floor((value + tileRadius) / (double) tileSize);
    int localValue = value - tileValue * tileSize;

    if (Math.floorMod(tileValue, 2) != 0) {
      localValue = -localValue;
    }

    return Math.clamp(localValue, -tileRadius, tileRadius);
  }

  private static int[] transformCoords(
    int z,
    int horizontalTileSize,
    int verticalTileSize
  ) {
    return new int[] {
      transformCoordForLatitude(z, horizontalTileSize),
      transformCoordForLatitude(z, verticalTileSize),
    };
  }

  private static double[] getGeographicCoords(int z) {
    int horizontalTileSize = TFCRealWorldConfig.SPEC != null
      ? TFCRealWorldConfig.HORIZONTAL_TILE_SIZE.get()
      : TFCRealWorldConfig.DEFAULT_TILE_SIZE;
    int verticalTileSize = TFCRealWorldConfig.SPEC != null
      ? TFCRealWorldConfig.VERTICAL_TILE_SIZE.get()
      : TFCRealWorldConfig.DEFAULT_TILE_SIZE;

    CachedSolarCalculation cached = CALCULATION_CACHE.get();
    if (
      cached != null &&
      cached.z() == z &&
      cached.horizontalTileSize() == horizontalTileSize &&
      cached.verticalTileSize() == verticalTileSize
    ) {
      return cached.geoCoords();
    }

    int[] transformedCoords = transformCoords(
      z,
      horizontalTileSize,
      verticalTileSize
    );
    double[] geoCoords = ProjectionManager.minecraftToGeographic(
      transformedCoords[0],
      transformedCoords[1]
    );

    CALCULATION_CACHE.set(
      new CachedSolarCalculation(
        z,
        horizontalTileSize,
        verticalTileSize,
        geoCoords
      )
    );

    return geoCoords;
  }

  @Inject(method = "getLatitude", at = @At("HEAD"), cancellable = true)
  private static void tfcrealworld$transformForLatitude(
    int z,
    float hemisphereScale,
    CallbackInfoReturnable<Float> cir
  ) {
    double[] geoCoords = getGeographicCoords(z);
    float latitude = (float) Math.toRadians(geoCoords[1]);

    cir.setReturnValue(latitude);
  }

  @Inject(
    method = "getInNorthernHemisphere(IF)Z",
    at = @At("HEAD"),
    cancellable = true
  )
  private static void tfcrealworld$transformForHemisphere(
    int z,
    float hemisphereScale,
    CallbackInfoReturnable<Boolean> cir
  ) {
    double[] geoCoords = getGeographicCoords(z);

    cir.setReturnValue(geoCoords[1] > 0);
  }
}
