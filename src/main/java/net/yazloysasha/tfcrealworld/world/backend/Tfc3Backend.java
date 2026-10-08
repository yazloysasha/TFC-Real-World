package net.yazloysasha.tfcrealworld.world.backend;

import java.util.Random;
import java.util.function.LongSupplier;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.layer.RegionEdgeBiomeLayer;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.layer.framework.AreaContext;
import net.dries007.tfc.world.layer.framework.AreaFactory;
import net.dries007.tfc.world.noise.Cellular2D;
import net.dries007.tfc.world.region.Region;
import net.yazloysasha.tfcrealworld.world.layer.LowCoastShoreLayer;
import net.yazloysasha.tfcrealworld.world.layer.ShoreBeforeTidalFlatsLayer;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicsMap;
import net.yazloysasha.tfcrealworld.world.volcano.CenteredFeatureAligner;

/** TFC 3 itself: its biomes, and ocean depth as the only tectonic field. */
public class Tfc3Backend implements WorldBackend {

  /** TFC 3 base ocean depths its biome choice reads as each depth class. */
  private static final byte[] BASE_OCEAN_DEPTH = { 0, 3, 1, 1, 6, 10 };

  /**
   * Annual mean temperatures at which TFC 4 and TFC 3 draw the same climate
   * border: ice cap, tundra, subarctic, subtropical, tropical. Between the
   * borders the scale is stretched evenly, and past them it is shifted.
   */
  private static final float[] TFC4_TEMPERATURES = { -17, -12, -2, 17, 21, 30 };
  private static final float[] TFC3_TEMPERATURES = { -20, -14, -5, 12, 18, 30 };

  /** Layer ids looked at for lake biomes: more than any generator has. */
  private static final int MAX_BIOMES = 256;

  /** By biome id, whether it is a lake. Built on first use. */
  private volatile boolean[] lakes;

  @Override
  public boolean isLake(int biome) {
    boolean[] known = lakes;
    if (known == null) {
      known = new boolean[MAX_BIOMES];
      for (int land = 0; land < MAX_BIOMES; land++) {
        if (hasLake(land)) {
          known[lakeFor(land)] = true;
        }
      }
      if (riftLake() != NONE) {
        known[riftLake()] = true;
      }
      lakes = known;
    }
    return biome >= 0 && biome < MAX_BIOMES && known[biome];
  }

  @Override
  public LongSupplier layerSeeds(long seed) {
    return new Random(seed)::nextLong;
  }

  @Override
  public AreaFactory regionEdges(long seed, AreaFactory layer) {
    return RegionEdgeBiomeLayer.INSTANCE.apply(seed, layer);
  }

  @Override
  public AreaFactory shores(long seed, AreaFactory layer) {
    return LowCoastShoreLayer.INSTANCE.apply(seed, layer);
  }

  @Override
  public AreaFactory moreShores(long seed, AreaFactory layer) {
    // TFC 3's own layer lays tidal flats on the sea: see LowCoastShoreLayer.
    return ShoreBeforeTidalFlatsLayer.INSTANCE.apply(seed, layer);
  }

  @Override
  public int moreShores(
    AreaContext context,
    int north,
    int east,
    int south,
    int west,
    int center
  ) {
    return ShoreBeforeTidalFlatsLayer.INSTANCE.apply(
      context,
      north,
      east,
      south,
      west,
      center
    );
  }

  @Override
  public BiomeExtension biome(int id) {
    return TFCLayers.getFromLayerId(id);
  }

  @Override
  public boolean isOcean(int biome) {
    return TFCLayers.isOcean(biome);
  }

  @Override
  public boolean isMountains(int biome) {
    return TFCLayers.isMountains(biome);
  }

  @Override
  public boolean isVolcanicBiome(int biome) {
    return biome(biome).isVolcanic();
  }

  @Override
  public boolean hasLake(int biome) {
    return TFCLayers.hasLake(biome);
  }

  @Override
  public int lakeFor(int biome) {
    return TFCLayers.lakeFor(biome);
  }

  @Override
  public boolean hasShore(int biome) {
    return TFCLayers.hasShore(biome);
  }

  @Override
  public int shoreFor(int biome) {
    return shoreOf(biome);
  }

  /**
   * The shore TFC 4 gives a land, among the shores TFC 3 has: its dunes,
   * cliffs and rocky shores are all TFC 3's shore, and the tidal flats of
   * its other coasts are TFC 3's tidal flats.
   */
  public static int shoreOf(int biome) {
    final int shore = TFCLayers.shoreFor(biome);
    final boolean flat =
      biome == TFCLayers.PLAINS ||
      biome == TFCLayers.BADLANDS ||
      biome == TFCLayers.INVERTED_BADLANDS;
    return shore == TFCLayers.SHORE && flat ? TFCLayers.TIDAL_FLATS : shore;
  }

  @Override
  public int ocean() {
    return TFCLayers.OCEAN;
  }

  @Override
  public int plains() {
    return TFCLayers.PLAINS;
  }

  @Override
  public int lake() {
    return TFCLayers.LAKE;
  }

  @Override
  public int shore() {
    return TFCLayers.SHORE;
  }

  @Override
  public int tidalFlats() {
    return TFCLayers.TIDAL_FLATS;
  }

  @Override
  public int oceanReef() {
    return TFCLayers.OCEAN_REEF;
  }

  @Override
  public int oldMountains() {
    return TFCLayers.OLD_MOUNTAINS;
  }

  @Override
  public float climateTemperature(float mapTemperature) {
    final int last = TFC4_TEMPERATURES.length - 1;
    if (mapTemperature <= TFC4_TEMPERATURES[0]) {
      return mapTemperature + TFC3_TEMPERATURES[0] - TFC4_TEMPERATURES[0];
    }
    if (mapTemperature >= TFC4_TEMPERATURES[last]) {
      return mapTemperature + TFC3_TEMPERATURES[last] - TFC4_TEMPERATURES[last];
    }
    int border = 1;
    while (mapTemperature > TFC4_TEMPERATURES[border]) {
      border++;
    }
    final float share =
      (mapTemperature - TFC4_TEMPERATURES[border - 1]) /
      (TFC4_TEMPERATURES[border] - TFC4_TEMPERATURES[border - 1]);
    return (
      TFC3_TEMPERATURES[border - 1] +
      share * (TFC3_TEMPERATURES[border] - TFC3_TEMPERATURES[border - 1])
    );
  }

  /** TFC 3 stands every volcano in the cells of one noise. */
  @Override
  public void alignCenteredFeatures(
    Region region,
    long seed,
    TectonicsMap tectonics
  ) {
    final Cellular2D cells = new Cellular2D(seed).spread(0.009f);
    CenteredFeatureAligner.stamp(
      region,
      this,
      tectonics,
      (x, z) -> {
        final Cellular2D.Cell cell = cells.cell(x, z);
        return new double[] { cell.x(), cell.y() };
      },
      biome -> isVolcanicBiome(biome) && isMountains(biome),
      biome -> false
    );
  }

  @Override
  public void setOceanDepth(Region.Point point, int depth) {
    point.baseOceanDepth = BASE_OCEAN_DEPTH[depth];
  }
}
