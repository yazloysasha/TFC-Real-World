package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.AddContinentsAndSetOceanDepths;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.registry.ContinentNoiseRegistry;
import net.yazloysasha.tfcrealworld.util.registry.TectonicsRegistry;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise.ContinentBand;
import net.yazloysasha.tfcrealworld.world.region.TfcContinentNoiseThresholds;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicClass;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicClass.LandRelief;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicClass.Volcanism;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicsMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code continent.png} decides land vs water; {@code tectonics.png} decides
 * what kind of place it is. Land points get the volcanic flag from the class,
 * water points get {@code oceanDepth} (and the volcanic flag on arcs). Vanilla
 * ChooseBiomes turns those fields into biomes.
 * <p>
 * continent.png says what is an island (island band, also islets inside
 * ocean cells). tectonics.png says what kind: a mountainous island is plain
 * land with mountain relief; a low one is one of vanilla's two islands, on a
 * volcanic arc the vanilla volcanic island chain (built at sea, as vanilla
 * places arcs over subduction zones), elsewhere a vanilla island.
 * <p>
 * A lake of the map is land here: a region cell is far larger than the
 * map's lakes, so the biome layer floods exactly the map's lake pixels
 * ({@code MapLandOceanCorrectionLayer}), each into the lake of its own land.
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

    final TectonicsMap tectonics = TectonicsRegistry.get(generator);
    for (final var point : context.region.points()) {
      // A region point covers [x, x + 1): sample the centre of its cell.
      final ContinentBand band = continentMap.bandAtGridHard(
        point.x + 0.5,
        point.z + 0.5
      );
      if (tectonics != null) {
        tfcrealworld$applyTectonics(
          point,
          band,
          tectonics.classAtGrid(point.x, point.z),
          continentMap
        );
      } else {
        tfcrealworld$applyProcedural(point, band, continentMap, generator);
      }
    }

    ci.cancel();
  }

  @Unique
  private static void tfcrealworld$applyTectonics(
    Region.Point point,
    ContinentBand band,
    TectonicClass tectonicClass,
    PNGContinentNoise continentMap
  ) {
    final Volcanism volcanism = tectonicClass.volcanism();
    final boolean arc = volcanism == Volcanism.ARC;
    // A mountainous island is land like any other: its relief, coast and
    // volcanism make it vanilla (volcanic) oceanic mountains. Vanilla's
    // island is the low one.
    final boolean island =
      tectonicClass.land() != LandRelief.MOUNTAIN &&
      (band == ContinentBand.ISLAND ||
        (band == ContinentBand.OCEAN &&
          continentMap.anyIslandInCell(point.x, point.z)));

    if (island && arc) {
      // Vanilla volcanic island chain, trimmed to continent.png land later.
      point.oceanDepth = 1;
      point.setVolcanic();
      point.setBarrierIsland();
      return;
    }
    if (island) {
      point.setLand();
      point.setIsland();
      tfcrealworld$setVolcanism(point, volcanism);
      return;
    }
    if (band == ContinentBand.OCEAN) {
      if (arc) {
        point.oceanDepth = 1;
        point.setVolcanic();
      } else {
        point.oceanDepth = tectonicClass.water().oceanDepth();
      }
      return;
    }

    point.setLand();
    tfcrealworld$setVolcanism(point, volcanism);
  }

  @Unique
  private static void tfcrealworld$setVolcanism(
    Region.Point point,
    Volcanism volcanism
  ) {
    if (volcanism != Volcanism.NONE) {
      point.setVolcanic();
    }
  }

  /**
   * Continent map without tectonics: vanilla ocean-depth buckets over the
   * binary continent continuum and procedural Voronoi boundaries.
   */
  @Unique
  private static void tfcrealworld$applyProcedural(
    Region.Point point,
    ContinentBand band,
    PNGContinentNoise continentMap,
    RegionGenerator generator
  ) {
    switch (band) {
      case LAND, LAKE, SALT_LAKE -> point.setLand();
      case ISLAND -> {
        point.setLand();
        point.setIsland();
      }
      case OCEAN -> {
        final double continent =
          (continentMap.noise(point.x, point.z) +
            tfcrealworld$vanillaRiftSeas(
              point.distanceToEdge,
              point.divergence
            )) *
          generator.continentFactor(point);
        if (point.divergence > 0 && point.distanceToEdge < 2) {
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
    }
  }

  @Unique
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
