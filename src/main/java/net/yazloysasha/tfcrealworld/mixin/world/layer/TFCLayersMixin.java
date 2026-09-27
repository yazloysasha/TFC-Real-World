package net.yazloysasha.tfcrealworld.mixin.world.layer;

import static net.dries007.tfc.world.layer.TFCLayers.GLACIALLY_CARVED_VOLCANIC_MOUNTAINS;
import static net.dries007.tfc.world.layer.TFCLayers.GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS;
import static net.dries007.tfc.world.layer.TFCLayers.GLACIATED_VOLCANIC_MOUNTAINS;
import static net.dries007.tfc.world.layer.TFCLayers.GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.dries007.tfc.world.Seed;
import net.dries007.tfc.world.layer.IceSheetEdgeLayer;
import net.dries007.tfc.world.layer.ShoreAndRiverLayer;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.layer.ZoomLayer;
import net.dries007.tfc.world.layer.framework.AreaFactory;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.Units;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.registry.ContinentNoiseRegistry;
import net.yazloysasha.tfcrealworld.world.layer.MapLandOceanCorrectionLayer;
import net.yazloysasha.tfcrealworld.world.layer.WidenShoreInlandLayer;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TFCLayers.class, remap = false)
public class TFCLayersMixin {

  /**
   * CONTINENT_FROM_MAP biome pipeline:
   * <ol>
   *   <li>Vanilla Zoom → 64</li>
   *   <li>{@link IceSheetEdgeLayer} at 64 (vanilla-ish glacial/ice rim width
   *       after later zooms; late IceSheetEdge call is skipped)</li>
   *   <li>EXTRA Zooms → 32 → 16 + {@link MapLandOceanCorrectionLayer}</li>
   *   <li>ShoreAndRiver + {@link WidenShoreInlandLayer} ×3 ({@code shoreFor} only)</li>
   *   <li>Vanilla MoreShores (unchanged)</li>
   *   <li>Skip late IceSheetEdge; skip 2 post-shore Zooms → quart</li>
   * </ol>
   */
  @Unique
  private static final int tfcrealworld$VANILLA_PRE_SHORE_ZOOMS = 1;

  @Unique
  private static final int tfcrealworld$ZOOMS_GRID_TO_CORRECTION =
    Units.GRID_BITS - (Units.QUART_BITS + 2);

  @Unique
  private static final int tfcrealworld$EXTRA_PRE_CORRECTION_ZOOMS =
    tfcrealworld$ZOOMS_GRID_TO_CORRECTION -
    tfcrealworld$VANILLA_PRE_SHORE_ZOOMS;

  /**
   * Inland widen passes after ShoreAndRiver at 16 (~64 blocks with first ring).
   */
  @Unique
  private static final int tfcrealworld$SHORE_WIDEN_INLAND_PASSES = 3;

  /**
   * Factory-build-time: how many post-shore ZoomLayer.apply calls to no-op so
   * net zoom depth stays grid→quart after EXTRA pre-correction zooms.
   */
  @Unique
  private static final ThreadLocal<Integer> tfcrealworld$skipPostShoreZooms =
    ThreadLocal.withInitial(() -> 0);

  /**
   * When set, the vanilla late {@link IceSheetEdgeLayer} call is a no-op
   * (already applied at 64 before EXTRA zooms).
   */
  @Unique
  private static final ThreadLocal<Boolean> tfcrealworld$skipLateIceSheetEdge =
    ThreadLocal.withInitial(() -> false);

  @Inject(method = "createRegionBiomeLayer", at = @At("HEAD"))
  private static void tfcrealworld$resetLayerFlags(
    RegionGenerator generator,
    Seed seed,
    CallbackInfoReturnable<AreaFactory> cir
  ) {
    tfcrealworld$skipPostShoreZooms.set(0);
    tfcrealworld$skipLateIceSheetEdge.set(false);
  }

  @WrapOperation(
    method = "createRegionBiomeLayer",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/layer/ShoreAndRiverLayer;apply(JLnet/dries007/tfc/world/layer/framework/AreaFactory;)Lnet/dries007/tfc/world/layer/framework/AreaFactory;"
    )
  )
  private static AreaFactory tfcrealworld$injectMapLandOceanCorrection(
    ShoreAndRiverLayer instance,
    long shoreSeed,
    AreaFactory prev,
    Operation<AreaFactory> original,
    RegionGenerator generator,
    Seed seed
  ) {
    AreaFactory layer = prev;
    if (TFCRealWorldConfig.CONTINENT_FROM_MAP.get()) {
      final PNGContinentNoise noise = ContinentNoiseRegistry.get(generator);
      if (noise != null) {
        layer = IceSheetEdgeLayer.INSTANCE.apply(
          shoreSeed ^ 0x49434531L,
          layer
        );
        tfcrealworld$skipLateIceSheetEdge.set(true);

        long zoomSeed = shoreSeed ^ 0x5A4F4F4DL;
        for (int i = 0; i < tfcrealworld$EXTRA_PRE_CORRECTION_ZOOMS; i++) {
          layer = ZoomLayer.NORMAL.apply(zoomSeed, layer);
          zoomSeed = zoomSeed * 0x9E3779B97F4A7C15L + 1L;
        }
        layer = new MapLandOceanCorrectionLayer(
          noise,
          generator,
          tfcrealworld$ZOOMS_GRID_TO_CORRECTION
        ).apply(shoreSeed ^ 0x4D4C4F43L, layer);
        tfcrealworld$skipPostShoreZooms.set(
          tfcrealworld$EXTRA_PRE_CORRECTION_ZOOMS
        );

        layer = original.call(instance, shoreSeed, layer);
        long widenSeed = shoreSeed ^ 0x57494445L; // "WIDE"
        for (int i = 0; i < tfcrealworld$SHORE_WIDEN_INLAND_PASSES; i++) {
          layer = WidenShoreInlandLayer.INSTANCE.apply(widenSeed, layer);
          widenSeed = widenSeed * 0x9E3779B97F4A7C15L + 1L;
        }
        return layer;
      }
    }
    return original.call(instance, shoreSeed, layer);
  }

  @WrapOperation(
    method = "createRegionBiomeLayer",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/layer/IceSheetEdgeLayer;apply(JLnet/dries007/tfc/world/layer/framework/AreaFactory;)Lnet/dries007/tfc/world/layer/framework/AreaFactory;"
    )
  )
  private static AreaFactory tfcrealworld$maybeSkipLateIceSheetEdge(
    IceSheetEdgeLayer instance,
    long iceSeed,
    AreaFactory prev,
    Operation<AreaFactory> original
  ) {
    if (Boolean.TRUE.equals(tfcrealworld$skipLateIceSheetEdge.get())) {
      return prev;
    }
    return original.call(instance, iceSeed, prev);
  }

  @WrapOperation(
    method = "createRegionBiomeLayer",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/layer/ZoomLayer;apply(JLnet/dries007/tfc/world/layer/framework/AreaFactory;)Lnet/dries007/tfc/world/layer/framework/AreaFactory;"
    )
  )
  private static AreaFactory tfcrealworld$maybeSkipPostShoreZoom(
    ZoomLayer instance,
    long zoomSeed,
    AreaFactory prev,
    Operation<AreaFactory> original
  ) {
    final int left = tfcrealworld$skipPostShoreZooms.get();
    if (left > 0) {
      tfcrealworld$skipPostShoreZooms.set(left - 1);
      return prev;
    }
    return original.call(instance, zoomSeed, prev);
  }

  @Inject(method = "hasShore", at = @At("HEAD"), cancellable = true)
  private static void tfcrealworld$keepGlaciatedVolcanicOceanicMountains(
    int value,
    CallbackInfoReturnable<Boolean> cir
  ) {
    if (value == GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS) {
      cir.setReturnValue(false);
    }
  }

  @Inject(method = "hasLake", at = @At("HEAD"), cancellable = true)
  private static void tfcrealworld$noLakesOnVolcanicGlacialMountains(
    int value,
    CallbackInfoReturnable<Boolean> cir
  ) {
    if (
      value == GLACIATED_VOLCANIC_OCEANIC_MOUNTAINS ||
      value == GLACIALLY_CARVED_VOLCANIC_OCEANIC_MOUNTAINS ||
      value == GLACIATED_VOLCANIC_MOUNTAINS ||
      value == GLACIALLY_CARVED_VOLCANIC_MOUNTAINS
    ) {
      cir.setReturnValue(false);
    }
  }
}
