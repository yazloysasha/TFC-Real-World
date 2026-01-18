package net.yazloysasha.tfcrealworld.world.climate;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/**
 * Köppen climate classification codes with Beck et al. (2018) classification criteria.
 *
 * Classification parameters:
 * - MAT = Mean Annual Temperature (°C)
 * - MAP = Mean Annual Precipitation (mm)
 * - Tcold = coldest month temperature (°C)
 * - Thot = hottest month temperature (°C)
 * - Pdry = driest month precipitation (mm)
 * - Tmon10 = number of months with T > 10°C
 * - Pth = precipitation threshold (depends on seasonal distribution)
 * - Psdry = driest summer month, Pwwet = wettest winter month
 * - Pwdry = driest winter month, Pswet = wettest summer month
 *
 * Tropical (A): Tcold >= 18°C
 *   AF  (beck_1)  -> Pdry >= 60
 *   AM  (beck_2)  -> Pdry >= 100 - MAP/25
 *   AW  (beck_3)  -> other (Pdry < 100 - MAP/25)
 *   AS  (beck_3)  -> same as AW (dry summer variant)
 *
 * Arid (B): MAP < 10 * Pth
 *   BWH (beck_4)  -> MAP < 5*Pth, MAT >= 18
 *   BWK (beck_5)  -> MAP < 5*Pth, MAT < 18
 *   BSH (beck_6)  -> MAP >= 5*Pth, MAT >= 18
 *   BSK (beck_7)  -> MAP >= 5*Pth, MAT < 18
 *
 * Temperate (C): Thot > 10 and 0 < Tcold < 18
 *   CSA (beck_8)  -> Psdry < 40 and Psdry < Pwwet/3, Thot >= 22
 *   CSB (beck_9)  -> Psdry < 40 and Psdry < Pwwet/3, Tmon10 >= 4
 *   CSC (beck_10) -> Psdry < 40 and Psdry < Pwwet/3, 1 <= Tmon10 < 4
 *   CWA (beck_11) -> Pwdry < Pswet/10, Thot >= 22
 *   CWB (beck_12) -> Pwdry < Pswet/10, Tmon10 >= 4
 *   CWC (beck_13) -> Pwdry < Pswet/10, 1 <= Tmon10 < 4
 *   CFA (beck_14) -> other, Thot >= 22
 *   CFB (beck_15) -> other, Tmon10 >= 4
 *   CFC (beck_16) -> other, 1 <= Tmon10 < 4
 *
 * Continental (D): Thot > 10 and Tcold <= 0
 *   DSA (beck_17) -> Psdry < 40 and Psdry < Pwwet/3, Thot >= 22
 *   DSB (beck_18) -> Psdry < 40 and Psdry < Pwwet/3, Tmon10 >= 4
 *   DSC (beck_19) -> Psdry < 40 and Psdry < Pwwet/3, other
 *   DSD (beck_20) -> Psdry < 40 and Psdry < Pwwet/3, Tcold < -38
 *   DWA (beck_21) -> Pwdry < Pswet/10, Thot >= 22
 *   DWB (beck_22) -> Pwdry < Pswet/10, Tmon10 >= 4
 *   DWC (beck_23) -> Pwdry < Pswet/10, other
 *   DWD (beck_24) -> Pwdry < Pswet/10, Tcold < -38
 *   DFA (beck_25) -> other, Thot >= 22
 *   DFB (beck_26) -> other, Tmon10 >= 4
 *   DFC (beck_27) -> other, normal
 *   DFD (beck_28) -> other, Tcold < -38
 *
 * Polar (E): Thot <= 10
 *   ET  (beck_29) -> 0 < Thot <= 10
 *   EF  (beck_30) -> Thot <= 0
 */

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
