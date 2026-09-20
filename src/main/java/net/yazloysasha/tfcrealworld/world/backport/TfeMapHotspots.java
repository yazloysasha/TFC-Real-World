package net.yazloysasha.tfcrealworld.world.backport;

import com.newterraearth.tfe.world.region.NTEPointAccess;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.registry.HotspotsNoiseRegistry;
import net.yazloysasha.tfcrealworld.world.volcano.MapHotspotApplicator;
import net.yazloysasha.tfcrealworld.world.volcano.MapHotspotLayout;

public final class TfeMapHotspots {

  private TfeMapHotspots() {}

  public static boolean applyFromMap(RegionGenerator.Context context) {
    if (!TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get()) {
      return false;
    }
    if (HotspotsNoiseRegistry.get(context.generator()) == null) {
      return false;
    }
    final MapHotspotLayout layout = HotspotsNoiseRegistry.biomeLayout();
    if (layout == null) {
      return false;
    }
    MapHotspotApplicator.applyAgesAndLand(
      context.region,
      layout,
      (point, age) -> ((NTEPointAccess) point).nte$setHotSpotAge(age)
    );
    return true;
  }
}
