package net.yazloysasha.tfcrealworld.world.noise.koppen;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.dries007.tfc.util.climate.KoppenClimateClassification;
import net.minecraft.util.Mth;

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
    KoppenClimateClassification,
    ParameterArray
  > climateCombinations;

  private final Map<KoppenClimateClassification, Float> baseTemperatures;
  private final Map<KoppenClimateClassification, Float> baseRainfalls;
  private final Map<KoppenClimateClassification, float[]> temperatureRanges;
  private final Map<KoppenClimateClassification, float[]> rainfallRanges;
  private final Map<KoppenClimateClassification, ParameterGrid> parameterGrids;

  private KoppenParameterCache() {
    this.climateCombinations = new HashMap<>();
    this.baseTemperatures = new HashMap<>();
    this.baseRainfalls = new HashMap<>();
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
    if (instance != null) {
      instance = null;
    }
  }

  public ParameterCombination getParametersFromGrayscale(
    KoppenClimateClassification climate,
    double temperatureGrayscale,
    double rainfallGrayscale
  ) {
    ParameterGrid grid = parameterGrids.get(climate);
    if (grid == null) {
      return new ParameterCombination(5.0f, 100.0f);
    }

    double normalizedTemp = Mth.clamp(temperatureGrayscale / 255.0, 0.0, 1.0);
    double normalizedRain = Mth.clamp(rainfallGrayscale / 255.0, 0.0, 1.0);

    int tempIndex = (int) Math.round(normalizedTemp * (grid.gridSize - 1));
    int rainIndex = (int) Math.round(normalizedRain * (grid.gridSize - 1));

    tempIndex = Mth.clamp(tempIndex, 0, grid.gridSize - 1);
    rainIndex = Mth.clamp(rainIndex, 0, grid.gridSize - 1);

    ParameterCombination result = grid.get(tempIndex, rainIndex);

    if (result == null) {
      return new ParameterCombination(5.0f, 100.0f);
    }

    return result;
  }

  public float getBaseTemperature(KoppenClimateClassification climate) {
    Float cached = baseTemperatures.get(climate);
    if (cached != null) {
      return cached;
    }
    return 5.0f;
  }

  public float getBaseRainfall(KoppenClimateClassification climate) {
    Float cached = baseRainfalls.get(climate);
    if (cached != null) {
      return cached;
    }
    return 100.0f;
  }

  public float[] getTemperatureRange(KoppenClimateClassification climate) {
    float[] cached = temperatureRanges.get(climate);
    if (cached != null) {
      return cached;
    }
    return new float[] { -20.0f, 30.0f };
  }

  public float[] getRainfallRange(KoppenClimateClassification climate) {
    float[] cached = rainfallRanges.get(climate);
    if (cached != null) {
      return cached;
    }
    return new float[] { 0.0f, 500.0f };
  }

  private void buildCache() {
    float[] temperatures = generateRange(-20.0f, 30.0f, 1.0f);
    float[] rainfalls = generateRange(0.0f, 500.0f, 10.0f);

    Map<KoppenClimateClassification, Integer> climateCounts = new HashMap<>();
    for (KoppenClimateClassification climate : KoppenClimateClassification.values()) {
      climateCounts.put(climate, 0);
    }

    for (float temp : temperatures) {
      for (float rain : rainfalls) {
        KoppenClimateClassification climate =
          KoppenClimateClassification.classify(temp, rain);
        climateCounts.merge(climate, 1, Integer::sum);
      }
    }

    Map<KoppenClimateClassification, Integer> climateIndices = new HashMap<>();
    for (Map.Entry<
      KoppenClimateClassification,
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
        KoppenClimateClassification climate =
          KoppenClimateClassification.classify(temp, rain);
        ParameterArray array = climateCombinations.get(climate);
        int index = climateIndices.get(climate);
        array.temperatures[index] = temp;
        array.rainfalls[index] = rain;
        climateIndices.put(climate, index + 1);
      }
    }

    for (KoppenClimateClassification climate : KoppenClimateClassification.values()) {
      ParameterArray array = climateCombinations.get(climate);
      if (array != null && array.temperatures.length > 0) {
        sortParameterArray(array);
        computeStatistics(climate, array);
        parameterGrids.put(climate, buildParameterGrid(climate, array));
      } else {
        setDefaultStatistics(climate);
      }
    }
  }

  private void computeStatistics(
    KoppenClimateClassification climate,
    ParameterArray combinations
  ) {
    float tempSum = 0.0f, tempMin = Float.MAX_VALUE, tempMax = Float.MIN_VALUE;
    float rainSum = 0.0f, rainMin = Float.MAX_VALUE, rainMax = Float.MIN_VALUE;

    for (int i = 0; i < combinations.temperatures.length; i++) {
      float temp = combinations.temperatures[i];
      float rain = combinations.rainfalls[i];

      tempSum += temp;
      if (temp < tempMin) tempMin = temp;
      if (temp > tempMax) tempMax = temp;

      rainSum += rain;
      if (rain < rainMin) rainMin = rain;
      if (rain > rainMax) rainMax = rain;
    }

    int length = combinations.temperatures.length;
    baseTemperatures.put(climate, tempSum / length);
    temperatureRanges.put(climate, new float[] { tempMin, tempMax });
    baseRainfalls.put(climate, rainSum / length);
    rainfallRanges.put(climate, new float[] { rainMin, rainMax });
  }

  private void setDefaultStatistics(KoppenClimateClassification climate) {
    baseTemperatures.put(climate, 5.0f);
    temperatureRanges.put(climate, new float[] { -20.0f, 30.0f });
    baseRainfalls.put(climate, 100.0f);
    rainfallRanges.put(climate, new float[] { 0.0f, 500.0f });
  }

  private ParameterGrid buildParameterGrid(
    KoppenClimateClassification climate,
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

  private void sortParameterArray(ParameterArray array) {
    int length = array.temperatures.length;
    if (length <= 1) {
      return;
    }

    float tempMin = Float.MAX_VALUE, tempMax = Float.MIN_VALUE;
    float rainMin = Float.MAX_VALUE, rainMax = Float.MIN_VALUE;

    for (int i = 0; i < length; i++) {
      float temp = array.temperatures[i];
      float rain = array.rainfalls[i];

      if (temp < tempMin) tempMin = temp;
      if (temp > tempMax) tempMax = temp;
      if (rain < rainMin) rainMin = rain;
      if (rain > rainMax) rainMax = rain;
    }

    final float finalTempMin = tempMin;
    final float finalTempRange = Math.max(tempMax - tempMin, 0.001f);
    final float finalRainMin = rainMin;
    final float finalRainRange = Math.max(rainMax - rainMin, 0.001f);

    Integer[] indices = new Integer[length];
    for (int i = 0; i < length; i++) {
      indices[i] = i;
    }

    Arrays.sort(indices, (i1, i2) -> {
      double norm1 =
        ((array.temperatures[i1] - finalTempMin) / finalTempRange) * 100.0 +
        ((array.rainfalls[i1] - finalRainMin) / finalRainRange) * 100.0;
      double norm2 =
        ((array.temperatures[i2] - finalTempMin) / finalTempRange) * 100.0 +
        ((array.rainfalls[i2] - finalRainMin) / finalRainRange) * 100.0;
      return Double.compare(norm1, norm2);
    });

    float[] tempTemps = new float[length];
    float[] tempRains = new float[length];

    for (int i = 0; i < length; i++) {
      int originalIndex = indices[i];
      tempTemps[i] = array.temperatures[originalIndex];
      tempRains[i] = array.rainfalls[originalIndex];
    }

    System.arraycopy(tempTemps, 0, array.temperatures, 0, length);
    System.arraycopy(tempRains, 0, array.rainfalls, 0, length);
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
