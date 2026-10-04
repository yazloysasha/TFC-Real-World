package net.yazloysasha.tfcrealworld.mixin.world.region;

import java.util.List;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.Units;
import net.dries007.tfc.world.settings.Settings;
import net.minecraft.util.RandomSource;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.helpers.WorldSeedHolder;
import net.yazloysasha.tfcrealworld.util.registry.ContinentNoiseRegistry;
import net.yazloysasha.tfcrealworld.util.registry.LevelSeedRegistry;
import net.yazloysasha.tfcrealworld.util.registry.RainVarianceRegistry;
import net.yazloysasha.tfcrealworld.util.registry.RiftLakesRegistry;
import net.yazloysasha.tfcrealworld.util.registry.RiversRegistry;
import net.yazloysasha.tfcrealworld.util.registry.TectonicsRegistry;
import net.yazloysasha.tfcrealworld.world.backend.Backends;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGRainVarianceNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGRainfallNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGTemperatureNoise;
import net.yazloysasha.tfcrealworld.world.region.cache.GlobalOceanDistanceCache;
import net.yazloysasha.tfcrealworld.world.region.cache.GlobalWestCoastDistanceCache;
import net.yazloysasha.tfcrealworld.world.river.MapRivers;
import net.yazloysasha.tfcrealworld.world.tectonics.MapRidges;
import net.yazloysasha.tfcrealworld.world.tectonics.MapRiftLakes;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicsMap;
import net.yazloysasha.tfcrealworld.world.volcano.MapHotspotLayout;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replaces the generator's procedural continent and climate noises with the
 * profile maps, and registers the maps and rivers the region tasks read.
 */
@Mixin(value = RegionGenerator.class, remap = false, priority = 500)
public class RegionGeneratorMixin {

  @Shadow
  @Final
  @Mutable
  public Noise2D continentNoise;

  @Shadow
  @Final
  @Mutable
  public Noise2D temperatureNoise;

  @Shadow
  @Final
  @Mutable
  public Noise2D rainfallNoise;

  /**
   * Grid cells between the points a cell is searched for regions at: more
   * than a river edge reaches (6), far less than a region is wide.
   */
  @Unique
  private static final int REGION_SEARCH_STEP = 8;

  @Inject(method = "<init>", at = @At("TAIL"))
  private void tfcrealworld$replaceNoises(
    Settings settings,
    RandomSource random,
    CallbackInfo ci
  ) {
    final RegionGenerator generator = (RegionGenerator) (Object) this;
    final long levelSeed = Backends.current().levelSeed(
      WorldSeedHolder.getSeed()
    );
    LevelSeedRegistry.register(generator, levelSeed);
    final int horizontalScale = TFCRealWorldConfig.HORIZONTAL_SCALE.get();
    final int verticalScale = TFCRealWorldConfig.VERTICAL_SCALE.get();
    if (TFCRealWorldConfig.CONTINENT_FROM_MAP.get()) {
      final PNGContinentNoise continent = new PNGContinentNoise(
        horizontalScale,
        verticalScale
      );
      continentNoise = continent;
      ContinentNoiseRegistry.register(generator, continent);
      GlobalOceanDistanceCache.initialize(continent);
      GlobalWestCoastDistanceCache.initialize(continent);
      final MapRivers rivers = MapRivers.tryLoad(
        horizontalScale,
        verticalScale
      );
      if (rivers != null) {
        RiversRegistry.register(generator, rivers);
      }
      TectonicsRegistry.clearStatics();
      if (TFCRealWorldConfig.TECTONICS_FROM_MAP.get()) {
        tfcrealworld$registerTectonics(
          generator,
          continent,
          levelSeed,
          horizontalScale,
          verticalScale
        );
      }
    }
    if (TFCRealWorldConfig.CLIMATE_FROM_MAP.get()) {
      temperatureNoise = new PNGTemperatureNoise(
        horizontalScale,
        verticalScale
      );
      rainfallNoise = new PNGRainfallNoise(horizontalScale, verticalScale);
      RainVarianceRegistry.register(
        generator,
        new PNGRainVarianceNoise(horizontalScale, verticalScale)
      );
    }
  }

  @Unique
  private static void tfcrealworld$registerTectonics(
    RegionGenerator generator,
    PNGContinentNoise continent,
    long levelSeed,
    int horizontalScale,
    int verticalScale
  ) {
    final TectonicsMap tectonics = TectonicsMap.tryCreate(
      horizontalScale,
      verticalScale
    );
    if (tectonics == null) {
      TFCRealWorld.LOGGER.warn(
        "Tectonics from map enabled but tectonics.png is missing for profile {}",
        TFCRealWorldConfig.MAP_PROFILE.get()
      );
      return;
    }
    TectonicsRegistry.register(
      generator,
      tectonics,
      TFCRealWorldConfig.VOLCANOES_FROM_MAP.get()
        ? MapHotspotLayout.create(tectonics, levelSeed)
        : null,
      MapRidges.tryLoad(horizontalScale, verticalScale)
    );
    RiftLakesRegistry.register(
      generator,
      MapRiftLakes.create(continent, tectonics)
    );
  }

  /**
   * Vanilla gathers the rivers of a partition from the regions at the corners
   * of the cells around it. Its own rivers grow inland from a region's shores
   * and rarely reach a part of the region that touches none of those corners;
   * map rivers run wherever the map has them, and one in such a part would be
   * cut off along the cell border. Every region that reaches into the cell,
   * or within a river's reach of it, is gathered.
   */
  @Inject(method = "getAllRegionsIn3x3CellArea", at = @At("RETURN"))
  private void tfcrealworld$gatherEveryRegionOfTheCell(
    int cellX,
    int cellZ,
    CallbackInfoReturnable<List<Region>> cir
  ) {
    final RegionGenerator generator = (RegionGenerator) (Object) this;
    if (RiversRegistry.get(generator) == null) {
      return;
    }
    final List<Region> regions = cir.getReturnValue();
    final int minX = Units.cellToGrid(cellX) - REGION_SEARCH_STEP;
    final int minZ = Units.cellToGrid(cellZ) - REGION_SEARCH_STEP;
    final int size = Units.CELL_WIDTH_IN_GRID + 2 * REGION_SEARCH_STEP;
    for (int dz = 0; dz <= size; dz += REGION_SEARCH_STEP) {
      for (int dx = 0; dx <= size; dx += REGION_SEARCH_STEP) {
        final Region region = generator.getOrCreateRegion(minX + dx, minZ + dz);
        if (!regions.contains(region)) {
          regions.add(region);
        }
      }
    }
  }
}
