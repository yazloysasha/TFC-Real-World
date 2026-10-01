package net.yazloysasha.tfcrealworld.test;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import net.dries007.tfc.world.Seed;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.Units;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.test.drawing.BuiltinWorldPreset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Writes every region cell of the default map to build/world_cells.csv for
 * offline analysis (climate and relief of each biome). Run with
 * {@code ./gradlew test -PworldCellDump --tests "*WorldCellDumpTest"}.
 */
public class WorldCellDumpTest implements TestSetup {

  @Test
  @EnabledIfSystemProperty(named = "worldCellDump", matches = "true")
  public void dump() throws IOException {
    final RegionGenerator generator = new RegionGenerator(
      BuiltinWorldPreset.defaultSettings(),
      Seed.of(Long.getLong("worldCellDumpSeed", 20260930L))
    );
    final int radiusX = Units.blockToGrid(
      TFCRealWorldConfig.HORIZONTAL_SCALE.get()
    );
    final int radiusZ = Units.blockToGrid(
      TFCRealWorldConfig.VERTICAL_SCALE.get()
    );
    final Path out = Path.of("world_cells.csv").toAbsolutePath();
    try (
      PrintWriter writer = new PrintWriter(
        Files.newBufferedWriter(out, StandardCharsets.UTF_8)
      )
    ) {
      writer.println(
        "x,z,biome,land,island,lake,mountain,coastal,volcanic,barrier,hotspot,karst,altitude,depth,ocean,edge,divergence,temperature,rainfall"
      );
      for (int x = -radiusX; x < radiusX; x++) {
        for (int z = -radiusZ; z < radiusZ; z++) {
          final Region.Point p = generator.getOrCreateRegionPoint(x, z);
          writer.println(
            String.format(
              Locale.ROOT,
              "%d,%d,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%.0f,%.2f,%.1f",
              x,
              z,
              TfcBiomeIds.name(p.biome),
              p.land() ? 1 : 0,
              p.island() ? 1 : 0,
              p.lake() ? 1 : 0,
              p.mountain() ? 1 : 0,
              p.coastalMountain() ? 1 : 0,
              p.volcanic() ? 1 : 0,
              p.barrierIsland() ? 1 : 0,
              p.hotSpotAge,
              p.isSurfaceRockKarst ? 1 : 0,
              p.discreteBiomeAltitude(),
              p.oceanDepth,
              p.distanceToOcean,
              p.distanceToEdge,
              p.divergence,
              p.temperature,
              p.rainfall
            )
          );
        }
      }
    }
    System.out.println("World cells -> " + out);
  }
}
