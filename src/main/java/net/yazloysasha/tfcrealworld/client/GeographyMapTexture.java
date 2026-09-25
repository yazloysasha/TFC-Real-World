package net.yazloysasha.tfcrealworld.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.awt.image.BufferedImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.noise.png.BasePNGNoise;
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

  private static @Nullable DynamicTexture texture;
  private static @Nullable ResourceLocation location;
  private static @Nullable String cacheKey;
  private static int texWidth = 2;
  private static int texHeight = 1;

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
      Integer.toHexString(GeographyUI.OCEAN_ARGB);
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
    int maxEdge = GeographyUI.TEXTURE_MAX_EDGE;
    if (source == null) {
      int hs = Math.max(1, TFCRealWorldConfig.HORIZONTAL_SCALE.get());
      int vs = Math.max(1, TFCRealWorldConfig.VERTICAL_SCALE.get());
      if (hs >= vs) {
        tw = maxEdge;
        th = Math.max(1, Math.round(maxEdge * (vs / (float) hs)));
      } else {
        th = maxEdge;
        tw = Math.max(1, Math.round(maxEdge * (hs / (float) vs)));
      }
    } else {
      int sw = Math.max(1, source.getWidth());
      int sh = Math.max(1, source.getHeight());
      if (sw >= sh) {
        tw = Math.min(sw, maxEdge);
        th = Math.max(1, Math.round(tw * (sh / (float) sw)));
      } else {
        th = Math.min(sh, maxEdge);
        tw = Math.max(1, Math.round(th * (sw / (float) sh)));
      }
    }
    texWidth = tw;
    texHeight = th;

    NativeImage image = new NativeImage(tw, th, false);
    if (source == null) {
      fill(image, tw, th, GeographyUI.OCEAN_ARGB);
    } else {
      paintFromMask(image, source, tw, th);
    }
    texture = new DynamicTexture(image);
    // Nearest-neighbor: bilinear sampling causes marker/map jitter on zoom.
    texture.setFilter(false, false);
    location = TFCRealWorld.id("dynamic/geography_map");
    Minecraft.getInstance().getTextureManager().register(location, texture);
    cacheKey = key;
  }

  private static void fill(NativeImage image, int tw, int th, int argb) {
    int abgr = toAbgr(argb);
    for (int y = 0; y < th; y++) {
      for (int x = 0; x < tw; x++) {
        image.setPixelRGBA(x, y, abgr);
      }
    }
  }

  private static void paintFromMask(
    NativeImage image,
    BufferedImage source,
    int tw,
    int th
  ) {
    int sw = source.getWidth();
    int sh = source.getHeight();
    int oceanR = (GeographyUI.OCEAN_ARGB >> 16) & 0xFF;
    int oceanG = (GeographyUI.OCEAN_ARGB >> 8) & 0xFF;
    int oceanB = GeographyUI.OCEAN_ARGB & 0xFF;
    int landR = (GeographyUI.LAND_ARGB >> 16) & 0xFF;
    int landG = (GeographyUI.LAND_ARGB >> 8) & 0xFF;
    int landB = GeographyUI.LAND_ARGB & 0xFF;
    for (int y = 0; y < th; y++) {
      for (int x = 0; x < tw; x++) {
        int x0 = (x * sw) / tw;
        int y0 = (y * sh) / th;
        int x1 = Math.min(sw - 1, ((x + 1) * sw) / tw);
        int y1 = Math.min(sh - 1, ((y + 1) * sh) / th);
        if (x1 < x0) {
          x1 = x0;
        }
        if (y1 < y0) {
          y1 = y0;
        }
        long sum = 0;
        int n = 0;
        for (int sy = y0; sy <= y1; sy++) {
          for (int sx = x0; sx <= x1; sx++) {
            sum += (source.getRGB(sx, sy) >> 16) & 0xFF;
            n++;
          }
        }
        int gray = n == 0 ? 0 : (int) (sum / n);
        // Soft AA shoreline: blend ocean↔land by mask gray (0=ocean, 255=land).
        int r = (oceanR * (255 - gray) + landR * gray) / 255;
        int g = (oceanG * (255 - gray) + landG * gray) / 255;
        int b = (oceanB * (255 - gray) + landB * gray) / 255;
        int argb = 0xFF000000 | (r << 16) | (g << 8) | b;
        image.setPixelRGBA(x, y, toAbgr(argb));
      }
    }
  }

  /**
   * NativeImage expects ABGR packed ints.
   */
  private static int toAbgr(int argb) {
    int a = (argb >> 24) & 0xFF;
    int r = (argb >> 16) & 0xFF;
    int g = (argb >> 8) & 0xFF;
    int b = argb & 0xFF;
    return (a << 24) | (b << 16) | (g << 8) | r;
  }
}
