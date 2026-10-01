package net.yazloysasha.tfcrealworld.world.tectonics;

import java.util.BitSet;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise.ContinentBand;

/**
 * Map lakes that lie in a rift: every connected {@code continent.png} lake
 * that touches a divergent boundary zone of {@code tectonics.png}.
 * <p>
 * Vanilla makes a rift lake only where its random pick for a rift cell says
 * so, and gives every other lake the lake form of the land under it (plain
 * lake for a rift valley, mountain lake for the rift shoulders). A real lake
 * fills its rift as a whole, so the whole lake is a rift lake.
 */
public final class MapRiftLakes {

  private static final int[][] NEIGHBOURS = {
    { 0, -1 },
    { 1, 0 },
    { 0, 1 },
    { -1, 0 },
  };

  private final PNGContinentNoise continent;
  private final BitSet riftLakePixels;

  private MapRiftLakes(PNGContinentNoise continent, BitSet riftLakePixels) {
    this.continent = continent;
    this.riftLakePixels = riftLakePixels;
  }

  public static MapRiftLakes create(
    PNGContinentNoise continent,
    TectonicsMap tectonics
  ) {
    final int width = continent.getWidth();
    final int height = continent.getHeight();
    final BitSet visited = new BitSet(width * height);
    final BitSet riftLakePixels = new BitSet(width * height);
    final int[] component = new int[width * height];
    for (int start = 0; start < width * height; start++) {
      if (visited.get(start) || !isLake(continent, start, width)) {
        continue;
      }
      visited.set(start);
      component[0] = start;
      int size = 1;
      boolean rift = false;
      for (int i = 0; i < size; i++) {
        final int index = component[i];
        final int x = index % width;
        final int z = index / width;
        rift = rift || isDivergent(continent, tectonics, x, z);
        for (final int[] offset : NEIGHBOURS) {
          final int nx = x + offset[0];
          final int nz = z + offset[1];
          if (nx < 0 || nz < 0 || nx >= width || nz >= height) {
            continue;
          }
          final int next = nz * width + nx;
          if (!visited.get(next) && isLake(continent, next, width)) {
            visited.set(next);
            component[size++] = next;
          }
        }
      }
      if (rift) {
        for (int i = 0; i < size; i++) {
          riftLakePixels.set(component[i]);
        }
      }
    }
    return new MapRiftLakes(continent, riftLakePixels);
  }

  public boolean isRiftLakeAtGrid(double gridX, double gridZ) {
    return riftLakePixels.get(continent.pixelIndexAtWorldRounded(gridX, gridZ));
  }

  private static boolean isLake(
    PNGContinentNoise continent,
    int index,
    int width
  ) {
    final ContinentBand band = continent.bandAtPixel(
      index % width,
      index / width
    );
    return band == ContinentBand.LAKE || band == ContinentBand.SALT_LAKE;
  }

  /**
   * Tectonic class under a continent pixel. A region point covers
   * {@code [x, x + 1)} and the tectonics map samples its centre, so the pixel
   * centre is shifted back by half a cell.
   */
  private static boolean isDivergent(
    PNGContinentNoise continent,
    TectonicsMap tectonics,
    int x,
    int z
  ) {
    final double gridX = (x - continent.getCenterX()) / continent.getScaleX();
    final double gridZ = (z - continent.getCenterZ()) / continent.getScaleZ();
    return (
      tectonics.classAtGrid(gridX - 0.5, gridZ - 0.5).boundary() ==
      TectonicClass.Boundary.DIVERGENT
    );
  }
}
