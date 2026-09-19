package net.yazloysasha.tfcrealworld.compat;

import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.LoadingModList;
import net.yazloysasha.tfcrealworld.util.projection.ProjectionManager;
import org.jetbrains.annotations.Nullable;

/**
 * Optional Firma: Civilization ({@code firmaciv}) integration.
 */
public final class FirmaCivCompat {

  public static final String MOD_ID = "firmaciv";

  private FirmaCivCompat() {}

  public static boolean isModPresent() {
    if (ModList.get() != null) {
      return ModList.get().isLoaded(MOD_ID);
    }
    final LoadingModList loading = LoadingModList.get();
    return loading != null && loading.getModFileById(MOD_ID) != null;
  }

  /**
   * Navigation coordinates for FirmaCiv tools: geographic latitude and longitude
   * (degrees, signed) plus altitude relative to sea level, matching
   * {@code AbstractNavItem#getNavLocation} layout.
   */
  public static double[] getNavLocation(Vec3 position) {
    final double[] geographic = ProjectionManager.classicToGeographic(
      position.x,
      position.z
    );
    final double longitude = signedLongitude(geographic[0]);
    final double latitude = geographic[1];
    final double altitude = position.y - 64.0;
    return new double[] { latitude, longitude, altitude };
  }

  private static double signedLongitude(double longitude) {
    double lon = longitude % 360.0;
    if (lon > 180.0) {
      lon -= 360.0;
    } else if (lon <= -180.0) {
      lon += 360.0;
    }
    return lon;
  }

  @Nullable
  public static String mixinDisableReason(String mixinClassName) {
    if (mixinClassName.contains(".firmaciv.") && !isModPresent()) {
      return "Firma: Civilization (firmaciv) is not loaded";
    }
    return null;
  }
}
