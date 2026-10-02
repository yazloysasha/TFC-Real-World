package net.yazloysasha.tfcrealworld.world.layer;

import static net.dries007.tfc.world.layer.TFCLayers.LAKE;
import static net.dries007.tfc.world.layer.TFCLayers.MELTWATER_LAKE;
import static net.dries007.tfc.world.layer.TFCLayers.OCEAN;
import static net.dries007.tfc.world.layer.TFCLayers.PLAINS;
import static net.dries007.tfc.world.layer.TFCLayers.RIFT_LAKE;
import static net.dries007.tfc.world.layer.TFCLayers.SUBGLACIAL_LAKE;

import com.google.common.base.Suppliers;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
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
import net.minecraft.world.level.ChunkPos;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.mixin.world.layer.TFCLayersAccessor;
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
 * lake as a whole ({@link MapRiftLakes}) and a lake at the edge of an ice
 * sheet a meltwater lake as a whole. With tectonics only hotspot shields
 * may reach into the sea; without, islands and volcanic arcs may too.
 */
public final class MapLandOceanCorrectionLayer implements TransformLayer {

  /** Cells searched around a map-only land cell for a land biome. */
  private static final int LAND_SEARCH_RADIUS = 4;

  /** Cells of a map lake searched for its form: past any lake at an edge. */
  private static final int MAX_LAKE_CELLS = 1024;

  /** No matching neighbour (layer biome ids are non-negative). */
  private static final int NONE = -1;

  /** North, east, south, west: the order vanilla layers read neighbours. */
  private static final int[][] NEIGHBOURS = {
    { 0, -1 },
    { 1, 0 },
    { 0, 1 },
    { -1, 0 },
  };

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
    this.lakesFromMap = TFCRealWorldConfig.LAKES_FROM_MAP.get();
    this.shoreWidth = shoreWidth;
    this.riftLakes = RiftLakesRegistry.get(regionGenerator);
  }

  @Override
  public int apply(AreaContext context, Area area, int x, int z) {
    final int center = area.get(x, z);
    final double gridX = x * layerToGrid;
    final double gridZ = z * layerToGrid;
    final ContinentBand band = continentNoise.bandAtGridHard(gridX, gridZ);

    if (continentNoise.isLakeAtGridHard(gridX, gridZ)) {
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
      return tectonicsFromMap ? oceanBiomeFromNeighbors(area, x, z) : OCEAN;
    }
    return center;
  }

  /**
   * The lake of a map lake pixel: vanilla's lake form of the land there.
   * Under a flat ice sheet that is a subglacial lake, at the sheet's edge a
   * meltwater lake, on the bare ground before it a plain lake; a map lake
   * that reaches the edge is a meltwater lake as a whole, or the ice over
   * one part would lift another out of the water.
   */
  private int lakeBiomeFrom(int center, Area area, int x, int z) {
    final int lake = ownLakeForm(center, area, x, z);
    return (
        (lake == SUBGLACIAL_LAKE || lake == LAKE) &&
        lakeReachesMeltwater(area, x, z)
      )
      ? MELTWATER_LAKE
      : lake;
  }

  private int ownLakeForm(int center, Area area, int x, int z) {
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
   * Whether the map lake of the cell has a cell that is a meltwater lake on
   * its own, looking no further than MAX_LAKE_CELLS cells of the lake.
   */
  private boolean lakeReachesMeltwater(Area area, int x, int z) {
    final LongOpenHashSet seen = new LongOpenHashSet();
    final LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
    seen.add(ChunkPos.asLong(x, z));
    queue.enqueue(ChunkPos.asLong(x, z));
    while (!queue.isEmpty() && seen.size() <= MAX_LAKE_CELLS) {
      final long cell = queue.dequeueLong();
      final int cellX = ChunkPos.getX(cell);
      final int cellZ = ChunkPos.getZ(cell);
      for (final int[] offset : NEIGHBOURS) {
        final int nx = cellX + offset[0];
        final int nz = cellZ + offset[1];
        final boolean mapLake = continentNoise.isLakeAtGridHard(
          nx * layerToGrid,
          nz * layerToGrid
        );
        if (!mapLake || !seen.add(ChunkPos.asLong(nx, nz))) {
          continue;
        }
        if (ownLakeForm(area.get(nx, nz), area, nx, nz) == MELTWATER_LAKE) {
          return true;
        }
        queue.enqueue(ChunkPos.asLong(nx, nz));
      }
    }
    return false;
  }

  /**
   * The land a lake biome stands for where the map has no lake: a biome
   * vanilla would flood into this very lake ({@link TFCLayers#lakeFor}), the
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
    final BiomeExtension[] biomes =
      TFCLayersAccessor.tfcrealworld$getBiomeLayers();
    final int[] forms = new int[biomes.length];
    Arrays.fill(forms, NONE);
    for (int biome = biomes.length - 1; biome >= 0; biome--) {
      if (biomes[biome] != null && TFCLayers.hasLake(biome)) {
        forms[TFCLayers.lakeFor(biome)] = biome;
      }
    }
    return forms;
  }

  /** The matching biome nearest to the cell within LAND_SEARCH_RADIUS. */
  private static int nearestBiome(
    Area area,
    int x,
    int z,
    IntPredicate matches
  ) {
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
   * so a stretch of map-only land takes its surroundings' biome (ice sheet
   * in Antarctica, not plains).
   */
  private int landBiomeFromNeighbors(Area area, int x, int z) {
    final int near = nearestBiome(
      area,
      x,
      z,
      MapLandOceanCorrectionLayer::isCopyableLand
    );
    if (near != NONE) {
      return near;
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
      // Land biomes stand only on continent.png land. Hotspot shield
      // volcanoes are the exception: as in vanilla they raise land around
      // their centre, and their flanks and seamounts reach into the sea.
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

  /** Sunken / shield-shore style biomes via TFC BiomeExtension (not IDs). */
  private static boolean isOceanicVolcanicBiome(int biome) {
    final BiomeExtension ext = TFCLayers.getFromLayerId(biome);
    return ext.isSalty() && (ext.hasTuffRings() || ext.hasCinderCones());
  }
}
