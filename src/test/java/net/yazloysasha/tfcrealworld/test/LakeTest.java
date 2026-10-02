package net.yazloysasha.tfcrealworld.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.dries007.tfc.world.BiomeNoiseSampler;
import net.dries007.tfc.world.ChunkBiomeSampler;
import net.dries007.tfc.world.ChunkHeightFiller;
import net.dries007.tfc.world.Seed;
import net.dries007.tfc.world.TFCChunkGenerator;
import net.dries007.tfc.world.biome.BiomeBlendType;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.biome.BiomeNoise;
import net.dries007.tfc.world.biome.BiomeSourceExtension;
import net.dries007.tfc.world.biome.TFCBiomes;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.layer.framework.Area;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.RegionPartition;
import net.dries007.tfc.world.region.Units;
import net.dries007.tfc.world.river.RiverBlendType;
import net.dries007.tfc.world.river.RiverNoiseSampler;
import net.dries007.tfc.world.shore.ShoreBlendType;
import net.dries007.tfc.world.shore.ShoreNoiseSampler;
import net.dries007.tfc.world.volcano.CenteredFeatureBlendType;
import net.dries007.tfc.world.volcano.CenteredFeatureNoiseSampler;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.yazloysasha.tfcrealworld.test.drawing.BuiltinWorldPreset;
import net.yazloysasha.tfcrealworld.util.geography.WaypointCoordinates;
import net.yazloysasha.tfcrealworld.world.surface.FrozenWaterSurfaceBuilder;
import org.junit.jupiter.api.Test;

/** Lakes of the map and the land between them. */
public class LakeTest implements TestSetup {

  private static final long SEED = 20261002L;

  /**
   * Finland is a land of small lakes: whole region cells are lakes, and the
   * land the map has inside them must be the land of the place, never a
   * biome picked from a list (a rift valley, once).
   */
  @Test
  public void landBetweenLakesIsTheLandOfThePlace() {
    final RegionGenerator generator = new RegionGenerator(
      BuiltinWorldPreset.defaultSettings(),
      Seed.of(SEED)
    );
    final Area layer = TFCLayers.createRegionBiomeLayer(
      generator,
      Seed.of(SEED)
    ).get();
    final int[] south = WaypointCoordinates.toBlockXZ(60.0, 21.0);
    final int[] north = WaypointCoordinates.toBlockXZ(69.0, 31.0);
    int rifts = 0;
    for (
      int x = Math.min(south[0], north[0]);
      x < Math.max(south[0], north[0]);
      x += 16
    ) {
      for (
        int z = Math.min(south[1], north[1]);
        z < Math.max(south[1], north[1]);
        z += 16
      ) {
        if (layer.get(x >> 2, z >> 2) == TFCLayers.RIFT_VALLEY) {
          rifts++;
        }
      }
    }
    assertEquals(0, rifts, "rift valley samples in Finland");
  }

  @Test
  public void waterFreezesLikeTheSea() {
    final Seed seed = Seed.of(SEED);
    assertTrue(
      TFCBiomes.LAKE.createSurfaceBuilder(seed) instanceof
        FrozenWaterSurfaceBuilder
    );
    assertTrue(
      TFCBiomes.MOUNTAIN_LAKE.createSurfaceBuilder(seed) instanceof
        FrozenWaterSurfaceBuilder
    );
    assertFalse(
      TFCBiomes.MELTWATER_LAKE.createSurfaceBuilder(seed) instanceof
        FrozenWaterSurfaceBuilder
    );
    assertTrue(
      TFCBiomes.SALT_MARSH.createSurfaceBuilder(seed) instanceof
        FrozenWaterSurfaceBuilder
    );
    assertTrue(
      TFCBiomes.RIVER.createSurfaceBuilder(seed) instanceof
        FrozenWaterSurfaceBuilder
    );
  }

  /**
   * At the ice sheet edge of Greenland one map lake is a subglacial lake
   * under the ice and a meltwater lake beside it. The subglacial lake has
   * the height of the ice and must not lift the meltwater out of the water.
   */
  @Test
  public void subglacialLakeDoesNotLiftMeltwater() {
    final Seed seed = Seed.of(SEED);
    final RegionGenerator generator = new RegionGenerator(
      BuiltinWorldPreset.defaultSettings(),
      seed
    );
    final Area layer = TFCLayers.createRegionBiomeLayer(generator, seed).get();
    final Heights heights = new Heights(generator, layer, seed);
    for (int x = -9431 - 320; x < -9431 + 320; x += 4) {
      for (int z = -17846 - 320; z < -17846 + 320; z += 4) {
        heights.at(x, z);
      }
    }
    assertTrue(heights.meltwater > 10, "meltwater: " + heights.meltwater);
    assertEquals(0, heights.dryMeltwater, "meltwater columns above water");
  }

  /** Ground heights as the chunk generator has them, without rivers. */
  private static final class Heights {

    private final RegionGenerator generator;
    private final Area layer;
    private final Seed seed;
    private final Map<ChunkPos, ChunkHeightFiller> fillers = new HashMap<>();

    /** Columns all but lake and a third meltwater; those above water. */
    int meltwater;
    int dryMeltwater;

    private final BiomeSourceExtension biomes = new BiomeSourceExtension() {
      @Override
      public BiomeExtension getBiomeExtensionNoRiver(int quartX, int quartZ) {
        return TFCLayers.getFromLayerId(layer.get(quartX, quartZ));
      }

      @Override
      public Holder<Biome> getBiomeFromExtension(BiomeExtension extension) {
        throw new UnsupportedOperationException();
      }

      @Override
      public RegionPartition.Point getPartition(int blockX, int blockZ) {
        return generator.getOrCreatePartitionPoint(
          Units.blockToGrid(blockX),
          Units.blockToGrid(blockZ)
        );
      }
    };

    Heights(RegionGenerator generator, Area layer, Seed seed) {
      this.generator = generator;
      this.layer = layer;
      this.seed = seed;
    }

    double at(int blockX, int blockZ) {
      return fillers
        .computeIfAbsent(new ChunkPos(blockX >> 4, blockZ >> 4), this::filler)
        .sampleHeight(blockX, blockZ);
    }

    private ChunkHeightFiller filler(ChunkPos pos) {
      final Object2DoubleMap<BiomeExtension>[] weights =
        ChunkBiomeSampler.sampleBiomes(
          pos,
          (x, z) -> biomes.getBiomeExtensionNoRiver(x >> 2, z >> 2),
          BiomeExtension::biomeBlendType
        );
      final ImmutableMap.Builder<BiomeExtension, BiomeNoiseSampler> samplers =
        ImmutableMap.builder();
      for (final BiomeExtension biome : TFCBiomes.REGISTRY) {
        final BiomeNoiseSampler sampler = biome.createNoiseSampler(
          seed.forkStable()
        );
        if (sampler != null) {
          samplers.put(biome, sampler);
        }
      }
      final Map<RiverBlendType, RiverNoiseSampler> rivers = new EnumMap<>(
        RiverBlendType.class
      );
      for (final RiverBlendType type : RiverBlendType.ALL) {
        rivers.put(type, type.createNoiseSampler(seed.forkStable()));
      }
      final Map<ShoreBlendType, ShoreNoiseSampler> shores = new EnumMap<>(
        ShoreBlendType.class
      );
      for (final ShoreBlendType type : ShoreBlendType.ALL) {
        shores.put(type, type.createNoiseSampler(seed.forkStable()));
      }
      final Map<CenteredFeatureBlendType, CenteredFeatureNoiseSampler> centred =
        new EnumMap<>(CenteredFeatureBlendType.class);
      for (final CenteredFeatureBlendType type : CenteredFeatureBlendType.ALL) {
        centred.put(type, type.createNoiseSampler(seed.forkStable()));
      }
      return new ChunkHeightFiller(
        weights,
        biomes,
        samplers.build(),
        rivers,
        shores,
        centred,
        TFCChunkGenerator.SEA_LEVEL_Y,
        BiomeNoise.shoreTideLevelNoise(seed)
      ) {
        @Override
        public double sampleHeight(int blockX, int blockZ) {
          setupColumn(blockX, blockZ);
          prepareColumnBiomeWeights();
          final double height = sampleColumnHeightAndBiome(
            biomeWeights1,
            false
          );
          double lakes = 0;
          for (final Object2DoubleMap.Entry<
            BiomeExtension
          > entry : biomeWeights1.object2DoubleEntrySet()) {
            if (entry.getKey().biomeBlendType() == BiomeBlendType.LAKE) {
              lakes += entry.getDoubleValue();
            }
          }
          if (
            lakes > 0.9 &&
            biomeWeights1.getDouble(TFCBiomes.MELTWATER_LAKE) > 0.3
          ) {
            meltwater++;
            if (height >= TFCChunkGenerator.SEA_LEVEL_Y) {
              dryMeltwater++;
            }
          }
          return height;
        }
      };
    }
  }
}
