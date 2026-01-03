package net.yazloysasha.tfcrealworld.world.noise;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.dries007.tfc.util.climate.KoppenClimateClassification;
import org.slf4j.Logger;

/**
 * Caches valid parameter combinations for each Köppen climate classification.
 * Stores all valid combinations of (temperature, rainfall, rainVar) that lead to each climate,
 * ensuring that randomly selected parameters always produce the correct climate.
 *
 * Uses the same classification logic as KoppenClimateClassification.classify().
 * Based on the approach from maps.py: _build_climate_to_parameters_mapper().
 *
 * Memory-optimized: stores data as primitive arrays instead of objects to reduce memory footprint.
 */
public class KoppenParameterCache {

  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * Represents a valid parameter combination for a climate.
   * Stores temperature, rainfall, and rainVar as a single valid combination.
   * Uses float instead of double to reduce memory usage.
   */
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

  /**
   * Memory-efficient storage for parameter combinations.
   * Stores data as three parallel arrays (temperatures, rainfalls, rainVars)
   * instead of objects to reduce memory overhead.
   */
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

  private static KoppenParameterCache instance;
  private final Map<
    KoppenClimateClassification,
    ParameterArray
  > climateCombinations;

  // Cached base values and ranges for fast access
  private final Map<KoppenClimateClassification, Float> baseTemperatures;
  private final Map<KoppenClimateClassification, Float> baseRainfalls;
  private final Map<KoppenClimateClassification, Float> baseRainVars;
  private final Map<KoppenClimateClassification, float[]> temperatureRanges;
  private final Map<KoppenClimateClassification, float[]> rainfallRanges;
  private final Map<KoppenClimateClassification, float[]> rainVarRanges;

  private KoppenParameterCache() {
    this.climateCombinations = new HashMap<>();
    this.baseTemperatures = new HashMap<>();
    this.baseRainfalls = new HashMap<>();
    this.baseRainVars = new HashMap<>();
    this.temperatureRanges = new HashMap<>();
    this.rainfallRanges = new HashMap<>();
    this.rainVarRanges = new HashMap<>();
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
      LOGGER.info("Clearing Köppen parameter cache");
      instance = null;
    }
  }

  /**
   * Gets a random valid parameter combination for the given climate.
   */
  public ParameterCombination getRandomParameters(
    KoppenClimateClassification climate,
    long seed
  ) {
    ParameterArray combinations = climateCombinations.get(climate);
    if (combinations == null || combinations.temperatures.length == 0) {
      LOGGER.warn(
        "No parameter combinations found for climate: {}, using defaults",
        climate
      );
      return new ParameterCombination(5.0f, 100.0f, 0.0f);
    }

    long currentSeed = seed;
    currentSeed = (currentSeed * 1103515245L + 12345L) & 0x7fffffffL;
    int index = (int) (currentSeed % combinations.temperatures.length);
    return combinations.get(index);
  }

  /**
   * Gets parameter combination by index (0.0 to 1.0) for the given climate.
   * Index represents position in sorted parameter array, considering all 3 parameters.
   * Returns valid combination that belongs to the specified climate zone.
   * Uses linear interpolation between adjacent parameter combinations for smoother transitions.
   */
  public ParameterCombination getParametersByIndex(
    KoppenClimateClassification climate,
    double index
  ) {
    ParameterArray combinations = climateCombinations.get(climate);
    if (combinations == null || combinations.temperatures.length == 0) {
      LOGGER.warn(
        "No parameter combinations found for climate: {}, using defaults",
        climate
      );
      return new ParameterCombination(5.0f, 100.0f, 0.0f);
    }

    // Clamp index to [0.0, 1.0]
    index = Math.clamp(index, 0.0, 1.0);

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

    // Linear interpolation between two adjacent parameter combinations
    ParameterCombination lower = combinations.get(lowerIndex);
    ParameterCombination upper = combinations.get(upperIndex);

    float temp = (float) (lower.temperature +
      (upper.temperature - lower.temperature) * t);
    float rain = (float) (lower.rainfall +
      (upper.rainfall - lower.rainfall) * t);
    float rainVar = (float) (lower.rainVar +
      (upper.rainVar - lower.rainVar) * t);

    return new ParameterCombination(temp, rain, rainVar);
  }

  /**
   * Gets the base (average) temperature for the given climate.
   */
  public float getBaseTemperature(KoppenClimateClassification climate) {
    Float cached = baseTemperatures.get(climate);
    if (cached != null) {
      return cached;
    }
    // Fallback (should not happen after cache is built)
    LOGGER.warn(
      "No cached temperature found for climate: {}, using default",
      climate
    );
    return 5.0f;
  }

  /**
   * Gets the base (average) rainfall for the given climate.
   */
  public float getBaseRainfall(KoppenClimateClassification climate) {
    Float cached = baseRainfalls.get(climate);
    if (cached != null) {
      return cached;
    }
    // Fallback (should not happen after cache is built)
    LOGGER.warn(
      "No cached rainfall found for climate: {}, using default",
      climate
    );
    return 100.0f;
  }

  /**
   * Gets the base (average) rainfall variance for the given climate.
   */
  public float getBaseRainVar(KoppenClimateClassification climate) {
    Float cached = baseRainVars.get(climate);
    if (cached != null) {
      return cached;
    }
    // Fallback (should not happen after cache is built)
    LOGGER.warn(
      "No cached rainVar found for climate: {}, using default",
      climate
    );
    return 0.0f;
  }

  /**
   * Gets the minimum and maximum temperature values for the given climate.
   */
  public float[] getTemperatureRange(KoppenClimateClassification climate) {
    float[] cached = temperatureRanges.get(climate);
    if (cached != null) {
      return cached;
    }
    return new float[] { -20.0f, 30.0f };
  }

  /**
   * Gets the minimum and maximum rainfall values for the given climate.
   */
  public float[] getRainfallRange(KoppenClimateClassification climate) {
    float[] cached = rainfallRanges.get(climate);
    if (cached != null) {
      return cached;
    }
    return new float[] { 0.0f, 500.0f };
  }

  /**
   * Gets the minimum and maximum rainfall variance values for the given climate.
   */
  public float[] getRainVarRange(KoppenClimateClassification climate) {
    float[] cached = rainVarRanges.get(climate);
    if (cached != null) {
      return cached;
    }
    return new float[] { -1.0f, 1.0f };
  }

  /**
   * Builds the cache by analyzing all possible parameter combinations
   * and grouping them by resulting climate classification.
   * Stores all valid combinations for each climate, ensuring that randomly selected
   * parameters always produce the correct climate.
   * Based on _build_climate_to_parameters_mapper() from maps.py.
   */
  private void buildCache() {
    LOGGER.info("Building Köppen parameter cache...");

    float[] temperatures = generateRange(-20.0f, 30.0f, 0.1f);
    float[] rainfalls = generateRange(0.0f, 500.0f, 10.0f);
    float[] rainVars = generateRange(-1.0f, 1.0f, 0.1f);
    Map<KoppenClimateClassification, Integer> climateCounts = new HashMap<>();
    for (KoppenClimateClassification climate : KoppenClimateClassification.values()) {
      climateCounts.put(climate, 0);
    }

    int processed = 0;

    for (float temp : temperatures) {
      for (float rain : rainfalls) {
        for (float rainVar : rainVars) {
          KoppenClimateClassification climate =
            KoppenClimateClassification.classify(temp, rain, rainVar, true);
          climateCounts.put(climate, climateCounts.get(climate) + 1);
          processed++;
        }
      }
    }

    LOGGER.info("Processed {} parameter combinations", processed);

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

    // Sort parameters by 3 values: lowest first, then gradually increasing
    LOGGER.info(
      "Sorting parameter combinations by temperature, rainfall, and rainVar..."
    );
    for (KoppenClimateClassification climate : KoppenClimateClassification.values()) {
      ParameterArray array = climateCombinations.get(climate);
      if (array != null && array.temperatures.length > 0) {
        sortParameterArray(array);
      }
    }

    int totalCombinations = climateCombinations
      .values()
      .stream()
      .mapToInt(array -> array.temperatures.length)
      .sum();

    LOGGER.info("Parameter combinations by climate:");
    for (KoppenClimateClassification climate : KoppenClimateClassification.values()) {
      ParameterArray combinations = climateCombinations.get(climate);
      int count = combinations.temperatures.length;
      if (count > 0) {
        double percentage = (count / (double) totalCombinations) * 100.0;
        LOGGER.info(
          "  {}: {} combinations ({})%",
          climate,
          count,
          String.format("%.2f", percentage)
        );
      } else {
        LOGGER.warn("  {}: No valid combinations found", climate);
      }
    }

    LOGGER.info(
      "Created {} parameter combinations across {} climates",
      totalCombinations,
      climateCombinations.size()
    );

    LOGGER.info("Pre-computing base values and ranges...");
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
      } else {
        baseTemperatures.put(climate, 5.0f);
        temperatureRanges.put(climate, new float[] { -20.0f, 30.0f });
        baseRainfalls.put(climate, 100.0f);
        rainfallRanges.put(climate, new float[] { 0.0f, 500.0f });
        baseRainVars.put(climate, 0.0f);
        rainVarRanges.put(climate, new float[] { -1.0f, 1.0f });
      }
    }

    long estimatedBytes = totalCombinations * 12L;
    estimatedBytes += 6L * 30L * 100L;
    LOGGER.info(
      "Estimated memory usage: ~{} MB (optimized from ~{} MB using objects)",
      String.format("%.1f", estimatedBytes / (1024.0 * 1024.0)),
      String.format("%.1f", (totalCombinations * 56.0) / (1024.0 * 1024.0))
    );

    LOGGER.info("Köppen parameter cache built successfully");
  }

  /**
   * Sorts parameter array by all three parameters simultaneously with equal weights.
   * All parameters are normalized to [0, 1] range and then combined with equal weights (100 each).
   * This ensures smooth interpolation across all parameters, not just temperature.
   */
  private void sortParameterArray(ParameterArray array) {
    int length = array.temperatures.length;
    if (length <= 1) {
      return;
    }

    // Find ranges for normalization
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

    // Avoid division by zero
    float tempRange = tempMax - tempMin;
    float rainRange = rainMax - rainMin;
    float rainVarRange = rainVarMax - rainVarMin;
    if (tempRange < 0.001f) tempRange = 1.0f;
    if (rainRange < 0.001f) rainRange = 1.0f;
    if (rainVarRange < 0.001f) rainVarRange = 1.0f;

    // Create list of indices
    List<Integer> indices = new ArrayList<>(length);
    for (int i = 0; i < length; i++) {
      indices.add(i);
    }

    // Sort indices by combined normalized metric with equal weights (100 each)
    // All three parameters are normalized to [0, 1] and summed with equal weights
    final float finalTempMin = tempMin;
    final float finalTempRange = tempRange;
    final float finalRainMin = rainMin;
    final float finalRainRange = rainRange;
    final float finalRainVarMin = rainVarMin;
    final float finalRainVarRange = rainVarRange;

    Collections.sort(
      indices,
      Comparator.comparingDouble((Integer i) -> {
        // Normalize all three parameters to [0, 1] range
        double normTemp =
          (array.temperatures[i] - finalTempMin) / finalTempRange;
        double normRain = (array.rainfalls[i] - finalRainMin) / finalRainRange;
        double normRainVar =
          (array.rainVars[i] - finalRainVarMin) / finalRainVarRange;

        // Combine all three parameters with equal weights (100 each)
        // Use weighted sum for smooth interpolation across all parameters simultaneously
        return normTemp * 100.0 + normRain * 100.0 + normRainVar * 100.0;
      })
    );

    // Create temporary arrays
    float[] tempTemps = new float[length];
    float[] tempRains = new float[length];
    float[] tempRainVars = new float[length];

    // Reorder arrays according to sorted indices
    for (int i = 0; i < length; i++) {
      int originalIndex = indices.get(i);
      tempTemps[i] = array.temperatures[originalIndex];
      tempRains[i] = array.rainfalls[originalIndex];
      tempRainVars[i] = array.rainVars[originalIndex];
    }

    // Copy back
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
