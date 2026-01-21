package net.yazloysasha.tfcrealworld.world.noise.koppen;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.Mth;
import net.yazloysasha.tfcrealworld.world.climate.RealKoppenClimateClassification;

public class KoppenParameterCache {

  public static class ParameterCombination {

    public final float temperature;
    public final float rainfall;

    public ParameterCombination(float temperature, float rainfall) {
      this.temperature = temperature;
      this.rainfall = rainfall;
    }
  }

  private static class ParameterArray {

    final float[] temperatures;
    final float[] rainfalls;

    ParameterArray(int size) {
      this.temperatures = new float[size];
      this.rainfalls = new float[size];
    }

    ParameterCombination get(int index) {
      return new ParameterCombination(temperatures[index], rainfalls[index]);
    }
  }

  private static class ParameterGrid {

    final int gridSize;
    final ParameterCombination[][] grid;

    ParameterGrid(int gridSize) {
      this.gridSize = gridSize;
      this.grid = new ParameterCombination[gridSize][gridSize];
    }

    void set(int tempIndex, int rainIndex, ParameterCombination params) {
      grid[tempIndex][rainIndex] = params;
    }

    ParameterCombination get(int tempIndex, int rainIndex) {
      return grid[tempIndex][rainIndex];
    }
  }

  private static KoppenParameterCache instance;

  private final Map<
    RealKoppenClimateClassification,
    ParameterArray
  > climateCombinations;
  private final Map<RealKoppenClimateClassification, float[]> temperatureRanges;
  private final Map<RealKoppenClimateClassification, float[]> rainfallRanges;
  private final Map<
    RealKoppenClimateClassification,
    ParameterGrid
  > parameterGrids;

  private KoppenParameterCache() {
    this.climateCombinations = new HashMap<>();
    this.temperatureRanges = new HashMap<>();
    this.rainfallRanges = new HashMap<>();
    this.parameterGrids = new HashMap<>();
    buildCache();
  }

  public static synchronized KoppenParameterCache getInstance() {
    if (instance == null) {
      instance = new KoppenParameterCache();
    }
    return instance;
  }

  public static synchronized void clear() {
    instance = null;
  }

  public ParameterCombination getParametersFromGrayscale(
    RealKoppenClimateClassification climate,
    double temperatureGrayscale,
    double rainfallGrayscale
  ) {
    ParameterGrid grid = parameterGrids.get(climate);
    if (grid == null) {
      return new ParameterCombination(
        ClimateConstants.DEFAULT_TEMP,
        ClimateConstants.DEFAULT_RAIN
      );
    }

    double normalizedTemp = Mth.clamp(temperatureGrayscale / 255.0, 0.0, 1.0);
    double normalizedRain = Mth.clamp(rainfallGrayscale / 255.0, 0.0, 1.0);

    int tempIndex = (int) Math.round(normalizedTemp * (grid.gridSize - 1));
    int rainIndex = (int) Math.round(normalizedRain * (grid.gridSize - 1));

    tempIndex = Mth.clamp(tempIndex, 0, grid.gridSize - 1);
    rainIndex = Mth.clamp(rainIndex, 0, grid.gridSize - 1);

    ParameterCombination result = grid.get(tempIndex, rainIndex);
    if (result == null) {
      return new ParameterCombination(
        ClimateConstants.DEFAULT_TEMP,
        ClimateConstants.DEFAULT_RAIN
      );
    }
    return result;
  }

  public float[] getTemperatureRange(RealKoppenClimateClassification climate) {
    float[] cached = temperatureRanges.get(climate);
    return cached != null
      ? cached
      : new float[] { ClimateConstants.TEMP_MIN, ClimateConstants.TEMP_MAX };
  }

  public float[] getRainfallRange(RealKoppenClimateClassification climate) {
    float[] cached = rainfallRanges.get(climate);
    return cached != null
      ? cached
      : new float[] { ClimateConstants.RAIN_MIN, ClimateConstants.RAIN_MAX };
  }

  private void buildCache() {
    float[] temperatures = generateRange(
      ClimateConstants.TEMP_MIN,
      ClimateConstants.TEMP_MAX,
      ClimateConstants.TEMP_STEP
    );
    float[] rainfalls = generateRange(
      ClimateConstants.RAIN_MIN,
      ClimateConstants.RAIN_MAX,
      ClimateConstants.RAIN_STEP
    );
    float[] rainVars = generateRange(
      ClimateConstants.RAINVAR_MIN,
      ClimateConstants.RAINVAR_MAX,
      ClimateConstants.RAINVAR_STEP
    );

    Map<RealKoppenClimateClassification, Integer> climateCounts =
      new HashMap<>();
    for (RealKoppenClimateClassification climate : RealKoppenClimateClassification.values()) {
      climateCounts.put(climate, 0);
    }

    for (float temp : temperatures) {
      for (float rain : rainfalls) {
        for (float rainVar : rainVars) {
          RealKoppenClimateClassification climate =
            RealKoppenClimateClassification.classify(temp, rain, rainVar, true);
          climateCounts.merge(climate, 1, Integer::sum);
        }
      }
    }

    Map<RealKoppenClimateClassification, Integer> climateIndices =
      new HashMap<>();
    for (Map.Entry<
      RealKoppenClimateClassification,
      Integer
    > entry : climateCounts.entrySet()) {
      climateCombinations.put(
        entry.getKey(),
        new ParameterArray(entry.getValue())
      );
      climateIndices.put(entry.getKey(), 0);
    }

    for (float temp : temperatures) {
      for (float rain : rainfalls) {
        for (float rainVar : rainVars) {
          RealKoppenClimateClassification climate =
            RealKoppenClimateClassification.classify(temp, rain, rainVar, true);
          ParameterArray array = climateCombinations.get(climate);
          int index = climateIndices.get(climate);
          array.temperatures[index] = temp;
          array.rainfalls[index] = rain;
          climateIndices.put(climate, index + 1);
        }
      }
    }

    for (RealKoppenClimateClassification climate : RealKoppenClimateClassification.values()) {
      ParameterArray array = climateCombinations.get(climate);
      if (array != null && array.temperatures.length > 0) {
        computeStatistics(climate, array);
        parameterGrids.put(climate, buildParameterGrid(climate, array));
      } else {
        setDefaultStatistics(climate);
      }
    }
  }

  private void computeStatistics(
    RealKoppenClimateClassification climate,
    ParameterArray combinations
  ) {
    float tempMin = Float.MAX_VALUE;
    float tempMax = Float.MIN_VALUE;
    float rainMin = Float.MAX_VALUE;
    float rainMax = Float.MIN_VALUE;

    for (int i = 0; i < combinations.temperatures.length; i++) {
      float temp = combinations.temperatures[i];
      float rain = combinations.rainfalls[i];

      if (temp < tempMin) tempMin = temp;
      if (temp > tempMax) tempMax = temp;

      if (rain < rainMin) rainMin = rain;
      if (rain > rainMax) rainMax = rain;
    }

    temperatureRanges.put(climate, new float[] { tempMin, tempMax });
    rainfallRanges.put(climate, new float[] { rainMin, rainMax });
  }

  private void setDefaultStatistics(RealKoppenClimateClassification climate) {
    temperatureRanges.put(
      climate,
      new float[] { ClimateConstants.TEMP_MIN, ClimateConstants.TEMP_MAX }
    );
    rainfallRanges.put(
      climate,
      new float[] { ClimateConstants.RAIN_MIN, ClimateConstants.RAIN_MAX }
    );
  }

  private ParameterGrid buildParameterGrid(
    RealKoppenClimateClassification climate,
    ParameterArray combinations
  ) {
    final int GRID_SIZE = 64;
    ParameterGrid grid = new ParameterGrid(GRID_SIZE);

    float[] tempRange = temperatureRanges.get(climate);
    float[] rainRange = rainfallRanges.get(climate);

    if (combinations.temperatures.length == 0) {
      return grid;
    }

    for (int tempIndex = 0; tempIndex < GRID_SIZE; tempIndex++) {
      float targetTemp =
        tempRange[0] +
        ((tempRange[1] - tempRange[0]) * tempIndex) / (GRID_SIZE - 1);

      for (int rainIndex = 0; rainIndex < GRID_SIZE; rainIndex++) {
        float targetRain =
          rainRange[0] +
          ((rainRange[1] - rainRange[0]) * rainIndex) / (GRID_SIZE - 1);

        ParameterCombination closest = findClosestCombination(
          combinations,
          targetTemp,
          targetRain,
          tempRange,
          rainRange
        );

        grid.set(tempIndex, rainIndex, closest);
      }
    }

    return grid;
  }

  private ParameterCombination findClosestCombination(
    ParameterArray combinations,
    float targetTemp,
    float targetRain,
    float[] tempRange,
    float[] rainRange
  ) {
    float minDistance = Float.MAX_VALUE;
    int bestIndex = 0;

    float tempSpan = Math.max(tempRange[1] - tempRange[0], 0.001f);
    float rainSpan = Math.max(rainRange[1] - rainRange[0], 0.001f);

    float invTempSpan = 1.0f / tempSpan;
    float invRainSpan = 1.0f / rainSpan;

    for (int i = 0; i < combinations.temperatures.length; i++) {
      float normTempDiff =
        (combinations.temperatures[i] - targetTemp) * invTempSpan;
      float normRainDiff =
        (combinations.rainfalls[i] - targetRain) * invRainSpan;
      float distance =
        normTempDiff * normTempDiff + normRainDiff * normRainDiff;

      if (distance < minDistance) {
        minDistance = distance;
        bestIndex = i;
      }
    }

    return combinations.get(bestIndex);
  }

  private float[] generateRange(float min, float max, float step) {
    int count = (int) Math.ceil((max - min) / step) + 1;
    float[] range = new float[count];
    for (int i = 0; i < count; i++) {
      range[i] = min + i * step;
      if (range[i] > max) {
        range[i] = max;
      }
    }
    return range;
  }
}
