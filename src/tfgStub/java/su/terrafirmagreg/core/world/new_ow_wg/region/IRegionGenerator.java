package su.terrafirmagreg.core.world.new_ow_wg.region;

import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.settings.Settings;

/**
 * Compile-only stub matching TFG's RegionGenerator extras. Not packaged.
 */
public interface IRegionGenerator {
  Settings tfg$getSettings();

  Noise2D tfg$getOceanicInfluenceNoise();

  Noise2D tfg$getHotSpotAgeNoise();

  Noise2D tfg$getHotSpotIntensityNoise();
}
