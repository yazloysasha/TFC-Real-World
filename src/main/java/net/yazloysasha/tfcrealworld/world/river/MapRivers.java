package net.yazloysasha.tfcrealworld.world.river;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RiverEdge;
import net.dries007.tfc.world.region.Units;
import net.dries007.tfc.world.river.River;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.profile.ProfileManager;
import org.jetbrains.annotations.Nullable;

/**
 * {@code maps/rivers.bin}: the real river network of the profile, as vanilla
 * river edges. Vanilla grows its rivers inland from the shore of each region;
 * here every region takes the edges of the map that lie in it, and the rest
 * (meanders, carving, flow) stays vanilla.
 * <p>
 * Edges are about as long as vanilla's, so a map river costs the game what a
 * vanilla river does: chunk generation walks every edge near a column.
 */
public final class MapRivers {

  public static final String FILE = "rivers.bin";

  private static final int MAGIC = 0x54465257;
  private static final int VERSION = 1;
  private static final float COORD_RANGE = 32767f;

  /** Vanilla AddRiversAndLakes.RIVER_LENGTH: the edge length in grid cells. */
  private static final float EDGE_LENGTH = 2.7f;

  /** Longer edges (a world larger than the profile's) are split at load. */
  private static final float MAX_EDGE_LENGTH = 1.5f * EDGE_LENGTH;

  /** Vanilla's localized rainfall increase around a lake or a river. */
  public static final float RAINFALL_SHARE = 0.09f;

  public static final float MAX_RAINFALL = 500f;

  /**
   * Vanilla adds its share at every step along a river, about twice to a
   * cell the river runs through.
   */
  private static final float VALLEY_RAINFALL_SHARE =
    1f - (1f - RAINFALL_SHARE) * (1f - RAINFALL_SHARE);

  /**
   * A valley is as wet as vanilla's within this many cells of the river and
   * no wetter than the land around it beyond the reach.
   */
  private static final float VALLEY_WET_CORE = 0.5f;
  private static final float VALLEY_WET_REACH = 2f;

  /**
   * Rivers this wide have a floodplain (vanilla's river valley). Map widths
   * run from 6 to 40 by the river's discharge, where vanilla's run 8 to 24.
   */
  private static final int VALLEY_MIN_WIDTH = 15;

  /** Vanilla's low band of discrete biome altitude. */
  private static final int LOWLAND_ALTITUDE = 0;

  private static final int BUCKET_BITS = 4;
  private static final long EDGE_MASK = 0xFFFFFFFFL;
  private static final long TILE_MASK = ~EDGE_MASK;
  private static final long GOLDEN_RATIO = 0x9E3779B97F4A7C15L;
  private static final long LAKE_SALT = 0x4C414B45L;

  /** Vanilla AddRiversAndLakes: a lake at one river source in three. */
  private static final int SOURCE_LAKE_RARITY = 3;

  /** Vanilla placeLakeNear: how far from the source the lake cells lie. */
  private static final float SOURCE_LAKE_OFFSET = 0.3f;

  /** Half the map in grid cells: river coordinates run from -half to half. */
  private final float halfGridX;
  private final float halfGridZ;

  private final float[] sourceX;
  private final float[] sourceZ;
  private final float[] drainX;
  private final float[] drainZ;
  private final byte[] width;

  /** The edge each edge drains into, or -1 at a mouth. */
  private final int[] downstream;

  /** Whether any edge drains into the edge: false at a river's source. */
  private final boolean[] fed;

  /** Edge indices by the bucket their midpoint lies in. */
  private final Long2ObjectOpenHashMap<IntArrayList> buckets =
    new Long2ObjectOpenHashMap<>();

  private MapRivers(
    float halfGridX,
    float halfGridZ,
    float[] sourceX,
    float[] sourceZ,
    float[] drainX,
    float[] drainZ,
    byte[] width,
    int[] downstream
  ) {
    this.halfGridX = halfGridX;
    this.halfGridZ = halfGridZ;
    this.sourceX = sourceX;
    this.sourceZ = sourceZ;
    this.drainX = drainX;
    this.drainZ = drainZ;
    this.width = width;
    this.downstream = downstream;
    this.fed = new boolean[width.length];
    for (final int below : downstream) {
      if (below >= 0) {
        fed[below] = true;
      }
    }
    for (int edge = 0; edge < width.length; edge++) {
      buckets
        .computeIfAbsent(
          bucket(midX(edge) >> BUCKET_BITS, midZ(edge) >> BUCKET_BITS),
          key -> new IntArrayList()
        )
        .add(edge);
    }
  }

  /** The rivers of the active profile, or {@code null} if it has none. */
  @Nullable
  public static MapRivers tryLoad(int horizontalScale, int verticalScale) {
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
      return read(
        new DataInputStream(new BufferedInputStream(stream)),
        (float) horizontalScale / Units.GRID_WIDTH_IN_BLOCK,
        (float) verticalScale / Units.GRID_WIDTH_IN_BLOCK
      );
    } catch (IOException e) {
      throw new IllegalStateException(
        "Failed to read " + FILE + " for profile " + profileId,
        e
      );
    }
  }

  private static MapRivers read(
    DataInputStream in,
    float halfGridX,
    float halfGridZ
  ) throws IOException {
    if (in.readInt() != MAGIC || in.readInt() != VERSION) {
      throw new IOException("Not a version " + VERSION + " " + FILE);
    }
    final int vertexCount = in.readInt();
    final float[] x = new float[vertexCount];
    final float[] z = new float[vertexCount];
    for (int i = 0; i < vertexCount; i++) {
      x[i] = (in.readShort() / COORD_RANGE) * halfGridX;
      z[i] = (in.readShort() / COORD_RANGE) * halfGridZ;
    }

    final int edgeCount = in.readInt();
    final List<float[]> pieces = new ArrayList<>(edgeCount);
    final IntArrayList widths = new IntArrayList(edgeCount);
    final IntArrayList drainVertex = new IntArrayList(edgeCount);
    final int[] firstPieceFromVertex = new int[vertexCount];
    Arrays.fill(firstPieceFromVertex, -1);
    for (int i = 0; i < edgeCount; i++) {
      final int source = in.readInt();
      final int drain = in.readInt();
      final int edgeWidth = in.readUnsignedByte();
      final float dx = x[drain] - x[source];
      final float dz = z[drain] - z[source];
      final int split = Math.max(
        1,
        (int) Math.ceil(Math.sqrt(dx * dx + dz * dz) / MAX_EDGE_LENGTH)
      );
      firstPieceFromVertex[source] = pieces.size();
      for (int piece = 0; piece < split; piece++) {
        final float from = (float) piece / split;
        final float to = (float) (piece + 1) / split;
        pieces.add(
          new float[] {
            x[source] + dx * from,
            z[source] + dz * from,
            x[source] + dx * to,
            z[source] + dz * to,
          }
        );
        widths.add(edgeWidth);
        // Inner pieces drain into the next piece; the last one at the vertex.
        drainVertex.add(piece == split - 1 ? drain : -1);
      }
    }

    final int count = pieces.size();
    final float[] sourceX = new float[count];
    final float[] sourceZ = new float[count];
    final float[] drainX = new float[count];
    final float[] drainZ = new float[count];
    final byte[] width = new byte[count];
    final int[] downstream = new int[count];
    for (int i = 0; i < count; i++) {
      final float[] piece = pieces.get(i);
      sourceX[i] = piece[0];
      sourceZ[i] = piece[1];
      drainX[i] = piece[2];
      drainZ[i] = piece[3];
      width[i] = (byte) widths.getInt(i);
      final int vertex = drainVertex.getInt(i);
      downstream[i] = vertex < 0 ? i + 1 : firstPieceFromVertex[vertex];
    }
    return new MapRivers(
      halfGridX,
      halfGridZ,
      sourceX,
      sourceZ,
      drainX,
      drainZ,
      width,
      downstream
    );
  }

  /**
   * Gives the region the map rivers that lie in it, in place of vanilla's
   * AddRiversAndLakes: the edges, their widths and downstream links, and the
   * river flag along the wide ones (vanilla river valleys). Past the edge of
   * the map the world is mirrored, and its rivers with it.
   */
  public void addTo(Region region, long worldSeed) {
    final Long2ObjectOpenHashMap<RiverEdge> edges =
      new Long2ObjectOpenHashMap<>();
    final List<RiverEdge> rivers = new ArrayList<>();
    for (
      int tileZ = tile(region.minZ(), halfGridZ);
      tileZ <= tile(region.maxZ(), halfGridZ);
      tileZ++
    ) {
      for (
        int tileX = tile(region.minX(), halfGridX);
        tileX <= tile(region.maxX(), halfGridX);
        tileX++
      ) {
        addTile(region, worldSeed, tileX, tileZ, edges, rivers);
      }
    }
    for (final var entry : edges.long2ObjectEntrySet()) {
      final long key = entry.getLongKey();
      final int below = downstream[(int) key];
      if (below >= 0) {
        // Null across a region border: the width then stays constant there.
        entry
          .getValue()
          .linkToDrain(edges.get((key & TILE_MASK) | (below & EDGE_MASK)));
      }
    }
    region.setRivers(rivers);
    final Int2FloatOpenHashMap wetness = new Int2FloatOpenHashMap();
    for (final RiverEdge river : rivers) {
      if (river.width >= VALLEY_MIN_WIDTH) {
        markValley(region, river, wetness);
      }
    }
    for (final var entry : wetness.int2FloatEntrySet()) {
      final Region.Point point = region.atIndex(entry.getIntKey());
      point.rainfall +=
        VALLEY_RAINFALL_SHARE *
        entry.getFloatValue() *
        (MAX_RAINFALL - point.rainfall);
    }
    if (!TFCRealWorldConfig.LAKES_FROM_MAP.get()) {
      for (final var entry : edges.long2ObjectEntrySet()) {
        final long key = entry.getLongKey();
        if (!fed[(int) key]) {
          placeSourceLakes(region, entry.getValue(), worldSeed, key);
        }
      }
    }
  }

  /**
   * Vanilla's lakes at river sources, for a world without lakes from the map:
   * one source in three gets a lake on the cells around it.
   */
  private static void placeSourceLakes(
    Region region,
    RiverEdge river,
    long worldSeed,
    long key
  ) {
    final XoroshiroRandomSource random = new XoroshiroRandomSource(
      worldSeed ^ ((key + 1) * GOLDEN_RATIO),
      LAKE_SALT
    );
    if (random.nextInt(SOURCE_LAKE_RARITY) != 0) {
      return;
    }
    for (int offsetX = -1; offsetX <= 1; offsetX += 2) {
      for (int offsetZ = -1; offsetZ <= 1; offsetZ += 2) {
        final Region.Point point = region.at(
          (int) (river.source().x() + SOURCE_LAKE_OFFSET * offsetX),
          (int) (river.source().y() + SOURCE_LAKE_OFFSET * offsetZ)
        );
        if (
          point != null &&
          point.land() &&
          point.distanceToOcean >= 2 &&
          point.distanceToEdge >= 2
        ) {
          point.setLake();
          point.rainfall += RAINFALL_SHARE * (MAX_RAINFALL - point.rainfall);
        }
      }
    }
  }

  /** The rivers of one copy of the map: itself, or mirrored on odd tiles. */
  private void addTile(
    Region region,
    long worldSeed,
    int tileX,
    int tileZ,
    Long2ObjectOpenHashMap<RiverEdge> edges,
    List<RiverEdge> rivers
  ) {
    final float centreX = tileX * 2f * halfGridX;
    final float centreZ = tileZ * 2f * halfGridZ;
    final float signX = (tileX & 1) == 0 ? 1f : -1f;
    final float signZ = (tileZ & 1) == 0 ? 1f : -1f;
    final float fromX = signX * (region.minX() - centreX);
    final float toX = signX * (region.maxX() - centreX);
    final float fromZ = signZ * (region.minZ() - centreZ);
    final float toZ = signZ * (region.maxZ() - centreZ);
    final long tileKey =
      ((long) (tileX & 0xFFFF) << 48) | ((long) (tileZ & 0xFFFF) << 32);
    for (
      int bucketZ = Mth.floor(Math.min(fromZ, toZ) - 1) >> BUCKET_BITS;
      bucketZ <= Mth.floor(Math.max(fromZ, toZ) + 1) >> BUCKET_BITS;
      bucketZ++
    ) {
      for (
        int bucketX = Mth.floor(Math.min(fromX, toX) - 1) >> BUCKET_BITS;
        bucketX <= Mth.floor(Math.max(fromX, toX) + 1) >> BUCKET_BITS;
        bucketX++
      ) {
        final IntArrayList inBucket = buckets.get(bucket(bucketX, bucketZ));
        if (inBucket == null) {
          continue;
        }
        for (int i = 0; i < inBucket.size(); i++) {
          final int edge = inBucket.getInt(i);
          final float fromSourceX = centreX + signX * sourceX[edge];
          final float fromSourceZ = centreZ + signZ * sourceZ[edge];
          final float toDrainX = centreX + signX * drainX[edge];
          final float toDrainZ = centreZ + signZ * drainZ[edge];
          if (
            region.at(
              Math.round(0.5f * (fromSourceX + toDrainX)),
              Math.round(0.5f * (fromSourceZ + toDrainZ))
            ) ==
            null
          ) {
            continue;
          }
          final long key = tileKey | edge;
          final RiverEdge river = createEdge(
            fromSourceX,
            fromSourceZ,
            toDrainX,
            toDrainZ,
            width[edge],
            worldSeed ^ ((key + 1) * GOLDEN_RATIO)
          );
          edges.put(key, river);
          rivers.add(river);
        }
      }
    }
  }

  private static RiverEdge createEdge(
    float fromX,
    float fromZ,
    float toX,
    float toZ,
    int width,
    long seed
  ) {
    final double dx = toX - fromX;
    final double dz = toZ - fromZ;
    final double angle = Math.atan2(dz, dx);
    final double length = Math.sqrt(dx * dx + dz * dz);
    final RiverEdge river = new RiverEdge(
      new River.Edge(
        new River.Vertex(fromX, fromZ, angle, length, 0),
        new River.Vertex(toX, toZ, angle, length, 0)
      ),
      new XoroshiroRandomSource(seed)
    );
    river.width = width;
    return river;
  }

  /** The copy of the map a grid coordinate lies in (0 is the map itself). */
  private static int tile(int grid, float halfGrid) {
    return Mth.floor((grid + halfGrid) / (2f * halfGrid));
  }

  /**
   * Vanilla annotateRiverGridScale: the cells a wide river runs through, and
   * how much wetter the valley makes the cells around it. Vanilla makes the
   * river's own cells wetter, again at every step along the river; here a
   * cell gets wetter once, the more the nearer the river runs to it, so the
   * wet strip fades to the sides instead of ending at cell borders.
   */
  private static void markValley(
    Region region,
    RiverEdge river,
    Int2FloatOpenHashMap wetness
  ) {
    final int fromX = (int) river.source().x();
    final int fromZ = (int) river.source().y();
    final int dx = (int) river.drain().x() - fromX;
    final int dz = (int) river.drain().y() - fromZ;
    final double length = Math.sqrt(dx * dx + dz * dz);
    for (double step = 0; step <= length; step++) {
      final int x = (int) (fromX + (length == 0 ? 0 : dx / length) * step);
      final int z = (int) (fromZ + (length == 0 ? 0 : dz / length) * step);
      markRiver(region.at(x, z));
      markRiver(region.at(x + 1, z));
      markRiver(region.at(x, z + 1));
      markRiver(region.at(x + 1, z + 1));
    }

    final float x0 = (float) river.source().x();
    final float z0 = (float) river.source().y();
    final float x1 = (float) river.drain().x();
    final float z1 = (float) river.drain().y();
    final int reach = Mth.ceil(VALLEY_WET_REACH);
    for (
      int z = Mth.floor(Math.min(z0, z1)) - reach;
      z <= Mth.floor(Math.max(z0, z1)) + reach;
      z++
    ) {
      for (
        int x = Mth.floor(Math.min(x0, x1)) - reach;
        x <= Mth.floor(Math.max(x0, x1)) + reach;
        x++
      ) {
        final Region.Point point = region.at(x, z);
        if (!isLowland(point)) {
          continue;
        }
        final float distance = distanceToSegment(
          x + 0.5f,
          z + 0.5f,
          x0,
          z0,
          x1,
          z1
        );
        final float wet = Mth.clamp(
          (VALLEY_WET_REACH - distance) / (VALLEY_WET_REACH - VALLEY_WET_CORE),
          0f,
          1f
        );
        if (wet > wetness.get(point.index)) {
          wetness.put(point.index, wet);
        }
      }
    }
  }

  private static float distanceToSegment(
    float x,
    float z,
    float x0,
    float z0,
    float x1,
    float z1
  ) {
    final float dx = x1 - x0;
    final float dz = z1 - z0;
    final float lengthSq = dx * dx + dz * dz;
    final float along =
      lengthSq == 0
        ? 0
        : Mth.clamp(((x - x0) * dx + (z - z0) * dz) / lengthSq, 0f, 1f);
    return Mth.sqrt(
      Mth.square(x - x0 - along * dx) + Mth.square(z - z0 - along * dz)
    );
  }

  /**
   * A wide valley is the floodplain of a river in the lowlands. Higher up
   * (uplands, plateaus, mountains) the same river runs in the gorge TFC
   * carves for it, so only lowland cells become vanilla river valleys.
   */
  private static void markRiver(@Nullable Region.Point point) {
    if (isLowland(point)) {
      point.setRiver();
    }
  }

  private static boolean isLowland(@Nullable Region.Point point) {
    return (
      point != null &&
      point.land() &&
      point.discreteBiomeAltitude() == LOWLAND_ALTITUDE
    );
  }

  private int midX(int edge) {
    return Math.round(0.5f * (sourceX[edge] + drainX[edge]));
  }

  private int midZ(int edge) {
    return Math.round(0.5f * (sourceZ[edge] + drainZ[edge]));
  }

  private static long bucket(int bucketX, int bucketZ) {
    return ((long) bucketX << 32) | (bucketZ & 0xFFFFFFFFL);
  }
}
