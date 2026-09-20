package net.yazloysasha.tfcrealworld.compat;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.LoadingModList;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.compat.tfe.TfeBindings;
import org.jetbrains.annotations.Nullable;

/**
 * Optional TerraFirmaEarth ({@code tfe}) detection. Mixins targeting TFE
 * classes are gated here so stub packages never resolve missing types.
 * Feature checks go through {@link TfeBindings} so a later TFE backport
 * that moves classes only needs constant updates.
 */
public final class TfeCompat {

  public static final String MOD_ID = TfeBindings.MOD_ID;

  private TfeCompat() {}

  public static boolean isModPresent() {
    if (ModList.get() != null) {
      return ModList.get().isLoaded(MOD_ID);
    }
    final LoadingModList loading = LoadingModList.get();
    return loading != null && loading.getModFileById(MOD_ID) != null;
  }

  /**
   * True when TFE is loaded and its region-access APIs are still present.
   * False if TFE is absent or a breaking backport dropped those types.
   */
  public static boolean useTfeOverworldPipeline() {
    return (
      isModPresent() &&
      TfeBindings.classPresent(TfeBindings.POINT_ACCESS) &&
      TfeBindings.classPresent(TfeBindings.GENERATOR_ACCESS)
    );
  }

  /**
   * 1.21.1 Köppen smoothing (rain variance + neighbor clamp) is TFE-only.
   * TFG new-OW worlds still sample {@code RegionGenerator} climate noises, so
   * keep the TFC 3 maps whenever TFG's backport pipeline is the one generating.
   */
  public static boolean useTfeKoppenMaps() {
    return useTfeOverworldPipeline() && !TfgCompat.useTfgOverworldPipeline();
  }

  /** Mixin plugin passes binary or dotted names depending on loader phase. */
  public static String normalizeClassName(String className) {
    return className.replace('/', '.');
  }

  @Nullable
  public static String mixinDisableReason(String mixinClassName) {
    if (!normalizeClassName(mixinClassName).contains(".tfe.")) {
      return null;
    }
    if (!isModPresent()) {
      return "TerraFirmaEarth (tfe) is not loaded";
    }
    final String required = requiredClass(mixinClassName);
    if (required != null && !TfeBindings.classPresent(required)) {
      TFCRealWorld.LOGGER.warn(
        "Skipping {} because TFE class {} is missing; update TfeBindings if the backport moved it",
        mixinClassName,
        required
      );
      return "TerraFirmaEarth class " + required + " is missing";
    }
    return null;
  }

  @Nullable
  private static String requiredClass(String mixinClassName) {
    final String name = normalizeClassName(mixinClassName);
    if (name.endsWith("TfeAddHotspotsMixin")) {
      return TfeBindings.ADD_HOTSPOTS;
    }
    if (name.endsWith("TfeRegionFeatureAnnotationsMixin")) {
      return TfeBindings.FEATURE_ANNOTATIONS;
    }
    if (name.endsWith("TfeRegionGeneratorContextMixin")) {
      return TfeBindings.GENERATOR_ACCESS;
    }
    if (
      name.endsWith("TfeAddContinentsMixin") ||
      name.endsWith("TfeAddMountainsTaskMixin") ||
      name.endsWith("TfeContinentFactorMixin") ||
      name.endsWith("TfeChooseBiomesMixin") ||
      name.endsWith("TfeAnnotateClimateMixin") ||
      name.endsWith("TfeAnnotateBiomeAltitudeMixin") ||
      name.endsWith("TfeTFCLayersMixin")
    ) {
      return TfeBindings.POINT_ACCESS;
    }
    if (name.endsWith("TfeRegionNoiseMixin")) {
      return TfeBindings.REGION_NOISE;
    }
    if (name.endsWith("TfeIceSheetEdgeLayerMixin")) {
      return TfeBindings.ICE_SHEET_EDGE;
    }
    if (name.endsWith("TfeRiverShoreLayerMixin")) {
      return TfeBindings.RIVER_SHORE;
    }
    if (name.endsWith("TfeBiomeExtensionMixin")) {
      return TfeBindings.BIOME_EXTENSION_ACCESS;
    }
    if (name.endsWith("TfeWestCoastHelpersMixin")) {
      return TfeBindings.CLIMATE_HELPERS;
    }
    return TfeBindings.POINT_ACCESS;
  }

  /**
   * TFE {@code @Overwrite}s these TFC 3 tasks. Keep our TFC 3 mixins off so
   * redirects looking for 3.x bytecode do not fight the backport.
   */
  @Nullable
  public static String tfc3DisableReason(
    String targetClassName,
    String mixinClassName
  ) {
    if (!isModPresent()) {
      return null;
    }
    final String mixin = normalizeClassName(mixinClassName);
    if (mixin.contains(".tfe.") || mixin.contains(".tfg.")) {
      return null;
    }
    if (isTfc3RegionTaskMixin(mixin)) {
      return "TerraFirmaEarth overwrites this TFC 3 region task";
    }
    final String target = normalizeClassName(targetClassName);
    if (
      target.endsWith(".AnnotateClimate") ||
      target.endsWith(".ChooseBiomes") ||
      target.endsWith(".AddMountains")
    ) {
      return "TerraFirmaEarth overwrites this TFC 3 region task";
    }
    if (target.endsWith(".MoreShoresLayer")) {
      return "TerraFirmaEarth overwrites MoreShoresLayer with the 1.21.1 shore expansion";
    }
    return null;
  }

  private static boolean isTfc3RegionTaskMixin(String normalizedMixinName) {
    final int dot = normalizedMixinName.lastIndexOf('.');
    final String simple = dot >= 0
      ? normalizedMixinName.substring(dot + 1)
      : normalizedMixinName;
    return (
      simple.equals("ChooseBiomesMixin") ||
      simple.equals("AnnotateClimateMixin") ||
      simple.equals("AddMountainsMixin") ||
      simple.equals("AddMountainsAccessor") ||
      simple.equals("MoreShoresLayerMixin")
    );
  }
}
