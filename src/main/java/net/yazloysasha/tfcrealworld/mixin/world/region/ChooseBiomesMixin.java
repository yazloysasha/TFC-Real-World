package net.yazloysasha.tfcrealworld.mixin.world.region;

import static net.dries007.tfc.world.layer.TFCLayers.OCEAN_RIDGE;
import static net.dries007.tfc.world.layer.TFCLayers.SUNKEN_SHIELD_VOLCANO;

import com.llamalad7.mixinextras.sugar.Local;
import net.dries007.tfc.world.region.ChooseBiomes;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.util.registry.TectonicsRegistry;
import net.yazloysasha.tfcrealworld.world.region.cache.GlobalOceanDistanceCache;
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
 * from the maps. Adjusted inputs: hotspot shields stay off the ocean (sunken
 * shields may also cover ocean ridges), coastal decisions read distances in
 * grid cells, and atolls stand where the tectonics map has coral reefs.
 */
@Mixin(value = ChooseBiomes.class, remap = false)
public class ChooseBiomesMixin {

  @Redirect(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/region/ChooseBiomes;getHotSpotBiome(I)I"
    )
  )
  private int tfcrealworld$hotspotBiomeWithoutRaisingLand(
    ChooseBiomes instance,
    int age,
    @Local Region.Point point
  ) {
    if (!point.land()) {
      if (age == 4 && point.biome == OCEAN_RIDGE) {
        return SUNKEN_SHIELD_VOLCANO;
      }
      return point.biome;
    }
    return (
      (ChooseBiomesAccessor) (Object) instance
    ).tfcrealworld$invokeGetHotSpotBiome(age);
  }

  /**
   * Coastal decisions read {@code distanceToOcean} four times: rift valley vs
   * rift lake (0), oceanic collisional mountains (1), oceanic ice sheet (2)
   * and salt marsh (3). They are vanilla thresholds in grid cells, except
   * where vanilla means "meets the sea". With tectonics, collisional
   * mountains are oceanic where the sea reaches into the range (the
   * {@code coast} property), as every other mountain; ice sheets meet the sea
   * only where vanilla's ice sheet edge layer finds the ocean next to them.
   */
  @Redirect(
    method = "apply",
    at = @At(
      value = "FIELD",
      target = "Lnet/dries007/tfc/world/region/Region$Point;distanceToOcean:B",
      opcode = Opcodes.GETFIELD,
      ordinal = 0
    )
  )
  private byte tfcrealworld$riftDistanceToOcean(Region.Point point) {
    return tfcrealworld$gridCellsToOcean(point);
  }

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
    return tfcrealworld$gridCellsToOcean(point);
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
    return tfcrealworld$gridCellsToOcean(point);
  }

  @Redirect(
    method = "apply",
    at = @At(
      value = "FIELD",
      target = "Lnet/dries007/tfc/world/region/Region$Point;distanceToOcean:B",
      opcode = Opcodes.GETFIELD,
      ordinal = 3
    )
  )
  private byte tfcrealworld$saltMarshDistanceToOcean(Region.Point point) {
    return tfcrealworld$gridCellsToOcean(point);
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

  @Unique
  private static byte tfcrealworld$gridCellsToOcean(Region.Point point) {
    return GlobalOceanDistanceCache.toGridCells(point.distanceToOcean);
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
