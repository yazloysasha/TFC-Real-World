package net.yazloysasha.tfcrealworld.mixin.world.region.tfe;

import com.newterraearth.tfe.world.region.NTEPointAccess;
import com.newterraearth.tfe.world.region.NTERegionGeneratorAccess;
import net.dries007.tfc.world.region.AddContinents;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.backport.TfeMapDivergence;
import net.yazloysasha.tfcrealworld.world.region.MapTectonics;
import net.yazloysasha.tfcrealworld.world.region.RegionCoords;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AddContinents.class, remap = false, priority = 1500)
public class TfeAddContinentsMixin {

  @Unique
  private static final double LAND_THRESHOLD = 4.4;

  @Unique
  private static final double CONTINENTAL_SHELF_THRESHOLD = 3.3;

  @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
  private void tfcrealworld$applyContinentsFromMapOnly(
    RegionGenerator.Context context,
    CallbackInfo ci
  ) {
    if (!TFCRealWorldConfig.CONTINENT_FROM_MAP.get()) {
      return;
    }

    TfeMapDivergence.stampFromMap(context);

    final Region region = context.region;
    final RegionGenerator generator = context.generator();
    final NTERegionGeneratorAccess generatorAccess =
      (NTERegionGeneratorAccess) generator;
    final boolean altitudeFromMap = TFCRealWorldConfig.ALTITUDE_FROM_MAP.get();
    final boolean mapTectonics = MapTectonics.isActive(generator);

    final Region.Point[] data = region.data();
    for (int index = 0; index < data.length; index++) {
      final Region.Point point = data[index];
      if (point == null) {
        continue;
      }
      final int gridX = RegionCoords.gridX(region, index);
      final int gridZ = RegionCoords.gridZ(region, index);
      final NTEPointAccess access = (NTEPointAccess) point;

      final double tectonicFeatures = mapTectonics
        ? MapTectonics.continentRiftAdjustment(
          (float) access.nte$getDivergence()
        )
        : 0;

      final double continent =
        (generator.continentNoise.noise(gridX, gridZ) + tectonicFeatures) *
        generatorAccess.nte$continentFactor(gridX, gridZ);

      if (continent > LAND_THRESHOLD) {
        point.setLand();
        access.nte$setOceanDepth((byte) 0);
      } else if (!altitudeFromMap) {
        if (
          mapTectonics &&
          access.nte$getDivergence() > MapTectonics.OCEAN_RIDGE_DIVERGENCE
        ) {
          access.nte$setOceanDepth((byte) 3);
        } else if (
          mapTectonics &&
          access.nte$getDivergence() < MapTectonics.TRENCH_DIVERGENCE
        ) {
          access.nte$setOceanDepth((byte) 5);
        } else if (continent > CONTINENTAL_SHELF_THRESHOLD) {
          access.nte$setOceanDepth((byte) 2);
        } else {
          access.nte$setOceanDepth((byte) 4);
        }
      }
    }

    ci.cancel();
  }
}
