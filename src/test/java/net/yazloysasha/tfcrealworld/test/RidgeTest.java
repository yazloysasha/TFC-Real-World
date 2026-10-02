package net.yazloysasha.tfcrealworld.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import net.dries007.tfc.world.Seed;
import net.dries007.tfc.world.TFCChunkGenerator;
import net.dries007.tfc.world.biome.BiomeNoise;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.Units;
import net.yazloysasha.tfcrealworld.test.drawing.BuiltinWorldPreset;
import net.yazloysasha.tfcrealworld.util.geography.WaypointCoordinates;
import org.junit.jupiter.api.Test;

/** The crest of the mid-ocean ridges. */
public class RidgeTest implements TestSetup {

  private static final long SEED = 20261003L;

  /** Vanilla's crest stands 12 to 36 blocks under the sea. */
  private static final int CREST_HEIGHT = TFCChunkGenerator.SEA_LEVEL_Y - 24;

  /**
   * Down the North Atlantic the ridge biome of the map has the crest of the
   * Mid-Atlantic Ridge running through it: one line along the real axis,
   * not uplifts here and there where vanilla's plate noise has an edge
   * (which leaves a crest in one row in five). The rows without one cross
   * the Azores, where the axis runs under islands, and vanilla's gaps.
   */
  @Test
  public void crestFollowsTheRidgeOfTheMap() {
    final RegionGenerator generator = new RegionGenerator(
      BuiltinWorldPreset.defaultSettings(),
      Seed.of(SEED)
    );
    final Noise2D ridge = BiomeNoise.oceanRidge(SEED);
    final int[] west = WaypointCoordinates.toBlockXZ(50.0, -50.0);
    final int[] east = WaypointCoordinates.toBlockXZ(10.0, -20.0);
    int rows = 0;
    int rowsWithCrest = 0;
    for (int z = west[1]; z < east[1]; z += 64) {
      boolean ridgeInRow = false;
      boolean crestInRow = false;
      for (int x = west[0]; x < east[0]; x += 16) {
        if (
          generator.getOrCreateRegionPoint(
            Units.blockToGrid(x),
            Units.blockToGrid(z)
          ).biome ==
          TFCLayers.OCEAN_RIDGE
        ) {
          ridgeInRow = true;
          crestInRow |= ridge.noise(x, z) >= CREST_HEIGHT;
        }
      }
      if (ridgeInRow) {
        rows++;
        if (crestInRow) {
          rowsWithCrest++;
        }
      }
    }
    assertTrue(rows > 50, "rows across the ridge: " + rows);
    assertTrue(
      rowsWithCrest >= 0.7 * rows,
      "rows across the ridge with a crest: " + rowsWithCrest + " of " + rows
    );
  }
}
