package net.yazloysasha.tfcrealworld.world.climate;

import java.util.Locale;
import net.dries007.tfc.util.climate.KoppenClimateClassification;
import net.minecraft.util.StringRepresentable;

/**
 * Detailed Köppen climate classification codes (AF, AM, AW, etc.)
 * that map to the simplified TFC climate zones (ARCTIC, TUNDRA, etc.).
 */
public enum KoppenClimateCode implements StringRepresentable {
  // Group A: Tropical
  AF(KoppenClimateClassification.TROPICAL_RAINFOREST),
  AM(KoppenClimateClassification.TROPICAL_RAINFOREST),
  AW(KoppenClimateClassification.TROPICAL_SAVANNA),
  AS(KoppenClimateClassification.TROPICAL_SAVANNA),

  // Group B: Arid
  BWH(KoppenClimateClassification.HOT_DESERT),
  BWK(KoppenClimateClassification.COLD_DESERT),
  BSH(KoppenClimateClassification.HOT_DESERT),
  BSK(KoppenClimateClassification.COLD_DESERT),

  // Group C: Temperate
  CSA(KoppenClimateClassification.SUBTROPICAL),
  CSB(KoppenClimateClassification.TEMPERATE),
  CSC(KoppenClimateClassification.TEMPERATE),
  CWA(KoppenClimateClassification.SUBTROPICAL),
  CWB(KoppenClimateClassification.TEMPERATE),
  CWC(KoppenClimateClassification.TEMPERATE),
  CFA(KoppenClimateClassification.HUMID_SUBTROPICAL),
  CFB(KoppenClimateClassification.HUMID_OCEANIC),
  CFC(KoppenClimateClassification.TEMPERATE),

  // Group D: Continental
  DSA(KoppenClimateClassification.TEMPERATE),
  DSB(KoppenClimateClassification.TEMPERATE),
  DSC(KoppenClimateClassification.SUBARCTIC),
  DSD(KoppenClimateClassification.SUBARCTIC),
  DWA(KoppenClimateClassification.TEMPERATE),
  DWB(KoppenClimateClassification.TEMPERATE),
  DWC(KoppenClimateClassification.HUMID_SUBARCTIC),
  DWD(KoppenClimateClassification.HUMID_SUBARCTIC),
  DFA(KoppenClimateClassification.HUMID_OCEANIC),
  DFB(KoppenClimateClassification.HUMID_OCEANIC),
  DFC(KoppenClimateClassification.HUMID_SUBARCTIC),
  DFD(KoppenClimateClassification.HUMID_SUBARCTIC),

  // Group E: Polar
  ET(KoppenClimateClassification.TUNDRA),
  EF(KoppenClimateClassification.ARCTIC);

  private final String name;
  private final KoppenClimateClassification[] tfcClimates;

  KoppenClimateCode(KoppenClimateClassification... tfcClimates) {
    this.name = name().toLowerCase(Locale.ROOT);
    this.tfcClimates = tfcClimates;
  }

  public KoppenClimateClassification[] getTFCClimates() {
    return tfcClimates;
  }

  @Override
  public String getSerializedName() {
    return name;
  }

  public static KoppenClimateCode classify(
    float averageTemperature,
    float rainfall
  ) {
    if (averageTemperature < -17f + 0.006 * rainfall) {
      return EF;
    } else if (averageTemperature <= -12f) {
      return ET;
    } else if (rainfall < 75f) {
      if (averageTemperature > 18f) {
        return BWH;
      } else {
        return BWK;
      }
    } else if (rainfall < 150f) {
      if (averageTemperature > 18) {
        return BSH;
      } else {
        return BSK;
      }
    } else if (averageTemperature > 21f) {
      if (rainfall > 600f) {
        return AM;
      } else {
        return AF;
      }
    } else if (averageTemperature > 8f) {
      if (averageTemperature > 17f) {
        return CFA;
      } else if (averageTemperature > 12f) {
        return CFB;
      } else {
        return CFC;
      }
    } else if (averageTemperature > 3f) {
      return DFA;
    } else if (averageTemperature > -2f) {
      return DFB;
    } else if (averageTemperature > -8f) {
      return DFC;
    } else {
      return DFD;
    }
  }
}
