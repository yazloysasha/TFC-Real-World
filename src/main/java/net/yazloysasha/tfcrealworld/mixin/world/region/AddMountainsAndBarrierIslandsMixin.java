package net.yazloysasha.tfcrealworld.mixin.world.region;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.dries007.tfc.world.region.AddMountainsAndBarrierIslands;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.minecraft.util.RandomSource;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.registry.DivergenceNoiseRegistry;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGAltitudeNoise;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGDivergenceNoise;
import net.yazloysasha.tfcrealworld.world.region.MapTectonics;
import net.yazloysasha.tfcrealworld.world.region.calculator.AltitudeCalculator;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AddMountainsAndBarrierIslands.class, remap = false)
public class AddMountainsAndBarrierIslandsMixin {

  private static final int MIN_ISLE_CHAIN_SIZE = 45;
  private static final int MAP_ISLE_ATTEMPTS = 80;
  private static final int MAX_MAP_ISLE_CHAINS = 12;

  /** Match prior map subduction-shelf band; excludes finite-map ocean edges. */
  private static final int ARC_MIN_DISTANCE_TO_LAND = 2;
  private static final int ARC_MAX_DISTANCE_TO_LAND = 8;
  private static final int ARC_MAX_DISTANCE_TO_DEEP = 3;
  private static final int TRENCH_SHELF_INFLUENCE_RADIUS = 2;

  /** Nearshore shelf → reef (oceanDepth 1) without volcanic/barrier flags. */
  private static final int REEF_MAX_DISTANCE_TO_LAND = 3;

  @Inject(method = "apply", at = @At("HEAD"))
  private void tfcrealworld$applyMapOceanDepthBeforeBarrierIslands(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    new AltitudeCalculator()
      .prepareOceanForBarrierIslands(context.region, context.generator());
  }

  @WrapOperation(
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
    int originIndex,
    Operation<IntSet> original
  ) {
    if (TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()) {
      return new IntOpenHashSet();
    }
    return original.call(instance, region, random, originIndex);
  }

  /**
   * Extra shelf chains when altitude comes from the map. Mirrors vanilla
   * {@code AddMountainsAndBarrierIslands} flag writes so ChooseBiomes can pick
   * {@code OCEANIC_VOLCANIC_ARC} / {@code VOLCANIC_ISLAND} via:
   * <ul>
   *   <li>{@code barrierIsland() && volcanic()} → VOLCANIC_ARC_BIOMES</li>
   *   <li>{@code oceanDepth == 1 && volcanic()} → OCEANIC_VOLCANIC_ARC</li>
   * </ul>
   * No biome IDs are assigned here — only the same inputs vanilla sets.
   * {@code distanceToLand} is capped so arcs seed in real subduction shelves,
   * not on finite-map borders where land is intentionally absent.
   */
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

      // Vanilla volcanic arc: shelf + subduction + near deep ocean, near land.
      if (
        islesPlaced < MAX_MAP_ISLE_CHAINS &&
        origin.divergence < 0 &&
        origin.distanceToDeepOcean <= ARC_MAX_DISTANCE_TO_DEEP &&
        origin.distanceToLand > ARC_MIN_DISTANCE_TO_LAND &&
        origin.distanceToLand < ARC_MAX_DISTANCE_TO_LAND
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

      // Vanilla barrier island: shelf + rift/passive margin near coast.
      if (
        islesPlaced < MAX_MAP_ISLE_CHAINS &&
        origin.divergence > 0 &&
        origin.distanceToLand > ARC_MIN_DISTANCE_TO_LAND &&
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

    // Random chains miss thin Earth-map shelves; paint remaining subduction
    // shelf with the same vanilla flags, then coastal reefs for depth-1.
    tfcrealworld$paintSubductionArcFlags(region, context.generator());
    tfcrealworld$paintCoastalReefDepths(region);
  }

  /**
   * Deterministic subduction-shelf → arc inputs (old map ChooseBiomes path,
   * but flags only). Requires shelf, local compression, trench influence, and
   * a land-distance band so map edges without land are skipped.
   */
  @Unique
  private static void tfcrealworld$paintSubductionArcFlags(
    Region region,
    RegionGenerator generator
  ) {
    if (!MapTectonics.isActive(generator)) {
      return;
    }
    final PNGDivergenceNoise divergenceNoise = DivergenceNoiseRegistry.get(
      generator
    );
    if (divergenceNoise == null) {
      return;
    }

    for (final Region.Point point : region.points()) {
      if (point == null || point.land()) {
        continue;
      }
      final int depth = Byte.toUnsignedInt(point.oceanDepth);
      if (
        depth != PNGAltitudeNoise.SHELF_OCEAN_DEPTH &&
        depth != PNGAltitudeNoise.REEF_OCEAN_DEPTH
      ) {
        continue;
      }
      if (point.divergence >= 0f) {
        continue;
      }
      if (
        point.distanceToLand <= ARC_MIN_DISTANCE_TO_LAND ||
        point.distanceToLand >= ARC_MAX_DISTANCE_TO_LAND
      ) {
        continue;
      }
      if (point.distanceToDeepOcean > ARC_MAX_DISTANCE_TO_DEEP) {
        continue;
      }
      if (
        !MapTectonics.isNearTrenchInfluence(
          divergenceNoise,
          point.x,
          point.z,
          TRENCH_SHELF_INFLUENCE_RADIUS
        )
      ) {
        continue;
      }

      // Contour cells facing deep ocean become barrier islands (VOLCANIC_ARC
      // biomes including VOLCANIC_ISLAND); the rest stay depth-1 volcanic
      // (OCEANIC_VOLCANIC_ARC only) — same split as vanilla placeVolcanicArc.
      if (point.distanceToDeepOcean <= 1) {
        point.setBarrierIsland();
      }
      point.setVolcanic();
      point.oceanDepth = PNGAltitudeNoise.REEF_OCEAN_DEPTH;
    }
  }

  /**
   * Nearshore non-subduction shelf → {@code oceanDepth == 1} so vanilla picks
   * {@code OCEAN_REEF}. Does not set volcanic/barrier flags. Skips cells
   * already claimed by arcs.
   */
  @Unique
  private static void tfcrealworld$paintCoastalReefDepths(Region region) {
    for (final Region.Point point : region.points()) {
      if (point == null || point.land() || point.volcanic()) {
        continue;
      }
      if (
        Byte.toUnsignedInt(point.oceanDepth) !=
        PNGAltitudeNoise.SHELF_OCEAN_DEPTH
      ) {
        continue;
      }
      if (
        point.distanceToLand < 1 ||
        point.distanceToLand > REEF_MAX_DISTANCE_TO_LAND
      ) {
        continue;
      }
      // Leave active subduction shelves for arc paint; reefs on passive /
      // neutral margins (and anything divergence paint already promoted).
      if (point.divergence < 0f) {
        continue;
      }
      point.oceanDepth = PNGAltitudeNoise.REEF_OCEAN_DEPTH;
    }
  }
}
