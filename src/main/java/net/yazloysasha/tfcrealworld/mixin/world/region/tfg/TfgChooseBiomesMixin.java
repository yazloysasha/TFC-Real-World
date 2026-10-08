package net.yazloysasha.tfcrealworld.mixin.world.region.tfg;

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
import su.terrafirmagreg.core.world.new_ow_wg.region.IRegionPoint;
import su.terrafirmagreg.core.world.new_ow_wg.region.TFGChooseBiomesTask;

/**
 * TerraFirmaGreg picks every biome from the region-point fields written
 * from the maps. Its island and mountain pools mix volcanic and other
 * biomes at random; with tectonics the map says which ground is volcanic,
 * and an ice sheet meets the sea only where its edge layer finds it.
 */
@Mixin(value = TFGChooseBiomesTask.class, remap = false)
public class TfgChooseBiomesMixin {

  @Unique
  private static final String RANDOM_SEEDED_FROM =
    "Lsu/terrafirmagreg/core/world/new_ow_wg/region/TFGChooseBiomesTask;randomSeededFrom(JI[I)I";

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

  /** The first thing the task asks of each point. */
  @Redirect(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/region/Region$Point;island()Z"
    )
  )
  private boolean tfcrealworld$at(Region.Point point) {
    final IRegionPoint coords = (IRegionPoint) (Object) point;
    MapBiomeChoice.current().at(point, coords.tfg$getX(), coords.tfg$getZ());
    return point.island();
  }

  @Redirect(
    method = "apply",
    at = @At(value = "INVOKE", target = RANDOM_SEEDED_FROM, ordinal = 0)
  )
  private int tfcrealworld$islandBiomeByVolcanism(
    TFGChooseBiomesTask instance,
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
  private int tfcrealworld$coastalMountainBiomeByVolcanism(
    TFGChooseBiomesTask instance,
    long rngSeed,
    int areaSeed,
    int[] choices
  ) {
    return tfcrealworld$pickMountain(rngSeed, areaSeed, choices);
  }

  @Redirect(
    method = "apply",
    at = @At(value = "INVOKE", target = RANDOM_SEEDED_FROM, ordinal = 2)
  )
  private int tfcrealworld$inlandMountainBiomeByVolcanism(
    TFGChooseBiomesTask instance,
    long rngSeed,
    int areaSeed,
    int[] choices
  ) {
    return tfcrealworld$pickMountain(rngSeed, areaSeed, choices);
  }

  @Unique
  private static int tfcrealworld$pickMountain(
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

  /** The task reads the distance for the oceanic ice sheet, then salt marsh. */
  @Redirect(
    method = "apply",
    at = @At(
      value = "FIELD",
      target = "Lnet/dries007/tfc/world/region/Region$Point;distanceToOcean:B",
      opcode = Opcodes.GETFIELD,
      ordinal = 0
    )
  )
  private byte tfcrealworld$iceSheetDistanceToOcean(Region.Point point) {
    return MapBiomeChoice.current().iceSheetDistanceToOcean(point);
  }

  @Redirect(
    method = "apply",
    at = @At(value = "INVOKE", target = RANDOM_SEEDED_FROM, ordinal = 9)
  )
  private int tfcrealworld$reefByMap(
    TFGChooseBiomesTask instance,
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
