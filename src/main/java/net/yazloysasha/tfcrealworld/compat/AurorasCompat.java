package net.yazloysasha.tfcrealworld.compat;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.projection.ProjectionManager;
import org.jetbrains.annotations.Nullable;

/**
 * Optional Auroras ({@code auroras}) integration.
 */
public final class AurorasCompat {

  public static final String MOD_ID = "auroras";

  private AurorasCompat() {}

  public static boolean isModPresent() {
    if (ModList.get() != null) {
      return ModList.get().isLoaded(MOD_ID);
    }
    final var loading = FMLLoader.getLoadingModList();
    return loading != null && loading.getModFileById(MOD_ID) != null;
  }

  /**
   * World Z coordinate as Auroras expects it: linear classic TFC latitude along Z.
   */
  public static double toVirtualClassicZ(double worldZ) {
    final int scale = TFCRealWorldConfig.VERTICAL_SCALE.get();
    final double latitude = ProjectionManager.resolveLatitudeFromWorldZ(worldZ);
    return ProjectionManager.virtualClassicZFromLatitude(latitude, scale);
  }

  @Nullable
  public static String mixinDisableReason(String mixinClassName) {
    if (mixinClassName.contains(".auroras.") && !isModPresent()) {
      return "Auroras (auroras) is not loaded";
    }
    return null;
  }
}
