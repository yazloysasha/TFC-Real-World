package net.yazloysasha.tfcrealworld.mixin.world.settings;

import java.util.Random;
import net.dries007.tfc.world.settings.Settings;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.types.CachedSpawnCenter;
import net.yazloysasha.tfcrealworld.util.WorldSeedHolder;
import net.yazloysasha.tfcrealworld.util.projection.ProjectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Settings.class)
public class SettingsMixin {

  private static final ThreadLocal<CachedSpawnCenter> SPAWN_CENTER_CACHE =
    new ThreadLocal<>();

  @Inject(
    method = "spawnDistance",
    at = @At("RETURN"),
    cancellable = true,
    remap = false
  )
  private void tfcrealworld$overrideSpawnDistance(
    CallbackInfoReturnable<Integer> cir
  ) {
    TFCRealWorldConfig.SpawnMode mode = TFCRealWorldConfig.SPAWN_MODE.get();
    int value = TFCRealWorldConfig.SPAWN_DISTANCE.get();

    if (mode == TFCRealWorldConfig.SpawnMode.RANDOM) {
      int halfX = TFCRealWorldConfig.HORIZONTAL_TILE_SIZE.get() / 2;
      int halfZ = TFCRealWorldConfig.VERTICAL_TILE_SIZE.get() / 2;
      value = Math.min(halfX, halfZ);
    }

    cir.setReturnValue(value);
  }

  @Inject(
    method = "spawnCenterX",
    at = @At("RETURN"),
    cancellable = true,
    remap = false
  )
  private void tfcrealworld$overrideSpawnCenterX(
    CallbackInfoReturnable<Integer> cir
  ) {
    int[] spawnCenter = getSpawnCenter();
    cir.setReturnValue(spawnCenter[0]);
  }

  @Inject(
    method = "spawnCenterZ",
    at = @At("RETURN"),
    cancellable = true,
    remap = false
  )
  private void tfcrealworld$overrideSpawnCenterZ(
    CallbackInfoReturnable<Integer> cir
  ) {
    int[] spawnCenter = getSpawnCenter();
    cir.setReturnValue(spawnCenter[1]);
  }

  private static int[] getSpawnCenter() {
    TFCRealWorldConfig.SpawnMode mode = TFCRealWorldConfig.SPAWN_MODE.get();

    long seed = mode == TFCRealWorldConfig.SpawnMode.RANDOM
      ? WorldSeedHolder.getSeed()
      : 0L;
    CachedSpawnCenter cachedSpawnCenter = SPAWN_CENTER_CACHE.get();
    if (
      cachedSpawnCenter != null &&
      cachedSpawnCenter.mode() == mode &&
      (mode != TFCRealWorldConfig.SpawnMode.RANDOM ||
        cachedSpawnCenter.seed() == seed)
    ) {
      return cachedSpawnCenter.coords();
    }

    int[] result;

    if (mode == TFCRealWorldConfig.SpawnMode.GEOGRAPHIC) {
      result = ProjectionManager.geographicToMinecraft(
        TFCRealWorldConfig.SPAWN_CENTER_LONGITUDE.get(),
        TFCRealWorldConfig.SPAWN_CENTER_LATITUDE.get(),
        TFCRealWorldConfig.HORIZONTAL_TILE_SIZE.get(),
        TFCRealWorldConfig.VERTICAL_TILE_SIZE.get(),
        TFCRealWorldConfig.WEST_EDGE_LONGITUDE.get(),
        TFCRealWorldConfig.EAST_EDGE_LONGITUDE.get(),
        TFCRealWorldConfig.SOUTH_EDGE_LATITUDE.get(),
        TFCRealWorldConfig.NORTH_EDGE_LATITUDE.get(),
        TFCRealWorldConfig.MAP_PROJECTION.get()
      );
    } else if (mode == TFCRealWorldConfig.SpawnMode.RANDOM) {
      long worldSeed = seed;
      Random rng = new Random(worldSeed ^ 0x1234ABCDL);

      int halfX = TFCRealWorldConfig.HORIZONTAL_TILE_SIZE.get() / 2;
      int halfZ = TFCRealWorldConfig.VERTICAL_TILE_SIZE.get() / 2;

      // For RANDOM mode, SPAWN_DISTANCE is a square movement radius
      int spawnDistance = Math.min(halfX, halfZ);

      // Ensure SPAWN_CENTER ± SPAWN_DISTANCE stays within half of the tile size:
      // |center| + spawnDistance <= halfSize on each axis
      int maxCenterX = Math.max(0, halfX - spawnDistance);
      int maxCenterZ = Math.max(0, halfZ - spawnDistance);

      int x = maxCenterX == 0
        ? 0
        : rng.nextInt(maxCenterX * 2 + 1) - maxCenterX;
      int z = maxCenterZ == 0
        ? 0
        : rng.nextInt(maxCenterZ * 2 + 1) - maxCenterZ;

      result = new int[] { x, z };
    } else {
      result = new int[] {
        TFCRealWorldConfig.SPAWN_CENTER_X.get(),
        TFCRealWorldConfig.SPAWN_CENTER_Z.get(),
      };
    }

    SPAWN_CENTER_CACHE.set(new CachedSpawnCenter(mode, seed, result));

    return result;
  }

  @Inject(
    method = "flatBedrock",
    at = @At("RETURN"),
    cancellable = true,
    remap = false
  )
  private void tfcrealworld$overrideFlatBedrock(
    CallbackInfoReturnable<Boolean> cir
  ) {
    cir.setReturnValue(TFCRealWorldConfig.FLAT_BEDROCK.get());
  }

  @Inject(
    method = "finiteContinents",
    at = @At("RETURN"),
    cancellable = true,
    remap = false
  )
  private void tfcrealworld$overrideFiniteContinents(
    CallbackInfoReturnable<Boolean> cir
  ) {
    cir.setReturnValue(TFCRealWorldConfig.FINITE_CONTINENTS.get());
  }

  @Inject(
    method = "continentalness",
    at = @At("RETURN"),
    cancellable = true,
    remap = false
  )
  private void tfcrealworld$overrideContinentalness(
    CallbackInfoReturnable<Float> cir
  ) {
    cir.setReturnValue(TFCRealWorldConfig.CONTINENTALNESS.get().floatValue());
  }

  @Inject(
    method = "grassDensity",
    at = @At("RETURN"),
    cancellable = true,
    remap = false
  )
  private void tfcrealworld$overrideGrassDensity(
    CallbackInfoReturnable<Float> cir
  ) {
    cir.setReturnValue(TFCRealWorldConfig.GRASS_DENSITY.get().floatValue());
  }

  @Inject(
    method = "temperatureScale",
    at = @At("RETURN"),
    cancellable = true,
    remap = false
  )
  private void tfcrealworld$overrideTemperatureScale(
    CallbackInfoReturnable<Integer> cir
  ) {
    cir.setReturnValue(TFCRealWorldConfig.TEMPERATURE_SCALE.get());
  }

  @Inject(
    method = "rainfallScale",
    at = @At("RETURN"),
    cancellable = true,
    remap = false
  )
  private void tfcrealworld$overrideRainfallScale(
    CallbackInfoReturnable<Integer> cir
  ) {
    cir.setReturnValue(TFCRealWorldConfig.RAINFALL_SCALE.get());
  }
}
