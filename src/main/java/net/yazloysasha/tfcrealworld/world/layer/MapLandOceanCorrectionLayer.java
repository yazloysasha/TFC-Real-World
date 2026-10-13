package net.yazloysasha.tfcrealworld.world.layer;

import com.google.common.base.Suppliers;
import java.util.Arrays;
import java.util.function.IntPredicate;
import java.util.function.Supplier;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.layer.framework.Area;
import net.dries007.tfc.world.layer.framework.AreaContext;
import net.dries007.tfc.world.layer.framework.TransformLayer;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.registry.TectonicsRegistry;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise.ContinentBand;
import net.yazloysasha.tfcrealworld.world.region.RegionCoords;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicPlaces;
import org.jetbrains.annotations.Nullable;

/**
 * Makes land, sea and lakes of the biome layer match {@code continent.png} at
 * 16-block cells, before vanilla shores and again after them. A biome on the
 * wrong side takes a neighbouring biome of the right kind; map lakes take the
 * lake form of the land around them. With tectonics only hotspot volcanoes
 * may reach into the sea; without, islands and volcanic ones may too.
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

  /** Layer ids looked at for the land of a lake: more than TFC has. */
  private static final int MAX_BIOMES = 256;

  /**
   * By lake biome: the first biome vanilla lists that it floods into the
   * lake. Built on first use, after every biome has its layer id.
   */
  private static final Supplier<int[]> FIRST_DRY_FORM = Suppliers.memoize(
    MapLandOceanCorrectionLayer::firstDryForms
  );

  private final PNGContinentNoise continentNoise;
  private final RegionGenerator regionGenerator;
  private final double layerToGrid;
  private final boolean tectonicsFromMap;
  private final boolean lakesFromMap;
  private final int shoreWidth;

  /** What the map says each region cell is, with tectonics from it. */
  @Nullable
  private final TectonicPlaces places;

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
    this.lakesFromMap = TFCRealWorldConfig.LAKES_FROM_MAP.get();
    this.shoreWidth = shoreWidth;
    this.places = TectonicPlaces.of(regionGenerator);
  }

  @Override
  public int apply(AreaContext context, Area area, int x, int z) {
    final int center = area.get(x, z);
    final double gridX = x * layerToGrid;
    final double gridZ = z * layerToGrid;
    final ContinentBand band = continentNoise.bandAtGridHard(gridX, gridZ);

    if (continentNoise.isLakeAtGridHard(gridX, gridZ)) {
      return lakeBiomeFrom(center, area, x, z);
    }

    final boolean mapNonOcean = band != ContinentBand.OCEAN;
    final boolean biomeOcean = TFCLayers.isOcean(center);

    if (mapNonOcean && biomeOcean) {
      return landBiomeFromNeighbors(area, x, z);
    }
    if (lakesFromMap && mapNonOcean && TFCLayers.isLake(center)) {
      // The region has no lakes of its own with lakes from the map, so this
      // is a lake vanilla chose as the form of the land (flooded tower
      // karst). Within the shore band it takes its vanilla shore form
      // (TOWER_KARST_LAKE → TOWER_KARST_BAY), as the shore layers do for any
      // coastal land; elsewhere the land it stands for.
      if (mapOceanWithin(x, z, shoreWidth)) {
        return TFCLayers.shoreFor(center);
      }
      return dryFormOf(center, area, x, z);
    }
    if (
      !mapNonOcean &&
      !biomeOcean &&
      !shouldPreserveOnMapOcean(center, gridX, gridZ)
    ) {
      return tectonicsFromMap
        ? oceanBiomeFromNeighbors(area, x, z)
        : TFCLayers.OCEAN;
    }
    return center;
  }

  /** The lake of a map lake pixel: vanilla's lake form of the land there. */
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
    return TFCLayers.LAKE;
  }

  /**
   * The land a lake biome stands for where the map has no lake: a biome
   * vanilla would flood into this very lake ({@code lakeFor}), the
   * nearest one around the cell or else the first vanilla lists. Vanilla can
   * flood land far wider than any neighbour search (tower karst lowlands),
   * so the answer never depends on what other land happens to be near.
   */
  private int dryFormOf(int lake, Area area, int x, int z) {
    final IntPredicate floodsIntoLake = biome ->
      TFCLayers.hasLake(biome) && TFCLayers.lakeFor(biome) == lake;
    final int near = nearestBiome(area, x, z, floodsIntoLake);
    if (near != NONE) {
      return near;
    }
    final int first = FIRST_DRY_FORM.get()[lake];
    return first != NONE ? first : landBiomeFromNeighbors(area, x, z);
  }

  private static int[] firstDryForms() {
    final int[] forms = new int[MAX_BIOMES];
    Arrays.fill(forms, NONE);
    for (int biome = MAX_BIOMES - 1; biome >= 0; biome--) {
      if (TFCLayers.hasLake(biome)) {
        forms[TFCLayers.lakeFor(biome)] = biome;
      }
    }
    return forms;
  }

  /** The matching biome nearest to the cell within LAND_SEARCH_RADIUS. */
  private int nearestBiome(Area area, int x, int z, IntPredicate matches) {
    final int neighbour = firstNeighbour(area, x, z, matches);
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
          if (matches.test(biome)) {
            return biome;
          }
        }
      }
    }
    return NONE;
  }

  /**
   * Land biome for a cell continent.png calls land: a land neighbour, else
   * the nearest land further out, else the land of the nearest region cell,
   * so a stretch of map-only land takes its surroundings' biome.
   */
  private int landBiomeFromNeighbors(Area area, int x, int z) {
    final int near = nearestBiome(area, x, z, this::isCopyableLand);
    if (near != NONE) {
      return near;
    }
    final int gx = (int) Math.floor(x * layerToGrid);
    final int gz = (int) Math.floor(z * layerToGrid);
    for (int dz = -1; dz <= 1; dz++) {
      for (int dx = -1; dx <= 1; dx++) {
        final Region.Point point = RegionCoords.pointAt(
          regionGenerator,
          gx + dx,
          gz + dz
        );
        if (point.land() && isCopyableLand(point.biome)) {
          return point.biome;
        }
      }
    }
    return TFCLayers.PLAINS;
  }

  /**
   * Sea around a clipped land biome keeps the seafloor of the neighbouring
   * cells (reef, shelf) instead of a generic ocean.
   */
  private int oceanBiomeFromNeighbors(Area area, int x, int z) {
    final int neighbour = firstNeighbour(area, x, z, TFCLayers::isOcean);
    return neighbour != NONE ? neighbour : TFCLayers.OCEAN;
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

  private boolean isCopyableLand(int biome) {
    return !TFCLayers.isOcean(biome) && !TFCLayers.isLake(biome);
  }

  /**
   * Keep islands / oceanic volcanoes and their ZoomLayer smear. Uses Region
   * flags (3×3) plus salty+volcanic BiomeExtension traits for outskirts
   * that left the island grid cell.
   */
  private boolean shouldPreserveOnMapOcean(
    int biome,
    double gridX,
    double gridZ
  ) {
    final int gx = (int) Math.floor(gridX);
    final int gz = (int) Math.floor(gridZ);
    if (tectonicsFromMap) {
      // Land biomes stand only on continent.png land, but for hotspots.
      return landRisenFromTheSeaNear(gx, gz);
    }
    if (isOceanicVolcanicBiome(biome)) {
      return true;
    }
    for (int dz = -1; dz <= 1; dz++) {
      for (int dx = -1; dx <= 1; dx++) {
        final Region.Point point = RegionCoords.pointAt(
          regionGenerator,
          gx + dx,
          gz + dz
        );
        if (point.island()) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Land the map's places have where continent.png has sea: hotspot
   * volcanoes, which as in vanilla raise land around their centre, and
   * whose flanks reach into the sea.
   */
  private boolean landRisenFromTheSeaNear(int gx, int gz) {
    if (places == null) {
      return false;
    }
    for (int dz = -1; dz <= 1; dz++) {
      for (int dx = -1; dx <= 1; dx++) {
        if (places.risesFromTheSea(gx + dx, gz + dz)) {
          return true;
        }
      }
    }
    return false;
  }

  /** Oceanic volcanic biomes via TFC BiomeExtension (not IDs). */
  private boolean isOceanicVolcanicBiome(int biome) {
    final BiomeExtension ext = TFCLayers.getFromLayerId(biome);
    return ext.isSalty() && ext.isVolcanic();
  }
}
