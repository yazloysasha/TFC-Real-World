package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.Seed;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.Units;
import net.dries007.tfc.world.settings.Settings;
import net.minecraft.util.Mth;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.helpers.WorldSeedHolder;
import net.yazloysasha.tfcrealworld.util.registry.ContinentNoiseRegistry;
import net.yazloysasha.tfcrealworld.util.registry.RiftLakesRegistry;
import net.yazloysasha.tfcrealworld.util.registry.RiversRegistry;
import net.yazloysasha.tfcrealworld.util.registry.TectonicsRegistry;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGRainVarianceNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGRainfallNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGTemperatureNoise;
import net.yazloysasha.tfcrealworld.world.region.cache.GlobalOceanDistanceCache;
import net.yazloysasha.tfcrealworld.world.region.cache.GlobalWestCoastDistanceCache;
import net.yazloysasha.tfcrealworld.world.river.MapRivers;
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
@Mixin(value = RegionGenerator.class, remap = false)
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

  @Shadow
  @Final
  @Mutable
  public Noise2D rainfallVarianceNoise;

  @Shadow
  @Final
  private Settings settings;

  /** Map edges: how far past the scale the continents fade out. */
  @Unique
  private static final float FINITE_CONTINENTS_FADE_FROM_MAP = 1.01f;

  @Unique
  private static final float FINITE_CONTINENTS_FADE = 1.2f;

  @Inject(method = "<init>", at = @At("TAIL"))
  private void tfcrealworld$replaceNoises(
    Settings settings,
    Seed seed,
    CallbackInfo ci
  ) {
    final RegionGenerator generator = (RegionGenerator) (Object) this;
    WorldSeedHolder.setSeed(seed.seed());
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
      TectonicsRegistry.clearHotspotLayout();
      if (TFCRealWorldConfig.TECTONICS_FROM_MAP.get()) {
        tfcrealworld$registerTectonics(
          generator,
          continent,
          seed,
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
      rainfallVarianceNoise = new PNGRainVarianceNoise(
        horizontalScale,
        verticalScale
      );
    }
  }

  @Unique
  private static void tfcrealworld$registerTectonics(
    RegionGenerator generator,
    PNGContinentNoise continent,
    Seed seed,
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
      MapHotspotLayout.create(tectonics, seed.seed())
    );
    RiftLakesRegistry.register(
      generator,
      MapRiftLakes.create(continent, tectonics)
    );
  }

  @Inject(
    method = "continentFactor(Lnet/dries007/tfc/world/region/Region$Point;)F",
    at = @At("HEAD"),
    cancellable = true
  )
  private void tfcrealworld$continentFactorFromMap(
    Region.Point point,
    CallbackInfoReturnable<Float> cir
  ) {
    if (!settings.finiteContinents()) {
      return;
    }
    final int scaleX = TFCRealWorldConfig.HORIZONTAL_SCALE.get();
    final int scaleZ = TFCRealWorldConfig.VERTICAL_SCALE.get();
    final float fade = TFCRealWorldConfig.CONTINENT_FROM_MAP.get()
      ? FINITE_CONTINENTS_FADE_FROM_MAP
      : FINITE_CONTINENTS_FADE;
    cir.setReturnValue(
      Math.min(
        tfcrealworld$edgeFactor(Units.gridToBlock(point.x), scaleX, fade),
        tfcrealworld$edgeFactor(Units.gridToBlock(point.z), scaleZ, fade)
      )
    );
  }

  @Unique
  private static float tfcrealworld$edgeFactor(
    float block,
    int scale,
    float fade
  ) {
    return scale == 0
      ? 1f
      : Mth.clampedMap(Math.abs(block), scale, fade * scale, 1, 0);
  }
}
