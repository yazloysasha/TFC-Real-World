package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.region.AnnotateBiomeAltitude;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.util.registry.TectonicsRegistry;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicClass;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicClass.LandRelief;
import net.yazloysasha.tfcrealworld.world.tectonics.TectonicsMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Land relief from {@code tectonics.png} → vanilla discrete biome altitude and
 * the mountain flag. Replaces both vanilla mountain placement and the altitude
 * BFS: which land is mountainous is data, not a height threshold.
 * <p>
 * Coastal mountains keep the vanilla meaning (valleys at sea level): ranges
 * the sea reaches into, per {@code tectonics.png}, so fjord coasts get the
 * oceanic variants and ranges above the sea stay inland mountains.
 */
@Mixin(value = AnnotateBiomeAltitude.class, remap = false, priority = 500)
public class AnnotateBiomeAltitudeMixin {

  @Unique
  private static final short FLAG_MOUNTAIN = 0b10000;

  @Unique
  private static final short FLAG_COASTAL_MOUNTAIN = 0b100000;

  @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$altitudeFromRelief(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    final TectonicsMap map = TectonicsRegistry.get(context.generator());
    if (map == null) {
      return;
    }
    final int width = AnnotateBiomeAltitude.WIDTH;

    for (final Region.Point point : context.region.points()) {
      if (point == null) {
        continue;
      }
      tfcrealworld$clearMountain(point);
      if (!point.land()) {
        point.biomeAltitude = 0;
        continue;
      }
      final TectonicClass tectonicClass = map.classAtGrid(point.x, point.z);
      final LandRelief relief = tectonicClass.land();
      point.biomeAltitude = (byte) (relief.discreteAltitude() * width);
      if (relief == LandRelief.MOUNTAIN) {
        point.setMountain();
        if (tectonicClass.isCoastalMountain()) {
          point.setCoastalMountain();
        }
      }
    }
    ci.cancel();
  }

  @Unique
  private static void tfcrealworld$clearMountain(Region.Point point) {
    final RegionPointAccessor flags = (RegionPointAccessor) (Object) point;
    flags.tfcrealworld$setFlags(
      (short) (flags.tfcrealworld$getFlags() &
        ~(FLAG_MOUNTAIN | FLAG_COASTAL_MOUNTAIN))
    );
  }
}
