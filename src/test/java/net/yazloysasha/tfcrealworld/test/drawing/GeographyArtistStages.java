package net.yazloysasha.tfcrealworld.test.drawing;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.layer.framework.Area;
import net.dries007.tfc.world.layer.framework.AreaFactory;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.Units;
import net.minecraft.core.QuartPos;
import net.yazloysasha.tfcrealworld.client.GeographyUI;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.geography.GeographyKind;
import net.yazloysasha.tfcrealworld.util.geography.GeographyManager;
import net.yazloysasha.tfcrealworld.util.geography.GeographyNode;
import net.yazloysasha.tfcrealworld.util.profile.MapProfile;
import net.yazloysasha.tfcrealworld.util.projection.ProjectionManager;
import net.yazloysasha.tfcrealworld.world.noise.png.BasePNGNoise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * High-res artist stages 27–28: GeographyUI-colored continent previews with
 * waypoint dots and English titles, written via {@link Draw} like stages 0–26.
 * <ul>
 *   <li>27 — source {@code continent.png} mask (soft gray blend like
 *       {@code GeographyMapTexture})</li>
 *   <li>28 — World Preview–style land: sample zoomed biome layers at quart /
 *       block coordinates and paint land for every biome outside the preview
 *       ocean whitelist (coasts and shores remain land; not
 *       {@link net.dries007.tfc.world.region.Region.Point#land()} alone)</li>
 * </ul>
 */
public final class GeographyArtistStages {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    GeographyArtistStages.class
  );

  /** Stage 27 source-mask upscale (source pixel → scale×scale). */
  public static final int SCALE = 16;

  /**
   * Stage 28 output upscale — same as stage 27 ({@link #SCALE}) to keep peak
   * memory and lag down; shore detail still comes from quart biome sampling.
   * Writes the finished map directly (no second Draw buffer on 2:1 panels).
   */
  public static final int GENERATED_SCALE = 16;

  private static final Color OCEAN = new Color(GeographyUI.OCEAN_ARGB, true);
  private static final Color LAND = new Color(GeographyUI.LAND_ARGB, true);
  private static final Color PANEL_BG = new Color(
    GeographyUI.PANEL_BG_ARGB,
    true
  );
  private static final Color WP_NORMAL = new Color(31, 31, 36);
  private static final Color WP_CAPITAL = new Color(56, 51, 46);
  private static final Color LABEL = new Color(240, 244, 255);

  private GeographyArtistStages() {}

  public static void drawSourceMask(String name) {
    MapProfile profile = loadProfile();
    BufferedImage source = BasePNGNoise.loadImage("continent");
    if (source == null) {
      throw new IllegalStateException(
        "continent.png missing for profile " +
        TFCRealWorldConfig.DEFAULT_MAP_PROFILE
      );
    }
    int sw = source.getWidth();
    int sh = source.getHeight();
    int mw = sw * SCALE;
    int mh = sh * SCALE;
    LOGGER.info(
      "Geography stage 27: source mask {}×{} → {}×{} (×{})",
      sw,
      sh,
      mw,
      mh,
      SCALE
    );

    BufferedImage map = new BufferedImage(mw, mh, BufferedImage.TYPE_INT_RGB);
    // Nearest-neighbor upscale of soft-blended source colors (GeographyMapTexture).
    for (int y = 0; y < mh; y++) {
      int sy = Math.min(sh - 1, y / SCALE);
      for (int x = 0; x < mw; x++) {
        int sx = Math.min(sw - 1, x / SCALE);
        int gray = (source.getRGB(sx, sy) >> 16) & 0xFF;
        map.setRGB(x, y, blendOceanLand(gray).getRGB());
      }
    }

    plotWaypointsAndLabels(map, profile, SCALE);
    writeContainPanel(name, map);
  }

  /**
   * Stage 28: mirror World Preview / in-game biome sampling — build the zoomed
   * region biome layer ({@link TFCLayers#createRegionBiomeLayer}), query at
   * quart coordinates derived from real block positions across the profile
   * extent, and paint land for every biome that is not open ocean
   * ({@link #isPreviewOcean(int)}). Coastal and shore biomes remain land;
   * reefs, oceanic volcanic arcs, and {@link TFCLayers#SUNKEN_SHIELD_VOLCANO}
   * are treated as ocean. Then NN-upscale to {@link #GENERATED_SCALE}.
   */
  public static void drawGeneratedLand(String name, RegionGenerator generator) {
    MapProfile profile = loadProfile();
    BufferedImage source = BasePNGNoise.loadImage("continent");
    if (source == null) {
      throw new IllegalStateException(
        "continent.png missing for profile " +
        TFCRealWorldConfig.DEFAULT_MAP_PROFILE
      );
    }
    int sw = source.getWidth();
    int sh = source.getHeight();
    int scale = GENERATED_SCALE;
    int mw = sw * scale;
    int mh = sh * scale;
    int hs = profile.horizontalScale();
    int vs = profile.verticalScale();

    // Match TFCChunkGenerator: RegionGenerator then biome layer share one Seed.
    final AreaFactory biomeLayerFactory = TFCLayers.createRegionBiomeLayer(
      generator,
      generator.seed()
    );
    final Area biomeLayer = biomeLayerFactory.get();

    // Quart-resolution land mask over the profile block extent (World Preview
    // samples biomes at quart scale; shore layers live between grid and quart).
    final int blockMinX = -hs;
    final int blockMaxX = hs - 1;
    final int blockMinZ = -vs;
    final int blockMaxZ = vs - 1;
    final int quartMinX = QuartPos.fromBlock(blockMinX);
    final int quartMaxX = QuartPos.fromBlock(blockMaxX);
    final int quartMinZ = QuartPos.fromBlock(blockMinZ);
    final int quartMaxZ = QuartPos.fromBlock(blockMaxZ);
    final int qw = quartMaxX - quartMinX + 1;
    final int qh = quartMaxZ - quartMinZ + 1;

    LOGGER.info(
      "Geography stage 28: sampling non–open-ocean biomes at quart {}×{} ([{}..{}],[{}..{}]) → {}×{} (×{})",
      qw,
      qh,
      quartMinX,
      quartMaxX,
      quartMinZ,
      quartMaxZ,
      mw,
      mh,
      scale
    );

    // Warm region pipeline at grid scale so biome zoom layers hit populated points.
    final int gridMinX = Units.blockToGrid(blockMinX);
    final int gridMaxX = Units.blockToGrid(blockMaxX);
    final int gridMinZ = Units.blockToGrid(blockMinZ);
    final int gridMaxZ = Units.blockToGrid(blockMaxZ);
    for (int gz = gridMinZ; gz <= gridMaxZ; gz++) {
      for (int gx = gridMinX; gx <= gridMaxX; gx++) {
        generator.getOrCreateRegionPoint(gx, gz);
      }
    }

    final boolean[] land = new boolean[qw * qh];
    int landCount = 0;
    for (int qz = quartMinZ; qz <= quartMaxZ; qz++) {
      final int row = (qz - quartMinZ) * qw;
      for (int qx = quartMinX; qx <= quartMaxX; qx++) {
        final int biome = biomeLayer.get(qx, qz);
        final boolean isLand = !isPreviewOcean(biome);
        land[(qx - quartMinX) + row] = isLand;
        if (isLand) {
          landCount++;
        }
      }
    }
    LOGGER.info(
      "Geography stage 28: land quarts {} / {} ({}%)",
      landCount,
      land.length,
      String.format(
        java.util.Locale.ROOT,
        "%.1f",
        (100.0 * landCount) / Math.max(1, land.length)
      )
    );

    BufferedImage map = new BufferedImage(mw, mh, BufferedImage.TYPE_INT_RGB);
    int oceanRgb = OCEAN.getRGB();
    int landRgb = LAND.getRGB();
    long spanX = Math.max(1L, 2L * hs);
    long spanZ = Math.max(1L, 2L * vs);
    for (int y = 0; y < mh; y++) {
      // Match GeographyMapViewport / Python block_to_map_pixel inverse.
      int blockZ = (int) Math.floor(-vs + ((y + 0.5) * spanZ) / mh);
      int qz = QuartPos.fromBlock(blockZ);
      int zi = Math.clamp(qz - quartMinZ, 0, qh - 1);
      int row = zi * qw;
      for (int x = 0; x < mw; x++) {
        int blockX = (int) Math.floor(-hs + ((x + 0.5) * spanX) / mw);
        int qx = QuartPos.fromBlock(blockX);
        int xi = Math.clamp(qx - quartMinX, 0, qw - 1);
        map.setRGB(x, y, land[xi + row] ? landRgb : oceanRgb);
      }
    }

    plotWaypointsAndLabels(map, profile, scale);
    writeContainPanel(name, map);
  }

  /**
   * Open-water ocean whitelist for stage 28 geography preview.
   * <p>
   * In addition to the open-water ids, this includes {@code OCEAN_REEF},
   * {@code OCEANIC_VOLCANIC_ARC}, and {@link TFCLayers#SUNKEN_SHIELD_VOLCANO}
   * for the stage 28 preview. All shore / coast biome ids
   * ({@code SHORE}, {@code TIDAL_FLATS}, {@code SEA_STACKS}, terraces,
   * {@code SETBACK_CLIFFS}, {@code COASTAL_DUNES}, {@code ROCKY_SHORES},
   * {@code EMBAYMENTS}, shield-volcano shores, {@code ICE_SHEET_SHORE},
   * {@code SALT_MARSH}, {@code TOWER_KARST_BAY}, …) are outside this whitelist
   * and therefore land.
   */
  static boolean isPreviewOcean(int biome) {
    return (
      biome == TFCLayers.OCEAN ||
      biome == TFCLayers.OCEAN_ATOLLS ||
      biome == TFCLayers.DEEP_OCEAN ||
      biome == TFCLayers.DEEP_OCEAN_ATOLLS ||
      biome == TFCLayers.DEEP_OCEAN_TRENCH ||
      biome == TFCLayers.OCEAN_RIDGE ||
      biome == TFCLayers.SUNKEN_SHIELD_VOLCANO ||
      biome == TFCLayers.OCEAN_REEF ||
      biome == TFCLayers.OCEANIC_VOLCANIC_ARC
    );
  }

  private static MapProfile loadProfile() {
    return MapProfile.loadFromResources(TFCRealWorldConfig.DEFAULT_MAP_PROFILE);
  }

  private static Color blendOceanLand(int gray) {
    int g = Math.clamp(gray, 0, 255);
    int or = OCEAN.getRed();
    int og = OCEAN.getGreen();
    int ob = OCEAN.getBlue();
    int lr = LAND.getRed();
    int lg = LAND.getGreen();
    int lb = LAND.getBlue();
    int r = (or * (255 - g) + lr * g) / 255;
    int gr = (og * (255 - g) + lg * g) / 255;
    int b = (ob * (255 - g) + lb * g) / 255;
    return new Color(r, gr, b);
  }

  /**
   * Diameter smaller than one upscaled source land pixel ({@code scale}×{@code scale}).
   */
  private static int dotRadius(int scale) {
    return Math.max(1, scale / 8);
  }

  private static void plotWaypointsAndLabels(
    BufferedImage map,
    MapProfile profile,
    int scale
  ) {
    GeographyManager.initialize();
    int mw = map.getWidth();
    int mh = map.getHeight();
    int hs = profile.horizontalScale();
    int vs = profile.verticalScale();
    int r = dotRadius(scale);
    // Slightly larger than the prior SCALE/2 default (was 8pt at ×16).
    int fontSize = Math.max(10, scale / 2 + 2);
    Font font = new Font(Font.SANS_SERIF, Font.PLAIN, fontSize);

    List<Plotted> plotted = new ArrayList<>();
    for (String ref : profile.waypoints()) {
      GeographyNode node = GeographyManager.get(ref);
      if (
        node == null ||
        node.kind() != GeographyKind.WAYPOINT ||
        node.latitude() == null ||
        node.longitude() == null
      ) {
        continue;
      }
      int[] block = toBlockXZ(node.latitude(), node.longitude(), profile);
      int[] pix = blockToMapPixel(block[0], block[1], mw, mh, hs, vs);
      if (pix[0] < -2 || pix[1] < -2 || pix[0] > mw + 2 || pix[1] > mh + 2) {
        continue;
      }
      String title = node.getTitle("en_us");
      if (title == null || title.isBlank()) {
        title = node.slug();
      }
      plotted.add(new Plotted(pix[0], pix[1], node.capital(), title));
    }

    Graphics2D g = map.createGraphics();
    try {
      g.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON
      );
      g.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING,
        RenderingHints.VALUE_TEXT_ANTIALIAS_ON
      );
      g.setFont(font);
      var fm = g.getFontMetrics();
      for (Plotted p : plotted) {
        Color fill = p.capital ? WP_CAPITAL : WP_NORMAL;
        g.setColor(fill);
        g.fillOval(p.x - r, p.y - r, r * 2 + 1, r * 2 + 1);
        g.setColor(LABEL);
        int tx = p.x + r + 2;
        int ty = p.y + fm.getAscent() / 2 - 1;
        g.drawString(p.title, tx, ty);
      }
    } finally {
      g.dispose();
    }
    LOGGER.info(
      "Geography waypoints plotted: {} / {} listed (dot r={}px, font={}pt)",
      plotted.size(),
      profile.waypoints().size(),
      r,
      fontSize
    );
  }

  private static int[] toBlockXZ(
    double latitude,
    double longitude,
    MapProfile profile
  ) {
    double west = profile.westEdgeLongitude();
    double east = profile.eastEdgeLongitude();
    double fitted = fitLongitude(longitude, west, east);
    double[] classic = ProjectionManager.geographicToClassic(
      fitted,
      latitude,
      profile.horizontalScale(),
      profile.verticalScale(),
      west,
      east,
      profile.southEdgeLatitude(),
      profile.northEdgeLatitude(),
      profile.mapProjection()
    );
    int x = (int) Math.round(
      Math.clamp(
        classic[0],
        -profile.horizontalScale(),
        profile.horizontalScale()
      )
    );
    int z = (int) Math.round(
      Math.clamp(classic[1], -profile.verticalScale(), profile.verticalScale())
    );
    return new int[] { x, z };
  }

  /** Same as {@code WaypointCoordinates.fitLongitude}. */
  private static double fitLongitude(
    double longitude,
    double west,
    double east
  ) {
    double mid = (west + east) / 2.0;
    Double bestInRange = null;
    double bestInRangeDist = Double.POSITIVE_INFINITY;
    for (int k = -2; k <= 2; k++) {
      double candidate = longitude + 360.0 * k;
      if (candidate >= west && candidate <= east) {
        double dist = Math.abs(candidate - mid);
        if (dist < bestInRangeDist) {
          bestInRangeDist = dist;
          bestInRange = candidate;
        }
      }
    }
    return bestInRange != null ? bestInRange : longitude;
  }

  private static int[] blockToMapPixel(
    int blockX,
    int blockZ,
    int mapW,
    int mapH,
    int hs,
    int vs
  ) {
    int spanX = Math.max(1, 2 * hs);
    int spanZ = Math.max(1, 2 * vs);
    int sx = (int) (((long) (blockX - (-hs)) * mapW) / spanX);
    int sy = (int) (((long) (blockZ - (-vs)) * mapH) / spanZ);
    return new int[] { sx, sy };
  }

  /**
   * Place map into a GeographyUI MAP_W×MAP_H aspect (2:1) panel with
   * PANEL_BG letterbox — same contain-fit idea as GeographyMapViewport zoom 1.
   * Exact 2:1 maps are written directly to avoid a second full-size buffer
   * (important for stage 28 at {@link #GENERATED_SCALE}).
   */
  private static void writeContainPanel(String name, BufferedImage mapImg) {
    int baseW = mapImg.getWidth();
    int baseH = mapImg.getHeight();
    float aspect = baseW / (float) Math.max(1, baseH);
    float panelAspect = GeographyUI.MAP_W / (float) GeographyUI.MAP_H;

    int panelW;
    int panelH;
    if (aspect + 1e-6f >= panelAspect) {
      panelW = baseW;
      panelH = Math.max(1, Math.round(baseW / panelAspect));
    } else {
      panelH = baseH;
      panelW = Math.max(1, Math.round(baseH * panelAspect));
    }
    int contentLeft = (panelW - baseW) / 2;
    int contentTop = (panelH - baseH) / 2;

    if (
      panelW == baseW && panelH == baseH && contentLeft == 0 && contentTop == 0
    ) {
      writePng(name, mapImg);
    } else {
      Draw.draw(name, panelW, panelH, image -> {
        Graphics2D g = image.createGraphics();
        try {
          g.setColor(PANEL_BG);
          g.fillRect(0, 0, panelW, panelH);
          g.drawImage(mapImg, contentLeft, contentTop, null);
        } finally {
          g.dispose();
        }
      });
    }
    LOGGER.info(
      "Geography wrote {} ({}×{}, letterbox offset {},{})",
      name,
      panelW,
      panelH,
      contentLeft,
      contentTop
    );
  }

  private static void writePng(String name, BufferedImage image) {
    Helpers.uncheck(() -> {
      new File(Draw.OUTPUT).mkdirs();
      ImageIO.write(image, "PNG", new File(Draw.OUTPUT + "/" + name + ".png"));
    });
  }

  private record Plotted(int x, int y, boolean capital, String title) {}
}
