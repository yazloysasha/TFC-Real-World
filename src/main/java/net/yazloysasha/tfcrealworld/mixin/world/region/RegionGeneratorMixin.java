package net.yazloysasha.tfcrealworld.mixin.world.region;

import java.lang.reflect.Field;
import net.dries007.tfc.world.noise.Cellular2D;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;
import net.dries007.tfc.world.region.RegionGenerator;
import net.minecraft.world.level.levelgen.RandomSource;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.helpers.WorldSeedHolder;
import net.yazloysasha.tfcrealworld.util.registry.AltitudeNoiseRegistry;
import net.yazloysasha.tfcrealworld.util.registry.HotspotsNoiseRegistry;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGAltitudeNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGHotspotsNoise;
import net.yazloysasha.tfcrealworld.world.region.cache.GlobalOceanDistanceCache;
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
  private long seed;

  @Shadow
  @Final
  private Cellular2D cellNoise;

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
  private void tfcrealworld$replaceNoises(CallbackInfo ci) {
    RegionGenerator instance = (RegionGenerator) (Object) this;

    WorldSeedHolder.setSeed(seed);

    try {
      int horizontalScale = TFCRealWorldConfig.HORIZONTAL_SCALE.get();
      int verticalScale = TFCRealWorldConfig.VERTICAL_SCALE.get();

      PNGContinentNoise continentNoise = null;
      if (TFCRealWorldConfig.CONTINENT_FROM_MAP.get()) {
        continentNoise = new PNGContinentNoise(horizontalScale, verticalScale);
        initializeContinentMap(instance, continentNoise);

        GlobalOceanDistanceCache.initialize(continentNoise);
      } else {
        initializeContinentNoiseWithContinentalness(
          instance,
          TFCRealWorldConfig.CONTINENTALNESS.get().floatValue() * 10f - 2.5f
        );
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
        HotspotsNoiseRegistry.register(instance, hotspotsNoise);
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
    long offset = UNSAFE.objectFieldOffset(continentField);
    UNSAFE.putObject(instance, offset, continentNoise);
  }

  private void initializeAltitudeMap(
    RegionGenerator instance,
    PNGAltitudeNoise altitudeNoise
  ) {
    AltitudeNoiseRegistry.register(instance, altitudeNoise);
  }

  private void initializeContinentNoiseWithContinentalness(
    RegionGenerator instance,
    float min
  ) throws NoSuchFieldException {
    RandomSource random = new XoroshiroRandomSource(seed);
    Noise2D newContinentNoise = cellNoise
      .then(c -> 1 - c.f1() / (0.37f + c.f2()))
      .lazyProduct(
        new OpenSimplex2D(random.nextLong())
          .spread(0.24f)
          .scaled(min, 8.7f)
          .octaves(4)
      );

    Field continentField =
      RegionGenerator.class.getDeclaredField("continentNoise");
    long offset = UNSAFE.objectFieldOffset(continentField);
    UNSAFE.putObject(instance, offset, newContinentNoise);
  }
}
