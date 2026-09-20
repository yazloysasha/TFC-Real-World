package com.newterraearth.tfe.world.region;

public interface NTEPointAccess {
  byte nte$getHotSpotAge();

  void nte$setHotSpotAge(byte age);

  byte nte$getDistanceToWestCoast();

  void nte$setDistanceToWestCoast(byte distanceToWestCoast);

  float nte$getRainfallVariance();

  void nte$setRainfallVariance(float rainfallVariance);

  boolean nte$isSurfaceRockKarst();

  void nte$setSurfaceRockKarst(boolean karst);

  double nte$getDivergence();

  void nte$setDivergence(double divergence);

  byte nte$getDistanceToDeepOcean();

  void nte$setDistanceToDeepOcean(byte distance);

  byte nte$getDistanceToLand();

  void nte$setDistanceToLand(byte distance);

  byte nte$getOceanDepth();

  void nte$setOceanDepth(byte depth);

  boolean nte$isVolcanic();

  void nte$setVolcanic(boolean volcanic);

  boolean nte$isBarrierIsland();

  void nte$setBarrierIsland(boolean barrierIsland);
}
