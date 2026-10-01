package net.yazloysasha.tfcrealworld.mixin.util.tracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.dries007.tfc.config.TFCConfig;
import net.dries007.tfc.mixin.accessor.SectionStorageAccessor;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.climate.ClimateModel;
import net.dries007.tfc.util.tracker.WeatherHelpers;
import net.dries007.tfc.util.tracker.WorldTracker;
import net.dries007.tfc.world.chunkdata.ChunkData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiSection;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Snow and sea ice catch-up of a chunk that has not been ticked for a while
 * (which includes every freshly generated chunk). TFC decides for the whole
 * chunk whether snow accumulates or melts from the temperature at one or two
 * columns, so the snow line follows chunk borders. Here the same weather
 * history (temperatures at a reference column, precipitation, TFC's
 * thresholds and update counts) is applied to each column with its own
 * average temperature, so the snow line follows the climate block by block.
 * Live melting likewise melts only snow that stands where it is warm.
 */
@Mixin(value = WeatherHelpers.class, remap = false)
public abstract class WeatherHelpersMixin {

  @Unique
  private static final int CATCH_UP_STEP_TICKS = 4_000;

  @Unique
  private static final int MAX_CATCH_UP_TICKS = 192_000;

  @Unique
  private static final int CHUNK_COLUMNS = 256;

  @Unique
  private static final float MELT_TEMPERATURE = 2f;

  @Unique
  private static final float FREEZE_TEMPERATURE = -2f;

  // TFC: 1 + 4000 / ticks per snow melt (80 * 3), 1 + 4000 / ticks per accumulation (80)
  @Unique
  private static final int MELT_PER_STEP = 1 + 4_000 / 240;

  @Unique
  private static final int ACCUMULATION_PER_STEP = 1 + 4_000 / 80;

  @Inject(method = "onTickChunk", at = @At("HEAD"), cancellable = true)
  private static void tfcrealworld$catchUpPerColumn(
    ServerLevel level,
    ChunkAccess chunk,
    CallbackInfo ci
  ) {
    final WorldTracker tracker = WorldTracker.get(level);
    final ClimateModel model = tracker.getClimateModel();
    if (!tracker.isWeatherEnabled() || !model.supportsRain()) {
      return;
    }
    final ChunkData data = ChunkData.get(chunk);
    final long currentTick = Calendars.SERVER.getTicks();
    final long lastTick = data.getLastRandomTick();
    final long sinceTick = currentTick - lastTick;
    if (sinceTick <= CATCH_UP_STEP_TICKS) {
      return;
    }

    final ChunkPos chunkPos = chunk.getPos();
    final BlockPos reference =
      WeatherHelpersInvoker.tfcrealworld$getRandomSurfacePos(level, chunkPos);
    final int daysInMonth = Calendars.SERVER.getCalendarDaysInMonth();
    final float rainfall = model.getTimeAverageRainfall(
      level,
      reference,
      lastTick,
      currentTick,
      daysInMonth
    );
    final float referenceAverage = model.getAverageTemperature(
      level,
      reference
    );

    // TFC's catch-up: one sample every CATCH_UP_STEP_TICKS over at most a
    // month, at the reference column.
    final List<float[]> history = new ArrayList<>();
    final long now = Calendars.SERVER.getCalendarTicks();
    long tick = now - Math.min(MAX_CATCH_UP_TICKS, sinceTick);
    while (tick < now) {
      tick += CATCH_UP_STEP_TICKS;
      final boolean precipitating = WeatherHelpers.isPrecipitating(
        model.getRain(tick),
        rainfall
      );
      history.add(
        new float[] {
          model.getInstantTemperature(level, reference, tick, daysInMonth),
          precipitating ? 1f : 0f,
        }
      );
    }

    final int maxUpdates = TFCConfig.SERVER.snowMaxAccumulationOnUpdate.get();
    final int accumulations = Math.min(
      maxUpdates,
      CHUNK_COLUMNS -
        WeatherHelpersInvoker.tfcrealworld$countExistingSnowInChunk(
          level,
          chunkPos
        )
    );
    for (int i = 0; i < accumulations; i++) {
      final BlockPos column = data.getNextSnowPos(chunkPos);
      data.iterateSnowPos(chunk);
      final BlockPos surface = level.getHeightmapPos(
        Heightmap.Types.MOTION_BLOCKING,
        column
      );
      final float offset =
        model.getAverageTemperature(level, surface) - referenceAverage;
      if (tfcrealworld$netSnow(history, offset) > 0) {
        WeatherHelpersInvoker.tfcrealworld$handleSnowAccumulation(
          level,
          surface
        );
      }
    }

    final int melts =
      maxUpdates * (int) Math.max(sinceTick / MAX_CATCH_UP_TICKS, 1);
    tfcrealworld$meltWarmColumns(
      level,
      chunkPos,
      model,
      history,
      referenceAverage,
      melts
    );

    data.setLastRandomTick(chunk, currentTick);
    ci.cancel();
  }

  /**
   * Live melting: TFC melts a random snow block of the chunk whenever one
   * random column is warm, which also thaws the cold side of a chunk on the
   * snow line. Here only snow that itself stands above the melting point
   * melts.
   */
  @Redirect(
    method = "onTickChunk",
    at = @At(
      value = "INVOKE",
      target = "Lnet/dries007/tfc/util/tracker/WeatherHelpers;handleSnowMelting(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/ChunkPos;I)V"
    )
  )
  private static void tfcrealworld$meltWhereWarm(
    ServerLevel level,
    ChunkPos chunkPos,
    int amount
  ) {
    final ClimateModel model = WorldTracker.get(level).getClimateModel();
    final SectionStorageAccessor<PoiSection> poi =
      WeatherHelpersInvoker.tfcrealworld$getPoiManager(level);
    final List<BlockPos> snow = new ArrayList<>();
    for (
      int sectionY = level.getMinSection();
      sectionY < level.getMaxSection();
      sectionY++
    ) {
      final Set<PoiRecord> records =
        WeatherHelpersInvoker.tfcrealworld$getPoiRecords(
          poi,
          chunkPos,
          sectionY
        );
      if (records != null) {
        for (final PoiRecord record : records) {
          snow.add(record.getPos());
        }
      }
    }
    for (int i = 0; i < amount && !snow.isEmpty(); i++) {
      final BlockPos pos = snow.remove(level.random.nextInt(snow.size()));
      if (model.getInstantTemperature(level, pos) > MELT_TEMPERATURE) {
        WeatherHelpersInvoker.tfcrealworld$removeSnowAt(level, pos);
      }
    }
  }

  /**
   * TFC's catch-up count for one column: below freezing while it snows adds
   * snow (less near the threshold), above the melting point removes it.
   */
  @Unique
  private static int tfcrealworld$netSnow(List<float[]> history, float offset) {
    int net = 0;
    for (final float[] step : history) {
      final float temperature = step[0] + offset;
      if (temperature > MELT_TEMPERATURE) {
        net -= MELT_PER_STEP;
      } else if (temperature < FREEZE_TEMPERATURE && step[1] > 0) {
        final float fuzz = Mth.clampedMap(temperature, -2f, -12f, 0.5f, 1f);
        net += (int) (ACCUMULATION_PER_STEP * fuzz);
      }
    }
    return net;
  }

  @Unique
  private static void tfcrealworld$meltWarmColumns(
    ServerLevel level,
    ChunkPos chunkPos,
    ClimateModel model,
    List<float[]> history,
    float referenceAverage,
    int amount
  ) {
    final SectionStorageAccessor<PoiSection> poi =
      WeatherHelpersInvoker.tfcrealworld$getPoiManager(level);
    for (
      int sectionY = level.getMinSection();
      sectionY < level.getMaxSection() && amount > 0;
      sectionY++
    ) {
      final Set<PoiRecord> records =
        WeatherHelpersInvoker.tfcrealworld$getPoiRecords(
          poi,
          chunkPos,
          sectionY
        );
      if (records == null || records.isEmpty()) {
        continue;
      }
      for (final PoiRecord record : new ArrayList<>(records)) {
        final BlockPos pos = record.getPos();
        final float offset =
          model.getAverageTemperature(level, pos) - referenceAverage;
        if (tfcrealworld$netSnow(history, offset) < 0) {
          WeatherHelpersInvoker.tfcrealworld$removeSnowAt(level, pos);
          if (--amount <= 0) {
            return;
          }
        }
      }
    }
  }
}
