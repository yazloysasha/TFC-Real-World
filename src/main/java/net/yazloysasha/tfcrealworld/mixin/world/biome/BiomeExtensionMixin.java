package net.yazloysasha.tfcrealworld.mixin.world.biome;

import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.biome.TFCBiomes;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.MapLakeWater;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BiomeExtension.class, remap = false)
public class BiomeExtensionMixin {

  /**
   * A lake biome takes its salinity from the map lake cells around the
   * column being sampled. Other biomes (oceans, land) stay as they are, so
   * the generator's own coastal rules still apply around a lake.
   */
  @Inject(method = "isSalty", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$saltLakeIsSalty(
    CallbackInfoReturnable<Boolean> cir
  ) {
    if (
      !MapLakeWater.isColumnOpen() ||
      !MapLakeWater.isLakeBiome((BiomeExtension) (Object) this)
    ) {
      return;
    }
    final Boolean salty = MapLakeWater.saltyFlagOverride();
    if (salty != null) {
      cir.setReturnValue(salty);
    }
  }

  /**
   * With coastlines from the map, the shore blends with the land, as in
   * TFC 3. TFC 2 blends it with the ocean, whose depth then pulls most of
   * the 64-block shore band under sea level.
   */
  @Inject(method = "getGroup", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$shoreBlendsWithLand(
    CallbackInfoReturnable<BiomeExtension.Group> cir
  ) {
    if (
      (Object) this == TFCBiomes.SHORE &&
      TFCRealWorldConfig.CONTINENT_FROM_MAP.get()
    ) {
      cir.setReturnValue(BiomeExtension.Group.LAND);
    }
  }
}
