package net.yazloysasha.tfcrealworld.world.backend;

import java.util.function.LongSupplier;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.layer.framework.AreaContext;
import net.dries007.tfc.world.layer.framework.AreaFactory;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicsMap;

/**
 * What the world generator in use offers beyond TFC 3: its biome layer ids
 * and the fields it adds to a region point. A generator that lacks a biome
 * answers {@link #NONE}; one that lacks a field ignores what is written to it
 * and reads it as unset.
 */
public interface WorldBackend {
  /** No such biome (layer biome ids are non-negative). */
  int NONE = -1;

  BiomeExtension biome(int id);

  boolean isOcean(int biome);

  boolean isMountains(int biome);

  /** A biome that builds volcanoes. */
  boolean isVolcanicBiome(int biome);

  /** A biome some land floods into, or the lake of a rift. */
  boolean isLake(int biome);

  boolean hasLake(int biome);

  int lakeFor(int biome);

  boolean hasShore(int biome);

  int shoreFor(int biome);

  /** The seeds the generator draws for its biome layers, in its order. */
  LongSupplier layerSeeds(long seed);

  AreaFactory regionEdges(long seed, AreaFactory layer);

  AreaFactory shores(long seed, AreaFactory layer);

  AreaFactory moreShores(long seed, AreaFactory layer);

  /** Whether the generator has a layer that makes the edges of ice sheets. */
  default boolean hasIceSheetEdges() {
    return false;
  }

  default AreaFactory iceSheetEdges(long seed, AreaFactory layer) {
    return layer;
  }

  /** The generator's layer that widens shores along the waterline. */
  int moreShores(
    AreaContext context,
    int north,
    int east,
    int south,
    int west,
    int center
  );

  int ocean();

  int plains();

  int lake();

  int shore();

  int tidalFlats();

  int oceanReef();

  int oldMountains();

  /**
   * A shore biome the generator lays on the sea side of the waterline, so
   * it stands on cells the map calls sea.
   */
  default boolean isSeawardShore(int biome) {
    return false;
  }

  /**
   * The annual mean temperature of the climate maps in the generator's own
   * scale. The maps are drawn in TFC 4's.
   */
  default float climateTemperature(float mapTemperature) {
    return mapTemperature;
  }

  default int meltwaterLake() {
    return NONE;
  }

  default int riftLake() {
    return NONE;
  }

  default int riverValley() {
    return NONE;
  }

  default boolean isFlatIceSheet(int biome) {
    return false;
  }

  /** Bare ground the ice sheet edge layer makes an ice sheet's edge of. */
  default boolean isIceSheetForeland(int biome) {
    return false;
  }

  /** Whether plate boundaries, ocean depths and volcanism are point fields. */
  default boolean hasTectonicFields() {
    return false;
  }

  /** Whether hotspots are a point field with biomes of their own. */
  default boolean hasHotspots() {
    return false;
  }

  default byte hotSpotAge(Region.Point point) {
    return 0;
  }

  default void setHotSpotAge(Region.Point point, byte age) {}

  default boolean volcanic(Region.Point point) {
    return false;
  }

  default void setVolcanic(Region.Point point) {}

  default boolean barrierIsland(Region.Point point) {
    return false;
  }

  default void setBarrierIsland(Region.Point point) {}

  default float divergence(Region.Point point) {
    return 0;
  }

  default void setDivergence(Region.Point point, float divergence) {}

  /**
   * The seed biome noise is made with.
   *
   * @param held the seed of the level the generator is being made for
   */
  default long levelSeed(long held) {
    return held;
  }

  /** How much of the continents is left at a point near the world's edge. */
  default float continentFactor(RegionGenerator generator, int x, int z) {
    return 1;
  }

  default void setRainfallVariance(Region.Point point, float variance) {}

  default void setDistanceToWestCoast(Region.Point point, byte distance) {}

  /**
   * Moves the centres of the generator's volcanic cones, which stand where
   * its cell noise puts them, onto the volcanic biomes of the map.
   */
  void alignCenteredFeatures(Region region, long seed, TectonicsMap tectonics);

  /**
   * Ocean depth in TFC 4 classes: 1 shallows of an arc or reef, 2 shelf,
   * 3 ridge, 4 deep ocean, 5 trench.
   */
  void setOceanDepth(Region.Point point, int depth);
}
