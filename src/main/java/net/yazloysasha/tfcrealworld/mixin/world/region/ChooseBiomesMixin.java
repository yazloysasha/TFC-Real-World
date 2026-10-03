package net.yazloysasha.tfcrealworld.mixin.world.region;

import com.llamalad7.mixinextras.sugar.Local;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.region.ChooseBiomes;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.util.registry.TectonicsRegistry;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicsMap;
import net.yazloysasha.tfcrealworld.world.volcano.CenteredFeatureAligner;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla ChooseBiomes picks every biome from the region-point fields written
 * from the maps. Adjusted inputs: islands are volcanic where the map has
 * volcanism and arc islands follow its relief, coastal decisions read
 * distances in grid cells, and atolls stand where the tectonics map has coral
 * reefs. Without the maps every input is vanilla's.
 */
@Mixin(value = ChooseBiomes.class, remap = false)
public class ChooseBiomesMixin {

  /**
   * Vanilla gives an island any of its island biomes: volcanic or not,
   * mountainous or not. With tectonics the map decides both. Islands here are
   * the low ones (a mountainous island is ordinary land with mountain relief),
   * so the pick is among the island biomes that are not mountains, volcanic
   * where the map has volcanism and the others where it has none.
   */
  @Redirect(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/region/ChooseBiomes;randomSeededFrom(JI[I)I",
      ordinal = 0
    )
  )
  private int tfcrealworld$islandBiomeByVolcanism(
    ChooseBiomes instance,
    long rngSeed,
    int areaSeed,
    int[] choices,
    @Local Region.Point point,
    @Local(argsOnly = true) RegionGenerator.Context context
  ) {
    final IntPredicate volcanism = biome ->
      tfcrealworld$isVolcanicBiome(biome) == point.volcanic();
    return tfcrealworld$chooseFitting(
      instance,
      rngSeed,
      areaSeed,
      choices,
      TectonicsRegistry.isActive(context.generator()),
      volcanism.and(biome -> !TFCLayers.isMountains(biome)),
      volcanism
    );
  }

  /**
   * Vanilla gives an island of a volcanic arc volcanic mountains, volcanic
   * island lowlands or the arc's sea floor, at random. With tectonics these
   * are the low islands of the arc, so they are its lowlands; the sea floor
   * of the arc comes from the arc's water around the island.
   */
  @Redirect(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/region/ChooseBiomes;randomSeededFrom(JI[I)I",
      ordinal = 8
    )
  )
  private int tfcrealworld$arcIslandBiomeByRelief(
    ChooseBiomes instance,
    long rngSeed,
    int areaSeed,
    int[] choices,
    @Local(argsOnly = true) RegionGenerator.Context context
  ) {
    return tfcrealworld$chooseFitting(
      instance,
      rngSeed,
      areaSeed,
      choices,
      TectonicsRegistry.isActive(context.generator()),
      biome -> !TFCLayers.isOcean(biome) && !TFCLayers.isMountains(biome)
    );
  }

  /**
   * Vanilla's seeded pick among the choices that fit the first rule that
   * leaves any, or among all of them if no rule does.
   */
  @Unique
  private static int tfcrealworld$chooseFitting(
    ChooseBiomes instance,
    long rngSeed,
    int areaSeed,
    int[] choices,
    boolean fromMap,
    IntPredicate... rules
  ) {
    int[] fitting = choices;
    if (fromMap) {
      for (final IntPredicate rule : rules) {
        final int[] kept = IntStream.of(choices).filter(rule).toArray();
        if (kept.length > 0) {
          fitting = kept;
          break;
        }
      }
    }
    return (
      (ChooseBiomesAccessor) (Object) instance
    ).tfcrealworld$invokeRandomSeededFrom(rngSeed, areaSeed, fitting);
  }

  /** A biome that builds volcanoes: stratovolcanoes or cinder cones. */
  @Unique
  private static boolean tfcrealworld$isVolcanicBiome(int biome) {
    final BiomeExtension extension = TFCLayers.getFromLayerId(biome);
    return extension.hasStratovolcanoes() || extension.hasCinderCones();
  }

  /**
   * Coastal decisions read {@code distanceToOcean} four times: rift valley vs
   * rift lake (0), oceanic collisional mountains (1), oceanic ice sheet (2)
   * and salt marsh (3). Two of them mean "meets the sea": with tectonics,
   * collisional mountains are oceanic where the sea reaches into the range
   * (the {@code coast} property), as every other mountain; ice sheets meet
   * the sea only where vanilla's ice sheet edge layer finds the ocean next
   * to them.
   */
  @Redirect(
    method = "apply",
    at = @At(
      value = "FIELD",
      target = "Lnet/dries007/tfc/world/region/Region$Point;distanceToOcean:B",
      opcode = Opcodes.GETFIELD,
      ordinal = 1
    )
  )
  private byte tfcrealworld$collisionalDistanceToOcean(
    Region.Point point,
    @Local(argsOnly = true) RegionGenerator.Context context
  ) {
    if (TectonicsRegistry.isActive(context.generator())) {
      return point.coastalMountain() ? 0 : Byte.MAX_VALUE;
    }
    return point.distanceToOcean;
  }

  @Redirect(
    method = "apply",
    at = @At(
      value = "FIELD",
      target = "Lnet/dries007/tfc/world/region/Region$Point;distanceToOcean:B",
      opcode = Opcodes.GETFIELD,
      ordinal = 2
    )
  )
  private byte tfcrealworld$iceSheetDistanceToOcean(
    Region.Point point,
    @Local(argsOnly = true) RegionGenerator.Context context
  ) {
    if (TectonicsRegistry.isActive(context.generator())) {
      return Byte.MAX_VALUE;
    }
    return point.distanceToOcean;
  }

  /**
   * Vanilla builds atolls in warm sea far enough from land, which on a real
   * map is nearly all of the tropical ocean and almost none of its shelf. With
   * tectonics the distance check is replaced by the {@code atolls} property:
   * the sea where coral reefs stand. Vanilla still asks for warm water.
   */
  @Redirect(
    method = "apply",
    at = @At(
      value = "FIELD",
      target = "Lnet/dries007/tfc/world/region/Region$Point;distanceToLand:B",
      opcode = Opcodes.GETFIELD
    )
  )
  private byte tfcrealworld$atollDistanceToLand(
    Region.Point point,
    @Local(argsOnly = true) RegionGenerator.Context context
  ) {
    final TectonicsMap tectonics = TectonicsRegistry.get(context.generator());
    if (tectonics == null) {
      return point.distanceToLand;
    }
    return tectonics.classAtGrid(point.x, point.z).atolls()
      ? Byte.MAX_VALUE
      : 0;
  }

  @Inject(method = "apply", at = @At("TAIL"))
  private void tfcrealworld$alignCenteredVolcanoes(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    if (!TectonicsRegistry.isActive(context.generator())) {
      return;
    }
    CenteredFeatureAligner.align(
      context.region,
      context.generator().seed().seed()
    );
  }
}
