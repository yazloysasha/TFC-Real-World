package net.yazloysasha.tfcrealworld.mixin.world.region;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.dries007.tfc.world.region.AddMountainsAndBarrierIslands;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.minecraft.util.RandomSource;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGAltitudeNoise;
import net.yazloysasha.tfcrealworld.world.region.calculator.AltitudeCalculator;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AddMountainsAndBarrierIslands.class, remap = false)
public class AddMountainsAndBarrierIslandsMixin {

  private static final int MIN_ISLE_CHAIN_SIZE = 45;
  private static final int MAP_ISLE_ATTEMPTS = 80;
  private static final int MAX_MAP_ISLE_CHAINS = 12;

  @Inject(method = "apply", at = @At("HEAD"))
  private void tfcrealworld$applyMapOceanDepthBeforeBarrierIslands(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    new AltitudeCalculator()
      .prepareOceanForBarrierIslands(context.region, context.generator());
  }

  @Redirect(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/region/AddMountainsAndBarrierIslands;placeRange(Lnet/dries007/tfc/world/region/Region;Lnet/minecraft/util/RandomSource;I)Lit/unimi/dsi/fastutil/ints/IntSet;"
    )
  )
  private IntSet tfcrealworld$skipProceduralMountainRangesWhenUsingAltitudeMap(
    AddMountainsAndBarrierIslands instance,
    Region region,
    RandomSource random,
    int originIndex
  ) {
    if (TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()) {
      return new IntOpenHashSet();
    }
    return (
      (AddMountainsAndBarrierIslandsAccessor) (Object) instance
    ).tfcrealworld$invokePlaceRange(region, random, originIndex);
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
    final AddMountainsAndBarrierIslandsAccessor islands =
      (AddMountainsAndBarrierIslandsAccessor) (Object) this;

    int islesPlaced = 0;
    for (
      int attempt = 0;
      attempt < MAP_ISLE_ATTEMPTS && islesPlaced < MAX_MAP_ISLE_CHAINS;
      attempt++
    ) {
      @Nullable
      final Region.Point origin = region.random(random);
      if (origin == null || origin.land()) {
        continue;
      }
      if (
        Byte.toUnsignedInt(origin.oceanDepth) !=
        PNGAltitudeNoise.SHELF_OCEAN_DEPTH
      ) {
        continue;
      }

      if (
        islesPlaced < MAX_MAP_ISLE_CHAINS &&
        origin.distanceToDeepOcean <= 3 &&
        origin.distanceToLand > 2
      ) {
        final IntSet range = islands.tfcrealworld$invokePlaceVolcanicArc(
          region,
          random,
          origin.index
        );
        if (range.size() > MIN_ISLE_CHAIN_SIZE) {
          final byte originContour = origin.distanceToDeepOcean;
          range.forEach(index -> {
            final Region.Point point = region.atIndex(index);
            if (
              point.distanceToDeepOcean >= originContour &&
              point.distanceToDeepOcean <= originContour + 1
            ) {
              point.setBarrierIsland();
            }
            point.setVolcanic();
            point.oceanDepth = PNGAltitudeNoise.REEF_OCEAN_DEPTH;
          });
          islesPlaced += 2;
          continue;
        }
      }

      if (
        islesPlaced < MAX_MAP_ISLE_CHAINS &&
        origin.distanceToLand > 2 &&
        origin.distanceToLand < 6
      ) {
        final IntSet range = islands.tfcrealworld$invokePlaceBarrier(
          region,
          random,
          origin.index
        );
        final byte startContour = (byte) Math.max(1, origin.distanceToLand);
        if (range.size() > MIN_ISLE_CHAIN_SIZE) {
          range.forEach(index -> {
            final Region.Point point = region.atIndex(index);
            if (point.distanceToLand == startContour) {
              point.setBarrierIsland();
            }
            point.oceanDepth = PNGAltitudeNoise.REEF_OCEAN_DEPTH;
          });
          islesPlaced++;
        }
      }
    }
  }
}
