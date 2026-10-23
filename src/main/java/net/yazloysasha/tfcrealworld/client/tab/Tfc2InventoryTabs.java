package net.yazloysasha.tfcrealworld.client.tab;

import java.util.ArrayList;
import java.util.List;
import net.dries007.tfc.client.screen.NutritionScreen;
import net.dries007.tfc.client.screen.button.PlayerInventoryTabButton;
import net.dries007.tfc.compat.patchouli.PatchouliIntegration;
import net.dries007.tfc.network.PacketHandler;
import net.dries007.tfc.network.SwitchInventoryTabPacket;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.network.PacketDistributor;

/** TFC's tabs. */
final class Tfc2InventoryTabs implements InventoryTabs {

  private static final TabChrome INACTIVE = new TabChrome(
    176,
    0,
    20,
    128,
    0,
    1
  );
  private static final TabChrome ACTIVE = new TabChrome(173, 0, 23, 148, 0, 1);
  /**
   * TFC has no tab for apart from the inventory: the open tab, less the three
   * columns that overlap and join the inventory's edge.
   */
  private static final TabChrome DETACHED_ACTIVE = new TabChrome(
    176,
    0,
    20,
    151,
    0,
    1
  );

  /** On a panel, the tabs hang off its right edge, apart from it. */
  @Override
  public Anchor panelAnchor(int left, int top, int width) {
    return new Anchor(left + width - INACTIVE.xIn(), top, true);
  }

  @Override
  public boolean isTabScreen(Screen screen) {
    return (
      screen instanceof NutritionScreen ||
      InventoryTabs.super.isTabScreen(screen)
    );
  }

  @Override
  public List<Button> tfcTabs(Anchor anchor, Runnable openInventory) {
    final int guiLeft = anchor.guiLeft();
    final int guiTop = anchor.guiTop();
    final List<Button> tabs = new ArrayList<>();
    tabs.add(
      tab(guiLeft, guiTop, 4, 0, 0, button -> {
        openInventory.run();
        PacketHandler.send(
          PacketDistributor.SERVER.noArg(),
          new SwitchInventoryTabPacket(SwitchInventoryTabPacket.Type.INVENTORY)
        );
      })
    );
    tabs.add(
      tab(guiLeft, guiTop, 27, 32, 0, SwitchInventoryTabPacket.Type.CALENDAR)
    );
    tabs.add(
      tab(guiLeft, guiTop, 50, 64, 0, SwitchInventoryTabPacket.Type.NUTRITION)
    );
    tabs.add(
      tab(guiLeft, guiTop, 73, 96, 0, SwitchInventoryTabPacket.Type.CLIMATE)
    );
    PatchouliIntegration.ifEnabled(() ->
      tabs.add(
        tab(guiLeft, guiTop, 96, 0, 32, SwitchInventoryTabPacket.Type.BOOK)
      )
    );
    return tabs;
  }

  @Override
  public TabChrome chrome(boolean active, boolean detached) {
    if (!active) {
      return INACTIVE;
    }
    return detached ? DETACHED_ACTIVE : ACTIVE;
  }

  private static Button tab(
    int guiLeft,
    int guiTop,
    int yIn,
    int iconU,
    int iconV,
    SwitchInventoryTabPacket.Type type
  ) {
    return tab(guiLeft, guiTop, yIn, iconU, iconV, button ->
      PacketHandler.send(
        PacketDistributor.SERVER.noArg(),
        new SwitchInventoryTabPacket(type)
      )
    );
  }

  private static Button tab(
    int guiLeft,
    int guiTop,
    int yIn,
    int iconU,
    int iconV,
    Button.OnPress onPress
  ) {
    return new PlayerInventoryTabButton(
      guiLeft,
      guiTop,
      INACTIVE.xIn(),
      yIn,
      INACTIVE.width(),
      22,
      INACTIVE.textureU(),
      INACTIVE.textureV(),
      INACTIVE.iconX(),
      3,
      iconU,
      iconV,
      onPress
    );
  }
}
