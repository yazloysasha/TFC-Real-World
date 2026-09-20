package net.yazloysasha.tfcrealworld.world.backport;

import java.util.function.IntUnaryOperator;
import net.dries007.tfc.world.region.Region;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.region.RegionCoords;

/**
 * Choose-biomes helpers shared by TFG and TFE. Layer ids stay with the host
 * mixin so a backport that renames biomes only needs its layer stub updated.
 */
public final class ChooseBiomesSupport {

  public static final double ICE_SHEET_EDGE_MELTWATER_LAKE_CHANCE = 0.16;
  public static final float LAKE_RAINFALL_BOOST = 0.09f;
  public static final long ICE_SHEET_EDGE_LAKE_SALT = 0x7a4f2c91e83b05d6L;

  public static final float TOWER_KARST_MIN_RAINFALL = 425f;
  public static final float TOWER_KARST_RAIN_TEMP_SCALE = 10f;
  public static final float TOWER_KARST_RAIN_TEMP_MIN = 500f;

  /**
   * Current region cell for TFE {@code ChooseBiomes} redirects. Set from the
   * biome PUTFIELD hook so {@code getTowerKarstBiome} / hotspot helpers see
   * the same point TFC 4 {@code @Local} would.
   */
  public static final ThreadLocal<Region.Point> CURRENT_POINT =
    new ThreadLocal<>();

  private ChooseBiomesSupport() {}

  public static boolean isTowerKarstClimate(float rainfall, float temperature) {
    return (
      rainfall > TOWER_KARST_MIN_RAINFALL &&
      rainfall + TOWER_KARST_RAIN_TEMP_SCALE * temperature >
      TOWER_KARST_RAIN_TEMP_MIN
    );
  }

  /**
   * After TFE's in-loop {@code getTowerKarstBiome}, coastal lowlands are
   * already {@code TOWER_KARST_LAKE} / {@code TOWER_KARST_PLAINS}. Map those
   * back so the 1.21.1 coastal {@code SALT_MARSH} input still applies.
   */
  public static int towerKarstCoastalBase(
    int biome,
    int towerKarstLake,
    int towerKarstPlains,
    int lowlands,
    int plains
  ) {
    if (biome == towerKarstLake) {
      return lowlands;
    }
    if (biome == towerKarstPlains) {
      return plains;
    }
    return biome;
  }

  public static int randomSeededFrom(
    long rngSeed,
    int areaSeed,
    int[] choices
  ) {
    return choices[Math.floorMod(rngSeed ^ areaSeed, choices.length)];
  }

  public static int burrenBase(
    int biome,
    int knobAndKettle,
    int patternedGround,
    int invertedPatternedGround,
    int iceSheetEdge,
    int drumlins,
    int lowCanyons,
    int lowlands,
    int plains,
    int oldMountains,
    int highlands,
    int stairStepCanyons,
    int mesas,
    int buttes,
    int plateau
  ) {
    if (
      biome == knobAndKettle ||
      biome == patternedGround ||
      biome == invertedPatternedGround ||
      biome == iceSheetEdge
    ) {
      return drumlins;
    }
    if (biome == lowCanyons || biome == lowlands) {
      return plains;
    }
    if (biome == oldMountains) {
      return highlands;
    }
    if (biome == stairStepCanyons || biome == mesas || biome == buttes) {
      return plateau;
    }
    return biome;
  }

  public static int towerKarstCoastalInput(
    byte distanceToOcean,
    int biome,
    int lowlands,
    int plains,
    int lowCanyons,
    int saltMarsh
  ) {
    if (
      distanceToOcean <= 2 &&
      (biome == lowlands || biome == plains || biome == lowCanyons)
    ) {
      return saltMarsh;
    }
    return biome;
  }

  public static boolean isSoftMountainFill(
    int biome,
    int oldMountains,
    int plateau,
    int plateauWide,
    int highlands,
    int rollingHills,
    int rockyPlateau
  ) {
    return (
      biome == oldMountains ||
      biome == plateau ||
      biome == plateauWide ||
      biome == highlands ||
      biome == rollingHills ||
      biome == rockyPlateau
    );
  }

  public static void rollIceSheetEdgeLakes(
    Region region,
    long worldSeed,
    int iceSheetEdge,
    IntUnaryOperator lakeFor
  ) {
    if (!TFCRealWorldConfig.KOPPEN_FROM_MAP.get()) {
      return;
    }
    final Region.Point[] data = region.data();
    for (int index = 0; index < data.length; index++) {
      final Region.Point point = data[index];
      if (point == null || !point.land() || point.lake()) {
        continue;
      }
      if (point.biome != iceSheetEdge) {
        continue;
      }
      final int gridX = RegionCoords.gridX(region, index);
      final int gridZ = RegionCoords.gridZ(region, index);
      if (
        !GridSeededRandom.chance(
          worldSeed,
          gridX,
          gridZ,
          ICE_SHEET_EDGE_LAKE_SALT,
          ICE_SHEET_EDGE_MELTWATER_LAKE_CHANCE
        )
      ) {
        continue;
      }
      point.setLake();
      point.rainfall += LAKE_RAINFALL_BOOST * (500f - point.rainfall);
      point.biome = lakeFor.applyAsInt(iceSheetEdge);
    }
  }
}
