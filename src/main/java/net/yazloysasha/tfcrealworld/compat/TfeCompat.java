package net.yazloysasha.tfcrealworld.compat;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.LoadingModList;
import org.jetbrains.annotations.Nullable;

/**
 * Optional TerraFirmaEarth ({@code tfe}) detection. Mixins that target its
 * classes, or TFC 3 tasks it rewrites, are applied only with it loaded.
 */
public final class TfeCompat {

  public static final String MOD_ID = "tfe";

  /** A class TerraFirmaEarth's region pipeline cannot work without. */
  private static final String POINT_ACCESS =
    "com.newterraearth.tfe.world.region.NTEPointAccess";

  /** TFC 3 mixins of tasks TerraFirmaEarth rewrites with another shape. */
  private static final String TFC3_CHOOSE_BIOMES = ".region.ChooseBiomesMixin";

  @Nullable
  private static Boolean pipelinePresent;

  private TfeCompat() {}

  public static boolean isModPresent() {
    if (ModList.get() != null) {
      return ModList.get().isLoaded(MOD_ID);
    }
    final LoadingModList loading = LoadingModList.get();
    return loading != null && loading.getModFileById(MOD_ID) != null;
  }

  /** True when TerraFirmaEarth is loaded and generates the overworld. */
  public static boolean useTfeOverworldPipeline() {
    Boolean present = pipelinePresent;
    if (present == null) {
      present = isModPresent() && classPresent(POINT_ACCESS);
      pipelinePresent = present;
    }
    return present;
  }

  @Nullable
  public static String mixinDisableReason(String mixinClassName) {
    final String mixin = mixinClassName.replace('/', '.');
    if (mixin.contains(".tfe.")) {
      return isModPresent() ? null : "TerraFirmaEarth (tfe) is not loaded";
    }
    if (isModPresent() && mixin.endsWith(TFC3_CHOOSE_BIOMES)) {
      return "TerraFirmaEarth rewrites this TFC 3 region task";
    }
    return null;
  }

  private static boolean classPresent(String name) {
    try {
      Class.forName(name, false, TfeCompat.class.getClassLoader());
      return true;
    } catch (ClassNotFoundException | LinkageError ignored) {
      return false;
    }
  }
}
