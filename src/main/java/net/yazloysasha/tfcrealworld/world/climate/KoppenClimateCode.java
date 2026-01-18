package net.yazloysasha.tfcrealworld.world.climate;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;

public enum KoppenClimateCode implements StringRepresentable {
  AF(22f, 28f, 250f, 500f),
  AM(22f, 28f, 200f, 400f),
  AW(20f, 28f, 100f, 250f),
  AS(20f, 28f, 100f, 250f),

  BWH(18f, 30f, 0f, 50f),
  BWK(-5f, 18f, 0f, 50f),
  BSH(18f, 30f, 50f, 150f),
  BSK(-5f, 18f, 50f, 150f),

  CSA(15f, 22f, 100f, 200f),
  CSB(10f, 18f, 100f, 250f),
  CSC(5f, 15f, 100f, 250f),
  CWA(15f, 22f, 100f, 250f),
  CWB(10f, 18f, 100f, 250f),
  CWC(5f, 15f, 100f, 250f),
  CFA(15f, 22f, 200f, 400f),
  CFB(10f, 18f, 200f, 400f),
  CFC(5f, 15f, 200f, 350f),

  DSA(5f, 15f, 100f, 200f),
  DSB(0f, 12f, 100f, 200f),
  DSC(-8f, 5f, 100f, 200f),
  DSD(-15f, 0f, 100f, 200f),
  DWA(5f, 15f, 100f, 250f),
  DWB(0f, 12f, 100f, 250f),
  DWC(-8f, 5f, 100f, 250f),
  DWD(-15f, 0f, 100f, 250f),
  DFA(5f, 15f, 200f, 400f),
  DFB(0f, 12f, 200f, 400f),
  DFC(-8f, 5f, 200f, 350f),
  DFD(-15f, 0f, 200f, 350f),

  ET(-20f, -10f, 100f, 300f),
  EF(-40f, -20f, 50f, 200f);

  private final String name;
  private final float minTemp;
  private final float maxTemp;
  private final float minRainfall;
  private final float maxRainfall;

  KoppenClimateCode(
    float minTemp,
    float maxTemp,
    float minRainfall,
    float maxRainfall
  ) {
    this.name = name().toLowerCase(Locale.ROOT);
    this.minTemp = minTemp;
    this.maxTemp = maxTemp;
    this.minRainfall = minRainfall;
    this.maxRainfall = maxRainfall;
  }

  public float getMinTemp() {
    return minTemp;
  }

  public float getMaxTemp() {
    return maxTemp;
  }

  public float getMinRainfall() {
    return minRainfall;
  }

  public float getMaxRainfall() {
    return maxRainfall;
  }

  @Override
  public String getSerializedName() {
    return name;
  }
}
