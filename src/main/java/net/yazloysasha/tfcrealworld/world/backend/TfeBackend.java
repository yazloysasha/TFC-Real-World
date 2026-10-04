package net.yazloysasha.tfcrealworld.world.backend;

import static com.newterraearth.tfe.world.NTELayerIds.*;
import static net.dries007.tfc.world.layer.TFCLayers.VOLCANIC_MOUNTAINS;
import static net.dries007.tfc.world.layer.TFCLayers.VOLCANIC_MOUNTAIN_LAKE;
import static net.dries007.tfc.world.layer.TFCLayers.VOLCANIC_OCEANIC_MOUNTAINS;
import static net.dries007.tfc.world.layer.TFCLayers.VOLCANIC_OCEANIC_MOUNTAIN_LAKE;

import com.newterraearth.tfe.world.NTESeed;
import com.newterraearth.tfe.world.layer.NTEIceSheetEdgeLayer;
import com.newterraearth.tfe.world.layer.NTERiverShoreLayer;
import com.newterraearth.tfe.world.noise.NTECellular2D;
import com.newterraearth.tfe.world.region.NTEPointAccess;
import com.newterraearth.tfe.world.region.NTERegionGeneratorAccess;
import java.util.function.IntPredicate;
import java.util.function.LongSupplier;
import net.dries007.tfc.world.layer.framework.AreaFactory;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicsMap;
import net.yazloysasha.tfcrealworld.world.volcano.CenteredFeatureAligner;

/**
 * TerraFirmaEarth: TFC 4 generation over TFC 3 classes. It rewrites the
 * {@code TFCLayers} queries for its own biomes, and keeps the point fields
 * TFC 3 lacks behind {@link NTEPointAccess}.
 */
public final class TfeBackend extends Tfc3Backend {

  private static final float STRATOVOLCANO_SPREAD = 0.0021f;
  private static final float SHIELD_CINDER_SPREAD = 0.009f;

  @Override
  public LongSupplier layerSeeds(long seed) {
    return NTESeed.of(seed)::next;
  }

  @Override
  public AreaFactory shores(long seed, AreaFactory layer) {
    return NTERiverShoreLayer.INSTANCE.apply(seed, layer);
  }

  @Override
  public boolean hasIceSheetEdges() {
    return true;
  }

  @Override
  public AreaFactory iceSheetEdges(long seed, AreaFactory layer) {
    return NTEIceSheetEdgeLayer.INSTANCE.apply(seed, layer);
  }

  @Override
  public int meltwaterLake() {
    return MELTWATER_LAKE;
  }

  @Override
  public int riftLake() {
    return RIFT_LAKE;
  }

  @Override
  public int riverValley() {
    return RIVER_VALLEY;
  }

  @Override
  public boolean isFlatIceSheet(int biome) {
    return (
      biome == ICE_SHEET || biome == ICE_SHEET_TUYAS || biome == SUBGLACIAL_LAKE
    );
  }

  @Override
  public boolean isIceSheetForeland(int biome) {
    return (
      biome == KNOB_AND_KETTLE ||
      biome == PATTERNED_GROUND ||
      biome == INVERTED_PATTERNED_GROUND ||
      biome == STONE_CIRCLES
    );
  }

  @Override
  public boolean hasTectonicFields() {
    return true;
  }

  @Override
  public boolean hasHotspots() {
    return true;
  }

  @Override
  public byte hotSpotAge(Region.Point point) {
    return access(point).nte$getHotSpotAge();
  }

  @Override
  public void setHotSpotAge(Region.Point point, byte age) {
    access(point).nte$setHotSpotAge(age);
  }

  @Override
  public boolean volcanic(Region.Point point) {
    return access(point).nte$isVolcanic();
  }

  @Override
  public void setVolcanic(Region.Point point) {
    access(point).nte$setVolcanic(true);
  }

  @Override
  public boolean barrierIsland(Region.Point point) {
    return access(point).nte$isBarrierIsland();
  }

  @Override
  public void setBarrierIsland(Region.Point point) {
    access(point).nte$setBarrierIsland(true);
  }

  @Override
  public float divergence(Region.Point point) {
    return (float) access(point).nte$getDivergence();
  }

  @Override
  public float continentFactor(RegionGenerator generator, int x, int z) {
    return ((NTERegionGeneratorAccess) (Object) generator).nte$continentFactor(
      x,
      z
    );
  }

  @Override
  public void setDivergence(Region.Point point, float divergence) {
    access(point).nte$setDivergence(divergence);
  }

  @Override
  public void setRainfallVariance(Region.Point point, float variance) {
    access(point).nte$setRainfallVariance(variance);
  }

  @Override
  public void setDistanceToWestCoast(Region.Point point, byte distance) {
    access(point).nte$setDistanceToWestCoast(distance);
  }

  @Override
  public void alignCenteredFeatures(
    Region region,
    long seed,
    TectonicsMap tectonics
  ) {
    stamp(
      region,
      tectonics,
      new NTECellular2D(seed, 2).spread(STRATOVOLCANO_SPREAD),
      TfeBackend::isStratovolcanoBiome
    );
    stamp(
      region,
      tectonics,
      new NTECellular2D(seed).spread(SHIELD_CINDER_SPREAD),
      biome ->
        biome == ACTIVE_SHIELD_VOLCANO || biome == VOLCANIC_MOUNTAIN_ISLANDS
    );
  }

  private void stamp(
    Region region,
    TectonicsMap tectonics,
    NTECellular2D cells,
    IntPredicate matches
  ) {
    CenteredFeatureAligner.stamp(
      region,
      this,
      tectonics,
      (x, z) -> {
        final NTECellular2D.Cell cell = cells.cell(x, z);
        return new double[] { cell.x(), cell.y() };
      },
      matches,
      TfeBackend::isShieldHotspotBiome
    );
  }

  @Override
  public boolean isVolcanicBiome(int biome) {
    return isStratovolcanoBiome(biome) || biome == VOLCANIC_MOUNTAIN_ISLANDS;
  }

  private static boolean isShieldHotspotBiome(int biome) {
    return (
      biome == ACTIVE_SHIELD_VOLCANO ||
      biome == DORMANT_SHIELD_VOLCANO ||
      biome == EXTINCT_SHIELD_VOLCANO ||
      biome == ANCIENT_SHIELD_VOLCANO ||
      biome == SUNKEN_SHIELD_VOLCANO ||
      biome == ICE_SHEET_SHIELD_VOLCANO ||
      biome == GLACIATED_SHIELD_VOLCANO ||
      biome == SHIELD_VOLCANO_SHORE ||
      biome == OLD_SHIELD_VOLCANO_SHORE
    );
  }

  private static boolean isStratovolcanoBiome(int biome) {
    return (
      biome == OCEANIC_VOLCANIC_ARC ||
      biome == VOLCANIC_MOUNTAINS ||
      biome == VOLCANIC_OCEANIC_MOUNTAINS ||
      biome == VOLCANIC_ISLAND ||
      biome == VOLCANIC_MOUNTAIN_LAKE ||
      biome == VOLCANIC_OCEANIC_MOUNTAIN_LAKE ||
      biome == ICE_SHEET_VOLCANIC_MOUNTAINS ||
      biome == ICE_SHEET_VOLCANIC_OCEANIC_MOUNTAINS ||
      biome == GLACIATED_VOLCANIC_MOUNTAINS ||
      biome == GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS ||
      biome == GLACIALLY_CARVED_VOLCANIC_MOUNTAINS ||
      biome == GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS
    );
  }

  @Override
  public void setOceanDepth(Region.Point point, int depth) {
    access(point).nte$setOceanDepth((byte) depth);
  }

  private static NTEPointAccess access(Region.Point point) {
    return (NTEPointAccess) (Object) point;
  }
}
