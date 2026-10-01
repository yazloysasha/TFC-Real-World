package net.yazloysasha.tfcrealworld.world.tectonics;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.profile.ProfileManager;
import net.yazloysasha.tfcrealworld.world.noise.png.BasePNGNoise;
import org.jetbrains.annotations.Nullable;

/**
 * {@code tectonics.png} (palette index per pixel) + {@code tectonics.json}
 * legend. One class per pixel describes the place for both land and water;
 * {@code continent.png} alone decides which half applies.
 * <p>
 * Also precomputes the distance (in region grid units) from every pixel to the
 * nearest active plate-boundary pixel. That distance becomes vanilla
 * {@code distanceToEdge}, the same role Voronoi cell edges play in vanilla.
 */
public final class TectonicsMap extends BasePNGNoise {

  public static final String MAP_NAME = "tectonics";
  public static final String LEGEND_FILE = "tectonics.json";

  private static final int PROPAGATION_PASSES = 2;

  private final byte[] indices;
  private final TectonicClass[] classes;
  private final float[] boundaryDistance;

  private TectonicsMap(
    int horizontalScale,
    int verticalScale,
    BufferedImage image,
    TectonicLegend legend
  ) {
    super(
      horizontalScale,
      verticalScale,
      MAP_NAME,
      "Failed to load tectonics map."
    );
    this.indices = readIndices(image);
    this.classes = resolveClasses(legend, indices);
    this.boundaryDistance = computeBoundaryDistance();
  }

  @Nullable
  public static TectonicsMap tryCreate(int horizontalScale, int verticalScale) {
    final BufferedImage image = loadImage(MAP_NAME);
    if (image == null) {
      return null;
    }
    final String profileId = TFCRealWorldConfig.MAP_PROFILE.get();
    final TectonicLegend legend;
    try (
      InputStream stream = ProfileManager.getProfileFileStream(
        profileId,
        LEGEND_FILE
      )
    ) {
      if (stream == null) {
        throw new IllegalStateException(
          "Profile " + profileId + " has tectonics.png but no " + LEGEND_FILE
        );
      }
      legend = TectonicLegend.read(stream);
    } catch (IOException e) {
      throw new IllegalStateException(
        "Failed to read " + LEGEND_FILE + " for profile " + profileId,
        e
      );
    }
    return new TectonicsMap(horizontalScale, verticalScale, image, legend);
  }

  public TectonicClass classAtGrid(double gridX, double gridZ) {
    return classes[pixelIndexAtGrid(gridX, gridZ)];
  }

  /**
   * Distance from the grid point to the nearest boundary-zone pixel, in grid
   * units, rounded up so only points inside a zone report {@code 0}.
   */
  public byte distanceToBoundaryAtGrid(double gridX, double gridZ) {
    final float distance = boundaryDistance[pixelIndexAtGrid(gridX, gridZ)];
    if (distance <= 0f) {
      return 0;
    }
    return (byte) Math.clamp((long) Math.ceil(distance), 1, Byte.MAX_VALUE);
  }

  public byte hotspotAgeAtPixel(int x, int z) {
    if (x < 0 || z < 0 || x >= width || z >= height) {
      return 0;
    }
    return classes[z * width + x].hotspot();
  }

  @Override
  protected double transformBrightness(double brightness) {
    return 0;
  }

  /**
   * A region point at grid {@code (x, z)} covers {@code [x, x + 1)}; sample its
   * centre, like vanilla {@code AddHotspots.shift}.
   */
  private int pixelIndexAtGrid(double gridX, double gridZ) {
    final double[] image = tileImageScratch();
    fillTileImageCoords(gridX + 0.5, gridZ + 0.5, image);
    final int x = Math.clamp(Math.round(image[0]), 0, width - 1);
    final int z = Math.clamp(Math.round(image[1]), 0, height - 1);
    return z * width + x;
  }

  private static byte[] readIndices(BufferedImage image) {
    final int w = image.getWidth();
    final int h = image.getHeight();
    if (image.getRaster().getNumBands() != 1) {
      throw new IllegalStateException(
        "tectonics.png must be an indexed (palette) or 8-bit grayscale PNG"
      );
    }
    final int[] samples = image
      .getRaster()
      .getSamples(0, 0, w, h, 0, (int[]) null);
    final byte[] out = new byte[samples.length];
    for (int i = 0; i < samples.length; i++) {
      out[i] = (byte) samples[i];
    }
    return out;
  }

  private static TectonicClass[] resolveClasses(
    TectonicLegend legend,
    byte[] indices
  ) {
    final TectonicClass[] resolved = new TectonicClass[indices.length];
    for (int i = 0; i < indices.length; i++) {
      final int index = indices[i] & 0xFF;
      final TectonicClass tectonicClass = legend.get(index);
      if (tectonicClass == null) {
        throw new IllegalStateException(
          "tectonics.png uses palette index " +
            index +
            " which is missing from " +
            LEGEND_FILE
        );
      }
      resolved[i] = tectonicClass;
    }
    return resolved;
  }

  /**
   * Nearest-seed propagation (dead reckoning) over 8-neighbourhoods; exact
   * enough at map resolution and linear in pixel count.
   */
  private float[] computeBoundaryDistance() {
    final int size = width * height;
    final int[] seed = new int[size];
    final float[] distance = new float[size];
    Arrays.fill(seed, -1);
    Arrays.fill(distance, Float.POSITIVE_INFINITY);
    for (int i = 0; i < size; i++) {
      if (classes[i].isBoundary()) {
        seed[i] = i;
        distance[i] = 0f;
      }
    }

    final double gridPerPixelX = 1.0 / scaleX;
    final double gridPerPixelZ = 1.0 / scaleZ;
    for (int pass = 0; pass < PROPAGATION_PASSES; pass++) {
      for (int z = 0; z < height; z++) {
        for (int x = 0; x < width; x++) {
          relax(seed, distance, x, z, x - 1, z, gridPerPixelX, gridPerPixelZ);
          relax(
            seed,
            distance,
            x,
            z,
            x - 1,
            z - 1,
            gridPerPixelX,
            gridPerPixelZ
          );
          relax(seed, distance, x, z, x, z - 1, gridPerPixelX, gridPerPixelZ);
          relax(
            seed,
            distance,
            x,
            z,
            x + 1,
            z - 1,
            gridPerPixelX,
            gridPerPixelZ
          );
        }
      }
      for (int z = height - 1; z >= 0; z--) {
        for (int x = width - 1; x >= 0; x--) {
          relax(seed, distance, x, z, x + 1, z, gridPerPixelX, gridPerPixelZ);
          relax(
            seed,
            distance,
            x,
            z,
            x + 1,
            z + 1,
            gridPerPixelX,
            gridPerPixelZ
          );
          relax(seed, distance, x, z, x, z + 1, gridPerPixelX, gridPerPixelZ);
          relax(
            seed,
            distance,
            x,
            z,
            x - 1,
            z + 1,
            gridPerPixelX,
            gridPerPixelZ
          );
        }
      }
    }
    return distance;
  }

  private void relax(
    int[] seed,
    float[] distance,
    int x,
    int z,
    int nx,
    int nz,
    double gridPerPixelX,
    double gridPerPixelZ
  ) {
    if (nx < 0 || nz < 0 || nx >= width || nz >= height) {
      return;
    }
    final int neighborSeed = seed[nz * width + nx];
    if (neighborSeed < 0) {
      return;
    }
    final int index = z * width + x;
    final double dx = (x - (neighborSeed % width)) * gridPerPixelX;
    final double dz = (z - neighborSeed / width) * gridPerPixelZ;
    final float candidate = (float) Math.sqrt(dx * dx + dz * dz);
    if (candidate < distance[index]) {
      distance[index] = candidate;
      seed[index] = neighborSeed;
    }
  }
}
