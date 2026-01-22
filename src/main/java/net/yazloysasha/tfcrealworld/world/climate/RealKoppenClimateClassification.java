package net.yazloysasha.tfcrealworld.world.climate;

import javax.annotation.Nullable;
import net.dries007.tfc.util.climate.KoppenClimateClassification;

public class RealKoppenClimateClassification {

  public static @Nullable KoppenClimateClassification classify(
    float averageTemperature,
    float rainfall,
    float rainVar,
    boolean isInNorthernHemisphere
  ) {
    if (!isInNorthernHemisphere) {
      rainVar = -rainVar;
    }

    if (averageTemperature < -17f + 0.006 * rainfall) {
      return KoppenClimateClassification.EF;
    } else if (averageTemperature <= -12f) {
      return KoppenClimateClassification.ET;
    } else if (rainfall < 75f) {
      if (averageTemperature > 18f) {
        return KoppenClimateClassification.BWH;
      } else {
        return KoppenClimateClassification.BWK;
      }
    } else if (rainfall < 150f) {
      if (averageTemperature > 18) {
        return KoppenClimateClassification.BSH;
      } else {
        return KoppenClimateClassification.BSK;
      }
    } else if (averageTemperature > 21f) {
      if (rainfall * (1 + rainVar) > 600f) {
        return KoppenClimateClassification.AM;
      } else if (rainVar > 0.5f) {
        return KoppenClimateClassification.AW;
      } else if (rainVar < -0.5f) {
        return KoppenClimateClassification.AS;
      } else {
        return KoppenClimateClassification.AF;
      }
    }

    TemperateClassifier temperateClassifier = new TemperateClassifier(
      rainVar,
      rainfall
    );

    if (averageTemperature > 8f) {
      if (averageTemperature > 17f) {
        return temperateClassifier.classify(
          KoppenClimateClassification.CWA,
          KoppenClimateClassification.CSA,
          KoppenClimateClassification.CFA
        );
      } else if (averageTemperature > 12f) {
        return temperateClassifier.classify(
          KoppenClimateClassification.CWB,
          KoppenClimateClassification.CSB,
          KoppenClimateClassification.CFB
        );
      } else {
        return temperateClassifier.classify(
          KoppenClimateClassification.CWC,
          KoppenClimateClassification.CSC,
          KoppenClimateClassification.CFC
        );
      }
    } else if (averageTemperature > 3f) {
      return temperateClassifier.classify(
        KoppenClimateClassification.CWA,
        KoppenClimateClassification.CSA,
        KoppenClimateClassification.CFA
      );
    } else if (averageTemperature > -2f) {
      return temperateClassifier.classify(
        KoppenClimateClassification.DWB,
        KoppenClimateClassification.DSB,
        KoppenClimateClassification.DFB
      );
    } else if (averageTemperature > -8f) {
      return temperateClassifier.classify(
        KoppenClimateClassification.DWC,
        KoppenClimateClassification.DSC,
        KoppenClimateClassification.DFC
      );
    } else {
      return temperateClassifier.classify(
        KoppenClimateClassification.DWD,
        KoppenClimateClassification.DSD,
        KoppenClimateClassification.DFD
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

    private @Nullable KoppenClimateClassification classify(
      KoppenClimateClassification c1,
      KoppenClimateClassification c2,
      KoppenClimateClassification c3
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
