package su.terrafirmagreg.core.utils;

import java.util.List;
import java.util.TreeMap;
import net.dries007.tfc.world.ChunkGeneratorExtension;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Compile-only stub. Runtime class is Core-Modern's CustomSpawnHelper.
 */
public class CustomSpawnHelper {

  public static final String VIEWER_SPAWN_ID = "tfcgenviewer";

  public static final TreeMap<
    String,
    CustomSpawnCondition
  > CUSTOM_SPAWN_CONDITIONS = new TreeMap<>();

  public static final List<
    CustomSpawnCondition
  > CREATE_WORLD_SPAWN_CYCLE_VALUES = List.of();

  public static final CustomSpawnCondition DEFAULT_SPAWN =
    new CustomSpawnCondition(
      "default",
      0,
      0,
      1,
      new float[] { -20f, 20f },
      new float[] { 0f, 400f },
      Level.OVERWORLD,
      Component.empty()
    );

  public static CustomSpawnCondition getFromConfig() {
    return DEFAULT_SPAWN;
  }

  public static void resetConfigValue() {}

  public static boolean testWithinRanges(
    float temperature,
    float rainfall,
    CustomSpawnCondition condition
  ) {
    return false;
  }

  public static BlockPos findSpawnBiome(
    int spawnCenterX,
    int spawnCenterZ,
    int spawnRadius,
    RandomSource random,
    ChunkGeneratorExtension extension
  ) {
    return BlockPos.ZERO;
  }

  public static Component createWorldSpawnCycleLabel(CustomSpawnCondition s) {
    return Component.empty();
  }

  public static Component createWorldSpawnTooltipText(
    CustomSpawnCondition condition
  ) {
    return Component.empty();
  }

  @OnlyIn(Dist.CLIENT)
  public static final class CreateWorldSpawnCycle {

    private CreateWorldSpawnCycle() {}

    public static void register(CycleButton<CustomSpawnCondition> button) {}

    public static Tooltip createTooltip(CustomSpawnCondition condition) {
      return Tooltip.create(Component.empty());
    }
  }

  public record CustomSpawnCondition(
    String id,
    int spawnCenterX,
    int spawnCenterZ,
    int spawnRadiusMultiplier,
    float[] temperatureRange,
    float[] rainfallRange,
    ResourceKey<Level> dimension,
    MutableComponent difficulty
  ) {}
}
