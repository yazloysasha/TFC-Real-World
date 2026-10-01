package net.yazloysasha.tfcrealworld.world.tectonics;

import java.util.Locale;

/**
 * Properties of one {@code tectonics.png} palette index. Describes the place,
 * never a biome: {@code continent.png} decides land vs water, then the
 * matching half ({@link #land} or {@link #water}) is written into vanilla
 * region-point fields and vanilla {@code ChooseBiomes} picks the biome.
 * {@link #coast}: the sea reaches into the place (fjords, rias, outlet
 * glaciers, steep island coasts).
 * {@link #atolls}: coral reefs stand in the sea here, so vanilla may build
 * its atolls (it still asks for warm water).
 */
public record TectonicClass(
  Boundary boundary,
  LandRelief land,
  WaterDepth water,
  Volcanism volcanism,
  boolean coast,
  boolean atolls,
  byte hotspot
) {
  public static final TectonicClass DEFAULT = new TectonicClass(
    Boundary.NONE,
    LandRelief.LOWLAND,
    WaterDepth.DEEP,
    Volcanism.NONE,
    false,
    false,
    (byte) 0
  );

  public boolean isBoundary() {
    return boundary != Boundary.NONE;
  }

  /**
   * Active plate boundary zone the point lies in. Vanilla reads only the sign:
   * {@code < 0} collision belts / trenches, {@code > 0} rifts / ridges.
   */
  public enum Boundary {
    NONE(0f),
    CONVERGENT(-1f),
    DIVERGENT(1f),
    TRANSFORM(0f);

    private final float divergence;

    Boundary(float divergence) {
      this.divergence = divergence;
    }

    public float divergence() {
      return divergence;
    }
  }

  /**
   * Land relief. Maps onto vanilla discrete biome altitude (low / mid / high)
   * and the mountain flag.
   */
  public enum LandRelief {
    LOWLAND(0, 2),
    UPLAND(1, 7),
    HIGHLAND(2, 13),
    MOUNTAIN(3, 20);

    private final int discreteAltitude;
    private final byte baseLandHeight;

    LandRelief(int discreteAltitude, int baseLandHeight) {
      this.discreteAltitude = discreteAltitude;
      this.baseLandHeight = (byte) baseLandHeight;
    }

    public int discreteAltitude() {
      return discreteAltitude;
    }

    public byte baseLandHeight() {
      return baseLandHeight;
    }
  }

  /**
   * Seafloor. Maps onto vanilla {@code oceanDepth}: reef 1, shelf 2, ridge 3,
   * deep (abyssal) 4, trench 5.
   */
  public enum WaterDepth {
    REEF(1),
    SHELF(2),
    RIDGE(3),
    DEEP(4),
    TRENCH(5);

    private final byte oceanDepth;

    WaterDepth(int oceanDepth) {
      this.oceanDepth = (byte) oceanDepth;
    }

    public byte oceanDepth() {
      return oceanDepth;
    }
  }

  /**
   * Volcanism not tied to hotspots. Everything except {@link #NONE} sets the
   * vanilla volcanic flag on land (volcanic rocks, volcanic mountain variants).
   * Only {@link #ARC} marks water, as vanilla volcanic arc seafloor.
   */
  public enum Volcanism {
    NONE,
    ARC,
    RIFT,
    INTRAPLATE,
  }

  /**
   * A mountain range the sea reaches into ({@link #coast}): vanilla coastal
   * (oceanic) mountains, whose valleys drop below sea level; other ranges are
   * inland mountains.
   */
  public boolean isCoastalMountain() {
    return coast && land == LandRelief.MOUNTAIN;
  }

  static <E extends Enum<E>> E parseEnum(
    Class<E> type,
    String value,
    E fallback,
    String field,
    int index
  ) {
    if (value == null) {
      return fallback;
    }
    try {
      return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
        "Tectonics legend class " +
          index +
          " has unknown " +
          field +
          " value '" +
          value +
          "'"
      );
    }
  }
}
