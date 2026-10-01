package net.yazloysasha.tfcrealworld.mixin.world.layer;

import net.dries007.tfc.world.Seed;
import net.dries007.tfc.world.layer.IceSheetEdgeLayer;
import net.dries007.tfc.world.layer.MoreShoresLayer;
import net.dries007.tfc.world.layer.RegionBiomeLayer;
import net.dries007.tfc.world.layer.RegionEdgeBiomeLayer;
import net.dries007.tfc.world.layer.RegionLayer;
import net.dries007.tfc.world.layer.ShoreAndRiverLayer;
import net.dries007.tfc.world.layer.SmoothLayer;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.layer.ZoomLayer;
import net.dries007.tfc.world.layer.framework.AreaFactory;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.Units;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.registry.ContinentNoiseRegistry;
import net.yazloysasha.tfcrealworld.world.layer.MapLandOceanCorrectionLayer;
import net.yazloysasha.tfcrealworld.world.layer.WidenShoreInlandLayer;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Region biome layers with continents from the map. Vanilla's layers in
 * vanilla order, except that shores are made at 16-block cells, where
 * {@code continent.png} gives the coastline, instead of 64-block cells:
 * <ol>
 *   <li>region biomes, region edges, zoom to 64 blocks (vanilla);</li>
 *   <li>ice sheet edges at 64 blocks, as vanilla before its later zooms;</li>
 *   <li>zoom to 16 blocks, then {@link MapLandOceanCorrectionLayer} makes
 *       land, sea and lakes match the map;</li>
 *   <li>vanilla shores, widened inland to vanilla's 64-block shore width by
 *       {@link WidenShoreInlandLayer};</li>
 *   <li>vanilla MoreShores, then the correction again, so no shore is left on
 *       map sea;</li>
 *   <li>the remaining vanilla zooms to quart scale and smoothing.</li>
 * </ol>
 * Vanilla's sequential seeds are drawn in vanilla order, and seeds of layers
 * moved earlier are skipped where vanilla would use them.
 */
@Mixin(value = TFCLayers.class, remap = false)
public class TFCLayersMixin {

  /** Zooms from the region grid to the 16-block cells of the coastline. */
  @Unique
  private static final int ZOOMS_GRID_TO_COASTLINE =
    Units.GRID_BITS - (Units.QUART_BITS + 2);

  /** Vanilla zooms once before its shores. */
  @Unique
  private static final int VANILLA_ZOOMS_BEFORE_SHORES = 1;

  /** Shore passes inland after ShoreAndRiver: 4 × 16 blocks = vanilla width. */
  @Unique
  private static final int SHORE_WIDEN_PASSES = 3;

  @Unique
  private static final long ICE_SHEET_EDGE_SALT = 0x49434531L;

  @Unique
  private static final long ZOOM_SALT = 0x5A4F4F4DL;

  @Unique
  private static final long CORRECTION_SALT = 0x4D4C4F43L;

  @Unique
  private static final long WIDEN_SALT = 0x57494445L;

  @Unique
  private static final long POST_SHORE_CORRECTION_SALT = 0x4F434E31L;

  @Unique
  private static final long GOLDEN_RATIO = 0x9E3779B97F4A7C15L;

  @Inject(
    method = "createRegionBiomeLayer",
    at = @At("HEAD"),
    cancellable = true
  )
  private static void tfcrealworld$regionBiomeLayerFromMap(
    RegionGenerator generator,
    Seed seed,
    CallbackInfoReturnable<AreaFactory> cir
  ) {
    if (!TFCRealWorldConfig.CONTINENT_FROM_MAP.get()) {
      return;
    }
    final PNGContinentNoise continent = ContinentNoiseRegistry.get(generator);
    if (continent == null) {
      return;
    }

    AreaFactory layer = RegionBiomeLayer.INSTANCE.apply(
      new RegionLayer(generator).apply(seed.next())
    );
    layer = RegionEdgeBiomeLayer.INSTANCE.apply(seed.next(), layer);
    layer = ZoomLayer.NORMAL.apply(seed.next(), layer);

    final long shoreSeed = seed.next();
    layer = IceSheetEdgeLayer.INSTANCE.apply(
      shoreSeed ^ ICE_SHEET_EDGE_SALT,
      layer
    );
    long zoomSeed = shoreSeed ^ ZOOM_SALT;
    for (
      int i = VANILLA_ZOOMS_BEFORE_SHORES;
      i < ZOOMS_GRID_TO_COASTLINE;
      i++
    ) {
      layer = ZoomLayer.NORMAL.apply(zoomSeed, layer);
      zoomSeed = zoomSeed * GOLDEN_RATIO + 1L;
    }
    layer = new MapLandOceanCorrectionLayer(
      continent,
      generator,
      ZOOMS_GRID_TO_COASTLINE,
      1 + SHORE_WIDEN_PASSES
    ).apply(shoreSeed ^ CORRECTION_SALT, layer);
    layer = ShoreAndRiverLayer.INSTANCE.apply(shoreSeed, layer);
    long widenSeed = shoreSeed ^ WIDEN_SALT;
    for (int i = 0; i < SHORE_WIDEN_PASSES; i++) {
      layer = WidenShoreInlandLayer.INSTANCE.apply(widenSeed, layer);
      widenSeed = widenSeed * GOLDEN_RATIO + 1L;
    }

    final long moreShoresSeed = seed.next();
    layer = MoreShoresLayer.INSTANCE.apply(moreShoresSeed, layer);
    layer = new MapLandOceanCorrectionLayer(
      continent,
      generator,
      ZOOMS_GRID_TO_COASTLINE,
      0
    ).apply(moreShoresSeed ^ POST_SHORE_CORRECTION_SALT, layer);

    // Vanilla's ice sheet edges and the zooms already made before shores.
    seed.next();
    for (
      int i = VANILLA_ZOOMS_BEFORE_SHORES;
      i < ZOOMS_GRID_TO_COASTLINE;
      i++
    ) {
      seed.next();
    }
    for (
      int i = ZOOMS_GRID_TO_COASTLINE;
      i < Units.GRID_BITS - Units.QUART_BITS;
      i++
    ) {
      layer = ZoomLayer.NORMAL.apply(seed.next(), layer);
    }
    layer = SmoothLayer.INSTANCE.apply(seed.next(), layer);
    cir.setReturnValue(layer);
  }
}
