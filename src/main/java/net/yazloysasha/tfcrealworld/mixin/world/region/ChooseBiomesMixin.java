package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.layer.framework.Area;
import net.dries007.tfc.world.region.ChooseBiomes;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.helpers.WorldSeedHolder;
import net.yazloysasha.tfcrealworld.util.registry.HotspotsNoiseRegistry;
import net.yazloysasha.tfcrealworld.world.region.BiomePools;
import net.yazloysasha.tfcrealworld.world.region.MapBiomeLakeRolls;
import net.yazloysasha.tfcrealworld.world.region.RegionCoords;
import net.yazloysasha.tfcrealworld.world.region.TfcMapOceanBiomes;
import net.yazloysasha.tfcrealworld.world.volcano.CenteredFeatureAligner;
import net.yazloysasha.tfcrealworld.world.volcano.MapHotspotBiomes;
import net.yazloysasha.tfcrealworld.world.volcano.MapHotspotLayout;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = ChooseBiomes.class, remap = false)
public class ChooseBiomesMixin {

  @Unique
  private static final ThreadLocal<int[]> CURRENT_GRID_POS =
    ThreadLocal.withInitial(() -> new int[] { 0, 0 });

  @Unique
  private static final ThreadLocal<Boolean> CURRENT_IN_HOTSPOT =
    ThreadLocal.withInitial(() -> Boolean.FALSE);

  @Unique
  private static volatile BiomePools POOLS;

  @Inject(method = "apply", at = @At("HEAD"))
  private void tfcrealworld$setupVolcanicFiltering(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    tfcrealworld$ensurePoolsInitialized();
    if (!TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get()) {
      return;
    }
    final MapHotspotLayout layout = HotspotsNoiseRegistry.biomeLayout();
    if (layout == null) {
      return;
    }
    layout.prepareChooseBiomes(context.region, WorldSeedHolder.getSeed());
    tfcrealworld$applyMapHotspotLand(context.region, layout);
  }

  /**
   * TFC 3 has no {@code AddHotspots} task; mirror 1.21.1 {@code AddHotspotsMixin}
   * so young map hotspots become land before biome paint (age 4 stays ocean).
   */
  @Unique
  private static void tfcrealworld$applyMapHotspotLand(
    Region region,
    MapHotspotLayout layout
  ) {
    final Region.Point[] data = region.data();
    for (int index = 0; index < data.length; index++) {
      final Region.Point point = data[index];
      if (point == null) {
        continue;
      }
      final byte age = layout.ageAtGrid(
        RegionCoords.gridX(region, index),
        RegionCoords.gridZ(region, index)
      );
      if (MapHotspotBiomes.shouldSetLandForMapAge(age)) {
        point.setLand();
      }
    }
  }

  @Inject(method = "apply", at = @At("TAIL"))
  private void tfcrealworld$afterChooseBiomes(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    TfcMapOceanBiomes.apply(context);
    MapBiomeLakeRolls.rollOceanicMountainLakes(
      context.region,
      WorldSeedHolder.getSeed(),
      TFCLayers.OCEANIC_MOUNTAINS,
      TFCLayers.VOLCANIC_OCEANIC_MOUNTAINS,
      TFCLayers::lakeFor
    );
    if (
      TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get() ||
      TFCRealWorldConfig.ALTITUDE_FROM_MAP.get()
    ) {
      CenteredFeatureAligner.alignTfc(
        context.region,
        WorldSeedHolder.getSeed()
      );
    }
    CURRENT_GRID_POS.remove();
    CURRENT_IN_HOTSPOT.remove();
  }

  @Inject(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/layer/framework/Area;get(II)I"
    ),
    locals = LocalCapture.CAPTURE_FAILHARD
  )
  private void tfcrealworld$captureGridPosForHotspotMask(
    RegionGenerator.Context context,
    CallbackInfo ci,
    Region region,
    Area blobArea,
    long rngSeed,
    long climateSeed,
    int x,
    int z,
    Region.Point point
  ) {
    final int[] pos = CURRENT_GRID_POS.get();
    pos[0] = x;
    pos[1] = z;
    CURRENT_IN_HOTSPOT.set(tfcrealworld$isInHotspot(x, z));
  }

  @Unique
  private static int tfcrealworld$altitudeMountainBiome(Region.Point point) {
    if (
      TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get() && CURRENT_IN_HOTSPOT.get()
    ) {
      final MapHotspotLayout layout = HotspotsNoiseRegistry.biomeLayout();
      if (layout != null) {
        final int[] pos = CURRENT_GRID_POS.get();
        final byte age = layout.ageAtGrid(pos[0], pos[1]);
        if (MapHotspotBiomes.shouldUseVolcanicMountainForMapAge(age)) {
          return MapHotspotBiomes.volcanicMountainFor(
            point,
            TFCLayers.VOLCANIC_MOUNTAINS,
            TFCLayers.VOLCANIC_OCEANIC_MOUNTAINS
          );
        }
      }
    }
    return point.coastalMountain()
      ? TFCLayers.OCEANIC_MOUNTAINS
      : TFCLayers.MOUNTAINS;
  }

  @Unique
  private static boolean tfcrealworld$isInHotspot(int x, int z) {
    final MapHotspotLayout layout = HotspotsNoiseRegistry.biomeLayout();
    return layout != null && layout.ageAtGrid(x, z) > 0;
  }

  @Unique
  private static void tfcrealworld$ensurePoolsInitialized() {
    if (POOLS != null) return;
    synchronized (ChooseBiomesMixin.class) {
      if (POOLS != null) return;
      POOLS = BiomePools.build();
    }
  }

  @Unique
  private static boolean tfcrealworld$isVolcanicLayer(int layerId) {
    final BiomePools pools = POOLS;
    if (
      pools != null && layerId >= 0 && layerId < pools.isVolcanicLayer.length
    ) {
      return pools.isVolcanicLayer[layerId];
    }
    return TFCLayers.getFromLayerId(layerId).isVolcanic();
  }

  @Redirect(
    method = "apply",
    at = @At(
      value = "FIELD",
      target = "Lnet/dries007/tfc/world/region/Region$Point;biome:I",
      opcode = Opcodes.PUTFIELD
    )
  )
  private void tfcrealworld$forceBiomeVolcanicInHotspots(
    Region.Point point,
    int proposedBiome
  ) {
    if (TFCRealWorldConfig.ALTITUDE_FROM_MAP.get() && point.mountain()) {
      proposedBiome = tfcrealworld$altitudeMountainBiome(point);
    }

    if (!TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get()) {
      point.biome = proposedBiome;
      return;
    }

    final MapHotspotLayout layout = HotspotsNoiseRegistry.biomeLayout();
    if (layout == null) {
      point.biome = proposedBiome;
      return;
    }

    final int[] pos = CURRENT_GRID_POS.get();
    final boolean inHotspot = CURRENT_IN_HOTSPOT.get();
    final boolean proposedIsVolcanic = tfcrealworld$isVolcanicLayer(
      proposedBiome
    );

    if (inHotspot) {
      point.biome = MapHotspotBiomes.assignTfc(
        point,
        proposedBiome,
        proposedIsVolcanic,
        pos[0],
        pos[1],
        layout
      );
    } else {
      point.biome = proposedIsVolcanic
        ? POOLS.pickNonVolcanic(point, pos[0], pos[1], proposedBiome)
        : proposedBiome;
    }
  }
}
