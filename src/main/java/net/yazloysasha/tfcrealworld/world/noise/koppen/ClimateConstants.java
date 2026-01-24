package net.yazloysasha.tfcrealworld.world.noise.koppen;

/**
 * Climate constants for the Köppen climate classification system.
 */
public final class ClimateConstants {

  private ClimateConstants() {}

  public static final float TEMP_MIN = -25.0f;
  public static final float TEMP_MAX = 30.0f;

  public static final float RAIN_MIN = 0.0f;
  public static final float RAIN_MAX = 500.0f;

  public static final float RAINVAR_MIN = -1.0f;
  public static final float RAINVAR_MAX = 1.0f;

  public static final float DEFAULT_TEMP = 2.5f;
  public static final float DEFAULT_RAIN = 250.0f;
  public static final float DEFAULT_RAINVAR = 0.0f;

  public static final float TEMP_STEP = 1.0f;
  public static final float RAIN_STEP = 10.0f;
  public static final float RAINVAR_STEP = 0.1f;

  public static float TEMP_TOLERANCE = 2.0f;
  public static float RAIN_TOLERANCE = 20.0f;
  public static float RAINVAR_TOLERANCE = 0.5f;
}
