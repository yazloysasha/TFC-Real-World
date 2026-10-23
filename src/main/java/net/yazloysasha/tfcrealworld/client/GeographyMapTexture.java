package net.yazloysasha.tfcrealworld.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.awt.image.BufferedImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.noise.png.BasePNGNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise.ContinentBand;
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
      Integer.toHexString(GeographyUI.OCEAN_ARGB) +
      "|" +
      TFCRealWorldConfig.LAKES_FROM_MAP.get();
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
    for (int y = 0; y < th; y++) {
      int sy = ((2 * y + 1) * sh) / (2 * th);
      for (int x = 0; x < tw; x++) {
        int sx = ((2 * x + 1) * sw) / (2 * tw);
        int gray = (source.getRGB(sx, sy) >> 16) & 0xFF;
        ContinentBand band = PNGContinentNoise.bandFromBrightness(gray);
        boolean water =
          band == ContinentBand.OCEAN ||
          (lakesFromMap &&
            (band == ContinentBand.LAKE || band == ContinentBand.SALT_LAKE));
        image.setPixelRGBA(x, y, water ? ocean : land);
      }
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
