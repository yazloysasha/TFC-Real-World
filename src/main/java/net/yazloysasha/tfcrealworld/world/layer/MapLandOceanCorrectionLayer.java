package net.yazloysasha.tfcrealworld.world.layer;

import static net.dries007.tfc.world.layer.TFCLayers.LAKE;
import static net.dries007.tfc.world.layer.TFCLayers.OCEAN;
import static net.dries007.tfc.world.layer.TFCLayers.PLAINS;
import static net.dries007.tfc.world.layer.TFCLayers.RIFT_LAKE;

import java.util.function.IntPredicate;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.layer.framework.Area;
import net.dries007.tfc.world.layer.framework.AreaContext;
import net.dries007.tfc.world.layer.framework.TransformLayer;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.util.registry.RiftLakesRegistry;
import net.yazloysasha.tfcrealworld.util.registry.TectonicsRegistry;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise.ContinentBand;
import net.yazloysasha.tfcrealworld.world.tectonics.MapRiftLakes;
import org.jetbrains.annotations.Nullable;

/**
 * Makes land, sea and lakes of the biome layer match {@code continent.png} at
 * 16-block cells, before vanilla shores and again after them. A biome on the
 * wrong side takes a neighbouring biome of the right kind; map lakes take the
 * lake form of the land around them, except that a lake in a rift is a rift
 * lake as a whole ({@link MapRiftLakes}). With tectonics only hotspot shields
 * may reach into the sea; without, islands and volcanic arcs may too.
 */
public final class MapLandOceanCorrectionLayer implements TransformLayer {

  /** Cells searched around a map-only land cell for a land biome. */
  private static final int LAND_SEARCH_RADIUS = 4;

  /** No matching neighbour (layer biome ids are non-negative). */
  private static final int NONE = -1;

  /** North, east, south, west: the order vanilla layers read neighbours. */
  private static final int[][] NEIGHBOURS = {
    { 0, -1 },
    { 1, 0 },
    { 0, 1 },
    { -1, 0 },
  };

  private final PNGContinentNoise continentNoise;
  private final RegionGenerator regionGenerator;
  private final double layerToGrid;
  private final boolean tectonicsFromMap;
  private final int shoreWidth;

  @Nullable
  private final MapRiftLakes riftLakes;

  /**
   * @param zoomsFromGrid zooms since region grid (3 → 16-block cells).
   * @param shoreWidth cells of shore band inland from map ocean (vanilla
   *     ShoreAndRiver plus the widen passes); 0 after shores are painted.
   */
  public MapLandOceanCorrectionLayer(
    PNGContinentNoise continentNoise,
    RegionGenerator regionGenerator,
    int zoomsFromGrid,
    int shoreWidth
  ) {
    this.continentNoise = continentNoise;
    this.regionGenerator = regionGenerator;
    this.layerToGrid = 1.0 / (1 << zoomsFromGrid);
    this.tectonicsFromMap = TectonicsRegistry.isActive(regionGenerator);
    this.shoreWidth = shoreWidth;
    this.riftLakes = RiftLakesRegistry.get(regionGenerator);
  }

  @Override
  public int apply(AreaContext context, Area area, int x, int z) {
    final int center = area.get(x, z);
    final double gridX = x * layerToGrid;
    final double gridZ = z * layerToGrid;
    final ContinentBand band = continentNoise.bandAtGridHard(gridX, gridZ);

    if (band == ContinentBand.LAKE || band == ContinentBand.SALT_LAKE) {
      if (riftLakes != null && riftLakes.isRiftLakeAtGrid(gridX, gridZ)) {
        return RIFT_LAKE;
      }
      return lakeBiomeFrom(center, area, x, z);
    }

    final boolean mapNonOcean = band != ContinentBand.OCEAN;
    final boolean biomeOcean = TFCLayers.isOcean(center);

    if (mapNonOcean && biomeOcean) {
      return landBiomeFromNeighbors(area, x, z);
    }
    if (mapNonOcean && TFCLayers.isLake(center)) {
      // Map says dry land / island — strip procedural lake biome. Within the
      // shore band it takes its vanilla shore form instead (TOWER_KARST_LAKE
      // → TOWER_KARST_BAY), as the shore layers do for any coastal land.
      if (mapOceanWithin(x, z, shoreWidth)) {
        return TFCLayers.shoreFor(center);
      }
      return landBiomeFromNeighbors(area, x, z);
    }
    if (
      !mapNonOcean &&
      !biomeOcean &&
      !shouldPreserveOnMapOcean(center, gridX, gridZ)
    ) {
      return tectonicsFromMap ? oceanBiomeFromNeighbors(area, x, z) : OCEAN;
    }
    return center;
  }

  /**
   * Hi-res lake outline from ×16 continent band. Vanilla ChooseBiomes already
   * set lake flags at grid scale; this refines the biome footprint.
   */
  private int lakeBiomeFrom(int center, Area area, int x, int z) {
    if (TFCLayers.isLake(center)) {
      return center;
    }
    if (TFCLayers.hasLake(center)) {
      return TFCLayers.lakeFor(center);
    }
    final int land = TFCLayers.isOcean(center)
      ? landBiomeFromNeighbors(area, x, z)
      : center;
    if (TFCLayers.hasLake(land)) {
      return TFCLayers.lakeFor(land);
    }
    return LAKE;
  }

  /**
   * Land biome for a cell continent.png calls land: a land neighbour, else
   * the nearest land further out, else the land of the nearest region cell,
   * so a stretch of map-only land takes its surroundings' biome (ice sheet
   * in Antarctica, not plains).
   */
  private int landBiomeFromNeighbors(Area area, int x, int z) {
    final int neighbour = firstNeighbour(
      area,
      x,
      z,
      MapLandOceanCorrectionLayer::isCopyableLand
    );
    if (neighbour != NONE) {
      return neighbour;
    }
    for (int radius = 2; radius <= LAND_SEARCH_RADIUS; radius++) {
      for (int d = -radius; d <= radius; d++) {
        for (final int[] offset : new int[][] {
          { d, -radius },
          { d, radius },
          { -radius, d },
          { radius, d },
        }) {
          final int biome = area.get(x + offset[0], z + offset[1]);
          if (isCopyableLand(biome)) {
            return biome;
          }
        }
      }
    }
    final int gx = (int) Math.floor(x * layerToGrid);
    final int gz = (int) Math.floor(z * layerToGrid);
    for (int dz = -1; dz <= 1; dz++) {
      for (int dx = -1; dx <= 1; dx++) {
        final Region.Point point = regionGenerator.getOrCreateRegionPoint(
          gx + dx,
          gz + dz
        );
        if (point.land() && isCopyableLand(point.biome)) {
          return point.biome;
        }
      }
    }
    return PLAINS;
  }

  /**
   * Sea around a clipped land biome keeps the seafloor of the neighbouring
   * cells (volcanic arc, reef, shelf) instead of a generic ocean.
   */
  private static int oceanBiomeFromNeighbors(Area area, int x, int z) {
    final int neighbour = firstNeighbour(area, x, z, TFCLayers::isOcean);
    return neighbour != NONE ? neighbour : OCEAN;
  }

  /** First of the north, east, south and west neighbours that matches. */
  private static int firstNeighbour(
    Area area,
    int x,
    int z,
    IntPredicate matches
  ) {
    for (final int[] offset : NEIGHBOURS) {
      final int biome = area.get(x + offset[0], z + offset[1]);
      if (matches.test(biome)) {
        return biome;
      }
    }
    return NONE;
  }

  /** Map ocean within {@code steps} 4-neighbour steps, as shores spread. */
  private boolean mapOceanWithin(int x, int z, int steps) {
    for (int dz = -steps; dz <= steps; dz++) {
      final int reach = steps - Math.abs(dz);
      for (int dx = -reach; dx <= reach; dx++) {
        if ((dx != 0 || dz != 0) && isMapOcean(x + dx, z + dz)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isMapOcean(int x, int z) {
    return (
      continentNoise.bandAtGridHard(x * layerToGrid, z * layerToGrid) ==
      ContinentBand.OCEAN
    );
  }

  private static boolean isCopyableLand(int biome) {
    return !TFCLayers.isOcean(biome) && !TFCLayers.isLake(biome);
  }

  /**
   * Keep islands / volcanic arcs / hotspot seamounts (incl. sunken shields) and
   * their ZoomLayer smear. Uses Region flags (3×3) plus salty+tuff/cinder
   * BiomeExtension traits for outskirts that left the hotspot grid cell.
   */
  private boolean shouldPreserveOnMapOcean(
    int biome,
    double gridX,
    double gridZ
  ) {
    final int gx = (int) Math.floor(gridX);
    final int gz = (int) Math.floor(gridZ);
    if (tectonicsFromMap) {
      // Land biomes stand only on continent.png land. Mantle-plume shield
      // volcanoes are the exception: their flanks and seamounts reach into
      // the sea around the island.
      return hotspotNear(gx, gz);
    }
    if (isOceanicVolcanicBiome(biome)) {
      return true;
    }
    for (int dz = -1; dz <= 1; dz++) {
      for (int dx = -1; dx <= 1; dx++) {
        final Region.Point point = regionGenerator.getOrCreateRegionPoint(
          gx + dx,
          gz + dz
        );
        if (
          point.island() ||
          point.barrierIsland() ||
          point.volcanic() ||
          point.hotSpotAge > 0
        ) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean hotspotNear(int gx, int gz) {
    for (int dz = -1; dz <= 1; dz++) {
      for (int dx = -1; dx <= 1; dx++) {
        if (
          regionGenerator.getOrCreateRegionPoint(gx + dx, gz + dz).hotSpotAge >
          0
        ) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Sunken / shield-shore style biomes via TFC BiomeExtension (not IDs).
   */
  private static boolean isOceanicVolcanicBiome(int biome) {
    final BiomeExtension ext = TFCLayers.getFromLayerId(biome);
    return ext.isSalty() && (ext.hasTuffRings() || ext.hasCinderCones());
  }
}
