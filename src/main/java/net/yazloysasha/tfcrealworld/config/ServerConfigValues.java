package net.yazloysasha.tfcrealworld.config;

public class ServerConfigValues {

  private static boolean isServerConfigActive = false;

  private static Double continentalness;
  private static Boolean finiteContinents;
  private static Boolean flatBedrock;
  private static Double grassDensity;
  private static Integer spawnCenterX;
  private static Integer spawnCenterZ;
  private static Integer spawnDistance;
  private static Integer temperatureScale;
  private static Integer rainfallScale;
  private static Integer verticalWorldScale;
  private static Integer horizontalWorldScale;
  private static Boolean continentFromMap;
  private static Boolean altitudeFromMap;
  private static Boolean hotspotsFromMap;
  private static Boolean koppenFromMap;
  private static Integer poleOffset;
  private static Boolean poleLooping;
  private static Boolean canyonsNotVolcanic;

  public static void setServerConfig(
    double continentalness,
    boolean finiteContinents,
    boolean flatBedrock,
    double grassDensity,
    int spawnCenterX,
    int spawnCenterZ,
    int spawnDistance,
    int temperatureScale,
    int rainfallScale,
    int verticalWorldScale,
    int horizontalWorldScale,
    boolean continentFromMap,
    boolean altitudeFromMap,
    boolean hotspotsFromMap,
    boolean koppenFromMap,
    int poleOffset,
    boolean poleLooping,
    boolean canyonsNotVolcanic
  ) {
    ServerConfigValues.continentalness = continentalness;
    ServerConfigValues.finiteContinents = finiteContinents;
    ServerConfigValues.flatBedrock = flatBedrock;
    ServerConfigValues.grassDensity = grassDensity;
    ServerConfigValues.spawnCenterX = spawnCenterX;
    ServerConfigValues.spawnCenterZ = spawnCenterZ;
    ServerConfigValues.spawnDistance = spawnDistance;
    ServerConfigValues.temperatureScale = temperatureScale;
    ServerConfigValues.rainfallScale = rainfallScale;
    ServerConfigValues.verticalWorldScale = verticalWorldScale;
    ServerConfigValues.horizontalWorldScale = horizontalWorldScale;
    ServerConfigValues.continentFromMap = continentFromMap;
    ServerConfigValues.altitudeFromMap = altitudeFromMap;
    ServerConfigValues.hotspotsFromMap = hotspotsFromMap;
    ServerConfigValues.koppenFromMap = koppenFromMap;
    ServerConfigValues.poleOffset = poleOffset;
    ServerConfigValues.poleLooping = poleLooping;
    ServerConfigValues.canyonsNotVolcanic = canyonsNotVolcanic;
    isServerConfigActive = true;
  }

  public static void clearServerConfig() {
    isServerConfigActive = false;
    continentalness = null;
    finiteContinents = null;
    flatBedrock = null;
    grassDensity = null;
    spawnCenterX = null;
    spawnCenterZ = null;
    spawnDistance = null;
    temperatureScale = null;
    rainfallScale = null;
    verticalWorldScale = null;
    horizontalWorldScale = null;
    continentFromMap = null;
    altitudeFromMap = null;
    hotspotsFromMap = null;
    koppenFromMap = null;
    poleOffset = null;
    poleLooping = null;
    canyonsNotVolcanic = null;
  }

  public static boolean isServerConfigActive() {
    return isServerConfigActive;
  }

  public static Double getContinentalness() {
    return continentalness;
  }

  public static Boolean getFiniteContinents() {
    return finiteContinents;
  }

  public static Boolean getFlatBedrock() {
    return flatBedrock;
  }

  public static Double getGrassDensity() {
    return grassDensity;
  }

  public static Integer getSpawnCenterX() {
    return spawnCenterX;
  }

  public static Integer getSpawnCenterZ() {
    return spawnCenterZ;
  }

  public static Integer getSpawnDistance() {
    return spawnDistance;
  }

  public static Integer getTemperatureScale() {
    return temperatureScale;
  }

  public static Integer getRainfallScale() {
    return rainfallScale;
  }

  public static Integer getVerticalWorldScale() {
    return verticalWorldScale;
  }

  public static Integer getHorizontalWorldScale() {
    return horizontalWorldScale;
  }

  public static Boolean getContinentFromMap() {
    return continentFromMap;
  }

  public static Boolean getAltitudeFromMap() {
    return altitudeFromMap;
  }

  public static Boolean getHotspotsFromMap() {
    return hotspotsFromMap;
  }

  public static Boolean getKoppenFromMap() {
    return koppenFromMap;
  }

  public static Integer getPoleOffset() {
    return poleOffset;
  }

  public static Boolean getPoleLooping() {
    return poleLooping;
  }

  public static Boolean getCanyonsNotVolcanic() {
    return canyonsNotVolcanic;
  }
}
