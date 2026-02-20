package net.yazloysasha.tfcrealworld.mixin.world.region;

import java.lang.reflect.Field;
import net.dries007.tfc.world.Seed;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.Units;
import net.dries007.tfc.world.settings.Settings;
import net.minecraft.util.Mth;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
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
      int horizontalScale = TFCRealWorldConfig.HORIZONTAL_SCALE.get();
      int verticalScale = TFCRealWorldConfig.VERTICAL_SCALE.get();

      PNGContinentNoise continentNoise = null;
      if (TFCRealWorldConfig.CONTINENT_FROM_MAP.get()) {
        continentNoise = new PNGContinentNoise(horizontalScale, verticalScale);
        initializeContinentMap(instance, continentNoise);

        GlobalOceanDistanceCache.initialize(continentNoise);
        GlobalWestCoastDistanceCache.initialize(continentNoise);
      }

      if (TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()) {
        PNGAltitudeNoise altitudeNoise = new PNGAltitudeNoise(
          horizontalScale,
          verticalScale
        );
        initializeAltitudeMap(instance, altitudeNoise);
      }

      if (TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get()) {
        PNGHotspotsNoise hotspotsNoise = new PNGHotspotsNoise(
          horizontalScale,
          verticalScale
        );
        initializeHotspotsMap(instance, hotspotsNoise);
      }

      if (TFCRealWorldConfig.KOPPEN_FROM_MAP.get()) {
        initializeKoppenBasedClimateMaps(
          instance,
          seed,
          horizontalScale,
          verticalScale
        );
      }
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(
        "Failed to find required field in RegionGenerator. This should not happen.",
        e
      );
    }
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
    if (settings.finiteContinents()) {
      int scaleX = TFCRealWorldConfig.HORIZONTAL_SCALE.get();
      int scaleZ = TFCRealWorldConfig.VERTICAL_SCALE.get();
      float blockX = Units.gridToBlock(point.x);
      float blockZ = Units.gridToBlock(point.z);
      float multiplier = TFCRealWorldConfig.CONTINENT_FROM_MAP.get()
        ? 1.01f
        : 1.2f;
      float factorX = scaleX == 0
        ? 1f
        : Mth.clampedMap(Math.abs(blockX), scaleX, multiplier * scaleX, 1, 0);
      float factorZ = scaleZ == 0
        ? 1f
        : Mth.clampedMap(Math.abs(blockZ), scaleZ, multiplier * scaleZ, 1, 0);
      cir.setReturnValue(Math.min(factorX, factorZ));
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
    int horizontalScale,
    int verticalScale
  ) throws NoSuchFieldException {
    PNGKoppenNoise koppenNoise = new PNGKoppenNoise(
      horizontalScale,
      verticalScale
    );

    PNGTemperatureNoise temperatureNoise = new PNGTemperatureNoise(
      horizontalScale,
      verticalScale
    );
    PNGRainfallNoise rainfallNoise = new PNGRainfallNoise(
      horizontalScale,
      verticalScale
    );

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
        rainfallNoise
      )
    );

    Field rainfallField =
      RegionGenerator.class.getDeclaredField("rainfallNoise");
    @SuppressWarnings("deprecation")
    long rainfallOffset = UNSAFE.objectFieldOffset(rainfallField);
    UNSAFE.putObject(
      instance,
      rainfallOffset,
      new KoppenBasedRainfallNoise(koppenNoise, temperatureNoise, rainfallNoise)
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
        rainfallNoise
      )
    );
  }
}
