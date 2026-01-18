package net.yazloysasha.tfcrealworld.world.noise.koppen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
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

  private final Map<KoppenClimateClassification, Float> baseTemperatures;
  private final Map<KoppenClimateClassification, Float> baseRainfalls;
  private final Map<KoppenClimateClassification, Float> baseRainVars;
  private final Map<KoppenClimateClassification, float[]> temperatureRanges;
  private final Map<KoppenClimateClassification, float[]> rainfallRanges;
  private final Map<KoppenClimateClassification, float[]> rainVarRanges;
  private final Map<KoppenClimateClassification, ParameterGrid> parameterGrids;

  private KoppenParameterCache() {
    this.climateCombinations = new HashMap<>();
    this.baseTemperatures = new HashMap<>();
    this.baseRainfalls = new HashMap<>();
    this.baseRainVars = new HashMap<>();
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
      return new ParameterCombination(5.0f, 100.0f, 0.0f);
    }

    double normalizedTemp = Mth.clamp(temperatureGrayscale / 255.0, 0.0, 1.0);
    double normalizedRain = Mth.clamp(rainfallGrayscale / 255.0, 0.0, 1.0);

    int tempIndex = (int) Math.round(normalizedTemp * (grid.gridSize - 1));
    int rainIndex = (int) Math.round(normalizedRain * (grid.gridSize - 1));

    tempIndex = Mth.clamp(tempIndex, 0, grid.gridSize - 1);
    rainIndex = Mth.clamp(rainIndex, 0, grid.gridSize - 1);

    ParameterCombination result = grid.get(tempIndex, rainIndex);

    if (result == null) {
      return new ParameterCombination(5.0f, 100.0f, 0.0f);
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

  public float getBaseRainVar(KoppenClimateClassification climate) {
    Float cached = baseRainVars.get(climate);
    if (cached != null) {
      return cached;
    }
    return 0.0f;
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

  public float[] getRainVarRange(KoppenClimateClassification climate) {
    float[] cached = rainVarRanges.get(climate);
    if (cached != null) {
      return cached;
    }
    return new float[] { -1.0f, 1.0f };
  }

  public float[] getRainVarRangeForCombination(
    KoppenClimateClassification climate,
    float temperature,
    float rainfall
  ) {
    ParameterArray combinations = climateCombinations.get(climate);
    if (combinations == null || combinations.temperatures.length == 0) {
      return new float[] { -1.0f, 1.0f };
    }

    float minRainVar = Float.MAX_VALUE;
    float maxRainVar = Float.MIN_VALUE;

    float tempTolerance = 1.0f;
    float rainTolerance = 10.0f;

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
    float[] temperatures = generateRange(-20.0f, 30.0f, 1.0f);
    float[] rainfalls = generateRange(0.0f, 500.0f, 10.0f);
    float[] rainVars = generateRange(-1.0f, 1.0f, 0.1f);

    Map<KoppenClimateClassification, Integer> climateCounts = new HashMap<>();
    for (KoppenClimateClassification climate : KoppenClimateClassification.values()) {
      climateCounts.put(climate, 0);
    }

    for (float temp : temperatures) {
      for (float rain : rainfalls) {
        for (float rainVar : rainVars) {
          KoppenClimateClassification climate =
            KoppenClimateClassification.classify(temp, rain, rainVar, true);
          climateCounts.put(climate, climateCounts.get(climate) + 1);
        }
      }
    }

    for (Map.Entry<
      KoppenClimateClassification,
      Integer
    > entry : climateCounts.entrySet()) {
      climateCombinations.put(
        entry.getKey(),
        new ParameterArray(entry.getValue())
      );
    }

    Map<KoppenClimateClassification, Integer> climateIndices = new HashMap<>();
    for (KoppenClimateClassification climate : KoppenClimateClassification.values()) {
      climateIndices.put(climate, 0);
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
      }
    }

    for (KoppenClimateClassification climate : KoppenClimateClassification.values()) {
      ParameterArray combinations = climateCombinations.get(climate);
      if (combinations != null && combinations.temperatures.length > 0) {
        float tempSum = 0.0f;
        float tempMin = Float.MAX_VALUE;
        float tempMax = Float.MIN_VALUE;
        for (float temp : combinations.temperatures) {
          tempSum += temp;
          if (temp < tempMin) tempMin = temp;
          if (temp > tempMax) tempMax = temp;
        }
        baseTemperatures.put(
          climate,
          tempSum / combinations.temperatures.length
        );
        temperatureRanges.put(climate, new float[] { tempMin, tempMax });

        float rainSum = 0.0f;
        float rainMin = Float.MAX_VALUE;
        float rainMax = Float.MIN_VALUE;
        for (float rain : combinations.rainfalls) {
          rainSum += rain;
          if (rain < rainMin) rainMin = rain;
          if (rain > rainMax) rainMax = rain;
        }
        baseRainfalls.put(climate, rainSum / combinations.rainfalls.length);
        rainfallRanges.put(climate, new float[] { rainMin, rainMax });

        float rainVarSum = 0.0f;
        float rainVarMin = Float.MAX_VALUE;
        float rainVarMax = Float.MIN_VALUE;
        for (float rainVar : combinations.rainVars) {
          rainVarSum += rainVar;
          if (rainVar < rainVarMin) rainVarMin = rainVar;
          if (rainVar > rainVarMax) rainVarMax = rainVar;
        }
        baseRainVars.put(climate, rainVarSum / combinations.rainVars.length);
        rainVarRanges.put(climate, new float[] { rainVarMin, rainVarMax });

        parameterGrids.put(climate, buildParameterGrid(climate, combinations));
      } else {
        baseTemperatures.put(climate, 5.0f);
        temperatureRanges.put(climate, new float[] { -20.0f, 30.0f });
        baseRainfalls.put(climate, 100.0f);
        rainfallRanges.put(climate, new float[] { 0.0f, 500.0f });
        baseRainVars.put(climate, 0.0f);
        rainVarRanges.put(climate, new float[] { -1.0f, 1.0f });
      }
    }
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

    float tempSpan = tempRange[1] - tempRange[0];
    float rainSpan = rainRange[1] - rainRange[0];

    if (tempSpan < 0.001f) tempSpan = 1.0f;
    if (rainSpan < 0.001f) rainSpan = 1.0f;

    for (int i = 0; i < combinations.temperatures.length; i++) {
      float normTempDiff =
        (combinations.temperatures[i] - targetTemp) / tempSpan;
      float normRainDiff = (combinations.rainfalls[i] - targetRain) / rainSpan;
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

    float tempMin = Float.MAX_VALUE;
    float tempMax = Float.MIN_VALUE;
    float rainMin = Float.MAX_VALUE;
    float rainMax = Float.MIN_VALUE;
    float rainVarMin = Float.MAX_VALUE;
    float rainVarMax = Float.MIN_VALUE;

    for (int i = 0; i < length; i++) {
      if (array.temperatures[i] < tempMin) tempMin = array.temperatures[i];
      if (array.temperatures[i] > tempMax) tempMax = array.temperatures[i];
      if (array.rainfalls[i] < rainMin) rainMin = array.rainfalls[i];
      if (array.rainfalls[i] > rainMax) rainMax = array.rainfalls[i];
      if (array.rainVars[i] < rainVarMin) rainVarMin = array.rainVars[i];
      if (array.rainVars[i] > rainVarMax) rainVarMax = array.rainVars[i];
    }

    float tempRange = tempMax - tempMin;
    float rainRange = rainMax - rainMin;
    float rainVarRange = rainVarMax - rainVarMin;
    if (tempRange < 0.001f) tempRange = 1.0f;
    if (rainRange < 0.001f) rainRange = 1.0f;
    if (rainVarRange < 0.001f) rainVarRange = 1.0f;

    List<Integer> indices = new ArrayList<>(length);
    for (int i = 0; i < length; i++) {
      indices.add(i);
    }

    final float finalTempMin = tempMin;
    final float finalTempRange = tempRange;
    final float finalRainMin = rainMin;
    final float finalRainRange = rainRange;
    final float finalRainVarMin = rainVarMin;
    final float finalRainVarRange = rainVarRange;

    Collections.sort(
      indices,
      Comparator.comparingDouble((Integer i) -> {
        double normTemp =
          (array.temperatures[i] - finalTempMin) / finalTempRange;
        double normRain = (array.rainfalls[i] - finalRainMin) / finalRainRange;
        double normRainVar =
          (array.rainVars[i] - finalRainVarMin) / finalRainVarRange;

        return normTemp * 100.0 + normRain * 100.0 + normRainVar * 100.0;
      })
    );

    float[] tempTemps = new float[length];
    float[] tempRains = new float[length];
    float[] tempRainVars = new float[length];

    for (int i = 0; i < length; i++) {
      int originalIndex = indices.get(i);
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
