package net.yazloysasha.tfcrealworld.compat.tfe;

/**
 * TerraFirmaEarth class and Unique-field names used by mixins and
 * {@link net.yazloysasha.tfcrealworld.compat.TfeCompat}.
 */
public final class TfeBindings {

  public static final String MOD_ID = "tfe";
  public static final String PKG = "com.newterraearth.tfe";

  public static final String POINT_ACCESS =
    PKG + ".world.region.NTEPointAccess";
  public static final String GENERATOR_ACCESS =
    PKG + ".world.region.NTERegionGeneratorAccess";
  public static final String ADD_HOTSPOTS =
    PKG + ".world.region.NTEAddHotspots";
  public static final String FEATURE_ANNOTATIONS =
    PKG + ".world.region.NTERegionFeatureAnnotations";
  public static final String TERRAIN_UPLIFT =
    PKG + ".world.terrain.NTETerrainUpliftSampler";
  public static final String ICE_SHEET_EDGE =
    PKG + ".world.layer.NTEIceSheetEdgeLayer";
  public static final String RIVER_SHORE =
    PKG + ".world.layer.NTERiverShoreLayer";
  public static final String BIOME_EXTENSION_ACCESS =
    PKG + ".world.NTEBiomeExtensionAccess";
  public static final String CLIMATE_HELPERS =
    PKG + ".world.NTE121ClimateHelpers";

  public static final String HOTSPOT_INTENSITY_FIELD =
    "tfe$hotSpotIntensityNoise";
  public static final String HOTSPOT_AGE_FIELD = "tfe$hotSpotAgeNoise";
  public static final String RAINFALL_VARIANCE_FIELD =
    "tfe$rainfallVarianceNoise";

  public static final String PLACE_VOLCANIC_ARC = "tfe$placeVolcanicArc";
  public static final String PLACE_BARRIER = "tfe$placeBarrier";

  private TfeBindings() {}

  public static boolean classPresent(String name) {
    try {
      Class.forName(name, false, TfeBindings.class.getClassLoader());
      return true;
    } catch (ClassNotFoundException | LinkageError ignored) {
      return false;
    }
  }
}
