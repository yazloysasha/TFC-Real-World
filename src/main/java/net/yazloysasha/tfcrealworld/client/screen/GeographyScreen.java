package net.yazloysasha.tfcrealworld.client.screen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.dries007.tfc.client.screen.button.PlayerInventoryTabButton;
import net.dries007.tfc.compat.patchouli.PatchouliIntegration;
import net.dries007.tfc.network.SwitchInventoryTabPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.client.GeographySessionState;
import net.yazloysasha.tfcrealworld.client.GeographyUI;
import net.yazloysasha.tfcrealworld.client.geography.GeographyFilterDropdown;
import net.yazloysasha.tfcrealworld.client.geography.GeographyMapPainter;
import net.yazloysasha.tfcrealworld.client.geography.GeographyMapViewport;
import net.yazloysasha.tfcrealworld.client.geography.GeographyWaypointUI;
import net.yazloysasha.tfcrealworld.client.geography.PlottedWaypoint;
import net.yazloysasha.tfcrealworld.client.widget.GeographyInventoryTabButton;
import net.yazloysasha.tfcrealworld.config.TFCRealWorldConfig;
import net.yazloysasha.tfcrealworld.util.geography.GeographyKind;
import net.yazloysasha.tfcrealworld.util.geography.GeographyManager;
import net.yazloysasha.tfcrealworld.util.geography.GeographyNode;
import net.yazloysasha.tfcrealworld.util.geography.WaypointCoordinates;
import net.yazloysasha.tfcrealworld.util.profile.MapProfile;
import net.yazloysasha.tfcrealworld.util.profile.ProfileManager;
import org.jetbrains.annotations.Nullable;

/**
 * Standalone geography page matching book size (272×180) and detached tab placement.
 * Orchestrates layout and input; map/filters/tooltips live in focused helpers.
 */
public class GeographyScreen extends Screen {

  public static final int PANEL_WIDTH = GeographyUI.PANEL_WIDTH;
  public static final int PANEL_HEIGHT = GeographyUI.PANEL_HEIGHT;

  private static final int CLICK_MOVE_THRESHOLD_SQ = 9; // 3×3 px

  private final List<PlottedWaypoint> allWaypoints = new ArrayList<>();
  private final Map<String, GeographyNode> continents = new HashMap<>();
  private final Map<String, GeographyNode> regions = new HashMap<>();
  private final Map<String, GeographyNode> subregions = new HashMap<>();

  private final GeographyFilterDropdown filters;
  private final GeographyWaypointUI waypointUI;

  private double zoomScrollAccum = 0.0;
  private int leftPos;
  private int topPos;

  private int worldMinX;
  private int worldMaxX;
  private int worldMinZ;
  private int worldMaxZ;

  private boolean pointerDown;
  private boolean panning;
  private double pressX;
  private double pressY;
  private double dragLastX;
  private double dragLastY;
  private int pressButton = -1;
  /** Waypoint under cursor at press — click-copy uses this, not a re-pick. */
  private @Nullable PlottedWaypoint pressHit;

  public GeographyScreen(Component title) {
    super(title);
    rebuildWaypointIndex();
    filters = new GeographyFilterDropdown(
      continents,
      regions,
      subregions,
      allWaypoints
    );
    waypointUI = new GeographyWaypointUI(continents, regions, subregions);
  }

  @Override
  protected void init() {
    leftPos = (width - PANEL_WIDTH) / 2;
    topPos = (height - PANEL_HEIGHT) / 2;

    addRenderableWidget(
      new PlayerInventoryTabButton(
        leftPos,
        topPos,
        false,
        true,
        PlayerInventoryTabButton.Tab.INVENTORY,
        button -> {
          if (minecraft != null && minecraft.player != null) {
            minecraft.player.containerMenu = minecraft.player.inventoryMenu;
            minecraft.setScreen(new InventoryScreen(minecraft.player));
            PacketDistributor.sendToServer(
              new SwitchInventoryTabPacket(
                PlayerInventoryTabButton.Tab.INVENTORY
              )
            );
          }
        }
      )
    );
    addRenderableWidget(
      new PlayerInventoryTabButton(
        leftPos,
        topPos,
        false,
        true,
        PlayerInventoryTabButton.Tab.CALENDAR
      )
    );
    addRenderableWidget(
      new PlayerInventoryTabButton(
        leftPos,
        topPos,
        false,
        true,
        PlayerInventoryTabButton.Tab.NUTRITION
      )
    );
    addRenderableWidget(
      new PlayerInventoryTabButton(
        leftPos,
        topPos,
        false,
        true,
        PlayerInventoryTabButton.Tab.CLIMATE
      )
    );
    PatchouliIntegration.ifEnabled(() ->
      addRenderableWidget(
        new PlayerInventoryTabButton(
          leftPos,
          topPos,
          false,
          true,
          PlayerInventoryTabButton.Tab.BOOK
        )
      )
    );
    addRenderableWidget(
      new GeographyInventoryTabButton(leftPos, topPos, true, true, b -> {})
    );
  }

  private void rebuildWaypointIndex() {
    allWaypoints.clear();
    continents.clear();
    regions.clear();
    subregions.clear();

    int hs = TFCRealWorldConfig.HORIZONTAL_SCALE.get();
    int vs = TFCRealWorldConfig.VERTICAL_SCALE.get();
    worldMinX = -hs;
    worldMaxX = hs;
    worldMinZ = -vs;
    worldMaxZ = vs;

    MapProfile profile = ProfileManager.getProfile(
      TFCRealWorldConfig.MAP_PROFILE.get()
    );
    for (String ref : profile.waypoints()) {
      GeographyNode wp = GeographyManager.get(ref);
      if (wp == null) {
        TFCRealWorld.LOGGER.warn(
          "Profile waypoint ref not found in geography data: {}",
          ref
        );
        continue;
      }
      if (
        wp.kind() != GeographyKind.WAYPOINT ||
        wp.latitude() == null ||
        wp.longitude() == null
      ) {
        TFCRealWorld.LOGGER.warn(
          "Profile waypoint ref is not a plottable waypoint (need lat/lon): {}",
          ref
        );
        continue;
      }
      int[] xz = WaypointCoordinates.toBlockXZ(wp.latitude(), wp.longitude());
      GeographyNode sub = wp.parentRef() != null
        ? GeographyManager.get(wp.parentRef())
        : null;
      GeographyNode region = sub != null && sub.parentRef() != null
        ? GeographyManager.get(sub.parentRef())
        : null;
      GeographyNode continent = region != null && region.parentRef() != null
        ? GeographyManager.get(region.parentRef())
        : null;
      if (sub != null) {
        subregions.put(sub.ref(), sub);
      }
      if (region != null) {
        regions.put(region.ref(), region);
      }
      if (continent != null) {
        continents.put(continent.ref(), continent);
      }
      allWaypoints.add(
        new PlottedWaypoint(
          wp,
          xz[0],
          xz[1],
          continent != null ? continent.ref() : null,
          region != null ? region.ref() : null,
          sub != null ? sub.ref() : null
        )
      );
    }
  }

  private List<PlottedWaypoint> filtered() {
    List<PlottedWaypoint> out = new ArrayList<>();
    for (PlottedWaypoint w : allWaypoints) {
      if (
        GeographySessionState.filterContinent != null &&
        (w.continentRef() == null ||
          !w.continentRef().equals(GeographySessionState.filterContinent))
      ) {
        continue;
      }
      if (
        GeographySessionState.filterRegion != null &&
        (w.regionRef() == null ||
          !w.regionRef().equals(GeographySessionState.filterRegion))
      ) {
        continue;
      }
      if (
        GeographySessionState.filterSubregion != null &&
        (w.subregionRef() == null ||
          !w.subregionRef().equals(GeographySessionState.filterSubregion))
      ) {
        continue;
      }
      out.add(w);
    }
    return out;
  }

  @Override
  public void tick() {
    super.tick();
    GeographyMapViewport.clampPan(leftPos, topPos);
  }

  @Override
  public void render(
    GuiGraphics graphics,
    int mouseX,
    int mouseY,
    float partial
  ) {
    renderBackground(graphics, mouseX, mouseY, partial);
    drawChrome(graphics);

    graphics.drawString(
      font,
      title,
      leftPos + GeographyUI.MAP_X,
      topPos + GeographyUI.TITLE_PAD,
      0xFFE8F0FF,
      false
    );

    filters.renderChips(graphics, font, leftPos, topPos);

    GeographyMapViewport.View view = GeographyMapViewport.compute(
      leftPos,
      topPos
    );
    GeographyMapPainter.render(
      graphics,
      view,
      filtered(),
      worldMinX,
      worldMaxX,
      worldMinZ,
      worldMaxZ,
      minecraft
    );

    // Suppress tab hover/tooltips while pointer is over an open dropdown.
    int widgetMx = mouseX;
    int widgetMy = mouseY;
    if (
      filters.isOpen() &&
      filters.isOverDropdown(leftPos, topPos, mouseX, mouseY)
    ) {
      widgetMx = -10000;
      widgetMy = -10000;
    }
    for (var widget : this.renderables) {
      widget.render(graphics, widgetMx, widgetMy, partial);
    }

    if (filters.isOpen()) {
      waypointUI.setHovered(null);
      filters.renderOpen(graphics, font, leftPos, topPos, mouseX, mouseY);
    } else {
      @Nullable
      PlottedWaypoint hovered = waypointUI.pick(
        view,
        filtered(),
        mouseX,
        mouseY,
        worldMinX,
        worldMaxX,
        worldMinZ,
        worldMaxZ
      );
      waypointUI.setHovered(hovered);
      if (hovered != null) {
        graphics.renderComponentTooltip(
          font,
          waypointUI.tooltipFor(hovered),
          mouseX,
          mouseY
        );
      }
      waypointUI.syncHover(hovered);
    }
  }

  private void drawChrome(GuiGraphics graphics) {
    graphics.fill(
      leftPos,
      topPos,
      leftPos + PANEL_WIDTH,
      topPos + PANEL_HEIGHT,
      0xD010141C
    );
    graphics.fill(
      leftPos + 1,
      topPos + 1,
      leftPos + PANEL_WIDTH - 1,
      topPos + PANEL_HEIGHT - 1,
      0xA0182030
    );
    graphics.renderOutline(
      leftPos,
      topPos,
      PANEL_WIDTH,
      PANEL_HEIGHT,
      0xFF3A4A5C
    );
    graphics.fill(
      leftPos + 2,
      topPos + 2,
      leftPos + PANEL_WIDTH - 2,
      topPos + 3,
      0x402A3A4C
    );
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (filters.mouseClicked(leftPos, topPos, mouseX, mouseY)) {
      return true;
    }

    GeographyMapViewport.View view = GeographyMapViewport.compute(
      leftPos,
      topPos
    );
    if (GeographyMapViewport.contains(view, mouseX, mouseY)) {
      pointerDown = true;
      panning = false;
      pressX = mouseX;
      pressY = mouseY;
      dragLastX = mouseX;
      dragLastY = mouseY;
      pressButton = button;
      // Same pick as tooltip hover — copy this on release if not a drag.
      pressHit = waypointUI.pick(
        view,
        filtered(),
        mouseX,
        mouseY,
        worldMinX,
        worldMaxX,
        worldMinZ,
        worldMaxZ
      );
      if (pressHit == null) {
        pressHit = waypointUI.hovered();
      }
      return true;
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override
  public boolean mouseReleased(double mouseX, double mouseY, int button) {
    if (pointerDown && button == pressButton) {
      pointerDown = false;
      boolean wasPanning = panning;
      panning = false;
      pressButton = -1;
      // Prefer the waypoint that owned the tooltip at press (same hit-test).
      // Fallback: re-pick at release so a stationary click still works.
      if (!wasPanning) {
        @Nullable
        PlottedWaypoint hit = pressHit;
        if (hit == null) {
          GeographyMapViewport.View view = GeographyMapViewport.compute(
            leftPos,
            topPos
          );
          if (GeographyMapViewport.contains(view, mouseX, mouseY)) {
            hit = waypointUI.pick(
              view,
              filtered(),
              mouseX,
              mouseY,
              worldMinX,
              worldMaxX,
              worldMinZ,
              worldMaxZ
            );
          }
        }
        pressHit = null;
        if (hit != null) {
          waypointUI.copyCoords(minecraft, hit);
          return true;
        }
      }
      pressHit = null;
      return true;
    }
    return super.mouseReleased(mouseX, mouseY, button);
  }

  @Override
  public boolean mouseDragged(
    double mouseX,
    double mouseY,
    int button,
    double dragX,
    double dragY
  ) {
    if (pointerDown && button == pressButton) {
      double dx = mouseX - pressX;
      double dy = mouseY - pressY;
      if (!panning && dx * dx + dy * dy >= CLICK_MOVE_THRESHOLD_SQ) {
        panning = true;
      }
      if (panning) {
        GeographyMapViewport.panByPixels(
          mouseX - dragLastX,
          mouseY - dragLastY,
          leftPos,
          topPos
        );
        dragLastX = mouseX;
        dragLastY = mouseY;
      }
      return true;
    }
    return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
  }

  @Override
  public void mouseMoved(double mouseX, double mouseY) {
    if (
      filters.isOpen() &&
      filters.isOverDropdown(leftPos, topPos, mouseX, mouseY)
    ) {
      return;
    }
    super.mouseMoved(mouseX, mouseY);
  }

  @Override
  public boolean mouseScrolled(
    double mouseX,
    double mouseY,
    double scrollX,
    double scrollY
  ) {
    double sensitivity = 1.0;
    if (minecraft != null) {
      sensitivity = minecraft.options.mouseWheelSensitivity().get();
    }
    double delta = scrollY * sensitivity;

    if (filters.mouseScrolled(delta)) {
      return true;
    }

    GeographyMapViewport.View view = GeographyMapViewport.compute(
      leftPos,
      topPos
    );
    if (!GeographyMapViewport.contains(view, mouseX, mouseY)) {
      return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    zoomScrollAccum += delta;
    while (zoomScrollAccum >= 1.0) {
      GeographyMapViewport.zoomBy(+1, mouseX, mouseY, leftPos, topPos);
      zoomScrollAccum -= 1.0;
    }
    while (zoomScrollAccum <= -1.0) {
      GeographyMapViewport.zoomBy(-1, mouseX, mouseY, leftPos, topPos);
      zoomScrollAccum += 1.0;
    }
    return true;
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }
}
