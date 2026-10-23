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
import net.dries007.tfc.world.river.RiverFractal;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.mixin.world.region.RegionAccessor;
import net.yazloysasha.tfcrealworld.util.profile.ProfileManager;
import net.yazloysasha.tfcrealworld.world.region.RegionCoords;
import org.jetbrains.annotations.Nullable;

/**
 * {@code maps/rivers.bin}: the real river network of the profile, as vanilla
 * river edges, all of vanilla's one width. Vanilla grows its rivers inland
 * from the shore of each region; here every region takes the edges of the map
 * that lie in it, and the rest
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

  /** TFC 3's localized rainfall increase around a lake or a river. */
  public static final float RAINFALL_SHARE = 0.09f;

  public static final float MAX_RAINFALL = 500f;

  /**
   * TFC 3 adds its share at every step along a river edge, about twice to
   * a cell the edge runs through; edges that meet add up, so a valley is
   * wettest at bends and confluences.
   */
  private static final float VALLEY_RAINFALL_SHARE =
    1f - (1f - RAINFALL_SHARE) * (1f - RAINFALL_SHARE);

  /**
   * TFC 3 wets the square of cells an edge steps through, a strip about a
   * cell to each side. Here an edge wets a cell in full within the core
   * distance of it and not at all beyond the reach, so the strip is as
   * narrow but ends in a slope, not at cell borders.
   */
  private static final float VALLEY_WET_CORE = 0.5f;
  private static final float VALLEY_WET_REACH = 1.25f;

  /**
   * How far outside a region the middle of an edge may lie and still wet a
   * valley in it: half the longest edge and the wet reach.
   */
  private static final int EDGE_MARGIN = Mth.ceil(
    0.5f * MAX_EDGE_LENGTH + VALLEY_WET_REACH
  );

  /**
   * Rivers this wide have a wet valley (TFC 3's river valley). TFC 2 has no
   * river valley biome, so only the rainfall of it is kept.
   */
  private static final int VALLEY_MIN_WIDTH = 15;

  /** TFC's low band of discrete biome altitude. */
  private static final int LOWLAND_ALTITUDE = 0;

  private static final int BUCKET_BITS = 4;
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

  /** River width by edge: TFC 2 rivers are all one width, so only valleys use it. */
  private final byte[] width;

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
    int[] downstream,
    byte[] width
  ) {
    this.halfGridX = halfGridX;
    this.halfGridZ = halfGridZ;
    this.sourceX = sourceX;
    this.sourceZ = sourceZ;
    this.drainX = drainX;
    this.drainZ = drainZ;
    this.width = width;
    this.fed = new boolean[downstream.length];
    for (final int below : downstream) {
      if (below >= 0) {
        fed[below] = true;
      }
    }
    for (int edge = 0; edge < downstream.length; edge++) {
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
    final Network network = tryLoadNetwork();
    if (network == null) {
      return null;
    }
    return fromNetwork(
      network,
      (float) horizontalScale / RegionCoords.GRID_WIDTH_IN_BLOCK,
      (float) verticalScale / RegionCoords.GRID_WIDTH_IN_BLOCK
    );
  }

  /**
   * The river network of the active profile as stored, or {@code null} if it
   * has none.
   */
  @Nullable
  public static Network tryLoadNetwork() {
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
      return readNetwork(new DataInputStream(new BufferedInputStream(stream)));
    } catch (IOException e) {
      throw new IllegalStateException(
        "Failed to read " + FILE + " for profile " + profileId,
        e
      );
    }
  }

  private static Network readNetwork(DataInputStream in) throws IOException {
    if (in.readInt() != MAGIC || in.readInt() != VERSION) {
      throw new IOException("Not a version " + VERSION + " " + FILE);
    }
    final int vertexCount = in.readInt();
    final float[] x = new float[vertexCount];
    final float[] z = new float[vertexCount];
    for (int i = 0; i < vertexCount; i++) {
      x[i] = in.readShort() / COORD_RANGE;
      z[i] = in.readShort() / COORD_RANGE;
    }
    final int edgeCount = in.readInt();
    final int[] source = new int[edgeCount];
    final int[] drain = new int[edgeCount];
    final int[] width = new int[edgeCount];
    for (int i = 0; i < edgeCount; i++) {
      source[i] = in.readInt();
      drain[i] = in.readInt();
      width[i] = in.readUnsignedByte();
    }
    return new Network(x, z, source, drain, width);
  }

  private static MapRivers fromNetwork(
    Network network,
    float halfGridX,
    float halfGridZ
  ) {
    final int vertexCount = network.x().length;
    final float[] x = new float[vertexCount];
    final float[] z = new float[vertexCount];
    for (int i = 0; i < vertexCount; i++) {
      x[i] = network.x()[i] * halfGridX;
      z[i] = network.z()[i] * halfGridZ;
    }

    final int edgeCount = network.source().length;
    final List<float[]> pieces = new ArrayList<>(edgeCount);
    final IntArrayList drainVertex = new IntArrayList(edgeCount);
    final IntArrayList widths = new IntArrayList(edgeCount);
    final int[] firstPieceFromVertex = new int[vertexCount];
    Arrays.fill(firstPieceFromVertex, -1);
    for (int i = 0; i < edgeCount; i++) {
      final int source = network.source()[i];
      final int drain = network.drain()[i];
      final int edgeWidth = network.width()[i];
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
        // Inner pieces drain into the next piece; the last one at the vertex.
        drainVertex.add(piece == split - 1 ? drain : -1);
        widths.add(edgeWidth);
      }
    }

    final int count = pieces.size();
    final float[] sourceX = new float[count];
    final float[] sourceZ = new float[count];
    final float[] drainX = new float[count];
    final float[] drainZ = new float[count];
    final int[] downstream = new int[count];
    final byte[] width = new byte[count];
    for (int i = 0; i < count; i++) {
      final float[] piece = pieces.get(i);
      sourceX[i] = piece[0];
      sourceZ[i] = piece[1];
      drainX[i] = piece[2];
      drainZ[i] = piece[3];
      final int vertex = drainVertex.getInt(i);
      downstream[i] = vertex < 0 ? i + 1 : firstPieceFromVertex[vertex];
      width[i] = (byte) widths.getInt(i);
    }
    return new MapRivers(
      halfGridX,
      halfGridZ,
      sourceX,
      sourceZ,
      drainX,
      drainZ,
      downstream,
      width
    );
  }

  /**
   * Gives the region the map rivers that lie in it, in place of vanilla's
   * AddRiversAndLakes, and the rainfall of the valleys of the wide ones.
   * Past the edge of the map the world is mirrored, and
   * its rivers with it.
   */
  public void addTo(Region region, long worldSeed) {
    final Long2ObjectOpenHashMap<RiverEdge> edges =
      new Long2ObjectOpenHashMap<>();
    final List<RiverEdge> rivers = new ArrayList<>();
    // By cell: the share of the way to the wettest climate still left.
    final Int2FloatOpenHashMap dryness = new Int2FloatOpenHashMap();
    dryness.defaultReturnValue(1f);
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
        addTile(region, worldSeed, tileX, tileZ, edges, rivers, dryness);
      }
    }
    ((RegionAccessor) (Object) region).tfcrealworld$setRivers(rivers);
    for (final var entry : dryness.int2FloatEntrySet()) {
      final Region.Point point = region.data()[entry.getIntKey()];
      point.rainfall +=
        (1f - entry.getFloatValue()) * (MAX_RAINFALL - point.rainfall);
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
        final Region.Point point = region.maybeAt(
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

  /**
   * The rivers of one copy of the map: itself, or mirrored as the maps are
   * (BasePNGNoise: mirrored past an edge, turned round past a pole). The
   * region owns the edges whose middle lies in it; a valley is wetted by
   * every edge that runs near, also by those of the regions next to it.
   */
  private void addTile(
    Region region,
    long worldSeed,
    int tileX,
    int tileZ,
    Long2ObjectOpenHashMap<RiverEdge> edges,
    List<RiverEdge> rivers,
    Int2FloatOpenHashMap dryness
  ) {
    final float centreX = tileX * 2f * halfGridX;
    final float centreZ = tileZ * 2f * halfGridZ;
    final float signX = ((tileX + tileZ) & 1) == 0 ? 1f : -1f;
    final float signZ = (tileZ & 1) == 0 ? 1f : -1f;
    final float fromX = signX * (region.minX() - centreX);
    final float toX = signX * (region.maxX() - centreX);
    final float fromZ = signZ * (region.minZ() - centreZ);
    final float toZ = signZ * (region.maxZ() - centreZ);
    final long tileKey = tileKey(tileX, tileZ);
    for (
      int bucketZ =
        Mth.floor(Math.min(fromZ, toZ) - EDGE_MARGIN) >> BUCKET_BITS;
      bucketZ <= Mth.floor(Math.max(fromZ, toZ) + EDGE_MARGIN) >> BUCKET_BITS;
      bucketZ++
    ) {
      for (
        int bucketX =
          Mth.floor(Math.min(fromX, toX) - EDGE_MARGIN) >> BUCKET_BITS;
        bucketX <= Mth.floor(Math.max(fromX, toX) + EDGE_MARGIN) >> BUCKET_BITS;
        bucketX++
      ) {
        final IntArrayList inBucket = buckets.get(bucket(bucketX, bucketZ));
        if (inBucket == null) {
          continue;
        }
        for (int i = 0; i < inBucket.size(); i++) {
          final int edge = inBucket.getInt(i);
          final float x0 = centreX + signX * sourceX[edge];
          final float z0 = centreZ + signZ * sourceZ[edge];
          final float x1 = centreX + signX * drainX[edge];
          final float z1 = centreZ + signZ * drainZ[edge];
          if (width[edge] >= VALLEY_MIN_WIDTH) {
            wetValley(region, x0, z0, x1, z1, dryness);
          }
          if (
            region.maybeAt(
              Math.round(0.5f * (x0 + x1)),
              Math.round(0.5f * (z0 + z1))
            ) ==
            null
          ) {
            continue;
          }
          final long key = tileKey | edge;
          final RiverEdge river = createEdge(key, worldSeed);
          edges.put(key, river);
          rivers.add(river);
        }
      }
    }
  }

  private static long tileKey(int tileX, int tileZ) {
    return ((long) (tileX & 0xFFFF) << 48) | ((long) (tileZ & 0xFFFF) << 32);
  }

  /** The edge of a tile and edge index, the same whichever region asks. */
  private RiverEdge createEdge(long key, long worldSeed) {
    final int tileX = (short) (key >>> 48);
    final int tileZ = (short) (key >>> 32);
    final int edge = (int) key;
    final float centreX = tileX * 2f * halfGridX;
    final float centreZ = tileZ * 2f * halfGridZ;
    final float signX = ((tileX + tileZ) & 1) == 0 ? 1f : -1f;
    final float signZ = (tileZ & 1) == 0 ? 1f : -1f;
    final float fromX = centreX + signX * sourceX[edge];
    final float fromZ = centreZ + signZ * sourceZ[edge];
    final double dx = centreX + signX * drainX[edge] - fromX;
    final double dz = centreZ + signZ * drainZ[edge] - fromZ;
    final double angle = Math.atan2(dz, dx);
    final double length = Math.sqrt(dx * dx + dz * dz);
    final RiverEdge river = new RiverEdge(
      new RiverFractal.Edge(
        new RiverFractal.Vertex(fromX, fromZ, (float) angle, (float) length, 0),
        new RiverFractal.Vertex(
          (float) (fromX + dx),
          (float) (fromZ + dz),
          (float) angle,
          (float) length,
          0
        )
      ),
      new XoroshiroRandomSource(worldSeed ^ ((key + 1) * GOLDEN_RATIO))
    );
    river.setSource(!fed[edge]);
    return river;
  }

  /**
   * TFC 3 annotateRiverGridScale: how much wetter a wide river makes the
   * lowland cells around it, the nearer it runs the wetter, so the wet
   * strip fades to the sides instead of ending at cell borders.
   */
  private static void wetValley(
    Region region,
    float x0,
    float z0,
    float x1,
    float z1,
    Int2FloatOpenHashMap dryness
  ) {
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
        final Region.Point point = region.maybeAt(x, z);
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
        if (wet > 0f) {
          dryness.put(
            region.index(x, z),
            dryness.get(region.index(x, z)) * (1f - VALLEY_RAINFALL_SHARE * wet)
          );
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

  /** Only lowland cells are a valley: higher up a river runs in a gorge. */
  private static boolean isLowland(@Nullable Region.Point point) {
    return (
      point != null &&
      point.land() &&
      point.discreteBiomeAltitude() == LOWLAND_ALTITUDE
    );
  }

  /** The copy of the map a grid coordinate lies in (0 is the map itself). */
  private static int tile(int grid, float halfGrid) {
    return Mth.floor((grid + halfGrid) / (2f * halfGrid));
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

  /**
   * The rivers as stored: vertices from -1 to 1 across the map, each edge
   * from a source vertex to the drain vertex below it, and its width.
   */
  public record Network(
    float[] x,
    float[] z,
    int[] source,
    int[] drain,
    int[] width
  ) {}
}
