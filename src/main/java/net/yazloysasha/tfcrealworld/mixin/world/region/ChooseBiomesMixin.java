package net.yazloysasha.tfcrealworld.mixin.world.region;

import static net.dries007.tfc.world.layer.TFCLayers.OCEAN_RIDGE;
import static net.dries007.tfc.world.layer.TFCLayers.SUNKEN_SHIELD_VOLCANO;

import com.llamalad7.mixinextras.sugar.Local;
import net.dries007.tfc.world.region.ChooseBiomes;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.region.MapTectonics;
import net.yazloysasha.tfcrealworld.world.volcano.CenteredFeatureAligner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keep hotspot ages from painting land shields onto ocean (AddHotspots never
 * {@code setLand}). Rift / ocean / collision / volcanic mountain biomes stay
 * vanilla ChooseBiomes consuming map divergence + distanceToEdge.
 * Age-4 sunken shields may overwrite OCEAN_RIDGE (vanilla omits that biome).
 */
@Mixin(value = ChooseBiomes.class, remap = false)
public class ChooseBiomesMixin {

  @Redirect(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/region/ChooseBiomes;getHotSpotBiome(I)I"
    )
  )
  private int tfcrealworld$hotspotBiomeWithoutRaisingLand(
    ChooseBiomes instance,
    int age,
    @Local Region.Point point
  ) {
    // Do not paint land shield biomes onto ocean (AddHotspots never setLand).
    // Vanilla age-4 sunken path omits OCEAN_RIDGE; allow sunken over ridges.
    if (!point.land()) {
      if (age == 4 && point.biome == OCEAN_RIDGE) {
        return SUNKEN_SHIELD_VOLCANO;
      }
      return point.biome;
    }
    return (
      (ChooseBiomesAccessor) (Object) instance
    ).tfcrealworld$invokeGetHotSpotBiome(age);
  }

  @Inject(method = "apply", at = @At("TAIL"))
  private void tfcrealworld$alignCenteredVolcanoes(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    if (
      !tfcrealworld$shouldAlignCenteredVolcanoes(
        TFCRealWorldConfig.ALTITUDE_FROM_MAP.get(),
        MapTectonics.isActive(context.generator())
      )
    ) {
      return;
    }
    CenteredFeatureAligner.align(
      context.region,
      context.generator().seed().seed()
    );
  }

  @Unique
  private static boolean tfcrealworld$shouldAlignCenteredVolcanoes(
    boolean altitudeFromMap,
    boolean mapTectonics
  ) {
    return (
      TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get() ||
      altitudeFromMap ||
      mapTectonics
    );
  }
}
