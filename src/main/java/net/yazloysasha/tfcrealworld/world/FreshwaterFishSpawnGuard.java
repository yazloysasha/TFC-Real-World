package net.yazloysasha.tfcrealworld.world;

import net.dries007.tfc.common.entities.aquatic.FreshwaterFish;
import net.dries007.tfc.common.fluids.TFCFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;

/** Keeps freshwater fish out of salt water at spawn time */
public final class FreshwaterFishSpawnGuard {

  private FreshwaterFishSpawnGuard() {}

  public static void onPositionCheck(MobSpawnEvent.PositionCheck event) {
    if (!(event.getEntity() instanceof FreshwaterFish)) {
      return;
    }
    final FluidState fluid = event
      .getLevel()
      .getFluidState(
        BlockPos.containing(event.getX(), event.getY(), event.getZ())
      );
    if (
      fluid.getType() == TFCFluids.SALT_WATER.getSource() ||
      fluid.getType() == TFCFluids.SALT_WATER.getFlowing()
    ) {
      event.setResult(Event.Result.DENY);
    }
  }
}
