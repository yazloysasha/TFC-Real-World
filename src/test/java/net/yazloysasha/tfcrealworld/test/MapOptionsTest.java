package net.yazloysasha.tfcrealworld.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Predicate;
import net.dries007.tfc.world.Seed;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.RiverEdge;
import net.dries007.tfc.world.region.Units;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.test.drawing.BuiltinWorldPreset;
import net.yazloysasha.tfcrealworld.util.geography.WaypointCoordinates;
import net.yazloysasha.tfcrealworld.util.registry.ContinentNoiseRegistry;
import net.yazloysasha.tfcrealworld.util.registry.RiversRegistry;
import net.yazloysasha.tfcrealworld.util.registry.TectonicsRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Each "from map" option puts its part of the world where the map has it
 * while it is on, hands that part back to procedural generation while it is
 * off, and does nothing without the option it needs.
 */
public class MapOptionsTest implements TestSetup {

  private static final long SEED = 20261001L;

  /** Half the side of the square scanned for procedural features, in cells. */
  private static final int SCAN_RADIUS = 96;

  private static final double RIVER_TOLERANCE_CELLS = 1.5;
  private static final double DRY_DISTANCE_CELLS = 3.0;

  private record Place(String name, double latitude, double longitude) {}

  private static final Place[] LAND = {
    new Place("Paris", 48.86, 2.35),
    new Place("Novosibirsk", 55.0, 82.9),
    new Place("Kinshasa", -4.3, 15.3),
    new Place("Denver", 39.7, -105.0),
    new Place("Alice Springs", -23.7, 133.9),
  };

  private static final Place[] SEA = {
    new Place("North Atlantic", 35.0, -45.0),
    new Place("Central Pacific", 0.0, -140.0),
    new Place("Indian Ocean", -30.0, 80.0),
    new Place("Southern Ocean", -58.0, -120.0),
    new Place("Philippine Sea", 20.0, 135.0),
  };

  private static final Place[] LAKES = {
    new Place("Lake Victoria", -1.0, 33.0),
    new Place("Lake Baikal", 53.5, 108.0),
    new Place("Lake Superior", 47.7, -87.5),
  };

  private static final Place[] MOUNTAINS = {
    new Place("Everest", 27.99, 86.93),
    new Place("Aconcagua", -32.65, -70.01),
    new Place("Mont Blanc", 45.83, 6.86),
  };

  private static final Place[] LOWLANDS = {
    new Place("Amazon basin", -3.0, -62.0),
    new Place("West Siberia", 61.0, 73.0),
    new Place("Congo basin", 0.0, 21.0),
  };

  private static final Place[] HOTSPOTS = {
    new Place("Hawaii", 19.6, -155.5),
    new Place("Reunion", -21.1, 55.5),
  };

  private static final Place[] RIVERS = {
    new Place("Nile at Luxor", 25.70, 32.64),
    new Place("Amazon at Manaus", -3.14, -59.98),
    new Place("Volga at Volgograd", 48.70, 44.52),
    new Place("Yangtze at Wuhan", 30.58, 114.28),
  };

  private static final Place SAHARA = new Place("Sahara", 23.0, 10.0);
  private static final Place AMAZON = new Place("Amazon", -3.0, -62.0);
  private static final Place ANTARCTICA = new Place("Antarctica", -80.0, 0.0);

  @AfterEach
  public void restoreConfig() {
    TFCRealWorldConfig.clearServerConfig();
  }

  @Test
  public void continentsFollowTheirOption() {
    RegionGenerator generator = generator(true, true, true, true, true, true);
    assertNotNull(ContinentNoiseRegistry.get(generator));
    assertEquals(LAND.length, count(generator, LAND, Region.Point::land));
    assertEquals(0, count(generator, SEA, Region.Point::land));

    generator = generator(false, true, true, true, true, true);
    assertTrue(
      count(generator, LAND, Region.Point::land) < LAND.length ||
        count(generator, SEA, Region.Point::land) > 0,
      "procedural continents"
    );
  }

  @Test
  public void lakesFollowTheirOption() {
    RegionGenerator generator = generator(true, true, true, true, true, true);
    assertEquals(LAKES.length, count(generator, LAKES, Region.Point::lake));

    generator = generator(true, false, true, true, true, true);
    assertEquals(LAKES.length, count(generator, LAKES, Region.Point::land));
    assertTrue(
      count(generator, LAKES, Region.Point::lake) < LAKES.length,
      "map lakes are gone"
    );
    assertTrue(scan(generator, Region.Point::lake) > 0, "procedural lakes");
  }

  @Test
  public void lakesNeedContinents() {
    final RegionGenerator generator = generator(
      false,
      true,
      true,
      true,
      true,
      true
    );
    assertTrue(count(generator, LAKES, Region.Point::lake) < LAKES.length);
  }

  @Test
  public void tectonicsFollowTheirOption() {
    RegionGenerator generator = generator(true, true, true, true, true, true);
    assertTrue(TectonicsRegistry.isActive(generator));
    assertEquals(
      MOUNTAINS.length,
      count(generator, MOUNTAINS, Region.Point::mountain)
    );
    assertEquals(0, count(generator, LOWLANDS, Region.Point::mountain));

    generator = generator(true, true, false, true, true, true);
    assertFalse(TectonicsRegistry.isActive(generator));
    assertTrue(
      count(generator, MOUNTAINS, Region.Point::mountain) < MOUNTAINS.length ||
        count(generator, LOWLANDS, Region.Point::mountain) > 0,
      "procedural relief"
    );
    assertEquals(LAND.length, count(generator, LAND, Region.Point::land));
  }

  @Test
  public void tectonicsNeedContinents() {
    assertFalse(
      TectonicsRegistry.isActive(generator(false, true, true, true, true, true))
    );
  }

  @Test
  public void volcanoesFollowTheirOption() {
    RegionGenerator generator = generator(true, true, true, true, true, true);
    assertEquals(
      HOTSPOTS.length,
      count(generator, HOTSPOTS, MapOptionsTest::hotspot)
    );

    generator = generator(true, true, true, false, true, true);
    assertTrue(
      count(generator, HOTSPOTS, MapOptionsTest::hotspot) < HOTSPOTS.length,
      "map hotspots are gone"
    );
    assertTrue(scan(generator, MapOptionsTest::hotspot) > 0, "procedural");
    assertEquals(
      MOUNTAINS.length,
      count(generator, MOUNTAINS, Region.Point::mountain),
      "tectonics still come from the map"
    );
  }

  @Test
  public void volcanoesNeedTectonics() {
    final RegionGenerator generator = generator(
      true,
      true,
      false,
      true,
      true,
      true
    );
    assertTrue(
      count(generator, HOTSPOTS, MapOptionsTest::hotspot) < HOTSPOTS.length
    );
  }

  @Test
  public void riversFollowTheirOption() {
    RegionGenerator generator = generator(true, true, true, true, true, true);
    assertNotNull(RiversRegistry.get(generator));
    assertEquals(RIVERS.length, onRivers(generator));
    final double fromMap = distanceToRiver(generator, SAHARA);
    assertTrue(fromMap > DRY_DISTANCE_CELLS, "no river in the Sahara");

    // Vanilla grows rivers from every shore, deserts included.
    generator = generator(true, true, true, true, false, true);
    assertNull(RiversRegistry.get(generator));
    assertTrue(
      distanceToRiver(generator, SAHARA) < fromMap,
      "procedural rivers"
    );
  }

  @Test
  public void riversNeedContinents() {
    assertNull(
      RiversRegistry.get(generator(false, true, true, true, true, true))
    );
  }

  @Test
  public void climateFollowsItsOption() {
    RegionGenerator generator = generator(true, true, true, true, true, true);
    final float[] fromMap = climate(generator);
    assertTrue(at(generator, SAHARA).rainfall < 75f, "dry Sahara");
    assertTrue(at(generator, AMAZON).rainfall > 250f, "wet Amazon");
    assertTrue(at(generator, ANTARCTICA).temperature < -15f, "cold pole");

    generator = generator(true, true, true, true, true, false);
    final float[] procedural = climate(generator);
    float difference = 0f;
    for (int i = 0; i < fromMap.length; i++) {
      difference = Math.max(difference, Math.abs(fromMap[i] - procedural[i]));
    }
    assertTrue(difference > 1f, "procedural climate");
    assertEquals(LAND.length, count(generator, LAND, Region.Point::land));
  }

  private static RegionGenerator generator(
    boolean continents,
    boolean lakes,
    boolean tectonics,
    boolean volcanoes,
    boolean rivers,
    boolean climate
  ) {
    TFCRealWorldConfig.CONTINENT_FROM_MAP.setServerValue(continents);
    TFCRealWorldConfig.LAKES_FROM_MAP.setServerValue(lakes);
    TFCRealWorldConfig.TECTONICS_FROM_MAP.setServerValue(tectonics);
    TFCRealWorldConfig.VOLCANOES_FROM_MAP.setServerValue(volcanoes);
    TFCRealWorldConfig.RIVERS_FROM_MAP.setServerValue(rivers);
    TFCRealWorldConfig.CLIMATE_FROM_MAP.setServerValue(climate);
    return new RegionGenerator(
      BuiltinWorldPreset.defaultSettings(),
      Seed.of(SEED)
    );
  }

  private static boolean hotspot(Region.Point point) {
    return point.hotSpotAge > 0;
  }

  private static double gridX(Place place) {
    return Units.blockToGridExact(
      WaypointCoordinates.toBlockXZ(place.latitude(), place.longitude())[0]
    );
  }

  private static double gridZ(Place place) {
    return Units.blockToGridExact(
      WaypointCoordinates.toBlockXZ(place.latitude(), place.longitude())[1]
    );
  }

  private static Region.Point at(RegionGenerator generator, Place place) {
    return generator.getOrCreateRegionPoint(
      (int) Math.floor(gridX(place)),
      (int) Math.floor(gridZ(place))
    );
  }

  private static int count(
    RegionGenerator generator,
    Place[] places,
    Predicate<Region.Point> feature
  ) {
    int count = 0;
    for (final Place place : places) {
      count += feature.test(at(generator, place)) ? 1 : 0;
    }
    return count;
  }

  private static int scan(
    RegionGenerator generator,
    Predicate<Region.Point> feature
  ) {
    int count = 0;
    for (int x = -SCAN_RADIUS; x < SCAN_RADIUS; x++) {
      for (int z = -SCAN_RADIUS; z < SCAN_RADIUS; z++) {
        count += feature.test(generator.getOrCreateRegionPoint(x, z)) ? 1 : 0;
      }
    }
    return count;
  }

  private static float[] climate(RegionGenerator generator) {
    final Place[] places = { SAHARA, AMAZON, ANTARCTICA };
    final float[] values = new float[2 * places.length];
    for (int i = 0; i < places.length; i++) {
      final Region.Point point = at(generator, places[i]);
      values[2 * i] = point.temperature;
      values[2 * i + 1] = point.rainfall;
    }
    return values;
  }

  /** How many of the river places have a river within the tolerance. */
  private static int onRivers(RegionGenerator generator) {
    int count = 0;
    for (final Place place : RIVERS) {
      count +=
        distanceToRiver(generator, place) <= RIVER_TOLERANCE_CELLS ? 1 : 0;
    }
    return count;
  }

  private static double distanceToRiver(
    RegionGenerator generator,
    Place place
  ) {
    final double gridX = gridX(place);
    final double gridZ = gridZ(place);
    double nearest = Double.MAX_VALUE;
    for (int dx = -1; dx <= 1; dx++) {
      for (int dz = -1; dz <= 1; dz++) {
        for (final RiverEdge edge : generator
          .getOrCreatePartitionPoint(
            (int) Math.floor(gridX) + dx * Units.PARTITION_WIDTH_IN_GRID,
            (int) Math.floor(gridZ) + dz * Units.PARTITION_WIDTH_IN_GRID
          )
          .rivers()) {
          nearest = Math.min(
            nearest,
            Math.sqrt(edge.fractal().intersectDistance(gridX, gridZ))
          );
        }
      }
    }
    return nearest;
  }
}
