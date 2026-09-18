package net.yazloysasha.tfcrealworld.compat;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.LoadingModList;
import org.jetbrains.annotations.Nullable;

/**
 * Optional Core-Modern (mod id {@code tfg}) detection. Mixins targeting TFG
 * classes are gated by {@link #isModPresent()}; runtime pipeline choice is
 * delegated to TFG so old worlds stay on classic TFC 3 generation.
 */
public final class TfgCompat {

  public static final String MOD_ID = "tfg";

  private static final String PREVIEW_STATE =
    "su.terrafirmagreg.core.world.new_ow_wg.TfgClientPreviewState";

  private TfgCompat() {}

  public static boolean isModPresent() {
    if (ModList.get() != null) {
      return ModList.get().isLoaded(MOD_ID);
    }
    final LoadingModList loading = LoadingModList.get();
    return loading != null && loading.getModFileById(MOD_ID) != null;
  }

  /**
   * True when TFG is running the 1.21-backport overworld task list.
   * False when TFG is absent, or when an old world stays on classic TFC 3.
   */
  public static boolean useTfgOverworldPipeline() {
    if (!isModPresent()) {
      return false;
    }
    try {
      final Class<?> preview = Class.forName(PREVIEW_STATE);
      return (Boolean) preview
        .getMethod("useTfgOverworldPipeline")
        .invoke(null);
    } catch (ReflectiveOperationException | LinkageError ignored) {
      return false;
    }
  }

  @Nullable
  public static String mixinDisableReason(String mixinClassName) {
    if (mixinClassName.contains(".tfg.") && !isModPresent()) {
      return "Core-Modern (tfg) is not loaded";
    }
    return null;
  }
}
