package net.yazloysasha.tfcrealworld.client.widget;

import net.dries007.tfc.client.ClientHelpers;
import net.dries007.tfc.client.RenderHelpers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.network.OpenGeographyTabPacket;
import net.yazloysasha.tfcrealworld.network.PacketHandler;

/**
 * Sixth TFC-style inventory tab, below TFC's own five. Uses TFC's tab
 * chrome with the globe for an icon.
 */
public class GeographyInventoryTabButton extends Button {

  /** Where TFC's tabs hang on the right edge of a 176-wide inventory. */
  public static final int TAB_X_IN = 176;
  public static final int TAB_Y_IN = 119;
  /** TFC's chrome of the open tab is wider and starts further left. */
  private static final int ACTIVE_X_IN = TAB_X_IN - 3;
  private static final int TAB_WIDTH = 20;
  private static final int ACTIVE_TAB_WIDTH = 23;
  private static final int TAB_HEIGHT = 22;
  private static final int TEXTURE_U = 128;
  private static final int ACTIVE_TEXTURE_U = 148;

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

  public GeographyInventoryTabButton(int guiLeft, int guiTop) {
    this(guiLeft, guiTop, false, button ->
      PacketHandler.INSTANCE.sendToServer(new OpenGeographyTabPacket())
    );
  }

  public GeographyInventoryTabButton(
    int guiLeft,
    int guiTop,
    boolean active,
    OnPress onPress
  ) {
    super(
      guiLeft + (active ? ACTIVE_X_IN : TAB_X_IN),
      guiTop + TAB_Y_IN,
      active ? ACTIVE_TAB_WIDTH : TAB_WIDTH,
      TAB_HEIGHT,
      Component.empty(),
      onPress,
      RenderHelpers.NARRATION
    );
    this.prevGuiLeft = guiLeft;
    this.prevGuiTop = guiTop;
    this.textureU = active ? ACTIVE_TEXTURE_U : TEXTURE_U;
    this.iconX = getX() + 1;
    this.iconY = getY() + 3;
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
      0.0F,
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
