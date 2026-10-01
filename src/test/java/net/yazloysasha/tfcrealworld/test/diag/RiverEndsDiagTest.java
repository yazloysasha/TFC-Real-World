package net.yazloysasha.tfcrealworld.test.diag;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.dries007.tfc.world.Seed;
import net.dries007.tfc.world.layer.TFCLayers;
import net.dries007.tfc.world.layer.framework.Area;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.yazloysasha.tfcrealworld.test.TestSetup;
import net.yazloysasha.tfcrealworld.test.drawing.BuiltinWorldPreset;
import org.junit.jupiter.api.Test;

public class RiverEndsDiagTest implements TestSetup {
  @Test
  public void run() throws Exception {
    final List<String> lines = Files.readAllLines(Path.of("/private/tmp/claude-501/-Users-yazloysasha-Desktop-Dev-GitHub-yazloysasha-TFC-Real-World/9f717bca-6ce3-4adf-b600-77835837ef09/scratchpad/river_ends.txt"));
    final Map<String, Integer> failures = new TreeMap<>();
    final long[] seeds = { 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L };
    for (long seed : seeds) {
      final RegionGenerator g = new RegionGenerator(BuiltinWorldPreset.defaultSettings(), Seed.of(seed));
      final Area layer = TFCLayers.createRegionBiomeLayer(g, Seed.of(seed)).get();
      int dry = 0;
      for (String line : lines) {
        final String[] p = line.split(" ");
        if (p[7].equals("dry")) continue;
        final double sx = Double.parseDouble(p[1]), sz = Double.parseDouble(p[2]), ex = Double.parseDouble(p[3]), ez = Double.parseDouble(p[4]);
        boolean touch = false;
        final int steps = (int) Math.ceil(Math.hypot(ex - sx, ez - sz) * 8);
        for (int i = 0; i <= steps && !touch; i++) {
          final double t = (double) i / Math.max(steps, 1);
          final int id = layer.get((int) Math.floor((sx + (ex - sx) * t) * 32), (int) Math.floor((sz + (ez - sz) * t) * 32));
          touch = TFCLayers.isOcean(id) || TFCLayers.isLake(id);
        }
        if (!touch) {
          dry++;
          final int endId = layer.get((int) Math.floor(ex * 32), (int) Math.floor(ez * 32));
          final Region.Point pt = g.getOrCreateRegionPoint((int) Math.floor(ex), (int) Math.floor(ez));
          failures.merge(String.format("%s %s to %.2f,%.2f w=%s mask=%s lonlat=%s,%s biome=%s land=%s hot=%d", p[0], p[7], ex, ez, p[5], p[6], p[8], p[9],
            TFCLayers.getFromLayerId(endId).key().location().getPath(), pt.land(), pt.hotSpotAge), 1, Integer::sum);
        }
      }
      System.out.println("RIVERENDS seed " + seed + " never touch water " + dry);
    }
    failures.forEach((k, v) -> System.out.println("RIVERENDS x" + v + "/" + seeds.length + " " + k));
  }
}
