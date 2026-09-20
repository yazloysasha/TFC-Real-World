package net.yazloysasha.tfcrealworld.mixin.world.terrain.tfe;

import com.newterraearth.tfe.world.terrain.NTETerrainUpliftSampler;
import net.dries007.tfc.world.noise.Noise2D;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.volcano.MapHotspotNoise;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = NTETerrainUpliftSampler.class, remap = false)
public abstract class TfeTerrainUpliftSamplerMixin {

  @Shadow
  @Final
  private long seed;

  @Inject(
    method = "activeShieldVolcanoSourceNoise",
    at = @At("HEAD"),
    cancellable = true
  )
  private void tfcrealworld$mapActiveShieldSource(
    CallbackInfoReturnable<Noise2D> cir
  ) {
    if (TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get()) {
      cir.setReturnValue(MapHotspotNoise.forAge((byte) 1, seed));
    }
  }

  @Inject(
    method = "dormantShieldVolcanoSourceNoise",
    at = @At("HEAD"),
    cancellable = true
  )
  private void tfcrealworld$mapDormantShieldSource(
    CallbackInfoReturnable<Noise2D> cir
  ) {
    if (TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get()) {
      cir.setReturnValue(MapHotspotNoise.forAge((byte) 2, seed));
    }
  }

  @Inject(
    method = "ancientShieldVolcanoSourceNoise",
    at = @At("HEAD"),
    cancellable = true
  )
  private void tfcrealworld$mapAncientShieldSource(
    CallbackInfoReturnable<Noise2D> cir
  ) {
    if (TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get()) {
      cir.setReturnValue(MapHotspotNoise.forAge((byte) 4, seed));
    }
  }

  @Inject(
    method = "shieldVolcanoIntensitySourceNoise",
    at = @At("HEAD"),
    cancellable = true
  )
  private void tfcrealworld$mapCombinedShieldSource(
    CallbackInfoReturnable<Noise2D> cir
  ) {
    if (TFCRealWorldConfig.HOTSPOTS_FROM_MAP.get()) {
      cir.setReturnValue(MapHotspotNoise.combined(seed));
    }
  }
}
