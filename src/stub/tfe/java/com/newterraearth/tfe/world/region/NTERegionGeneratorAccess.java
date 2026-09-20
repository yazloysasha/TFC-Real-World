package com.newterraearth.tfe.world.region;

import net.dries007.tfc.world.noise.Cellular2D;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.settings.RockSettings;
import net.dries007.tfc.world.settings.Settings;

public interface NTERegionGeneratorAccess {
  void nte$setRootLevelSeed(long rootLevelSeed);

  Noise2D nte$getHotSpotAgeNoise();

  Noise2D nte$getHotSpotIntensityNoise();

  Cellular2D nte$getPlateRegionNoise();

  Noise2D nte$getOceanicInfluenceNoise();

  Noise2D nte$getRainfallVarianceNoise();

  Settings nte$getSettings();

  float nte$continentFactor(int gridX, int gridZ);

  double nte$getDivergence(int gridX, int gridZ);

  RockSettings nte$getSurfaceRock(Region.Point point);
}
