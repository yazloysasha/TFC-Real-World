package net.yazloysasha.tfcrealworld.mixin.world.region.tfe;

import com.newterraearth.tfe.world.region.NTERegionFeatureAnnotations;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.world.backport.TfeMapDivergence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * TFE restamps {@code nte$setDivergence} from cellular plates right before
 * mountains. Rewrite PNG after that pass.
 */
@Mixin(value = NTERegionFeatureAnnotations.class, remap = false)
public class TfeRegionFeatureAnnotationsMixin {

  @Inject(method = "prepareForMountainPlacement", at = @At("RETURN"))
  private void tfcrealworld$restoreMapDivergenceAfterPlateRestamp(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    TfeMapDivergence.stampFromMap(context);
  }
}
