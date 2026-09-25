package net.yazloysasha.tfcrealworld.client.widget;

import net.dries007.tfc.client.ClientHelpers;
import net.dries007.tfc.client.RenderHelpers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.network.OpenGeographyTabPacket;

/**
 * Sixth TFC-style inventory tab (below BOOK). Uses TFC tab chrome + our globe icon.
 * Positions mirror {@code PlayerInventoryTabButton.Tab} spacing (yIn = 119).
 */
public class GeographyInventoryTabButton extends Button {

  public static final int TAB_X_IN = 176;
  public static final int TAB_Y_IN = 119;

  private static final ResourceLocation GLOBE_ICON = TFCRealWorld.id(
    "textures/item/globe.png"
  );

  private int iconX;
  private int iconY;
  private int prevGuiLeft;
  private int prevGuiTop;
  private final boolean active;
  private final int textureU;
  private Runnable tickCallback = () -> {};

  public GeographyInventoryTabButton(
    int guiLeft,
    int guiTop,
    boolean active,
    boolean detached
  ) {
    this(guiLeft, guiTop, active, detached, button ->
      PacketDistributor.sendToServer(new OpenGeographyTabPacket())
    );
  }

  public GeographyInventoryTabButton(
    int guiLeft,
    int guiTop,
    boolean active,
    boolean detached,
    OnPress onPress
  ) {
    super(
      detached
        ? (guiLeft + TAB_X_IN + 110)
        : (guiLeft + TAB_X_IN + (active ? -3 : -2)),
      detached ? (guiTop + TAB_Y_IN + 5) : (guiTop + TAB_Y_IN),
      24,
      22,
      Component.empty(),
      onPress,
      RenderHelpers.NARRATION
    );
    this.prevGuiLeft = guiLeft;
    this.prevGuiTop = guiTop;
    this.textureU = detached ? (active ? 72 : 48) : (active ? 24 : 0);
    this.iconX = detached
      ? (guiLeft + TAB_X_IN + 113 + 1)
      : (guiLeft + TAB_X_IN + 1);
    this.iconY = detached
      ? (guiTop + TAB_Y_IN + 4 + 4)
      : (guiTop + TAB_Y_IN + 3);
    this.active = active;
  }

  public GeographyInventoryTabButton setRecipeBookCallback(
    InventoryScreen screen
  ) {
    this.tickCallback = new Runnable() {
      boolean recipeBookVisible = screen.getRecipeBookComponent().isVisible();

      @Override
      public void run() {
        boolean now = screen.getRecipeBookComponent().isVisible();
        if (now != recipeBookVisible) {
          recipeBookVisible = now;
          GeographyInventoryTabButton.this.updateGuiSize(
              screen.getGuiLeft(),
              screen.getGuiTop()
            );
        }
      }
    };
    return this;
  }

  @Override
  public void renderWidget(
    GuiGraphics graphics,
    int mouseX,
    int mouseY,
    float partialTicks
  ) {
    tickCallback.run();
    graphics.blit(
      ClientHelpers.GUI_ICONS,
      getX(),
      getY(),
      0,
      (float) textureU,
      16.0F,
      width,
      height,
      256,
      256
    );
    graphics.blit(GLOBE_ICON, iconX, iconY, 0, 0, 16, 16, 16, 16);
    if (this.isHovered() && !this.active) {
      graphics.renderTooltip(
        Minecraft.getInstance().font,
        Component.translatable("tfc_real_world.screen.geography"),
        mouseX,
        mouseY
      );
    }
  }

  public void updateGuiSize(int guiLeft, int guiTop) {
    setX(getX() + guiLeft - prevGuiLeft);
    setY(getY() + guiTop - prevGuiTop);
    this.iconX += guiLeft - prevGuiLeft;
    this.iconY += guiTop - prevGuiTop;
    prevGuiLeft = guiLeft;
    prevGuiTop = guiTop;
  }
}
