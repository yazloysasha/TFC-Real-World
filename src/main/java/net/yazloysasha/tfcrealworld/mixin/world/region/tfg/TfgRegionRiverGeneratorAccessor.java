package net.yazloysasha.tfcrealworld.mixin.world.region.tfg;

import net.dries007.tfc.world.region.Region;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import su.terrafirmagreg.core.world.new_ow_wg.region.TFGAddRiversAndLakes$RegionRiverGenerator;

@Mixin(value = TFGAddRiversAndLakes$RegionRiverGenerator.class, remap = false)
public interface TfgRegionRiverGeneratorAccessor {
  @Accessor("region")
  Region tfcrealworld$getRegion();
}
