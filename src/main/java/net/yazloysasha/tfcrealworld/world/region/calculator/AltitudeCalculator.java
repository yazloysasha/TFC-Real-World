package net.yazloysasha.tfcrealworld.world.region.calculator;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import java.util.BitSet;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.registry.AltitudeNoiseRegistry;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGAltitudeNoise;
import net.yazloysasha.tfcrealworld.world.region.MapTectonics;
import org.jetbrains.annotations.Nullable;

public class AltitudeCalculator extends RegionPointCalculator {

  private static final double CONTINENTAL_SHELF_THRESHOLD = 3.3;

  /**
   * Shelf farther than this from land is open ocean on Earth-scale maps.
   * Demote to deep so vanilla does not treat it as {@code OCEAN_ATOLLS}
   * habitat (depth 2 + warm + distanceToLand &gt; 4). Arc/barrier origins use
   * a tighter band ({@code distanceToLand < 8}).
   */
  private static final int OPEN_OCEAN_SHELF_DISTANCE_TO_LAND = 8;

  /**
   * Apply map ocean depths before vanilla barrier/arc placement. Must leave
   * continental shelf as depth 2 (vanilla arc/barrier origins require it) and
   * refresh {@code distanceToDeepOcean} because that annotation ran earlier
   * when map depths were still unset.
   */
  public void prepareOceanForBarrierIslands(
    Region region,
    RegionGenerator generator
  ) {
    if (!TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()) {
      return;
    }
    applyOceanDepthsFromMap(region, generator);
    applyContinentalShelf(region, generator);
    // Demote map-edge / open-ocean "shelf" before arcs so placeVolcanicArc
    // cannot seed on finite-map borders where there is intentionally no land.
    trimOpenOceanShelf(region);
    // Trench/ridge inputs after arcs/barriers (see calculate); do not stomp
    // shelf here — placeVolcanicArc / placeBarrier require oceanDepth == 2.
    reannotateDistanceToDeepOcean(region);
  }

  public void applyOceanDepthsFromMap(
    Region region,
    RegionGenerator generator
  ) {
    if (!TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()) {
      return;
    }
    final PNGAltitudeNoise noise = resolveNoise(generator);
    forEachPoint(region, point -> {
      if (!point.land()) {
        setMapOceanDepth(point, noise);
      }
    });
  }

  @Override
  public void calculate(Region region, RegionGenerator generator) {
    if (!TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()) {
      return;
    }

    final PNGAltitudeNoise noise = resolveNoise(generator);
    forEachPoint(region, point -> {
      if (point.land()) {
        point.baseLandHeight = noise.getBaseLandHeight(point.x, point.z);
      } else {
        setMapOceanDepth(point, noise);
      }
    });
    applyContinentalShelf(region, generator);
    trimOpenOceanShelf(region);
    applyTectonicOceanDepths(region, generator);
    reannotateDistanceToDeepOcean(region);
  }

  /**
   * Feed vanilla ChooseBiomes trench/ridge depth inputs (5 / 3). Never assign
   * biomes. Never overwrite reef (1) or continental shelf (2): vanilla keeps
   * shelf when continent &gt; 3.3 ahead of trench, and barrier/arc placement
   * requires shelf depth 2 in subduction / rift zones.
   */
  private void applyTectonicOceanDepths(
    Region region,
    RegionGenerator generator
  ) {
    if (!MapTectonics.isActive(generator)) {
      return;
    }
    forEachPoint(region, point -> {
      if (point.land()) {
        return;
      }
      final int depth = Byte.toUnsignedInt(point.oceanDepth);
      if (
        depth == PNGAltitudeNoise.REEF_OCEAN_DEPTH ||
        depth == PNGAltitudeNoise.SHELF_OCEAN_DEPTH
      ) {
        return;
      }
      if (MapTectonics.isNearTrench(point.divergence)) {
        point.oceanDepth = PNGAltitudeNoise.TRENCH_OCEAN_DEPTH;
        return;
      }
      if (
        depth != PNGAltitudeNoise.TRENCH_OCEAN_DEPTH &&
        MapTectonics.isNearOceanRidge(point.divergence)
      ) {
        point.oceanDepth = PNGAltitudeNoise.RIDGE_OCEAN_DEPTH;
      }
    });
  }

  private void applyContinentalShelf(Region region, RegionGenerator generator) {
    final boolean mapTectonics = MapTectonics.isActive(generator);
    forEachPoint(region, point -> {
      if (point.land()) {
        return;
      }
      final int depth = Byte.toUnsignedInt(point.oceanDepth);
      if (
        depth == PNGAltitudeNoise.REEF_OCEAN_DEPTH ||
        depth == PNGAltitudeNoise.TRENCH_OCEAN_DEPTH ||
        depth == PNGAltitudeNoise.RIDGE_OCEAN_DEPTH
      ) {
        return;
      }
      final double tectonicFeatures = mapTectonics
        ? MapTectonics.continentRiftAdjustment(point.divergence)
        : 0;
      final double continent =
        (generator.continentNoise.noise(point.x, point.z) + tectonicFeatures) *
        generator.continentFactor(point);
      if (continent > CONTINENTAL_SHELF_THRESHOLD) {
        point.oceanDepth = PNGAltitudeNoise.SHELF_OCEAN_DEPTH;
      }
    });
  }

  /**
   * Earth-scale maps leave shallow coastal greys far from any land (map
   * borders, mid-basin plateaus). Those normalize to shelf (2) and vanilla
   * then picks {@code OCEAN_ATOLLS} when warm and {@code distanceToLand > 4}.
   * Demote that open-ocean shelf to deep so atoll habitat stays near real
   * shelves; keep shelf inside the arc/barrier distance band.
   */
  private static void trimOpenOceanShelf(Region region) {
    forEachPointStatic(region, point -> {
      if (point.land()) {
        return;
      }
      if (
        Byte.toUnsignedInt(point.oceanDepth) !=
        PNGAltitudeNoise.SHELF_OCEAN_DEPTH
      ) {
        return;
      }
      if (point.distanceToLand <= OPEN_OCEAN_SHELF_DISTANCE_TO_LAND) {
        return;
      }
      point.oceanDepth = PNGAltitudeNoise.DEEP_OCEAN_DEPTH;
    });
  }

  /**
   * Mirror vanilla {@code AnnotateDistanceToDeepOcean}: seed on depth &gt; 2,
   * BFS into shelf/reef (depth ≤ 2). Needed when map depths are applied after
   * that task already ran with unset depths.
   */
  private static void reannotateDistanceToDeepOcean(Region region) {
    final BitSet explored = new BitSet(region.size());
    final IntArrayFIFOQueue queue = new IntArrayFIFOQueue();

    forEachPointStatic(region, point -> {
      point.distanceToDeepOcean = 0;
      if (Byte.toUnsignedInt(point.oceanDepth) > 2) {
        point.distanceToDeepOcean = -1;
        queue.enqueue(point.index);
        explored.set(point.index);
      }
    });

    while (!queue.isEmpty()) {
      final int last = queue.dequeueInt();
      final Region.Point lastPoint = region.atIndex(last);
      final int nextDistance = lastPoint.distanceToDeepOcean + 1;

      for (int dx = -1; dx <= 1; dx++) {
        for (int dz = -1; dz <= 1; dz++) {
          @Nullable
          final Region.Point point = region.atOffset(last, dx, dz);
          if (
            point != null &&
            Byte.toUnsignedInt(point.oceanDepth) <= 2 &&
            point.distanceToDeepOcean == 0
          ) {
            if (!explored.get(point.index)) {
              point.distanceToDeepOcean = (byte) nextDistance;
              queue.enqueue(point.index);
            }
            explored.set(point.index);
          }
        }
      }
    }
  }

  private static void forEachPointStatic(
    Region region,
    java.util.function.Consumer<Region.Point> consumer
  ) {
    for (final var point : region.points()) {
      if (point != null) {
        consumer.accept(point);
      }
    }
  }

  private static PNGAltitudeNoise resolveNoise(RegionGenerator generator) {
    PNGAltitudeNoise altitudeNoise = AltitudeNoiseRegistry.get(generator);
    if (altitudeNoise == null) {
      altitudeNoise = new PNGAltitudeNoise(
        TFCRealWorldConfig.HORIZONTAL_SCALE.get(),
        TFCRealWorldConfig.VERTICAL_SCALE.get()
      );
      AltitudeNoiseRegistry.register(generator, altitudeNoise);
    }
    return altitudeNoise;
  }

  private static void setMapOceanDepth(
    Region.Point point,
    PNGAltitudeNoise noise
  ) {
    if (
      Byte.toUnsignedInt(point.oceanDepth) == PNGAltitudeNoise.REEF_OCEAN_DEPTH
    ) {
      return;
    }
    point.oceanDepth = PNGAltitudeNoise.normalizeMapOceanDepth(
      noise.getBaseOceanDepth(point.x, point.z)
    );
  }
}
