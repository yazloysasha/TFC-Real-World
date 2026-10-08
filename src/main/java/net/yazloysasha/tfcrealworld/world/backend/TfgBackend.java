package net.yazloysasha.tfcrealworld.world.backend;

import java.util.function.LongSupplier;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.layer.framework.AreaContext;
import net.dries007.tfc.world.layer.framework.AreaFactory;
import net.dries007.tfc.world.region.Region;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicsMap;
import net.yazloysasha.tfcrealworld.world.volcano.CenteredFeatureAligner;
import su.terrafirmagreg.core.world.new_ow_wg.Seed;
import su.terrafirmagreg.core.world.new_ow_wg.TFGLayers;
import su.terrafirmagreg.core.world.new_ow_wg.layers.TFGIceSheetEdgeLayer;
import su.terrafirmagreg.core.world.new_ow_wg.layers.TFGMoreShoresLayer;
import su.terrafirmagreg.core.world.new_ow_wg.layers.TFGRegionEdgeBiomeLayer;
import su.terrafirmagreg.core.world.new_ow_wg.layers.TFGShoreLayer;
import su.terrafirmagreg.core.world.new_ow_wg.noise.TFGCellular2D;
import su.terrafirmagreg.core.world.new_ow_wg.region.IRegionPoint;

/**
 * TerraFirmaGreg's backport: its own biome table and tasks, with hotspots
 * and the west coast as point fields. Ocean depth is TFC 3's, and it has no
 * plate boundaries, volcanic arcs or rainfall variance.
 */
public final class TfgBackend extends Tfc3Backend {

  @Override
  public BiomeExtension biome(int id) {
    return TFGLayers.getFromLayerId(id);
  }

  @Override
  public boolean isOcean(int biome) {
    return TFGLayers.isOcean(biome);
  }

  @Override
  public boolean isMountains(int biome) {
    return TFGLayers.isMountains(biome);
  }

  @Override
  public boolean isVolcanicBiome(int biome) {
    return (
      biome == TFGLayers.VOLCANIC_MOUNTAINS ||
      biome == TFGLayers.VOLCANIC_OCEANIC_MOUNTAINS ||
      biome == TFGLayers.VOLCANIC_MOUNTAIN_LAKE ||
      biome == TFGLayers.VOLCANIC_OCEANIC_MOUNTAIN_LAKE
    );
  }

  @Override
  public boolean hasLake(int biome) {
    return TFGLayers.hasLake(biome);
  }

  @Override
  public int lakeFor(int biome) {
    return TFGLayers.lakeFor(biome);
  }

  @Override
  public boolean hasShore(int biome) {
    return TFGLayers.hasShore(biome);
  }

  @Override
  public int shoreFor(int biome) {
    return TFGLayers.shoreFor(biome);
  }

  @Override
  public LongSupplier layerSeeds(long seed) {
    return Seed.of(seed)::next;
  }

  @Override
  public AreaFactory regionEdges(long seed, AreaFactory layer) {
    return TFGRegionEdgeBiomeLayer.INSTANCE.apply(seed, layer);
  }

  @Override
  public AreaFactory shores(long seed, AreaFactory layer) {
    return TFGShoreLayer.INSTANCE.apply(seed, layer);
  }

  @Override
  public AreaFactory moreShores(long seed, AreaFactory layer) {
    return TFGMoreShoresLayer.INSTANCE.apply(seed, layer);
  }

  @Override
  public boolean hasIceSheetEdges() {
    return true;
  }

  @Override
  public AreaFactory iceSheetEdges(long seed, AreaFactory layer) {
    return TFGIceSheetEdgeLayer.INSTANCE.apply(seed, layer);
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
    return TFGMoreShoresLayer.INSTANCE.apply(
      context,
      north,
      east,
      south,
      west,
      center
    );
  }

  @Override
  public int ocean() {
    return TFGLayers.OCEAN;
  }

  @Override
  public int plains() {
    return TFGLayers.PLAINS;
  }

  @Override
  public int lake() {
    return TFGLayers.LAKE;
  }

  @Override
  public int shore() {
    return TFGLayers.SHORE;
  }

  @Override
  public int tidalFlats() {
    return TFGLayers.TIDAL_FLATS;
  }

  @Override
  public int oceanReef() {
    return TFGLayers.OCEAN_REEF;
  }

  @Override
  public int oldMountains() {
    return TFGLayers.OLD_MOUNTAINS;
  }

  @Override
  public int meltwaterLake() {
    return TFGLayers.MELTWATER_LAKE;
  }

  @Override
  public boolean isFlatIceSheet(int biome) {
    return TFGLayers.isFlatIceSheet(biome);
  }

  @Override
  public boolean isIceSheetForeland(int biome) {
    return (
      biome == TFGLayers.KNOB_AND_KETTLE ||
      biome == TFGLayers.PATTERNED_GROUND ||
      biome == TFGLayers.INVERTED_PATTERNED_GROUND ||
      biome == TFGLayers.STONE_CIRCLES
    );
  }

  /** Its previews make generators without a level; it keeps the seed. */
  @Override
  public long levelSeed(long held) {
    return Seed.worldSeed;
  }

  @Override
  public boolean hasHotspots() {
    return true;
  }

  @Override
  public byte hotSpotAge(Region.Point point) {
    return access(point).tfg$getHotSpotAge();
  }

  @Override
  public void setHotSpotAge(Region.Point point, byte age) {
    access(point).tfg$setHotSpotAge(age);
  }

  @Override
  public void setDistanceToWestCoast(Region.Point point, byte distance) {
    access(point).tfg$setDistanceToWestCoast(distance);
  }

  @Override
  public void alignCenteredFeatures(
    Region region,
    long seed,
    TectonicsMap tectonics
  ) {
    final TFGCellular2D cells = new TFGCellular2D(seed).spread(0.009f);
    CenteredFeatureAligner.stamp(
      region,
      this,
      tectonics,
      (x, z) -> {
        final TFGCellular2D.TFGCell cell = cells.cell(x, z);
        return new double[] { cell.x(), cell.y() };
      },
      biome ->
        biome == TFGLayers.ACTIVE_SHIELD_VOLCANO || isVolcanicBiome(biome),
      TfgBackend::keepsItsCenter
    );
  }

  /** Hotspot shields and ice-covered mountains keep their own biome. */
  private static boolean keepsItsCenter(int biome) {
    return (
      biome == TFGLayers.ACTIVE_SHIELD_VOLCANO ||
      biome == TFGLayers.DORMANT_SHIELD_VOLCANO ||
      biome == TFGLayers.EXTINCT_SHIELD_VOLCANO ||
      biome == TFGLayers.ANCIENT_SHIELD_VOLCANO ||
      biome == TFGLayers.SUNKEN_SHIELD_VOLCANO ||
      biome == TFGLayers.ICE_SHEET_SHIELD_VOLCANO ||
      biome == TFGLayers.GLACIATED_SHIELD_VOLCANO ||
      biome == TFGLayers.SHIELD_VOLCANO_SHORE ||
      biome == TFGLayers.OLD_SHIELD_VOLCANO_SHORE ||
      biome == TFGLayers.ICE_SHEET_MOUNTAINS ||
      biome == TFGLayers.ICE_SHEET_OCEANIC_MOUNTAINS ||
      biome == TFGLayers.GLACIATED_MOUNTAINS ||
      biome == TFGLayers.GLACIATED_OCEANIC_MOUNTAINS ||
      biome == TFGLayers.GLACIALLY_CARVED_MOUNTAINS ||
      biome == TFGLayers.GLACIALLY_CARVED_OCEANIC_MOUNTAINS
    );
  }

  private static IRegionPoint access(Region.Point point) {
    return (IRegionPoint) (Object) point;
  }
}
