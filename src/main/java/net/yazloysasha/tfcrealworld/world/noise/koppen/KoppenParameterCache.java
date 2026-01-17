package net.yazloysasha.tfcrealworld.world.noise.koppen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.dries007.tfc.util.climate.KoppenClimateClassification;
import net.minecraft.util.Mth;
import net.yazloysasha.tfcrealworld.world.climate.KoppenClimateCode;

/**
 * Caches valid parameter combinations for each Köppen climate classification.
 * When a Köppen code maps to multiple TFC zones, parameters are collected from all zones proportionally.
 */
public class KoppenParameterCache {

  /**
   * Represents a valid parameter combination for a climate.
   */
  public static class ParameterCombination {

    public final float temperature;
    public final float rainfall;

    public ParameterCombination(float temperature, float rainfall) {
      this.temperature = temperature;
      this.rainfall = rainfall;
    }
  }

  /**
   * Memory-efficient storage for parameter combinations.
   */
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

  private static KoppenParameterCache instance;
  private final Map<KoppenClimateCode, ParameterArray> climateCombinations;

  private final Map<KoppenClimateCode, Float> baseTemperatures;
  private final Map<KoppenClimateCode, Float> baseRainfalls;
  private final Map<KoppenClimateCode, float[]> temperatureRanges;
  private final Map<KoppenClimateCode, float[]> rainfallRanges;

  private KoppenParameterCache() {
    this.climateCombinations = new HashMap<>();
    this.baseTemperatures = new HashMap<>();
    this.baseRainfalls = new HashMap<>();
    this.temperatureRanges = new HashMap<>();
    this.rainfallRanges = new HashMap<>();
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

  public ParameterCombination getRandomParameters(
    KoppenClimateCode climate,
    long seed
  ) {
    ParameterArray combinations = climateCombinations.get(climate);
    if (combinations == null || combinations.temperatures.length == 0) {
      return new ParameterCombination(5.0f, 100.0f);
    }

    long currentSeed = seed;
    currentSeed = (currentSeed * 1103515245L + 12345L) & 0x7fffffffL;
    int index = (int) (currentSeed % combinations.temperatures.length);
    return combinations.get(index);
  }

  public ParameterCombination getParametersByIndex(
    KoppenClimateCode climate,
    double index
  ) {
    ParameterArray combinations = climateCombinations.get(climate);
    if (combinations == null || combinations.temperatures.length == 0) {
      return new ParameterCombination(5.0f, 100.0f);
    }

    index = Mth.clamp(index, 0.0, 1.0);

    int arrayLength = combinations.temperatures.length;

    if (arrayLength == 1) {
      return combinations.get(0);
    }

    double exactPosition = index * (arrayLength - 1);
    int lowerIndex = (int) Math.floor(exactPosition);
    int upperIndex = Math.min(lowerIndex + 1, arrayLength - 1);

    double t = exactPosition - lowerIndex;

    if (t < 0.001 || lowerIndex == upperIndex) {
      return combinations.get(lowerIndex);
    }

    ParameterCombination lower = combinations.get(lowerIndex);
    ParameterCombination upper = combinations.get(upperIndex);

    float temp = (float) (lower.temperature +
      (upper.temperature - lower.temperature) * t);
    float rain = (float) (lower.rainfall +
      (upper.rainfall - lower.rainfall) * t);

    return new ParameterCombination(temp, rain);
  }

  public float getBaseTemperature(KoppenClimateCode climate) {
    Float cached = baseTemperatures.get(climate);
    if (cached != null) {
      return cached;
    }
    return 5.0f;
  }

  public float getBaseRainfall(KoppenClimateCode climate) {
    Float cached = baseRainfalls.get(climate);
    if (cached != null) {
      return cached;
    }
    return 100.0f;
  }

  public float[] getTemperatureRange(KoppenClimateCode climate) {
    float[] cached = temperatureRanges.get(climate);
    if (cached != null) {
      return cached;
    }
    return new float[] { -20.0f, 30.0f };
  }

  public float[] getRainfallRange(KoppenClimateCode climate) {
    float[] cached = rainfallRanges.get(climate);
    if (cached != null) {
      return cached;
    }
    return new float[] { 0.0f, 500.0f };
  }

  /**
   * Builds the cache by analyzing all possible parameter combinations
   * and grouping them by resulting climate classification.
   * Stores all valid combinations for each climate, ensuring that randomly selected
   * parameters always produce the correct climate.
   *
   * When a Köppen code maps to multiple TFC zones, collects parameters from all zones
   * proportionally (e.g., if a code maps to [ZONE_A, ZONE_A, ZONE_B], parameters from
   * ZONE_A will appear twice as often as from ZONE_B).
   */
  private void buildCache() {
    float[] temperatures = generateRange(-20.0f, 30.0f, 1.0f);
    float[] rainfalls = generateRange(0.0f, 500.0f, 10.0f);

    Map<
      KoppenClimateClassification,
      List<ParameterCombination>
    > tfcZoneToParams = new HashMap<>();
    for (KoppenClimateClassification zone : KoppenClimateClassification.values()) {
      tfcZoneToParams.put(zone, new ArrayList<>());
    }

    for (float temp : temperatures) {
      for (float rain : rainfalls) {
        KoppenClimateClassification tfcZone =
          KoppenClimateClassification.classify(temp, rain);
        tfcZoneToParams.get(tfcZone).add(new ParameterCombination(temp, rain));
      }
    }

    for (KoppenClimateCode koppenCode : KoppenClimateCode.values()) {
      KoppenClimateClassification[] tfcZones = koppenCode.getTFCClimates();

      List<ParameterCombination> allParams = new ArrayList<>();
      for (KoppenClimateClassification zone : tfcZones) {
        List<ParameterCombination> zoneParams = tfcZoneToParams.get(zone);
        if (zoneParams != null) {
          allParams.addAll(zoneParams);
        }
      }

      if (allParams.isEmpty()) {
        climateCombinations.put(koppenCode, new ParameterArray(0));
        continue;
      }

      ParameterArray array = new ParameterArray(allParams.size());
      for (int i = 0; i < allParams.size(); i++) {
        ParameterCombination param = allParams.get(i);
        array.temperatures[i] = param.temperature;
        array.rainfalls[i] = param.rainfall;
      }

      sortParameterArray(array);

      climateCombinations.put(koppenCode, array);
    }

    for (KoppenClimateCode climate : KoppenClimateCode.values()) {
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
      } else {
        baseTemperatures.put(climate, 5.0f);
        temperatureRanges.put(climate, new float[] { -20.0f, 30.0f });
        baseRainfalls.put(climate, 100.0f);
        rainfallRanges.put(climate, new float[] { 0.0f, 500.0f });
      }
    }
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

    for (int i = 0; i < length; i++) {
      if (array.temperatures[i] < tempMin) tempMin = array.temperatures[i];
      if (array.temperatures[i] > tempMax) tempMax = array.temperatures[i];
      if (array.rainfalls[i] < rainMin) rainMin = array.rainfalls[i];
      if (array.rainfalls[i] > rainMax) rainMax = array.rainfalls[i];
    }

    float tempRange = tempMax - tempMin;
    float rainRange = rainMax - rainMin;
    if (tempRange < 0.001f) tempRange = 1.0f;
    if (rainRange < 0.001f) rainRange = 1.0f;

    List<Integer> indices = new ArrayList<>(length);
    for (int i = 0; i < length; i++) {
      indices.add(i);
    }

    final float finalTempMin = tempMin;
    final float finalTempRange = tempRange;
    final float finalRainMin = rainMin;
    final float finalRainRange = rainRange;

    Collections.sort(
      indices,
      Comparator.comparingDouble((Integer i) -> {
        double normTemp =
          (array.temperatures[i] - finalTempMin) / finalTempRange;
        double normRain = (array.rainfalls[i] - finalRainMin) / finalRainRange;

        return normTemp * 100.0 + normRain * 100.0;
      })
    );

    float[] tempTemps = new float[length];
    float[] tempRains = new float[length];

    for (int i = 0; i < length; i++) {
      int originalIndex = indices.get(i);
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
