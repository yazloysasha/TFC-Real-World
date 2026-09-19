package su.terrafirmagreg.core.world.new_ow_wg.noise;

public class TFGCellular2D {

  public TFGCellular2D(long seed) {}

  public TFGCellular2D(long seed, float jitter, int sample) {}

  public TFGCellular2D spread(double scaleFactor) {
    return this;
  }

  public TFGCell cell(double x, double y) {
    return new TFGCell(0, 0, 0, 0, 0, 0, 0, 0);
  }

  public record TFGCell(
    double x,
    double y,
    int cx,
    int cy,
    double f1,
    double f2,
    double noise,
    double angle
  ) {}
}
