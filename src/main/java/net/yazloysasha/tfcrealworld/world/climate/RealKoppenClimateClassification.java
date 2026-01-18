package net.yazloysasha.tfcrealworld.world.climate;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;

public enum RealKoppenClimateClassification implements StringRepresentable {
  AF,
  AM,
  AW,
  AS,
  BWH,
  BWK,
  BSH,
  BSK,
  CSA,
  CSB,
  CSC,
  CWA,
  CWB,
  CWC,
  CFA,
  CFB,
  CFC,
  DSA,
  DSB,
  DSC,
  DSD,
  DWA,
  DWB,
  DWC,
  DWD,
  DFA,
  DFB,
  DFC,
  DFD,
  ET,
  EF;

  private final String name;

  RealKoppenClimateClassification() {
    this.name = name().toLowerCase(Locale.ROOT);
  }

  @Override
  public String getSerializedName() {
    return name;
  }

  public static RealKoppenClimateClassification classify(
    float averageTemperature,
    float rainfall,
    float rainVar,
    boolean isInNorthernHemisphere
  ) {
    if (!isInNorthernHemisphere) {
      rainVar = -rainVar;
    }

    if (averageTemperature < -17f + 0.006f * rainfall) {
      return EF;
    } else if (averageTemperature <= -12f) {
      return ET;
    } else if (rainfall < 75f) {
      return averageTemperature > 18f ? BWH : BWK;
    } else if (rainfall < 150f) {
      return averageTemperature > 18f ? BSH : BSK;
    } else if (averageTemperature > 21f) {
      if (rainfall * (1f + rainVar) > 600f) {
        return AM;
      } else if (rainVar > 0.5f) {
        return AW;
      } else if (rainVar < -0.5f) {
        return AS;
      } else {
        return AF;
      }
    } else if (averageTemperature > 8f) {
      if (averageTemperature > 17f) {
        if (rainVar > 0.5f) return CWA;
        if (rainVar < -0.5f) return CSA;
        return CFA;
      } else if (averageTemperature > 12f) {
        if (rainVar > 0.5f) return CWB;
        if (rainVar < -0.5f) return CSB;
        return CFB;
      } else {
        if (rainVar > 0.5f) return CWC;
        if (rainVar < -0.5f) return CSC;
        return CFC;
      }
    } else if (averageTemperature > 3f) {
      if (rainVar > 0.5f) return DWA;
      if (rainVar < -0.5f) return DSA;
      return DFA;
    } else if (averageTemperature > -2f) {
      if (rainVar > 0.5f) return DWB;
      if (rainVar < -0.5f) return DSB;
      return DFB;
    } else if (averageTemperature > -8f) {
      if (rainVar > 0.5f) return DWC;
      if (rainVar < -0.5f) return DSC;
      return DFC;
    } else if (rainVar > 0.5f) {
      return DWD;
    } else if (rainVar < -0.5f) {
      return DSD;
    } else {
      return DFD;
    }
  }
}
