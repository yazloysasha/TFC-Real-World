package net.yazloysasha.tfcrealworld.world.volcano;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.Units;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGHotspotsNoise;
import net.yazloysasha.tfcrealworld.world.region.RegionCoords;
import org.jetbrains.annotations.Nullable;

/**
 * Scans the hotspot PNG into discrete volcanic centers. Used to keep map
 * mountain cells from being overwritten by TFG/TFC shield-volcano overlay.
 */
public final class MapHotspotLayout {

  private static final double POLE_CURVE_EXPONENT = 1.25;
  private static final double MAP_PLACEMENT_CHANCE_EQUATOR = 0.75;
  private static final double MAP_PLACEMENT_CHANCE_POLE = 1.0;
  private static final double MOUNTAIN_STRATOVOLCANO_CHANCE_EQUATOR = 0.5;
  private static final double MOUNTAIN_STRATOVOLCANO_CHANCE_POLE = 1.0;
  private static final double INV_GRID_WIDTH_IN_BLOCK =
    1.0 / Units.GRID_WIDTH_IN_BLOCK;
  private static final double MIN_RADIUS_BLOCKS = 384;
  private static final double BIOME_RADIUS_SCALE = 1.05;
  private static final double HALF_GRID_BLOCK = Units.GRID_WIDTH_IN_BLOCK * 0.5;

  public enum MountainStyle {
    SHIELD,
    STRATOVOLCANO,
    NATURAL_MOUNTAIN,
  }

  private final PNGHotspotsNoise noise;
  private final Center[] centers;
  private final List<Center>[] centersByAge;
  private final MountainStyle[] mountainStyles;
  private final double blocksPerPixelX;
  private final double blocksPerPixelZ;

  @SuppressWarnings("unchecked")
  private MapHotspotLayout(
    PNGHotspotsNoise noise,
    Center[] centers,
    double blocksPerPixelX,
    double blocksPerPixelZ
  ) {
    this.noise = noise;
    this.centers = centers;
    this.blocksPerPixelX = blocksPerPixelX;
    this.blocksPerPixelZ = blocksPerPixelZ;
    this.mountainStyles = new MountainStyle[centers.length];
    this.centersByAge = new List[5];
    for (int age = 1; age <= 4; age++) {
      centersByAge[age] = new ArrayList<>();
    }
    for (final Center center : centers) {
      centersByAge[center.age()].add(center);
    }
  }

  public static MapHotspotLayout create(
    PNGHotspotsNoise noise,
    long worldSeed
  ) {
    final double blocksPerPixelX =
      (2.0 * noise.getTileRadiusBlocksX()) / noise.getWidth();
    final double blocksPerPixelZ =
      (2.0 * noise.getTileRadiusBlocksZ()) / noise.getHeight();
    final List<Center> scanned = scanCenters(
      noise,
      blocksPerPixelX,
      blocksPerPixelZ
    );
    final List<Center> placed = new ArrayList<>();
    int nextId = 0;
    for (final Center center : scanned) {
      if (
        seededChance(worldSeed, center, 0x9e3779b97f4a7c15L) <
        center.placementChance()
      ) {
        placed.add(center.withId(nextId++));
      }
    }
    return new MapHotspotLayout(
      noise,
      placed.toArray(Center[]::new),
      blocksPerPixelX,
      blocksPerPixelZ
    );
  }

  public Noise2D intensityNoise(byte age, long seed) {
    if (age < 1 || age > 4) {
      return (x, z) -> 0;
    }
    final List<Center> scoped = centersByAge[age];
    if (scoped.isEmpty()) {
      return (x, z) -> 0;
    }
    final OpenSimplex2D warp = new OpenSimplex2D(seed + 7919L * age)
      .octaves(2)
      .spread(0.004)
      .scaled(-0.1, 0.1);
    return (x, z) -> peakAt(x, z, scoped, warp.noise(x, z));
  }

  public byte ageAtGrid(int gridX, int gridZ) {
    final Center center = nearestCenterAtGrid(gridX, gridZ);
    return center == null ? 0 : center.age();
  }

  @Nullable
  public Center nearestCenterAtGrid(int gridX, int gridZ) {
    final double blockX = RegionCoords.gridToBlock(gridX) + HALF_GRID_BLOCK;
    final double blockZ = RegionCoords.gridToBlock(gridZ) + HALF_GRID_BLOCK;
    final double[] image = noise.tileToImage(
      blockX * INV_GRID_WIDTH_IN_BLOCK,
      blockZ * INV_GRID_WIDTH_IN_BLOCK
    );
    final double imageX = image[0];
    final double imageZ = image[1];

    Center nearest = null;
    double nearestDistSq = Double.MAX_VALUE;
    for (final Center center : centers) {
      final int dxGrid = gridX - center.gridX();
      final int dzGrid = gridZ - center.gridZ();
      if (
        dxGrid > center.maxGridRadius() ||
        dxGrid < -center.maxGridRadius() ||
        dzGrid > center.maxGridRadius() ||
        dzGrid < -center.maxGridRadius()
      ) {
        continue;
      }
      final double dx = (imageX - center.imageX()) * blocksPerPixelX;
      final double dz = (imageZ - center.imageZ()) * blocksPerPixelZ;
      final double distSq = dx * dx + dz * dz;
      final double maxDist = center.radiusBlocks() * BIOME_RADIUS_SCALE;
      final double maxDistSq = maxDist * maxDist;
      if (distSq >= maxDistSq || distSq >= nearestDistSq) {
        continue;
      }
      nearestDistSq = distSq;
      nearest = center;
    }
    return nearest;
  }

  public void resolveMountainStyles(Region region, long worldSeed) {
    for (final Center center : centers) {
      if (mountainStyles[center.id()] != null) {
        continue;
      }
      final Region.Point centerPoint = region.maybeAt(
        center.gridX(),
        center.gridZ()
      );
      if (centerPoint == null) {
        continue;
      }
      if (!centerPoint.mountain()) {
        mountainStyles[center.id()] = MountainStyle.SHIELD;
        continue;
      }
      mountainStyles[center.id()] = seededChance(
          worldSeed,
          center,
          0x6c622b72L
        ) <
        center.stratovolcanoChance()
        ? MountainStyle.STRATOVOLCANO
        : MountainStyle.NATURAL_MOUNTAIN;
    }
  }

  public MountainStyle mountainStyle(int centerId) {
    final MountainStyle style = mountainStyles[centerId];
    return style == null ? MountainStyle.SHIELD : style;
  }

  public void prepareChooseBiomes(Region region, long worldSeed) {
    resolveMountainStyles(region, worldSeed);
  }

  public boolean keepMountainBiome(
    int gridX,
    int gridZ,
    boolean mountain,
    byte hotSpotAge
  ) {
    if (hotSpotAge <= 0 || !mountain) {
      return false;
    }
    final Center center = nearestCenterAtGrid(gridX, gridZ);
    if (center == null) {
      return false;
    }
    final MountainStyle style = mountainStyles[center.id()];
    return (
      style == MountainStyle.NATURAL_MOUNTAIN ||
      style == MountainStyle.STRATOVOLCANO
    );
  }

  private double peakAt(
    double blockX,
    double blockZ,
    List<Center> scoped,
    double warp
  ) {
    final double[] image = noise.tileToImage(
      blockX * INV_GRID_WIDTH_IN_BLOCK,
      blockZ * INV_GRID_WIDTH_IN_BLOCK
    );
    final double imageX = image[0];
    final double imageZ = image[1];
    double best = 0;
    for (final Center center : scoped) {
      final double radius = center.radiusBlocks() * (1 + warp);
      if (radius <= 0) {
        continue;
      }
      final double dx = (imageX - center.imageX()) * blocksPerPixelX;
      final double dz = (imageZ - center.imageZ()) * blocksPerPixelZ;
      final double dist = Math.hypot(dx, dz);
      if (dist >= radius) {
        continue;
      }
      final double t = dist / radius;
      final double peak = (1 - t) * (1 - t);
      if (peak > best) {
        best = peak;
      }
    }
    return best;
  }

  private static double seededChance(long worldSeed, Center center, long salt) {
    long hash = worldSeed;
    hash ^= salt;
    hash ^= (long) center.age() * 0x632BE59BD9B4E019L;
    hash ^= Double.doubleToLongBits(center.imageX()) * 0x517cc1b727220a95L;
    hash ^= Double.doubleToLongBits(center.imageZ()) * 0x6C078965L;
    hash = mix64(hash);
    return (hash >>> 11) * (1.0 / (1L << 53));
  }

  private static long mix64(long z) {
    z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
    z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
    return z ^ (z >>> 33);
  }

  private static List<Center> scanCenters(
    PNGHotspotsNoise noise,
    double blocksPerPixelX,
    double blocksPerPixelZ
  ) {
    final int width = noise.getWidth();
    final int height = noise.getHeight();
    final boolean[] visited = new boolean[width * height];
    final List<Center> centers = new ArrayList<>();

    for (int z = 0; z < height; z++) {
      for (int x = 0; x < width; x++) {
        final int index = z * width + x;
        if (visited[index]) {
          continue;
        }
        final byte age = PNGHotspotsNoise.ageFromBrightness(
          noise.getBrightness(x, z)
        );
        if (age == 0) {
          visited[index] = true;
          continue;
        }
        centers.add(
          withPoleChances(
            floodFill(
              noise,
              visited,
              x,
              z,
              age,
              blocksPerPixelX,
              blocksPerPixelZ
            ),
            height
          )
        );
      }
    }
    return centers;
  }

  private static Center floodFill(
    PNGHotspotsNoise noise,
    boolean[] visited,
    int startX,
    int startZ,
    byte age,
    double blocksPerPixelX,
    double blocksPerPixelZ
  ) {
    final int width = noise.getWidth();
    final int height = noise.getHeight();
    final ArrayDeque<Integer> queue = new ArrayDeque<>();
    final int startIndex = startZ * width + startX;
    queue.add(startIndex);
    visited[startIndex] = true;

    double sumX = 0;
    double sumZ = 0;
    int pixels = 0;

    while (!queue.isEmpty()) {
      final int index = queue.removeFirst();
      final int x = index % width;
      final int z = index / width;
      sumX += x;
      sumZ += z;
      pixels++;

      enqueueIfSameAge(noise, visited, queue, x - 1, z, width, height, age);
      enqueueIfSameAge(noise, visited, queue, x + 1, z, width, height, age);
      enqueueIfSameAge(noise, visited, queue, x, z - 1, width, height, age);
      enqueueIfSameAge(noise, visited, queue, x, z + 1, width, height, age);
    }

    final double imageX = sumX / pixels;
    final double imageZ = sumZ / pixels;
    final double areaBlocks = pixels * blocksPerPixelX * blocksPerPixelZ;
    final double radius = Math.max(
      MIN_RADIUS_BLOCKS,
      Math.sqrt(areaBlocks / Math.PI)
    );
    final int gridX = (int) Math.round(
      (imageX - noise.getCenterX()) / noise.getScaleX()
    );
    final int gridZ = (int) Math.round(
      (imageZ - noise.getCenterZ()) / noise.getScaleZ()
    );
    return new Center(0, age, imageX, imageZ, radius, gridX, gridZ, 0, 0, 0);
  }

  private static Center withPoleChances(Center center, int mapHeight) {
    final double w = poleCurveWeight(center.imageZ(), mapHeight);
    final double placement =
      MAP_PLACEMENT_CHANCE_EQUATOR +
      (MAP_PLACEMENT_CHANCE_POLE - MAP_PLACEMENT_CHANCE_EQUATOR) * w;
    final double stratovolcano =
      MOUNTAIN_STRATOVOLCANO_CHANCE_EQUATOR +
      (MOUNTAIN_STRATOVOLCANO_CHANCE_POLE -
        MOUNTAIN_STRATOVOLCANO_CHANCE_EQUATOR) *
      w;
    final int maxGrid = (int) Math.ceil(
      center.radiusBlocks() * BIOME_RADIUS_SCALE * INV_GRID_WIDTH_IN_BLOCK
    );
    return new Center(
      center.id(),
      center.age(),
      center.imageX(),
      center.imageZ(),
      center.radiusBlocks(),
      center.gridX(),
      center.gridZ(),
      placement,
      stratovolcano,
      maxGrid
    );
  }

  private static double poleCurveWeight(double imageZ, int mapHeight) {
    if (mapHeight <= 1) {
      return 1.0;
    }
    final double equatorZ = mapHeight * 0.5;
    final double t = Math.min(1.0, Math.abs(imageZ - equatorZ) / equatorZ);
    if (t <= 0.0) {
      return 0.0;
    }
    if (t >= 1.0) {
      return 1.0;
    }
    return Math.pow(t, POLE_CURVE_EXPONENT);
  }

  private static void enqueueIfSameAge(
    PNGHotspotsNoise noise,
    boolean[] visited,
    ArrayDeque<Integer> queue,
    int x,
    int z,
    int width,
    int height,
    byte age
  ) {
    if (x < 0 || z < 0 || x >= width || z >= height) {
      return;
    }
    final int index = z * width + x;
    if (visited[index]) {
      return;
    }
    if (PNGHotspotsNoise.ageFromBrightness(noise.getBrightness(x, z)) != age) {
      return;
    }
    visited[index] = true;
    queue.add(index);
  }

  public record Center(
    int id,
    byte age,
    double imageX,
    double imageZ,
    double radiusBlocks,
    int gridX,
    int gridZ,
    double placementChance,
    double stratovolcanoChance,
    int maxGridRadius
  ) {
    Center withId(int newId) {
      return new Center(
        newId,
        age,
        imageX,
        imageZ,
        radiusBlocks,
        gridX,
        gridZ,
        placementChance,
        stratovolcanoChance,
        maxGridRadius
      );
    }
  }
}
