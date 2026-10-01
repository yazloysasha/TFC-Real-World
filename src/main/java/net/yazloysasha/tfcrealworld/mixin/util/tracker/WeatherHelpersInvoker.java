package net.yazloysasha.tfcrealworld.mixin.util.tracker;

import java.util.Set;
import net.dries007.tfc.mixin.accessor.SectionStorageAccessor;
import net.dries007.tfc.util.tracker.WeatherHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiSection;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = WeatherHelpers.class, remap = false)
public interface WeatherHelpersInvoker {
  @Invoker("handleSnowAccumulation")
  static void tfcrealworld$handleSnowAccumulation(
    ServerLevel level,
    BlockPos surfacePos
  ) {
    throw new AssertionError();
  }

  @Invoker("removeSnowAt")
  static void tfcrealworld$removeSnowAt(ServerLevel level, BlockPos pos) {
    throw new AssertionError();
  }

  @Invoker("countExistingSnowInChunk")
  static int tfcrealworld$countExistingSnowInChunk(
    ServerLevel level,
    ChunkPos chunkPos
  ) {
    throw new AssertionError();
  }

  @Invoker("getRandomSurfacePos")
  static BlockPos tfcrealworld$getRandomSurfacePos(
    ServerLevel level,
    ChunkPos chunkPos
  ) {
    throw new AssertionError();
  }

  @Invoker("getPoiManager")
  static SectionStorageAccessor<PoiSection> tfcrealworld$getPoiManager(
    ServerLevel level
  ) {
    throw new AssertionError();
  }

  @Invoker("getPoiRecords")
  static Set<PoiRecord> tfcrealworld$getPoiRecords(
    SectionStorageAccessor<PoiSection> poi,
    ChunkPos chunkPos,
    int sectionY
  ) {
    throw new AssertionError();
  }
}
