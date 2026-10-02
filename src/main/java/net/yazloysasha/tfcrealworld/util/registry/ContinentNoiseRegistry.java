package net.yazloysasha.tfcrealworld.util.registry;

import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.Units;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.world.noise.png.PNGContinentNoise;
import org.jetbrains.annotations.Nullable;

/**
 * Holds per-{@link RegionGenerator} continent noise for biome-layer sampling.
 */
public class ContinentNoiseRegistry
  extends BaseNoiseRegistry<PNGContinentNoise>
{

  private static final ContinentNoiseRegistry INSTANCE =
    new ContinentNoiseRegistry();

  private ContinentNoiseRegistry() {}

  public static void register(
    RegionGenerator generator,
    PNGContinentNoise noise
  ) {
    INSTANCE.registerNoise(generator, noise);
  }

  @Nullable
  public static PNGContinentNoise get(RegionGenerator generator) {
    return INSTANCE.getNoise(generator);
  }

  /**
   * The continent map of the world being generated. Chunk fill does not hold
   * the {@link RegionGenerator}, so it takes the map registered last.
   */
  @Nullable
  public static PNGContinentNoise any() {
    return INSTANCE.latestNoise();
  }

  /**
   * Whether the map of the world being generated has sea at a block. A
   * river of the map may run on under the sea to deep water (down a narrow
   * inlet the game might close); there it is no river.
   */
  public static boolean isSeaAtBlock(int blockX, int blockZ) {
    final PNGContinentNoise continent =
      TFCRealWorldConfig.CONTINENT_FROM_MAP.get() ? any() : null;
    return (
      continent != null &&
      continent.isOceanAtGridHard(
        Units.blockToGridExact(blockX),
        Units.blockToGridExact(blockZ)
      )
    );
  }
}
