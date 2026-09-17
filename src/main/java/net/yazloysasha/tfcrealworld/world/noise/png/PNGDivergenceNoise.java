package net.yazloysasha.tfcrealworld.world.noise.png;

import org.jetbrains.annotations.Nullable;

/** PB2002 plate boundaries: 128 neutral, bright divergent, dark convergent. Nearest-neighbour sample. */
public class PNGDivergenceNoise extends BasePNGNoise {

  private static final String MAP_NAME = "divergence";
  private static final int NEUTRAL = 128;
  private static final int DEAD_ZONE = 24;
  private static final float MAX_MAGNITUDE = 2f;

  private PNGDivergenceNoise(int horizontalScale, int verticalScale) {
    super(
      horizontalScale,
      verticalScale,
      MAP_NAME,
      "Failed to load divergence map."
    );
  }

  @Nullable
  public static PNGDivergenceNoise tryCreate(
    int horizontalScale,
    int verticalScale
  ) {
    if (loadImage(MAP_NAME) == null) {
      return null;
    }
    return new PNGDivergenceNoise(horizontalScale, verticalScale);
  }

  @Override
  protected double transformBrightness(double brightness) {
    return grayToDivergence((int) Math.round(brightness));
  }

  public float getDivergence(double x, double z) {
    return grayToDivergence(sampleGrayAtWorldRounded(x, z));
  }

  private static float grayToDivergence(int gray) {
    int delta = gray - NEUTRAL;
    if (Math.abs(delta) <= DEAD_ZONE) {
      return 0f;
    }
    if (delta > 0) {
      float t =
        (gray - (NEUTRAL + DEAD_ZONE)) / (float) (255 - NEUTRAL - DEAD_ZONE);
      return Math.clamp(t, 0f, 1f) * MAX_MAGNITUDE;
    }
    float t = (NEUTRAL - DEAD_ZONE - gray) / (float) (NEUTRAL - DEAD_ZONE);
    return -Math.clamp(t, 0f, 1f) * MAX_MAGNITUDE;
  }
}
