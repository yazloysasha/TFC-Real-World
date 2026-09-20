package com.newterraearth.tfe.world.noise;

public class NTECellular2D {

  public NTECellular2D(long seed) {}

  public NTECellular2D(long seed, int sample) {}

  public NTECellular2D(long seed, float jitter, int sample) {}

  public NTECellular2D spread(double scaleFactor) {
    return this;
  }

  public Cell cell(double x, double y) {
    return new Cell(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
  }

  public record Cell(
    double x,
    double y,
    int cx,
    int cy,
    double nx,
    double ny,
    double f1,
    double f2,
    double noise,
    double angle
  ) {}
}
