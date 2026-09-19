package net.yazloysasha.tfcrealworld.compat;

import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.LoadingModList;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.projection.ProjectionManager;
import org.jetbrains.annotations.Nullable;
import tfccaelum.Config;
import tfccaelum.Helpers;

/**
 * Optional TFC Caelum ({@code tfccaelum}) integration.
 */
public final class CaelumCompat {

  public static final String MOD_ID = "tfccaelum";

  private CaelumCompat() {}

  public static boolean isModPresent() {
    if (ModList.get() != null) {
      return ModList.get().isLoaded(MOD_ID);
    }
    final LoadingModList loading = LoadingModList.get();
    return loading != null && loading.getModFileById(MOD_ID) != null;
  }

  /**
   * Star-field latitude rotation for Caelum, using Real World geographic latitude
   * while preserving TFC Caelum's seasonal pole shift in classic Z space.
   */
  public static double calculateStarLatitudeRotation(
    Level level,
    double worldZ
  ) {
    final int scale = TFCRealWorldConfig.VERTICAL_SCALE.get();
    final double solsticeFraction = Helpers.solsticeFraction(level);
    final double seasonalTilt = Config.COMMON.earthSeasonalTilt.get();
    final double tilt = scale * solsticeFraction * seasonalTilt * 0.01 * -1.0;

    final double northZ = -scale * 0.5 - tilt;
    final double spanZ = scale * 1.5 + tilt - northZ;

    final double latitude = ProjectionManager.resolveLatitudeFromWorldZ(worldZ);
    final double virtualClassicZ =
      ProjectionManager.virtualClassicZFromLatitude(latitude, scale);

    final double t = Mth.clamp((virtualClassicZ - northZ) / spanZ, 0.0, 1.0);
    return t - 0.5;
  }

  @Nullable
  public static String mixinDisableReason(String mixinClassName) {
    if (mixinClassName.contains(".caelum.") && !isModPresent()) {
      return "TFC Caelum (tfccaelum) is not loaded";
    }
    return null;
  }
}
