package net.yazloysasha.tfcrealworld.mixin.world.region;

import java.lang.reflect.Field;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.settings.Settings;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.AltitudeNoiseRegistry;
import net.yazloysasha.tfcrealworld.util.HotspotsNoiseRegistry;
import net.yazloysasha.tfcrealworld.util.SettingsHelper;
import net.yazloysasha.tfcrealworld.world.noise.KoppenBasedRainfallNoise;
import net.yazloysasha.tfcrealworld.world.noise.KoppenBasedRainfallVarianceNoise;
import net.yazloysasha.tfcrealworld.world.noise.KoppenBasedTemperatureNoise;
import net.yazloysasha.tfcrealworld.world.noise.PNGAltitudeNoise;
import net.yazloysasha.tfcrealworld.world.noise.PNGContinentNoise;
import net.yazloysasha.tfcrealworld.world.noise.PNGHotspotsNoise;
import net.yazloysasha.tfcrealworld.world.noise.PNGKoppenNoise;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sun.misc.Unsafe;

@Mixin(RegionGenerator.class)
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
  private net.dries007.tfc.world.Seed seed;

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
  private void realworld$replaceNoises(
    Settings settings,
    net.dries007.tfc.world.Seed seed,
    CallbackInfo ci
  ) {
    RegionGenerator instance = (RegionGenerator) (Object) this;

    try {
      int horizontalWorldScale = SettingsHelper.getHorizontalWorldScale(
        settings
      );
      int verticalWorldScale = SettingsHelper.getVerticalWorldScale(settings);

      initializeContinentMap(
        instance,
        horizontalWorldScale,
        verticalWorldScale
      );

      if (TFCRealWorldConfig.CONTINENT_FROM_MAP.get()) {
        net.yazloysasha.tfcrealworld.world.region.GlobalOceanDistanceCache.initialize(
          horizontalWorldScale,
          verticalWorldScale
        );
        net.yazloysasha.tfcrealworld.world.region.GlobalWestCoastDistanceCache.initialize(
          horizontalWorldScale,
          verticalWorldScale
        );
      }

      initializeAltitudeMap(instance, horizontalWorldScale, verticalWorldScale);
      initializeHotspotsMap(instance, horizontalWorldScale, verticalWorldScale);

      if (TFCRealWorldConfig.KOPPEN_FROM_MAP.get()) {
        initializeKoppenBasedClimateMaps(
          instance,
          seed,
          horizontalWorldScale,
          verticalWorldScale
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
    int horizontalWorldScale,
    int verticalWorldScale
  ) throws NoSuchFieldException {
    if (TFCRealWorldConfig.CONTINENT_FROM_MAP.get()) {
      Field continentField =
        RegionGenerator.class.getDeclaredField("continentNoise");
      @SuppressWarnings("deprecation")
      long offset = UNSAFE.objectFieldOffset(continentField);
      UNSAFE.putObject(
        instance,
        offset,
        new PNGContinentNoise(horizontalWorldScale, verticalWorldScale)
      );
    }
  }

  private void initializeAltitudeMap(
    RegionGenerator instance,
    int horizontalWorldScale,
    int verticalWorldScale
  ) {
    if (TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()) {
      PNGAltitudeNoise altitudeNoise = new PNGAltitudeNoise(
        horizontalWorldScale,
        verticalWorldScale
      );
      AltitudeNoiseRegistry.register(instance, altitudeNoise);
    }
  }

  private void initializeHotspotsMap(
    RegionGenerator instance,
    int horizontalWorldScale,
    int verticalWorldScale
  ) throws NoSuchFieldException {
    if (TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get()) {
      PNGHotspotsNoise hotspotsNoise = new PNGHotspotsNoise(
        horizontalWorldScale,
        verticalWorldScale
      );
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
  }

  private void initializeKoppenBasedClimateMaps(
    RegionGenerator instance,
    net.dries007.tfc.world.Seed seed,
    int horizontalWorldScale,
    int verticalWorldScale
  ) throws NoSuchFieldException {
    PNGKoppenNoise koppenNoise = new PNGKoppenNoise(
      horizontalWorldScale,
      verticalWorldScale
    );

    long koppenSeed = seed.next();
    Field tempField =
      RegionGenerator.class.getDeclaredField("temperatureNoise");
    @SuppressWarnings("deprecation")
    long tempOffset = UNSAFE.objectFieldOffset(tempField);
    UNSAFE.putObject(
      instance,
      tempOffset,
      new KoppenBasedTemperatureNoise(koppenNoise, koppenSeed)
    );

    Field rainfallField =
      RegionGenerator.class.getDeclaredField("rainfallNoise");
    @SuppressWarnings("deprecation")
    long rainfallOffset = UNSAFE.objectFieldOffset(rainfallField);
    UNSAFE.putObject(
      instance,
      rainfallOffset,
      new KoppenBasedRainfallNoise(koppenNoise, koppenSeed)
    );

    Field rainfallVarianceField =
      RegionGenerator.class.getDeclaredField("rainfallVarianceNoise");
    @SuppressWarnings("deprecation")
    long rainVarOffset = UNSAFE.objectFieldOffset(rainfallVarianceField);
    UNSAFE.putObject(
      instance,
      rainVarOffset,
      new KoppenBasedRainfallVarianceNoise(koppenNoise, koppenSeed)
    );
  }
}
