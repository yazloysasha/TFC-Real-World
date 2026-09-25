package net.yazloysasha.tfcrealworld.client.geography;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.yazloysasha.tfcrealworld.client.ClientVisitedWaypoints;
import net.yazloysasha.tfcrealworld.util.geography.GeographyNode;
import org.jetbrains.annotations.Nullable;

/**
 * Hit-testing, coord copy feedback, and tooltip lines for map waypoints.
 */
public final class GeographyWaypointUI {

  private static final long COPY_FEEDBACK_MS = 2500L;
  private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern(
    "yyyy-MM-dd HH:mm"
  ).withZone(ZoneId.systemDefault());

  private final Map<String, GeographyNode> continents;
  private final Map<String, GeographyNode> regions;
  private final Map<String, GeographyNode> subregions;

  private @Nullable String copiedWaypointRef;
  private long copiedUntilMs;
  /** Last waypoint whose tooltip was shown (same pick as click-to-copy). */
  private @Nullable PlottedWaypoint hovered;

  public GeographyWaypointUI(
    Map<String, GeographyNode> continents,
    Map<String, GeographyNode> regions,
    Map<String, GeographyNode> subregions
  ) {
    this.continents = continents;
    this.regions = regions;
    this.subregions = subregions;
  }

  /**
   * Topmost drawn marker under the cursor. Hit half-extent matches the drawn
   * orb size ({@link GeographyMapPainter#orbSize}). Draw order = list order;
   * reverse scan so later (top) markers win; among hits, closest wins.
   */
  public @Nullable PlottedWaypoint pick(
    GeographyMapViewport.View view,
    List<PlottedWaypoint> list,
    double mouseX,
    double mouseY,
    int worldMinX,
    int worldMaxX,
    int worldMinZ,
    int worldMaxZ
  ) {
    if (!GeographyMapViewport.contains(view, mouseX, mouseY)) {
      return null;
    }
    @Nullable
    PlottedWaypoint best = null;
    double bestDist = Double.POSITIVE_INFINITY;
    for (int i = list.size() - 1; i >= 0; i--) {
      PlottedWaypoint w = list.get(i);
      int[] screen = GeographyMapViewport.toScreen(
        view,
        w.x(),
        w.z(),
        worldMinX,
        worldMaxX,
        worldMinZ,
        worldMaxZ
      );
      // Match blit: orb occupies [cx - size/2, cx - size/2 + size).
      double half = GeographyMapPainter.orbSize(w.node().capital()) / 2.0;
      double dx = mouseX - screen[0];
      double dy = mouseY - screen[1];
      if (Math.abs(dx) > half || Math.abs(dy) > half) {
        continue;
      }
      double dist = dx * dx + dy * dy;
      if (dist < bestDist) {
        bestDist = dist;
        best = w;
      }
    }
    return best;
  }

  public @Nullable PlottedWaypoint hovered() {
    return hovered;
  }

  /** Update hover used for tooltip and click-to-copy (same pick result). */
  public void setHovered(@Nullable PlottedWaypoint w) {
    hovered = w;
  }

  public void copyCoords(Minecraft minecraft, PlottedWaypoint w) {
    String coords = w.x() + " ~ " + w.z();
    if (minecraft != null) {
      minecraft.keyboardHandler.setClipboard(coords);
      copiedWaypointRef = w.node().ref();
      copiedUntilMs = System.currentTimeMillis() + COPY_FEEDBACK_MS;
    }
  }

  /** Clear copy feedback when hover leaves the copied waypoint. */
  public void syncHover(@Nullable PlottedWaypoint hovered) {
    if (hovered == null) {
      copiedWaypointRef = null;
      return;
    }
    if (
      copiedWaypointRef == null ||
      !copiedWaypointRef.equals(hovered.node().ref())
    ) {
      if (
        copiedWaypointRef != null && System.currentTimeMillis() > copiedUntilMs
      ) {
        copiedWaypointRef = null;
      } else if (copiedWaypointRef != null) {
        copiedWaypointRef = null;
      }
    }
  }

  private boolean showingCopiedFeedback(PlottedWaypoint w) {
    if (
      copiedWaypointRef == null || !copiedWaypointRef.equals(w.node().ref())
    ) {
      return false;
    }
    if (System.currentTimeMillis() > copiedUntilMs) {
      copiedWaypointRef = null;
      return false;
    }
    return true;
  }

  public List<Component> tooltipFor(PlottedWaypoint w) {
    String lang = Minecraft.getInstance().options.languageCode;
    List<Component> lines = new ArrayList<>();
    lines.add(
      Component.literal(w.node().getTitle(lang)).withStyle(
        Style.EMPTY.withColor(ChatFormatting.WHITE)
      )
    );
    if (w.node().hasSubtitle(lang)) {
      lines.add(
        Component.literal(w.node().getSubtitle(lang)).withStyle(
          Style.EMPTY.withColor(ChatFormatting.GRAY)
        )
      );
    }
    boolean visited = ClientVisitedWaypoints.isVisited(w.node().ref());
    Long at = ClientVisitedWaypoints.getVisitedAt(w.node().ref());
    if (visited && at != null) {
      lines.add(
        Component.translatable(
          "tfc_real_world.geography.tooltip.discovered",
          TIME_FMT.format(Instant.ofEpochMilli(at))
        ).withStyle(ChatFormatting.GREEN)
      );
    } else {
      lines.add(
        Component.translatable(
          "tfc_real_world.geography.tooltip.undiscovered"
        ).withStyle(ChatFormatting.DARK_GRAY)
      );
    }
    if (w.continentRef() != null) {
      GeographyNode c = continents.get(w.continentRef());
      lines.add(
        Component.translatable(
          "tfc_real_world.geography.tooltip.continent",
          c != null ? c.getTitle(lang) : w.continentRef()
        ).withStyle(ChatFormatting.DARK_AQUA)
      );
    }
    if (w.regionRef() != null) {
      GeographyNode r = regions.get(w.regionRef());
      lines.add(
        Component.translatable(
          "tfc_real_world.geography.tooltip.region",
          r != null ? r.getTitle(lang) : w.regionRef()
        ).withStyle(ChatFormatting.DARK_AQUA)
      );
    }
    if (w.subregionRef() != null) {
      GeographyNode s = subregions.get(w.subregionRef());
      lines.add(
        Component.translatable(
          "tfc_real_world.geography.tooltip.subregion",
          s != null ? s.getTitle(lang) : w.subregionRef()
        ).withStyle(ChatFormatting.DARK_AQUA)
      );
    }
    lines.add(
      Component.translatable(
        "tfc_real_world.geography.tooltip.xz",
        w.x(),
        w.z()
      ).withStyle(ChatFormatting.YELLOW)
    );
    if (showingCopiedFeedback(w)) {
      lines.add(
        Component.translatable(
          "tfc_real_world.geography.tooltip.copied"
        ).withStyle(ChatFormatting.GREEN)
      );
    } else {
      lines.add(
        Component.translatable(
          "tfc_real_world.geography.tooltip.copy_hint"
        ).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)
      );
    }
    return lines;
  }
}
