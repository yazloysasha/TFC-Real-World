package net.yazloysasha.tfcrealworld.world.climate;

import javax.annotation.Nullable;

public final class TfeKoppenClassification {

  private TfeKoppenClassification() {}

  public static @Nullable RealKoppenClimateClassification classify(
    float averageTemperature,
    float rainfall,
    float rainVar,
    boolean isInNorthernHemisphere
  ) {
    if (!isInNorthernHemisphere) {
      rainVar = -rainVar;
    }

    if (averageTemperature < -17f + 0.006 * rainfall) {
      return RealKoppenClimateClassification.EF;
    } else if (averageTemperature <= -12f) {
      return RealKoppenClimateClassification.ET;
    } else if (rainfall < 75f) {
      if (averageTemperature > 18f) {
        return RealKoppenClimateClassification.BWH;
      } else {
        return RealKoppenClimateClassification.BWK;
      }
    } else if (rainfall < 150f) {
      if (averageTemperature > 18) {
        return RealKoppenClimateClassification.BSH;
      } else {
        return RealKoppenClimateClassification.BSK;
      }
    } else if (averageTemperature > 21f) {
      if (rainfall * (1 + rainVar) > 600f) {
        return RealKoppenClimateClassification.AM;
      } else if (rainVar > 0.5f) {
        return RealKoppenClimateClassification.AW;
      } else if (rainVar < -0.5f) {
        return RealKoppenClimateClassification.AS;
      } else {
        return RealKoppenClimateClassification.AF;
      }
    }

    TemperateClassifier temperateClassifier = new TemperateClassifier(
      rainVar,
      rainfall
    );

    if (averageTemperature > 8f) {
      if (averageTemperature > 17f) {
        return temperateClassifier.classify(
          RealKoppenClimateClassification.CWA,
          RealKoppenClimateClassification.CSA,
          RealKoppenClimateClassification.CFA
        );
      } else if (averageTemperature > 12f) {
        return temperateClassifier.classify(
          RealKoppenClimateClassification.CWB,
          RealKoppenClimateClassification.CSB,
          RealKoppenClimateClassification.CFB
        );
      } else {
        return temperateClassifier.classify(
          RealKoppenClimateClassification.CWC,
          RealKoppenClimateClassification.CSC,
          RealKoppenClimateClassification.CFC
        );
      }
    } else if (averageTemperature > 3f) {
      return temperateClassifier.classify(
        RealKoppenClimateClassification.DWA,
        RealKoppenClimateClassification.DSA,
        RealKoppenClimateClassification.DFA
      );
    } else if (averageTemperature > -2f) {
      return temperateClassifier.classify(
        RealKoppenClimateClassification.DWB,
        RealKoppenClimateClassification.DSB,
        RealKoppenClimateClassification.DFB
      );
    } else if (averageTemperature > -8f) {
      return temperateClassifier.classify(
        RealKoppenClimateClassification.DWC,
        RealKoppenClimateClassification.DSC,
        RealKoppenClimateClassification.DFC
      );
    } else {
      return temperateClassifier.classify(
        RealKoppenClimateClassification.DWD,
        RealKoppenClimateClassification.DSD,
        RealKoppenClimateClassification.DFD
      );
    }
  }

  private static class TemperateClassifier {

    TemperateClassifier(float rainVar, float rainfall) {
      this.rainVar = rainVar;
      this.rainfall = rainfall;
    }

    private final float rainVar;
    private final float rainfall;

    private @Nullable RealKoppenClimateClassification classify(
      RealKoppenClimateClassification c1,
      RealKoppenClimateClassification c2,
      RealKoppenClimateClassification c3
    ) {
      if (rainVar > 0.5f && rainfall > 315f) return c1;
      if (rainVar < -0.5f && rainfall < 175f) return c2;
      if (
        rainVar <= 0.5f &&
        rainVar >= -0.5f &&
        rainfall <= 315f &&
        rainfall >= 175f
      ) return c3;
      return null;
    }
  }
}
