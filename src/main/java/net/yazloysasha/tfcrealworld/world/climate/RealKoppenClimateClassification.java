package net.yazloysasha.tfcrealworld.world.climate;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;
import net.yazloysasha.tfcrealworld.types.ClimateCategory;
import net.yazloysasha.tfcrealworld.types.TemperatureCharacteristic;

public enum RealKoppenClimateClassification implements StringRepresentable {
  AF("Humid Tropical"),
  AM("Tropical Monsoon"),
  AW("Tropical Wet/Dry"),
  AS("Tropical Dry/Wet"),
  BWH("Hot Desert"),
  BWK("Cold Desert"),
  BSH("Hot Semi-Arid"),
  BSK("Cold Semi-Arid"),
  CSA("Coastal Subtropical"),
  CSB("Coastal"),
  CSC("Cold Coastal"),
  CWA("Monsoonal Subtropical"),
  CWB("Monsoonal Temperate"),
  CWC("Cold Monsoonal Temperate"),
  CFA("Oceanic Subtropical"),
  CFB("Oceanic"),
  CFC("Cold Oceanic"),
  DSA("Coastal Continental"),
  DSB("Cold Coastal Continental"),
  DSC("Coastal Subarctic"),
  DSD("Coastal Cold Subarctic"),
  DWA("Monsoonal Continental"),
  DWB("Cold Monsoonal Continental"),
  DWC("Monsoonal Subarctic"),
  DWD("Cold Monsoonal Subarctic"),
  DFA("Continental"),
  DFB("Cold Continental"),
  DFC("Subarctic"),
  DFD("Cold Subarctic"),
  ET("Tundra"),
  EF("Polar");

  private final String name;
  private final String displayName;
  private final ClimateCategory category;

  RealKoppenClimateClassification(String displayName) {
    this.name = name().toLowerCase(Locale.ROOT);
    this.displayName = displayName;
    this.category = determineCategory();
  }

  @Override
  public String getSerializedName() {
    return name;
  }

  public String getDisplayName() {
    return displayName;
  }

  public ClimateCategory getCategory() {
    return category;
  }

  private ClimateCategory determineCategory() {
    String code = name();
    if (code.startsWith("A")) {
      return ClimateCategory.TROPICAL;
    } else if (code.startsWith("B")) {
      return ClimateCategory.DRY;
    } else if (code.startsWith("C")) {
      return ClimateCategory.TEMPERATE;
    } else if (code.startsWith("D")) {
      return ClimateCategory.CONTINENTAL;
    } else if (code.startsWith("E")) {
      return ClimateCategory.POLAR;
    }
    throw new IllegalStateException("Unknown climate category for " + code);
  }

  public TemperatureCharacteristic getTemperatureCharacteristic() {
    String code = name();
    if (code.length() < 3) {
      return null;
    }

    char thirdChar = code.charAt(2);

    if (category == ClimateCategory.DRY) {
      if (thirdChar == 'H') {
        return TemperatureCharacteristic.HOT;
      } else if (thirdChar == 'K') {
        return TemperatureCharacteristic.COLD;
      }
    } else if (category == ClimateCategory.TEMPERATE) {
      if (thirdChar == 'A') {
        return TemperatureCharacteristic.HOT;
      } else if (thirdChar == 'B') {
        return TemperatureCharacteristic.WARM;
      } else if (thirdChar == 'C') {
        return TemperatureCharacteristic.COLD;
      }
    } else if (category == ClimateCategory.CONTINENTAL) {
      if (thirdChar == 'A') {
        return TemperatureCharacteristic.HOT;
      } else if (thirdChar == 'B') {
        return TemperatureCharacteristic.WARM;
      } else if (thirdChar == 'C') {
        return TemperatureCharacteristic.COLD;
      } else if (thirdChar == 'D') {
        return TemperatureCharacteristic.VERY_COLD;
      }
    }
    return null;
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

    if (averageTemperature < -20f + 0.003f * rainfall) {
      return EF;
    } else if (
      averageTemperature < -14f && rainfall >= 140f && rainfall <= 310f
    ) {
      return ET;
    } else if (rainfall < 75f) {
      return averageTemperature > 10f ? BWH : BWK;
    } else if (rainfall < 150f) {
      return averageTemperature > 10f ? BSH : BSK;
    } else if (averageTemperature > 18f) {
      if (rainfall * (1f + rainVar) > 500f) {
        return AM;
      } else if (rainVar < -0.5f && rainfall <= 150f) {
        return AS;
      } else if (rainfall > 300f && rainVar >= -0.5f && rainVar <= 0.5f) {
        return AF;
      } else {
        return AW;
      }
    } else if (averageTemperature > 8f) {
      if (averageTemperature > 12f) {
        if (rainVar > 0.5f) return CWA;
        if (rainVar < -0.5f) return CSA;
        return CFA;
      } else if (averageTemperature > 9f) {
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
