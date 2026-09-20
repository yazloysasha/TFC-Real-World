package net.yazloysasha.tfcrealworld.world.volcano;

import static com.newterraearth.tfe.world.NTELayerIds.*;
import static net.dries007.tfc.world.layer.TFCLayers.HIGHLANDS;
import static net.dries007.tfc.world.layer.TFCLayers.MOUNTAINS;
import static net.dries007.tfc.world.layer.TFCLayers.OCEANIC_MOUNTAINS;
import static net.dries007.tfc.world.layer.TFCLayers.OLD_MOUNTAINS;
import static net.dries007.tfc.world.layer.TFCLayers.PLATEAU;
import static net.dries007.tfc.world.layer.TFCLayers.ROLLING_HILLS;
import static net.dries007.tfc.world.layer.TFCLayers.VOLCANIC_MOUNTAINS;
import static net.dries007.tfc.world.layer.TFCLayers.VOLCANIC_OCEANIC_MOUNTAINS;

import com.newterraearth.tfe.world.region.NTEPointAccess;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.registry.HotspotsNoiseRegistry;
import net.yazloysasha.tfcrealworld.world.backport.ChooseBiomesSupport;
import net.yazloysasha.tfcrealworld.world.region.RegionCoords;
import net.yazloysasha.tfcrealworld.world.volcano.MapHotspotLayout.MountainStyle;
import org.jetbrains.annotations.Nullable;

public final class TfeVolcanoMapPipeline {

  private static final ThreadLocal<Region> CURRENT_REGION = new ThreadLocal<>();
  private static final ThreadLocal<Boolean> ASSIGNING_HOTSPOT_BIOME =
    ThreadLocal.withInitial(() -> Boolean.FALSE);

  private TfeVolcanoMapPipeline() {}

  public static void onChooseBiomesHead(
    RegionGenerator.Context context,
    long worldSeed
  ) {
    CURRENT_REGION.set(context.region);
    if (!TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get()) {
      return;
    }
    final MapHotspotLayout layout = HotspotsNoiseRegistry.biomeLayout();
    if (layout == null) {
      return;
    }
    layout.prepareChooseBiomes(context.region, worldSeed);
    markStratovolcanoVolcanic(context.region, layout);
  }

  public static void onChooseBiomesTail(
    RegionGenerator.Context context,
    long worldSeed,
    Object chooseBiomesReceiver
  ) {
    if (TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get()) {
      finalizeMapHotspotBiomes(context.region, chooseBiomesReceiver);
    }
    applyAltitudeSoftMountainFill(context.region);
    if (
      TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get() ||
      TFCRealWorldConfig.ALTITUDE_FROM_MAP.get() ||
      net.yazloysasha.tfcrealworld.world.region.MapTectonics.isActive(
        context.generator()
      )
    ) {
      TfeCenteredFeatureAligner.align(context.region, worldSeed);
    }
    CURRENT_REGION.remove();
    ASSIGNING_HOTSPOT_BIOME.remove();
    ChooseBiomesSupport.CURRENT_POINT.remove();
  }

  public static void beginHotspotBiomeAssignment() {
    ASSIGNING_HOTSPOT_BIOME.set(Boolean.TRUE);
  }

  public static boolean isAssigningHotspotBiome() {
    return Boolean.TRUE.equals(ASSIGNING_HOTSPOT_BIOME.get());
  }

  public static void clearHotspotBiomeAssignment() {
    ASSIGNING_HOTSPOT_BIOME.set(Boolean.FALSE);
  }

  public static boolean shouldKeepMountainBiome(Region.Point point) {
    final MapHotspotLayout layout = HotspotsNoiseRegistry.biomeLayout();
    final Region region = CURRENT_REGION.get();
    if (layout == null || region == null) {
      return false;
    }
    return layout.keepMountainBiome(
      region,
      point,
      ((NTEPointAccess) point).nte$getHotSpotAge()
    );
  }

  public static boolean shouldPaintStratovolcano(Region.Point point) {
    if (isIceMountain(point.biome)) {
      return false;
    }
    final MapHotspotLayout layout = HotspotsNoiseRegistry.biomeLayout();
    final Region region = CURRENT_REGION.get();
    if (layout == null || region == null) {
      return false;
    }
    return layout.styleAt(region, point) == MountainStyle.STRATOVOLCANO;
  }

  private static void finalizeMapHotspotBiomes(
    Region region,
    Object chooseBiomesReceiver
  ) {
    final Region.Point[] data = region.data();
    for (final Region.Point point : data) {
      if (point == null) {
        continue;
      }
      final byte age = ((NTEPointAccess) point).nte$getHotSpotAge();
      if (age <= 0) {
        continue;
      }
      if (shouldKeepMountainBiome(point)) {
        if (shouldPaintStratovolcano(point)) {
          point.biome = MapHotspotBiomes.volcanicMountainFor(
            point,
            VOLCANIC_MOUNTAINS,
            VOLCANIC_OCEANIC_MOUNTAINS
          );
        }
        continue;
      }
      point.biome = TfeShieldHotspotBiomes.assign(
        point,
        age,
        chooseBiomesReceiver
      );
    }
  }

  private static void applyAltitudeSoftMountainFill(Region region) {
    if (!TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()) {
      return;
    }
    final Region.Point[] data = region.data();
    for (final Region.Point point : data) {
      if (point == null || !point.mountain()) {
        continue;
      }
      if (
        ChooseBiomesSupport.isSoftMountainFill(
          point.biome,
          OLD_MOUNTAINS,
          PLATEAU,
          PLATEAU_WIDE,
          HIGHLANDS,
          ROLLING_HILLS,
          ROCKY_PLATEAU
        )
      ) {
        point.biome = point.coastalMountain() ? OCEANIC_MOUNTAINS : MOUNTAINS;
      }
    }
  }

  private static void markStratovolcanoVolcanic(
    Region region,
    MapHotspotLayout layout
  ) {
    final Region.Point[] data = region.data();
    for (int index = 0; index < data.length; index++) {
      final Region.Point point = data[index];
      if (point == null || !point.mountain()) {
        continue;
      }
      if (
        layout.styleAtGrid(
          RegionCoords.gridX(region, index),
          RegionCoords.gridZ(region, index)
        ) ==
        MountainStyle.STRATOVOLCANO
      ) {
        ((NTEPointAccess) point).nte$setVolcanic(true);
      }
    }
  }

  public static boolean isShieldHotspotBiome(int biome) {
    return (
      biome == ACTIVE_SHIELD_VOLCANO ||
      biome == DORMANT_SHIELD_VOLCANO ||
      biome == EXTINCT_SHIELD_VOLCANO ||
      biome == ANCIENT_SHIELD_VOLCANO ||
      biome == SUNKEN_SHIELD_VOLCANO ||
      biome == ICE_SHEET_SHIELD_VOLCANO ||
      biome == GLACIATED_SHIELD_VOLCANO ||
      biome == SHIELD_VOLCANO_SHORE ||
      biome == OLD_SHIELD_VOLCANO_SHORE
    );
  }

  private static boolean isIceMountain(int biome) {
    return (
      biome == ICE_SHEET_MOUNTAINS ||
      biome == ICE_SHEET_OCEANIC_MOUNTAINS ||
      biome == ICE_SHEET_VOLCANIC_MOUNTAINS ||
      biome == ICE_SHEET_VOLCANIC_OCEANIC_MOUNTAINS ||
      biome == GLACIATED_MOUNTAINS ||
      biome == GLACIATED_OCEANIC_MOUNTAINS ||
      biome == GLACIATED_VOLCANIC_MOUNTAINS ||
      biome == GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS ||
      biome == GLACIALLY_CARVED_MOUNTAINS ||
      biome == GLACIALLY_CARVED_OCEANIC_MOUNTAINS ||
      biome == GLACIALLY_CARVED_VOLCANIC_MOUNTAINS ||
      biome == GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS
    );
  }

  @Nullable
  public static Region currentRegion() {
    return CURRENT_REGION.get();
  }
}
