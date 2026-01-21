package net.yazloysasha.tfcrealworld.world.climate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.dries007.tfc.util.climate.KoppenClimateClassification;
import net.yazloysasha.tfcrealworld.types.ClimateCategory;
import net.yazloysasha.tfcrealworld.world.noise.koppen.ClimateConstants;
import org.junit.jupiter.api.Test;

public class KoppenClimateGroupReportTest {

  @Test
  public void printKoppenGroupPercentages() {
    final Map<
      RealKoppenClimateClassification,
      Map<KoppenClimateClassification, Long>
    > counts = new EnumMap<>(RealKoppenClimateClassification.class);
    final Map<RealKoppenClimateClassification, Long> totals = new EnumMap<>(
      RealKoppenClimateClassification.class
    );

    for (RealKoppenClimateClassification climate : RealKoppenClimateClassification.values()) {
      counts.put(climate, new EnumMap<>(KoppenClimateClassification.class));
      totals.put(climate, 0L);
    }

    for (
      float temp = ClimateConstants.TEMP_MIN;
      temp <= ClimateConstants.TEMP_MAX + 1e-6f;
      temp += ClimateConstants.TEMP_STEP
    ) {
      for (
        float rain = ClimateConstants.RAIN_MIN;
        rain <= ClimateConstants.RAIN_MAX + 1e-6f;
        rain += ClimateConstants.RAIN_STEP
      ) {
        accumulate(counts, totals, temp, rain);
        accumulate(counts, totals, temp, rain);
      }
    }

    final Map<ClimateCategory, List<ClimateReport>> reportsByCategory =
      new LinkedHashMap<>();
    for (ClimateCategory category : ClimateCategory.values()) {
      reportsByCategory.put(category, new ArrayList<>());
    }

    for (RealKoppenClimateClassification climate : RealKoppenClimateClassification.values()) {
      final long total = totals.get(climate);
      assertTrue(total > 0, "No samples for climate " + climate);

      final List<GroupPct> breakdown = new ArrayList<>();
      for (Map.Entry<KoppenClimateClassification, Long> e : counts
        .get(climate)
        .entrySet()) {
        final long c = e.getValue();
        if (c <= 0) continue;
        final double pct = (100.0 * (double) c) / (double) total;
        breakdown.add(new GroupPct(e.getKey(), pct, c));
      }
      breakdown.sort(
        Comparator.<GroupPct>comparingDouble(g -> g.pct)
          .reversed()
          .thenComparing(g -> g.group.name())
      );

      double pctSum = 0.0;
      final StringBuilder sb = new StringBuilder();
      sb.append(climate.name()).append(" (");
      final var tempChar = climate.getTemperatureCharacteristic();
      if (tempChar != null) {
        sb.append(tempChar.name()).append(" | ");
      }
      sb.append(climate.getDisplayName()).append(") -> ");
      for (int i = 0; i < breakdown.size(); i++) {
        final GroupPct g = breakdown.get(i);
        pctSum += g.pct;
        if (i > 0) sb.append(", ");
        sb.append(g.group.name()).append(String.format(" %.2f%%", g.pct));
      }

      assertEquals(100.0, pctSum, 1e-6, "Percent sum mismatch for " + climate);

      reportsByCategory
        .get(climate.getCategory())
        .add(new ClimateReport(climate, sb.toString()));
    }

    for (Map.Entry<
      ClimateCategory,
      List<ClimateReport>
    > entry : reportsByCategory.entrySet()) {
      if (entry.getValue().isEmpty()) continue;
      System.out.println();
      System.out.println("=== " + entry.getKey().name() + " ===");
      for (ClimateReport report : entry.getValue()) {
        System.out.println(report.output);
      }
    }
  }

  private static void accumulate(
    Map<
      RealKoppenClimateClassification,
      Map<KoppenClimateClassification, Long>
    > counts,
    Map<RealKoppenClimateClassification, Long> totals,
    float temp,
    float rain
  ) {
    final RealKoppenClimateClassification climate =
      RealKoppenClimateClassification.classify(temp, rain);
    final KoppenClimateClassification group =
      KoppenClimateClassification.classify(temp, rain);

    totals.put(climate, totals.get(climate) + 1L);
    final Map<KoppenClimateClassification, Long> map = counts.get(climate);
    map.put(group, map.getOrDefault(group, 0L) + 1L);
  }

  private record GroupPct(
    KoppenClimateClassification group,
    double pct,
    long count
  ) {}

  private record ClimateReport(
    RealKoppenClimateClassification climate,
    String output
  ) {}
}
