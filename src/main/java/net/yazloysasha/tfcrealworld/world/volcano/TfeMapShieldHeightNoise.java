package net.yazloysasha.tfcrealworld.world.volcano;

import com.newterraearth.tfe.world.NTEBiomeNoise;
import java.lang.reflect.Method;
import net.dries007.tfc.world.BiomeNoiseSampler;
import net.dries007.tfc.world.noise.Noise2D;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import org.jetbrains.annotations.Nullable;

public final class TfeMapShieldHeightNoise {

  private static final Method ACTIVE_SHAPE = shapeMethod(
    "activeShieldVolcano",
    long.class,
    Noise2D.class
  );
  private static final Method DORMANT_SHAPE = shapeMethod(
    "dormantShieldVolcano",
    long.class,
    Noise2D.class
  );
  private static final Method EXTINCT_SHAPE = shapeMethod(
    "extinctShieldVolcano",
    long.class,
    Noise2D.class
  );
  private static final Method ANCIENT_SHAPE = shapeMethod(
    "ancientShieldVolcano",
    long.class,
    double.class,
    double.class,
    Noise2D.class
  );
  private static final Method SUNKEN_SHAPE = shapeMethod(
    "sunkenShieldVolcano",
    long.class,
    Noise2D.class
  );
  private static final Method GLACIATED_SHAPE = shapeMethod(
    "glaciatedShieldVolcano",
    long.class,
    Noise2D.class
  );
  private static final Method ICE_SHEET_SURFACE_SHAPE = shapeMethod(
    "shieldVolcanoIceSheetSurface",
    long.class,
    Noise2D.class
  );
  private static final Method GLACIER_SURFACE_SHAPE = shapeMethod(
    "shieldVolcanoGlacierSurface",
    long.class,
    Noise2D.class
  );
  private static final Method GLACIAL_TEXTURE = shapeMethod(
    "glacialSurfaceTexture",
    long.class
  );

  private TfeMapShieldHeightNoise() {}

  @Nullable
  public static BiomeNoiseSampler createSampler(String biomePath, long seed) {
    if (!TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get()) {
      return null;
    }
    final Noise2D height = heightNoise(biomePath, seed);
    if (height == null) {
      return null;
    }
    return BiomeNoiseSampler.fromHeightNoise(height);
  }

  @Nullable
  private static Noise2D heightNoise(String path, long seed) {
    return switch (path) {
      case "active_shield_volcano" -> invokeShape(
        ACTIVE_SHAPE,
        seed,
        MapHotspotNoise.forAge((byte) 1, seed)
      );
      case "dormant_shield_volcano" -> invokeShape(
        DORMANT_SHAPE,
        seed,
        MapHotspotNoise.forAge((byte) 2, seed)
      );
      case "extinct_shield_volcano" -> invokeShape(
        EXTINCT_SHAPE,
        seed,
        MapHotspotNoise.forAge((byte) 3, seed)
      );
      case "ancient_shield_volcano" -> invokeShape(
        ANCIENT_SHAPE,
        seed,
        90.0,
        130.0,
        MapHotspotNoise.forAge((byte) 4, seed)
      );
      case "sunken_shield_volcano" -> invokeShape(
        SUNKEN_SHAPE,
        seed,
        MapHotspotNoise.forAge((byte) 4, seed)
      );
      case "glaciated_shield_volcano" -> glacialTerrain(
        seed,
        GLACIER_SURFACE_SHAPE
      );
      case "ice_sheet_shield_volcano" -> glacialTerrain(
        seed,
        ICE_SHEET_SURFACE_SHAPE
      );
      case "shield_volcano_shore", "old_shield_volcano_shore" -> null;
      default -> null;
    };
  }

  /**
   * TFC 4.2.10 / TFE {@code TFCBiomes}: {@code glaciatedShieldVolcano.max(
   * iceOrGlacierSurface.add(glacialSurfaceTexture))}.
   */
  private static Noise2D glacialTerrain(long seed, Method iceOrGlacierSurface) {
    final Noise2D intensity = MapHotspotNoise.combined(seed);
    final Noise2D rock = invokeShape(GLACIATED_SHAPE, seed, intensity);
    final Noise2D ice = invokeShape(iceOrGlacierSurface, seed, intensity);
    final Noise2D texture = invokeTexture(seed);
    return (x, z) ->
      Math.max(rock.noise(x, z), ice.noise(x, z) + texture.noise(x, z));
  }

  private static Noise2D invokeTexture(long seed) {
    try {
      return (Noise2D) GLACIAL_TEXTURE.invoke(null, seed);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(
        "Failed to build glacialSurfaceTexture",
        e
      );
    }
  }

  private static Method shapeMethod(String name, Class<?>... params) {
    try {
      final Method method = NTEBiomeNoise.class.getDeclaredMethod(name, params);
      method.setAccessible(true);
      return method;
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(
        "TFE NTEBiomeNoise." +
        name +
        " missing; update TfeMapShieldHeightNoise",
        e
      );
    }
  }

  private static Noise2D invokeShape(
    Method method,
    long seed,
    Noise2D intensity
  ) {
    try {
      return (Noise2D) method.invoke(null, seed, intensity);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(
        "Failed to build map shield height via " + method.getName(),
        e
      );
    }
  }

  private static Noise2D invokeShape(
    Method method,
    long seed,
    double min,
    double max,
    Noise2D intensity
  ) {
    try {
      return (Noise2D) method.invoke(null, seed, min, max, intensity);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(
        "Failed to build map shield height via " + method.getName(),
        e
      );
    }
  }
}
