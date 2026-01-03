package net.yazloysasha.tfcrealworld.util;

import net.dries007.tfc.world.settings.Settings;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;

public class SettingsHelper {

  private static java.lang.reflect.Field worldScaleField;
  private static boolean worldScaleFieldInitialized = false;

  public static int getVerticalWorldScale(Settings settings) {
    if (!worldScaleFieldInitialized) {
      try {
        worldScaleField = Settings.class.getDeclaredField("worldScale");
        worldScaleField.setAccessible(true);
      } catch (NoSuchFieldException e) {
        worldScaleField = null;
      }
      worldScaleFieldInitialized = true;
    }

    if (worldScaleField != null) {
      try {
        return worldScaleField.getInt(settings);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(
          "Failed to get worldScale from Settings via reflection",
          e
        );
      }
    }

    return TFCRealWorldConfig.getVerticalWorldScale();
  }

  public static int getHorizontalWorldScale(Settings settings) {
    return TFCRealWorldConfig.getHorizontalWorldScale();
  }

  @Deprecated
  public static int getWorldScale(Settings settings) {
    return getVerticalWorldScale(settings);
  }
}
