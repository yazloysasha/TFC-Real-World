package net.yazloysasha.tfcrealworld.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.dries007.tfc.world.Seed;
import net.dries007.tfc.world.biome.TFCBiomes;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.layer.framework.Area;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.test.drawing.BuiltinWorldPreset;
import net.yazloysasha.tfcrealworld.util.geography.WaypointCoordinates;
import net.yazloysasha.tfcrealworld.world.surface.FrozenWaterSurfaceBuilder;
import org.junit.jupiter.api.Test;

/** Lakes of the map and the land between them. */
public class LakeTest implements TestSetup {

  private static final long SEED = 20261002L;

  /**
   * Finland is a land of small lakes: whole region cells are lakes, and the
   * land the map has inside them must be the land of the place, never a
   * biome picked from a list (a rift valley, once).
   */
  @Test
  public void landBetweenLakesIsTheLandOfThePlace() {
    final RegionGenerator generator = new RegionGenerator(
      BuiltinWorldPreset.defaultSettings(),
      Seed.of(SEED)
    );
    final Area layer = TFCLayers.createRegionBiomeLayer(
      generator,
      Seed.of(SEED)
    ).get();
    final int[] south = WaypointCoordinates.toBlockXZ(60.0, 21.0);
    final int[] north = WaypointCoordinates.toBlockXZ(69.0, 31.0);
    int rifts = 0;
    for (
      int x = Math.min(south[0], north[0]);
      x < Math.max(south[0], north[0]);
      x += 16
    ) {
      for (
        int z = Math.min(south[1], north[1]);
        z < Math.max(south[1], north[1]);
        z += 16
      ) {
        if (layer.get(x >> 2, z >> 2) == TFCLayers.RIFT_VALLEY) {
          rifts++;
        }
      }
    }
    assertEquals(0, rifts, "rift valley samples in Finland");
  }

  /**
   * At the ice sheet edge of Greenland a map lake lies half under the flat
   * ice sheet, half at its edge. It is one lake form as a whole, a meltwater
   * lake: a subglacial lake has the height of the ice, and beside it the
   * meltwater would be lifted out of the water.
   */
  @Test
  public void lakeAtTheIceEdgeIsOneForm() {
    final RegionGenerator generator = new RegionGenerator(
      BuiltinWorldPreset.defaultSettings(),
      Seed.of(SEED)
    );
    final Area layer = TFCLayers.createRegionBiomeLayer(
      generator,
      Seed.of(SEED)
    ).get();
    int meltwater = 0;
    int mixed = 0;
    for (int x = (-9431 - 320) >> 2; x < (-9431 + 320) >> 2; x++) {
      for (int z = (-17846 - 320) >> 2; z < (-17846 + 320) >> 2; z++) {
        if (layer.get(x, z) != TFCLayers.MELTWATER_LAKE) {
          continue;
        }
        meltwater++;
        if (
          layer.get(x + 1, z) == TFCLayers.SUBGLACIAL_LAKE ||
          layer.get(x - 1, z) == TFCLayers.SUBGLACIAL_LAKE ||
          layer.get(x, z + 1) == TFCLayers.SUBGLACIAL_LAKE ||
          layer.get(x, z - 1) == TFCLayers.SUBGLACIAL_LAKE
        ) {
          mixed++;
        }
      }
    }
    assertTrue(meltwater > 100, "meltwater lake samples: " + meltwater);
    assertEquals(0, mixed, "meltwater next to a subglacial lake");
  }

  @Test
  public void waterFreezesLikeTheSea() {
    final Seed seed = Seed.of(SEED);
    assertTrue(
      TFCBiomes.LAKE.createSurfaceBuilder(seed) instanceof
        FrozenWaterSurfaceBuilder
    );
    assertTrue(
      TFCBiomes.MOUNTAIN_LAKE.createSurfaceBuilder(seed) instanceof
        FrozenWaterSurfaceBuilder
    );
    assertFalse(
      TFCBiomes.MELTWATER_LAKE.createSurfaceBuilder(seed) instanceof
        FrozenWaterSurfaceBuilder
    );
    assertTrue(
      TFCBiomes.SALT_MARSH.createSurfaceBuilder(seed) instanceof
        FrozenWaterSurfaceBuilder
    );
    assertTrue(
      TFCBiomes.RIVER.createSurfaceBuilder(seed) instanceof
        FrozenWaterSurfaceBuilder
    );
  }
}
