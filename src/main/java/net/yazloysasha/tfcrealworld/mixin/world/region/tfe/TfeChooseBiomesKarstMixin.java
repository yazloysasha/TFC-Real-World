package net.yazloysasha.tfcrealworld.mixin.world.region.tfe;

import static com.newterraearth.tfe.world.NTELayerIds.*;
import static net.dries007.tfc.world.layer.TFCLayers.HIGHLANDS;
import static net.dries007.tfc.world.layer.TFCLayers.LOWLANDS;
import static net.dries007.tfc.world.layer.TFCLayers.LOW_CANYONS;
import static net.dries007.tfc.world.layer.TFCLayers.OLD_MOUNTAINS;
import static net.dries007.tfc.world.layer.TFCLayers.PLAINS;
import static net.dries007.tfc.world.layer.TFCLayers.PLATEAU;
import static net.dries007.tfc.world.layer.TFCLayers.SALT_MARSH;

import com.newterraearth.tfe.world.region.NTEPointAccess;
import net.dries007.tfc.world.region.ChooseBiomes;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.world.backport.ChooseBiomesSupport;
import net.yazloysasha.tfcrealworld.world.backport.TfeKarstBiomeInvoke;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChooseBiomes.class, remap = false, priority = 1500)
public class TfeChooseBiomesKarstMixin {

  @Redirect(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/region/ChooseBiomes;getBurrenBiome(I)I"
    )
  )
  private int tfcrealworld$tfeExtendedBurrenBase(
    ChooseBiomes instance,
    int biome
  ) {
    return TfeKarstBiomeInvoke.burrenBiome(
      instance,
      ChooseBiomesSupport.burrenBase(
        biome,
        KNOB_AND_KETTLE,
        PATTERNED_GROUND,
        INVERTED_PATTERNED_GROUND,
        ICE_SHEET_EDGE,
        DRUMLINS,
        LOW_CANYONS,
        LOWLANDS,
        PLAINS,
        OLD_MOUNTAINS,
        HIGHLANDS,
        STAIR_STEP_CANYONS,
        MESAS,
        BUTTES,
        PLATEAU
      )
    );
  }

  /**
   * Same 1.21.1 / TFG coastal hook: only while TFE is already in the tower
   * karst climate branch. Do not remap post-karst {@code SALT_MARSH}.
   */
  @Redirect(
    method = "apply",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/world/region/ChooseBiomes;getTowerKarstBiome(I)I"
    )
  )
  private int tfcrealworld$tfeCoastalLowlandsAsMarshForTowerKarst(
    ChooseBiomes instance,
    int biome
  ) {
    final Region.Point point = ChooseBiomesSupport.CURRENT_POINT.get();
    if (point != null) {
      biome = ChooseBiomesSupport.towerKarstCoastalInput(
        point.distanceToOcean,
        biome,
        LOWLANDS,
        PLAINS,
        LOW_CANYONS,
        SALT_MARSH
      );
    }
    return TfeKarstBiomeInvoke.towerKarstBiome(instance, biome);
  }

  /**
   * If the in-apply redirect missed TFE's merged invoke, still convert coastal
   * tower-karst lake/plains to bay — only when the TFC 4 climate gate holds.
   */
  @Inject(method = "apply", at = @At("TAIL"))
  private void tfcrealworld$tfeTowerKarstCoastalPass(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    final Object chooseBiomes = this;
    final Region.Point[] data = context.region.data();
    for (int index = 0; index < data.length; index++) {
      final Region.Point point = data[index];
      if (point == null || !((NTEPointAccess) point).nte$isSurfaceRockKarst()) {
        continue;
      }
      if (
        !ChooseBiomesSupport.isTowerKarstClimate(
          point.rainfall,
          point.temperature
        )
      ) {
        continue;
      }
      final int before = point.biome;
      if (before == TOWER_KARST_BAY) {
        continue;
      }
      final int coastal = ChooseBiomesSupport.towerKarstCoastalInput(
        point.distanceToOcean,
        ChooseBiomesSupport.towerKarstCoastalBase(
          before,
          TOWER_KARST_LAKE,
          TOWER_KARST_PLAINS,
          LOWLANDS,
          PLAINS
        ),
        LOWLANDS,
        PLAINS,
        LOW_CANYONS,
        SALT_MARSH
      );
      if (coastal == before) {
        continue;
      }
      point.biome = TfeKarstBiomeInvoke.towerKarstBiome(chooseBiomes, coastal);
    }
  }
}
