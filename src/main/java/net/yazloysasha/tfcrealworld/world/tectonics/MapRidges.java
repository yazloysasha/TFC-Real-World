package net.yazloysasha.tfcrealworld.world.tectonics;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import net.dries007.tfc.world.noise.FastNoiseLite;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.profile.ProfileManager;
import org.jetbrains.annotations.Nullable;

/**
 * {@code maps/ridges.bin}: the axes of the real mid-ocean ridges.
 * <p>
 * Vanilla raises the crest of its ocean ridge biome along the edges of its
 * own plate noise, where it also places the biome. With tectonics from the
 * map the biome stands where the map has a ridge, and the crest follows the
 * ridge's real axis: the distance vanilla measures to its plate edge is
 * measured to the axis instead, and vanilla's warps and profile stay.
 */
public final class MapRidges {

  public static final String FILE = "ridges.bin";

  private static final int MAGIC = 0x54465247;
  private static final int VERSION = 1;
  private static final double COORD_RANGE = 32767.0;

  /**
   * Blocks from the axis vanilla still shapes: its crest ends at 120 and its
   * seafloor mud deepens up to 200, and its warps move the axis by up to 155.
   */
  private static final int REACH = 360;

  /** Segments are found by the square of this many blocks they pass near. */
  private static final int BUCKET_BITS = 9;

  /** Ends of every segment, in blocks. */
  private final double[] x0;
  private final double[] z0;
  private final double[] x1;
  private final double[] z1;

  private final Long2ObjectOpenHashMap<IntArrayList> buckets =
    new Long2ObjectOpenHashMap<>();

  /** Vanilla's ridge noises of the seed asked for last. */
  @Nullable
  private volatile Warps warps;

  private MapRidges(double[] x0, double[] z0, double[] x1, double[] z1) {
    this.x0 = x0;
    this.z0 = z0;
    this.x1 = x1;
    this.z1 = z1;
    for (int segment = 0; segment < x0.length; segment++) {
      final int minX = bucketOf(Math.min(x0[segment], x1[segment]) - REACH);
      final int maxX = bucketOf(Math.max(x0[segment], x1[segment]) + REACH);
      final int minZ = bucketOf(Math.min(z0[segment], z1[segment]) - REACH);
      final int maxZ = bucketOf(Math.max(z0[segment], z1[segment]) + REACH);
      for (int bucketZ = minZ; bucketZ <= maxZ; bucketZ++) {
        for (int bucketX = minX; bucketX <= maxX; bucketX++) {
          buckets
            .computeIfAbsent(ChunkPos.asLong(bucketX, bucketZ), key ->
              new IntArrayList()
            )
            .add(segment);
        }
      }
    }
  }

  /** The ridges of the active profile, or {@code null} if it has none. */
  @Nullable
  public static MapRidges tryLoad(int horizontalScale, int verticalScale) {
    final String profileId = TFCRealWorldConfig.MAP_PROFILE.get();
    try (
      InputStream stream = ProfileManager.getProfileFileStream(
        profileId,
        ProfileManager.MAPS_DIR + "/" + FILE
      )
    ) {
      if (stream == null) {
        return null;
      }
      final DataInputStream in = new DataInputStream(
        new BufferedInputStream(stream)
      );
      if (in.readInt() != MAGIC || in.readInt() != VERSION) {
        throw new IOException("Not a version " + VERSION + " " + FILE);
      }
      final int count = in.readInt();
      final double[][] ends = new double[4][count];
      for (int segment = 0; segment < count; segment++) {
        for (int end = 0; end < 4; end++) {
          ends[end][segment] =
            (in.readShort() / COORD_RANGE) *
            (end % 2 == 0 ? horizontalScale : verticalScale);
        }
      }
      return new MapRidges(ends[0], ends[1], ends[2], ends[3]);
    } catch (IOException e) {
      throw new IllegalStateException(
        "Failed to read " + FILE + " for profile " + profileId,
        e
      );
    }
  }

  /**
   * Vanilla's {@code getOceanRidgeWarpedEdgeDistanceAndScale} with the ridge
   * axis of the map for the plate edge: the warped distance to the axis in
   * blocks and, if asked, the distance to the nearest gap in the crest.
   */
  public FastNoiseLite.Vector2 warpedAxisDistanceAndScale(
    double x,
    double z,
    long seed,
    boolean getDistanceToGaps
  ) {
    final IntArrayList near = buckets.get(
      ChunkPos.asLong(bucketOf(x), bucketOf(z))
    );
    double nearest = (double) REACH * REACH;
    double axisX = 0;
    double axisZ = 0;
    double side = 0;
    boolean found = false;
    if (near != null) {
      for (int i = 0; i < near.size(); i++) {
        final int segment = near.getInt(i);
        final double dx = x1[segment] - x0[segment];
        final double dz = z1[segment] - z0[segment];
        final double length = dx * dx + dz * dz;
        final double along =
          length == 0
            ? 0
            : Mth.clamp(
                ((x - x0[segment]) * dx + (z - z0[segment]) * dz) / length,
                0,
                1
              );
        final double pointX = x0[segment] + along * dx;
        final double pointZ = z0[segment] + along * dz;
        final double distance =
          (x - pointX) * (x - pointX) + (z - pointZ) * (z - pointZ);
        if (distance < nearest) {
          found = true;
          nearest = distance;
          axisX = pointX;
          axisZ = pointZ;
          side = dx * (z - z0[segment]) - dz * (x - x0[segment]);
        }
      }
    }
    if (!found) {
      // No axis within reach: abyssal plain, as far from a crest as any.
      return new FastNoiseLite.Vector2(REACH, 0);
    }
    final Warps noise = warps(seed);
    // As vanilla: the warps move the axis, the same way on both its sides.
    final double warp = noise.small.noise(x, z) + noise.big.noise(axisX, axisZ);
    return new FastNoiseLite.Vector2(
      Math.abs(Math.sqrt(nearest) + (side > 0 ? -warp : warp)),
      getDistanceToGaps
        ? Math.abs(
            Mth.positiveModulo(noise.faulting.noise(axisX, axisZ), 1) - 0.5
          )
        : 0
    );
  }

  private Warps warps(long seed) {
    Warps noise = warps;
    if (noise == null || noise.seed != seed) {
      // Vanilla's noises, built once instead of at every column.
      final Noise2D faulting = new OpenSimplex2D(seed)
        .octaves(2)
        .spread(0.0018f)
        .scaled(-4.5, 4.5);
      noise = new Warps(
        seed,
        faulting,
        new OpenSimplex2D(seed).octaves(2).spread(0.05f).scaled(0, 20),
        faulting.map(value -> 30 * Math.round(value))
      );
      warps = noise;
    }
    return noise;
  }

  private static int bucketOf(double block) {
    return Mth.floor(block) >> BUCKET_BITS;
  }

  /**
   * @param faulting where the crest breaks into offset pieces
   * @param small texture of the crest
   * @param big the offsets of the pieces, the same on both sides of the axis
   */
  private record Warps(
    long seed,
    Noise2D faulting,
    Noise2D small,
    Noise2D big
  ) {}
}
