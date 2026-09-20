package net.yazloysasha.tfcrealworld.mixin.world.region.tfe;

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
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.layer.framework.Area;
import net.dries007.tfc.world.region.ChooseBiomes;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.helpers.WorldSeedHolder;
import net.yazloysasha.tfcrealworld.util.registry.DivergenceNoiseRegistry;
import net.yazloysasha.tfcrealworld.util.registry.HotspotsNoiseRegistry;
import net.yazloysasha.tfcrealworld.world.backport.ChooseBiomesSupport;
import net.yazloysasha.tfcrealworld.world.backport.GridSeededRandom;
import net.yazloysasha.tfcrealworld.world.backport.MapOceanBiomeFromAltitude;
import net.yazloysasha.tfcrealworld.world.backport.TfeBiomeQueries;
import net.yazloysasha.tfcrealworld.world.backport.TfeKarstBiomeInvoke;
import net.yazloysasha.tfcrealworld.world.backport.TfeMapDivergence;
import net.yazloysasha.tfcrealworld.world.biome.CoverageRareBiomes;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGDivergenceNoise;
import net.yazloysasha.tfcrealworld.world.region.MapBiomeLakeRolls;
import net.yazloysasha.tfcrealworld.world.region.MapTectonics;
import net.yazloysasha.tfcrealworld.world.region.RegionCoords;
import net.yazloysasha.tfcrealworld.world.volcano.MapHotspotBiomes;
import net.yazloysasha.tfcrealworld.world.volcano.MapHotspotLayout;
import net.yazloysasha.tfcrealworld.world.volcano.MapHotspotLayout.MountainStyle;
import net.yazloysasha.tfcrealworld.world.volcano.TfeCenteredFeatureAligner;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChooseBiomes.class, remap = false, priority = 1500)
public class TfeChooseBiomesMixin {

  @Unique
  private static final int[] RIFT_VALLEY_BIOMES = {
    RIFT_VALLEY,
    RIFT_VALLEY,
    RIFT_VALLEY,
    RIFT_LAKE,
    RIFT_LAKE,
  };

  @Unique
  private static final int[] LAND_RIFT_SPAWN_ROLL = { 1, 0 };

  @Unique
  private static final int[] MAP_SUBDUCTION_SHELF_BIOMES = {
    OCEANIC_VOLCANIC_ARC,
    OCEANIC_VOLCANIC_ARC,
    OCEANIC_VOLCANIC_ARC,
    VOLCANIC_ISLAND,
    OCEANIC_VOLCANIC_ARC,
  };

  @Unique
  private static final float COASTAL_VOLCANIC_ICE_SHEET_BASE_TEMP = -16f;

  @Unique
  private static final float COASTAL_VOLCANIC_ICE_SHEET_RAIN_SCALE = 0.006f;

  @Unique
  private static final float COASTAL_VOLCANIC_ICE_SHEET_TEMP_OFFSET = 2f;

  @Unique
  private static final float COASTAL_VOLCANIC_GLACIATED_TEMP_OFFSET = 6f;

  @Unique
  private static final float COASTAL_VOLCANIC_CARVED_TEMP_OFFSET = 10f;

  @Unique
  private static final float VOLCANIC_OCEANIC_GLACIAL_TEMP_CHAOS = 1f;

  @Unique
  private static final double VOLCANIC_OCEANIC_GLACIAL_ECOTONE_CHANCE = 0.5;

  @Unique
  private static final ThreadLocal<Region> CURRENT_REGION = new ThreadLocal<>();

  @Unique
  private static final ThreadLocal<Boolean> ASSIGNING_HOTSPOT_BIOME =
    ThreadLocal.withInitial(() -> Boolean.FALSE);

  @Inject(method = "apply", at = @At("HEAD"))
  private void tfcrealworld$prepareMapChooseBiomes(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    CURRENT_REGION.set(context.region);
    TfeMapDivergence.stampFromMapAndSuppressDefaultRifts(context);
    if (TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get()) {
      final MapHotspotLayout layout = HotspotsNoiseRegistry.biomeLayout();
      if (layout != null) {
        layout.prepareChooseBiomes(context.region, WorldSeedHolder.getSeed());
        tfcrealworld$markStratovolcanoVolcanic(context.region, layout);
      }
    }
    tfcrealworld$markTrenchMountainsVolcanic(context);
  }

  @Inject(method = "apply", at = @At("TAIL"))
  private void tfcrealworld$tfeAfterChooseBiomes(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    tfcrealworld$adjustMountainAndHotspotBiomes(context.region);
    final long worldSeed = WorldSeedHolder.getSeed();
    tfcrealworld$applyMapOceanAndRiftBiomes(context);
    tfcrealworld$applyVolcanicOceanicGlacialBands(context.region, worldSeed);
    ChooseBiomesSupport.rollIceSheetEdgeLakes(
      context.region,
      worldSeed,
      ICE_SHEET_EDGE,
      TFCLayers::lakeFor
    );
    MapBiomeLakeRolls.rollOceanicMountainLakes(
      context.region,
      worldSeed,
      OCEANIC_MOUNTAINS,
      VOLCANIC_OCEANIC_MOUNTAINS,
      TFCLayers::lakeFor
    );
    if (
      TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get() ||
      TFCRealWorldConfig.ALTITUDE_FROM_MAP.get() ||
      MapTectonics.isActive(context.generator())
    ) {
      TfeCenteredFeatureAligner.align(context.region, worldSeed);
    }
    CURRENT_REGION.remove();
    ASSIGNING_HOTSPOT_BIOME.remove();
    ChooseBiomesSupport.CURRENT_POINT.remove();
  }

  @Redirect(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/region/ChooseBiomes;getHotSpotBiome(I)I"
    )
  )
  private int tfcrealworld$tfeMapHotspotBiomeOrKeepVanillaMountain(
    ChooseBiomes instance,
    int age
  ) {
    ASSIGNING_HOTSPOT_BIOME.set(Boolean.TRUE);
    final Region.Point point = ChooseBiomesSupport.CURRENT_POINT.get();
    if (point != null && tfcrealworld$shouldKeepMountainBiome(point)) {
      return point.biome;
    }
    return TfeKarstBiomeInvoke.hotSpotBiome(instance, age);
  }

  @Redirect(
    method = "apply",
    at = @At(
      value = "FIELD",
      target = "Lnet/dries007/tfc/world/region/Region$Point;biome:I",
      opcode = Opcodes.PUTFIELD
    )
  )
  private void tfcrealworld$tfeKeepMapMountainBiomeUnderHotspot(
    Region.Point point,
    int proposedBiome
  ) {
    ChooseBiomesSupport.CURRENT_POINT.set(point);
    try {
      if (tfcrealworld$tfeHotspotWouldReplaceMountain(point, proposedBiome)) {
        if (tfcrealworld$shouldPaintStratovolcano(point)) {
          proposedBiome = MapHotspotBiomes.volcanicMountainFor(
            point,
            VOLCANIC_MOUNTAINS,
            VOLCANIC_OCEANIC_MOUNTAINS
          );
        } else {
          return;
        }
      }
      point.biome = proposedBiome;
    } finally {
      ASSIGNING_HOTSPOT_BIOME.set(Boolean.FALSE);
    }
  }

  @Unique
  private static void tfcrealworld$adjustMountainAndHotspotBiomes(
    Region region
  ) {
    final Region.Point[] data = region.data();
    for (int index = 0; index < data.length; index++) {
      final Region.Point point = data[index];
      if (point == null) {
        continue;
      }
      int biome = point.biome;
      if (tfcrealworld$tfeHotspotWouldReplaceMountain(point, biome)) {
        biome = tfcrealworld$shouldPaintStratovolcano(point)
          ? MapHotspotBiomes.volcanicMountainFor(
            point,
            VOLCANIC_MOUNTAINS,
            VOLCANIC_OCEANIC_MOUNTAINS
          )
          : (point.coastalMountain() ? OCEANIC_MOUNTAINS : MOUNTAINS);
      } else if (
        TFCRealWorldConfig.ALTITUDE_FROM_MAP.get() &&
        point.mountain() &&
        ChooseBiomesSupport.isSoftMountainFill(
          biome,
          OLD_MOUNTAINS,
          PLATEAU,
          PLATEAU_WIDE,
          HIGHLANDS,
          ROLLING_HILLS,
          ROCKY_PLATEAU
        )
      ) {
        biome = point.coastalMountain() ? OCEANIC_MOUNTAINS : MOUNTAINS;
      }
      point.biome = biome;
    }
  }

  @Unique
  private static void tfcrealworld$markStratovolcanoVolcanic(
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

  @Unique
  private static void tfcrealworld$markTrenchMountainsVolcanic(
    RegionGenerator.Context context
  ) {
    if (!TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()) {
      return;
    }
    if (!MapTectonics.isActive(context.generator())) {
      return;
    }
    final Region.Point[] data = context.region.data();
    for (final Region.Point point : data) {
      if (point == null || !point.land() || !point.mountain()) {
        continue;
      }
      final NTEPointAccess access = (NTEPointAccess) point;
      if (access.nte$getDivergence() < MapTectonics.TRENCH_DIVERGENCE) {
        access.nte$setVolcanic(true);
      }
    }
  }

  @Unique
  private static boolean tfcrealworld$tfeHotspotWouldReplaceMountain(
    Region.Point point,
    int proposedBiome
  ) {
    if (!tfcrealworld$shouldKeepMountainBiome(point)) {
      return false;
    }
    return (
      Boolean.TRUE.equals(ASSIGNING_HOTSPOT_BIOME.get()) ||
      tfcrealworld$isShieldHotspotBiome(proposedBiome)
    );
  }

  @Unique
  private static boolean tfcrealworld$isShieldHotspotBiome(int biome) {
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

  @Unique
  private static boolean tfcrealworld$shouldKeepMountainBiome(
    Region.Point point
  ) {
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

  @Unique
  private static boolean tfcrealworld$shouldPaintStratovolcano(
    Region.Point point
  ) {
    if (tfcrealworld$isIceMountain(point.biome)) {
      return false;
    }
    final MapHotspotLayout layout = HotspotsNoiseRegistry.biomeLayout();
    final Region region = CURRENT_REGION.get();
    if (layout == null || region == null) {
      return false;
    }
    return layout.styleAt(region, point) == MountainStyle.STRATOVOLCANO;
  }

  @Unique
  private static boolean tfcrealworld$isIceMountain(int biome) {
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

  @Unique
  private void tfcrealworld$applyMapOceanAndRiftBiomes(
    RegionGenerator.Context context
  ) {
    final RegionGenerator generator = context.generator();
    final boolean altitudeFromMap = TFCRealWorldConfig.ALTITUDE_FROM_MAP.get();
    final PNGDivergenceNoise divergenceNoise = MapTectonics.isActive(generator)
      ? DivergenceNoiseRegistry.get(generator)
      : null;
    if (divergenceNoise == null && !altitudeFromMap) {
      return;
    }

    final Area blobArea = generator.biomeArea.get();
    final long rngSeed = context.random.nextLong();
    final Region region = context.region;
    final Region.Point[] data = region.data();
    for (int index = 0; index < data.length; index++) {
      final Region.Point point = data[index];
      if (point == null) {
        continue;
      }
      final int x = RegionCoords.gridX(region, index);
      final int z = RegionCoords.gridZ(region, index);
      if (divergenceNoise != null) {
        tfcrealworld$applyLandRiftBiomes(
          region,
          point,
          x,
          z,
          divergenceNoise,
          blobArea,
          rngSeed
        );
      }
      if (altitudeFromMap && !tfcrealworld$skipOceanBiome(point)) {
        MapOceanBiomeFromAltitude.assign(
          point,
          x,
          z,
          (NTEPointAccess) point,
          divergenceNoise,
          blobArea,
          rngSeed,
          MAP_SUBDUCTION_SHELF_BIOMES
        );
      }
    }
  }

  @Unique
  private static void tfcrealworld$applyLandRiftBiomes(
    Region region,
    Region.Point point,
    int x,
    int z,
    PNGDivergenceNoise divergenceNoise,
    Area blobArea,
    long rngSeed
  ) {
    final NTEPointAccess access = (NTEPointAccess) point;
    if (
      !point.land() ||
      point.island() ||
      access.nte$getHotSpotAge() > 0 ||
      tfcrealworld$skipLandRiftBiomeReplace(point) ||
      !MapTectonics.isLandRiftCore(
        divergenceNoise,
        x,
        z,
        (float) access.nte$getDivergence()
      )
    ) {
      return;
    }
    final int areaSeed = blobArea.get(x, z);
    if (
      ChooseBiomesSupport.randomSeededFrom(
        rngSeed,
        areaSeed,
        LAND_RIFT_SPAWN_ROLL
      ) !=
      1
    ) {
      return;
    }
    if (point.distanceToOcean > 2) {
      point.biome = ChooseBiomesSupport.randomSeededFrom(
        rngSeed,
        areaSeed ^ 0x5f3759df,
        RIFT_VALLEY_BIOMES
      );
    } else {
      point.biome = RIFT_VALLEY;
    }
  }

  @Unique
  private static boolean tfcrealworld$skipOceanBiome(Region.Point point) {
    final NTEPointAccess access = (NTEPointAccess) point;
    return (
      point.land() ||
      point.island() ||
      point.mountain() ||
      access.nte$getHotSpotAge() > 0 ||
      access.nte$isBarrierIsland()
    );
  }

  @Unique
  private static boolean tfcrealworld$skipLandRiftBiomeReplace(
    Region.Point point
  ) {
    if (point.lake() || TfeBiomeQueries.isLake(point.biome)) {
      return true;
    }
    if (CoverageRareBiomes.preserve(point.biome)) {
      return true;
    }
    if (((NTEPointAccess) point).nte$isSurfaceRockKarst()) {
      return true;
    }
    return (
      TfeBiomeQueries.isFlatIceSheet(point.biome) ||
      point.biome == ICE_SHEET_EDGE ||
      point.biome == ICE_SHEET_SHORE
    );
  }

  @Unique
  private static void tfcrealworld$applyVolcanicOceanicGlacialBands(
    Region region,
    long worldSeed
  ) {
    if (!TFCRealWorldConfig.KOPPEN_FROM_MAP.get()) {
      return;
    }
    final Region.Point[] data = region.data();
    for (int index = 0; index < data.length; index++) {
      final Region.Point point = data[index];
      if (
        point == null ||
        !point.land() ||
        point.lake() ||
        TFCLayers.hasLake(point.biome) ||
        !tfcrealworld$isVolcanicOceanicMountainFamily(point.biome)
      ) {
        continue;
      }
      final float maxIceSheetTemp =
        COASTAL_VOLCANIC_ICE_SHEET_BASE_TEMP +
        COASTAL_VOLCANIC_ICE_SHEET_RAIN_SCALE * point.rainfall;
      final int gridX = RegionCoords.gridX(region, index);
      final int gridZ = RegionCoords.gridZ(region, index);
      final float biomeTemp =
        point.temperature +
        VOLCANIC_OCEANIC_GLACIAL_TEMP_CHAOS *
        GridSeededRandom.signedUnit(
          worldSeed,
          gridX,
          gridZ,
          0x51c3e90a7b6d24f8L
        );
      point.biome = tfcrealworld$coastalVolcanicOceanicBiomeForTemp(
        biomeTemp,
        maxIceSheetTemp
      );
    }
    tfcrealworld$paintVolcanicOceanicGlacialEcotone(region, worldSeed);
  }

  @Unique
  private static void tfcrealworld$paintVolcanicOceanicGlacialEcotone(
    Region region,
    long worldSeed
  ) {
    final IntArrayList toGlaciated = new IntArrayList();
    final IntArrayList toCarved = new IntArrayList();
    final Region.Point[] data = region.data();
    for (int index = 0; index < data.length; index++) {
      final Region.Point point = data[index];
      if (
        point == null ||
        !point.land() ||
        point.lake() ||
        TFCLayers.hasLake(point.biome)
      ) {
        continue;
      }
      final int gridX = RegionCoords.gridX(region, index);
      final int gridZ = RegionCoords.gridZ(region, index);
      if (
        !GridSeededRandom.chance(
          worldSeed,
          gridX,
          gridZ,
          0x2e9b14c86a70d5f3L,
          VOLCANIC_OCEANIC_GLACIAL_ECOTONE_CHANCE
        )
      ) {
        continue;
      }
      if (
        point.biome == ICE_SHEET_VOLCANIC_OCEANIC_MOUNTAINS &&
        tfcrealworld$touchesWarmerVolcanicOceanic(region, index)
      ) {
        toGlaciated.add(index);
      } else if (
        point.biome == VOLCANIC_OCEANIC_MOUNTAINS &&
        tfcrealworld$touchesIceOrGlaciatedVolcanicOceanic(region, index)
      ) {
        toCarved.add(index);
      }
    }
    for (int i = 0; i < toGlaciated.size(); i++) {
      final Region.Point point = data[toGlaciated.getInt(i)];
      if (point != null) {
        point.biome = GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS;
      }
    }
    for (int i = 0; i < toCarved.size(); i++) {
      final Region.Point point = data[toCarved.getInt(i)];
      if (point != null) {
        point.biome = GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS;
      }
    }
  }

  @Unique
  private static int tfcrealworld$coastalVolcanicOceanicBiomeForTemp(
    float temp,
    float maxIceSheetTemp
  ) {
    if (temp < maxIceSheetTemp + COASTAL_VOLCANIC_ICE_SHEET_TEMP_OFFSET) {
      return ICE_SHEET_VOLCANIC_OCEANIC_MOUNTAINS;
    }
    if (temp < maxIceSheetTemp + COASTAL_VOLCANIC_GLACIATED_TEMP_OFFSET) {
      return GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS;
    }
    if (temp < maxIceSheetTemp + COASTAL_VOLCANIC_CARVED_TEMP_OFFSET) {
      return GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS;
    }
    return VOLCANIC_OCEANIC_MOUNTAINS;
  }

  @Unique
  private static boolean tfcrealworld$isVolcanicOceanicMountainFamily(
    int biome
  ) {
    return (
      biome == VOLCANIC_OCEANIC_MOUNTAINS ||
      biome == ICE_SHEET_VOLCANIC_OCEANIC_MOUNTAINS ||
      biome == GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS ||
      biome == GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS
    );
  }

  @Unique
  private static boolean tfcrealworld$touchesWarmerVolcanicOceanic(
    Region region,
    int index
  ) {
    return tfcrealworld$anyNeighbor(
      region,
      index,
      biome ->
        biome == VOLCANIC_OCEANIC_MOUNTAINS ||
        biome == GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS ||
        biome == GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS
    );
  }

  @Unique
  private static boolean tfcrealworld$touchesIceOrGlaciatedVolcanicOceanic(
    Region region,
    int index
  ) {
    return tfcrealworld$anyNeighbor(
      region,
      index,
      biome ->
        biome == ICE_SHEET_VOLCANIC_OCEANIC_MOUNTAINS ||
        biome == GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS
    );
  }

  @Unique
  private static boolean tfcrealworld$anyNeighbor(
    Region region,
    int index,
    java.util.function.IntPredicate match
  ) {
    final int[] dx = { 1, -1, 0, 0 };
    final int[] dz = { 0, 0, 1, -1 };
    for (int i = 0; i < 4; i++) {
      final Region.Point neighbor = RegionCoords.atOffset(
        region,
        index,
        dx[i],
        dz[i]
      );
      if (neighbor != null && neighbor.land() && match.test(neighbor.biome)) {
        return true;
      }
    }
    return false;
  }
}
