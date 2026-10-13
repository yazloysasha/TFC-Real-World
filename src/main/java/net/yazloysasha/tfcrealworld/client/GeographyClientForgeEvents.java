package net.yazloysasha.tfcrealworld.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.yazloysasha.tfcrealworld.TFCRealWorld;
import net.yazloysasha.tfcrealworld.client.tab.InventoryTabs;
import net.yazloysasha.tfcrealworld.client.widget.GeographyInventoryTabButton;

@Mod.EventBusSubscriber(modid = TFCRealWorld.MOD_ID, value = Dist.CLIENT)
public final class GeographyClientForgeEvents {

  private GeographyClientForgeEvents() {}

  /** Adds the globe below TFC's tabs wherever TFC shows them. */
  @SubscribeEvent
  public static void onScreenInit(ScreenEvent.InitScreenEvent.Post event) {
    Screen screen = event.getScreen();
    final InventoryTabs.Anchor book = InventoryTabs.CURRENT.bookAnchor(
      screen,
      event.getListenersList()
    );
    if (book != null) {
      event.addListener(new GeographyInventoryTabButton(book, false));
      return;
    }
    if (screen instanceof InventoryScreen inventoryScreen) {
      event.addListener(
        new GeographyInventoryTabButton(
          inventoryScreen.getGuiLeft(),
          inventoryScreen.getGuiTop()
        ).setRecipeBookCallback(inventoryScreen)
      );
      return;
    }
    if (
      screen instanceof AbstractContainerScreen<?> container &&
      InventoryTabs.CURRENT.isTabScreen(screen)
    ) {
      event.addListener(
        new GeographyInventoryTabButton(
          container.getGuiLeft(),
          container.getGuiTop()
        )
      );
    }
  }

  @SubscribeEvent
  public static void onLogout(ClientPlayerNetworkEvent.LoggedOutEvent event) {
    ClientVisitedWaypoints.clear();
    GeographyMapTexture.clear();
    GeographySessionState.clear();
  }
}
