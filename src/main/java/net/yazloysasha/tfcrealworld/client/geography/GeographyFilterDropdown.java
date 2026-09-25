package net.yazloysasha.tfcrealworld.client.geography;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.yazloysasha.tfcrealworld.client.ClientVisitedWaypoints;
import net.yazloysasha.tfcrealworld.client.GeographySessionState;
import net.yazloysasha.tfcrealworld.client.GeographyUI;
import net.yazloysasha.tfcrealworld.util.geography.GeographyManager;
import net.yazloysasha.tfcrealworld.util.geography.GeographyNode;
import org.jetbrains.annotations.Nullable;

/**
 * Continent / region / subregion filter chips and their dropdown menus.
 */
public final class GeographyFilterDropdown {

  public record Row(@Nullable String ref, String left, String right) {}

  private int openDropdown = -1;
  private int dropdownScroll = 0;
  private double filterScrollAccum = 0.0;

  private final Map<String, GeographyNode> continents;
  private final Map<String, GeographyNode> regions;
  private final Map<String, GeographyNode> subregions;
  private final List<PlottedWaypoint> allWaypoints;

  public GeographyFilterDropdown(
    Map<String, GeographyNode> continents,
    Map<String, GeographyNode> regions,
    Map<String, GeographyNode> subregions,
    List<PlottedWaypoint> allWaypoints
  ) {
    this.continents = continents;
    this.regions = regions;
    this.subregions = subregions;
    this.allWaypoints = allWaypoints;
  }

  public int openIndex() {
    return openDropdown;
  }

  public boolean isOpen() {
    return openDropdown >= 0;
  }

  public void close() {
    openDropdown = -1;
    dropdownScroll = 0;
    filterScrollAccum = 0;
  }

  public void renderChips(
    GuiGraphics graphics,
    Font font,
    int leftPos,
    int topPos
  ) {
    drawChip(
      graphics,
      font,
      leftPos + GeographyUI.CHIP_X0,
      topPos + GeographyUI.CHIP_Y,
      filterLabel(GeographySessionState.filterContinent, continents),
      GeographyUI.CHIP_W
    );
    drawChip(
      graphics,
      font,
      leftPos + GeographyUI.CHIP_X1,
      topPos + GeographyUI.CHIP_Y,
      filterLabel(GeographySessionState.filterRegion, regions),
      GeographyUI.CHIP_W
    );
    drawChip(
      graphics,
      font,
      leftPos + GeographyUI.CHIP_X2,
      topPos + GeographyUI.CHIP_Y,
      filterLabel(GeographySessionState.filterSubregion, subregions),
      GeographyUI.CHIP_W
    );
  }

  public void renderOpen(
    GuiGraphics graphics,
    Font font,
    int leftPos,
    int topPos,
    int mouseX,
    int mouseY
  ) {
    if (openDropdown < 0) {
      return;
    }
    List<Row> rows = dropdownRows(openDropdown);
    int boxX = dropdownBoxX(leftPos);
    int boxY = topPos + GeographyUI.DROPDOWN_TOP;
    int boxW = GeographyUI.DROPDOWN_W;
    int visible = Math.min(GeographyUI.DROPDOWN_VISIBLE, rows.size());
    int boxH = visible * GeographyUI.DROPDOWN_ROW_H;
    int maxScroll = Math.max(0, rows.size() - visible);
    dropdownScroll = Mth.clamp(dropdownScroll, 0, maxScroll);
    boolean showScroll = maxScroll > 0;
    int textRightPad = showScroll
      ? (GeographyUI.SCROLLBAR_PAD_LEFT +
        GeographyUI.SCROLLBAR_W +
        GeographyUI.SCROLLBAR_PAD_RIGHT)
      : 3;

    graphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xF0080C10);
    graphics.renderOutline(boxX, boxY, boxW, boxH, 0xFF8A9AAC);
    graphics.enableScissor(boxX + 1, boxY, boxX + boxW - 1, boxY + boxH);

    @Nullable
    String selectedRef =
      switch (openDropdown) {
        case 0 -> GeographySessionState.filterContinent;
        case 1 -> GeographySessionState.filterRegion;
        default -> GeographySessionState.filterSubregion;
      };

    int y = boxY;
    for (
      int i = dropdownScroll;
      i < dropdownScroll + visible && i < rows.size();
      i++
    ) {
      Row row = rows.get(i);
      boolean selected =
        (row.ref() == null && selectedRef == null) ||
        (row.ref() != null && row.ref().equals(selectedRef));
      boolean hovered =
        mouseX >= boxX &&
        mouseX < boxX + boxW &&
        mouseY >= y &&
        mouseY < y + GeographyUI.DROPDOWN_ROW_H;
      if (selected) {
        graphics.fill(
          boxX + 1,
          y,
          boxX + boxW - 1,
          y + GeographyUI.DROPDOWN_ROW_H,
          0xC040A050
        );
      } else if (hovered) {
        graphics.fill(
          boxX + 1,
          y,
          boxX + boxW - 1,
          y + GeographyUI.DROPDOWN_ROW_H,
          0x80405060
        );
      }
      int color = selected ? 0xFFFFFFAA : 0xFFE8F0FF;
      int textMax =
        boxW -
        36 -
        (showScroll
            ? GeographyUI.SCROLLBAR_W +
            GeographyUI.SCROLLBAR_PAD_LEFT +
            GeographyUI.SCROLLBAR_PAD_RIGHT
            : 0);
      String left = ellipsize(
        font,
        Component.literal(row.left()),
        textMax
      ).getString();
      graphics.drawString(font, left, boxX + 3, y + 2, color, false);
      graphics.drawString(
        font,
        row.right(),
        boxX + boxW - textRightPad - font.width(row.right()),
        y + 2,
        color,
        false
      );
      y += GeographyUI.DROPDOWN_ROW_H;
    }
    graphics.disableScissor();

    if (showScroll) {
      int trackX =
        boxX + boxW - GeographyUI.SCROLLBAR_PAD_RIGHT - GeographyUI.SCROLLBAR_W;
      int trackY = boxY + GeographyUI.SCROLLBAR_PAD_RIGHT;
      int trackH = boxH - 2 * GeographyUI.SCROLLBAR_PAD_RIGHT;
      graphics.fill(
        trackX,
        trackY,
        trackX + GeographyUI.SCROLLBAR_W,
        trackY + trackH,
        0x80405060
      );
      float thumbRatio = visible / (float) rows.size();
      int thumbH = Math.max(8, Math.round(trackH * thumbRatio));
      int thumbTravel = Math.max(0, trackH - thumbH);
      int thumbY =
        trackY +
        (maxScroll == 0
            ? 0
            : Math.round(thumbTravel * (dropdownScroll / (float) maxScroll)));
      graphics.fill(
        trackX,
        thumbY,
        trackX + GeographyUI.SCROLLBAR_W,
        thumbY + thumbH,
        0xFFA0B0C0
      );
    }
  }

  public boolean isOverDropdown(
    int leftPos,
    int topPos,
    double mouseX,
    double mouseY
  ) {
    if (openDropdown < 0) {
      return false;
    }
    List<Row> rows = dropdownRows(openDropdown);
    int visible = Math.min(GeographyUI.DROPDOWN_VISIBLE, rows.size());
    int boxX = dropdownBoxX(leftPos);
    int boxY = topPos + GeographyUI.DROPDOWN_TOP;
    int boxW = GeographyUI.DROPDOWN_W;
    int boxH = visible * GeographyUI.DROPDOWN_ROW_H;
    return (
      mouseX >= boxX &&
      mouseX < boxX + boxW &&
      mouseY >= boxY &&
      mouseY < boxY + boxH
    );
  }

  /** @return true if the click was consumed by chips/dropdown. */
  public boolean mouseClicked(
    int leftPos,
    int topPos,
    double mouseX,
    double mouseY
  ) {
    if (openDropdown >= 0) {
      List<Row> rows = dropdownRows(openDropdown);
      int boxX = dropdownBoxX(leftPos);
      int boxY = topPos + GeographyUI.DROPDOWN_TOP;
      int boxW = GeographyUI.DROPDOWN_W;
      int visible = Math.min(GeographyUI.DROPDOWN_VISIBLE, rows.size());
      int y = boxY;
      for (
        int i = dropdownScroll;
        i < dropdownScroll + visible && i < rows.size();
        i++
      ) {
        if (
          mouseX >= boxX &&
          mouseX < boxX + boxW &&
          mouseY >= y &&
          mouseY < y + GeographyUI.DROPDOWN_ROW_H
        ) {
          applyDropdown(openDropdown, rows.get(i).ref());
          close();
          return true;
        }
        y += GeographyUI.DROPDOWN_ROW_H;
      }
      close();
      return true;
    }
    if (clickChip(leftPos, topPos, mouseX, mouseY, GeographyUI.CHIP_X0)) {
      openDropdown = 0;
      dropdownScroll = 0;
      filterScrollAccum = 0;
      return true;
    }
    if (clickChip(leftPos, topPos, mouseX, mouseY, GeographyUI.CHIP_X1)) {
      openDropdown = 1;
      dropdownScroll = 0;
      filterScrollAccum = 0;
      return true;
    }
    if (clickChip(leftPos, topPos, mouseX, mouseY, GeographyUI.CHIP_X2)) {
      openDropdown = 2;
      dropdownScroll = 0;
      filterScrollAccum = 0;
      return true;
    }
    return false;
  }

  /** @return true if scroll was consumed. */
  public boolean mouseScrolled(double delta) {
    if (openDropdown < 0) {
      return false;
    }
    List<Row> rows = dropdownRows(openDropdown);
    int visible = Math.min(GeographyUI.DROPDOWN_VISIBLE, rows.size());
    int maxScroll = Math.max(0, rows.size() - visible);
    filterScrollAccum += -delta;
    while (filterScrollAccum >= 1.0) {
      dropdownScroll = Mth.clamp(dropdownScroll + 1, 0, maxScroll);
      filterScrollAccum -= 1.0;
    }
    while (filterScrollAccum <= -1.0) {
      dropdownScroll = Mth.clamp(dropdownScroll - 1, 0, maxScroll);
      filterScrollAccum += 1.0;
    }
    return true;
  }

  private boolean clickChip(
    int leftPos,
    int topPos,
    double mouseX,
    double mouseY,
    int chipX
  ) {
    return (
      mouseX >= leftPos + chipX &&
      mouseX < leftPos + chipX + GeographyUI.CHIP_W &&
      mouseY >= topPos + GeographyUI.CHIP_Y &&
      mouseY < topPos + GeographyUI.CHIP_Y + GeographyUI.CHIP_H
    );
  }

  private int dropdownChipX() {
    return openDropdown == 0
      ? GeographyUI.CHIP_X0
      : openDropdown == 1 ? GeographyUI.CHIP_X1 : GeographyUI.CHIP_X2;
  }

  private int dropdownBoxX(int leftPos) {
    int mapLeft = leftPos + GeographyUI.MAP_X;
    int mapRight = mapLeft + GeographyUI.MAP_W;
    int chipCenter = leftPos + dropdownChipX() + GeographyUI.CHIP_W / 2;
    int boxW = GeographyUI.DROPDOWN_W;
    int boxX = chipCenter - boxW / 2;
    return Mth.clamp(boxX, mapLeft, mapRight - boxW);
  }

  private void drawChip(
    GuiGraphics graphics,
    Font font,
    int x,
    int y,
    Component label,
    int w
  ) {
    graphics.fill(x, y, x + w, y + GeographyUI.CHIP_H, 0xE0283240);
    graphics.renderOutline(x, y, w, GeographyUI.CHIP_H, 0xFF5A6A7C);
    graphics.drawString(
      font,
      ellipsize(font, label, w - 6),
      x + 3,
      y + 2,
      0xFFE8F0FF,
      false
    );
  }

  private static Component ellipsize(Font font, Component c, int maxWidth) {
    String s = c.getString();
    if (font.width(s) <= maxWidth) {
      return c;
    }
    while (s.length() > 1 && font.width(s + "…") > maxWidth) {
      s = s.substring(0, s.length() - 1);
    }
    return Component.literal(s + "…");
  }

  private Component filterLabel(
    @Nullable String selected,
    Map<String, GeographyNode> pool
  ) {
    if (selected == null) {
      return Component.translatable("tfc_real_world.geography.filter.all");
    }
    GeographyNode node = pool.get(selected);
    String lang = Minecraft.getInstance().options.languageCode;
    return Component.literal(node != null ? node.getTitle(lang) : selected);
  }

  private List<Row> dropdownRows(int which) {
    String lang = Minecraft.getInstance().options.languageCode;
    List<Row> rows = new ArrayList<>();
    rows.add(
      new Row(
        null,
        Component.translatable(
          "tfc_real_world.geography.filter.all"
        ).getString(),
        countsFor(which, null)
      )
    );
    Map<String, GeographyNode> pool =
      switch (which) {
        case 0 -> continents;
        case 1 -> regions;
        default -> subregions;
      };
    List<GeographyNode> sorted = pool
      .values()
      .stream()
      .sorted(
        Comparator.comparing(n -> n.getTitle(lang).toLowerCase(Locale.ROOT))
      )
      .toList();
    for (GeographyNode node : sorted) {
      if (which == 1 && GeographySessionState.filterContinent != null) {
        if (
          node.parentRef() == null ||
          !node
            .parentRef()
            .equalsIgnoreCase(GeographySessionState.filterContinent)
        ) {
          continue;
        }
      }
      if (which == 2) {
        if (GeographySessionState.filterRegion != null) {
          if (
            node.parentRef() == null ||
            !node
              .parentRef()
              .equalsIgnoreCase(GeographySessionState.filterRegion)
          ) {
            continue;
          }
        } else if (GeographySessionState.filterContinent != null) {
          GeographyNode region = node.parentRef() != null
            ? GeographyManager.get(node.parentRef())
            : null;
          if (
            region == null ||
            region.parentRef() == null ||
            !region
              .parentRef()
              .equalsIgnoreCase(GeographySessionState.filterContinent)
          ) {
            continue;
          }
        }
      }
      rows.add(
        new Row(node.ref(), node.getTitle(lang), countsFor(which, node.ref()))
      );
    }
    return rows;
  }

  private String countsFor(int which, @Nullable String ref) {
    int n = 0;
    int m = 0;
    for (PlottedWaypoint w : allWaypoints) {
      boolean match =
        switch (which) {
          case 0 -> ref == null ||
          (w.continentRef() != null && w.continentRef().equals(ref));
          case 1 -> ref == null ||
          (w.regionRef() != null && w.regionRef().equals(ref));
          default -> ref == null ||
          (w.subregionRef() != null && w.subregionRef().equals(ref));
        };
      if (!match) {
        continue;
      }
      if (
        which >= 1 &&
        GeographySessionState.filterContinent != null &&
        (w.continentRef() == null ||
          !w.continentRef().equals(GeographySessionState.filterContinent))
      ) {
        continue;
      }
      if (
        which >= 2 &&
        GeographySessionState.filterRegion != null &&
        (w.regionRef() == null ||
          !w.regionRef().equals(GeographySessionState.filterRegion))
      ) {
        continue;
      }
      m++;
      if (ClientVisitedWaypoints.isVisited(w.node().ref())) {
        n++;
      }
    }
    return n + "/" + m;
  }

  private static void applyDropdown(int which, @Nullable String ref) {
    switch (which) {
      case 0 -> {
        GeographySessionState.filterContinent = ref;
        GeographySessionState.filterRegion = null;
        GeographySessionState.filterSubregion = null;
      }
      case 1 -> {
        GeographySessionState.filterRegion = ref;
        GeographySessionState.filterSubregion = null;
      }
      default -> GeographySessionState.filterSubregion = ref;
    }
  }
}
