package net.yazloysasha.tfcrealworld.mixin.world.region;

import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.layer.framework.Area;
import net.dries007.tfc.world.region.ChooseBiomes;
import net.dries007.tfc.world.region.Region;
import net.yazloysasha.tfcrealworld.util.helpers.RegionContextHolder;
import net.yazloysasha.tfcrealworld.util.registry.HotspotsNoiseRegistry;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGHotspotsNoise;
import net.yazloysasha.tfcrealworld.world.region.BiomePools;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChooseBiomes.class, remap = false)
public class ChooseBiomesMixin {

  @Unique
  private static final float ACTIVE_VOLCANIC_BIOME_SIZE_MULTIPLIER = 1.4f;

  @Unique
  private static final float INACTIVE_VOLCANIC_BIOME_SIZE_MULTIPLIER = 1.0f;

  @Unique
  private static final ThreadLocal<int[]> CURRENT_GRID_POS =
    ThreadLocal.withInitial(() -> new int[] { 0, 0 });

  @Unique
  private static final ThreadLocal<Boolean> CURRENT_IN_HOTSPOT =
    ThreadLocal.withInitial(() -> Boolean.FALSE);

  @Unique
  private static final ThreadLocal<PNGHotspotsNoise> CURRENT_HOTSPOTS =
    new ThreadLocal<>();

  @Unique
  private static volatile BiomePools POOLS;

  @Inject(
    method = "apply(Lnet/dries007/tfc/world/region/RegionGenerator$Context;)V",
    at = @At("HEAD"),
    remap = false
  )
  private void tfcrealworld$setupVolcanicFiltering(CallbackInfo ci) {
    final var generator = RegionContextHolder.getGenerator();
    if (generator == null) {
      return;
    }
    CURRENT_HOTSPOTS.set(HotspotsNoiseRegistry.get(generator));
    tfcrealworld$ensurePoolsInitialized();
  }

  @Inject(
    method = "apply(Lnet/dries007/tfc/world/region/RegionGenerator$Context;)V",
    at = @At("TAIL"),
    remap = false
  )
  private void tfcrealworld$cleanupVolcanicFiltering(CallbackInfo ci) {
    CURRENT_HOTSPOTS.remove();
  }

  @Redirect(
    method = "apply(Lnet/dries007/tfc/world/region/RegionGenerator$Context;)V",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/layer/framework/Area;get(II)I"
    ),
    remap = false
  )
  private int tfcrealworld$captureGridPosForHotspotMask(
    Area blobArea,
    int x,
    int z
  ) {
    final int[] pos = CURRENT_GRID_POS.get();
    pos[0] = x;
    pos[1] = z;

    final PNGHotspotsNoise hotspots = CURRENT_HOTSPOTS.get();
    CURRENT_IN_HOTSPOT.set(tfcrealworld$isInHotspot(hotspots, x, z));

    return blobArea.get(x, z);
  }

  @Unique
  private static boolean tfcrealworld$isInHotspot(
    PNGHotspotsNoise hotspots,
    int x,
    int z
  ) {
    if (hotspots == null) return false;

    final byte ageHere = hotspots.getHotSpotAge(x, z);
    if (ageHere > 0) return true;

    final float m = ACTIVE_VOLCANIC_BIOME_SIZE_MULTIPLIER;

    final int r = Math.max(
      0,
      (int) Math.ceil((m - INACTIVE_VOLCANIC_BIOME_SIZE_MULTIPLIER) * 2.0f)
    );
    for (int dz = -r; dz <= r; dz++) {
      for (int dx = -r; dx <= r; dx++) {
        if (hotspots.hasActiveHotspot(x + dx, z + dz)) {
          return true;
        }
      }
    }
    return false;
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
    final int[] pos = CURRENT_GRID_POS.get();
    final boolean inHotspot = CURRENT_IN_HOTSPOT.get();

    final boolean proposedIsVolcanic = tfcrealworld$isVolcanicLayer(
      proposedBiome
    );

    if (inHotspot) {
      point.biome = proposedIsVolcanic
        ? proposedBiome
        : POOLS.pickVolcanic(point, pos[0], pos[1], proposedBiome);
    } else {
      point.biome = proposedIsVolcanic
        ? POOLS.pickNonVolcanic(point, pos[0], pos[1], proposedBiome)
        : proposedBiome;
    }
  }
}
