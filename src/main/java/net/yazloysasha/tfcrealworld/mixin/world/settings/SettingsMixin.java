package net.yazloysasha.tfcrealworld.mixin.world.settings;

import java.util.Random;
import net.dries007.tfc.world.settings.Settings;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.WorldSeedHolder;
import net.yazloysasha.tfcrealworld.util.projection.ProjectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Settings.class)
public class SettingsMixin {

  @Inject(
    method = "spawnDistance",
    at = @At("RETURN"),
    cancellable = true,
    remap = false
  )
  private void tfcrealworld$overrideSpawnDistance(
    CallbackInfoReturnable<Integer> cir
  ) {
    cir.setReturnValue(TFCRealWorldConfig.SPAWN_DISTANCE.get());
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
    TFCRealWorldConfig.SpawnMode spawnMode =
      TFCRealWorldConfig.SPAWN_MODE.get();

    if (spawnMode == TFCRealWorldConfig.SpawnMode.GEOGRAPHIC) {
      int x = ProjectionManager.geographicToMinecraftX(
        TFCRealWorldConfig.SPAWN_CENTER_LONGITUDE.get(),
        TFCRealWorldConfig.SOUTH_EDGE_LATITUDE.get(),
        TFCRealWorldConfig.HORIZONTAL_TILE_SIZE.get(),
        TFCRealWorldConfig.VERTICAL_TILE_SIZE.get(),
        TFCRealWorldConfig.WEST_EDGE_LONGITUDE.get(),
        TFCRealWorldConfig.EAST_EDGE_LONGITUDE.get(),
        TFCRealWorldConfig.SOUTH_EDGE_LATITUDE.get(),
        TFCRealWorldConfig.NORTH_EDGE_LATITUDE.get(),
        TFCRealWorldConfig.MAP_PROJECTION.get()
      );
      cir.setReturnValue(x);
    } else if (spawnMode == TFCRealWorldConfig.SpawnMode.RANDOM) {
      cir.setReturnValue(getRandomSpawnX());
    } else {
      cir.setReturnValue(TFCRealWorldConfig.SPAWN_CENTER_X.get());
    }
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
    TFCRealWorldConfig.SpawnMode spawnMode =
      TFCRealWorldConfig.SPAWN_MODE.get();

    if (spawnMode == TFCRealWorldConfig.SpawnMode.GEOGRAPHIC) {
      int z = ProjectionManager.geographicToMinecraftZ(
        TFCRealWorldConfig.SPAWN_CENTER_LONGITUDE.get(),
        TFCRealWorldConfig.SOUTH_EDGE_LATITUDE.get(),
        TFCRealWorldConfig.HORIZONTAL_TILE_SIZE.get(),
        TFCRealWorldConfig.VERTICAL_TILE_SIZE.get(),
        TFCRealWorldConfig.WEST_EDGE_LONGITUDE.get(),
        TFCRealWorldConfig.EAST_EDGE_LONGITUDE.get(),
        TFCRealWorldConfig.SOUTH_EDGE_LATITUDE.get(),
        TFCRealWorldConfig.NORTH_EDGE_LATITUDE.get(),
        TFCRealWorldConfig.MAP_PROJECTION.get()
      );
      cir.setReturnValue(z);
    } else if (spawnMode == TFCRealWorldConfig.SpawnMode.RANDOM) {
      cir.setReturnValue(getRandomSpawnZ());
    } else {
      cir.setReturnValue(TFCRealWorldConfig.SPAWN_CENTER_Z.get());
    }
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

  /**
   * TODO: Сделать так, чтобы случайные координаты X/Z были различными
   * Также нужно брать только те регионы, в которых есть суша (включая континенты и острова)
   */

  private static int getRandomSpawnX() {
    long worldSeed = WorldSeedHolder.getSeed();
    Random random = new Random(worldSeed);

    int horizontalTileSize = TFCRealWorldConfig.HORIZONTAL_TILE_SIZE.get();
    int spawnDistance = TFCRealWorldConfig.SPAWN_DISTANCE.get();

    int maxX = horizontalTileSize / 2 - spawnDistance;
    int minX = -maxX;

    return random.nextInt(maxX - minX + 1) + minX;
  }

  private static int getRandomSpawnZ() {
    long worldSeed = WorldSeedHolder.getSeed();
    Random random = new Random(worldSeed);

    int verticalTileSize = TFCRealWorldConfig.VERTICAL_TILE_SIZE.get();
    int spawnDistance = TFCRealWorldConfig.SPAWN_DISTANCE.get();

    int maxZ = verticalTileSize / 2 - spawnDistance;
    int minZ = -maxZ;

    return random.nextInt(maxZ - minZ + 1) + minZ;
  }
}
