package net.yazloysasha.tfcrealworld.world.noise;

import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.region.Units;

public class TransformedNoise2D implements Noise2D {

  private final Noise2D baseNoise;
  private final double offsetX;
  private final double offsetZ;
  private final int rotation;
  private final boolean looping;
  private final boolean loopX;
  private final boolean loopZ;
  private final double loopPeriodX;
  private final double loopPeriodZ;

  public TransformedNoise2D(
    Noise2D baseNoise,
    int offsetX,
    int offsetZ,
    int rotation,
    boolean looping,
    boolean loopX,
    boolean loopZ,
    int loopPeriodX,
    int loopPeriodZ
  ) {
    this.baseNoise = baseNoise;
    this.offsetX = offsetX / (double) Units.GRID_WIDTH_IN_BLOCK;
    this.offsetZ = offsetZ / (double) Units.GRID_WIDTH_IN_BLOCK;
    int normalizedRotation = rotation % 360;
    if (normalizedRotation < 0) {
      normalizedRotation += 360;
    }
    if (normalizedRotation <= 45 || normalizedRotation > 315) {
      this.rotation = 0;
    } else if (normalizedRotation <= 135) {
      this.rotation = 90;
    } else if (normalizedRotation <= 225) {
      this.rotation = 180;
    } else {
      this.rotation = 270;
    }
    this.looping = looping;
    this.loopX = loopX;
    this.loopZ = loopZ;
    this.loopPeriodX = loopPeriodX / (double) Units.GRID_WIDTH_IN_BLOCK;
    this.loopPeriodZ = loopPeriodZ / (double) Units.GRID_WIDTH_IN_BLOCK;
  }

  @Override
  public double noise(double x, double z) {
    double tx = x + offsetX;
    double tz = z + offsetZ;

    double rx = tx;
    double rz = tz;
    switch (rotation) {
      case 90:
        rx = tz;
        rz = -tx;
        break;
      case 180:
        rx = -tx;
        rz = -tz;
        break;
      case 270:
        rx = -tz;
        rz = tx;
        break;
    }

    double lx = rx;
    double lz = rz;
    if (looping) {
      if (loopX && loopPeriodX > 0) {
        lx = rx % loopPeriodX;
        if (lx < 0) {
          lx += loopPeriodX;
        }
      }
      if (loopZ && loopPeriodZ > 0) {
        lz = rz % loopPeriodZ;
        if (lz < 0) {
          lz += loopPeriodZ;
        }
      }
    }

    return baseNoise.noise(lx, lz);
  }
}
