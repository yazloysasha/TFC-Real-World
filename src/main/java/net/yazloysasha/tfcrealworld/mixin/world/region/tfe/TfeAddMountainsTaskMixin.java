package net.yazloysasha.tfcrealworld.mixin.world.region.tfe;

import com.newterraearth.tfe.world.region.NTEPointAccess;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.lang.reflect.Method;
import net.dries007.tfc.world.region.AddMountains;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.minecraft.util.RandomSource;
import net.yazloysasha.tfcrealworld.compat.tfe.TfeBindings;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.backport.TfeOceanDepth;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGAltitudeNoise;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AddMountains.class, remap = false, priority = 1500)
public class TfeAddMountainsTaskMixin {

  @Unique
  private static final int MIN_ISLE_CHAIN_SIZE = 45;

  @Unique
  private static final int MAP_ISLE_ATTEMPTS = 80;

  @Unique
  private static final int MAX_MAP_ISLE_CHAINS = 12;

  @Inject(method = "apply", at = @At("HEAD"))
  private void tfcrealworld$applyMapOceanDepthBeforeBarrierIslands(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    TfeOceanDepth.prepareOceanForBarrierIslands(
      context.region,
      context.generator()
    );
  }

  @Redirect(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/region/Region$Point;setMountain()V"
    )
  )
  private void tfcrealworld$tfeSkipProceduralMountainsWhenUsingAltitudeMap(
    Region.Point point
  ) {
    if (!TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()) {
      point.setMountain();
    }
  }

  @Inject(method = "tfe$placeRange", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$tfeSkipProceduralMountainRangesWhenUsingAltitudeMap(
    Region region,
    RandomSource random,
    int originIndex,
    CallbackInfoReturnable<IntSet> cir
  ) {
    if (TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()) {
      cir.setReturnValue(new IntOpenHashSet());
    }
  }

  @Inject(method = "apply", at = @At("TAIL"))
  private void tfcrealworld$placeMapShelfReefChains(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    if (!TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()) {
      return;
    }

    final Region region = context.region;
    final RandomSource random = context.random;
    final Region.Point[] data = region.data();

    int islesPlaced = 0;
    for (
      int attempt = 0;
      attempt < MAP_ISLE_ATTEMPTS && islesPlaced < MAX_MAP_ISLE_CHAINS;
      attempt++
    ) {
      final int x = region.minX() + random.nextInt(region.sizeX());
      final int z = region.minZ() + random.nextInt(region.sizeZ());
      @Nullable
      final Region.Point origin = region.maybeAt(x, z);
      if (origin == null || origin.land()) {
        continue;
      }
      final NTEPointAccess originAccess = (NTEPointAccess) origin;
      if (
        originAccess.nte$getOceanDepth() != PNGAltitudeNoise.SHELF_OCEAN_DEPTH
      ) {
        continue;
      }

      final int originIndex = region.index(x, z);
      if (
        originAccess.nte$getDistanceToDeepOcean() <= 3 &&
        originAccess.nte$getDistanceToLand() > 2
      ) {
        final IntSet range = tfcrealworld$invokeTfeFlood(
          this,
          TfeBindings.PLACE_VOLCANIC_ARC,
          region,
          random,
          originIndex
        );
        if (range.size() > MIN_ISLE_CHAIN_SIZE) {
          range.forEach(index -> {
            final Region.Point point = data[index];
            if (point == null) {
              return;
            }
            final NTEPointAccess access = (NTEPointAccess) point;
            access.nte$setVolcanic(true);
            access.nte$setOceanDepth(PNGAltitudeNoise.REEF_OCEAN_DEPTH);
          });
          islesPlaced += 2;
          continue;
        }
      }

      if (
        originAccess.nte$getDistanceToLand() > 2 &&
        originAccess.nte$getDistanceToLand() < 6
      ) {
        final IntSet range = tfcrealworld$invokeTfeFlood(
          this,
          TfeBindings.PLACE_BARRIER,
          region,
          random,
          originIndex
        );
        if (range.size() > MIN_ISLE_CHAIN_SIZE) {
          range.forEach(index -> {
            final Region.Point point = data[index];
            if (point == null) {
              return;
            }
            ((NTEPointAccess) point).nte$setOceanDepth(
                PNGAltitudeNoise.REEF_OCEAN_DEPTH
              );
          });
          islesPlaced++;
        }
      }
    }
  }

  @Unique
  private static IntSet tfcrealworld$invokeTfeFlood(
    Object instance,
    String name,
    Region region,
    RandomSource random,
    int originIndex
  ) {
    try {
      final Method method = instance
        .getClass()
        .getDeclaredMethod(name, Region.class, RandomSource.class, int.class);
      method.setAccessible(true);
      return (IntSet) method.invoke(instance, region, random, originIndex);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(
        "TFE Unique " + name + " is missing on AddMountains",
        e
      );
    }
  }
}
