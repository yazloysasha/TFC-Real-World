package net.yazloysasha.tfcrealworld.mixin.world.region.tfe;

import com.newterraearth.tfe.world.region.NTEPointAccess;
import java.util.function.IntPredicate;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.region.ChooseBiomes;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.world.region.MapBiomeChoice;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * TerraFirmaEarth's ChooseBiomes is TFC 4's: it picks every biome from the
 * region-point fields written from the maps. Adjusted inputs: islands are
 * volcanic where the map has volcanism and arc islands follow its relief,
 * coastal decisions read distances as "meets the sea", and atolls stand
 * where the tectonics map has coral reefs. Without the maps every input is
 * the generator's.
 */
@Mixin(value = ChooseBiomes.class, remap = false, priority = 1500)
public class TfeChooseBiomesMixin {

  @Unique
  private static final String RANDOM_SEEDED_FROM =
    "Lnet/dries007/tfc/world/region/ChooseBiomes;randomSeededFrom(JI[I)I";

  @Unique
  private static final String DISTANCE_TO_OCEAN =
    "Lnet/dries007/tfc/world/region/Region$Point;distanceToOcean:B";

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

  /**
   * An island of a volcanic arc takes volcanic mountains, volcanic island
   * lowlands or the arc's sea floor, at random. With tectonics these are
   * the low islands of the arc, so they are its lowlands; the sea floor of
   * the arc comes from the arc's water around the island.
   */
  @Redirect(
    method = "apply",
    at = @At(value = "INVOKE", target = RANDOM_SEEDED_FROM, ordinal = 8)
  )
  private int tfcrealworld$arcIslandBiomeByRelief(
    ChooseBiomes instance,
    long rngSeed,
    int areaSeed,
    int[] choices
  ) {
    final IntPredicate lowland = biome ->
      !TFCLayers.isOcean(biome) && !TFCLayers.isMountains(biome);
    return MapBiomeChoice.current().pick(rngSeed, areaSeed, choices, lowland);
  }

  /**
   * The task reads {@code distanceToOcean} four times: rift valley or rift
   * lake (0), oceanic collisional mountains (1), oceanic ice sheet (2) and
   * salt marsh (3). Two of them mean "meets the sea".
   */
  @Redirect(
    method = "apply",
    at = @At(
      value = "FIELD",
      target = DISTANCE_TO_OCEAN,
      opcode = Opcodes.GETFIELD,
      ordinal = 1
    )
  )
  private byte tfcrealworld$collisionalDistanceToOcean(Region.Point point) {
    return MapBiomeChoice.current().collisionalDistanceToOcean(point);
  }

  @Redirect(
    method = "apply",
    at = @At(
      value = "FIELD",
      target = DISTANCE_TO_OCEAN,
      opcode = Opcodes.GETFIELD,
      ordinal = 2
    )
  )
  private byte tfcrealworld$iceSheetDistanceToOcean(Region.Point point) {
    return MapBiomeChoice.current().iceSheetDistanceToOcean(point);
  }

  @Redirect(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lcom/newterraearth/tfe/world/region/NTEPointAccess;nte$getDistanceToLand()B"
    )
  )
  private byte tfcrealworld$atollDistanceToLand(NTEPointAccess point) {
    return MapBiomeChoice.current().atollDistanceToLand(
      point.nte$getDistanceToLand()
    );
  }

  @Inject(method = "apply", at = @At("TAIL"))
  private void tfcrealworld$leave(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    MapBiomeChoice.leave(context);
  }
}
