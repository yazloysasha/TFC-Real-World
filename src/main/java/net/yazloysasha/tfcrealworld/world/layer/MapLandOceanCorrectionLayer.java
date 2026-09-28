package net.yazloysasha.tfcrealworld.world.layer;

import static net.dries007.tfc.world.layer.TFCLayers.LAKE;
import static net.dries007.tfc.world.layer.TFCLayers.OCEAN;
import static net.dries007.tfc.world.layer.TFCLayers.PLAINS;

import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.layer.framework.Area;
import net.dries007.tfc.world.layer.framework.AreaContext;
import net.dries007.tfc.world.layer.framework.TransformLayer;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise.ContinentBand;

/**
 * Forces biome land/ocean/lake family to match {@code continent.png} before
 * shores. Injected at 16-block scale (same hi-res path as the shore mask) so
 * map lakes keep detailed outlines. Preserves oceanic islands / hotspots via
 * Region flags and BiomeExtension helpers (no biome-ID list).
 */
public final class MapLandOceanCorrectionLayer implements TransformLayer {

  private final PNGContinentNoise continentNoise;
  private final RegionGenerator regionGenerator;
  private final double layerToGrid;

  /**
   * @param zoomsFromGrid zooms since region grid (3 → 16-block cells).
   */
  public MapLandOceanCorrectionLayer(
    PNGContinentNoise continentNoise,
    RegionGenerator regionGenerator,
    int zoomsFromGrid
  ) {
    this.continentNoise = continentNoise;
    this.regionGenerator = regionGenerator;
    this.layerToGrid = 1.0 / (1 << zoomsFromGrid);
  }

  @Override
  public int apply(AreaContext context, Area area, int x, int z) {
    final int center = area.get(x, z);
    final double gridX = x * layerToGrid;
    final double gridZ = z * layerToGrid;
    final ContinentBand band = continentNoise.bandAtGridHard(gridX, gridZ);

    if (band == ContinentBand.LAKE) {
      return lakeBiomeFrom(center, area, x, z);
    }

    final boolean mapNonOcean = band != ContinentBand.OCEAN;
    final boolean biomeOcean = TFCLayers.isOcean(center);

    if (mapNonOcean && biomeOcean) {
      return landBiomeFromNeighbors(area, x, z);
    }
    if (mapNonOcean && TFCLayers.isLake(center)) {
      // Map says dry land / island — strip procedural lake biome.
      return landBiomeFromNeighbors(area, x, z);
    }
    if (
      !mapNonOcean &&
      !biomeOcean &&
      !shouldPreserveOnMapOcean(center, gridX, gridZ)
    ) {
      return OCEAN;
    }
    return center;
  }

  /**
   * Hi-res lake outline from ×16 continent band. Vanilla ChooseBiomes already
   * set lake flags at grid scale; this refines the biome footprint.
   */
  private static int lakeBiomeFrom(int center, Area area, int x, int z) {
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

  private static int landBiomeFromNeighbors(Area area, int x, int z) {
    final int north = area.get(x, z - 1);
    if (isCopyableLand(north)) {
      return north;
    }
    final int east = area.get(x + 1, z);
    if (isCopyableLand(east)) {
      return east;
    }
    final int south = area.get(x, z + 1);
    if (isCopyableLand(south)) {
      return south;
    }
    final int west = area.get(x - 1, z);
    if (isCopyableLand(west)) {
      return west;
    }
    return PLAINS;
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
    if (isOceanicVolcanicBiome(biome)) {
      return true;
    }
    final int gx = (int) Math.floor(gridX);
    final int gz = (int) Math.floor(gridZ);
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

  /**
   * Sunken / shield-shore style biomes via TFC BiomeExtension (not IDs).
   */
  private static boolean isOceanicVolcanicBiome(int biome) {
    final BiomeExtension ext = TFCLayers.getFromLayerId(biome);
    return ext.isSalty() && (ext.hasTuffRings() || ext.hasCinderCones());
  }
}
