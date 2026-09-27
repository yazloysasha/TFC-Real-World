package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.AddContinentsAndSetOceanDepths;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.registry.AltitudeNoiseRegistry;
import net.yazloysasha.tfcrealworld.util.registry.ContinentNoiseRegistry;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGAltitudeNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise;
import net.yazloysasha.tfcrealworld.world.region.MapTectonics;
import net.yazloysasha.tfcrealworld.world.region.TfcContinentNoiseThresholds;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code continent.png} → hard {@code setLand} only. Continuous shelf / trench
 * continuum (vanilla 4.4 / 3.3 thresholds) comes from {@code altitude.png} when
 * altitude-from-map is on. Divergence / map tectonics keep vanilla bucket
 * meanings; no Voronoi {@code distanceToEdge} catch-all when map tectonics are
 * active.
 * <p>
 * Reef depth (1) is not set here — vanilla comment: "Reef - 1 (Set later)".
 * Nearshore shelf (2) must exist so vanilla AddMountains barriers/arcs can
 * promote it to depth 1.
 */
@Mixin(value = AddContinentsAndSetOceanDepths.class, remap = false)
public class AddContinentsAndSetOceanDepthsMixin {

  @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$applyContinentsFromMapOnly(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    if (!TFCRealWorldConfig.CONTINENT_FROM_MAP.get()) {
      return;
    }

    final RegionGenerator generator = context.generator();
    final PNGContinentNoise continentMap = ContinentNoiseRegistry.get(
      generator
    );
    if (continentMap == null) {
      return;
    }

    final boolean mapTectonics = MapTectonics.isActive(generator);
    final PNGAltitudeNoise altitudeNoise =
      TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()
        ? AltitudeNoiseRegistry.get(generator)
        : null;

    for (final var point : context.region.points()) {
      final double tectonicFeatures = mapTectonics
        ? MapTectonics.continentRiftAdjustment(point.divergence)
        : tfcrealworld$vanillaRiftSeas(point.distanceToEdge, point.divergence);

      // Continuum for shelf/trench buckets: altitude map, else binary mask noise.
      final double continuum = altitudeNoise != null
        ? altitudeNoise.getContinentNoise(point.x, point.z)
        : continentMap.noise(point.x, point.z);

      final double continentFactor = generator.continentFactor(point);
      final double continent = (continuum + tectonicFeatures) * continentFactor;

      // Land membership is the binary mask — never altitude continuum / 4.4.
      if (continentMap.isLandAtGridHard(point.x, point.z)) {
        point.setLand();
      } else if (mapTectonics) {
        // Match vanilla ocean-depth priority (AddContinentsAndSetOceanDepths):
        //   ridge (div>0 near edge) BEFORE shelf — "Ocean ridges should override
        //   continental shelves in rifting areas"; then shelf; then trench
        //   (continent>3 && div<0); else abyssal.
        // No Voronoi edge catch-all: distanceToEdge here is already synthesized
        // from |divergence| in AnnotateBoundaryTypes (strong boundary → edge<2).
        // Polarity replaces Voronoi cell borders.
        if (point.divergence > 0 && point.distanceToEdge < 2) {
          point.oceanDepth = 3;
        } else if (
          continuum * continentFactor >
          TfcContinentNoiseThresholds.CONTINENTAL_SHELF
        ) {
          // Un-rifted continuum so weak-div nearshore stays shelf when ridge
          // did not fire (edge>=2).
          point.oceanDepth = 2;
        } else if (
          continent > TfcContinentNoiseThresholds.TRENCH_CONTINENT &&
          point.divergence < 0
        ) {
          point.oceanDepth = 5;
        } else {
          point.oceanDepth = 4;
        }
      } else if (point.divergence > 0 && point.distanceToEdge < 2) {
        point.oceanDepth = 3;
      } else if (continent > TfcContinentNoiseThresholds.CONTINENTAL_SHELF) {
        point.oceanDepth = 2;
      } else if (
        continent > TfcContinentNoiseThresholds.TRENCH_CONTINENT &&
        point.divergence < 0
      ) {
        point.oceanDepth = 5;
      } else if (
        point.distanceToEdge < 2 && !(point.divergence < 0 && continent > 2)
      ) {
        point.oceanDepth = 3;
      } else {
        point.oceanDepth = 4;
      }
    }

    ci.cancel();
  }

  /**
   * Vanilla {@code addRiftSeas} when map tectonics are off.
   */
  private static double tfcrealworld$vanillaRiftSeas(
    byte distanceToEdge,
    float divergence
  ) {
    if (distanceToEdge <= 5 && divergence > 0) {
      return (5 - distanceToEdge) * -0.12;
    }
    return 0;
  }
}
