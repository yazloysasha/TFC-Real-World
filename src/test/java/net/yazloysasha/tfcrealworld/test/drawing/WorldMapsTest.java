package net.yazloysasha.tfcrealworld.test.drawing;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;
import net.dries007.tfc.util.climate.KoppenClimateClassification;
import net.dries007.tfc.world.BiomeNoiseSampler;
import net.dries007.tfc.world.ChunkBiomeSampler;
import net.dries007.tfc.world.ChunkHeightFiller;
import net.dries007.tfc.world.Seed;
import net.dries007.tfc.world.TFCChunkGenerator;
import net.dries007.tfc.world.biome.BiomeBlendType;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.biome.BiomeNoise;
import net.dries007.tfc.world.biome.BiomeSourceExtension;
import net.dries007.tfc.world.biome.TFCBiomes;
import net.dries007.tfc.world.chunkdata.ChunkData;
import net.dries007.tfc.world.chunkdata.ChunkDataGenerator;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.layer.framework.Area;
import net.dries007.tfc.world.layer.framework.AreaFactory;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.RegionPartition;
import net.dries007.tfc.world.region.RiverEdge;
import net.dries007.tfc.world.region.Units;
import net.dries007.tfc.world.river.RiverBlendType;
import net.dries007.tfc.world.river.RiverNoiseSampler;
import net.dries007.tfc.world.shore.ShoreBlendType;
import net.dries007.tfc.world.shore.ShoreNoiseSampler;
import net.dries007.tfc.world.volcano.CenteredFeatureBlendType;
import net.dries007.tfc.world.volcano.CenteredFeatureNoiseSampler;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.test.TestSetup;
import net.yazloysasha.tfcrealworld.util.profile.MapProfile;
import net.yazloysasha.tfcrealworld.world.MapLakeWater;
import net.yazloysasha.tfcrealworld.world.noise.png.BasePNGNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGRainVarianceNoise;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Draws a whole profile a chunk to a pixel, as the game generates it:
 * altitude, biomes, climate zones and a view as from a satellite. Run with
 * {@code ./gradlew test -PworldMaps=<seed> --tests "*WorldMapsTest"}; the
 * images are written to the artist directory.
 */
public class WorldMapsTest implements TestSetup {

  /** The profiles that are drawn: the view as from a satellite. */
  private static final String[] PROFILES = {
    "DEFAULT:FULL_EQUAL_EARTH",
    "DEFAULT:OLD_WORLD_EQUAL_EARTH",
    "DEFAULT:NEW_WORLD_EQUAL_EARTH",
  };

  /** The profile drawn in altitude, biomes and climate zones as well. */
  private static final String IN_FULL = "DEFAULT:OLD_WORLD_EQUAL_EARTH";

  private static final int SEA_LEVEL = TFCChunkGenerator.SEA_LEVEL_Y;

  /**
   * Altitude: the land by its height in the tones of the biome map, relief
   * shaded from the north-west. Most of the scale lies low, where most of
   * the land does: vanilla's old mountains top out some 60 blocks over the
   * sea and its mountains at 90, and only collisional ranges go far higher,
   * so the browns start early and grey and white are left to those. Block
   * height, then red, green, blue.
   */
  private static final int[][] LAND_TINTS = {
    { SEA_LEVEL, 92, 150, 84 },
    { SEA_LEVEL + 6, 116, 168, 88 },
    { SEA_LEVEL + 14, 158, 186, 100 },
    { SEA_LEVEL + 24, 200, 192, 112 },
    { SEA_LEVEL + 36, 204, 168, 100 },
    { SEA_LEVEL + 50, 184, 136, 92 },
    { SEA_LEVEL + 70, 156, 112, 88 },
    { SEA_LEVEL + 100, 130, 100, 92 },
    { SEA_LEVEL + 145, 172, 164, 160 },
    { SEA_LEVEL + 200, 250, 250, 250 },
  };
  private static final int[][] ICE_TINTS = {
    { SEA_LEVEL, 214, 228, 240 },
    { SEA_LEVEL + 120, 250, 252, 255 },
  };

  /**
   * Chunks to each side the height a tint is taken from is gathered over. A
   * chunk samples one column, a peak or the valley beside it: a range tinted
   * by single columns falls apart into spots. The tint takes the mean of the
   * surroundings and their highest point in equal parts, so a range is one
   * body; the shading still shows every slope.
   */
  private static final int TINT_REACH = 3;

  /** The artist's seas, by vanilla's ocean depth 1 (reef) to 5 (trench). */
  private static final Color[] SEA_BY_DEPTH = {
    new Color(150, 160, 255),
    new Color(120, 120, 240),
    new Color(105, 105, 210),
    new Color(90, 90, 180),
    new Color(60, 60, 120),
  };

  /** Blocks between the samples the relief is shaded from: one chunk. */
  private static final double SHADE_STEP = 16;

  /** How strongly a slope darkens or lightens the tint. */
  private static final double SHADE_STRENGTH = 1.6;

  /**
   * Slopes (blocks of height to a block) below the first are not shaded, so
   * rolling plains stay flat; from the second on the shade is full.
   */
  private static final double SHADE_FROM_SLOPE = 0.08;
  private static final double SHADE_FULL_SLOPE = 0.35;

  /** The seafloor is shaded this much of the land. */
  private static final double SEA_SHADE = 0.5;

  /** Annual mean temperatures of the rows of {@link #GROUND}, warm to cold. */
  private static final double[] GROUND_TEMPERATURES = { 26, 12, 1, -9 };

  /** Annual rainfall of the stops of {@link #GROUND}, dry to wet. */
  private static final double[] GROUND_RAINFALLS = {
    0,
    70,
    150,
    250,
    350,
    500,
  };

  /**
   * The ground from above by climate: desert sand, dry steppe, grassland,
   * woodland, forest, rainforest across a row; tropics, temperate lands,
   * boreal lands, tundra down the rows.
   */
  private static final double[][][] GROUND = {
    {
      { 224, 198, 150 },
      { 204, 174, 120 },
      { 160, 152, 90 },
      { 98, 126, 62 },
      { 56, 98, 46 },
      { 36, 80, 42 },
    },
    {
      { 208, 188, 148 },
      { 184, 170, 122 },
      { 142, 150, 92 },
      { 92, 124, 66 },
      { 60, 100, 54 },
      { 46, 88, 52 },
    },
    {
      { 178, 166, 138 },
      { 158, 154, 122 },
      { 126, 138, 96 },
      { 84, 112, 70 },
      { 58, 92, 60 },
      { 48, 82, 58 },
    },
    {
      { 152, 146, 130 },
      { 142, 138, 120 },
      { 126, 128, 106 },
      { 110, 116, 98 },
      { 98, 108, 94 },
      { 92, 102, 92 },
    },
  };

  /** The sea from above by the height of its floor. */
  private static final double[][] SATELLITE_SEA = {
    { SEA_LEVEL - 62, 8, 22, 56 },
    { SEA_LEVEL - 40, 12, 34, 76 },
    { SEA_LEVEL - 20, 18, 54, 100 },
    { SEA_LEVEL - 8, 28, 82, 118 },
    { SEA_LEVEL - 1, 52, 116, 132 },
  };
  private static final double[] SATELLITE_FRESH = { 34, 66, 98 };
  private static final double[] SATELLITE_SNOW = { 244, 247, 250 };
  private static final double[] SATELLITE_ROCK = { 122, 110, 100 };

  /**
   * Degrees colder for every block of height. The climate map already holds
   * the real temperature of the ground at its real height, so only half of
   * TFC's own rate (0.16) is taken: enough to cap the game's peaks with
   * snow, not to bury every plateau.
   */
  private static final double LAPSE_RATE = 0.08;

  /** Annual mean temperature at the ground from which snow lies, and all of it. */
  private static final double SNOW_FROM = -9;
  private static final double SNOW_FULL = -18;

  /** Annual mean temperature from which the sea carries ice, and is covered. */
  private static final double SEA_ICE_FROM = -13;
  private static final double SEA_ICE_FULL = -20;

  /** Slopes from which high ground shows bare rock, and is all rock. */
  private static final double ROCK_FROM_SLOPE = 0.5;
  private static final double ROCK_FULL_SLOPE = 1.6;

  /** How strongly a slope darkens or lightens the satellite view. */
  private static final double SATELLITE_SHADE = 2.0;

  private static final byte LAND = 0;
  private static final byte SEA = 1;
  private static final byte FRESH = 2;
  private static final byte ICE = 3;

  @Test
  @EnabledIfSystemProperty(named = "worldMaps", matches = "-?\\d+")
  @Timeout(value = 12, unit = TimeUnit.HOURS)
  public void drawWorldMaps() throws IOException {
    final long seed = Long.parseLong(System.getProperty("worldMaps"));
    final String oldProfile = TFCRealWorldConfig.MAP_PROFILE.get();
    final int oldHorizontal = TFCRealWorldConfig.HORIZONTAL_SCALE.get();
    final int oldVertical = TFCRealWorldConfig.VERTICAL_SCALE.get();
    try {
      for (final String profile : PROFILES) {
        final MapProfile settings = MapProfile.loadFromResources(profile);
        TFCRealWorldConfig.MAP_PROFILE.setServerValue(profile);
        TFCRealWorldConfig.HORIZONTAL_SCALE.setServerValue(
          settings.horizontalScale()
        );
        TFCRealWorldConfig.VERTICAL_SCALE.setServerValue(
          settings.verticalScale()
        );
        BasePNGNoise.clearImageCache();
        draw(
          profile.substring(profile.indexOf(':') + 1).toLowerCase(),
          settings.horizontalScale(),
          settings.verticalScale(),
          Seed.of(seed),
          profile.equals(IN_FULL)
        );
      }
    } finally {
      TFCRealWorldConfig.MAP_PROFILE.setServerValue(oldProfile);
      TFCRealWorldConfig.HORIZONTAL_SCALE.setServerValue(oldHorizontal);
      TFCRealWorldConfig.VERTICAL_SCALE.setServerValue(oldVertical);
      BasePNGNoise.clearImageCache();
    }
  }

  private static void draw(
    String name,
    int halfX,
    int halfZ,
    Seed seed,
    boolean inFull
  ) throws IOException {
    final RegionGenerator generator = new RegionGenerator(
      BuiltinWorldPreset.defaultSettings(),
      seed
    );
    final AreaFactory layers = TFCLayers.createRegionBiomeLayer(
      generator,
      seed
    );
    final ChunkDataGenerator chunks = generator.chunkDataGenerator();
    final ThreadLocal<Painter> painters = ThreadLocal.withInitial(() ->
      new Painter(generator, layers.get(), seed)
    );

    final int width = (2 * halfX) >> 4;
    final int height = (2 * halfZ) >> 4;
    final int[] altitude = new int[width * height];
    final float[] heights = new float[width * height];
    final byte[] surface = new byte[width * height];
    final float[] temperature = new float[width * height];
    final float[] rainfall = new float[width * height];
    final int[] biomes = new int[width * height];
    final int[] climate = new int[width * height];
    final Map<BiomeExtension, Integer> biomeColors = biomeColors();
    final AtomicInteger rows = new AtomicInteger();
    final long start = System.currentTimeMillis();

    IntStream.range(0, height)
      .parallel()
      .forEach(row -> {
        final Painter painter = painters.get();
        for (int column = 0; column < width; column++) {
          final ChunkPos pos = new ChunkPos(
            (-halfX >> 4) + column,
            (-halfZ >> 4) + row
          );
          final int x = pos.getMiddleBlockX();
          final int z = pos.getMiddleBlockZ();
          final BiomeExtension land = painter.biome(x, z);
          final boolean river = land.hasRivers() && painter.isRiver(x, z);
          final double ground = painter.height(pos, x, z);
          final boolean sea = land.biomeBlendType() == BiomeBlendType.OCEAN;
          final boolean lake =
            MapLakeWater.isLakeBiome(land) && ground < SEA_LEVEL;
          // Dry land below sea level (a salt flat, a desert basin) is land.
          final boolean underSea =
            ground < SEA_LEVEL && (sea || land.isShore());
          final int pixel = row * width + column;

          heights[pixel] = (float) ground;
          // The sea is the same on all three maps: the artist's depths.
          final int seaColor = SEA_BY_DEPTH[seaDepth(land) - 1].getRGB();
          biomes[pixel] = underSea
            ? seaColor
            : biomeColors.get(river ? TFCBiomes.RIVER : land);
          if (river || lake) {
            altitude[pixel] = biomes[pixel];
            surface[pixel] = FRESH;
          } else if (underSea) {
            altitude[pixel] = seaColor;
            surface[pixel] = SEA;
          } else if (isIce(land)) {
            surface[pixel] = ICE;
          }
          final ChunkData data = chunks.generate(new ChunkData(chunks, pos));
          temperature[pixel] = data.getAverageSeaLevelTemp(x, z);
          rainfall[pixel] = data.getAverageRainfall(x, z);
          // Rivers and lakes are not drawn: the climate of their place.
          climate[pixel] = underSea
            ? seaColor
            : RegionGeneratorTests.koppenClimateColor(
                KoppenClimateClassification.classify(
                  temperature[pixel],
                  rainfall[pixel],
                  data.getRainVariance(x, z),
                  PNGRainVarianceNoise.isNorth(Units.blockToGrid(z))
                )
              ).getRGB();
        }
        final int done = rows.incrementAndGet();
        if (done % 100 == 0) {
          System.out.printf(
            "World maps %s: %d of %d rows, %d s%n",
            name,
            done,
            height,
            (System.currentTimeMillis() - start) / 1000
          );
        }
      });

    if (inFull) {
      tintLand(altitude, heights, surface, width, height);
      shadeRelief(altitude, heights, surface, width, height);
      write(name + "_altitude", altitude, width, height);
      write(name + "_biomes", biomes, width, height);
      write(name + "_climate", climate, width, height);
    }
    write(
      name + "_satellite",
      satellite(heights, surface, temperature, rainfall, width, height),
      width,
      height
    );
  }

  /** The biome palette of World Preview TFC (its biome_colors.json). */
  private static Map<BiomeExtension, Integer> biomeColors() throws IOException {
    final JsonObject palette;
    try (
      InputStream stream = WorldMapsTest.class.getResourceAsStream(
        "/world_maps/biome_colors.json"
      )
    ) {
      palette = JsonParser.parseReader(
        new InputStreamReader(stream, StandardCharsets.UTF_8)
      ).getAsJsonObject();
    }
    final Map<BiomeExtension, Integer> colors = new IdentityHashMap<>();
    for (final BiomeExtension biome : TFCBiomes.REGISTRY) {
      final JsonObject color = palette.getAsJsonObject(
        biome.key().location().toString()
      );
      colors.put(
        biome,
        new Color(
          color.get("r").getAsInt(),
          color.get("g").getAsInt(),
          color.get("b").getAsInt()
        ).getRGB()
      );
    }
    return colors;
  }

  private static boolean isIce(BiomeExtension biome) {
    final String name = biome.key().location().getPath();
    return name.contains("ice_sheet") || name.contains("glaciated");
  }

  /**
   * The world as from a satellite: the ground by its climate (sand where it
   * is dry, grass and forest where it rains, tundra where it is cold), snow
   * where the cold of the place and of its height keeps it, bare rock on
   * steep high ground, the sea by its depth with ice on the polar seas, and
   * the relief shaded.
   */
  private static int[] satellite(
    float[] heights,
    byte[] surface,
    float[] temperature,
    float[] rainfall,
    int width,
    int height
  ) {
    final int[] image = new int[width * height];
    for (int z = 0; z < height; z++) {
      for (int x = 0; x < width; x++) {
        final int pixel = z * width + x;
        final float ground = heights[pixel];
        final float west = heights[z * width + Math.max(x - 1, 0)];
        final float east = heights[z * width + Math.min(x + 1, width - 1)];
        final float north = heights[Math.max(z - 1, 0) * width + x];
        final float south = heights[Math.min(z + 1, height - 1) * width + x];
        final double slope =
          ((east - west) + (south - north)) / (4 * SHADE_STEP);
        final double steepness =
          Math.hypot(east - west, south - north) / (2 * SHADE_STEP);
        double[] color;
        double shadeShare = 1;
        if (surface[pixel] == FRESH) {
          color = SATELLITE_FRESH;
          shadeShare = 0;
        } else if (surface[pixel] == SEA) {
          color = gradient(SATELLITE_SEA, ground);
          // Pack ice on the seas that stay frozen.
          color = mix(
            color,
            SATELLITE_SNOW,
            0.85 * share(temperature[pixel], SEA_ICE_FROM, SEA_ICE_FULL)
          );
          shadeShare = 0.35;
        } else {
          final double cold =
            temperature[pixel] - LAPSE_RATE * Math.max(ground - SEA_LEVEL, 0);
          color = groundColor(temperature[pixel], rainfall[pixel]);
          color = mix(
            color,
            SATELLITE_ROCK,
            0.75 *
              share(steepness, ROCK_FROM_SLOPE, ROCK_FULL_SLOPE) *
              share(ground, SEA_LEVEL + 40, SEA_LEVEL + 110)
          );
          color = mix(
            color,
            SATELLITE_SNOW,
            surface[pixel] == ICE ? 1 : share(cold, SNOW_FROM, SNOW_FULL)
          );
        }
        final double shade = Math.clamp(
          1 +
            shadeShare *
            SATELLITE_SHADE *
            share(Math.abs(slope), 0.04, 0.3) *
            slope,
          0.5,
          1.25
        );
        image[pixel] = new Color(
          (int) Math.clamp(color[0] * shade, 0, 255),
          (int) Math.clamp(color[1] * shade, 0, 255),
          (int) Math.clamp(color[2] * shade, 0, 255)
        ).getRGB();
      }
    }
    return image;
  }

  /** The ground of a climate, between the rows and stops of the palette. */
  private static double[] groundColor(double temperature, double rainfall) {
    int row = 0;
    while (
      row < GROUND_TEMPERATURES.length - 2 &&
      temperature < GROUND_TEMPERATURES[row + 1]
    ) {
      row++;
    }
    int stop = 0;
    while (
      stop < GROUND_RAINFALLS.length - 2 &&
      rainfall > GROUND_RAINFALLS[stop + 1]
    ) {
      stop++;
    }
    final double warm = share(
      temperature,
      GROUND_TEMPERATURES[row + 1],
      GROUND_TEMPERATURES[row]
    );
    final double wet = share(
      rainfall,
      GROUND_RAINFALLS[stop],
      GROUND_RAINFALLS[stop + 1]
    );
    return mix(
      mix(GROUND[row + 1][stop], GROUND[row + 1][stop + 1], wet),
      mix(GROUND[row][stop], GROUND[row][stop + 1], wet),
      warm
    );
  }

  private static double[] gradient(double[][] stops, double value) {
    for (int i = 1; i < stops.length; i++) {
      if (value <= stops[i][0]) {
        final double t = share(value, stops[i - 1][0], stops[i][0]);
        return mix(tail(stops[i - 1]), tail(stops[i]), t);
      }
    }
    return tail(stops[stops.length - 1]);
  }

  private static double[] tail(double[] stop) {
    return new double[] { stop[1], stop[2], stop[3] };
  }

  private static double[] mix(double[] from, double[] to, double share) {
    return new double[] {
      from[0] + (to[0] - from[0]) * share,
      from[1] + (to[1] - from[1]) * share,
      from[2] + (to[2] - from[2]) * share,
    };
  }

  /** Where the value lies from one bound (0) to the other (1), clamped. */
  private static double share(double value, double from, double to) {
    return Math.clamp((value - from) / (to - from), 0, 1);
  }

  /** Vanilla's ocean depth a sea biome stands for, 1 (reef) to 5 (trench). */
  private static int seaDepth(BiomeExtension biome) {
    if (biome == TFCBiomes.DEEP_OCEAN_TRENCH) {
      return 5;
    }
    if (biome == TFCBiomes.DEEP_OCEAN || biome == TFCBiomes.DEEP_OCEAN_ATOLLS) {
      return 4;
    }
    if (biome == TFCBiomes.OCEAN_RIDGE) {
      return 3;
    }
    if (biome == TFCBiomes.OCEAN || biome == TFCBiomes.OCEAN_ATOLLS) {
      return 2;
    }
    return 1;
  }

  /** Tints the land by the height of its surroundings ({@link #TINT_REACH}). */
  private static void tintLand(
    int[] image,
    float[] heights,
    byte[] surface,
    int width,
    int height
  ) {
    for (int z = 0; z < height; z++) {
      for (int x = 0; x < width; x++) {
        final int pixel = z * width + x;
        final byte kind = surface[pixel];
        if (kind == SEA || kind == FRESH) {
          continue;
        }
        double sum = 0;
        double highest = heights[pixel];
        int count = 0;
        for (int dz = -TINT_REACH; dz <= TINT_REACH; dz++) {
          for (int dx = -TINT_REACH; dx <= TINT_REACH; dx++) {
            final int nx = x + dx;
            final int nz = z + dz;
            if (nx < 0 || nz < 0 || nx >= width || nz >= height) {
              continue;
            }
            final byte near = surface[nz * width + nx];
            if (near == SEA || near == FRESH) {
              continue;
            }
            final float ground = heights[nz * width + nx];
            sum += ground;
            highest = Math.max(highest, ground);
            count++;
          }
        }
        image[pixel] = tint(
          kind == ICE ? ICE_TINTS : LAND_TINTS,
          0.5 * (sum / count) + 0.5 * highest
        );
      }
    }
  }

  /**
   * Hill shading: ground facing the light in the north-west is lightened,
   * ground facing away darkened, by its slope between the neighbouring
   * chunks. Gentle slopes are left alone, and rivers and lakes are flat.
   */
  private static void shadeRelief(
    int[] image,
    float[] heights,
    byte[] surface,
    int width,
    int height
  ) {
    for (int z = 0; z < height; z++) {
      for (int x = 0; x < width; x++) {
        final int pixel = z * width + x;
        if (surface[pixel] == FRESH) {
          continue;
        }
        final float west = heights[z * width + Math.max(x - 1, 0)];
        final float east = heights[z * width + Math.min(x + 1, width - 1)];
        final float north = heights[Math.max(z - 1, 0) * width + x];
        final float south = heights[Math.min(z + 1, height - 1) * width + x];
        // Positive where the ground rises towards the south-east.
        final double slope =
          ((east - west) + (south - north)) / (4 * SHADE_STEP);
        final double steepness = Mth.clamp(
          Mth.inverseLerp(Math.abs(slope), SHADE_FROM_SLOPE, SHADE_FULL_SLOPE),
          0,
          1
        );
        final double strength =
          SHADE_STRENGTH *
          steepness *
          steepness *
          (surface[pixel] == SEA ? SEA_SHADE : 1);
        final double shade = Math.clamp(1 + strength * slope, 0.55, 1.2);
        final Color color = new Color(image[pixel]);
        image[pixel] = new Color(
          (int) Math.min(255, color.getRed() * shade),
          (int) Math.min(255, color.getGreen() * shade),
          (int) Math.min(255, color.getBlue() * shade)
        ).getRGB();
      }
    }
  }

  private static int tint(int[][] tints, double height) {
    if (height <= tints[0][0]) {
      return new Color(tints[0][1], tints[0][2], tints[0][3]).getRGB();
    }
    for (int i = 1; i < tints.length; i++) {
      if (height <= tints[i][0]) {
        final float t = (float) Mth.inverseLerp(
          height,
          tints[i - 1][0],
          tints[i][0]
        );
        return new Color(
          Mth.lerpInt(t, tints[i - 1][1], tints[i][1]),
          Mth.lerpInt(t, tints[i - 1][2], tints[i][2]),
          Mth.lerpInt(t, tints[i - 1][3], tints[i][3])
        ).getRGB();
      }
    }
    final int[] top = tints[tints.length - 1];
    return new Color(top[1], top[2], top[3]).getRGB();
  }

  private static void write(String name, int[] pixels, int width, int height)
    throws IOException {
    final BufferedImage image = new BufferedImage(
      width,
      height,
      BufferedImage.TYPE_INT_RGB
    );
    image.setRGB(0, 0, width, height, pixels, 0, width);
    final File directory = new File(Artist.ARTIST_DIRECTORY);
    directory.mkdirs();
    final File file = new File(directory, name + ".png");
    try {
      ImageIO.write(image, "PNG", file);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    System.out.println("World map written: " + file.getCanonicalPath());
  }

  /** One thread's view of the world: its biome layer and noise samplers. */
  private static final class Painter implements BiomeSourceExtension {

    /** Half a chunk across: a river that crosses the chunk is drawn. */
    private static final double CHUNK_REACH = 8.0 / Units.GRID_WIDTH_IN_BLOCK;

    private final RegionGenerator generator;
    private final Area layer;
    private final Map<BiomeExtension, BiomeNoiseSampler> biomeSamplers;
    private final Map<RiverBlendType, RiverNoiseSampler> rivers = new EnumMap<>(
      RiverBlendType.class
    );
    private final Map<ShoreBlendType, ShoreNoiseSampler> shores = new EnumMap<>(
      ShoreBlendType.class
    );
    private final Map<
      CenteredFeatureBlendType,
      CenteredFeatureNoiseSampler
    > centred = new EnumMap<>(CenteredFeatureBlendType.class);
    private final net.dries007.tfc.world.noise.Noise2D tide;

    Painter(RegionGenerator generator, Area layer, Seed seed) {
      this.generator = generator;
      this.layer = layer;
      final ImmutableMap.Builder<BiomeExtension, BiomeNoiseSampler> samplers =
        ImmutableMap.builder();
      for (final BiomeExtension biome : TFCBiomes.REGISTRY) {
        final BiomeNoiseSampler sampler = biome.createNoiseSampler(
          seed.forkStable()
        );
        if (sampler != null) {
          samplers.put(biome, sampler);
        }
      }
      this.biomeSamplers = samplers.build();
      for (final RiverBlendType type : RiverBlendType.ALL) {
        rivers.put(type, type.createNoiseSampler(seed.forkStable()));
      }
      for (final ShoreBlendType type : ShoreBlendType.ALL) {
        shores.put(type, type.createNoiseSampler(seed.forkStable()));
      }
      for (final CenteredFeatureBlendType type : CenteredFeatureBlendType.ALL) {
        centred.put(type, type.createNoiseSampler(seed.forkStable()));
      }
      this.tide = BiomeNoise.shoreTideLevelNoise(seed);
    }

    BiomeExtension biome(int blockX, int blockZ) {
      return getBiomeExtensionNoRiver(blockX >> 2, blockZ >> 2);
    }

    boolean isRiver(int blockX, int blockZ) {
      final double gridX = Units.blockToGridExact(blockX);
      final double gridZ = Units.blockToGridExact(blockZ);
      for (final RiverEdge edge : getPartition(blockX, blockZ).rivers()) {
        final double reach =
          CHUNK_REACH +
          Math.sqrt(edge.widthSq(gridX, gridZ)) / Units.GRID_WIDTH_IN_BLOCK;
        if (edge.fractal().intersect(gridX, gridZ, reach)) {
          return true;
        }
      }
      return false;
    }

    double height(ChunkPos pos, int blockX, int blockZ) {
      final Object2DoubleMap<BiomeExtension>[] weights =
        ChunkBiomeSampler.sampleBiomes(
          pos,
          (x, z) -> getBiomeExtensionNoRiver(x >> 2, z >> 2),
          BiomeExtension::biomeBlendType
        );
      return new ChunkHeightFiller(
        weights,
        this,
        biomeSamplers,
        rivers,
        shores,
        centred,
        SEA_LEVEL,
        tide
      ).sampleHeight(blockX, blockZ);
    }

    @Override
    public BiomeExtension getBiomeExtensionNoRiver(int quartX, int quartZ) {
      return TFCLayers.getFromLayerId(layer.get(quartX, quartZ));
    }

    @Override
    public Holder<Biome> getBiomeFromExtension(BiomeExtension extension) {
      throw new UnsupportedOperationException();
    }

    @Override
    public RegionPartition.Point getPartition(int blockX, int blockZ) {
      return generator.getOrCreatePartitionPoint(
        Units.blockToGrid(blockX),
        Units.blockToGrid(blockZ)
      );
    }
  }
}
