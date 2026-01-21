package net.yazloysasha.tfcrealworld.world.noise.koppen;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.dries007.tfc.util.climate.KoppenClimateClassification;
import net.minecraft.util.Mth;

/**
 * Caches valid parameter combinations for each Köppen climate classification.
 */
public class KoppenParameterCache {

  public static class ParameterCombination {

    public final float temperature;
    public final float rainfall;
    public final float rainVar;

    public ParameterCombination(
      float temperature,
      float rainfall,
      float rainVar
    ) {
      this.temperature = temperature;
      this.rainfall = rainfall;
      this.rainVar = rainVar;
    }
  }

  private static class ParameterArray {

    final float[] temperatures;
    final float[] rainfalls;
    final float[] rainVars;

    ParameterArray(int size) {
      this.temperatures = new float[size];
      this.rainfalls = new float[size];
      this.rainVars = new float[size];
    }

    ParameterCombination get(int index) {
      return new ParameterCombination(
        temperatures[index],
        rainfalls[index],
        rainVars[index]
      );
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

  private final Map<KoppenClimateClassification, float[]> temperatureRanges;
  private final Map<KoppenClimateClassification, float[]> rainfallRanges;
  private final Map<KoppenClimateClassification, float[]> rainVarRanges;
  private final Map<KoppenClimateClassification, ParameterGrid> parameterGrids;

  private KoppenParameterCache() {
    this.climateCombinations = new HashMap<>();
    this.temperatureRanges = new HashMap<>();
    this.rainfallRanges = new HashMap<>();
    this.rainVarRanges = new HashMap<>();
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
      return new ParameterCombination(
        ClimateConstants.DEFAULT_TEMP,
        ClimateConstants.DEFAULT_RAIN,
        ClimateConstants.DEFAULT_RAINVAR
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
        ClimateConstants.DEFAULT_RAIN,
        ClimateConstants.DEFAULT_RAINVAR
      );
    }

    return result;
  }

  public float[] getTemperatureRange(KoppenClimateClassification climate) {
    float[] cached = temperatureRanges.get(climate);
    if (cached != null) {
      return cached;
    }
    return new float[] { ClimateConstants.TEMP_MIN, ClimateConstants.TEMP_MAX };
  }

  public float[] getRainfallRange(KoppenClimateClassification climate) {
    float[] cached = rainfallRanges.get(climate);
    if (cached != null) {
      return cached;
    }
    return new float[] { ClimateConstants.RAIN_MIN, ClimateConstants.RAIN_MAX };
  }

  public float[] getRainVarRange(KoppenClimateClassification climate) {
    float[] cached = rainVarRanges.get(climate);
    if (cached != null) {
      return cached;
    }
    return new float[] {
      ClimateConstants.RAINVAR_MIN,
      ClimateConstants.RAINVAR_MAX,
    };
  }

  public float[] getRainVarRangeForCombination(
    KoppenClimateClassification climate,
    float temperature,
    float rainfall
  ) {
    ParameterArray combinations = climateCombinations.get(climate);
    if (combinations == null || combinations.temperatures.length == 0) {
      return new float[] {
        ClimateConstants.RAINVAR_MIN,
        ClimateConstants.RAINVAR_MAX,
      };
    }

    float minRainVar = Float.MAX_VALUE;
    float maxRainVar = Float.MIN_VALUE;

    float tempTolerance = ClimateConstants.TEMP_TOLERANCE;
    float rainTolerance = ClimateConstants.RAIN_TOLERANCE;

    for (int i = 0; i < combinations.temperatures.length; i++) {
      float tempDiff = Math.abs(combinations.temperatures[i] - temperature);
      float rainDiff = Math.abs(combinations.rainfalls[i] - rainfall);

      if (tempDiff <= tempTolerance && rainDiff <= rainTolerance) {
        if (combinations.rainVars[i] < minRainVar) {
          minRainVar = combinations.rainVars[i];
        }
        if (combinations.rainVars[i] > maxRainVar) {
          maxRainVar = combinations.rainVars[i];
        }
      }
    }

    if (minRainVar == Float.MAX_VALUE || maxRainVar == Float.MIN_VALUE) {
      return getRainVarRange(climate);
    }

    return new float[] { minRainVar, maxRainVar };
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

    Map<KoppenClimateClassification, Integer> climateCounts = new HashMap<>();
    for (KoppenClimateClassification climate : KoppenClimateClassification.values()) {
      climateCounts.put(climate, 0);
    }

    for (float temp : temperatures) {
      for (float rain : rainfalls) {
        for (float rainVar : rainVars) {
          KoppenClimateClassification climate =
            KoppenClimateClassification.classify(temp, rain, rainVar, true);
          climateCounts.merge(climate, 1, Integer::sum);
        }
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
        for (float rainVar : rainVars) {
          KoppenClimateClassification climate =
            KoppenClimateClassification.classify(temp, rain, rainVar, true);
          ParameterArray array = climateCombinations.get(climate);
          int index = climateIndices.get(climate);
          array.temperatures[index] = temp;
          array.rainfalls[index] = rain;
          array.rainVars[index] = rainVar;
          climateIndices.put(climate, index + 1);
        }
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
    float tempMin = Float.MAX_VALUE, tempMax = Float.MIN_VALUE;
    float rainMin = Float.MAX_VALUE, rainMax = Float.MIN_VALUE;
    float rainVarMin = Float.MAX_VALUE, rainVarMax = Float.MIN_VALUE;

    for (int i = 0; i < combinations.temperatures.length; i++) {
      float temp = combinations.temperatures[i];
      float rain = combinations.rainfalls[i];
      float rainVar = combinations.rainVars[i];

      if (temp < tempMin) tempMin = temp;
      if (temp > tempMax) tempMax = temp;

      if (rain < rainMin) rainMin = rain;
      if (rain > rainMax) rainMax = rain;

      if (rainVar < rainVarMin) rainVarMin = rainVar;
      if (rainVar > rainVarMax) rainVarMax = rainVar;
    }

    temperatureRanges.put(climate, new float[] { tempMin, tempMax });
    rainfallRanges.put(climate, new float[] { rainMin, rainMax });
    rainVarRanges.put(climate, new float[] { rainVarMin, rainVarMax });
  }

  private void setDefaultStatistics(KoppenClimateClassification climate) {
    temperatureRanges.put(
      climate,
      new float[] { ClimateConstants.TEMP_MIN, ClimateConstants.TEMP_MAX }
    );
    rainfallRanges.put(
      climate,
      new float[] { ClimateConstants.RAIN_MIN, ClimateConstants.RAIN_MAX }
    );
    rainVarRanges.put(
      climate,
      new float[] { ClimateConstants.RAINVAR_MIN, ClimateConstants.RAINVAR_MAX }
    );
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
    float rainVarMin = Float.MAX_VALUE, rainVarMax = Float.MIN_VALUE;

    for (int i = 0; i < length; i++) {
      float temp = array.temperatures[i];
      float rain = array.rainfalls[i];
      float rainVar = array.rainVars[i];

      if (temp < tempMin) tempMin = temp;
      if (temp > tempMax) tempMax = temp;
      if (rain < rainMin) rainMin = rain;
      if (rain > rainMax) rainMax = rain;
      if (rainVar < rainVarMin) rainVarMin = rainVar;
      if (rainVar > rainVarMax) rainVarMax = rainVar;
    }

    final float finalTempMin = tempMin;
    final float finalTempRange = Math.max(tempMax - tempMin, 0.001f);
    final float finalRainMin = rainMin;
    final float finalRainRange = Math.max(rainMax - rainMin, 0.001f);
    final float finalRainVarMin = rainVarMin;
    final float finalRainVarRange = Math.max(rainVarMax - rainVarMin, 0.001f);

    Integer[] indices = new Integer[length];
    for (int i = 0; i < length; i++) {
      indices[i] = i;
    }

    Arrays.sort(indices, (i1, i2) -> {
      double norm1 =
        ((array.temperatures[i1] - finalTempMin) / finalTempRange) * 100.0 +
        ((array.rainfalls[i1] - finalRainMin) / finalRainRange) * 100.0 +
        ((array.rainVars[i1] - finalRainVarMin) / finalRainVarRange) * 100.0;
      double norm2 =
        ((array.temperatures[i2] - finalTempMin) / finalTempRange) * 100.0 +
        ((array.rainfalls[i2] - finalRainMin) / finalRainRange) * 100.0 +
        ((array.rainVars[i2] - finalRainVarMin) / finalRainVarRange) * 100.0;
      return Double.compare(norm1, norm2);
    });

    float[] tempTemps = new float[length];
    float[] tempRains = new float[length];
    float[] tempRainVars = new float[length];

    for (int i = 0; i < length; i++) {
      int originalIndex = indices[i];
      tempTemps[i] = array.temperatures[originalIndex];
      tempRains[i] = array.rainfalls[originalIndex];
      tempRainVars[i] = array.rainVars[originalIndex];
    }

    System.arraycopy(tempTemps, 0, array.temperatures, 0, length);
    System.arraycopy(tempRains, 0, array.rainfalls, 0, length);
    System.arraycopy(tempRainVars, 0, array.rainVars, 0, length);
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
