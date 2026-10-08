package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.ChooseBiomes;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.world.region.MapBiomeChoice;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * TFC 3 picks every biome from the region-point fields written from the
 * maps. Its island and mountain pools mix volcanic and other biomes at
 * random; with tectonics the map says which ground is volcanic.
 */
@Mixin(value = ChooseBiomes.class, remap = false)
public class ChooseBiomesMixin {

  @Unique
  private static final String RANDOM_SEEDED_FROM =
    "Lnet/dries007/tfc/world/region/ChooseBiomes;randomSeededFrom(JI[I)I";

  @Shadow
  @Final
  private static int[] MOUNTAIN_ALTITUDE_BIOMES;

  @Shadow
  @Final
  private static int[] OCEANIC_MOUNTAIN_ALTITUDE_BIOMES;

  @Inject(method = "apply", at = @At("HEAD"))
  private void tfcrealworld$enter(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    MapBiomeChoice.enter(context);
  }

  @Redirect(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/region/Region;maybeAt(II)Lnet/dries007/tfc/world/region/Region$Point;"
    )
  )
  private Region.Point tfcrealworld$at(Region region, int x, int z) {
    final Region.Point point = region.maybeAt(x, z);
    MapBiomeChoice.current().at(point, x, z);
    return point;
  }

  @Redirect(
    method = "apply",
    at = @At(value = "INVOKE", target = RANDOM_SEEDED_FROM, ordinal = 0)
  )
  private int tfcrealworld$islandBiomeByVolcanism(
    ChooseBiomes instance,
    long rngSeed,
    int areaSeed,
    int[] choices
  ) {
    return MapBiomeChoice.current().pickIsland(rngSeed, areaSeed, choices);
  }

  @Redirect(
    method = "apply",
    at = @At(value = "INVOKE", target = RANDOM_SEEDED_FROM, ordinal = 1)
  )
  private int tfcrealworld$mountainBiomeByVolcanism(
    ChooseBiomes instance,
    long rngSeed,
    int areaSeed,
    int[] choices
  ) {
    return MapBiomeChoice.current().pickMountain(
      rngSeed,
      areaSeed,
      choices,
      MOUNTAIN_ALTITUDE_BIOMES,
      OCEANIC_MOUNTAIN_ALTITUDE_BIOMES
    );
  }

  @Redirect(
    method = "apply",
    at = @At(value = "INVOKE", target = RANDOM_SEEDED_FROM, ordinal = 3)
  )
  private int tfcrealworld$reefByMap(
    ChooseBiomes instance,
    long rngSeed,
    int areaSeed,
    int[] choices
  ) {
    return MapBiomeChoice.current().pickMidDepthOcean(
      rngSeed,
      areaSeed,
      choices
    );
  }

  @Redirect(
    method = "apply",
    at = @At(
      value = "FIELD",
      target = "Lnet/dries007/tfc/world/region/Region$Point;distanceToEdge:B",
      opcode = Opcodes.GETFIELD
    )
  )
  private byte tfcrealworld$reefAtAnyBoundary(Region.Point point) {
    return MapBiomeChoice.current().oceanDistanceToEdge(point);
  }

  @Inject(method = "apply", at = @At("TAIL"))
  private void tfcrealworld$leave(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    MapBiomeChoice.leave(context);
  }
}
