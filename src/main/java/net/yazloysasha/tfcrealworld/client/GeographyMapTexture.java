package net.yazloysasha.tfcrealworld.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import java.awt.image.BufferedImage;
import net.dries007.tfc.world.river.River;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.noise.png.BasePNGNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise.ContinentBand;
import net.yazloysasha.tfcrealworld.world.river.MapRivers;
import org.jetbrains.annotations.Nullable;

/**
 * Land/ocean overview from the active profile's {@code continent.png}.
 * <p>
 * Texture size/aspect matches the source mask (same-aspect downsample): full
 * world masks are 2:1, old/new world masks are 1:1. Full PNG UV maps to world
 * block bounds {@code ±HORIZONTAL_SCALE} × {@code ±VERTICAL_SCALE} — the same
 * space as {@code BasePNGNoise} and waypoint classic coordinates.
 */
public final class GeographyMapTexture {

  private static @Nullable UploadedTexture texture;
  private static @Nullable ResourceLocation location;
  private static @Nullable String cacheKey;
  private static int texWidth = 2;
  private static int texHeight = 1;

  /** TFC's RiverEdge: how often a river edge is bisected. */
  private static final int RIVER_BISECTIONS = 4;
  private static final long RIVER_BEND_SALT = 0x9E3779B97F4A7C15L;

  private GeographyMapTexture() {}

  public static ResourceLocation getOrCreate() {
    String key =
      TFCRealWorldConfig.MAP_PROFILE.get() +
      "|" +
      TFCRealWorldConfig.HORIZONTAL_SCALE.get() +
      "|" +
      TFCRealWorldConfig.VERTICAL_SCALE.get() +
      "|" +
      Integer.toHexString(GeographyUI.LAND_ARGB) +
      "|" +
      Integer.toHexString(GeographyUI.OCEAN_ARGB) +
      "|" +
      TFCRealWorldConfig.LAKES_FROM_MAP.get() +
      "|" +
      TFCRealWorldConfig.RIVERS_FROM_MAP.get();
    if (location != null && key.equals(cacheKey)) {
      return location;
    }
    rebuild(key);
    return location;
  }

  public static int width() {
    getOrCreate();
    return texWidth;
  }

  public static int height() {
    getOrCreate();
    return texHeight;
  }

  public static float aspectRatio() {
    getOrCreate();
    return texWidth / (float) Math.max(1, texHeight);
  }

  public static void clear() {
    if (texture != null) {
      texture.close();
      texture = null;
    }
    location = null;
    cacheKey = null;
  }

  private static void rebuild(String key) {
    clear();
    BufferedImage source = BasePNGNoise.loadImage("continent");
    int tw;
    int th;
    int maxShort = GeographyUI.TEXTURE_MAX_SHORT_EDGE;
    if (source == null) {
      int hs = Math.max(1, TFCRealWorldConfig.HORIZONTAL_SCALE.get());
      int vs = Math.max(1, TFCRealWorldConfig.VERTICAL_SCALE.get());
      if (hs >= vs) {
        th = maxShort;
        tw = Math.max(1, Math.round(maxShort * (hs / (float) vs)));
      } else {
        tw = maxShort;
        th = Math.max(1, Math.round(maxShort * (vs / (float) hs)));
      }
    } else {
      int sw = Math.max(1, source.getWidth());
      int sh = Math.max(1, source.getHeight());
      if (sw >= sh) {
        th = Math.min(sh, maxShort);
        tw = Math.max(1, Math.round(th * (sw / (float) sh)));
      } else {
        tw = Math.min(sw, maxShort);
        th = Math.max(1, Math.round(tw * (sh / (float) sw)));
      }
    }
    texWidth = tw;
    texHeight = th;

    NativeImage image = new NativeImage(tw, th, false);
    if (source == null) {
      image.fillRect(0, 0, tw, th, toAbgr(GeographyUI.OCEAN_ARGB));
    } else {
      paintFromMask(image, source, tw, th);
    }
    if (TFCRealWorldConfig.RIVERS_FROM_MAP.get()) {
      paintRivers(image, tw, th);
    }
    texture = new UploadedTexture(image);
    location = TFCRealWorld.id("dynamic/geography_map");
    Minecraft.getInstance().getTextureManager().register(location, texture);
    cacheKey = key;
  }

  /**
   * One source pixel per texel, at the texel's center, colored by the band the
   * world generator reads from it.
   */
  private static void paintFromMask(
    NativeImage image,
    BufferedImage source,
    int tw,
    int th
  ) {
    int sw = source.getWidth();
    int sh = source.getHeight();
    boolean lakesFromMap = TFCRealWorldConfig.LAKES_FROM_MAP.get();
    int land = toAbgr(GeographyUI.LAND_ARGB);
    int ocean = toAbgr(GeographyUI.OCEAN_ARGB);
    int[] row = new int[sw];
    for (int y = 0; y < th; y++) {
      int sy = ((2 * y + 1) * sh) / (2 * th);
      BasePNGNoise.readBrightnessRow(source, sy, row);
      for (int x = 0; x < tw; x++) {
        int gray = row[((2 * x + 1) * sw) / (2 * tw)];
        ContinentBand band = PNGContinentNoise.bandFromBrightness(gray);
        boolean water =
          band == ContinentBand.OCEAN ||
          (lakesFromMap &&
            (band == ContinentBand.LAKE || band == ContinentBand.SALT_LAKE));
        image.setPixelRGBA(x, y, water ? ocean : land);
      }
    }
  }

  /**
   * The profile's rivers in the ocean color, as wide as their channels. Each
   * edge bends as TFC bends river edges in the world, though not along the
   * same curve: the world's bends come from the world seed, which the client
   * lacks.
   */
  private static void paintRivers(NativeImage image, int tw, int th) {
    MapRivers.Network rivers = MapRivers.tryLoadNetwork();
    if (rivers == null) {
      return;
    }
    int water = toAbgr(GeographyUI.OCEAN_ARGB);
    float texelsPerBlock =
      tw / (2f * Math.max(1, TFCRealWorldConfig.HORIZONTAL_SCALE.get()));
    for (int edge = 0; edge < rivers.source().length; edge++) {
      int from = rivers.source()[edge];
      int to = rivers.drain()[edge];
      double x0 = texelCoord(rivers.x()[from], tw);
      double y0 = texelCoord(rivers.z()[from], th);
      double x1 = texelCoord(rivers.x()[to], tw);
      double y1 = texelCoord(rivers.z()[to], th);
      double angle = Math.atan2(y1 - y0, x1 - x0);
      double length = Math.hypot(x1 - x0, y1 - y0);
      double[] points = new River.Edge(
        new River.Vertex(x0, y0, angle, length, 0),
        new River.Vertex(x1, y1, angle, length, 0)
      ).fractal(
        new XoroshiroRandomSource((edge + 1) * RIVER_BEND_SALT),
        RIVER_BISECTIONS
      ).segments;
      // Stored width is half the channel's, in blocks.
      int brush = Math.max(
        1,
        Math.round(2 * rivers.width()[edge] * texelsPerBlock)
      );
      for (int i = 0; i + 3 < points.length; i += 2) {
        drawLine(
          image,
          texel(points[i], tw),
          texel(points[i + 1], th),
          texel(points[i + 2], tw),
          texel(points[i + 3], th),
          brush,
          water
        );
      }
    }
  }

  /** Where a coordinate from -1 to 1 across the map falls, in texels. */
  private static double texelCoord(float coord, int size) {
    return (coord + 1.0) * 0.5 * size;
  }

  /** The texel a position in texels falls in. */
  private static int texel(double position, int size) {
    return Math.min(size - 1, Math.max(0, (int) position));
  }

  /** Bresenham's line, both ends included, of a round brush this wide. */
  private static void drawLine(
    NativeImage image,
    int x0,
    int y0,
    int x1,
    int y1,
    int brush,
    int abgr
  ) {
    int dx = Math.abs(x1 - x0);
    int dy = -Math.abs(y1 - y0);
    int sx = x0 < x1 ? 1 : -1;
    int sy = y0 < y1 ? 1 : -1;
    int err = dx + dy;
    while (true) {
      stamp(image, x0, y0, brush, abgr);
      if (x0 == x1 && y0 == y1) {
        return;
      }
      int e2 = 2 * err;
      if (e2 >= dy) {
        err += dy;
        x0 += sx;
      }
      if (e2 <= dx) {
        err += dx;
        y0 += sy;
      }
    }
  }

  /** A round brush this wide, centered on the texel or, if even, its corner. */
  private static void stamp(
    NativeImage image,
    int x,
    int y,
    int brush,
    int abgr
  ) {
    int lo = -(brush / 2);
    int hi = (brush - 1) / 2;
    float centre = (lo + hi) * 0.5f;
    float radiusSq = brush * brush * 0.25f;
    for (int oy = lo; oy <= hi; oy++) {
      int py = y + oy;
      if (py < 0 || py >= image.getHeight()) {
        continue;
      }
      for (int ox = lo; ox <= hi; ox++) {
        int px = x + ox;
        float fx = ox - centre;
        float fy = oy - centre;
        if (px >= 0 && px < image.getWidth() && fx * fx + fy * fy <= radiusSq) {
          image.setPixelRGBA(px, py, abgr);
        }
      }
    }
  }

  /**
   * A texture that keeps no copy of its pixels once they are on the GPU, as
   * {@code DynamicTexture} does: tens of megabytes for the whole world.
   * Nearest-neighbor: bilinear sampling causes marker/map jitter on zoom.
   */
  private static final class UploadedTexture extends AbstractTexture {

    private UploadedTexture(NativeImage image) {
      TextureUtil.prepareImage(getId(), image.getWidth(), image.getHeight());
      image.upload(
        0,
        0,
        0,
        0,
        0,
        image.getWidth(),
        image.getHeight(),
        false,
        true,
        false,
        true
      );
    }

    @Override
    public void load(ResourceManager resourceManager) {}

    @Override
    public void close() {
      releaseId();
    }
  }

  /** NativeImage expects ABGR packed ints. */
  private static int toAbgr(int argb) {
    int a = (argb >> 24) & 0xFF;
    int r = (argb >> 16) & 0xFF;
    int g = (argb >> 8) & 0xFF;
    int b = argb & 0xFF;
    return (a << 24) | (b << 16) | (g << 8) | r;
  }
}
