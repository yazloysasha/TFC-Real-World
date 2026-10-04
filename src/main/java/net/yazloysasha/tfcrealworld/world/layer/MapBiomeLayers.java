package net.yazloysasha.tfcrealworld.world.layer;

import java.util.function.LongSupplier;
import net.dries007.tfc.world.layer.RegionBiomeLayer;
import net.dries007.tfc.world.layer.RegionLayer;
import net.dries007.tfc.world.layer.SmoothLayer;
import net.dries007.tfc.world.layer.ZoomLayer;
import net.dries007.tfc.world.layer.framework.AreaFactory;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.Units;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.registry.ContinentNoiseRegistry;
import net.yazloysasha.tfcrealworld.world.backend.WorldBackend;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise;
import org.jetbrains.annotations.Nullable;

/**
 * Region biome layers with continents from the map. The generator's layers
 * in its order, except that shores are made at 16-block cells, where
 * {@code continent.png} gives the coastline, instead of 64-block cells:
 * <ol>
 *   <li>region biomes, region edges, zoom to 64 blocks (the generator's);</li>
 *   <li>ice sheet edges at 64 blocks, as the generator has them before its
 *       later zooms;</li>
 *   <li>zoom to 16 blocks, then {@link MapLandOceanCorrectionLayer} makes
 *       land, sea and lakes match the map;</li>
 *   <li>the generator's shores, widened inland to its 64-block shore width
 *       by {@link WidenShoreInlandLayer};</li>
 *   <li>its MoreShores, then the correction again, so no shore is left on
 *       map sea;</li>
 *   <li>the remaining zooms to quart scale and smoothing.</li>
 * </ol>
 * The generator's sequential seeds are drawn in its order, and seeds of
 * layers moved earlier are skipped where it would use them.
 */
public final class MapBiomeLayers {

  /** Zooms from the region grid to the 16-block cells of the coastline. */
  private static final int ZOOMS_GRID_TO_COASTLINE =
    Units.GRID_BITS - (Units.QUART_BITS + 2);
  /** The generator zooms once before its shores. */
  private static final int ZOOMS_BEFORE_SHORES = 1;
  /** Shore passes inland after the shores: 4 × 16 blocks = 64 blocks. */
  private static final int SHORE_WIDEN_PASSES = 3;
  private static final long ICE_SHEET_EDGE_SALT = 0x49434531L;
  private static final long ZOOM_SALT = 0x5A4F4F4DL;
  private static final long CORRECTION_SALT = 0x4D4C4F43L;
  private static final long WIDEN_SALT = 0x57494445L;
  private static final long POST_SHORE_CORRECTION_SALT = 0x4F434E31L;
  private static final long GOLDEN_RATIO = 0x9E3779B97F4A7C15L;

  private MapBiomeLayers() {}

  /** @return the layers, or null to leave the generator's own in place */
  @Nullable
  public static AreaFactory create(
    RegionGenerator generator,
    WorldBackend backend,
    long layerSeed
  ) {
    if (!TFCRealWorldConfig.CONTINENT_FROM_MAP.get()) {
      return null;
    }
    final PNGContinentNoise continent = ContinentNoiseRegistry.get(generator);
    if (continent == null) {
      return null;
    }
    final LongSupplier seed = backend.layerSeeds(layerSeed);
    AreaFactory layer = RegionBiomeLayer.INSTANCE.apply(
      new RegionLayer(generator).apply(seed.getAsLong())
    );
    layer = backend.regionEdges(seed.getAsLong(), layer);
    layer = ZoomLayer.NORMAL.apply(seed.getAsLong(), layer);
    final long shoreSeed = seed.getAsLong();
    layer = backend.iceSheetEdges(shoreSeed ^ ICE_SHEET_EDGE_SALT, layer);
    long zoomSeed = shoreSeed ^ ZOOM_SALT;
    for (int i = ZOOMS_BEFORE_SHORES; i < ZOOMS_GRID_TO_COASTLINE; i++) {
      layer = ZoomLayer.NORMAL.apply(zoomSeed, layer);
      zoomSeed = zoomSeed * GOLDEN_RATIO + 1L;
    }
    layer = new MapLandOceanCorrectionLayer(
      continent,
      generator,
      backend,
      ZOOMS_GRID_TO_COASTLINE,
      1 + SHORE_WIDEN_PASSES
    ).apply(shoreSeed ^ CORRECTION_SALT, layer);
    layer = backend.shores(shoreSeed, layer);
    final WidenShoreInlandLayer widen = new WidenShoreInlandLayer(backend);
    long widenSeed = shoreSeed ^ WIDEN_SALT;
    for (int i = 0; i < SHORE_WIDEN_PASSES; i++) {
      layer = widen.apply(widenSeed, layer);
      widenSeed = widenSeed * GOLDEN_RATIO + 1L;
    }
    final long moreShoresSeed = seed.getAsLong();
    layer = backend.moreShores(moreShoresSeed, layer);
    layer = new MapLandOceanCorrectionLayer(
      continent,
      generator,
      backend,
      ZOOMS_GRID_TO_COASTLINE,
      0
    ).apply(moreShoresSeed ^ POST_SHORE_CORRECTION_SALT, layer);
    // The ice sheet edges and the zooms already made before shores.
    if (backend.hasIceSheetEdges()) {
      seed.getAsLong();
    }
    for (int i = ZOOMS_BEFORE_SHORES; i < ZOOMS_GRID_TO_COASTLINE; i++) {
      seed.getAsLong();
    }
    for (
      int i = ZOOMS_GRID_TO_COASTLINE;
      i < Units.GRID_BITS - Units.QUART_BITS;
      i++
    ) {
      layer = ZoomLayer.NORMAL.apply(seed.getAsLong(), layer);
    }
    return SmoothLayer.INSTANCE.apply(seed.getAsLong(), layer);
  }
}
