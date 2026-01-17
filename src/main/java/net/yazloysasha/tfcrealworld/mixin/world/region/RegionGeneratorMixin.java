package net.yazloysasha.tfcrealworld.mixin.world.region;

import java.lang.reflect.Field;
import net.dries007.tfc.world.Seed;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.settings.Settings;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.helpers.WorldSeedHolder;
import net.yazloysasha.tfcrealworld.util.registry.AltitudeNoiseRegistry;
import net.yazloysasha.tfcrealworld.util.registry.HotspotsNoiseRegistry;
import net.yazloysasha.tfcrealworld.world.noise.koppen.KoppenBasedRainfallNoise;
import net.yazloysasha.tfcrealworld.world.noise.koppen.KoppenBasedRainfallVarianceNoise;
import net.yazloysasha.tfcrealworld.world.noise.koppen.KoppenBasedTemperatureNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGAltitudeNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGHotspotsNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGKoppenNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGRainfallNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGTemperatureNoise;
import net.yazloysasha.tfcrealworld.world.region.cache.GlobalOceanDistanceCache;
import net.yazloysasha.tfcrealworld.world.region.cache.GlobalWestCoastDistanceCache;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sun.misc.Unsafe;

@Mixin(value = RegionGenerator.class, remap = false)
public class RegionGeneratorMixin {

  @Shadow
  @Final
  public Noise2D continentNoise;

  @Shadow
  @Final
  public Noise2D temperatureNoise;

  @Shadow
  @Final
  public Noise2D rainfallNoise;

  @Shadow
  @Final
  private Settings settings;

  @Shadow
  @Final
  private Seed seed;

  private static final Unsafe UNSAFE;

  static {
    try {
      Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
      unsafeField.setAccessible(true);
      UNSAFE = (Unsafe) unsafeField.get(null);
    } catch (Exception e) {
      throw new RuntimeException("Failed to get Unsafe instance", e);
    }
  }

  @Inject(method = "<init>", at = @At("TAIL"))
  private void tfcrealworld$replaceNoises(
    Settings settings,
    Seed seed,
    CallbackInfo ci
  ) {
    RegionGenerator instance = (RegionGenerator) (Object) this;

    WorldSeedHolder.setSeed(seed.seed());

    try {
      int horizontalTileSize = TFCRealWorldConfig.HORIZONTAL_TILE_SIZE.get();
      int verticalTileSize = TFCRealWorldConfig.VERTICAL_TILE_SIZE.get();

      PNGContinentNoise continentNoise = null;
      if (TFCRealWorldConfig.CONTINENT_FROM_MAP.get()) {
        continentNoise = new PNGContinentNoise(
          horizontalTileSize,
          verticalTileSize
        );
        initializeContinentMap(instance, continentNoise);

        GlobalOceanDistanceCache.initialize(continentNoise);
        GlobalWestCoastDistanceCache.initialize(continentNoise);
      }

      if (TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()) {
        PNGAltitudeNoise altitudeNoise = new PNGAltitudeNoise(
          horizontalTileSize,
          verticalTileSize
        );
        initializeAltitudeMap(instance, altitudeNoise);
      }

      if (TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get()) {
        PNGHotspotsNoise hotspotsNoise = new PNGHotspotsNoise(
          horizontalTileSize,
          verticalTileSize
        );
        initializeHotspotsMap(instance, hotspotsNoise);
      }

      if (TFCRealWorldConfig.KOPPEN_FROM_MAP.get()) {
        initializeKoppenBasedClimateMaps(
          instance,
          seed,
          horizontalTileSize,
          verticalTileSize
        );
      }
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(
        "Failed to find required field in RegionGenerator. This should not happen.",
        e
      );
    }
  }

  private void initializeContinentMap(
    RegionGenerator instance,
    PNGContinentNoise continentNoise
  ) throws NoSuchFieldException {
    Field continentField =
      RegionGenerator.class.getDeclaredField("continentNoise");
    @SuppressWarnings("deprecation")
    long offset = UNSAFE.objectFieldOffset(continentField);
    UNSAFE.putObject(instance, offset, continentNoise);
  }

  private void initializeAltitudeMap(
    RegionGenerator instance,
    PNGAltitudeNoise altitudeNoise
  ) {
    AltitudeNoiseRegistry.register(instance, altitudeNoise);
  }

  private void initializeHotspotsMap(
    RegionGenerator instance,
    PNGHotspotsNoise hotspotsNoise
  ) throws NoSuchFieldException {
    Field hotspotIntensityField =
      RegionGenerator.class.getDeclaredField("hotSpotIntensityNoise");
    @SuppressWarnings("deprecation")
    long intensityOffset = UNSAFE.objectFieldOffset(hotspotIntensityField);
    UNSAFE.putObject(instance, intensityOffset, hotspotsNoise);
    Field hotspotAgeField =
      RegionGenerator.class.getDeclaredField("hotSpotAgeNoise");
    @SuppressWarnings("deprecation")
    long ageOffset = UNSAFE.objectFieldOffset(hotspotAgeField);
    Noise2D ageNoise = new Noise2D() {
      @Override
      public double noise(double x, double z) {
        return hotspotsNoise.getHotSpotAge(x, z);
      }
    };
    UNSAFE.putObject(instance, ageOffset, ageNoise);
    HotspotsNoiseRegistry.register(instance, hotspotsNoise);
  }

  private void initializeKoppenBasedClimateMaps(
    RegionGenerator instance,
    Seed seed,
    int horizontalTileSize,
    int verticalTileSize
  ) throws NoSuchFieldException {
    PNGKoppenNoise koppenNoise = new PNGKoppenNoise(
      horizontalTileSize,
      verticalTileSize
    );

    PNGTemperatureNoise temperatureNoise = new PNGTemperatureNoise(
      horizontalTileSize,
      verticalTileSize
    );
    PNGRainfallNoise rainfallNoise = new PNGRainfallNoise(
      horizontalTileSize,
      verticalTileSize
    );

    long koppenSeed = seed.next();
    Field tempField =
      RegionGenerator.class.getDeclaredField("temperatureNoise");
    @SuppressWarnings("deprecation")
    long tempOffset = UNSAFE.objectFieldOffset(tempField);
    UNSAFE.putObject(
      instance,
      tempOffset,
      new KoppenBasedTemperatureNoise(
        koppenNoise,
        temperatureNoise,
        rainfallNoise,
        koppenSeed
      )
    );

    Field rainfallField =
      RegionGenerator.class.getDeclaredField("rainfallNoise");
    @SuppressWarnings("deprecation")
    long rainfallOffset = UNSAFE.objectFieldOffset(rainfallField);
    UNSAFE.putObject(
      instance,
      rainfallOffset,
      new KoppenBasedRainfallNoise(
        koppenNoise,
        temperatureNoise,
        rainfallNoise,
        koppenSeed
      )
    );

    Field rainfallVarianceField =
      RegionGenerator.class.getDeclaredField("rainfallVarianceNoise");
    @SuppressWarnings("deprecation")
    long rainVarOffset = UNSAFE.objectFieldOffset(rainfallVarianceField);
    UNSAFE.putObject(
      instance,
      rainVarOffset,
      new KoppenBasedRainfallVarianceNoise(
        koppenNoise,
        temperatureNoise,
        rainfallNoise,
        koppenSeed
      )
    );
  }
}
